package com.vinnovateit.latch.features.wifi.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build

object WiFiConnectionDetector {

    /**
     * Checks if the device is currently connected to any Wi-Fi network.
     * Uses a combination of approaches to ensure compatibility across different Android skins and versions.
     *
     * @param context Application context
     * @return true if connected to a Wi-Fi network, false otherwise
     */
    fun isConnectedToWiFi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        // 1. Check active network capabilities (Modern API - Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Ignore and fall through
            }
        }

        // 2. Iterate all networks (Modern API - sometimes activeNetwork is cellular but WiFi is connected without internet)
        try {
            val networks = connectivityManager.allNetworks
            for (network in networks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore and fall through
        }

        // 3. Fallback to deprecated NetworkInfo API (More reliable on some older devices and custom skins)
        try {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            if (activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected) {
                return true
            }

            @Suppress("DEPRECATION")
            val wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            @Suppress("DEPRECATION")
            if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected) {
                return true
            }
        } catch (e: Exception) {
            // Ignore and fall through
        }

        // 4. Fallback to WifiManager (Check supplicant state as last resort)
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null && wifiManager.isWifiEnabled) {
                val wifiInfo = wifiManager.connectionInfo
                if (wifiInfo != null && wifiInfo.networkId != -1) {
                    if (wifiInfo.supplicantState == android.net.wifi.SupplicantState.COMPLETED) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }
}