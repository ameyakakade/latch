package com.vinnovateit.autonetconnector.funtionality

import android.content.Context
import android.net.*
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

// Detects if the connected WiFi network has a captive portal (login page).
fun detectCaptivePortal(context: Context, onDetected: (Boolean) -> Unit) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Build a network request to listen for WiFi connectivity
    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    // Define a callback that triggers when a network becomes available or unavailable
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("CaptivePortal", "Network available: $network")

            // Bind the current process to this network (for HTTP requests)
            connectivityManager.bindProcessToNetwork(network)

            // Check if the network has been validated (i.e., internet access)
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

            if (!hasInternetCapability) {
                // If not validated, a captive portal is likely
                Log.d("CaptivePortal", "No VALIDATED capability — likely captive portal")
                checkGenerate204(onDetected)
            } else {
                // Even if validated, still check 204 URL for confirmation
                Log.d("CaptivePortal", "VALIDATED capability exists — checking 204 URL to be sure")
                checkGenerate204(onDetected)
            }

            // Unregister the callback to avoid memory leaks
            connectivityManager.unregisterNetworkCallback(this)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Log.d("CaptivePortal", "Network unavailable")
            onDetected(false) // No captive portal since network is not even available
        }
    }

    // Start listening for the WiFi network connection
    connectivityManager.registerNetworkCallback(request, callback)
}


/**
 * Makes an HTTP request to Google's generate_204 endpoint to detect captive portals.
 * If the response code is not 204, it's likely a captive portal.
 */
private fun checkGenerate204(onDetected: (Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("http://clients3.google.com/generate_204")
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.useCaches = false
            conn.connect()

            val responseCode = conn.responseCode
            Log.d("CaptivePortal", "HTTP response code: $responseCode")

            if (responseCode != 204) {
                Log.d("CaptivePortal", "Not 204 — Captive portal likely")
                withContext(Dispatchers.Main) {
                    onDetected(true)
                }
            } else {
                Log.d("CaptivePortal", "204 received — Internet is accessible")
                withContext(Dispatchers.Main) {
                    onDetected(false)
                }
            }

        } catch (e: Exception) {
            Log.e("CaptivePortal", "Error checking 204 URL: ${e.message}")
            withContext(Dispatchers.Main) {
                onDetected(true) // Assume captive portal if 204 check fails
            }
        }
    }
}
