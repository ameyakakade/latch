package com.vinnovateit.autonetconnector

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality2.background.MyForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.functionality2.manager.WiFiStatusViewModel
import com.vinnovateit.autonetconnector.HomeScreen
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner
    private lateinit var wifiStatusViewModel: WiFiStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiScanner = WifiScanner(this)
        requestLocationPermissionIfNeeded()
        WiFiMonitor.startMonitoring(applicationContext)

        // Start foreground service
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Init ViewModel
        wifiStatusViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WiFiStatusViewModel(application) as T
                }
            }
        )[WiFiStatusViewModel::class.java]

        setContent {
            AutoNetConnectorTheme {
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModelFactory(application)
                )
                val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()

                val isConnected by wifiStatusViewModel.isConnected.collectAsState()
                val ssid by wifiStatusViewModel.ssid.collectAsState()

                val context = LocalContext.current

                Surface(modifier = Modifier.fillMaxSize()) {
                    val currentSpeedBytesPerSecond = (sessionToShow?.history?.lastOrNull()?.usage?.rxBytes ?: 0L) / 2
                    val formattedSpeed = com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes(currentSpeedBytesPerSecond)
                    val networkSpeedString = "${formattedSpeed.first} ${formattedSpeed.second}/s"

                    HomeScreen(
                        isConnected = isConnected,
                        networkSpeed = networkSpeedString,
                        onSpectrumClick = {
                            val intent = Intent(context, StatsActivity::class.java)
                            intent.putExtra("CURRENT_SSID", ssid)
                            context.startActivity(intent)
                        },
                        session = sessionToShow,
                        ssid = ssid,
                        onConnectClick = {
                            wifiStatusViewModel.authenticatePortal()
                        }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, SecondPageActivity::class.java)
                                intent.putExtra("editMode", true)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 24.dp, end = 24.dp)
                        ) {
                            Text("Change Credentials")
                        }
                    }
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