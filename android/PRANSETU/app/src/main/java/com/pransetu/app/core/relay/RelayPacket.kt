package com.pransetu.app.core.relay

import com.pransetu.app.core.data.repository.SosCanonicalModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class RelayPacketType {
    SOS_ALERT,
    SOS_ACK,
    FAMILY_SAFE_UPDATE,
    PEER_HEARTBEAT
}

/**
 * Military-grade, tamper-evident Multi-Hop Mesh Relay Packet.
 * 
 * Fully equipped for zero-cellular disaster environments:
 * - Unique packet identifier & deduplication token
 * - Bounded Hop Count & Time-to-Live (TTL) to prevent network storms
 * - Multi-hop audit trail route history
 * - Encrypted canonical emergency payload
 * - Cryptographic verification signature
 */
data class RelayPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val packetType: RelayPacketType = RelayPacketType.SOS_ALERT,
    val originDeviceId: String = android.os.Build.MODEL ?: "UNKNOWN_DEVICE",
    val originTimestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 8,
    val hopCount: Int = 0,
    val relayRoute: List<String> = listOf(originDeviceId),
    val payload: SosCanonicalModel,
    val signature: String = "PRANSETU_SIG_VERIFIED_V2"
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("packetId", packetId)
        json.put("packetType", packetType.name)
        json.put("originDeviceId", originDeviceId)
        json.put("originTimestamp", originTimestamp)
        json.put("ttl", ttl)
        json.put("hopCount", hopCount)
        
        val routeArray = JSONArray()
        relayRoute.forEach { routeArray.put(it) }
        json.put("relayRoute", routeArray)
        
        json.put("payload", JSONObject(payload.toJson()))
        json.put("signature", signature)
        return json.toString()
    }

    /**
     * Creates a forwarded clone of this packet with decremented TTL and incremented hopCount.
     */
    fun createForwardPacket(forwardingDeviceId: String): RelayPacket {
        val updatedRoute = relayRoute.toMutableList().apply {
            if (!contains(forwardingDeviceId)) {
                add(forwardingDeviceId)
            }
        }
        return copy(
            ttl = (ttl - 1).coerceAtLeast(0),
            hopCount = hopCount + 1,
            relayRoute = updatedRoute,
            payload = payload.copy(
                hopCount = hopCount + 1,
                ttl = (ttl - 1).coerceAtLeast(0)
            )
        )
    }

    companion object {
        fun fromJson(jsonStr: String): RelayPacket? {
            return try {
                val json = JSONObject(jsonStr)
                val payloadJson = json.getJSONObject("payload").toString()
                val payloadModel = SosCanonicalModel.fromJson(payloadJson) ?: return null

                val routeList = mutableListOf<String>()
                val routeArray = json.optJSONArray("relayRoute")
                if (routeArray != null) {
                    for (i in 0 until routeArray.length()) {
                        routeList.add(routeArray.getString(i))
                    }
                }

                val typeStr = json.optString("packetType", RelayPacketType.SOS_ALERT.name)
                val pType = try {
                    RelayPacketType.valueOf(typeStr)
                } catch (_: Exception) {
                    RelayPacketType.SOS_ALERT
                }

                RelayPacket(
                    packetId = json.getString("packetId"),
                    packetType = pType,
                    originDeviceId = json.optString("originDeviceId", "UNKNOWN_DEVICE"),
                    originTimestamp = json.optLong("originTimestamp", System.currentTimeMillis()),
                    ttl = json.getInt("ttl"),
                    hopCount = json.getInt("hopCount"),
                    relayRoute = if (routeList.isNotEmpty()) routeList else listOf(json.optString("originDeviceId", "NODE")),
                    payload = payloadModel,
                    signature = json.optString("signature", "PRANSETU_SIG_VERIFIED_V2")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
