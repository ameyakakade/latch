package com.vinnovateit.latch.features.wifi.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.features.wifi.detector.CaptivePortalDetector
import com.vinnovateit.latch.features.wifi.manager.AutoLoginManager
import com.vinnovateit.latch.features.wifi.manager.LoginResult

/**
 * A streamlined background worker that performs the most direct login sequence:
 * 1. Checks for a captive portal.
 * 2. If found, attempts to log in with stored credentials.
 */
class DirectLoginWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    Log.d("DirectLoginWorker", "Checking for captive portal and attempting login...")

    // Use the new status check method. A non-204 status indicates a portal or no internet.
    if (CaptivePortalDetector.checkPortalStatus(applicationContext) != 204) {
      Log.d("DirectLoginWorker", "Captive portal detected or no internet. Getting credentials.")
      val userId = StoredCredentials.getUserId(applicationContext)
      val password = StoredCredentials.getPassword(applicationContext)

      if (userId == null || password == null) {
        Log.e("DirectLoginWorker", "Credentials not found.")
        return Result.failure()
      }

      Log.d("DirectLoginWorker", "Attempting login with user: $userId")
      val success = AutoLoginManager.attemptLogin(userId, password)

      if (success == LoginResult.Success) {
        Log.d("DirectLoginWorker", "Login successful.")
        Result.success()
      } else {
        Log.e("DirectLoginWorker", "Login failed.")
        Result.failure()
      }
    } else {
      Log.d("DirectLoginWorker", "No captive portal detected. Nothing to do.")
    }
    return Result.success()
  }
  }


/**
 * A simple worker that hits the logout URL to disconnect the session.
 */
class DirectLogoutWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    Log.d("DirectLogoutWorker", "Attempting to log out...")
    val success = AutoLoginManager.attemptLogout()
    return if (success) {
      Log.d("DirectLogoutWorker", "Logout successful.")
      Result.success()
    } else {
      Log.e("DirectLogoutWorker", "Logout failed.")
      Result.failure()
    }
  }
}