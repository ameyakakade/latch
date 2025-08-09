package com.vinnovateit.latch.features.wifi.background


import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoLoginScheduler {
    fun scheduleAutoLogin(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AutoLoginWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)  // Optional delay for Wi-Fi to stabilize
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
