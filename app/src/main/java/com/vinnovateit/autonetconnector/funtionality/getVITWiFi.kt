package com.vinnovateit.autonetconnector.funtionality

import android.content.Context
import android.net.wifi.ScanResult
import android.util.Log
import android.widget.Toast

object getVITWiFI {

    fun getVitWifiList(
        context: Context,
        scanResults: List<ScanResult>,
        onResult: (List<WifiEntry>) -> Unit
    ) {
        Log.d("WiFiScanner", "=== All Available Networks ===")

        if (scanResults.isEmpty()) {
            Log.d("WiFiScanner", "No scan results received!")
            Toast.makeText(context, "No WiFi networks found", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
            return
        } else {
            scanResults.forEach { scanResult ->
                Log.d("WiFiScanner", "SSID: '${scanResult.SSID}', Signal: ${scanResult.level} dBm")
            }
        }

        val vitNetworks = mutableListOf<WifiEntry>()

        // Get hostel WiFi name from cache
        val preferredHostelSSID = getPreferredHostelSSIDFromCache(context)

        // If hostel name is cached, try to find it in scan results and prioritize
        if (!preferredHostelSSID.isNullOrEmpty()) {
            val preferredNetwork = scanResults.find {
                !it.SSID.isNullOrEmpty() && it.SSID.equals(preferredHostelSSID, ignoreCase = true)
            }

            if (preferredNetwork != null) {
                vitNetworks.add(WifiEntry(preferredNetwork.SSID, preferredNetwork.level))
                Log.d("VITWiFiScanner", "Found preferred hostel WiFi: ${preferredNetwork.SSID} with signal: ${preferredNetwork.level} dBm")
                onResult(vitNetworks.sortedByDescending { it.level })
                return
            } else {
                Log.d("VITWiFiScanner", "Preferred hostel WiFi ($preferredHostelSSID) not found in scan.")
            }
        } else {
            Log.d("VITWiFiScanner", "No preferred hostel WiFi cached.")
        }

        // If not found, find other VIT networks (starting with "VIT")
        val otherVitNetworks = scanResults.filter {
            !it.SSID.isNullOrEmpty() && it.SSID.startsWith("VIT", ignoreCase = true)
        }.distinctBy { it.SSID }

        val bestVitAlternative = otherVitNetworks.maxByOrNull { it.level }

        if (bestVitAlternative != null) {
            Toast.makeText(
                context,
                "Preferred WiFi not found. Best available VIT network: ${bestVitAlternative.SSID}",
                Toast.LENGTH_SHORT
            ).show()

            Log.d(
                "VITWiFiScanner",
                "Fallback to best VIT: ${bestVitAlternative.SSID} (${bestVitAlternative.level} dBm)"
            )

            onResult(listOf(WifiEntry(bestVitAlternative.SSID, bestVitAlternative.level)))
        } else {
            Toast.makeText(context, "No VIT WiFi networks available", Toast.LENGTH_SHORT).show()
            Log.d("VITWiFiScanner", "No VIT WiFi networks found at all.")
            onResult(emptyList())
        }
    }

    /**
     * Helper function to get preferred hostel SSID from cache.
     * Replace this with your actual implementation of fetching preferred hostel WiFi from cache.
     * this is a sample part
     * to be perfected
     */
    fun getPreferredHostelSSIDFromCache(context: Context): String? {
        // For example, you can reuse your existing credentials cache or
        // create a new SharedPreferences key for preferred hostel SSID.

        // Example implementation using SharedPreferences (replace as needed):
        val prefs = context.getSharedPreferences("hostel_wifi_cache", Context.MODE_PRIVATE)
        return prefs.getString("preferred_hostel_ssid", null)
    }
}
