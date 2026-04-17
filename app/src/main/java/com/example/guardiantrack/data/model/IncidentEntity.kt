package com.example.guardiantrack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val isSynced: Boolean = false
)