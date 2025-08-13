package com.vinnovateit.latch.features.wifi.background

import android.app.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vinnovateit.latch.R
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.wifi.detector.CaptivePortalDetector
import com.vinnovateit.latch.features.wifi.detector.WiFiStateDetector
import com.vinnovateit.latch.features.wifi.manager.AutoLoginManager
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatus
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatusManager
import com.vinnovateit.latch.features.wifi.manager.LoginResult
import kotlinx.coroutines.*
class ForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var connectivityManager: ConnectivityManager
    private var healthCheckJob: Job? = null

    companion object {
        const val ACTION_TRIGGER_LOGIN_CHECK = "com.vinnovateit.latch.ACTION_TRIGGER_LOGIN_CHECK"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        startForeground(1, createNotification())
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TRIGGER_LOGIN_CHECK) {
            Log.d("ForegroundService", "Manual login check triggered via intent.")
            ConnectionStatusManager.postStatus(ConnectionStatus.Connecting("Initializing..."))
            if (!WiFiStateDetector.isWiFiEnabled(this)) {
                Log.w("ForegroundService", "Wi-Fi is disabled, aborting manual check.")
                ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Wi-Fi is disabled"))
                return START_STICKY
            }
            connectivityManager.activeNetwork?.let { activeNetwork ->
                checkNetworkAndAct(activeNetwork)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ForegroundService", "Service destroyed")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationChannelId = "WIFI_LOGIN_CHANNEL"
        val channelName = "Latch Wi-Fi Service"

        val chan = NotificationChannel(
            notificationChannelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Latch Running")
            .setContentText("Monitoring Wi-Fi connection...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun checkNetworkAndAct(network: Network) {
        serviceScope.launch(Dispatchers.IO) {
            // If no internet, proceed directly to the login attempt which now also serves as verification.
            Log.d("ForegroundService", "No internet access. Attempting to authenticate portal.")
            handleCaptivePortal(network)
        }
    }

    private fun handleCaptivePortal(network: Network) {
        serviceScope.launch(Dispatchers.IO) {
            ConnectionStatusManager.postStatus(ConnectionStatus.Connecting("Authenticating..."))
            connectivityManager.bindProcessToNetwork(network)
            try {
                val user = StoredCredentials.getUserId(applicationContext)
                val pass = StoredCredentials.getPassword(applicationContext)
                if (user != null && pass != null) {
                    when (AutoLoginManager.attemptLogin(user, pass, network)) {
                        is LoginResult.Success -> {
                            Log.d("ForegroundService", "Login successful, re-validating network.")
                            checkNetworkAndAct(network)
                        }
                        is LoginResult.UnsupportedNetwork -> {
                            ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Unsupported Network"))
                        }
                        is LoginResult.Failure -> {
                            ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Login Failed"))
                        }
                    }
                } else {
                    ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Credentials not set"))
                }
            } finally {
                connectivityManager.bindProcessToNetwork(null)
            }
        }
    }

    private fun startHealthCheck(network: Network) {
        healthCheckJob?.cancel()
        healthCheckJob = serviceScope.launch {
            while (isActive) {
                delay(60_000) // Check every 60 seconds
                Log.d("ForegroundService", "Performing periodic health check...")
                val status = CaptivePortalDetector.checkPortalStatus(applicationContext, network)
                if (status != 204) {
                    Log.w("ForegroundService", "Health check failed (status: $status). Session may have expired. Triggering re-login.")
                    checkNetworkAndAct(network) // Re-run the full check/login process
                    // No need to delay here, checkNetworkAndAct has its own async logic
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
                if (!WiFiStateDetector.isWiFiEnabled(this@ForegroundService)) {
                    return
                }
                checkNetworkAndAct(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("ForegroundService", "Network lost: $network")
                healthCheckJob?.cancel() // Stop checking when network is lost
                SessionRepository.stopSession()
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
}