package com.vinnovateit.autonetconnector.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vinnovateit.autonetconnector.functionality2.manager.WifiStatsManager
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBitsPerSecond
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    if (latestState != null && latestState.liveData.size > 1) {
      val latestDataPoint = latestState.liveData.last()

      // **THE FIX**: The usage data is for a 2-second interval.
      // We must divide by 2 to get the correct bytes per second.
      val bytesPerSecond = latestDataPoint.usage.rxBytes / 2
      val (speedValue, speedUnit) = formatBitsPerSecond(bytesPerSecond)

      widgetState = LatchWidgetState(
        status = "Connected",
        ssid = latestState.ssid.ifEmpty { "N/A" },
        speed = "$speedValue $speedUnit",
        isConnected = true
      )
    } else {
      // Default "Disconnected" state if not connected or no data yet.
      widgetState = LatchWidgetState(
        status = if (latestState != null) "Connected" else "Disconnected",
        ssid = latestState?.ssid ?: "N/A",
        speed = "0 KBPS",
        isConnected = latestState != null
      )
    }

    // Update all active widgets with the new state.
    glanceIds.forEach { glanceId ->
      updateAppWidgetState(context, glanceId) { prefs ->
        val stateKey = androidx.datastore.preferences.core.stringPreferencesKey(WIDGET_STATE_KEY)
        prefs[stateKey] = Json.encodeToString(widgetState)
      }
      LatchWidget().update(context, glanceId)
    }

    return Result.success()
  }
}