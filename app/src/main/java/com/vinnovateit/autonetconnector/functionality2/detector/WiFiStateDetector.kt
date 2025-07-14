package com.vinnovateit.autonetconnector.functionality2.detector

import android.content.Context
import android.net.wifi.WifiManager

object WiFiStateDetector {

    /**
     * Checks if Wi-Fi is currently enabled on the device.
     *
     * @param context Application context
     * @return true if Wi-Fi is ON, false otherwise
     */
    fun isWiFiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled == true
    }
}
