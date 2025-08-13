package com.vinnovateit.latch.domain.model

import android.app.Application
import android.content.Context
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.vinnovateit.latch.features.wifi.widget.LatchWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class DataUsage(val rxBytes: Long, val txBytes: Long)
data class LiveDataPoint(val timestamp: Long, val usage: DataUsage)
data class LiveConnectionStatus(val startTimeMillis: Long, val liveData: List<LiveDataPoint>)
data class SessionSummary(
  val startTimestamp: Long,
  val endTimestamp: Long,
  val totalData: DataUsage,
  val history: List<LiveDataPoint>
)

object WifiStatsManager {
  private const val PREFS_NAME = "wifi_stats_prefs"
  private const val KEY_SESSIONS = "session_summaries"
  private const val KEY_LIVE_SESSION = "live_session_status"
  private const val POLLING_INTERVAL_MS = 2000L
  private val gson = Gson()

  private var applicationContext: Application? = null
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var loggingJob: Job? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private val isInitialized = AtomicBoolean(false)

  private var startRxBytes: Long = 0
  private var startTxBytes: Long = 0

  private val _liveStatus = MutableStateFlow<LiveConnectionStatus?>(null)
  val liveStatus = _liveStatus.asStateFlow()

  private val _lastSession = MutableStateFlow<SessionSummary?>(null)
  val lastSession = _lastSession.asStateFlow()

  private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
  val sessionSummaries = _sessionSummaries.asStateFlow()

  private val _systemStatus = MutableStateFlow<String?>(null)
  val systemStatus = _systemStatus.asStateFlow()

  fun initialize(context: Application) {
    if (isInitialized.getAndSet(true)) return
    applicationContext = context
    loadSessions()
    resumeLiveSession()
  }

  fun startLogging(context: Context) {
    if (loggingJob?.isActive == true) return

    showToast("Starting stats")
    startRxBytes = TrafficStats.getTotalRxBytes()
    startTxBytes = TrafficStats.getTotalTxBytes()

    if (startRxBytes == TrafficStats.UNSUPPORTED.toLong() || startTxBytes == TrafficStats.UNSUPPORTED.toLong()) {
      _systemStatus.value = "TrafficStats not supported on this device."
      return
    }

    val startTime = System.currentTimeMillis()
    val initialStatus = LiveConnectionStatus(
      startTimeMillis = startTime,
      liveData = listOf(LiveDataPoint(startTime, DataUsage(0, 0)))
    )
    _liveStatus.value = initialStatus
    saveLiveSessionState(initialStatus)

    var lastTimestampRxBytes = startRxBytes
    var lastTimestampTxBytes = startTxBytes

    loggingJob = managerScope.launch {
      var initialUpdateSent = false
      while (true) {
        delay(POLLING_INTERVAL_MS)
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val intervalRx = (currentRxBytes - lastTimestampRxBytes).coerceAtLeast(0)
        val intervalTx = (currentTxBytes - lastTimestampTxBytes).coerceAtLeast(0)
        lastTimestampRxBytes = currentRxBytes
        lastTimestampTxBytes = currentTxBytes

        val currentStatus = _liveStatus.value
        if (currentStatus != null) {
          _liveStatus.value = currentStatus.copy(
            liveData = currentStatus.liveData + LiveDataPoint(System.currentTimeMillis(), DataUsage(intervalRx, intervalTx))
          )
        }

        if (!initialUpdateSent) {
          triggerWidgetUpdate(context)
          initialUpdateSent = true
        }
      }
    }
  }

  fun stopLogging(context: Context) {
    if (loggingJob == null && _liveStatus.value == null) return
    loggingJob?.cancel()
    loggingJob = null
    val session = _liveStatus.value ?: return

    _liveStatus.value = null
    showToast("Stopping stats")

    val duration = System.currentTimeMillis() - session.startTimeMillis
    val totalRx = (TrafficStats.getTotalRxBytes() - startRxBytes).coerceAtLeast(0)
    val totalTx = (TrafficStats.getTotalTxBytes() - startTxBytes).coerceAtLeast(0)

    if (duration < 5000 && totalRx < 1024 && totalTx < 1024) {
      clearLiveSessionState()
      triggerWidgetUpdate(context)
      return
    }

    val summary = SessionSummary(
      startTimestamp = session.startTimeMillis,
      endTimestamp = System.currentTimeMillis(),
      totalData = DataUsage(totalRx, totalTx),
      history = session.liveData
    )

    val updatedHistory = (_sessionSummaries.value + summary).sortedByDescending { it.startTimestamp }
    _sessionSummaries.value = updatedHistory
    _lastSession.value = summary
    saveSessions()
    clearLiveSessionState()
    triggerWidgetUpdate(context)
  }

  private fun triggerWidgetUpdate(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
  }

  private fun resumeLiveSession() {
    val context = applicationContext ?: return
    managerScope.launch {
      val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LIVE_SESSION, null)
      if (json != null) {
        val type = object : com.google.gson.reflect.TypeToken<LiveConnectionStatus>() {}.type
        val resumedSession: LiveConnectionStatus = gson.fromJson(json, type)
        startLogging(context)
      }
    }
  }

  fun clearHistory() {
    val context = applicationContext ?: return
    managerScope.launch {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
      _sessionSummaries.value = emptyList()
      _lastSession.value = null
      showToast("Stats history cleared")
    }
  }

  private fun saveSessions() {
    val context = applicationContext ?: return
    managerScope.launch {
      val json = gson.toJson(_sessionSummaries.value)
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putString(KEY_SESSIONS, json)?.apply()
    }
  }

  private fun loadSessions() {
    val context = applicationContext ?: return
    managerScope.launch {
      val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getString(KEY_SESSIONS, null)
      if (json != null) {
        val type = object : com.google.gson.reflect.TypeToken<ArrayList<SessionSummary>>() {}.type
        val sessions: List<SessionSummary> = gson.fromJson(json, type)
        val sortedSessions = sessions.sortedByDescending { it.startTimestamp }
        _sessionSummaries.value = sortedSessions
        if (_liveStatus.value == null) {
          _lastSession.value = sortedSessions.firstOrNull()
        }
      }
    }
  }

  private fun saveLiveSessionState(liveStatus: LiveConnectionStatus) {
    val context = applicationContext ?: return
    managerScope.launch {
      val json = gson.toJson(liveStatus)
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_LIVE_SESSION, json)
      }
    }
  }

  private fun clearLiveSessionState() {
    val context = applicationContext ?: return
    managerScope.launch {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        remove(KEY_LIVE_SESSION)
      }
    }
  }

  private fun showToast(message: String) {
    mainHandler.post {
      Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
  }
}