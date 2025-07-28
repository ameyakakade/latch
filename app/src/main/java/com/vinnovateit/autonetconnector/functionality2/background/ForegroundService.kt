package com.vinnovateit.autonetconnector.functionality2.background

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
        startForeground(1, createNotification())
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        val channelName = "VIT Wi-Fi Auto Login"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                notificationChannelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("AutoNet Connector Running")
            .setContentText("Monitoring VIT Wi-Fi connection...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("WiFiMonitor", "Wi-Fi connected")

                serviceScope.launch {
                    if (VITWiFiIdentifier.isConnectedToVITWiFi(applicationContext, network)) {
                        Log.d("WiFiMonitor", "✅ VIT Wi-Fi detected. Running login.")
                        LoginTestRunner.run(applicationContext)
                    } else {
                        Log.d("WiFiMonitor", "❌ Not VIT Wi-Fi detected.")
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d("WiFiMonitor", "Wi-Fi lost")
            }
        })
    }
}
