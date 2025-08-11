package com.vinnovateit.latch.features.wifi.background

import android.app.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.wifi.detector.VITWiFiIdentifier
import com.vinnovateit.latch.common.util.LoginTestRunner
import kotlinx.coroutines.*

class ForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
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

    private fun registerNetworkCallback() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
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