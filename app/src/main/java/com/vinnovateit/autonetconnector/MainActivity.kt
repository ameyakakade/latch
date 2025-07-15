package com.vinnovateit.autonetconnector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality2.background.MyForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.funtionality.*
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiScanner = WifiScanner(this)

        requestLocationPermissionIfNeeded()
        WiFiMonitor.startMonitoring(applicationContext)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Location permission is required for WiFi scanning", Toast.LENGTH_LONG).show()
            }
        }

        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            AutoNetConnectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    // 🔒️ Commented out teammate's tab-based UI (preserved for later)
                    /*
                    var selectedTab by remember { mutableStateOf(0) }
                    val tabs = listOf("WiFi Scanner", "Credentials Debug")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> WifiScannerScreen(
                                    onRequestPermission = {
                                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    onScanRequest = { callback ->
                                        wifiScanner.scanWifiNetworks(callback)
                                    },
                                    wifiScanner = wifiScanner,
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> CredentialsScreen(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    */

                    // ✅ FINAL UI: HomeScreen with AutoLogin Button
                    HomeScreen(
                        isConnected = false,
                        networkName = "Vit S-block 2.4",
                        networkSpeed = "6 mbps"
                    )
                }
            }
        }
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
