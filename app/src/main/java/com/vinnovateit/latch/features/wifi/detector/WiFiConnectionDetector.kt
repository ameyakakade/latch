package com.vinnovateit.latch.features.wifi.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object WiFiConnectionDetector {

    /**
     * Checks if the device is currently connected to any Wi-Fi network.
     *
     * @param context Application context
     * @return true if connected to a Wi-Fi network, false otherwise
     */
    fun isConnectedToWiFi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)

        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
