package com.vinnovateit.autonetconnector.funtionality

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiScanner(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

    fun scanWifiNetworks(onComplete: (List<ScanResult>) -> Unit) {
        if (!hasPermission()) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        try {
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        context?.unregisterReceiver(this)
                        val results = wifiManager.scanResults
                        onComplete(results)
                    } catch (e: SecurityException) {
                        Log.e("WifiScanner", "Permission error while reading results: ${e.message}")
                        onComplete(emptyList())
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)
            wifiManager.startScan()

        } catch (e: SecurityException) {
            Log.e("WifiScanner", "Scan blocked: ${e.message}")
            onComplete(emptyList())
        }
    }
}

@Composable
fun WifiScanScreen() {
    val context = LocalContext.current
    val wifiScanner = remember { WifiScanner(context) }
    var wifiDataJson by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            wifiScanner.scanWifiNetworks { results ->
                val data = results.map {
                    WifiEntry(ssid = it.SSID, level = it.level)
                }
                wifiDataJson = data.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
                    """  { "ssid": "${it.ssid}", "level": ${it.level} }"""
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (wifiScanner.hasPermission()) {
            wifiScanner.scanWifiNetworks { results ->
                val data = results.map {
                    WifiEntry(ssid = it.SSID, level = it.level)
                }
                wifiDataJson = data.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
                    """  { "ssid": "${it.ssid}", "level": ${it.level} }"""
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Scanned Wi-Fi List (as JSON)", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = wifiDataJson,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}