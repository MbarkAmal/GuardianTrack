package com.example.guardiantrack.di

import android.content.Context
import com.example.guardiantrack.data.model.AppDatabase
import com.example.guardiantrack.data.model.IncidentDao
import com.example.guardiantrack.data.model.EmergencyContactDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideIncidentDao(database: AppDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    fun provideEmergencyContactDao(database: AppDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }
}
