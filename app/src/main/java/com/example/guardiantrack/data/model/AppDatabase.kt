package com.example.guardiantrack.data.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [Incident::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guardian_track_db"
                )
                    .fallbackToDestructiveMigration() //  for dev only
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}