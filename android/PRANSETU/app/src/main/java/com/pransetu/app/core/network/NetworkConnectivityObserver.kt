package com.pransetu.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
    Available, Unavailable
}

/**
 * Observes live device network connectivity with real-time reactive updates.
 *
 * Uses [ConnectivityManager.registerDefaultNetworkCallback] (API 24+) to accurately track
 * the primary system network, avoiding false disconnect spikes during Wi-Fi/Cellular handover.
 */
class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Synchronously checks if the device currently has active internet connectivity.
     */
    fun isCurrentlyConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNet = connectivityManager.activeNetwork ?: return false
                val caps = connectivityManager.getNetworkCapabilities(activeNet) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                         caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            } else {
                @Suppress("DEPRECATION")
                val netInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                netInfo != null && netInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Error checking network status", e)
            false
        }
    }

    /**
     * Reactive Flow that emits [NetworkStatus.Available] or [NetworkStatus.Unavailable]
     * whenever network connectivity changes in real time.
     */
    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val isConnected = isCurrentlyConnected()
                Log.d("NetworkObserver", "Network onAvailable -> connected=$isConnected")
                trySend(if (isConnected) NetworkStatus.Available else NetworkStatus.Unavailable)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val isConnected = hasInternet && (isValidated ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                Log.d("NetworkObserver", "onCapabilitiesChanged -> hasInternet=$hasInternet, isValidated=$isValidated -> isConnected=$isConnected")
                trySend(if (isConnected) NetworkStatus.Available else NetworkStatus.Unavailable)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // Re-evaluate in case another network interface is still active (e.g. mobile data when WiFi drops)
                val isConnected = isCurrentlyConnected()
                Log.d("NetworkObserver", "Network onLost -> fallback active connected=$isConnected")
                trySend(if (isConnected) NetworkStatus.Available else NetworkStatus.Unavailable)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d("NetworkObserver", "Network onUnavailable")
                trySend(NetworkStatus.Unavailable)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }
        } catch (e: Exception) {
            Log.e("NetworkObserver", "Failed to register network callback", e)
            trySend(if (isCurrentlyConnected()) NetworkStatus.Available else NetworkStatus.Unavailable)
        }

        // Emit initial status immediately on subscription
        val initialStatus = if (isCurrentlyConnected()) NetworkStatus.Available else NetworkStatus.Unavailable
        Log.d("NetworkObserver", "Initial network status: $initialStatus")
        trySend(initialStatus)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                Log.e("NetworkObserver", "Failed to unregister network callback", e)
            }
        }
    }.distinctUntilChanged()
}
