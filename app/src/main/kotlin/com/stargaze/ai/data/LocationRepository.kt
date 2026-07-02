package com.stargaze.ai.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stargaze.ai.astronomy.GeoLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the observer's [GeoLocation]. Uses the real fused location provider when permission is
 * granted; otherwise falls back to the default (Mumbai) so the sky is always computable. Raw device
 * coordinates are clamped/normalised via [GeoLocation.ofClamped] (untrusted-input hardening).
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow(GeoLocation.MUMBAI)
    val location: StateFlow<GeoLocation> = _location.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Refreshes the current location if permission is granted. Returns the resolved [GeoLocation].
     * Any failure leaves the last-known/default value intact and returns it.
     */
    suspend fun refresh(): GeoLocation {
        if (!hasLocationPermission()) return _location.value
        return try {
            val current: Location? = fusedClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
                ?: fusedClient.lastLocation.await()
            if (current != null) {
                val resolved = GeoLocation.ofClamped(current.latitude, current.longitude, "Your location")
                _location.value = resolved
                resolved
            } else {
                _location.value
            }
        } catch (e: SecurityException) {
            // Permission revoked between check and call — keep default.
            _location.value
        } catch (e: Exception) {
            _location.value
        }
    }
}
