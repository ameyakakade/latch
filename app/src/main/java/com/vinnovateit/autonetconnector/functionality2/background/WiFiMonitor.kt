package com.vinnovateit.autonetconnector.functionality2.background

import android.content.Context
import android.net.*
import android.util.Log
import kotlinx.coroutines.*
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner

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
                Log.d("WiFiMonitor", "✅ Wi-Fi connected")

                CoroutineScope(Dispatchers.IO).launch {
                    // Bind process to Wi-Fi even if mobile data is active
                    cm.bindProcessToNetwork(network)

                    // Retry to detect VIT Wi-Fi
                    repeat(3) { attempt ->
                        delay(2000)
                        if (VITWiFiIdentifier.isConnectedToVITWiFi(context)) {
                            Log.d("WiFiMonitor", "✅ VIT Wi-Fi detected. Running login.")
                            LoginTestRunner.run(context)
                            return@launch
                        } else {
                            Log.d("WiFiMonitor", "⏳ Attempt ${attempt + 1}: SSID not VIT.")
                        }
                    }
                    Log.d("WiFiMonitor", "❌ Failed to detect VIT Wi-Fi after retries.")
                }
            }

            // Removed onLost override and scan logic
        })
    }
}
