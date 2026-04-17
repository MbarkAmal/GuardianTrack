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

    // Step 2: Request background location SEPARATELY (Android 11+ requirement)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "Accès GPS en arrière-plan accordé ✅", Toast.LENGTH_SHORT).show()
        } else {
            showBackgroundLocationDialog()
        }
        startSurveillanceService()
    }

    // Step 1: Request foreground permissions first
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationGranted) {
            // On Android 11+, background location MUST be a separate request after foreground is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bgGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!bgGranted) {
                    showBackgroundLocationDialog()
                    return@registerForActivityResult
                }
            }
        } else {
            showGpsRationaleDialog()
        }
        startSurveillanceService()
    }

    private fun showGpsRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Accès GPS requis")
            .setMessage("L'accès à la position est nécessaire pour enregistrer l'emplacement exact lors d'une chute ou d'une alerte batterie. Veuillez activer la localisation.")
            .setPositiveButton("Paramètres") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Continuer sans GPS") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showBackgroundLocationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📍 Localisation en arrière-plan")
            .setMessage("Pour détecter l'emplacement lors d'une chute (même écran éteint), veuillez aller dans Paramètres → Autorisations → Position et choisir \"Toujours autoriser\".")
            .setPositiveButton("Ouvrir les paramètres") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                startSurveillanceService()
            }
            .setNegativeButton("Plus tard") { dialog, _ ->
                dialog.dismiss()
                startSurveillanceService()
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
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        // NOTE: ACCESS_BACKGROUND_LOCATION is intentionally NOT added here.
        // On Android 11+, it must be requested separately AFTER foreground is granted.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Only Android 10 allows background location in same batch
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
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