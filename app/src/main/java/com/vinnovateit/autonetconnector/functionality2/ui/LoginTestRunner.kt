package com.vinnovateit.autonetconnector.functionality2.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.vinnovateit.autonetconnector.functionality2.storage.StoredCredentials
import com.vinnovateit.autonetconnector.functionality2.detector.*
import com.vinnovateit.autonetconnector.functionality2.manager.AutoLoginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoginTestRunner {

    suspend fun run(context: Context) = withContext(Dispatchers.IO) {
        Log.d("LoginTest", "Running full auto-login chain...")

        if (!WiFiStateDetector.isWiFiEnabled(context)) {
            Log.d("LoginTest", "Wi-Fi is disabled.")
            return@withContext
        }

        if (!WiFiConnectionDetector.isConnectedToWiFi(context)) {
            Log.d("LoginTest", "Not connected to any Wi-Fi.")
            return@withContext
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LoginTest", "Location permission not granted.")
            return@withContext
        }

        if (!VITWiFiIdentifier.isConnectedToVITWiFi(context)) {
            Log.d("LoginTest", "Connected Wi-Fi is not a VIT Wi-Fi.")
            return@withContext
        }

        if (!CaptivePortalDetector.isCaptivePortalActive(context)) {
            Log.d("LoginTest", "No captive portal detected. Internet may already be working.")
            return@withContext
        }

        Log.d("LoginTest", "Captive portal detected. Proceeding to login...")

        val userId = StoredCredentials.getUserId(context)
        val password = StoredCredentials.getPassword(context)

        val success = AutoLoginManager.attemptLogin(userId ?: "", password ?: "")

        if (success) {
            Log.d("LoginTest", "✅ Auto-login successful!")
        } else {
            Log.d("LoginTest", "❌ Auto-login failed.")
        }
    }
}
