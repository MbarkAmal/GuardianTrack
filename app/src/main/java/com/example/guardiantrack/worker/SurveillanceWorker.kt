package com.example.guardiantrack.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.guardiantrack.service.SurveillanceService

class SurveillanceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // As a fallback for Android 12+, we start the service from this expedited worker.
        // It bypasses the "no starting foreground service from background" limit.
        val serviceIntent = Intent(applicationContext, SurveillanceService::class.java)
        
        return try {
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
