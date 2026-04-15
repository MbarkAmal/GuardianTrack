package com.example.guardiantrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.guardiantrack.data.MonitoringManager
import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.data.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentViewModel @Inject constructor(
    private val repository: IncidentRepository,
    private val monitoringManager: MonitoringManager
) : ViewModel() {

    // Observe real-time monitoring state
    val monitoringState = monitoringManager.state.asLiveData()

    // Trigger a manual emergency alert
    fun sendEmergencyAlert() {
        viewModelScope.launch {
            repository.insertIncident(
                IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "MANUAL",
                    latitude = 0.0, // GPS logic to follow
                    longitude = 0.0,
                    isSynced = false
                )
            )
            monitoringManager.updateState { it.copy(lastIncidentType = "MANUAL") }
        }
    }
}
