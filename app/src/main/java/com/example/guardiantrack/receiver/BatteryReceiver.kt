package com.example.guardiantrack.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.data.repository.IncidentRepository
import com.example.guardiantrack.util.LocationProvider
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BatteryReceiverEntryPoint {
    fun repository(): IncidentRepository
    fun locationProvider(): LocationProvider
}

@AndroidEntryPoint
class BatteryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: IncidentRepository

    companion object {
        private const val CHANNEL_ID   = "battery_alert_channel"
        private const val CHANNEL_NAME = "Battery Alerts"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_LOW) return

        val pendingResult = goAsync()
        
        // 1. Insert a BATTERIE_FAIBLE incident into Room
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Support manual instantiation for testing/simulation
                val (repo, locProvider) = if (::repository.isInitialized) {
                    Pair(repository, null) // In standard Hilt injection, we can't easily init locationProvider here without another check
                } else {
                    val entryPoint = EntryPointAccessors.fromApplication(context, BatteryReceiverEntryPoint::class.java)
                    Pair(entryPoint.repository(), entryPoint.locationProvider())
                }

                // Actually, let's just use the EntryPoint for both to be safe in Async receiver
                val finalRepo = EntryPointAccessors.fromApplication(context, BatteryReceiverEntryPoint::class.java).repository()
                val finalLocProvider = EntryPointAccessors.fromApplication(context, BatteryReceiverEntryPoint::class.java).locationProvider()

                val (lat, lon) = finalLocProvider.getCurrentLocation()
                val address = finalLocProvider.getAddressFromLocation(lat, lon)

                finalRepo.insertIncident(
                    IncidentEntity(
                        timestamp = System.currentTimeMillis(),
                        type      = "BATTERY",
                        latitude  = lat,
                        longitude = lon,
                        address   = address,
                        isSynced  = false
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }

        // 2. Send a high-priority notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("⚠️ Batterie Faible")
            .setContentText("Votre batterie est très faible. GuardianTrack a enregistré cet incident.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de batterie critique pour GuardianTrack"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}