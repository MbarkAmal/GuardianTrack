package com.example.guardiantrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.guardiantrack.data.PreferenceManager
import com.example.guardiantrack.service.SurveillanceService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permissions accordées ✅", Toast.LENGTH_SHORT).show()
        } else {
            showGpsRationaleDialog()
        }
        startSurveillanceService()
    }

    private fun showGpsRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Accès GPS requis 📍")
            .setMessage("L'accès à la position est nécessaire pour localiser précisément les incidents de chute. Sans cela, vos incidents seront enregistrés avec la position par défaut (0, 0). Voulez-vous activer la position dans les paramètres ?")
            .setPositiveButton("Paramètres") { _, _ ->
                // Instructions for the user to go to settings
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Continuer sans GPS") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
    }


    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startSurveillanceService() {
        val serviceIntent = Intent(this, SurveillanceService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}