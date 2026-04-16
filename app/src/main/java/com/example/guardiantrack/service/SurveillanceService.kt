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
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.guardiantrack.data.MonitoringManager
import com.example.guardiantrack.data.PreferenceManager
import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.data.repository.IncidentRepository
import com.example.guardiantrack.util.LocationProvider
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

    @Inject
    lateinit var locationProvider: LocationProvider

    // Fall Detection Thresholds — exactly as per spec §2.2
    private val FREE_FALL_THRESHOLD = 3.0f       // m/s² — Phase 1: magnitude must drop below this
    private var currentImpactThreshold = 15.0f    // m/s² — Phase 2: dynamic threshold (default 15)
    private val FREE_FALL_MIN_DURATION_MS = 100L // Phase 1: free-fall must last strictly > 100ms
    private val IMPACT_WINDOW_MS = 200L           // Phase 2: impact must occur within 200ms

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())
    
    // Dedicated thread for sensor processing (§4.2)
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler

    // Fall Detection State Machine
    private var freeFallStartTime: Long = 0L
    private var isFreeFallDetected = false
    private var freeFallEndTime: Long = 0L

    companion object {
        private const val TAG = "GuardianTrack_Service"
        private const val MONITOR_CHANNEL_ID = "surveillance_channel"
        private const val ALERT_CHANNEL_ID = "alert_channel"
        private const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupSensors()
        startPeriodicUpdates()
        observePreferences()
    }

    private fun observePreferences() {
        serviceScope.launch {
            preferenceManager.sensitivityThreshold.collect { threshold ->
                currentImpactThreshold = threshold
            }
        }
    }

    private fun setupSensors() {
        // Create dedicated thread as per spec §4.2
        sensorThread = HandlerThread("SensorThread").apply { start() }
        sensorHandler = Handler(sensorThread.looper)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            // Register using the dedicated background thread handler
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, sensorHandler)
            monitoringManager.updateState { state -> state.copy(isAccelerometerActive = true) }
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
        val notification = NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setContentTitle("GuardianTrack actif")
            .setContentText("Surveillance en arrière-plan en cours...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        // Android 14+ (API 34) requires specifying the foregroundServiceType flag
        // when calling startForeground(), otherwise the service is killed silently.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // --- Phase 1: Free-fall detection ---
        if (magnitude < FREE_FALL_THRESHOLD) {
            if (freeFallStartTime == 0L) {
                Log.d(TAG, "FALL DETECTOR: Potential free-fall started...")
                freeFallStartTime = now
            }
        } else {
            if (!isFreeFallDetected && freeFallStartTime != 0L) {
                if ((now - freeFallStartTime) > FREE_FALL_MIN_DURATION_MS) {
                    val duration = now - freeFallStartTime
                    Log.d(TAG, "FALL DETECTOR: Phase 1 CONFIRMED (Free-fall duration: ${duration}ms)")
                    isFreeFallDetected = true
                    freeFallEndTime = now
                } else {
                    freeFallStartTime = 0L
                }
            }

            // --- Phase 2: Impact detection ---
            if (isFreeFallDetected) {
                if ((now - freeFallEndTime) <= IMPACT_WINDOW_MS) {
                    if (magnitude > currentImpactThreshold) {
                        Log.d(TAG, "FALL DETECTOR: Phase 2 CONFIRMED (Impact Magnitude: ${magnitude}m/s²)")
                        recordIncident("FALL")
                        sendFallNotification()
                        resetFallState()
                    }
                } else {
                    Log.d(TAG, "FALL DETECTOR: Impact window expired. reset.")
                    resetFallState()
                }
            }
        }
    }

    private fun resetFallState() {
        isFreeFallDetected = false
        freeFallStartTime = 0L
        freeFallEndTime = 0L
    }

    private fun sendFallNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Chute Détectée !")
            .setContentText("Une chute a été détectée et enregistrée.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun recordIncident(type: String) {
        serviceScope.launch {
            // Fetch real location as per spec §4.1
            val (lat, lon) = locationProvider.getCurrentLocation()
            
            repository.insertIncident(
                IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    latitude = lat,
                    longitude = lon,
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
        sensorThread.quitSafely()
        handler.removeCallbacksAndMessages(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. MONITORING CHANNEL (Silent, Low priority)
            val monitorChannel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Monitor Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that GuardianTrack is protecting you in background"
            }

            // 2. ALERT CHANNEL (Loud, High priority, Vibrates)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Fall Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when a fall is detected"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000)
                setBypassDnd(true) // Important for safety
            }

            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }
}

