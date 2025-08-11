// main/java/com/vinnovateit/latch/domain/model/SessionRepository.kt
package com.vinnovateit.latch.domain.model

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.util.formatBytes
import com.vinnovateit.latch.data.DailyUsage
import com.vinnovateit.latch.data.LatchDatabase
import com.vinnovateit.latch.data.Session
import com.vinnovateit.latch.data.StatsDao
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.wifi.widget.LatchWidgetUpdater
import com.vinnovateit.latch.features.wifi.manager.TrafficStatsLogger
import com.vinnovateit.latch.features.wifi.manager.UiNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object SessionRepository {
  private var applicationContext: Application? = null
  private lateinit var statsDao: StatsDao
  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionUpdateJob: Job? = null
  private val isInitialized = AtomicBoolean(false)
  private var notificationSent = false


  private val _liveStatus = MutableStateFlow<LiveConnectionStatus?>(null)
  val liveStatus = _liveStatus.asStateFlow()

  private val _lastSession = MutableStateFlow<SessionSummary?>(null)
  val lastSession = _lastSession.asStateFlow()

  private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
  val sessionSummaries = _sessionSummaries.asStateFlow()

  fun initialize(context: Application) {
    if (isInitialized.getAndSet(true)) return
    applicationContext = context
    val database = LatchDatabase.getDatabase(context)
    statsDao = database.statsDao()
    loadSessionsFromDb()
  }

  private fun loadSessionsFromDb() {
    repoScope.launch {
      statsDao.getAllSessions().map { dbSessions ->
        // Map Room Session entities to UI SessionSummary objects
        dbSessions.map {
          SessionSummary(
            ssid = "VIT-WiFi", // SSID is not stored in DB, using a placeholder
            startTimestamp = it.startTime.time,
            endTimestamp = it.endTime.time,
            totalData = DataUsage(
              rxBytes = it.rxBytes,
              txBytes = it.txBytes
            ),
            history = emptyList() // History is not persisted
          )
        }
      }.collect { summaries ->
        _sessionSummaries.value = summaries
        _lastSession.value = summaries.firstOrNull()
      }
    }
  }

  fun startSession(ssid: String) {
    if (sessionUpdateJob?.isActive == true || _liveStatus.value != null) return
    val context = applicationContext ?: return
    notificationSent = false // Reset notification flag for new session


    UiNotifier.showToast(context, "Starting stats for: $ssid")

    val startTime = System.currentTimeMillis()
    val initialStatus =
      LiveConnectionStatus(
        startTimeMillis = startTime,
        ssid = ssid,
        liveData = listOf(
          LiveDataPoint(
            startTime,
            DataUsage(0, 0)
          )
        )
      )
    _liveStatus.value = initialStatus
    TrafficStatsLogger.start()

    sessionUpdateJob = repoScope.launch {
      TrafficStatsLogger.dataUsageFlow.collect { dataUsage ->
        val currentStatus = _liveStatus.value
        if (currentStatus != null) {
          val newPoint =
            LiveDataPoint(
              System.currentTimeMillis(),
              dataUsage
            )
          val updatedStatus = currentStatus.copy(
            liveData = currentStatus.liveData + newPoint
          )
          _liveStatus.value = updatedStatus
          checkDataThreshold(updatedStatus)
        }
      }
    }
    triggerWidgetUpdate()
  }

  private fun checkDataThreshold(status: LiveConnectionStatus) {
    val context = applicationContext ?: return
    if (!SettingsManager.dataAlertEnabled.value || notificationSent) {
      return
    }

    var thresholdGB = SettingsManager.dataThreshold.value
    if (thresholdGB == 0f) return // A zero threshold makes no sense, so we ignore it.
    if (thresholdGB < 0) {
      thresholdGB = -thresholdGB
    }

    val thresholdBytes = thresholdGB * 1024 * 1024 * 1024
    val currentUsageBytes = status.liveData.sumOf { it.usage.rxBytes + it.usage.txBytes }

    if (currentUsageBytes >= thresholdBytes) {
      sendDataUsageNotification(currentUsageBytes, thresholdBytes.toFloat())
      notificationSent = true
    }
  }

  private fun sendDataUsageNotification(currentUsage: Long, threshold: Float) {
    val context = applicationContext ?: return
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "data_usage_alert_channel"

    val channel = NotificationChannel(
      channelId,
      "Data Usage Alerts",
      NotificationManager.IMPORTANCE_HIGH
    )
    notificationManager.createNotificationChannel(channel)

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, channelId)
      .setContentTitle("Data Usage Alert")
      .setContentText("You've used ${formatBytes(currentUsage).first} ${formatBytes(currentUsage).second} of your ${formatBytes(threshold.toLong()).first} ${formatBytes(threshold.toLong()).second} limit.")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(2, notification)
  }


  fun stopSession() {
    if (sessionUpdateJob == null && _liveStatus.value == null) return
    val sessionToFinalize = _liveStatus.value ?: return

    sessionUpdateJob?.cancel()
    sessionUpdateJob = null
    TrafficStatsLogger.stop()
    _liveStatus.value = null

    UiNotifier.showToast(applicationContext!!, "Stopping stats for: ${sessionToFinalize.ssid}")

    val totalRxBytes = sessionToFinalize.liveData.sumOf { it.usage.rxBytes }
    val totalTxBytes = sessionToFinalize.liveData.sumOf { it.usage.txBytes }
    val totalDataUsed = totalRxBytes + totalTxBytes

    if (totalDataUsed < 1024) { // Don't save empty sessions
      triggerWidgetUpdate()
      return
    }

    repoScope.launch {
      val session = Session(
        startTime = Date(sessionToFinalize.startTimeMillis),
        endTime = Date(System.currentTimeMillis()),
        rxBytes = totalRxBytes,
        txBytes = totalTxBytes
      )
      addSessionToDb(session)
    }
    triggerWidgetUpdate()
  }

  private suspend fun addSessionToDb(session: Session) {
    statsDao.insertSession(session)
    val sessionDate = getStartOfDay(session.startTime)
    val existingDailyUsage = statsDao.getUsageForDay(sessionDate)

    if (existingDailyUsage == null) {
      statsDao.insertDailyUsage(DailyUsage(date = sessionDate, totalRxBytes = session.rxBytes, totalTxBytes = session.txBytes))
    } else {
      val updatedRx = existingDailyUsage.totalRxBytes + session.rxBytes
      val updatedTx = existingDailyUsage.totalTxBytes + session.txBytes
      statsDao.updateDailyUsage(existingDailyUsage.copy(totalRxBytes = updatedRx, totalTxBytes = updatedTx))
    }
  }

  fun clearHistory() {
    repoScope.launch {
      statsDao.clearAllSessions()
      statsDao.clearAllDailyUsage()
      UiNotifier.showToast(applicationContext!!, "Stats history cleared")
    }
  }

  private fun triggerWidgetUpdate() {
    val context = applicationContext ?: return
    val workRequest = OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
  }

  private fun getStartOfDay(date: Date): Date {
    return Calendar.getInstance().apply {
      time = date
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time
  }
}