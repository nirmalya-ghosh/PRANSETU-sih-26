package com.pransetu.app.core.network.nearby

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.pransetu.app.MainActivity
import com.pransetu.app.R
import com.pransetu.app.core.data.local.FamilyDao
import com.pransetu.app.core.data.local.MeshPacketDao
import com.pransetu.app.core.data.local.MeshPacketEntity
import com.pransetu.app.core.data.local.SosDao
import com.pransetu.app.core.data.repository.SosCanonicalModel
import com.pransetu.app.core.data.repository.SosRepository
import com.pransetu.app.core.data.repository.toEntity
import com.pransetu.app.core.network.NetworkConnectivityObserver
import com.pransetu.app.core.relay.RelayPacket
import com.pransetu.app.core.relay.RelayPacketType
import com.pransetu.app.core.relay.RelayValidator
import com.pransetu.app.core.sos.DeliveryState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class MeshPeerNode(
    val endpointId: String,
    val deviceName: String,
    val connectedAt: Long = System.currentTimeMillis()
)

data class MeshRelayLog(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "SOS_ORIGINATED", "PACKET_RECEIVED", "PACKET_FORWARDED", "GATEWAY_UPLINK", "GATEWAY_SUCCESS", "ACK_RECEIVED", "PEER_DISCOVERED", "DEVICE_DISCOVERED"
    val message: String,
    val hopCount: Int = 0,
    val ttl: Int = 0,
    val sosId: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Autonomous Zero-Cellular Multi-Hop Mesh Relay Engine with True Device Name Resolution.
 * 
 * Features:
 * 1. True Device Name resolution across Bluetooth & Wi-Fi Direct.
 * 2. Force continuous Discovery & Simultaneous Advertising (Strategy.P2P_CLUSTER).
 * 3. Multi-Hop Store-and-Forward Flooding with bounded TTL & Hop perimeter.
 * 4. Autonomous Internet Gateway Uplink to OSDMA / EOC.
 * 5. Emergency Heads-Up Push Notifications & Real-Time Alerts on every relay phone: "PRANSETU SOS FORWARDED".
 * 6. Local Room Store-and-Forward Queue: flushes to newly discovered devices automatically.
 */
class NearbyConnectionsManager(
    private val context: Context,
    private val sosDao: SosDao? = null,
    private val meshPacketDao: MeshPacketDao? = null,
    private val familyDao: FamilyDao? = null,
    private val remoteSosRepo: SosRepository? = null,
    private val networkObserver: NetworkConnectivityObserver? = null
) {

    private val TAG = "NearbyMeshEngine"
    private val SERVICE_ID = "com.pransetu.app.SOS_MESH_V2"
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val NOTIFICATION_CHANNEL_ID = "pransetu_mesh_emergency_channel"

    private var connectionsClient: ConnectionsClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Discovered & Connected True Endpoint Names Map
    private val endpointTrueNames = ConcurrentHashMap<String, String>()
    private val connectedPeers = ConcurrentHashMap<String, MeshPeerNode>()

    private val _peersFlow = MutableStateFlow<List<MeshPeerNode>>(emptyList())
    val peersFlow: StateFlow<List<MeshPeerNode>> = _peersFlow.asStateFlow()

    private val _peerCount = MutableStateFlow(0)
    val peerCount: StateFlow<Int> = _peerCount.asStateFlow()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    // Activity Log Flow for UI
    private val _meshLogs = MutableStateFlow<List<MeshRelayLog>>(emptyList())
    val meshLogs: StateFlow<List<MeshRelayLog>> = _meshLogs.asStateFlow()

    private val relayValidator = RelayValidator()
    val myDeviceName: String by lazy { getMyAdvertisedDeviceName() }

    private var externalPacketListener: ((RelayPacket) -> Unit)? = null

    init {
        createNotificationChannel()
    }

    /**
     * Resolves the True Device Name of this phone (e.g. "Samsung Galaxy S24 FE" or Bluetooth Name).
     */
    fun getMyAdvertisedDeviceName(): String {
        return try {
            val btAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
            val btName = try { btAdapter?.name } catch (_: SecurityException) { null }
            val systemDeviceName = try {
                Settings.Global.getString(context.contentResolver, "device_name")
            } catch (_: Exception) { null }

            val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            val model = Build.MODEL
            val rawModel = "$manufacturer $model"

            when {
                !btName.isNullOrBlank() -> btName
                !systemDeviceName.isNullOrBlank() -> "$systemDeviceName ($rawModel)"
                else -> rawModel
            }
        } catch (_: Exception) {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }
    }

    private fun getClient(): ConnectionsClient? {
        if (connectionsClient == null) {
            connectionsClient = try {
                Nearby.getConnectionsClient(context)
            } catch (e: Exception) {
                Log.e(TAG, "Google Play Services Nearby Connections unavailable", e)
                null
            }
        }
        return connectionsClient
    }

    fun setOnMessageReceivedListener(listener: (RelayPacket) -> Unit) {
        externalPacketListener = listener
    }

    fun setPowerSaveMode(enabled: Boolean) {
        if (_isPowerSaveMode.value != enabled) {
            _isPowerSaveMode.value = enabled
            addLog("POWER_MODE", if (enabled) "⚡ Eco-Mesh Mode Active (<15% Battery)" else "⚡ Full Power Mesh Active")
        }
    }

    fun addLog(eventType: String, message: String, hopCount: Int = 0, ttl: Int = 0, sosId: String = "") {
        val logItem = MeshRelayLog(
            eventType = eventType,
            message = message,
            hopCount = hopCount,
            ttl = ttl,
            sosId = sosId
        )
        _meshLogs.update { current ->
            (listOf(logItem) + current).take(60) // Keep last 60 events
        }
        Log.d(TAG, "[$eventType] $message")
    }

    // --- On-Screen Toast & High-Priority Emergency Notifications ---

    private fun showMainThreadToast(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {}
        }
    }

    private fun showEmergencyNotification(title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display emergency notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "PRANSETU Emergency Mesh Forwarding",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts whenever a disaster SOS is forwarded across Bluetooth/Wi-Fi mesh."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    // --- Nearby Connections Handshake & Discovery Callbacks ---

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    handleIncomingPacketBytes(endpointId, bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val trueName = info.endpointName.ifBlank { "Nearby Device ${endpointId.takeLast(4)}" }
            endpointTrueNames[endpointId] = trueName
            Log.d(TAG, "Connection initiated with: $trueName (Endpoint: $endpointId)")
            
            // Automatically accept incoming mesh handshake in zero cellular
            getClient()?.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val trueName = endpointTrueNames[endpointId] ?: "Nearby Device ${endpointId.takeLast(4)}"
            if (result.status.isSuccess) {
                val peer = MeshPeerNode(endpointId, trueName)
                connectedPeers[endpointId] = peer
                updatePeersState()
                addLog("PEER_DISCOVERED", "🟢 Connected to real device: $trueName via Bluetooth/Wi-Fi Direct")

                // Immediately flush all pending local store-and-forward SOS packets to this new peer
                flushPendingStoreAndForwardQueueToPeer(endpointId, trueName)
            } else {
                Log.w(TAG, "Connection resolution failed for $trueName: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            val removed = connectedPeers.remove(endpointId)
            val name = removed?.deviceName ?: endpointTrueNames[endpointId] ?: endpointId
            updatePeersState()
            addLog("PEER_LOST", "🔴 In-range device disconnected: $name")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val trueName = info.endpointName.ifBlank { "Nearby Device ${endpointId.takeLast(4)}" }
            endpointTrueNames[endpointId] = trueName
            Log.d(TAG, "Force Discovered In-Range Device: $trueName (Endpoint: $endpointId)")

            addLog("DEVICE_DISCOVERED", "🔍 Discovered In-Range Device: $trueName • Auto-Connecting...")
            
            // Force immediate connection request with true device name
            getClient()?.requestConnection(
                myDeviceName,
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            val name = endpointTrueNames[endpointId] ?: endpointId
            Log.d(TAG, "Lost sight of device: $name")
        }
    }

    private fun updatePeersState() {
        val list = connectedPeers.values.toList()
        _peersFlow.value = list
        _peerCount.value = list.size
    }

    // --- Mesh Lifecycle Control ---

    fun startMesh() {
        try {
            if (getClient() == null) {
                Log.w(TAG, "Cannot start mesh: Nearby Connections client is null")
                return
            }
            _isMeshActive.value = true
            startAdvertising()
            startDiscovery()
            addLog("MESH_STARTED", "📡 PRANSETU Zero-Cellular Mesh Active • Device: $myDeviceName")
        } catch (e: Exception) {
            Log.e(TAG, "Error in startMesh", e)
        }
    }

    fun stopMesh() {
        try {
            getClient()?.stopAdvertising()
            getClient()?.stopDiscovery()
            getClient()?.stopAllEndpoints()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mesh", e)
        }
        connectedPeers.clear()
        updatePeersState()
        _isMeshActive.value = false
        addLog("MESH_STOPPED", "🛑 Mesh Engine Stopped")
    }

    private fun startAdvertising() {
        try {
            val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            getClient()?.startAdvertising(
                myDeviceName,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            )?.addOnSuccessListener {
                Log.d(TAG, "Advertising active as $myDeviceName")
            }?.addOnFailureListener {
                Log.e(TAG, "Advertising failed", it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startAdvertising", e)
        }
    }

    private fun startDiscovery() {
        try {
            val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            getClient()?.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                discoveryOptions
            )?.addOnSuccessListener {
                Log.d(TAG, "Discovery active for $SERVICE_ID")
            }?.addOnFailureListener {
                Log.e(TAG, "Discovery failed", it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startDiscovery", e)
        }
    }

    // --- Autonomous Multi-Hop Packet Processing Engine ---

    private fun handleIncomingPacketBytes(senderEndpointId: String, bytes: ByteArray) {
        coroutineScope.launch {
            val jsonString = String(bytes, Charsets.UTF_8)
            val packet = RelayPacket.fromJson(jsonString) ?: return@launch

            // 1. Validation & Deduplication (prevents infinite echoing)
            if (!relayValidator.validate(packet)) {
                Log.d(TAG, "Packet ${packet.packetId} dropped by deduplication / TTL validator")
                return@launch
            }

            when (packet.packetType) {
                RelayPacketType.SOS_ALERT -> handleIncomingSosAlert(senderEndpointId, packet)
                RelayPacketType.SOS_ACK -> handleIncomingSosAck(senderEndpointId, packet)
                RelayPacketType.FAMILY_SAFE_UPDATE -> handleIncomingFamilySafeUpdate(senderEndpointId, packet)
                RelayPacketType.PEER_HEARTBEAT -> {}
            }

            externalPacketListener?.invoke(packet)
        }
    }

    private suspend fun handleIncomingSosAlert(senderEndpointId: String, packet: RelayPacket) {
        val sos = packet.payload
        val sosShortId = sos.sosId.take(8)
        val routeStr = packet.relayRoute.joinToString(" ➔ ")
        val senderName = endpointTrueNames[senderEndpointId] ?: "Nearby Device"

        // 1. Emergency On-Screen Toast & Heads-Up Notification on THIS relay phone!
        showMainThreadToast("⚡ PRANSETU SOS FORWARDED: #$sosShortId from ${packet.originDeviceId} relayed to all nearby devices (Hop ${packet.hopCount + 1})")
        showEmergencyNotification(
            title = "🚨 PRANSETU SOS FORWARDED",
            message = "Emergency SOS #$sosShortId from ${packet.originDeviceId} was received from $senderName and forwarded over Bluetooth & Wi-Fi mesh (Hop ${packet.hopCount + 1})."
        )

        addLog(
            eventType = "PACKET_RECEIVED",
            message = "📥 RECEIVED & FORWARDED SOS #$sosShortId from ${packet.originDeviceId} via $senderName (Hop ${packet.hopCount}, TTL ${packet.ttl})\nRoute: $routeStr",
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            sosId = sos.sosId
        )

        // 2. Local Persistence in Room Database
        try {
            val entity = sos.toEntity().copy(
                deliveryState = DeliveryState.RELAYING,
                hopCount = packet.hopCount,
                ttl = packet.ttl
            )
            sosDao?.insertSos(entity)
        } catch (_: Exception) {}

        // 3. Save to Store-and-Forward Mesh Queue
        try {
            val meshEntity = MeshPacketEntity(
                packetId = packet.packetId,
                sosId = sos.sosId,
                originDeviceId = packet.originDeviceId,
                originTimestamp = packet.originTimestamp,
                ttl = packet.ttl,
                hopCount = packet.hopCount,
                relayRoute = packet.relayRoute.joinToString(","),
                payloadJson = packet.toJson(),
                status = "RELAYED"
            )
            meshPacketDao?.insertPacket(meshEntity)
        } catch (_: Exception) {}

        // 4. Autonomous Gateway Uplink Check: Does THIS device have internet connectivity?
        val isOnline = networkObserver?.isCurrentlyConnected() == true
        if (isOnline && remoteSosRepo != null) {
            // THIS PHONE IS THE INTERNET GATEWAY! Transmit directly to OSDMA / EOC Server!
            addLog(
                eventType = "GATEWAY_UPLINK",
                message = "🌐 GATEWAY DETECTED: This device has internet! Transmitting SOS #$sosShortId to OSDMA / EOC Disaster Command...",
                hopCount = packet.hopCount,
                ttl = packet.ttl,
                sosId = sos.sosId
            )

            val uplinkResult = remoteSosRepo.submitSos(sos)
            if (uplinkResult.isSuccess) {
                sosDao?.updateDeliveryState(sos.sosId, DeliveryState.SERVER_RECEIVED)
                meshPacketDao?.updatePacketRelayStatus(packet.packetId, "DELIVERED_TO_GATEWAY", System.currentTimeMillis(), 0)

                showMainThreadToast("✅ PRANSETU: SOS #$sosShortId Transmitted to OSDMA / EOC Command Platform via Internet Gateway!")
                showEmergencyNotification(
                    title = "🌐 OSDMA / EOC GATEWAY UPLINK SUCCESS",
                    message = "Successfully delivered SOS #$sosShortId to Odisha State Disaster Management Authority (OSDMA) / EOC Platform via Cloud Gateway."
                )

                addLog(
                    eventType = "GATEWAY_SUCCESS",
                    message = "✅ GATEWAY UPLINK COMPLETE: SOS #$sosShortId delivered to OSDMA / EOC Server! Broadcasting ACK to mesh.",
                    hopCount = packet.hopCount,
                    ttl = packet.ttl,
                    sosId = sos.sosId
                )

                // Broadcast SOS_ACK back to the mesh network
                val ackPacket = RelayPacket(
                    packetType = RelayPacketType.SOS_ACK,
                    originDeviceId = myDeviceName,
                    originTimestamp = System.currentTimeMillis(),
                    ttl = 8,
                    hopCount = 0,
                    relayRoute = listOf(myDeviceName),
                    payload = sos.copy(deliveryState = DeliveryState.SERVER_RECEIVED.name)
                )
                broadcastBytes(ackPacket.toJson().toByteArray(Charsets.UTF_8), excludeEndpoint = senderEndpointId)
                return
            }
        }

        // 5. Zero-Cellular Multi-Hop Forwarding: Decrement TTL, Increment Hop, Re-broadcast to ALL OTHER in-range peers!
        if (packet.ttl > 1) {
            val forwardPacket = packet.createForwardPacket(myDeviceName)
            val forwardBytes = forwardPacket.toJson().toByteArray(Charsets.UTF_8)
            val targetCount = (connectedPeers.size - 1).coerceAtLeast(0)

            addLog(
                eventType = "PACKET_FORWARDED",
                message = "🔄 MULTI-HOP FORWARD: Re-broadcasting SOS #$sosShortId to $targetCount in-range device(s) (Next TTL: ${forwardPacket.ttl}, Hop: ${forwardPacket.hopCount})",
                hopCount = forwardPacket.hopCount,
                ttl = forwardPacket.ttl,
                sosId = sos.sosId
            )

            broadcastBytes(forwardBytes, excludeEndpoint = senderEndpointId)
            meshPacketDao?.updatePacketRelayStatus(packet.packetId, "RELAYED", System.currentTimeMillis(), connectedPeers.size)
        } else {
            addLog("TTL_EXPIRED", "⚠️ SOS #$sosShortId reached maximum TTL limit (0 hops remaining). Stored locally in queue.")
        }
    }

    private suspend fun handleIncomingSosAck(senderEndpointId: String, ackPacket: RelayPacket) {
        val sosId = ackPacket.payload.sosId
        val sosShortId = sosId.take(8)

        addLog(
            eventType = "ACK_RECEIVED",
            message = "🎯 ACK RECEIVED: Confirmation from Gateway that SOS #$sosShortId reached OSDMA / EOC Command Platform!",
            sosId = sosId
        )

        // Mark as acknowledged in local databases
        sosDao?.markAcknowledged(sosId, System.currentTimeMillis())
        meshPacketDao?.markSosAcknowledged(sosId)

        // Rebroadcast ACK if TTL > 0 so other intermediate nodes know the SOS is safe
        if (ackPacket.ttl > 1) {
            val fwdAck = ackPacket.createForwardPacket(myDeviceName)
            broadcastBytes(fwdAck.toJson().toByteArray(Charsets.UTF_8), excludeEndpoint = senderEndpointId)
        }
    }

    private suspend fun handleIncomingFamilySafeUpdate(senderEndpointId: String, packet: RelayPacket) {
        val sos = packet.payload
        val citizenName = sos.userName ?: packet.originDeviceId
        val citizenPhone = sos.userPhone ?: ""
        val lat = sos.latitude
        val lon = sos.longitude
        val locName = if (lat != null && lon != null) "GPS: %.4f°, %.4f°".format(lat, lon) else "Verified Safe Zone"
        val senderName = endpointTrueNames[senderEndpointId] ?: "Nearby Device"

        // 1. High-Priority Heads-Up Family Notification & Audible Alert on THIS device
        showEmergencyNotification(
            title = "💚 Family Member Safe: $citizenName",
            message = "$citizenName has marked themselves as SAFE!\nLocation: $locName"
        )
        showMainThreadToast("💚 FAMILY UPDATE: $citizenName is SAFE at $locName!")

        // 2. Real-Time Activity Log
        addLog(
            eventType = "FAMILY_SAFE",
            message = "💚 FAMILY SAFE CHECK-IN: $citizenName marked SAFE ($locName) via $senderName",
            hopCount = packet.hopCount,
            ttl = packet.ttl,
            sosId = sos.sosId
        )

        // 3. Update local database for Family Circle UI
        try {
            familyDao?.updateMemberByContact(
                name = citizenName,
                phoneNumber = citizenPhone,
                status = "SAFE",
                locationName = locName,
                lat = lat,
                lon = lon,
                timestamp = sos.locationTimestamp ?: System.currentTimeMillis(),
                batteryPercent = sos.batteryPercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update familyDao on SAFE check-in", e)
        }

        // 4. Multi-hop forward to other peers in range so the whole family network gets informed
        if (packet.ttl > 1) {
            val fwd = packet.createForwardPacket(myDeviceName)
            broadcastBytes(fwd.toJson().toByteArray(Charsets.UTF_8), excludeEndpoint = senderEndpointId)
        }
    }

    // --- Broadcast SOS to All In-Range Peers ---

    /**
     * Broadcasts a "I AM SAFE" status update over the offline mesh to all nearby family devices.
     */
    fun broadcastFamilySafeUpdate(safeModel: SosCanonicalModel) {
        coroutineScope.launch {
            val packet = RelayPacket(
                packetType = RelayPacketType.FAMILY_SAFE_UPDATE,
                originDeviceId = myDeviceName,
                originTimestamp = System.currentTimeMillis(),
                ttl = 8,
                hopCount = 0,
                relayRoute = listOf(myDeviceName),
                payload = safeModel
            )

            val packetBytes = packet.toJson().toByteArray(Charsets.UTF_8)
            relayValidator.markSeen(safeModel.sosId)

            val peerCount = connectedPeers.size
            if (peerCount > 0) {
                showMainThreadToast("💚 Family Status Broadcasted to $peerCount in-range device(s) via Mesh!")
                addLog(
                    eventType = "FAMILY_SAFE_ORIGIN",
                    message = "💚 FAMILY STATUS BROADCAST: Broadcasted 'I AM SAFE' status to $peerCount nearby device(s) over Mesh.",
                    sosId = safeModel.sosId
                )
                broadcastBytes(packetBytes)
            } else {
                showMainThreadToast("💚 'I AM SAFE' recorded. Scanning for nearby family devices to auto-sync...")
                addLog(
                    eventType = "FAMILY_SAFE_ORIGIN",
                    message = "💚 'I AM SAFE' saved. Waiting for in-range family devices to auto-sync.",
                    sosId = safeModel.sosId
                )
            }
        }
    }

    /**
     * Called when the citizen triggers an SOS on THIS device (Origin Node).
     */
    fun broadcastOriginSos(sosModel: SosCanonicalModel) {
        coroutineScope.launch {
            val packet = RelayPacket(
                packetType = RelayPacketType.SOS_ALERT,
                originDeviceId = myDeviceName,
                originTimestamp = System.currentTimeMillis(),
                ttl = 8,
                hopCount = 0,
                relayRoute = listOf(myDeviceName),
                payload = sosModel
            )

            val packetBytes = packet.toJson().toByteArray(Charsets.UTF_8)
            relayValidator.markSeen(sosModel.sosId)

            // Save in local Store-and-Forward Mesh Queue
            try {
                val entity = MeshPacketEntity(
                    packetId = packet.packetId,
                    sosId = sosModel.sosId,
                    originDeviceId = myDeviceName,
                    originTimestamp = packet.originTimestamp,
                    ttl = packet.ttl,
                    hopCount = 0,
                    relayRoute = myDeviceName,
                    payloadJson = packet.toJson(),
                    status = "PENDING_FORWARD"
                )
                meshPacketDao?.insertPacket(entity)
            } catch (_: Exception) {}

            val peerCount = connectedPeers.size
            if (peerCount > 0) {
                showMainThreadToast("🚨 PRANSETU SOS TRANSMITTED to $peerCount in-range device(s) via Bluetooth & Wi-Fi Direct!")
                addLog(
                    eventType = "SOS_ORIGINATED",
                    message = "🚨 SOS ORIGINATED: Transmitting over Zero-Cellular Mesh to $peerCount in-range real device(s) via Bluetooth & Wi-Fi Direct.",
                    hopCount = 0,
                    ttl = 8,
                    sosId = sosModel.sosId
                )
                broadcastBytes(packetBytes)
            } else {
                showMainThreadToast("📦 ZERO CELLULAR & NO PEERS: SOS saved to offline Store-and-Forward queue. Scanning for nearby devices...")
                addLog(
                    eventType = "SOS_ORIGINATED",
                    message = "📦 ZERO CELLULAR & NO PEERS: SOS #${sosModel.sosId.take(8)} saved to local queue. Will auto-flood the moment any device comes in range.",
                    sosId = sosModel.sosId
                )
            }
        }
    }

    private fun broadcastBytes(bytes: ByteArray, excludeEndpoint: String? = null) {
        val targets = connectedPeers.keys.filter { it != excludeEndpoint }
        if (targets.isEmpty()) return

        try {
            val payload = Payload.fromBytes(bytes)
            getClient()?.sendPayload(targets, payload)?.addOnFailureListener {
                Log.e(TAG, "Payload broadcast failed to $targets", it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending payload", e)
        }
    }

    /**
     * Flushes all un-acknowledged SOS packets in the Room queue to a newly connected device.
     */
    private fun flushPendingStoreAndForwardQueueToPeer(endpointId: String, deviceName: String) {
        coroutineScope.launch {
            try {
                val pendingList = meshPacketDao?.getPendingForwardPackets() ?: emptyList()
                if (pendingList.isNotEmpty()) {
                    addLog(
                        eventType = "QUEUE_FLUSH",
                        message = "📦 STORE-AND-FORWARD: Auto-flushing ${pendingList.size} pending offline SOS packet(s) to real device: $deviceName"
                    )

                    for (item in pendingList) {
                        val packet = RelayPacket.fromJson(item.payloadJson) ?: continue
                        if (packet.ttl > 0) {
                            val forwardPacket = packet.createForwardPacket(myDeviceName)
                            val bytes = forwardPacket.toJson().toByteArray(Charsets.UTF_8)
                            val payload = Payload.fromBytes(bytes)
                            getClient()?.sendPayload(endpointId, payload)
                            delay(200) // Small delay between packets to prevent buffer overflow
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing pending queue", e)
            }
        }
    }

    fun broadcastMessage(bytes: ByteArray) {
        broadcastBytes(bytes)
    }
}
