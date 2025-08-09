package com.vinnovateit.autonetconnector.features.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.common.util.formatBitsPerSecond
import com.vinnovateit.autonetconnector.features.settings.manager.SettingsManager
import com.vinnovateit.autonetconnector.features.stats.StatsViewModel
import com.vinnovateit.autonetconnector.features.wifi.background.WiFiMonitor
import com.vinnovateit.autonetconnector.features.wifi.manager.WiFiStatusViewModel
import com.vinnovateit.autonetconnector.ui.theme.LatchTheme

class MainActivity : ComponentActivity() {

    private val wifiStatusViewModel: WiFiStatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SettingsManager.initialize(this)
        requestLocationPermissionIfNeeded()
        WiFiMonitor.startMonitoring(applicationContext)

        setContent {
            LatchTheme {
                val statsViewModel: StatsViewModel = viewModel()

                val isConnected by wifiStatusViewModel.isConnected.collectAsStateWithLifecycle()
                val ssid by wifiStatusViewModel.ssid.collectAsStateWithLifecycle()
                val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()

                val currentSpeedBytesPerSecond = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
                val formattedSpeed = formatBitsPerSecond(currentSpeedBytesPerSecond)

                val networkSpeedString = if (isConnected && liveStatus != null) {
                    "${formattedSpeed.first} ${formattedSpeed.second}"
                } else {
                    ""
                }

                val sessionForHomeScreen = if (isConnected && liveStatus != null) {
                    statsViewModel.sessionToShow.collectAsStateWithLifecycle().value
                } else {
                    null
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                      isConnected = isConnected,
                      networkSpeed = networkSpeedString,
                      session = sessionForHomeScreen,
                      ssid = ssid,
                      onConnectClick = {
                        wifiStatusViewModel.authenticatePortal()
                      },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wifiStatusViewModel.refreshStatus()
    }

    private fun requestLocationPermissionIfNeeded() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, getString(R.string.permission_location_required), Toast.LENGTH_LONG).show()
            }
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}