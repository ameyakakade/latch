package com.vinnovateit.autonetconnector.functionality2.manager

import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vinnovateit.autonetconnector.data.DailyUsage
import com.vinnovateit.autonetconnector.data.LatchDatabase
import com.vinnovateit.autonetconnector.data.Session
import com.vinnovateit.autonetconnector.data.StatsDao
import com.vinnovateit.autonetconnector.widget.LatchWidgetUpdater
import java.util.Calendar
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object SessionRepository {
  private var applicationContext: Application? = null
  private lateinit var statsDao: StatsDao
  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionUpdateJob: Job? = null
  private val isInitialized = AtomicBoolean(false)

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
            totalData = DataUsage(it.dataUsed, 0), // DB only stores total data
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

    UiNotifier.showToast(context, "Starting stats for: $ssid")

    val startTime = System.currentTimeMillis()
    val initialStatus = LiveConnectionStatus(
      startTimeMillis = startTime,
      ssid = ssid,
      liveData = listOf(LiveDataPoint(startTime, DataUsage(0, 0)))
    )
    _liveStatus.value = initialStatus
    TrafficStatsLogger.start()

    sessionUpdateJob = repoScope.launch {
      TrafficStatsLogger.dataUsageFlow.collect { dataUsage ->
        val currentStatus = _liveStatus.value
        if (currentStatus != null) {
          val newPoint = LiveDataPoint(System.currentTimeMillis(), dataUsage)
          _liveStatus.value = currentStatus.copy(
            liveData = currentStatus.liveData + newPoint
          )
        }
      }
    }
    triggerWidgetUpdate()
  }

  fun stopSession() {
    if (sessionUpdateJob == null && _liveStatus.value == null) return
    val sessionToFinalize = _liveStatus.value ?: return

    sessionUpdateJob?.cancel()
    sessionUpdateJob = null
    TrafficStatsLogger.stop()
    _liveStatus.value = null

    UiNotifier.showToast(applicationContext!!, "Stopping stats for: ${sessionToFinalize.ssid}")

    val totalDataUsed = sessionToFinalize.liveData.sumOf { it.usage.rxBytes + it.usage.txBytes }

    if (totalDataUsed < 1024) { // Don't save empty sessions
      triggerWidgetUpdate()
      return
    }

    repoScope.launch {
      val session = Session(
        startTime = Date(sessionToFinalize.startTimeMillis),
        endTime = Date(System.currentTimeMillis()),
        dataUsed = totalDataUsed
      )
      addSessionToDb(session)
    }
    triggerWidgetUpdate()
  }

  private suspend fun addSessionToDb(session: Session) {
    statsDao.insertSession(session)
    val sessionDate = getStartOfDay(session.startTime)
    val dataUsed = session.dataUsed
    val existingDailyUsage = statsDao.getUsageForDay(sessionDate)

    if (existingDailyUsage == null) {
      statsDao.insertDailyUsage(DailyUsage(date = sessionDate, totalDataUsed = dataUsed))
    } else {
      val updatedUsage = existingDailyUsage.totalDataUsed + dataUsed
      statsDao.updateDailyUsage(existingDailyUsage.copy(totalDataUsed = updatedUsage))
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
