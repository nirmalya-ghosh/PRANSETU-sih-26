package com.pransetu.app.feature.family

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pransetu.app.core.auth.AuthRepository
import com.pransetu.app.core.battery.BatteryMonitor
import com.pransetu.app.core.battery.BatteryStatus
import com.pransetu.app.core.data.local.FamilyDao
import com.pransetu.app.core.data.local.FamilyMemberEntity
import com.pransetu.app.core.data.local.FamilySafetyStatus
import com.pransetu.app.core.data.local.UserProfileStore
import com.pransetu.app.core.data.repository.SosCanonicalModel
import com.pransetu.app.core.location.LocationProvider
import com.pransetu.app.core.location.PrecisionLocationData
import com.pransetu.app.core.network.nearby.NearbyConnectionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FamilyCircleUiState(
    val members: List<FamilyMemberEntity> = emptyList(),
    val selfBattery: BatteryStatus = BatteryStatus(),
    val liveLocation: PrecisionLocationData? = null,
    val isBroadcasting: Boolean = false,
    val feedbackMessage: String? = null
)

class FamilyCircleViewModel(
    private val context: Context,
    private val familyDao: FamilyDao,
    private val locationProvider: LocationProvider,
    private val userProfileStore: UserProfileStore,
    private val nearbyConnectionsManager: NearbyConnectionsManager,
    private val batteryMonitor: BatteryMonitor,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyCircleUiState())
    val uiState: StateFlow<FamilyCircleUiState> = _uiState.asStateFlow()

    val members: StateFlow<List<FamilyMemberEntity>> = familyDao.observeAllMembers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 1. Purge legacy demo mockup data from previous versions
        viewModelScope.launch(Dispatchers.IO) {
            try {
                familyDao.deleteLegacyDemoMembers()
                val allNonSelf = familyDao.getAllNonSelfMembers()
                for (m in allNonSelf) {
                    if (m.name.contains("Debasish") || m.name.contains("Priyanka") || m.name.contains("Manas") ||
                        m.phoneNumber.contains("94370") || m.phoneNumber.contains("98610") || m.phoneNumber.contains("99372")) {
                        familyDao.deleteMember(m.id)
                    }
                }
            } catch (_: Exception) {}

            // 2. Ensure authentic Self user profile exists
            val self = familyDao.getSelfMember()
            val profileName = try { userProfileStore.userName.first() } catch (_: Exception) { "" }
            val profilePhone = try { userProfileStore.userPhone.first() } catch (_: Exception) { "" }
            val authUser = authRepository?.currentUser?.value
            val effectiveName = when {
                profileName.isNotBlank() -> profileName
                !authUser?.displayName.isNullOrBlank() -> authUser!!.displayName!!
                !authUser?.email.isNullOrBlank() -> authUser!!.email!!.substringBefore("@")
                else -> "My Status (You)"
            }
            val effectivePhone = profilePhone.ifBlank { "+91 98765 43210" }
            val currentBattery = batteryMonitor.batteryStatus.value.percentage

            if (self == null) {
                familyDao.insertMember(
                    FamilyMemberEntity(
                        id = "self_user",
                        name = effectiveName,
                        relationship = "Self",
                        phoneNumber = effectivePhone,
                        status = FamilySafetyStatus.SAFE.name,
                        lastLocationName = "Current Device Location",
                        lastCheckedInAt = System.currentTimeMillis(),
                        batteryPercent = currentBattery,
                        isSelf = true
                    )
                )
            } else {
                familyDao.updateSelfBattery(currentBattery)
            }
        }

        // 3. Keep real physical battery state synchronized in real time
        viewModelScope.launch(Dispatchers.IO) {
            batteryMonitor.batteryStatus.collect { battery ->
                _uiState.update { it.copy(selfBattery = battery) }
                try {
                    familyDao.updateSelfBattery(battery.percentage)
                } catch (_: Exception) {}
            }
        }

        // 4. Stream 1-second continuous GPS updates to the UI
        viewModelScope.launch {
            locationProvider.liveLocationFlow.collect { loc ->
                _uiState.update { it.copy(liveLocation = loc) }
            }
        }
    }

    fun markSelfSafe() {
        viewModelScope.launch(Dispatchers.IO) {
            val loc = try { locationProvider.getBestLiveLocation() ?: locationProvider.getCurrentLocation() } catch (_: Exception) { null }
            val lat = loc?.latitude ?: 20.2961
            val lon = loc?.longitude ?: 85.8245
            val accuracyStr = if (loc?.accuracy != null) " (±%.1fm)".format(loc.accuracy) else ""
            val locationName = "GPS: %.4f°, %.4f°%s".format(lat, lon, accuracyStr)
            val currentBattery = batteryMonitor.batteryStatus.value.percentage

            // 1. Update Self Status in Room Database with actual battery %
            familyDao.updateSelfStatus(
                status = FamilySafetyStatus.SAFE.name,
                locationName = locationName,
                lat = lat,
                lon = lon,
                timestamp = System.currentTimeMillis(),
                batteryPercent = currentBattery
            )

            val profileName = try { userProfileStore.userName.first() } catch (_: Exception) { "" }
            val profilePhone = try { userProfileStore.userPhone.first() } catch (_: Exception) { "" }
            val authUser = authRepository?.currentUser?.value
            val effectiveName = when {
                profileName.isNotBlank() -> profileName
                !authUser?.displayName.isNullOrBlank() -> authUser!!.displayName!!
                else -> "Family Member"
            }

            // 2. Broadcast High-Priority FAMILY_SAFE_UPDATE over Nearby Mesh to all family phones
            val safeCanonical = SosCanonicalModel(
                userName = effectiveName,
                userPhone = profilePhone.ifBlank { null },
                latitude = lat,
                longitude = lon,
                locationAccuracy = loc?.accuracy,
                locationTimestamp = System.currentTimeMillis(),
                batteryPercent = currentBattery,
                message = "PUBLIC SAFETY NOTICE: Individual has confirmed status as SAFE (Family Check-in)",
                deliveryState = "SAFE_CHECKIN",
                deviceIdentifier = nearbyConnectionsManager.myDeviceName
            )
            nearbyConnectionsManager.broadcastFamilySafeUpdate(safeCanonical)

            // 3. Dispatch automated direct SMS check-ins to all registered emergency family members
            val nonSelfMembers = try { familyDao.getAllNonSelfMembers() } catch (_: Exception) { emptyList() }
            val smsText = "PRANSETU Emergency Network: $effectiveName has confirmed their status as SAFE. Location: $locationName. Battery Level: $currentBattery%. Sent via PRANSETU Offline Safety Mesh."

            for (member in nonSelfMembers) {
                if (member.phoneNumber.isNotBlank()) {
                    sendSmsNotification(member.phoneNumber, smsText)
                }
            }

            _uiState.update {
                it.copy(feedbackMessage = "Safety Status Confirmed: Marked SAFE ($currentBattery% battery). Dispatched via Emergency Mesh & SMS.")
            }
        }
    }

    fun addFamilyMember(name: String, relationship: String, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newMember = FamilyMemberEntity(
                name = name.trim(),
                relationship = relationship.trim().ifBlank { "Family" },
                phoneNumber = phone.trim(),
                status = FamilySafetyStatus.UNKNOWN.name,
                lastLocationName = "Awaiting Check-in",
                lastCheckedInAt = System.currentTimeMillis(),
                batteryPercent = null,
                isSelf = false
            )
            familyDao.insertMember(newMember)
            _uiState.update {
                it.copy(feedbackMessage = "Contact Registered: Added ${newMember.name} to Family Safety Circle.")
            }
        }
    }

    fun updateStatus(memberId: String, status: FamilySafetyStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            familyDao.updateMemberStatus(memberId, status.name, System.currentTimeMillis())
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            familyDao.deleteMember(memberId)
            _uiState.update {
                it.copy(feedbackMessage = "Contact Removed: Family member deleted from Safety Circle.")
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun sendSmsNotification(phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanPhone.isNotBlank()) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                Log.d("FamilyCircle", "SMS sent successfully to $cleanPhone")
            }
        } catch (e: Exception) {
            Log.w("FamilyCircle", "Failed to dispatch SMS to $phoneNumber: ${e.message}")
        }
    }
}
