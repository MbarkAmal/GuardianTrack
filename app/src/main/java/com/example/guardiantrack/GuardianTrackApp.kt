package com.example.guardiantrack

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.guardiantrack.data.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GuardianTrackApp : Application() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()

        // Apply theme immediately on startup
        CoroutineScope(Dispatchers.Main).launch {
            val isDarkModeActive = preferenceManager.darkModeActive.first()
            val mode = if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}

