package com.vinnovateit.autonetconnector.functionality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class WifiScanner(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        // A standard delay to wait for scan results to become available.
        private const val SCAN_RESULTS_DELAY_MS = 4000L
    }

    /**
     * Checks for all necessary permissions for scanning for Wi-Fi networks.
     * Includes ACCESS_WIFI_STATE for better compatibility.
     */
    fun hasPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val wifiState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        // Log the status of each permission for easier debugging.
        Log.d("PermissionsCheck", "ACCESS_FINE_LOCATION: $fineLocation, ACCESS_WIFI_STATE: $wifiState, NEARBY_WIFI_DEVICES: $nearbyWifi")

        return fineLocation && wifiState && nearbyWifi
    }

    /**
     * Initiates a WiFi scan with a more robust and simplified logic.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun scanWifiNetworks(onComplete: (List<WifiEntry>) -> Unit) {
        Log.d("WifiScan_Stable", "Checking permissions and WiFi state...")
        if (!hasPermission()) {
            Log.w("WifiScan_Stable", "One or more permissions are not granted.")
            Toast.makeText(context, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        if (!wifiManager.isWifiEnabled) {
            Log.w("WifiScan_Stable", "WiFi is disabled.")
            Toast.makeText(context, "WiFi is disabled.", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        performStableScan(onComplete)
    }

    /**
     * Performs the scan by initiating it and then polling for results after a delay.
     * This method is more resilient to Android's scan throttling.
     */
    private fun performStableScan(onComplete: (List<WifiEntry>) -> Unit) {
        Log.d("WifiScan_Stable", "Attempting to start a scan...")

        val scanStarted = wifiManager.startScan()

        if (!scanStarted) {
            // This is a common occurrence due to system throttling of scans.
            Log.w("WifiScan_Stable", "wifiManager.startScan() returned false. This is likely due to throttling.")
            Toast.makeText(context, "Scan throttled by system.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("WifiScan_Stable", "Scan initiated successfully. Waiting for results...")
        }

        // This unified path is simpler and more robust. We always wait for the delay
        // and then retrieve the latest available results from the system.
        mainHandler.postDelayed({
            Log.d("WifiScan_Stable", "Polling for scan results now.")
            try {
                // Permissions are checked before this function is called.
                @Suppress("MissingPermission")
                val results = wifiManager.scanResults
                Log.d("WifiScan_Stable", "Found ${results.size} results in the scan list.")

                // Provide helpful feedback if the list is empty.
                if (results.isEmpty()) {
                    Toast.makeText(context, "No networks found. Please ensure Location is enabled on your device.", Toast.LENGTH_LONG).show()
                }

                getVITWiFI.getVitWifiList(context, results, onComplete)
            } catch (e: Exception) {
                Log.e("WifiScan_Stable", "An exception occurred while getting scan results: ${e.message}")
                onComplete(emptyList())
            }
        }, SCAN_RESULTS_DELAY_MS)
    }

    /*
     * == TESTING NOTE ==
     * The autoConnectToPreferredWifi function remains disabled for testing.
     * You should connect to a Wi-Fi network manually through the device's
     * settings to allow the WifiStatsLogger service to begin recording data.
     */
}
