package com.pransetu.app.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class LocationStatus {
    Available, Unavailable
}

class LocationAvailabilityObserver(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val locationStatus: Flow<LocationStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    trySend(checkLocationStatus())
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )

        // Initial state
        trySend(checkLocationStatus())

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()

    private fun checkLocationStatus(): LocationStatus {
        val isEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
        return if (isEnabled) LocationStatus.Available else LocationStatus.Unavailable
    }
}
