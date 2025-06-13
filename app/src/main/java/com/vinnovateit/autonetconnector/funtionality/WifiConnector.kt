package com.vinnovateit.autonetconnector.funtionality

import android.content.Context
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
fun connectToWifi(
    context: Context,
    ssid: String,
    password: String,
    onConnected: () -> Unit,
    onFailed: () -> Unit
) {
    val wifiSpecifier = WifiNetworkSpecifier.Builder()
        .setSsid(ssid)
        .setWpa2Passphrase(password)
        .build()

    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .setNetworkSpecifier(wifiSpecifier)
        .build()

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("WiFiConnect", "Connected to $ssid")
            Toast.makeText(context, "Connected to $ssid", Toast.LENGTH_SHORT).show()
            onConnected()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Log.e("WiFiConnect", "Failed to connect to $ssid")
            Toast.makeText(context, "Failed to connect to $ssid", Toast.LENGTH_SHORT).show()
            onFailed()
        }
    }

    connectivityManager.requestNetwork(request, networkCallback)
}
