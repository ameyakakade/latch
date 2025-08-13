package com.vinnovateit.latch.features.wifi.widget

import android.app.UiModeManager
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.wifi.detector.WiFiConnectionDetector
import com.vinnovateit.latch.domain.model.SessionRepository
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
    val WIDGET_STATE_PREF_KEY = stringPreferencesKey("latch_widget_state")

    fun enqueueOneTimeUpdate(context: Context) {
      val request = OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
      WorkManager.getInstance(context).enqueue(request)
    }

    fun enqueuePeriodicUpdate(context: Context) {
      val periodicRequest = PeriodicWorkRequestBuilder<LatchWidgetUpdater>(
        15, TimeUnit.MINUTES
      ).build()
      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "latch_widget_periodic_update",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicRequest
      )
    }
  }

  override suspend fun doWork(): Result {
    val manager = GlanceAppWidgetManager(applicationContext)
    val glanceIds = manager.getGlanceIds(LatchWidget::class.java)
    if (glanceIds.isEmpty()) return Result.success()

    // Detect current theme mode
    val uiModeManager = applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isDarkMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

    // Read current state
    val isConnectedToWifi = WiFiConnectionDetector.isConnectedToWiFi(applicationContext)
    val hasInternet = isConnectedToWifi && true
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
          durationMillis < 30_000 -> applicationContext.getString(R.string.widget_duration_just_now)
          durationMillis < 60_000 -> "30 sec"
          durationMillis < 120_000 -> "1 min"
          durationMillis < 5 * 60_000 -> "${durationMillis / 60_000} min"
          durationMillis < 60 * 60_000 -> "${durationMillis / 60_000}m"
          else -> "${TimeUnit.MILLISECONDS.toHours(durationMillis)}h"
        }
      } ?: applicationContext.getString(R.string.widget_duration_fallback)
      LatchWidgetState(
        status = applicationContext.getString(R.string.widget_status_connected),
        connectedDuration = durationString,
        isConnected = true,
        isLightTheme = !isDarkMode
      )
    } else {
      LatchWidgetState(
        status = applicationContext.getString(R.string.widget_status_disconnected),
        connectedDuration = applicationContext.getString(R.string.widget_duration_fallback),
        isConnected = false,
        isLightTheme = !isDarkMode
      )
    }

    glanceIds.forEach { glanceId ->
      updateAppWidgetState(applicationContext, glanceId) { prefs ->
        prefs[WIDGET_STATE_PREF_KEY] = Json.encodeToString(widgetState)
      }
      LatchWidget().update(applicationContext, glanceId)
    }

    return Result.success()
  }
}
