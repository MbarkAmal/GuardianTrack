package com.example.guardiantrack.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MonitoringState(
    val batteryLevel: Int = 0,
    val isAccelerometerActive: Boolean = false,
    val lastIncidentType: String? = null,
    val gpsStatus: String = "Inactif"
)

@Singleton
class MonitoringManager @Inject constructor() {
    private val _state = MutableStateFlow(MonitoringState())
    val state = _state.asStateFlow()

    fun updateState(reducer: (MonitoringState) -> MonitoringState) {
        _state.value = reducer(_state.value)
    }
}
