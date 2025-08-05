package com.vinnovateit.autonetconnector.features.wifi.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vinnovateit.autonetconnector.features.wifi.widget.LatchWidgetUpdater

/**
 * A simple worker whose only job is to notify other app components that the connection
 * state may have changed. It triggers both the widget update and a broadcast for the QS tile.
 */
class StateUpdateWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    // 1. Correctly enqueue the widget worker to update its state.
    val updateWidgetWorkRequest = OneTimeWorkRequestBuilder<LatchWidgetUpdater>().build()
    WorkManager.getInstance(appContext).enqueue(updateWidgetWorkRequest)

    return Result.success()
  }
}
