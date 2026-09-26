package com.example.kinetiq.utils

import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.kinetiq.data.local.entity.GpsLogEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * LocationUtils provides GPS location tracking functionality for workouts.
 *
 * This utility captures current coordinates when a workout or meal is logged.
 * If GPS is unavailable or permissions are not granted, it returns a GpsLogEntity
 * with gpsOk = false.
 */
object LocationUtils {

    /**
     * Attempt to get current GPS location.
     *
     * Returns a GpsLogEntity with:
     * - latitude, longitude, accuracy (if GPS is available)
     * - gpsOk = true (if successful)
     * - gpsOk = false (if GPS unavailable or permission denied)
     */
    suspend fun getCurrentLocation(context: Context): GpsLogEntity = suspendCancellableCoroutine { continuation ->
        try {
            // Check if location permissions are granted
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fineLocationGranted && !coarseLocationGranted) {
                // No permissions granted, return offline GPS log
                continuation.resume(GpsLogEntity(
                    latitude = null,
                    longitude = null,
                    accuracy = null,
                    timestamp = System.currentTimeMillis(),
                    gpsOk = false
                ))
                return@suspendCancellableCoroutine
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                continuation.resume(GpsLogEntity(
                    latitude = null,
                    longitude = null,
                    accuracy = null,
                    timestamp = System.currentTimeMillis(),
                    gpsOk = false
                ))
                return@suspendCancellableCoroutine
            }

            // Try GPS provider first, then network provider
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )

            val hasLocationPermission = fineLocationGranted || coarseLocationGranted
            val bestLocation = getLastKnownLocation(locationManager, providers, hasLocationPermission)

            if (bestLocation != null) {
                continuation.resume(GpsLogEntity(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    accuracy = bestLocation.accuracy,
                    timestamp = System.currentTimeMillis(),
                    gpsOk = true
                ))
            } else {
                // No location available
                continuation.resume(GpsLogEntity(
                    latitude = null,
                    longitude = null,
                    accuracy = null,
                    timestamp = System.currentTimeMillis(),
                    gpsOk = false
                ))
            }
        } catch (e: Exception) {
            // If any error occurs, return offline GPS log
            continuation.resume(GpsLogEntity(
                latitude = null,
                longitude = null,
                accuracy = null,
                timestamp = System.currentTimeMillis(),
                gpsOk = false
            ))
        }
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(
        locationManager: LocationManager,
        providers: List<String>,
        hasPermission: Boolean
    ): android.location.Location? {
        if (!hasPermission) return null

        return providers.mapNotNull { provider ->
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }.minByOrNull { it.accuracy }
    }
}

