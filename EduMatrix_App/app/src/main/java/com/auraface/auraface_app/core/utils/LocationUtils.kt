package com.auraface.auraface_app.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

object LocationUtils {

    // Example classroom coordinates (Update as per your college environment)
    private const val CLASSROOM_LAT = 12.9716 // Example: Bangalore
    private const val CLASSROOM_LNG = 77.5946
    private const val GEOFENCE_RADIUS_METERS = 100.0 // 100 meter radius

    @SuppressLint("MissingPermission")
    suspend fun isWithinClassroom(context: Context): Boolean {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await() ?: return false
            
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                CLASSROOM_LAT, CLASSROOM_LNG,
                results
            )
            
            results[0] <= GEOFENCE_RADIUS_METERS
        } catch (e: Exception) {
            false
        }
    }
    
    fun getDistanceToClassroom(location: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            CLASSROOM_LAT, CLASSROOM_LNG,
            results
        )
        return results[0]
    }
}
