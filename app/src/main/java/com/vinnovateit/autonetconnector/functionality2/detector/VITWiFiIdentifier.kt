package com.vinnovateit.autonetconnector.functionality2.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

object VITWiFiIdentifier {

    fun getCurrentSSID(context: Context): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
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


    fun isConnectedToVITWiFi(context: Context): Boolean {
        val ssid = getCurrentSSID(context)
        Log.d("VITWiFiIdentifier", "Current SSID: $ssid")
        return ssid != null && ssid.lowercase().contains("vit")
    }
}
