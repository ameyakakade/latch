package com.vinnovateit.autonetconnector.features.wifi.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.vinnovateit.autonetconnector.features.home.MainActivity
import com.vinnovateit.autonetconnector.features.wifi.manager.AutoLoginManager
import com.vinnovateit.autonetconnector.domain.model.WifiStatsManager
import com.vinnovateit.autonetconnector.data.StoredCredentials
import com.vinnovateit.autonetconnector.domain.model.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LatchTileService : TileService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var isProcessing = false
  private lateinit var sessionRepository: SessionRepository

  override fun onCreate() {
    super.onCreate()
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
              withContext(Dispatchers.Main) {
                updateTileState()
                Log.d(TAG, "Set tile to connecting state")
              }

                Log.d(TAG, "Step 1: Checking current authentication status...")
                val isAlreadyAuthenticated = try {
                    val status = WifiStatsManager.liveStatus.value
                    Log.d(TAG, "WifiStatsManager.liveStatus.value = $status")
                    status != null
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking liveStatus", e)
                    false
                }

                if (isAlreadyAuthenticated) {
                    Log.d(TAG, "Already authenticated - opening app for disconnect options")
                  withContext(Dispatchers.Main) { openApp() }
                    return@launch
                }

                Log.d(TAG, "Step 2: Getting current SSID...")
                val currentSSID = getCurrentSSID()
                Log.d(TAG, "Current SSID: '$currentSSID'")

                if (currentSSID.isNullOrBlank()) {
                    Log.d(TAG, "No SSID detected - not connected to WiFi, opening app")
                  withContext(Dispatchers.Main) { openApp() }
                    return@launch
                }

                if (!currentSSID.contains("vit", ignoreCase = true)) {
                    Log.d(TAG, "SSID '$currentSSID' doesn't contain 'vit' - opening app")
                  withContext(Dispatchers.Main) { openApp() }
                    return@launch
                }

                Log.d(TAG, "SSID validation passed: '$currentSSID' contains 'vit'")

                Log.d(TAG, "Step 4: Getting stored credentials...")
                val user = StoredCredentials.getUserId(this@LatchTileService)
                val pass = StoredCredentials.getPassword(this@LatchTileService)

                Log.d(TAG, "Retrieved credentials - User: ${if (user.isNullOrBlank()) "NULL/EMPTY" else "EXISTS"}, Pass: ${if (pass.isNullOrBlank()) "NULL/EMPTY" else "EXISTS"}")

                if (user.isNullOrBlank() || pass.isNullOrBlank()) {
                    Log.d(TAG, "No valid credentials found - opening app")
                  withContext(Dispatchers.Main) { openApp() }
                    return@launch
                }

                Log.d(TAG, "Step 5: Attempting login with AutoLoginManager...")
                Log.d(TAG, "Calling AutoLoginManager.attemptLogin('$user', '[PASSWORD_HIDDEN]')")

                val success = AutoLoginManager.attemptLogin(user, pass)
                Log.d(TAG, "AutoLoginManager.attemptLogin returned: $success")

                if (success) {
                    Log.d(TAG, "Login successful! Starting WiFi logging...")
                    WifiStatsManager.startLogging(this@LatchTileService, currentSSID)
                    Log.d(TAG, "WiFi logging started for SSID: $currentSSID")
                } else {
                    Log.w(TAG, "Login failed - AutoLoginManager returned false")
                }

                Log.d(TAG, "Waiting 2 seconds for status to update...")
              delay(2000)

            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL ERROR during tile click processing", e)
                e.printStackTrace()
              withContext(Dispatchers.Main) { openApp() }
            } finally {
                isProcessing = false
              withContext(Dispatchers.Main) {
                updateTileState()
                Log.d(TAG, "=== TILE PROCESSING COMPLETE ===")
              }
            }
        }
    }

    private fun getCurrentSSID(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
                ?: return null.also { Log.e(TAG, "WifiManager is null") }

            val wifiInfo = wifiManager.connectionInfo
                ?: return null.also { Log.e(TAG, "WifiInfo is null") }

            val ssid = wifiInfo.ssid
            Log.d(TAG, "Raw SSID from WifiManager: '$ssid'")

            val cleanedSSID = ssid?.replace("\"", "")

            if (cleanedSSID == "<unknown ssid>" || cleanedSSID.isNullOrBlank()) {
                Log.d(TAG, "Invalid SSID detected: '$cleanedSSID'")
                null
            } else {
                Log.d(TAG, "Cleaned SSID: '$cleanedSSID'")
                cleanedSSID
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting current SSID", e)
            e.printStackTrace()
            null
        }
    }

//    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        try {
            Log.d(TAG, "Opening main app...")
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
            Log.d(TAG, "Successfully opened main app")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app", e)
            e.printStackTrace()
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            try {
                val qsTile = qsTile
                if (qsTile == null) {
                    Log.e(TAG, "qsTile is null, cannot update")
                    return@launch
                }

                Log.d(TAG, "Updating tile state...")

                when {
                    isProcessing -> {
                        Log.d(TAG, "Setting tile to PROCESSING state")
                        qsTile.state = Tile.STATE_UNAVAILABLE
                        qsTile.label = "Connecting..."
                        qsTile.contentDescription = "Latch is connecting to WiFi"
                    }
                    else -> {
                        Log.d(TAG, "Checking connection status for tile update...")
                        val isConnected = try {
                            val status = WifiStatsManager.liveStatus.value
                            Log.d(TAG, "WifiStatsManager.liveStatus.value = $status")
                            status != null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking connection status for tile", e)
                            false
                        }

                        val currentSSID = getCurrentSSID()
                        val isVitNetwork = currentSSID?.contains("vit", ignoreCase = true) == true

                        Log.d(TAG, "Tile state calculation: isConnected=$isConnected, currentSSID='$currentSSID', isVitNetwork=$isVitNetwork")

                        if (isConnected) {
                            Log.d(TAG, "Setting tile to CONNECTED state")
                            qsTile.state = Tile.STATE_ACTIVE
                            qsTile.label = "Connected"
                            qsTile.contentDescription = "WiFi is connected via Latch. Tap to open app."
                        } else if (isVitNetwork) {
                            Log.d(TAG, "Setting tile to CONNECT state (VIT network)")
                            qsTile.state = Tile.STATE_INACTIVE
                            qsTile.label = "Connect"
                            qsTile.contentDescription = "Tap to connect to VIT WiFi via Latch"
                        } else {
                            Log.d(TAG, "Setting tile to OPEN APP state (non-VIT or no network)")
                            qsTile.state = Tile.STATE_INACTIVE
                            qsTile.label = "Open App"
                            qsTile.contentDescription = "Tap to open Latch app"
                        }
                    }
                }

                qsTile.updateTile()
                Log.d(TAG, "Tile updated successfully - State: ${qsTile.state}, Label: '${qsTile.label}'")

            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL ERROR updating tile state", e)
                e.printStackTrace()
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Log.d(TAG, "=== TILE ADDED TO QUICK SETTINGS ===")
        updateTileState()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.d(TAG, "=== TILE REMOVED FROM QUICK SETTINGS ===")
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "=== onStopListening called ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== SERVICE DESTROYED ===")
    }
}