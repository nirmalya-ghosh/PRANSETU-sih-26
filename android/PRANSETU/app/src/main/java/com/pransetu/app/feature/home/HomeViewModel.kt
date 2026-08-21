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
    networkConnectivityObserver: NetworkConnectivityObserver,
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
                        val location = try { locationProvider.getLastKnownLocation() } catch (e: Exception) { null }
                        // Retrieve citizen identity for the SOS record
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
                            userEmail = effectiveEmail
                        )

                        _uiState.update { it.copy(sosFeedbackMessage = "Saving SOS to Canonical Database...") }

                        // 1. Local Room Persistence + Remote Sync attempt
                        val result = sosRepository.submitSos(sosModel)

                        // 2. Autonomous Multi-Hop Mesh Broadcast across Bluetooth & Wi-Fi Direct
                        nearbyConnectionsManager.broadcastOriginSos(sosModel)

                        if (result.isSuccess) {
                            _uiState.update {
                                it.copy(sosFeedbackMessage = "🚨 SOS Active! Broadcasted over Zero-Cellular Mesh (Bluetooth/Wi-Fi) & Saved locally.")
                            }
                        } else {
                            _uiState.update {
                                it.copy(sosFeedbackMessage = "SOS Queued offline! Transmitting over nearby Mesh Relay.")
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(sosFeedbackMessage = "SOS Error: ${e.message}") }
                    }
                }
            }
            is HomeIntent.DismissSosFeedback -> {
                _uiState.update { it.copy(sosFeedbackMessage = null) }
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
                    _uiState.update { it.copy(sosFeedbackMessage = "Mesh error: ${e.message}", isMeshEnabled = false) }
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
