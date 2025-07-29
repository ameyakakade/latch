package com.vinnovateit.autonetconnector.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.*
import java.util.concurrent.TimeUnit

class LatchWidgetReceiver : GlanceAppWidgetReceiver() {

  // Point to the GlanceAppWidget implementation
  override val glanceAppWidget: GlanceAppWidget = LatchWidget()

  companion object {
    private const val WIDGET_UPDATE_WORKER = "latch-widget-update-worker"
  }

  /**
   * Called when the first widget instance is placed on the home screen.
   * This is the ideal place to start our background work.
   */
  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    val workRequest = PeriodicWorkRequestBuilder<UpdateWidgetWorker>(15, TimeUnit.MINUTES)
      .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      WIDGET_UPDATE_WORKER,
      ExistingPeriodicWorkPolicy.KEEP,
      workRequest
    )
  }

  /**
   * Called when the last widget instance is removed from the home screen.
   * This is the ideal place to cancel our background work to save resources.
   */
  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORKER)
  }
}