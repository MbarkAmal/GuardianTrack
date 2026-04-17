package com.example.guardiantrack.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GuardianTrack_Location"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Pair<Double, Double> {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "getCurrentLocation: fineGranted=$fineGranted, coarseGranted=$coarseGranted")

        if (!fineGranted && !coarseGranted) {
            Log.w(TAG, "No location permission granted — returning 0.0/0.0")
            return Pair(0.0, 0.0)
        }

        return try {
            // Attempt 1: High Accuracy (GPS) — fresh token
            val token1 = CancellationTokenSource()
            var location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                token1.token
            ).await()
            Log.d(TAG, "High accuracy result: $location")

            // Attempt 2: Balanced (Wi-Fi/Cell) — NEW fresh token (critical fix)
            if (location == null) {
                val token2 = CancellationTokenSource()
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    token2.token
                ).await()
                Log.d(TAG, "Balanced accuracy result: $location")
            }

            // Attempt 3: Last Known Location
            if (location == null) {
                location = fusedLocationClient.lastLocation.await()
                Log.d(TAG, "Last known location: $location")
            }

            if (location != null) {
                Log.d(TAG, "Using location: lat=${location.latitude}, lon=${location.longitude}")
                Pair(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "All location attempts failed — returning 0.0/0.0")
                Pair(0.0, 0.0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting location: ${e.message}")
            Pair(0.0, 0.0)
        }
    }

    fun getAddressFromLocation(lat: Double, lon: Double): String {
        if (lat == 0.0 && lon == 0.0) {
            Log.w(TAG, "getAddressFromLocation called with 0,0 — GPS permission may be denied")
            return "Permission GPS non accordée"
        }

        return try {
            Log.d(TAG, "Geocoding lat=$lat, lon=$lon")
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                val result = sb.toString().trim()
                Log.d(TAG, "Geocoded address: $result")
                result
            } else {
                Log.w(TAG, "Geocoder returned no results — using raw coordinates")
                "Lat: $lat, Lon: $lon"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error: ${e.message}")
            "Lat: $lat, Lon: $lon"
        }
    }
}
