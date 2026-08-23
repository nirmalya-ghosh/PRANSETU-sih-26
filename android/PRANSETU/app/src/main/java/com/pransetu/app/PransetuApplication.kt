package com.pransetu.app

import android.app.Application
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
        
        // Enqueue periodic offline sync as a fallback
        com.pransetu.app.core.network.sync.SyncManager.enqueuePeriodicSync(this)
    }
}
