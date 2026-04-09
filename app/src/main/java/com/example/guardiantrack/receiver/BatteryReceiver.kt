package com.example.guardiantrack.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.guardiantrack.data.model.AppDatabase
import com.example.guardiantrack.data.model.Incident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID   = "battery_alert_channel"
        private const val CHANNEL_NAME = "Battery Alerts"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_LOW) return

        // 1. Insert a BATTERIE_CRITIQUE incident into Room
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            db.incidentDao().insertIncident(
                Incident(
                    timestamp = System.currentTimeMillis(),
                    type      = "BATTERIE_CRITIQUE",
                    latitude  = 0.0,
                    longitude = 0.0,
                    isSynced  = false
                )
            )
        }

        // 2. Send a local "last-resort" notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create the channel once (no-op on subsequent calls)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts triggered when battery is critically low"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Batterie critique")
            .setContentText("GuardianTrack a enregistré un incident de batterie faible.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}