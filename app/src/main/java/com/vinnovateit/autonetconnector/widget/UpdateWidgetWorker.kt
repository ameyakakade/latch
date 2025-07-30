package com.vinnovateit.autonetconnector.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vinnovateit.autonetconnector.functionality2.manager.WifiStatsManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class UpdateWidgetWorker(
  private val context: Context,
  workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

  companion object {
    const val WIDGET_STATE_KEY = "LATCH_WIDGET_STATE"
  }

  override suspend fun doWork(): Result {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(LatchWidget::class.java)
    if (glanceIds.isEmpty()) return Result.success()

    val latestState = WifiStatsManager.liveStatus.first()
    val widgetState: LatchWidgetState
    if (latestState != null) {
      // Time-based connected duration string
      val connectedAt = latestState.startTimeMillis
      val now = System.currentTimeMillis()
      val durationMillis = now - connectedAt
      val durationString = when {
        durationMillis < 30_000 -> "Just now"
        durationMillis < 60_000 -> "30 sec"
        durationMillis < 120_000 -> "1 min"
        durationMillis < 5 * 60_000 -> "${durationMillis / 60_000} min"
        durationMillis < 60 * 60_000 -> "${durationMillis / 60_000}m"
        else -> "${TimeUnit.MILLISECONDS.toHours(durationMillis)}h"
      }

      widgetState = LatchWidgetState(
        status = "Connected",
        ssid = latestState.ssid.ifEmpty { "N/A" },
        connectedDuration = durationString,
        isConnected = true
      )
    } else {
      widgetState = LatchWidgetState(
        status = "Disconnected",
        ssid = "N/A",
        connectedDuration = "-",
        isConnected = false
      )
    }

    glanceIds.forEach { glanceId ->
      updateAppWidgetState(context, glanceId) { prefs ->
        val stateKey = stringPreferencesKey(WIDGET_STATE_KEY)
        prefs[stateKey] = Json.encodeToString(widgetState)
      }
      LatchWidget().update(context, glanceId)
    }

    return Result.success()
  }
}
