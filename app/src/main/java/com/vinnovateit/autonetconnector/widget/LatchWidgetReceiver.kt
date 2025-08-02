package com.vinnovateit.autonetconnector.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager

class LatchWidgetReceiver : GlanceAppWidgetReceiver() {

  override val glanceAppWidget: GlanceAppWidget = AutoNetWidget()

  companion object {
    private const val PERIODIC_UPDATE_TAG = "autonet_widget_periodic_update"
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    // Manually update the widget when the system requests it.
    if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
      LatchWidgetUpdater.enqueueOneTimeUpdate(context)
    }
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    // Enqueue an immediate update when the widget is first enabled.
    LatchWidgetUpdater.enqueueOneTimeUpdate(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    // Cancel any remaining work when the last widget is disabled.
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_UPDATE_TAG)
  }
}