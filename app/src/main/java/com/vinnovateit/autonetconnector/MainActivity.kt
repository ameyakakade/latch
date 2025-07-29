package com.vinnovateit.autonetconnector

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality2.background.ForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.functionality2.manager.WiFiStatusViewModel
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner
    private lateinit var wifiStatusViewModel: WiFiStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiScanner = WifiScanner(this)
        requestLocationPermissionIfNeeded()
        WiFiMonitor.startMonitoring(applicationContext)

        // Start foreground service
        val serviceIntent = Intent(this, ForegroundService::class.java)
        startForegroundService(serviceIntent)

        // Init ViewModel
        wifiStatusViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return WiFiStatusViewModel(application) as T
                }
            }
        )[WiFiStatusViewModel::class.java]

        setContent {
            AutoNetConnectorTheme {
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModelFactory(application)
                )

                val isConnected by wifiStatusViewModel.isConnected.collectAsState()
                val ssid by wifiStatusViewModel.ssid.collectAsState()
                val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()
                val context = LocalContext.current

                // Calculate speed based on the most recent LIVE data point
                val currentSpeedBytesPerSecond = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
                val formattedSpeed = com.vinnovateit.autonetconnector.screen.stats.utils.formatBitsPerSecond(currentSpeedBytesPerSecond)

                // Update network speed string based on connection status
                val networkSpeedString = if (isConnected && liveStatus != null) {
                    "${formattedSpeed.first} ${formattedSpeed.second}"
                } else {
                    "0 B/s"
                }

                // Only show the session data (for the graph) if connected and logging
                val sessionForHomeScreen = if (isConnected && liveStatus != null) {
                    statsViewModel.sessionToShow.collectAsStateWithLifecycle().value
                } else {
                    null
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        isConnected = isConnected,
                        networkSpeed = networkSpeedString,
                        session = sessionForHomeScreen, // Pass the conditional session
                        ssid = ssid,
                        onConnectClick = {
                            wifiStatusViewModel.authenticatePortal()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 🔁 Refresh Wi-Fi status every time the activity comes to foreground
        wifiStatusViewModel.refreshStatus()
    }

    private fun requestLocationPermissionIfNeeded() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Location permission is required to detect WiFi network.", Toast.LENGTH_LONG).show()
            }
        }

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}