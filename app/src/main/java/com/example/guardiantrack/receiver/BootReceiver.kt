package com.example.guardiantrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.guardiantrack.worker.SurveillanceWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            
            // Requis par la contrainte Android 12+ (API 31+) :
            // Démarrer un service en arrière-plan directement depuis un Receiver est interdit.
            // On délègue la tâche au WorkManager en "Expedited".
            val workRequest = OneTimeWorkRequestBuilder<SurveillanceWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
