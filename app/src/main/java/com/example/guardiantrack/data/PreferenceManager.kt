package com.example.guardiantrack.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "guardian_track_settings")

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Keys
    private val DARK_MODE_ACTIVE = booleanPreferencesKey("dark_mode_active")
    private val SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")

    // Dark Mode
    val darkModeActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_ACTIVE] ?: false
    }

    suspend fun saveDarkMode(isActive: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ACTIVE] = isActive
        }
    }

    // Sensitivity Threshold (Default 15.0f as per spec)
    val sensitivityThreshold: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SENSITIVITY_THRESHOLD] ?: 15.0f
    }

    suspend fun saveSensitivityThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[SENSITIVITY_THRESHOLD] = threshold
        }
    }
}

