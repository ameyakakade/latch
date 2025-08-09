package com.vinnovateit.latch.features.wifi.manager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.features.wifi.detector.CaptivePortalDetector
import com.vinnovateit.latch.features.wifi.detector.VITWiFiIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WiFiStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    private val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Expose the connection state from the repository, which is the single source of truth
    val isConnected: StateFlow<Boolean> = SessionRepository.liveStatus
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionRepository.liveStatus.value != null
        )

    private val _ssid = MutableStateFlow("Not Connected")
    val ssid: StateFlow<String> = _ssid.asStateFlow()

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
            val currentSSID = VITWiFiIdentifier.getCurrentSSID(ctx)

            if (currentSSID != null && currentSSID.contains("vit", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    _ssid.value = currentSSID
                }
                val authenticated = !CaptivePortalDetector.isCaptivePortalActive(ctx)

                if (authenticated) {
                    SessionRepository.startSession(currentSSID)
                } else {
                    SessionRepository.stopSession()
                }
            } else {
                withContext(Dispatchers.Main) {
                    _ssid.value = currentSSID ?: "Not Connected"
                }
                SessionRepository.stopSession()
            }
        }
    }

    /**
     * Initiates a manual login attempt.
     */
    fun authenticatePortal() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = StoredCredentials.getUserId(ctx) ?: return@launch
            val pass = StoredCredentials.getPassword(ctx) ?: return@launch
            val success = AutoLoginManager.attemptLogin(user, pass)
            if (success) {
                // After a successful login, force a refresh. The service's NetworkCallback
                // will also detect this change, ensuring the session starts reliably.
                refreshStatus()
            }
        }
    }

    /**
     * Starts the ForegroundService to ensure stats are collected even when the app is closed.
     */
    private fun startStatsService() {
        val serviceIntent = Intent(getApplication(), ForegroundService::class.java)
        getApplication<Application>().startForegroundService(serviceIntent)
    }
}
