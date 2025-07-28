package com.vinnovateit.autonetconnector.functionality2.background

import android.app.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality.WifiStatsManager
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
        startForeground(1, createNotification())
        // Initialize the manager and attempt to resume any session that was interrupted
        WifiStatsManager.initialize(application)
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ForegroundService", "Service destroyed")
        // When the service is destroyed, we ensure the current session is properly saved.
        WifiStatsManager.stopLogging()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationChannelId = "WIFI_LOGIN_CHANNEL"
        val channelName = "VIT Wi-Fi Auto Login"

        val chan = NotificationChannel(
            notificationChannelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("AutoNet Connector Running")
            .setContentText("Monitoring VIT Wi-Fi connection...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return

        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val currentSsid = (networkCapabilities.transportInfo as? WifiInfo)?.ssid?.replace("\"", "")

                if (hasInternet && currentSsid != null && WifiStatsManager.liveStatus.value == null) {
                    Log.d("ForegroundService", "Internet connection validated on $currentSsid. Starting stats.")
                    WifiStatsManager.startLogging(currentSsid)
                } else if (!hasInternet && WifiStatsManager.liveStatus.value != null) {
                    Log.d("ForegroundService", "Internet connection lost. Stopping stats.")
                    WifiStatsManager.stopLogging()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("ForegroundService", "Wi-Fi network lost completely. Stopping stats.")
                if (WifiStatsManager.liveStatus.value != null) {
                    WifiStatsManager.stopLogging()
                }
            }
        }
        cm.registerNetworkCallback(request, networkCallback!!)
    }
}