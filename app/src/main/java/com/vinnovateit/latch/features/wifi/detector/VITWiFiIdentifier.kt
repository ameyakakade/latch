package com.vinnovateit.latch.features.wifi.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

object VITWiFiIdentifier {

    fun getCurrentSSID(context: Context, network: Network? = null): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val targetNetwork = network ?: connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(targetNetwork)

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ssid = wifiManager.connectionInfo.ssid
                Log.d("VITWiFiIdentifier", "Raw SSID: $ssid")

                if (ssid != null && ssid != "<unknown ssid>") {
                    ssid.replace("\"", "") // Remove quotes
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VITWiFiIdentifier", "Error getting SSID: ${e.message}")
            null
        }
    }


    fun isConnectedToVITWiFi(context: Context, network: Network? = null): Boolean {
        val ssid = getCurrentSSID(context, network)
        Log.d("VITWiFiIdentifier", "Current SSID: $ssid")
        return ssid != null && ssid.lowercase().contains("vit")
    }
}
