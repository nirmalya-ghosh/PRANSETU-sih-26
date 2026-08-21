package com.pransetu.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mesh_packets")
data class MeshPacketEntity(
    @PrimaryKey val packetId: String,
    val sosId: String,
    val originDeviceId: String,
    val originTimestamp: Long,
    val ttl: Int,
    val hopCount: Int,
    val relayRoute: String, // Comma separated: e.g. "Galaxy S22 -> Redmi Note 11"
    val payloadJson: String,
    val status: String, // "PENDING_FORWARD", "RELAYED", "DELIVERED_TO_GATEWAY", "ACKNOWLEDGED"
    val createdAt: Long = System.currentTimeMillis(),
    val lastForwardedAt: Long? = null,
    val forwardedToPeersCount: Int = 0
)

@Dao
interface MeshPacketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPacket(packet: MeshPacketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPackets(packets: List<MeshPacketEntity>)

    @Query("SELECT * FROM mesh_packets WHERE status = 'PENDING_FORWARD' OR status = 'RELAYED' ORDER BY originTimestamp ASC")
    fun getPendingForwardPackets(): List<MeshPacketEntity>

    @Query("SELECT * FROM mesh_packets ORDER BY createdAt DESC")
    fun observeAllMeshPackets(): Flow<List<MeshPacketEntity>>

    @Query("SELECT * FROM mesh_packets WHERE status = 'PENDING_FORWARD' ORDER BY createdAt DESC")
    fun observePendingMeshPackets(): Flow<List<MeshPacketEntity>>

    @Query("UPDATE mesh_packets SET status = :newStatus, lastForwardedAt = :forwardedAt, forwardedToPeersCount = forwardedToPeersCount + :peersIncrement WHERE packetId = :packetId")
    fun updatePacketRelayStatus(packetId: String, newStatus: String, forwardedAt: Long, peersIncrement: Int)

    @Query("UPDATE mesh_packets SET status = 'ACKNOWLEDGED' WHERE sosId = :sosId")
    fun markSosAcknowledged(sosId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM mesh_packets WHERE packetId = :packetId)")
    fun hasPacket(packetId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM mesh_packets WHERE sosId = :sosId)")
    fun hasSos(sosId: String): Boolean

    @Query("DELETE FROM mesh_packets WHERE createdAt < :beforeTimestamp")
    fun pruneOldPackets(beforeTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM mesh_packets")
    fun count(): Int
}
