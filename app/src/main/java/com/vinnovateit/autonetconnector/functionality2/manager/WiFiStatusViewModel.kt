package com.vinnovateit.autonetconnector.functionality2.manager

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinnovateit.autonetconnector.functionality2.detector.CaptivePortalDetector
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.storage.StoredCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WiFiStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext

    private val _networkUp = MutableStateFlow(false)
    private val _isAuthenticated = MutableStateFlow(false)

    val isConnected: StateFlow<Boolean> = combine(_networkUp, _isAuthenticated) { net, auth ->
        net && auth
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _ssid = MutableStateFlow("Not Connected")
    val ssid: StateFlow<String> = _ssid

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateNetworkInfo()
            evaluateCurrentConnection()
        }
    }

    init {
        // Initialize the repository
        SessionRepository.initialize(application)
        ctx.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        updateNetworkInfo()
        evaluateCurrentConnection()
    }

    override fun onCleared() {
        super.onCleared()
        ctx.unregisterReceiver(receiver)
    }

    private fun updateNetworkInfo() {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info: NetworkInfo? = cm.activeNetworkInfo
        val currentlyConnected = info?.isConnected == true
        _networkUp.value = currentlyConnected

        if (!currentlyConnected) {
            // If network is down, stop any running session
            SessionRepository.stopSession()
        }

        _ssid.value = when {
            !currentlyConnected -> "Not Connected"
            info?.type == ConnectivityManager.TYPE_WIFI -> {
                VITWiFiIdentifier.getCurrentSSID(ctx)
                    .orEmpty()
                    .ifBlank { "Unknown Wi‑Fi" }
            }
            else -> "Other Network"
        }
    }

    fun evaluateCurrentConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSSID = _ssid.value
            if (currentSSID.contains("vit", ignoreCase = true)) {
                val authenticated = !CaptivePortalDetector.isCaptivePortalActive(ctx)
                _isAuthenticated.value = authenticated

                if (authenticated) {
                    // Start session logging
                    SessionRepository.startSession(currentSSID)
                } else {
                    // Stop session logging
                    SessionRepository.stopSession()
                }
            } else {
                _isAuthenticated.value = false
                // Stop session logging if not on a VIT network
                SessionRepository.stopSession()
            }
        }
    }

    fun authenticatePortal() {
        val currentSSID = _ssid.value
        if (!currentSSID.contains("vit", ignoreCase = true)) return

        viewModelScope.launch(Dispatchers.IO) {
            val user = StoredCredentials.getUserId(ctx) ?: return@launch
            val pass = StoredCredentials.getPassword(ctx) ?: return@launch
            val success = AutoLoginManager.attemptLogin(user, pass)
            _isAuthenticated.value = success

            if (success) {
                // Start session logging after successful manual login
                SessionRepository.startSession(currentSSID)
            }
        }
    }

    fun refreshStatus() {
        updateNetworkInfo()
        evaluateCurrentConnection()
    }
}
