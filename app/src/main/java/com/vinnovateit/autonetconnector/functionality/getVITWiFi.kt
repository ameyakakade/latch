package com.vinnovateit.autonetconnector.functionality

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
        }

        // Log all available SSIDs
        scanResults.forEach { scanResult ->
            Log.d("WiFiScanner", "SSID: '${scanResult.SSID}', Signal: ${scanResult.level} dBm")
        }

        // Filter all WiFi networks containing "VIT" (case-insensitive)
        val vitNetworks = scanResults.filter {
            !it.SSID.isNullOrEmpty() && it.SSID.contains("VIT", ignoreCase = true)
        }.distinctBy { it.SSID }
            .map { WifiEntry(it.SSID, it.level) }
            .sortedByDescending { it.level }

        if (vitNetworks.isNotEmpty()) {
            Log.d("VITWiFiScanner", "Found ${vitNetworks.size} VIT WiFi networks.")
            onResult(vitNetworks)
        } else {
            Log.d("VITWiFiScanner", "No VIT WiFi networks found.")
            Toast.makeText(context, "No VIT WiFi networks available", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
    }
}