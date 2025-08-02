package com.vinnovateit.autonetconnector.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality2.detector.CaptivePortalDetector
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.detector.WiFiConnectionDetector
import com.vinnovateit.autonetconnector.functionality2.manager.SessionRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LatchWidgetUpdater(
  context: Context,
  params: WorkerParameters
) : CoroutineWorker(context, params) {

  companion object {
    val WIDGET_STATE_PREF_KEY = stringPreferencesKey("autonet_widget_state")

    fun enqueueOneTimeUpdate(context: Context) {
      val request = OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
      WorkManager.getInstance(context).enqueue(request)
    }
  }

  override suspend fun doWork(): Result {
    val manager = GlanceAppWidgetManager(applicationContext)
    val glanceIds = manager.getGlanceIds(AutoNetWidget::class.java)
    if (glanceIds.isEmpty()) return Result.success()

    // Read current state
    val isConnectedToWifi = WiFiConnectionDetector.isConnectedToWiFi(applicationContext)
    val hasInternet = isConnectedToWifi && !CaptivePortalDetector.isCaptivePortalActive(applicationContext)

    val widgetState = if (hasInternet) {
      val liveSession = try {
        SessionRepository.liveStatus.firstOrNull()
      } catch (e: Exception) {
        null // Handle repository errors gracefully
      }
      val durationString = liveSession?.let {
        val connectedAt = it.startTimeMillis
        val now = System.currentTimeMillis() + TimeZone.getTimeZone("Asia/Kolkata").rawOffset // IST adjustment
        val durationMillis = now - connectedAt
        when {
          durationMillis < 30_000 -> "Just now"
          durationMillis < 60_000 -> "30 sec"
          durationMillis < 120_000 -> "1 min"
          durationMillis < 5 * 60_000 -> "${durationMillis / 60_000} min"
          durationMillis < 60 * 60_000 -> "${durationMillis / 60_000}m"
          else -> "${TimeUnit.MILLISECONDS.toHours(durationMillis)}h"
        }
      } ?: applicationContext.getString(R.string.widget_duration_fallback)
      AutoNetWidgetState(
        status = "Connected",
        ssid = VITWiFiIdentifier.getCurrentSSID(applicationContext) ?: "VIT WiFi",
        connectedDuration = durationString,
        isConnected = true
      )
    } else {
      AutoNetWidgetState(
        status = applicationContext.getString(R.string.widget_status_disconnected),
        ssid = if (isConnectedToWifi) applicationContext.getString(R.string.widget_login_required) else applicationContext.getString(R.string.widget_ssid_na),
        connectedDuration = applicationContext.getString(R.string.widget_duration_fallback),
        isConnected = false
      )
    }

    glanceIds.forEach { glanceId ->
      updateAppWidgetState(applicationContext, glanceId) { prefs ->
        prefs[WIDGET_STATE_PREF_KEY] = Json.encodeToString(widgetState)
      }
      AutoNetWidget().update(applicationContext, glanceId)
    }
    return Result.success()
  }
}