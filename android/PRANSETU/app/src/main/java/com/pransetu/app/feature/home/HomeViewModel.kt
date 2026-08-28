package com.pransetu.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.battery.BatteryMonitor
import com.pransetu.app.core.data.local.UserProfileStore
import com.pransetu.app.core.data.repository.SosCanonicalModel
import com.pransetu.app.core.data.repository.SosRepository
import com.pransetu.app.core.location.LocationAvailabilityObserver
import com.pransetu.app.core.location.LocationProvider
import com.pransetu.app.core.localization.LanguagePreferencesRepository
import com.pransetu.app.core.network.NetworkConnectivityObserver
import com.pransetu.app.core.network.nearby.NearbyConnectionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val networkConnectivityObserver: NetworkConnectivityObserver,
    locationAvailabilityObserver: LocationAvailabilityObserver,
    private val locationProvider: LocationProvider,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    private val userProfileStore: UserProfileStore,
    private val sosRepository: SosRepository,
    private val nearbyConnectionsManager: NearbyConnectionsManager,
    private val batteryMonitor: BatteryMonitor,
    private val authRepository: com.pransetu.app.core.auth.AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        _uiState,
        networkConnectivityObserver.networkStatus,
        locationAvailabilityObserver.locationStatus,
        languagePreferencesRepository.selectedLanguageFlow,
        combine(
            nearbyConnectionsManager.peerCount,
            batteryMonitor.batteryStatus,
            userProfileStore.userName,
            nearbyConnectionsManager.meshLogs,
            nearbyConnectionsManager.isMeshActive
        ) { peers, battery, name, logs, meshActive ->
            Tuple5(peers, battery, name, logs, meshActive)
        }
    ) { state, network, location, language, (peerCount, battery, profileName, logs, meshActive) ->
        // Automatically adapt mesh duty cycle when in power save mode (<15% battery)
        nearbyConnectionsManager.setPowerSaveMode(battery.isPowerSaveMode)

        val authUser = authRepository?.currentUser?.value
        val effectiveName = when {
            profileName.isNotBlank() -> profileName
            !authUser?.displayName.isNullOrBlank() -> authUser!!.displayName!!
            !authUser?.email.isNullOrBlank() -> authUser!!.email!!.substringBefore("@")
            else -> "Citizen"
        }

        state.copy(
            networkStatus = network,
            locationStatus = location,
            selectedLanguage = language,
            peerCount = peerCount,
            batteryStatus = battery,
            userName = effectiveName,
            meshLogs = logs,
            isMeshActive = meshActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnSosClicked -> {
                viewModelScope.launch {
                    try {
                        val location = try { locationProvider.getBestLiveLocation() ?: locationProvider.getCurrentLocation() } catch (e: Exception) { null }
                        // Retrieve citizen identity for the SOS record (Original user details)
                        val profileName = try { userProfileStore.userName.first() } catch (_: Exception) { "" }
                        val profilePhone = try { userProfileStore.userPhone.first() } catch (_: Exception) { "" }
                        val authUser = authRepository?.currentUser?.value
                        val effectiveEmail = authUser?.email

                        val sosModel = SosCanonicalModel(
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            locationAccuracy = location?.accuracy,
                            locationTimestamp = location?.time ?: System.currentTimeMillis(),
                            message = intent.message,
                            userName = profileName.ifBlank { authUser?.displayName },
                            userPhone = profilePhone.ifBlank { null },
                            userEmail = effectiveEmail,
                            deviceIdentifier = nearbyConnectionsManager.myDeviceName
                        )

                        _uiState.update { it.copy(sosFeedbackMessage = "Encrypting and dispatching emergency distress signal...") }

                        val isOnline = networkConnectivityObserver.isCurrentlyConnected()
                        if (isOnline) {
                            // DIRECT UPLINK TO OSDMA / EOC:
                            // The device has active internet/cellular connection. Send directly to OSDMA / Supabase.
                            val result = sosRepository.submitSos(sosModel)
                            if (result.isSuccess) {
                                _uiState.update {
                                    it.copy(
                                        sosFeedbackMessage = "✅ SOS transmitted to State Emergency Operations Centre (SEOC / OSDMA).",
                                        sosTransmitted = true
                                    )
                                }
                            } else {
                                // Fallback to mesh relay if direct remote call encountered an issue
                                nearbyConnectionsManager.broadcastOriginSos(sosModel)
                                _uiState.update {
                                    it.copy(
                                        sosFeedbackMessage = "✅ SOS saved & routing across Emergency Peer Mesh to reach a Gateway.",
                                        sosTransmitted = true
                                    )
                                }
                            }
                        } else {
                            // ZERO-CELLULAR / OFFLINE MODE:
                            // No internet/cellular connection.
                            // 1. Persist locally to Room
                            sosRepository.submitSos(sosModel)

                            // 2. Force-start mesh if not already running (bypasses manual toggle)
                            if (!nearbyConnectionsManager.isMeshActive.value) {
                                nearbyConnectionsManager.startMesh()
                            }

                            // 3. Relay to other devices via Bluetooth/Wi-Fi Direct Mesh
                            nearbyConnectionsManager.broadcastOriginSos(sosModel)

                            _uiState.update {
                                it.copy(
                                    sosFeedbackMessage = "✅ SOS encrypted & broadcasting across Emergency Peer Mesh.",
                                    sosTransmitted = true
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(sosFeedbackMessage = "Emergency Transmission Error: ${e.message}") }
                    }
                }
            }
            is HomeIntent.DismissSosFeedback -> {
                _uiState.update { it.copy(sosFeedbackMessage = null, sosTransmitted = false) }
            }
            is HomeIntent.ToggleMesh -> {
                try {
                    if (intent.enable) {
                        nearbyConnectionsManager.startMesh()
                    } else {
                        nearbyConnectionsManager.stopMesh()
                    }
                    _uiState.update { it.copy(isMeshEnabled = intent.enable) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(sosFeedbackMessage = "Mesh Network Error: Unable to initialize relay engine (${e.message})", isMeshEnabled = false) }
                }
            }
            is HomeIntent.SetLanguage -> {
                viewModelScope.launch {
                    languagePreferencesRepository.saveLanguagePreference(intent.languageCode)
                }
            }
        }
    }
}

data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
