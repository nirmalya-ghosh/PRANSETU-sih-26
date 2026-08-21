package com.pransetu.app.feature.alerts

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.data.local.AlertDao
import com.pransetu.app.core.data.local.AlertEntity
import com.pransetu.app.core.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AlertItemUi(
    val entity: AlertEntity,
    val distanceKm: Double? = null,
    val isUserInImpactZone: Boolean = false,
    val timeToImpactFormatted: String? = null,
    val liveTimeAgoFormatted: String = "🟢 LIVE"
)

data class AlertsUiState(
    val alerts: List<AlertItemUi> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: String = "ALL", // "ALL", "SEISMIC", "WEATHER", "NEAR_ME", "ACTIVE"
    val userLocation: Location? = null,
    val impactZoneAlertCount: Int = 0,
    val isRefreshing: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

class AlertsViewModel(
    private val alertDao: AlertDao,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val liveFeedService = com.pransetu.app.core.network.api.LiveDisasterFeedService(alertDao)
    private val _selectedFilter = MutableStateFlow("ALL")
    private val _userLocation = MutableStateFlow<Location?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<AlertsUiState> = combine(
        alertDao.observeAllAlerts(),
        _selectedFilter,
        _userLocation,
        _isRefreshing,
        _lastSyncTime
    ) { alertList, filter, userLoc, refreshing, lastSync ->
        val now = System.currentTimeMillis()
        val mappedAlerts = alertList.map { entity ->
            var distanceKm: Double? = null
            var inImpactZone = false
            if (userLoc != null && entity.latitude != null && entity.longitude != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLoc.latitude,
                    userLoc.longitude,
                    entity.latitude,
                    entity.longitude,
                    results
                )
                distanceKm = (results[0] / 1000.0)
                inImpactZone = distanceKm <= entity.impactRadiusKm
            }

            val timeDiff = now - entity.timestamp
            val liveTimeAgo = when {
                timeDiff < 60_000L -> "🟢 LIVE • Just now"
                timeDiff < 3_600_000L -> "🟢 LIVE • ${timeDiff / 60_000L}m ago"
                timeDiff < 86_400_000L -> {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entity.timestamp))
                    "🟢 TODAY • $timeStr"
                }
                else -> {
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entity.timestamp))
                    "📅 $dateStr"
                }
            }

            val timeToImpact = if (entity.isUpcoming && entity.expectedImpactTime != null) {
                val diffMs = entity.expectedImpactTime - now
                if (diffMs > 0) {
                    val hours = diffMs / (1000 * 60 * 60)
                    val mins = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                    "Strikes in ${hours}h ${mins}m"
                } else {
                    "Impact Imminent"
                }
            } else if (!entity.isUpcoming) {
                "Active Now"
            } else null

            AlertItemUi(
                entity = entity,
                distanceKm = distanceKm,
                isUserInImpactZone = inImpactZone,
                timeToImpactFormatted = timeToImpact,
                liveTimeAgoFormatted = liveTimeAgo
            )
        }

        val filtered = when (filter) {
            "SEISMIC" -> mappedAlerts.filter { it.entity.category.equals("EARTHQUAKE", ignoreCase = true) }
            "WEATHER" -> mappedAlerts.filter { it.entity.category.equals("WEATHER", ignoreCase = true) }
            "ACTIVE" -> mappedAlerts.filter { it.entity.severity >= 1 }
            "NEAR_ME" -> mappedAlerts.filter { (it.distanceKm ?: 9999.0) <= 200.0 }
            else -> mappedAlerts
        }

        val impactZoneCount = mappedAlerts.count { it.isUserInImpactZone }

        AlertsUiState(
            alerts = filtered,
            unreadCount = alertList.count { !it.isRead },
            selectedFilter = filter,
            userLocation = userLoc,
            impactZoneAlertCount = impactZoneCount,
            isRefreshing = refreshing,
            lastSyncTimestamp = lastSync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertsUiState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Clean out any legacy mock data from previous sessions
            try {
                alertDao.deleteAllAlerts()
            } catch (_: Exception) {}

            // Immediately ingest real-time live disaster feeds
            refreshLiveDisasterFeeds()
        }
        refreshUserLocation()
    }

    fun refreshUserLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loc = locationProvider.getCurrentLocation() ?: locationProvider.getLastKnownLocation()
                _userLocation.value = loc
                if (loc != null) {
                    liveFeedService.fetchAndIngestLiveAlerts(loc)
                    _lastSyncTime.value = System.currentTimeMillis()
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshLiveDisasterFeeds(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val loc = locationProvider.getCurrentLocation() ?: locationProvider.getLastKnownLocation()
                _userLocation.value = loc

                val result = liveFeedService.fetchAndIngestLiveAlerts(loc)
                _lastSyncTime.value = System.currentTimeMillis()
                _isRefreshing.value = false

                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    onComplete(true, "✅ Synced $count live satellite & seismic alerts in real-time")
                } else {
                    onComplete(false, "⚠️ Live sync warning: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _isRefreshing.value = false
                onComplete(false, "⚠️ Sync error: ${e.message}")
            }
        }
    }

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun markAsRead(alertId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            alertDao.markAsRead(alertId)
        }
    }
}
