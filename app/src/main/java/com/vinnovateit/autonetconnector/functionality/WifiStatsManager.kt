package com.vinnovateit.autonetconnector.functionality

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// Data classes remain the same
data class DataUsage(val rxBytes: Long, val txBytes: Long)
data class LiveDataPoint(val timestamp: Long, val usage: DataUsage)
data class LiveConnectionStatus(val startTimeMillis: Long, val ssid: String, val liveData: List<LiveDataPoint>)
data class SessionSummary(
  val ssid: String,
  val startTimestamp: Long,
  val endTimestamp: Long,
  val totalData: DataUsage,
  val history: List<LiveDataPoint>
)

object WifiStatsManager {
  private const val TAG = "WifiStatsManager"
  private const val PREFS_NAME = "wifi_stats_prefs"
  private const val KEY_SESSIONS = "session_summaries"
  private const val KEY_LIVE_SESSION = "live_session_status" // Key for saving ongoing session
  private const val POLLING_INTERVAL_MS = 2000L
  private val gson = Gson()

  private var applicationContext: Application? = null
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var loggingJob: Job? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private val isInitialized = AtomicBoolean(false)

  // State holders
  private var startRxBytes: Long = 0
  private var startTxBytes: Long = 0

  // --- StateFlows ---
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
    resumeLiveSession() // Attempt to resume a session on initialization
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

  fun startLogging(ssid: String) {
    if (loggingJob?.isActive == true) return

    showToast("Starting stats for: $ssid")
    startRxBytes = TrafficStats.getTotalRxBytes()
    startTxBytes = TrafficStats.getTotalTxBytes()

    if (startRxBytes == TrafficStats.UNSUPPORTED.toLong() || startTxBytes == TrafficStats.UNSUPPORTED.toLong()) {
      Log.e(TAG, "TrafficStats is not supported on this device.")
      _systemStatus.value = "TrafficStats not supported on this device."
      return
    }

    val startTime = System.currentTimeMillis()
    val initialStatus = LiveConnectionStatus(
      startTimeMillis = startTime,
      ssid = ssid,
      liveData = listOf(LiveDataPoint(startTime, DataUsage(0, 0)))
    )
    _liveStatus.value = initialStatus
    saveLiveSessionState(initialStatus) // Save initial state immediately

    var lastTimestampRxBytes = startRxBytes
    var lastTimestampTxBytes = startTxBytes

    loggingJob = managerScope.launch {
      while (true) {
        delay(POLLING_INTERVAL_MS)
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val intervalRx = (currentRxBytes - lastTimestampRxBytes).coerceAtLeast(0)
        val intervalTx = (currentTxBytes - lastTimestampTxBytes).coerceAtLeast(0)
        lastTimestampRxBytes = currentRxBytes
        lastTimestampTxBytes = currentTxBytes

        if (intervalRx > 0 || intervalTx > 0) {
          val currentStatus = _liveStatus.value
          if (currentStatus != null) {
            _liveStatus.value = currentStatus.copy(
              liveData = currentStatus.liveData + LiveDataPoint(System.currentTimeMillis(), DataUsage(intervalRx, intervalTx))
            )
          }
        }
      }
    }
  }

  fun stopLogging() {
    loggingJob?.cancel()
    loggingJob = null
    val session = _liveStatus.value ?: return
    _liveStatus.value = null
    showToast("Stopping stats for: ${session.ssid}")

    val duration = System.currentTimeMillis() - session.startTimeMillis
    val totalRx = (TrafficStats.getTotalRxBytes() - startRxBytes).coerceAtLeast(0)
    val totalTx = (TrafficStats.getTotalTxBytes() - startTxBytes).coerceAtLeast(0)

    if (duration < 5000 && totalRx < 1024 && totalTx < 1024) {
      Log.d(TAG, "Trivial session discarded for SSID: ${session.ssid}")
      clearLiveSessionState()
      return
    }

    val summary = SessionSummary(
      ssid = session.ssid,
      startTimestamp = session.startTimeMillis,
      endTimestamp = System.currentTimeMillis(),
      totalData = DataUsage(totalRx, totalTx),
      history = session.liveData
    )

    val updatedHistory = (_sessionSummaries.value + summary).sortedByDescending { it.startTimestamp }
    _sessionSummaries.value = updatedHistory
    _lastSession.value = summary
    saveSessions()
    clearLiveSessionState() // Clear the live session state after saving the final summary
  }

  private fun saveSessions() {
    val context = applicationContext ?: return
    managerScope.launch {
      try {
        val json = gson.toJson(_sessionSummaries.value)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putString(KEY_SESSIONS, json)?.apply()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save sessions", e)
      }
    }
  }

  private fun loadSessions() {
    val context = applicationContext ?: return
    managerScope.launch {
      try {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getString(KEY_SESSIONS, null)
        if (json != null) {
          val type = object : TypeToken<ArrayList<SessionSummary>>() {}.type
          val sessions: List<SessionSummary> = gson.fromJson(json, type)
          val sortedSessions = sessions.sortedByDescending { it.startTimestamp }
          _sessionSummaries.value = sortedSessions
          if (_liveStatus.value == null) {
            _lastSession.value = sortedSessions.firstOrNull()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load sessions", e)
      }
    }
  }

  private fun saveLiveSessionState(liveStatus: LiveConnectionStatus) {
    val context = applicationContext ?: return
    managerScope.launch {
      try {
        val json = gson.toJson(liveStatus)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
          putString(KEY_LIVE_SESSION, json)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save live session state", e)
      }
    }
  }

  private fun resumeLiveSession() {
    val context = applicationContext ?: return
    managerScope.launch {
      try {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LIVE_SESSION, null)
        if (json != null) {
          val type = object : TypeToken<LiveConnectionStatus>() {}.type
          val resumedSession: LiveConnectionStatus = gson.fromJson(json, type)
          Log.d(TAG, "Resuming interrupted session for ${resumedSession.ssid}")
          showToast("Resuming session for ${resumedSession.ssid}")

          // To resume, we essentially just restart the logging with the original start time
          // A more complex implementation could try to reconstruct the lost data points
          startLogging(resumedSession.ssid)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to resume live session", e)
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