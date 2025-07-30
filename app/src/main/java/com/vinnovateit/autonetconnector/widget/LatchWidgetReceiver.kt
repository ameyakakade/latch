package com.vinnovateit.autonetconnector.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LatchWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = LatchWidget()

  companion object {
    private const val WIDGET_UPDATE_WORKER = "latch-widget-update-worker"
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
      intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
      // Trigger a one-time worker to refresh the widget on theme or config changes
      val workRequest = OneTimeWorkRequestBuilder<UpdateWidgetWorker>().build()
      WorkManager.getInstance(context).enqueue(workRequest)
    }
  }

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

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORKER)
  }
}
