package com.vinnovateit.latch.features.wifi.manager

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.features.wifi.detector.WiFiConnectionDetector
import com.vinnovateit.latch.features.wifi.detector.WiFiStateDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WiFiStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    val connectionStatus: StateFlow<ConnectionStatus> = ConnectionStatusManager.status
    val isConnected: StateFlow<Boolean> = SessionRepository.liveStatus
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionRepository.liveStatus.value != null
        )

    private val _ssid = MutableStateFlow("Not Connected")

    // The NetworkCallback will now live in the ForegroundService to run persistently.
    // The ViewModel will simply observe the results from the SessionRepository.

    init {
        // When the ViewModel is created, ensure the ForegroundService is running
        // and perform an initial status check.
        startStatsService()
        refreshStatus()
    }

    /**
     * Evaluates the current network connection to determine SSID and authentication status.
     * It then updates the SessionRepository, which is the single source of truth for the app.
     * This logic is now also mirrored in the ForegroundService for background operation.
     */
    fun refreshStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check both if Wi-Fi is connected AND if a session is active
            val isSessionActive = SessionRepository.liveStatus.value != null

            withContext(Dispatchers.Main) {
                _ssid.value = if (isSessionActive) "Connected" else "Not Connected"
            }
            Log.d("WiFiStatusViewModel", "UI Refreshed: SSID is ${_ssid.value}, IsSessionActive is $isSessionActive")
        }
    }

    /**
     * Initiates a manual login attempt.
     */
    fun authenticatePortal() {
        if (!WiFiStateDetector.isWiFiEnabled(ctx)) {
            UiNotifier.showToast(ctx, "Wi-Fi is turned off.")
            ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Wi-Fi is turned off"))
            return
        }

        if (!WiFiConnectionDetector.isConnectedToWiFi(ctx)) {
            UiNotifier.showToast(ctx, "Not connected to a Wi-Fi network.")
            ConnectionStatusManager.postStatus(ConnectionStatus.Failed("Not connected to Wi-Fi"))
            return
        }

        ConnectionStatusManager.postStatus(ConnectionStatus.Connecting("Initializing..."))

        Log.d("WiFiStatusViewModel", "Delegating network check to ForegroundService.")
        val serviceIntent = Intent(getApplication(), ForegroundService::class.java).apply {
            action = ForegroundService.ACTION_TRIGGER_LOGIN_CHECK
        }
        getApplication<Application>().startService(serviceIntent)
    }

    /**
     * Starts the ForegroundService to ensure stats are collected even when the app is closed.
     */
    private fun startStatsService() {
        val serviceIntent = Intent(getApplication(), ForegroundService::class.java)
        getApplication<Application>().startForegroundService(serviceIntent)
    }
}
