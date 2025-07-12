package com.vinnovateit.autonetconnector.functionality

import android.content.Context
import android.net.wifi.ScanResult
import android.util.Log
import android.widget.Toast

object getVITWiFI {

    /**
     * MODIFIED FOR TESTING: This function now returns a list of all available WiFi networks
     * found during a scan, instead of specifically filtering for "VIT" networks.
     * This is to allow the stats logger and UI to be tested with any connection.
     */
    fun getVitWifiList(
        context: Context,
        scanResults: List<ScanResult>,
        onResult: (List<WifiEntry>) -> Unit
    ) {
        Log.d("WiFiScanner_TestMode", "Returning all scanned networks for testing.")

        if (scanResults.isEmpty()) {
            Log.w("WiFiScanner_TestMode", "No scan results were received.")
            Toast.makeText(context, "No WiFi networks found", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
            return
        }

        // Convert all valid scan results to our WifiEntry data class.
        // This logic replaces the original VIT-specific filtering.
        val allAvailableNetworks = scanResults
            .filter { !it.SSID.isNullOrEmpty() } // Ignore networks without an SSID
            .map { WifiEntry(it.SSID, it.level) } // Convert to our custom type
            .distinctBy { it.ssid } // Ensure the list has unique network names
            .sortedByDescending { it.level } // Show the strongest networks first

        Log.d("WiFiScanner_TestMode", "Found ${allAvailableNetworks.size} unique networks.")
        onResult(allAvailableNetworks)
    }


    fun getPreferredHostelSSIDFromCache(context: Context): String? {
        val prefs = context.getSharedPreferences("user_credentials_cache", Context.MODE_PRIVATE)
        return prefs.getString("wifiName", null)
    }
}