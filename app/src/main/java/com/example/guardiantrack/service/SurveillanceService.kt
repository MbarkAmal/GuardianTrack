package com.example.guardiantrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.guardiantrack.data.MonitoringManager
import com.example.guardiantrack.data.PreferenceManager
import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.data.repository.IncidentRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class SurveillanceService : Service(), SensorEventListener {

    @Inject
    lateinit var repository: IncidentRepository

    @Inject
    lateinit var monitoringManager: MonitoringManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    // Fall Detection Sensitivity (Fixed)
    private val DEFAULT_THRESHOLD = 15.0f

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    
    // Fall Detection Variables
    private var freeFallStartTime: Long = 0
    private var isPhase1Complete = false
    private var phase1CompletionTime: Long = 0

    companion object {
        private const val CHANNEL_ID = "surveillance_channel"
        private const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupSensors()
        startPeriodicUpdates()
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            monitoringManager.updateState { it.copy(isAccelerometerActive = true) }
        }
    }

    private fun startPeriodicUpdates() {
        val runnable = object : Runnable {
            override fun run() {
                updateBatteryLevel()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
        handler.post(runnable)
    }

    private fun updateBatteryLevel() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            applicationContext.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        monitoringManager.updateState { it.copy(batteryLevel = level) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianTrack actif")
            .setContentText("Surveillance en arrière-plan en cours...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val magnitude = sqrt(
                event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
            )

            val now = System.currentTimeMillis()

            // Phase 1: Free-fall
            if (magnitude < 3.0f) {
                if (freeFallStartTime == 0L) {
                    freeFallStartTime = now
                } else if (!isPhase1Complete && (now - freeFallStartTime) > 100) {
                    isPhase1Complete = true
                    phase1CompletionTime = now
                }
            } else {
                if (!isPhase1Complete) {
                    freeFallStartTime = 0L // Reset if it didn't stay under <3.0m/s2 for 100ms
                }
            }

            // Phase 2: Impact
            if (isPhase1Complete) {
                if (now - phase1CompletionTime <= 200) {
                    if (magnitude > DEFAULT_THRESHOLD) {
                        recordIncident("FALL")
                        sendFallNotification()
                        
                        // Reset
                        isPhase1Complete = false
                        freeFallStartTime = 0L
                    }
                } else {
                    // Time window expired
                    isPhase1Complete = false
                    freeFallStartTime = 0L
                }
            }
        }
    }

    private fun sendFallNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chute Détectée !")
            .setContentText("Une chute a été détectée et enregistrée dans l'historique.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun recordIncident(type: String) {
        serviceScope.launch {
            repository.insertIncident(
                IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    latitude = 0.0,
                    longitude = 0.0,
                    isSynced = false
                )
            )
            monitoringManager.updateState { it.copy(lastIncidentType = type) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Surveillance Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient l'application en arrière-plan pour la détection"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

