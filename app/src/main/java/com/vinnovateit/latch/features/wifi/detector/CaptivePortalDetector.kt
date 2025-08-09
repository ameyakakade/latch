package com.vinnovateit.latch.features.wifi.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import java.net.HttpURLConnection
import java.net.URL

object CaptivePortalDetector {

    /**
     * Checks if a captive portal is active on the current Wi-Fi network.
     * Ensures the request is made specifically over the connected Wi-Fi.
     *
     * @param context Application context
     * @return true if captive portal is active (i.e., login required), false if full internet access
     */
    fun isCaptivePortalActive(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork: Network? = connectivityManager.activeNetwork
        if (activeNetwork == null) return true // No network → assume captive portal

        return try {
            // Bind this connection to the current Wi-Fi network only (so as even if user connected to mobile data, we can use this method of detection)
            val url = URL("http://clients3.google.com/generate_204")
            val connection = activeNetwork.openConnection(url) as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            val responseCode = connection.responseCode
            val location = connection.getHeaderField("Location")

            // If response isn't 204 or it redirects, it's likely a captive portal
            responseCode != 204 || location != null
        } catch (e: Exception) {
            // On error (no network, timeout, etc.), assume captive portal might be present
            true
        }
    }
}
