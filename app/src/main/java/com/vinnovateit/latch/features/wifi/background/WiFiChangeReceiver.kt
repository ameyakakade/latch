package com.vinnovateit.latch.features.wifi.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.util.Log

class WiFiChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("WiFiChangeReceiver", "Received broadcast: $action")

        if (ConnectivityManager.CONNECTIVITY_ACTION == action) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val isWifiConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                val networkInfo: NetworkInfo? = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                networkInfo?.isConnected == true
            }

            if (isWifiConnected) {
                Log.d("WiFiChangeReceiver", "Wi-Fi is connected. Delegating to WiFiMonitor.")
                WiFiMonitor.startMonitoring(context)
            } else {
                Log.d("WiFiChangeReceiver", "Wi-Fi not connected.")
            }
        }
    }
}
