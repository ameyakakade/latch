package com.vinnovateit.autonetconnector.functionality

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class WifiScanner(private val context: Context) {

    // Accessing the system WiFi service
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    companion object {
        const val REQUEST_CODE_LOCATION = 123
    }

    // Check if required permissions are granted
    fun hasPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fineLocation && nearbyWifi
    }

//    Begin scanning WiFi networks and handle result through callback
    @RequiresApi(Build.VERSION_CODES.Q)
    fun scanWifiNetworks(onComplete: (List<WifiEntry>) -> Unit) {
        Log.d("WifiScan", "Starting scan...")
        Log.d("WifiScan", "WiFi enabled: ${wifiManager.isWifiEnabled}")

        if (!hasPermission()) {
            Log.d("WifiScan", "Permission not granted")
            Toast.makeText(context, "Location/WiFi permission not granted", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        // ✅ NEW: Location Services (GPS/Network) Check
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            Log.d("WifiScan", "Location Services are OFF")
            Toast.makeText(context, "Please turn ON Location Services (GPS) to scan WiFi", Toast.LENGTH_LONG).show()
            onComplete(emptyList())
            return
        }

        try {
            if (!wifiManager.isWifiEnabled) {
                Log.d("WifiScan", "WiFi was disabled, enabling...")
                wifiManager.isWifiEnabled = true
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    performScan(onComplete)
                }, 2000)
            } else {
                performScan(onComplete)
            }

        } catch (e: SecurityException) {
            Log.e("WifiScanner", "Scan blocked: ${e.message}")
            onComplete(emptyList())
        }
    }

    // Internal method to perform WiFi scan and handle results
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun performScan(onComplete: (List<WifiEntry>) -> Unit) {
        var receiverUnregistered = false

        // Check if cached results are already available
        @Suppress("MissingPermission")
        val cachedResults = wifiManager.scanResults
        if (cachedResults.isNotEmpty()) {
            Log.d("WifiScan", "Using cached WiFi results (count: ${cachedResults.size})")
            getVITWiFI.getVitWifiList(context, cachedResults) { filteredList ->
                autoConnectToPreferredWifi(filteredList)
                onComplete(filteredList)
            }
            return //  Do not continue to scan
        }

        // Scan if no cached results
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (receiverUnregistered) return

                    try {
                        context?.unregisterReceiver(this)
                        receiverUnregistered = true

                        @Suppress("MissingPermission")
                        val results = wifiManager.scanResults
                        Log.d("WifiScan", "Scan results count: ${results.size}")
                        results.forEach { result ->
                            Log.d("WifiScan", "Found: ${result.SSID} (${result.level})")
                        }

                        getVITWiFI.getVitWifiList(this@WifiScanner.context, results) { filteredList ->
                            Log.d("WifiScan", "Filtered VIT WiFi count: ${filteredList.size}")
                            autoConnectToPreferredWifi(filteredList)
                            onComplete(filteredList)
                        }

                    } catch (e: Exception) {
                        Log.e("WifiScanner", "Scan receive error: ${e.message}")
                        onComplete(emptyList())
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)

            //  Timeout fallback
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!receiverUnregistered) {
                    try {
                        context.unregisterReceiver(receiver)
                        receiverUnregistered = true
                        Log.d("WifiScan", "Scan timeout, using cached results")

                        @Suppress("MissingPermission")
                        val results = wifiManager.scanResults

                        getVITWiFI.getVitWifiList(context, results) { filteredList ->
                            autoConnectToPreferredWifi(filteredList)
                            onComplete(filteredList)
                        }

                    } catch (e: Exception) {
                        Log.e("WifiScan", "Timeout error: ${e.message}")
                        onComplete(emptyList())
                    }
                }
            }, 5000)

            val scanStarted = wifiManager.startScan()
            Log.d("WifiScan", "Scan started: $scanStarted")

            if (!scanStarted) {
                context.unregisterReceiver(receiver)
                receiverUnregistered = true
                Log.w("WifiScan", "Scan failed, using cached results")

                @Suppress("MissingPermission")
                val results = wifiManager.scanResults

                getVITWiFI.getVitWifiList(context, results) { filteredList ->
                    autoConnectToPreferredWifi(filteredList)
                    onComplete(filteredList)
                }
            }

        } catch (e: Exception) {
            Log.e("WifiScanner", "Unexpected error: ${e.message}")
            onComplete(emptyList())
        }
    }


    //    connects temporarily for captive portal login
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun autoConnectToPreferredWifi(filteredList: List<WifiEntry>) {
        val credentials = getUserCredentials(context)

        // Default SSID (can make it configurable later)
        val defaultWifiName = "D-ANX-VIT"

        if (credentials != null) {
            val matched = filteredList.find {
                it.ssid.equals(defaultWifiName, ignoreCase = true)
            }

            if (matched != null) {
                connectToWifi(
                    context = context,
                    ssid = defaultWifiName,
                    password = credentials.password, // use password from saved credentials
                    onConnected = {
                        Log.d("WifiScanner", "✅ Connected to $defaultWifiName")
                        detectCaptivePortal(context) { isCaptive ->
                            if (isCaptive) {
                                Log.d("WifiScanner", "Captive portal detected — trigger login")
                                // TODO: Login logic here
                            } else {
                                Log.d("WifiScanner", "Full internet access")
                            }
                        }
                    },
                    onFailed = {
                        Log.e("WifiScanner", "❌ Failed to connect to $defaultWifiName")
                    }
                )
            } else {
                Log.w("WifiScanner", "No matching WiFi SSID ($defaultWifiName) found in scan")
            }
        } else {
            Log.w("WifiScanner", "User credentials not found in cache")
        }
    }
}
