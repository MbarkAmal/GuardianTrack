package com.example.guardiantrack.data.repository

import com.example.guardiantrack.data.model.Incident
import com.example.guardiantrack.data.model.IncidentDao

class IncidentRepository(private val dao: IncidentDao) {

    suspend fun insertIncident(incident: Incident) {
        dao.insertIncident(incident)
    }

    fun getAllIncidents() = dao.getAllIncidents()
}