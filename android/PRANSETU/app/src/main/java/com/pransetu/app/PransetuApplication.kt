package com.pransetu.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.pransetu.app.core.data.local.PransetuDatabase
import com.pransetu.app.core.data.local.UserProfileStore
import com.pransetu.app.core.data.repository.RoomSosRepository
import com.pransetu.app.core.location.LocationAvailabilityObserver
import com.pransetu.app.core.localization.LanguagePreferencesRepository
import com.pransetu.app.core.network.NetworkConnectivityObserver

class PransetuApplication : Application() {

    lateinit var networkObserver: NetworkConnectivityObserver
        private set

    lateinit var locationObserver: LocationAvailabilityObserver
        private set
        
    lateinit var locationProvider: com.pransetu.app.core.location.LocationProvider
        private set

    lateinit var languageRepository: LanguagePreferencesRepository
        private set
        
    lateinit var authRepository: com.pransetu.app.core.auth.AuthRepository
        private set
        
    /** Offline-first SOS repository backed by Room + Firestore sync */
    lateinit var sosRepository: RoomSosRepository
        private set

    lateinit var nearbyConnectionsManager: com.pransetu.app.core.network.nearby.NearbyConnectionsManager
        private set

    lateinit var batteryMonitor: com.pransetu.app.core.battery.BatteryMonitor
        private set

    lateinit var userProfileStore: UserProfileStore
        private set

    lateinit var database: PransetuDatabase
        private set

    private val TAG = "PransetuApp"

    override fun onCreate() {
        super.onCreate()
        
        batteryMonitor = com.pransetu.app.core.battery.BatteryMonitor(this)
        batteryMonitor.startMonitoring()
        networkObserver = NetworkConnectivityObserver(this)
        locationObserver = LocationAvailabilityObserver(this)
        locationProvider = com.pransetu.app.core.location.LocationProvider(this)
        languageRepository = LanguagePreferencesRepository(this)
        authRepository = com.pransetu.app.core.auth.SupabaseAuthRepository(this)
        // Supabase remote repository + Room offline-first database
        database = PransetuDatabase.getInstance(this)
        val supabaseRepo = com.pransetu.app.core.data.repository.SupabaseSosRepository()
        sosRepository = RoomSosRepository(database.sosDao(), supabaseRepo)

        nearbyConnectionsManager = com.pransetu.app.core.network.nearby.NearbyConnectionsManager(
            context = this,
            sosDao = database.sosDao(),
            meshPacketDao = database.meshPacketDao(),
            familyDao = database.familyDao(),
            remoteSosRepo = supabaseRepo,
            networkObserver = networkObserver
        )
        userProfileStore = UserProfileStore(this)
        
        // Initialize Central Event Manager & emit Application Started
        val eventManager = com.pransetu.app.core.network.events.EventManager.getInstance(this)
        eventManager.recordEvent(
            eventType = "APPLICATION_STARTED",
            payload = org.json.JSONObject().apply {
                put("app_version", "1.0.0")
                put("device_name", nearbyConnectionsManager.myDeviceName)
                put("battery_percent", batteryMonitor.batteryStatus.value.percentage)
            }
        )

        // Enqueue periodic offline sync as a fallback
        com.pransetu.app.core.network.sync.SyncManager.enqueuePeriodicSync(this)

        // Initialize all Notification Channels (Disaster Sirens, SOS, Mesh, System)
        com.pransetu.app.core.network.AppNotificationManager.initChannels(this)

        // Launch 24/7 Emergency Broadcast Daemon so sirens trigger even if app is closed
        com.pransetu.app.core.network.EmergencyBroadcastDaemonService.startDaemon(this)

        // Auto-start mesh when network connectivity is lost so SOS relay is always ready
        registerAutoMeshNetworkCallback()

        // If currently offline, start mesh immediately
        if (!networkObserver.isCurrentlyConnected()) {
            Log.d(TAG, "Device is currently OFFLINE. Auto-starting mesh relay engine.")
            try { nearbyConnectionsManager.startMesh() } catch (_: Exception) {}
        }
    }

    /**
     * Registers a system NetworkCallback that automatically activates/deactivates
     * the zero-cellular mesh relay based on internet availability.
     */
    private fun registerAutoMeshNetworkCallback() {
        try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    Log.d(TAG, "🔴 Internet LOST. Auto-activating emergency mesh relay.")
                    com.pransetu.app.core.network.AppNotificationManager.notifyNetworkStatus(this@PransetuApplication, false)
                    try { nearbyConnectionsManager.startMesh() } catch (_: Exception) {}
                }

                override fun onAvailable(network: Network) {
                    Log.d(TAG, "🟢 Internet RESTORED. Mesh relay will flush pending SOS via gateway uplink.")
                    com.pransetu.app.core.network.AppNotificationManager.notifyNetworkStatus(this@PransetuApplication, true)
                    // Don't stop mesh immediately — let it flush any pending SOS packets first
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not register auto-mesh network callback", e)
        }
    }
}

