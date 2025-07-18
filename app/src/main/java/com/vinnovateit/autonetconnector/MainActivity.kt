package com.vinnovateit.autonetconnector

import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality2.background.MyForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner
    private lateinit var connectivityReceiver: ConnectivityReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiScanner = WifiScanner(this)

        requestLocationPermissionIfNeeded()

        // Start monitoring WiFi immediately
        WiFiMonitor.startMonitoring(applicationContext)

        // Register connectivity change receiver
        connectivityReceiver = ConnectivityReceiver()
        registerReceiver(
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        // Start Foreground Service
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            AutoNetConnectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AutoLoginTestScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    )
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Location permission is required for WiFi scanning", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectivityReceiver)
    }
}

/**
 * Connectivity change listener.
 */
class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        val connectedType = when {
            networkCapabilities == null -> "No connection"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            else -> "Unknown"
        }

        Toast.makeText(context, "Connected to: $connectedType", Toast.LENGTH_SHORT).show()

        // Start or restart monitoring regardless of network type
        WiFiMonitor.startMonitoring(context.applicationContext)
    }
}

@Composable
fun AutoLoginTestScreen(onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = status, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            status = "Running auto-login test..."
            scope.launch {
                LoginTestRunner.run(context.applicationContext)
                status = "Test finished. Check logcat for output."
            }
        }) {
            Text("Run Auto-Login Test")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRequestPermission) {
            Text("Grant Location Permission")
        }
    }
}
