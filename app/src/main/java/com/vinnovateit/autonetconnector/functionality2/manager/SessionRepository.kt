package com.vinnovateit.autonetconnector.functionality2.manager

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vinnovateit.autonetconnector.widget.UpdateWidgetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages session data, including live status, history, and persistence.
 * This is the single source of truth for all session-related information.
 */
object SessionRepository {
  // region --- Constants and Properties ---
  private const val PREFS_NAME = "wifi_stats_prefs"
  private const val KEY_SESSIONS = "session_summaries"
  private const val KEY_LIVE_SESSION = "live_session_status"
  private val gson = Gson()

  private var applicationContext: Application? = null
  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var sessionUpdateJob: Job? = null
  private val isInitialized = AtomicBoolean(false)

  private val _liveStatus = MutableStateFlow<LiveConnectionStatus?>(null)
  val liveStatus = _liveStatus.asStateFlow()

  private val _lastSession = MutableStateFlow<SessionSummary?>(null)
  val lastSession = _lastSession.asStateFlow()

  private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
  val sessionSummaries = _sessionSummaries.asStateFlow()
  // endregion

  /**
   * Initializes the repository, loading any persisted session data.
   * Must be called once, typically from an Application class or the first ViewModel.
   */
  fun initialize(context: Application) {
    if (isInitialized.getAndSet(true)) return
    applicationContext = context
    loadSessions()
    // If the app was killed, this will finalize the stale session.
    resumeLiveSession()
  }

  /**
   * Starts a new logging session for the given SSID.
   * It begins polling for traffic stats and updates the live status.
   */
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
    saveLiveSessionState(initialStatus)

    TrafficStatsLogger.start()

    // Collect data from the logger and update the live session
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
    triggerWidgetUpdate(context)
  }

  /**
   * Stops the current logging session, finalizes the data, and persists it.
   */
  fun stopSession() {
    if (sessionUpdateJob == null && _liveStatus.value == null) return
    val context = applicationContext ?: return

    sessionUpdateJob?.cancel()
    sessionUpdateJob = null
    TrafficStatsLogger.stop()

    val sessionToFinalize = _liveStatus.value ?: return
    _liveStatus.value = null // Clear live status immediately

    UiNotifier.showToast(context, "Stopping stats for: ${sessionToFinalize.ssid}")

    val totalRx = sessionToFinalize.liveData.sumOf { it.usage.rxBytes }
    val totalTx = sessionToFinalize.liveData.sumOf { it.usage.txBytes }
    val duration = System.currentTimeMillis() - sessionToFinalize.startTimeMillis

    // Don't save very short or empty sessions
    if (duration < 5000 && totalRx < 1024 && totalTx < 1024) {
      clearLiveSessionState()
      triggerWidgetUpdate(context)
      return
    }

    val summary = SessionSummary(
      ssid = sessionToFinalize.ssid,
      startTimestamp = sessionToFinalize.startTimeMillis,
      endTimestamp = System.currentTimeMillis(),
      totalData = DataUsage(totalRx, totalTx),
      history = sessionToFinalize.liveData
    )

    val updatedHistory = (_sessionSummaries.value + summary).sortedByDescending { it.startTimestamp }
    _sessionSummaries.value = updatedHistory
    _lastSession.value = summary
    saveSessions()
    clearLiveSessionState()
    triggerWidgetUpdate(context)
  }

  /**
   * Clears all persisted session history.
   */
  fun clearHistory() {
    val context = applicationContext ?: return
    repoScope.launch {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
      _sessionSummaries.value = emptyList()
      _lastSession.value = null
      UiNotifier.showToast(context, "Stats history cleared")
    }
  }

  // region --- Persistence ---
  private fun resumeLiveSession() {
    val context = applicationContext ?: return
    repoScope.launch {
      val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LIVE_SESSION, null)
      if (json != null) {
        // If a live session was saved, it means the app was killed.
        // We finalize it as a completed session.
        stopSession()
      }
    }
  }

  private fun triggerWidgetUpdate(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<UpdateWidgetWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
  }

  private fun saveSessions() {
    val context = applicationContext ?: return
    repoScope.launch {
      val json = gson.toJson(_sessionSummaries.value)
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putString(KEY_SESSIONS, json)?.apply()
    }
  }

  private fun loadSessions() {
    val context = applicationContext ?: return
    repoScope.launch {
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
    }
  }

  private fun saveLiveSessionState(liveStatus: LiveConnectionStatus) {
    val context = applicationContext ?: return
    repoScope.launch {
      val json = gson.toJson(liveStatus)
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_LIVE_SESSION, json)
      }
    }
  }

  private fun clearLiveSessionState() {
    val context = applicationContext ?: return
    repoScope.launch {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        remove(KEY_LIVE_SESSION)
      }
    }
  }
  // endregion
}
