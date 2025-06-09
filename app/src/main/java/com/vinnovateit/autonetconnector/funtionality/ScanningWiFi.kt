package com.vinnovateit.autonetconnector.funtionality

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiScanner(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    companion object {
        const val REQUEST_CODE_LOCATION = 123
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION
        )
    }

    fun scanWifiNetworks(onComplete: (List<WifiEntry>) -> Unit) {
        // ADD DEBUGGING LOGS HERE
        Log.d("WifiScan", "Starting scan...")
        Log.d("WifiScan", "WiFi enabled: ${wifiManager.isWifiEnabled}")

        if (!hasPermission()) {
            Log.d("WifiScan", "Permission not granted")
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        try {
            // CHECK WIFI STATE BEFORE SCANNING
            if (!wifiManager.isWifiEnabled) {
                Log.d("WifiScan", "WiFi was disabled, enabling...")
                wifiManager.isWifiEnabled = true
                // ADD DELAY AFTER ENABLING WIFI
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    performScan(onComplete)
                }, 2000) // Wait 2 seconds for WiFi to enable
            } else {
                Log.d("WifiScan", "WiFi already enabled")
                performScan(onComplete)
            }

        } catch (e: SecurityException) {
            Log.e("WifiScanner", "Scan blocked: ${e.message}")
            onComplete(emptyList())
        }
    }

    // SEPARATE FUNCTION FOR ACTUAL SCANNING
    private fun performScan(onComplete: (List<WifiEntry>) -> Unit) {
        var receiverUnregistered = false

        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (receiverUnregistered) return

                    try {
                        context?.unregisterReceiver(this)
                        receiverUnregistered = true

                        val results = wifiManager.scanResults
                        Log.d("WifiScan", "Scan results count: ${results.size}")
                        results.forEach { result ->
                            Log.d("WifiScan", "Found: ${result.SSID} (${result.level})")
                        }

                        // Pass the ScanResult list to getVitWifiList, which returns WifiEntry list
                        getVITWiFI.getVitWifiList(this@WifiScanner.context, results) { filteredList ->
                            Log.d("WifiScan", "Filtered VIT WiFi count: ${filteredList.size}")
                            onComplete(filteredList)
                        }

                    } catch (e: SecurityException) {
                        Log.e("WifiScanner", "Permission error while reading results: ${e.message}")
                        onComplete(emptyList())
                    } catch (e: Exception) {
                        Log.e("WifiScanner", "Error reading results: ${e.message}")
                        onComplete(emptyList())
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)

            // ADD TIMEOUT PROTECTION
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!receiverUnregistered) {
                    try {
                        context.unregisterReceiver(receiver)
                        receiverUnregistered = true
                        Log.d("WifiScan", "Scan timeout, using cached results")

                        val results = wifiManager.scanResults
                        Log.d("WifiScan", "Cached results count: ${results.size}")

                        // Use the same filtering logic for timeout case
                        getVITWiFI.getVitWifiList(context, results) { filteredList ->
                            Log.d("WifiScan", "Timeout - Filtered VIT WiFi count: ${filteredList.size}")
                            onComplete(filteredList)
                        }

                    } catch (e: Exception) {
                        Log.e("WifiScan", "Timeout error: ${e.message}")
                        onComplete(emptyList())
                    }
                }
            }, 5000) // 5 second timeout

            val scanStarted = wifiManager.startScan()
            Log.d("WifiScan", "Scan started: $scanStarted")

            if (!scanStarted) {
                Log.w("WifiScan", "Failed to start scan, using cached results")
                // If scan fails to start, try to use cached results immediately
                try {
                    context.unregisterReceiver(receiver)
                    receiverUnregistered = true
                    val results = wifiManager.scanResults
                    getVITWiFI.getVitWifiList(context, results) { filteredList ->
                        onComplete(filteredList)
                    }
                } catch (e: Exception) {
                    Log.e("WifiScan", "Error getting cached results: ${e.message}")
                    onComplete(emptyList())
                }
            }

        } catch (e: SecurityException) {
            Log.e("WifiScanner", "Scan blocked: ${e.message}")
            onComplete(emptyList())
        } catch (e: Exception) {
            Log.e("WifiScanner", "Unexpected error: ${e.message}")
            onComplete(emptyList())
        }
    }
}