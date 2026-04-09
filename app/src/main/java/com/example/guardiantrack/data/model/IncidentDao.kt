package com.example.guardiantrack.data.model
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: Incident)

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<Incident>>

    @Query("SELECT * FROM incidents WHERE isSynced = 0")
    suspend fun getUnsyncedIncidents(): List<Incident>

    @Query("UPDATE incidents SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}