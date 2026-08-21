package com.pransetu.app.core.relay

import java.util.concurrent.ConcurrentHashMap

/**
 * Validates incoming RelayPackets across the offline mesh network.
 * 
 * Enforces:
 * 1. Payload validity (non-empty SOS ID)
 * 2. Non-zero TTL (Time-To-Live)
 * 3. Maximum Hop Count Boundary (prevents infinite echoing)
 * 4. Maximum Packet Age (48 hours)
 * 5. Replay Protection & Deduplication
 */
class RelayValidator {

    private val seenPacketIds = ConcurrentHashMap.newKeySet<String>()
    private val seenSosIds = ConcurrentHashMap<String, Long>() // sosId -> timestamp

    private val MAX_HOPS = 12
    private val MAX_PACKET_AGE_MS = 48 * 60 * 60 * 1000L // 48 Hours

    fun validate(packet: RelayPacket): Boolean {
        // Check 1: Must have a valid SOS ID
        if (packet.payload.sosId.isBlank()) return false

        // Check 2: TTL must be strictly positive
        if (packet.ttl <= 0) return false

        // Check 3: Hop count must not exceed maximum hop perimeter
        if (packet.hopCount >= MAX_HOPS) return false

        // Check 4: Packet must not be expired
        val age = System.currentTimeMillis() - packet.originTimestamp
        if (age < 0 || age > MAX_PACKET_AGE_MS) return false

        // Check 5: Replay & Deduplication Protection
        if (seenPacketIds.contains(packet.packetId)) {
            return false
        }

        // Check 6: Check if SOS ID is already seen recently
        val lastSeen = seenSosIds[packet.payload.sosId]
        if (lastSeen != null && System.currentTimeMillis() - lastSeen < 10_000L) {
            // Already received this exact SOS in last 10 seconds (immediate broadcast collision)
            return false
        }

        // Mark as seen
        seenPacketIds.add(packet.packetId)
        seenSosIds[packet.payload.sosId] = System.currentTimeMillis()

        // Clean cache if too large
        if (seenPacketIds.size > 2000) {
            seenPacketIds.clear()
        }

        return verifySignature(packet)
    }

    private fun verifySignature(packet: RelayPacket): Boolean {
        return packet.signature.isNotBlank()
    }

    fun markSeen(sosId: String) {
        seenSosIds[sosId] = System.currentTimeMillis()
    }
}
