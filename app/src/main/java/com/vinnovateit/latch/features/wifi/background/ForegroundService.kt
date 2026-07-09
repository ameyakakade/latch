package com.vinnovateit.latch.features.wifi.background

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vinnovateit.latch.R
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.wifi.detector.CaptivePortalDetector
import com.vinnovateit.latch.features.wifi.detector.WiFiStateDetector
import com.vinnovateit.latch.features.wifi.manager.AutoLoginManager
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatus
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatusManager
import com.vinnovateit.latch.features.wifi.manager.LoginResult
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var connectivityManager: ConnectivityManager
    private var healthCheckJob: Job? = null
    private val notificationId = 1
    private val channelId = "WIFI_LOGIN_CHANNEL"

    private var currentWifiNetwork: Network? = null

    companion object {
        const val ACTION_TRIGGER_LOGIN_CHECK = "com.vinnovateit.latch.ACTION_TRIGGER_LOGIN_CHECK"
        const val ACTION_TRIGGER_LOGOUT = "com.vinnovateit.latch.ACTION_TRIGGER_LOGOUT"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("ForegroundService", "Service created")
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        SettingsManager.initialize(applicationContext)

        try {
            val notification = createNotificationBuilder("Latch is Running", getString(R.string.notification_text)).build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e("ForegroundService", "Foreground service start not allowed. System time limit exhausted.", e)
            stopSelf()
            return
        }

        serviceScope.launch {
            delay(5L * 60L * 60L * 1000L + 45L * 60L * 1000L) // 5 hours 45 mins
            Log.w("ForegroundService", "Proactively stopping service to avoid OS FGS time limit.")
            stopSelf()
        }

        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_TRIGGER_LOGIN_CHECK -> {
                Log.d("ForegroundService", "Manual login check triggered via intent.")
                ConnectionStatusManager.postStatus(
                    ConnectionStatus.Companion.Connecting(getString(R.string.status_initializing))
                )

                if (!WiFiStateDetector.isWiFiEnabled(this)) {
                    Log.w("ForegroundService", "Wi-Fi is disabled, aborting manual check.")
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_wifi_off)))
                    return START_STICKY
                }

                val targetWifiNetwork = currentWifiNetwork

                if (targetWifiNetwork != null) {
                    checkNetworkAndAct(targetWifiNetwork, false)
                } else {
                    Log.w("ForegroundService", "Could not find a connected Wi-Fi network. Aborting check.")
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_not_on_wifi)))
                }
            }
            ACTION_TRIGGER_LOGOUT -> {
                Log.d("ForegroundService", "Manual logout triggered via intent.")
                serviceScope.launch(Dispatchers.IO) { logoutAndStop() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ForegroundService", "Service destroyed")
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationBuilder(title: String, text: String): NotificationCompat.Builder {
        val channelName = getString(R.string.notification_channel_name)

        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_latch)
            .setOnlyAlertOnce(false)
    }

    private fun updateNotification(title: String, text: String) {
        val notification = createNotificationBuilder(title, text).build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    // Note: I added a default parameter `retryCount: Int = 0` to the function signature
    private fun checkNetworkAndAct(network: Network, isRevalidating: Boolean, retryCount: Int = 0) {
        serviceScope.launch(Dispatchers.IO) {
            ConnectionStatusManager.postStatus(
                ConnectionStatus.Companion.Connecting(getString(R.string.status_checking_internet))
            )

            val bypassPortal = SettingsManager.bypassPortalCheck.value

            if (bypassPortal && !isRevalidating) {
                Log.d("ForegroundService", "Bypass enabled: Skipping portal validation, proceeding directly to login.")
                handleCaptivePortal(network)
                return@launch
            }

            val internetStatus = CaptivePortalDetector.checkPortalStatus(applicationContext, network)

            if (internetStatus == 204) {
                // FIX #1: If we just successfully logged in (isRevalidating), we KNOW it's the target network.
                // No need to check isTargetCaptivePortal again, which fails when the internet is already open!
                val isTarget = isRevalidating || AutoLoginManager.isTargetCaptivePortal(network)

                if (isTarget) {
                    Log.d("ForegroundService", "Valid WiFi with internet. Starting session.")
                    connectivityManager.reportNetworkConnectivity(network, true)
                    ConnectionStatusManager.postStatus(ConnectionStatus.Success)
                    SessionRepository.startSession(network)

                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val timeString = timeFormat.format(Date())
                    updateNotification("Latched", "Connected at $timeString")

                    startHealthCheck(network)
                } else {
                    Log.d("ForegroundService", "Non-target WiFi with internet. Ignoring.")
                    ConnectionStatusManager.postStatus(
                        ConnectionStatus.Failed(getString(R.string.status_unsupported_network))
                    )
                }
                return@launch
            }

            // --- IF INTERNET IS NOT 204 YET ---

            // FIX #2: If we just logged in, give the portal 2 seconds to open the gates instead of infinite looping.
            if (isRevalidating) {
                if (retryCount < 3) {
                    Log.d("ForegroundService", "Waiting for network gates to open... (Attempt ${retryCount + 1})")
                    delay(2000) // Wait 2 seconds
                    checkNetworkAndAct(network, true, retryCount + 1)
                } else {
                    Log.w("ForegroundService", "Network never granted internet after successful login.")
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Network timeout after login"))
                }
                return@launch
            }

            ConnectionStatusManager.postStatus(
                ConnectionStatus.Companion.Connecting(getString(R.string.status_verifying_network))
            )

            if (AutoLoginManager.isTargetCaptivePortal(network)) {
                Log.d("ForegroundService", "Target captive portal confirmed via HTTP ping.")
                handleCaptivePortal(network)
            } else {
                Log.d("ForegroundService", "Non-target captive portal. Ignoring.")
                ConnectionStatusManager.postStatus(ConnectionStatus.Failed(
                    getString(R.string.status_unsupported_network)
                ))
            }
        }
    }

    private fun handleCaptivePortal(network: Network) {
        serviceScope.launch(Dispatchers.IO) {
            ConnectionStatusManager.postStatus(
                ConnectionStatus.Companion.Connecting(getString(R.string.status_authenticating))
            )
            connectivityManager.bindProcessToNetwork(network)
            try {
                val user = StoredCredentials.getUserId(applicationContext)
                val pass = StoredCredentials.getPassword(applicationContext)
                if (user != null && pass != null) {
                    when (AutoLoginManager.attemptLogin(user, pass, network)) {
                        is LoginResult.Success -> {
                            Log.d("ForegroundService", "Login successful, re-validating network.")
                            checkNetworkAndAct(network, true)
                        }
                        is LoginResult.UnsupportedNetwork -> {
                            ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_unsupported_network)))
                        }
                        is LoginResult.Failure -> {
                            ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_login_failed)))
                        }
                    }
                } else {
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_login_failed)))
                }
            } finally {
                connectivityManager.bindProcessToNetwork(null)
            }
        }
    }

    private fun logoutAndStop() {
        ConnectionStatusManager.postStatus(
            ConnectionStatus.Companion.Connecting(getString(R.string.status_logging_out))
        )
        healthCheckJob?.cancel()

        val network = currentWifiNetwork ?: connectivityManager.activeNetwork

        if (network != null) {
            connectivityManager.bindProcessToNetwork(network)
            try {
                val ok = AutoLoginManager.attemptLogout()
                if (ok) {
                    Log.d("ForegroundService", "Logout success.")
                    SessionRepository.stopSession()
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_disconnected_message)))
                } else {
                    Log.w("ForegroundService", "Logout failed.")
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_logout_failed)))
                }
            } finally {
                connectivityManager.bindProcessToNetwork(null)
            }
        } else {
            Log.w("ForegroundService", "No active network during logout.")
            SessionRepository.stopSession()
            ConnectionStatusManager.postStatus(ConnectionStatus.Failed(getString(R.string.status_disconnected_message)))
        }

        stopSelf()
    }

    private fun startHealthCheck(network: Network) {
        healthCheckJob?.cancel()
        healthCheckJob = serviceScope.launch {
            while (isActive) {
                delay(60_000)
                Log.d("ForegroundService", "Performing periodic health check...")
                val status = CaptivePortalDetector.checkPortalStatus(applicationContext, network)
                if (status != 204) {
                    Log.w("ForegroundService", "Health check failed (status: $status). Session may have expired. Triggering re-login.")
                    checkNetworkAndAct(network, false)
                } else {
                    Log.d("ForegroundService", "Health check passed.")
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                currentWifiNetwork = network

                if (!WiFiStateDetector.isWiFiEnabled(this@ForegroundService)) {
                    return
                }
                checkNetworkAndAct(network, false)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (currentWifiNetwork == network) {
                    currentWifiNetwork = null
                }

                Log.d("ForegroundService", "Network lost: $network")
                healthCheckJob?.cancel()
                SessionRepository.stopSession()
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
}