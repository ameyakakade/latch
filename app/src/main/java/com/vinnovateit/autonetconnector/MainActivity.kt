package com.vinnovateit.autonetconnector

import android.app.Application
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality2.background.MyForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import com.vinnovateit.autonetconnector.functionality.*
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import kotlinx.coroutines.delay

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
                val viewModel: StatsViewModel = viewModel(
                    factory = StatsViewModelFactory(LocalContext.current.applicationContext as Application)
                )
                val sessionToShow by viewModel.sessionToShow.collectAsStateWithLifecycle()
                val context = LocalContext.current

                // FIX: This effect ensures that if no real data is available after 2 seconds,
                // the app will show a mock graph instead of a "no data" message.
                LaunchedEffect(sessionToShow) {
                    if (sessionToShow == null) {
                        delay(2000) // Wait for 2 seconds for real data
                        if (sessionToShow == null) { // Check again
                            viewModel.onToggleMockData(true)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        isConnected = false, // This should be made dynamic
                        networkName = "Vit S-block 2.4", // This should be made dynamic
                        networkSpeed = "6 mbps", // This should be made dynamic
                        onSpectrumClick = {
                            context.startActivity(Intent(context, StatsActivity::class.java))
                        },
                        session = sessionToShow
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, SecondPageActivity::class.java)
                                intent.putExtra("editMode", true)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp)
                        ) {
                            Text("Change Credentials")
                        }
                    }
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
