package com.vinnovateit.autonetconnector.common.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.vinnovateit.autonetconnector.features.wifi.detector.CaptivePortalDetector
import com.vinnovateit.autonetconnector.features.wifi.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.features.wifi.detector.WiFiConnectionDetector
import com.vinnovateit.autonetconnector.features.wifi.detector.WiFiStateDetector
import com.vinnovateit.autonetconnector.features.wifi.manager.AutoLoginManager
import com.vinnovateit.autonetconnector.data.StoredCredentials
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