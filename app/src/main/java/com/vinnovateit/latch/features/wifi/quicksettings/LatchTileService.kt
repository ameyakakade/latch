package com.vinnovateit.latch.features.wifi.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.features.wifi.manager.WiFiStatusViewModel
import android.app.Application
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.wifi.manager.AutoLoginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LatchTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isProcessing = false
    private val wifiStatusViewModel by lazy {
        WiFiStatusViewModel(application as Application)
    }

    companion object {
        private const val TAG = "LatchTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "=== onStartListening called ===")
        updateTileState()
    }

    override fun onClick() {
        Log.d(TAG, "=== TILE CLICKED ===")

        if (isProcessing) {
            Log.d(TAG, "Already processing, ignoring click")
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                isProcessing = true
                withContext(Dispatchers.Main) { updateTileState() }

                val isConnected = SessionRepository.liveStatus.value != null
                if (isConnected) {
                    Log.d(TAG, "Currently connected. Attempting logout...")
                    AutoLoginManager.attemptLogout()
                } else {
                    Log.d(TAG, "Currently disconnected. Attempting portal authentication...")
                    wifiStatusViewModel.authenticatePortal()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling tile click", e)
                withContext(Dispatchers.Main) { openApp() }
            } finally {
                isProcessing = false
                withContext(Dispatchers.Main) { updateTileState() }
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app", e)
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            try {
                val qsTile = qsTile ?: return@launch
                when {
                    isProcessing -> {
                        qsTile.state = Tile.STATE_UNAVAILABLE
                        qsTile.label = "Processing..."
                    }
                    SessionRepository.liveStatus.value != null -> {
                        qsTile.state = Tile.STATE_ACTIVE
                        qsTile.label = "Connected"
                    }
                    else -> {
                        qsTile.state = Tile.STATE_INACTIVE
                        qsTile.label = "Connect"
                    }
                }
                qsTile.updateTile()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating tile state", e)
            }
        }
    }
}