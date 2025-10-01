package com.vinnovateit.latch.features.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat // Import this
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.latch.common.util.formatBitsPerSecond
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.stats.StatsViewModel
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.features.wifi.manager.WiFiStatusViewModel
import com.vinnovateit.latch.ui.theme.LatchTheme

class MainActivity : ComponentActivity() {

    private val wifiStatusViewModel: WiFiStatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is the crucial line you were missing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        SettingsManager.initialize(this)

        // Trigger auto-login check on launch if the setting is enabled
        if (SettingsManager.autoLogin.value) {
            val serviceIntent = Intent(this, ForegroundService::class.java).apply {
                action = ForegroundService.ACTION_TRIGGER_LOGIN_CHECK
            }
            startService(serviceIntent)
        }

        setContent {
            LatchTheme {
                val statsViewModel: StatsViewModel = viewModel()
                val isConnected by wifiStatusViewModel.isConnected.collectAsStateWithLifecycle()
                val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()
                val connectionStatus by wifiStatusViewModel.connectionStatus.collectAsStateWithLifecycle()
                val speedUnits by SettingsManager.speedUnits.collectAsStateWithLifecycle()

                val currentSpeedBytesPerSecond = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
                val formattedSpeed = formatBitsPerSecond(currentSpeedBytesPerSecond, speedUnits)

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
                        connectionStatus = connectionStatus,
                        speedUnits
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wifiStatusViewModel.refreshStatus()
    }
}