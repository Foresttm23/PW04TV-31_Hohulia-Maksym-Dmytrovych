package com.example.mse.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.example.mse.model.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationUnavailableException(message: String) : Exception(message)

interface LocationSource {
    fun updates(): Flow<GeoPoint>
}

// Використовуємо системний LocationManager, а не FusedLocationProviderClient: не потрібні Google Play Services.
class AndroidLocationSource(private val context: Context) : LocationSource {

    @SuppressLint("MissingPermission")
    override fun updates(): Flow<GeoPoint> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            close(LocationUnavailableException("Геолокацію вимкнено в налаштуваннях пристрою"))
            return@callbackFlow
        }

        val listener = LocationListener { location: Location -> trySend(GeoPoint(location.latitude, location.longitude)) }
        providers.firstNotNullOfOrNull { manager.getLastKnownLocation(it) }
            ?.let { trySend(GeoPoint(it.latitude, it.longitude)) }
        // 2 с / 25 м: достатньо для пішохода і не розряджає батарею.
        providers.forEach { manager.requestLocationUpdates(it, 2_000L, 25f, listener, Looper.getMainLooper()) }

        awaitClose { manager.removeUpdates(listener) }
    }
}
