package com.vinnovateit.autonetconnector.functionality2.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier

class WiFiChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("WiFiChangeReceiver", "Received broadcast: $action")

        if (ConnectivityManager.CONNECTIVITY_ACTION == action) {
            val currentSSID = VITWiFiIdentifier.getCurrentSSID(context)
            Log.d("WiFiChangeReceiver", "Detected SSID: $currentSSID")

            if (currentSSID != null && currentSSID.lowercase().contains("vit")) {
                Log.d("WiFiChangeReceiver", "Connected to VIT Wi-Fi: $currentSSID. Scheduling login...")
                AutoLoginScheduler.scheduleAutoLogin(context)
            } else {
                Log.d("WiFiChangeReceiver", "Not a VIT Wi-Fi. Skipping login.")
            }
        }
    }
}
