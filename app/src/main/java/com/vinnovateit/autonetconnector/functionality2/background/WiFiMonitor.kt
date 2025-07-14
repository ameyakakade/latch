package com.vinnovateit.autonetconnector.functionality2.background

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import kotlinx.coroutines.delay

object WiFiMonitor {
    private var isMonitoring = false

    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("WiFiMonitor", "Wi-Fi connected")

                CoroutineScope(Dispatchers.IO).launch {
                    repeat(3) { attempt ->
                        delay(2000) // wait for SSID to become available
                        if (VITWiFiIdentifier.isConnectedToVITWiFi(context)) {
                            Log.d("WiFiMonitor", "✅ VIT Wi-Fi detected. Running login.")
                            LoginTestRunner.run(context)
                            return@launch
                        } else {
                            Log.d("WiFiMonitor", "⏳ Attempt ${attempt + 1}: SSID not available or not VIT.")
                        }
                    }
                    Log.d("WiFiMonitor", "❌ Failed to detect VIT Wi-Fi after retries.")
                }
            }


            override fun onLost(network: Network) {
                Log.d("WiFiMonitor", "Wi-Fi lost")
            }
        })

    }
}
