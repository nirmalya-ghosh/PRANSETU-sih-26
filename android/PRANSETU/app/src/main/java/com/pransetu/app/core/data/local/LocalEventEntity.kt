package com.pransetu.app.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local offline event entity.
 * Buffers operational events in SQLite before uploading to the central PRANSETU event bus.
 */
@Entity(
    tableName = "local_events",
    indices = [
        Index("syncState"),
        Index("occurredAt"),
        Index("eventType")
    ]
)
data class LocalEventEntity(
    @PrimaryKey
    val localEventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val eventVersion: Int = 1,
    val occurredAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val deviceId: String? = null,
    val sessionId: String? = null,
    val sosId: String? = null,
    val incidentId: String? = null,
    val campaignId: String? = null,
    val source: String = "android",
    val payloadJson: String = "{}",
    val syncState: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val attemptCount: Int = 0,
    val priority: Int = 1, // 1 = normal, 3 = high/SOS
    val createdAt: Long = System.currentTimeMillis()
)
