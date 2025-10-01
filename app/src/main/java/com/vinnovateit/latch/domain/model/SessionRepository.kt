package com.vinnovateit.latch.domain.model

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.vinnovateit.latch.data.StatsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import com.vinnovateit.latch.data.LatchDatabase
import com.vinnovateit.latch.data.Session
import com.vinnovateit.latch.features.wifi.manager.TrafficStatsLogger
import com.vinnovateit.latch.features.wifi.manager.UiNotifier
import com.vinnovateit.latch.features.wifi.quicksettings.LatchTileService
import com.vinnovateit.latch.features.wifi.widget.LatchWidgetUpdater
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date

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
        dbSessions.map {
          SessionSummary(
            startTimestamp = it.startTime.time,
            endTimestamp = it.endTime.time,
            totalData = DataUsage(
              rxBytes = it.rxBytes,
              txBytes = it.txBytes
            ),
            history = emptyList(),
            maxRxBps = it.maxRxBps,
            maxTxBps = it.maxTxBps
          )
        }
      }.collect { summaries ->
        _sessionSummaries.value = summaries
        _lastSession.value = summaries.firstOrNull()
      }
    }
  }

  fun startSession(network: android.net.Network) {
    if (sessionUpdateJob?.isActive == true || _liveStatus.value != null) return
    val context = applicationContext ?: return
    notificationSent = false

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    connectivityManager.bindProcessToNetwork(network)

    val startTime = System.currentTimeMillis()
    val initialStatus =
      LiveConnectionStatus(
        startTimeMillis = startTime,
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
        }
      }
    }
    triggerWidgetUpdate()
    notifyTileUpdate()
  }

  fun stopSession() {
    val context = applicationContext ?: return
    if (sessionUpdateJob == null && _liveStatus.value == null) return
    val sessionToFinalize = _liveStatus.value ?: return

    sessionUpdateJob?.cancel()
    sessionUpdateJob = null
    TrafficStatsLogger.stop()

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    connectivityManager.bindProcessToNetwork(null)
    _liveStatus.value = null

    UiNotifier.showToast(applicationContext!!, "Disconnected")

    val totalRxBytes = sessionToFinalize.liveData.sumOf { it.usage.rxBytes }
    val totalTxBytes = sessionToFinalize.liveData.sumOf { it.usage.txBytes }
    val totalDataUsed = totalRxBytes + totalTxBytes
    val maxRxBps = sessionToFinalize.liveData.maxOfOrNull { it.usage.rxBytes } ?: 0L
    val maxTxBps = sessionToFinalize.liveData.maxOfOrNull { it.usage.txBytes } ?: 0L

    if (totalDataUsed < 1024) {
      triggerWidgetUpdate()
      notifyTileUpdate()
      return
    }

    repoScope.launch {
      val session = Session(
        startTime = Date(sessionToFinalize.startTimeMillis),
        endTime = Date(System.currentTimeMillis()),
        rxBytes = totalRxBytes,
        txBytes = totalTxBytes,
        maxRxBps = maxRxBps,
        maxTxBps = maxTxBps
      )
      addSessionToDb(session)
    }
    triggerWidgetUpdate()
    notifyTileUpdate()
  }

  private suspend fun addSessionToDb(session: Session) {
    statsDao.insertSession(session)
  }

  fun clearHistory() {
    repoScope.launch {
      statsDao.clearAllSessions()
      UiNotifier.showToast(applicationContext!!, "Stats Cleared!")
    }
  }

  private fun triggerWidgetUpdate() {
    val context = applicationContext ?: return
    val workRequest = androidx.work.OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
    androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
  }

  private fun notifyTileUpdate() {
    val context = applicationContext ?: return
    TileService.requestListeningState(
      context,
      ComponentName(context, LatchTileService::class.java)
    )
  }
}