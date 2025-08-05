package com.vinnovateit.autonetconnector.features.wifi.background


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vinnovateit.autonetconnector.common.util.LoginTestRunner

class AutoLoginWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AutoLoginWorker", "Executing auto-login worker...")
        LoginTestRunner.run(applicationContext)
        return Result.success()
    }
}
