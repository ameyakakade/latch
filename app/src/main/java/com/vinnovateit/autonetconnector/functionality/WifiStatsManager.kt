package com.vinnovateit.autonetconnector.functionality

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
  private const val POLLING_INTERVAL_MS = 1000L
  private val gson = Gson()

  private var applicationContext: Application? = null
  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var loggingJob: Job? = null
  private var networkCallback: ConnectivityManager.NetworkCallback? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private val isInitialized = AtomicBoolean(false)

  // State holders
  private var startRxBytes: Long = 0
  private var startTxBytes: Long = 0
  private var lastTimestampRxBytes: Long = 0
  private var lastTimestampTxBytes: Long = 0

  // --- StateFlows ---
  private val _liveStatus = MutableStateFlow<LiveConnectionStatus?>(null)
  val liveStatus = _liveStatus.asStateFlow()

  private val _lastSession = MutableStateFlow<SessionSummary?>(null)
  val lastSession = _lastSession.asStateFlow()

  private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
  val sessionSummaries = _sessionSummaries.asStateFlow()

  // NEW: Status flow to communicate issues like disabled location to the UI
  private val _systemStatus = MutableStateFlow<String?>(null)
  val systemStatus = _systemStatus.asStateFlow()

  fun initialize(context: Application) {
    if (isInitialized.getAndSet(true)) return
    applicationContext = context
    showToast("WifiStatsManager Initialized")
    loadSessions()
    registerNetworkCallback()
  }

  fun cleanup() {
    if (!isInitialized.getAndSet(false)) return
    val context = applicationContext ?: return
    showToast("WifiStatsManager Cleaned Up")
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    try {
      networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error unregistering network callback", e)
    }
    managerScope.cancel(CancellationException("WifiStatsManager cleaned up"))
    applicationContext = null
    networkCallback = null
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

  private fun registerNetworkCallback() {
    val context = applicationContext ?: return
    if (networkCallback != null) return

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkRequest = NetworkRequest.Builder()
      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
      .build()

    networkCallback = object : ConnectivityManager.NetworkCallback() {
      @RequiresApi(Build.VERSION_CODES.Q)
      override fun onAvailable(network: Network) {
        super.onAvailable(network)
        showToast("Wi-Fi network available")
        handleWifiConnection(connectivityManager, network)
      }
      override fun onLost(network: Network) {
        super.onLost(network)
        showToast("Wi-Fi network lost")
        if (_liveStatus.value != null) {
          stopLogging()
        }
      }
      @RequiresApi(Build.VERSION_CODES.Q)
      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
          Log.d(TAG, "Wi-Fi capabilities changed.")
          handleWifiConnection(connectivityManager, network)
        }
      }
    }
    connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    showToast("Network callback registered")
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun handleWifiConnection(cm: ConnectivityManager, network: Network) {
    val context = applicationContext ?: return

    // 1. Check for required location permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      _systemStatus.value = "Location permission is required to get the Wi-Fi name."
      Log.w(TAG, "Location permission not granted.")
      return
    }

    // 2. Check if Location Services are enabled on the device
    if (!isLocationEnabled(context)) {
      _systemStatus.value = "Please turn on Location Services to see the Wi-Fi name."
      Log.w(TAG, "Location services are turned off.")
      if (_liveStatus.value?.ssid != "Location Off") {
        stopLogging()
        startLogging("Location Off")
      }
      return
    }

    // If we reach here, permissions and settings are okay. Clear any status message.
    _systemStatus.value = null

    // 3. Get NetworkCapabilities to access WifiInfo safely
    val capabilities = cm.getNetworkCapabilities(network)
    val wifiInfo = capabilities?.transportInfo as? WifiInfo

    if (wifiInfo == null) {
      Log.w(TAG, "WifiInfo is null. Cannot retrieve SSID yet.")
      return // The callback will be triggered again when it's ready.
    }

    val ssid = wifiInfo.ssid?.replace("\"", "")

    if (ssid != null && ssid != "<unknown ssid>") {
      if (_liveStatus.value?.ssid != ssid) {
        stopLogging()
        startLogging(ssid)
      }
    } else {
      Log.w(TAG, "Could not retrieve a valid SSID. Current value: $ssid")
    }
  }

  private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
  }

  private fun startLogging(ssid: String) {
    if (loggingJob?.isActive == true) {
      if (_liveStatus.value?.ssid == ssid) return // Already logging for the same network
      stopLogging() // Stop previous session
    }

    showToast("Starting stats for: $ssid")
    startRxBytes = TrafficStats.getTotalRxBytes()
    startTxBytes = TrafficStats.getTotalTxBytes()

    if (startRxBytes == TrafficStats.UNSUPPORTED.toLong() || startTxBytes == TrafficStats.UNSUPPORTED.toLong()) {
      Log.e(TAG, "TrafficStats is not supported on this device.")
      _systemStatus.value = "TrafficStats not supported on this device."
      return
    }

    lastTimestampRxBytes = startRxBytes
    lastTimestampTxBytes = startTxBytes
    val startTime = System.currentTimeMillis()

    _liveStatus.value = LiveConnectionStatus(
      startTimeMillis = startTime,
      ssid = ssid,
      liveData = listOf(LiveDataPoint(startTime, DataUsage(0, 0)))
    )

    loggingJob = managerScope.launch {
      while (isActive) {
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

  private fun stopLogging() {
    loggingJob?.cancel()
    loggingJob = null
    val session = _liveStatus.value ?: return
    showToast("Stopping stats for: ${session.ssid}")

    val duration = System.currentTimeMillis() - session.startTimeMillis
    val totalRx = (TrafficStats.getTotalRxBytes() - startRxBytes).coerceAtLeast(0)
    val totalTx = (TrafficStats.getTotalTxBytes() - startTxBytes).coerceAtLeast(0)

    if (duration < 5000 && totalRx < 1024 && totalTx < 1024) {
      Log.d(TAG, "Trivial session discarded for SSID: ${session.ssid}")
      _liveStatus.value = null
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

    _liveStatus.value = null
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

  private fun showToast(message: String) {
    mainHandler.post {
      Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
  }
}
