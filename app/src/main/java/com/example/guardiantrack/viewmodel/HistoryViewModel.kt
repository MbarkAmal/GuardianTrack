package com.example.guardiantrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.guardiantrack.data.model.Incident
import com.example.guardiantrack.data.repository.IncidentRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repo: IncidentRepository) : ViewModel() {

    val incidents = repo.getAllIncidents().asLiveData()

    fun addTestIncident() {
        viewModelScope.launch {
            repo.insertIncident(
                Incident(
                    timestamp = System.currentTimeMillis(),
                    type = "MANUAL",
                    latitude = 0.0,
                    longitude = 0.0,
                    isSynced = false
                )
            )
        }
    }
}

