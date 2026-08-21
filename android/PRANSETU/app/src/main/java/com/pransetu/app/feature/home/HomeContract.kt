package com.pransetu.app.feature.home

import com.pransetu.app.core.battery.BatteryStatus
import com.pransetu.app.core.location.LocationStatus
import com.pransetu.app.core.network.NetworkStatus

data class HomeUiState(
    val networkStatus: NetworkStatus = NetworkStatus.Unavailable,
    val locationStatus: LocationStatus = LocationStatus.Unavailable,
    val selectedLanguage: String = "en",
    val sosFeedbackMessage: String? = null,
    val isMeshEnabled: Boolean = true,
    val isMeshActive: Boolean = true,
    val peerCount: Int = 0,
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val userName: String = "",
    val meshLogs: List<com.pransetu.app.core.network.nearby.MeshRelayLog> = emptyList()
)

sealed interface HomeIntent {
    data class OnSosClicked(val message: String? = null) : HomeIntent
    object DismissSosFeedback : HomeIntent
    data class ToggleMesh(val enable: Boolean) : HomeIntent
    data class SetLanguage(val languageCode: String) : HomeIntent
}
