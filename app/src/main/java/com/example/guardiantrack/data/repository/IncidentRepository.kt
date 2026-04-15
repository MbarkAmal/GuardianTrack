package com.example.guardiantrack.data.repository

import com.example.guardiantrack.data.model.IncidentEntity
import com.example.guardiantrack.data.model.IncidentDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(private val dao: IncidentDao) {

    suspend fun insertIncident(incident: IncidentEntity) {
        dao.insertIncident(incident)
    }

    fun getAllIncidents() = dao.getAllIncidents()
    suspend fun deleteIncident(incident: IncidentEntity) = dao.deleteIncident(incident)
}