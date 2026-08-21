package com.pransetu.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pransetu.app.core.sos.DeliveryState

/**
 * Room entity for locally persisted SOS records.
 * 
 * This is the offline-first source of truth. Every SOS is written here
 * before any transmission attempt. The [deliveryState] column tracks
 * lifecycle progress so the UI can show deterministic status.
 */
@Entity(tableName = "sos_records")
data class SosEntity(
    @PrimaryKey
    val sosId: String,

    /** Protocol version for forward compatibility */
    val protocolVersion: Int = 1,

    /** Epoch millis when the SOS was created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Source device identifier */
    val sourceDeviceId: String = "",

    // --- Location ---
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationTimestamp: Long = 0L,
    val locationAccuracy: Float = 0f,

    // --- Severity & Details ---
    val severityCode: String = "CRITICAL",
    val emergencyType: String = "OTHER",
    val peopleCount: Int = 1,
    val medicalRequired: Boolean = false,

    // --- Relay ---
    val hopCount: Int = 0,
    val ttl: Int = 5,

    // --- Lifecycle ---
    val deliveryState: DeliveryState = DeliveryState.CREATED,
    val retryCount: Int = 0,
    val lastRetryAt: Long = 0L,

    /** Epoch millis when an acknowledgement was received (0 = not yet) */
    val acknowledgedAt: Long = 0L,

    /** Free-text user message (original language preserved) */
    val userMessage: String = "",

    /** Language code of the user at SOS creation time */
    val userLanguage: String = "en",

    // --- Identity ---
    val userName: String? = null,
    val userPhone: String? = null,
    val userEmail: String? = null
)
