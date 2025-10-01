package com.vinnovateit.latch.features.wifi.quicksettings

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import kotlinx.coroutines.*

class LatchTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isProcessing = false

    companion object {
        private const val TAG = "LatchTileService"
        private const val ACTION_TIMEOUT_MS = 10000L // 10 seconds timeout
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "=== onStartListening called ===")
        updateTileState()

        serviceScope.launch {
            SessionRepository.liveStatus.collect {
                if (!isProcessing) {
                    updateTileState()
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "=== onStopListening called ===")
        serviceScope.coroutineContext.cancelChildren()
    }


    override fun onClick() {
        Log.d(TAG, "=== TILE CLICKED ===")

        if (isProcessing) {
            Log.d(TAG, "Already processing, ignoring click")
            return
        }

        serviceScope.launch {
            try {
                isProcessing = true
                updateTileState()

                val autoLoginEnabled = SettingsManager.autoLogin.value
                val isConnected = SessionRepository.liveStatus.value != null

                if (autoLoginEnabled || isConnected) {
                    Log.d(TAG, "Currently connected or auto-login enabled. Triggering logout...")
                    val intent = Intent(this@LatchTileService, ForegroundService::class.java).apply {
                        action = ForegroundService.ACTION_TRIGGER_LOGOUT
                    }
                    startService(intent)
                    if (autoLoginEnabled) {
                        SettingsManager.setAutoLogin(false)
                    }
                } else {
                    Log.d(TAG, "Currently disconnected and auto-login disabled. Triggering login check...")
                    val intent = Intent(this@LatchTileService, ForegroundService::class.java).apply {
                        action = ForegroundService.ACTION_TRIGGER_LOGIN_CHECK
                    }
                    startService(intent)
                }

                waitForStatusChange(timeoutMs = ACTION_TIMEOUT_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error handling tile click", e)
            } finally {
                isProcessing = false
                updateTileState()
            }
        }
    }
    private suspend fun waitForStatusChange(timeoutMs: Long) {
        val initialStatus = SessionRepository.liveStatus.value
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentStatus = SessionRepository.liveStatus.value
            if (currentStatus != initialStatus) break
            delay(200)
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            try {
                val qsTile = qsTile ?: return@launch
                val isConnected = SessionRepository.liveStatus.value != null

                when {
                    isConnected && !isProcessing -> {
                        qsTile.state = Tile.STATE_ACTIVE
                        qsTile.label = "Connected"
                    }
                    !isConnected && !isProcessing -> {
                        qsTile.state = Tile.STATE_INACTIVE
                        qsTile.label = "Latch"
                    }
                    isProcessing && isConnected -> {
                        qsTile.state = Tile.STATE_ACTIVE
                        qsTile.label = "Disconnecting..."
                    }
                    isProcessing && !isConnected -> {
                        qsTile.state = Tile.STATE_INACTIVE
                        qsTile.label = "Connecting..."
                    }
                }

                qsTile.updateTile()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating tile state", e)
            }
        }
    }
}