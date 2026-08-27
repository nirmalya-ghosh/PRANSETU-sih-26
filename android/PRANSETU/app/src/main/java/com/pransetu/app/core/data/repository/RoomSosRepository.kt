package com.pransetu.app.core.data.repository

import com.pransetu.app.core.data.local.SosDao
import com.pransetu.app.core.data.local.SosEntity
import com.pransetu.app.core.sos.DeliveryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Offline-first SOS repository.
 *
 * Write path: Room (local) first → then attempt Supabase sync.
 * Read path: Always from Room.
 *
 * This ensures SOS records survive even when there is no connectivity.
 * The [SupabaseSosRepository] is used as a secondary sync target when online.
 */
class RoomSosRepository(
    private val sosDao: SosDao,
    private val remoteRepo: SosRepository = SupabaseSosRepository()
) : SosRepository {

    /**
     * Submits an SOS: persists locally first, then attempts remote sync.
     * Never claims delivery without explicit acknowledgement.
     */
    override suspend fun submitSos(sos: SosCanonicalModel): Result<Unit> = withContext(Dispatchers.IO) {
        val eventManager = com.pransetu.app.core.network.events.EventManager.get()
        try {
            // 1. Emit SOS_CREATED & SOS_SAVED_LOCALLY
            eventManager?.recordEvent(
                eventType = "SOS_CREATED",
                sosId = sos.sosId,
                userId = sos.userEmail,
                deviceId = sos.deviceIdentifier,
                payload = org.json.JSONObject().apply {
                    put("latitude", sos.latitude)
                    put("longitude", sos.longitude)
                    put("severity_code", sos.severityCode)
                    put("people_count", sos.peopleCount)
                    put("medical_required", sos.medicalRequired)
                    put("battery_percent", sos.batteryPercent)
                },
                priority = 3
            )

            // Convert to entity and persist locally FIRST
            val entity = sos.toEntity().copy(deliveryState = DeliveryState.STORED)
            sosDao.insertSos(entity)

            eventManager?.recordEvent(
                eventType = "SOS_SAVED_LOCALLY",
                sosId = sos.sosId,
                payload = org.json.JSONObject().apply {
                    put("storage_engine", "ROOM_SQLITE")
                },
                priority = 2
            )

            // 2. Attempt remote sync (non-blocking for the user)
            try {
                eventManager?.recordEvent(
                    eventType = "SOS_UPLOAD_STARTED",
                    sosId = sos.sosId,
                    payload = org.json.JSONObject().apply {
                        put("endpoint", "supabase/sos_events")
                    },
                    priority = 2
                )

                val result = remoteRepo.submitSos(sos)
                if (result.isSuccess) {
                    sosDao.updateDeliveryState(sos.sosId, DeliveryState.SERVER_RECEIVED)
                    eventManager?.recordEvent(
                        eventType = "SOS_BACKEND_RECEIVED",
                        sosId = sos.sosId,
                        priority = 3
                    )
                } else {
                    // Remote failed — stays STORED, will retry later
                    sosDao.updateDeliveryState(sos.sosId, DeliveryState.QUEUED)
                }
            } catch (_: Exception) {
                // Network error — mark as queued for retry
                sosDao.updateDeliveryState(sos.sosId, DeliveryState.QUEUED)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasSos(sosId: String): Boolean = withContext(Dispatchers.IO) {
        sosDao.hasSos(sosId)
    }

    override suspend fun getSosStatus(sosId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val entity = sosDao.getSosById(sosId)
            if (entity != null) {
                Result.success(entity.deliveryState.name)
            } else {
                Result.failure(Exception("SOS not found in local DB"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Extended operations ---

    suspend fun getSosById(sosId: String): SosEntity? = withContext(Dispatchers.IO) {
        sosDao.getSosById(sosId)
    }

    fun observeAllSos(): Flow<List<SosEntity>> {
        return sosDao.observeAllSos()
    }

    fun observePendingSos(): Flow<List<SosEntity>> {
        return sosDao.observeSosByStates(
            listOf(DeliveryState.STORED, DeliveryState.QUEUED, DeliveryState.RELAYING, DeliveryState.FAILED_RETRYING)
        )
    }

    suspend fun updateDeliveryState(sosId: String, newState: DeliveryState) = withContext(Dispatchers.IO) {
        sosDao.updateDeliveryState(sosId, newState)
    }

    suspend fun markAcknowledged(sosId: String) = withContext(Dispatchers.IO) {
        sosDao.markAcknowledged(sosId, System.currentTimeMillis())
    }

    suspend fun syncSosStatus(sosId: String) = withContext(Dispatchers.IO) {
        val result = remoteRepo.getSosStatus(sosId)
        if (result.isSuccess) {
            val statusStr = result.getOrNull()
            if (statusStr != null) {
                try {
                    val newState = DeliveryState.valueOf(statusStr)
                    sosDao.updateDeliveryState(sosId, newState)
                    if (newState == DeliveryState.ACKNOWLEDGED || newState == DeliveryState.CLOSED) {
                        sosDao.markAcknowledged(sosId, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
    }

    /**
     * Retries sending all pending SOS records to the backend.
     * Called by WorkManager or when connectivity is restored.
     */
    suspend fun retryPendingSos(): Int = withContext(Dispatchers.IO) {
        val pending = sosDao.getPendingSos(maxRetries = 5)
        var successCount = 0

        for (entity in pending) {
            try {
                val model = entity.toCanonicalModel()
                val result = remoteRepo.submitSos(model)
                if (result.isSuccess) {
                    sosDao.updateDeliveryState(entity.sosId, DeliveryState.SERVER_RECEIVED)
                    successCount++
                } else {
                    sosDao.incrementRetry(entity.sosId, DeliveryState.FAILED_RETRYING, System.currentTimeMillis())
                }
            } catch (_: Exception) {
                sosDao.incrementRetry(entity.sosId, DeliveryState.FAILED_RETRYING, System.currentTimeMillis())
            }
        }

        successCount
    }
}

// --- Extension functions for conversion ---

fun SosCanonicalModel.toEntity(): SosEntity {
    return SosEntity(
        sosId = sosId,
        protocolVersion = protocolVersion.toIntOrNull() ?: 1,
        createdAt = createdAt,
        sourceDeviceId = deviceIdentifier,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        locationTimestamp = locationTimestamp ?: 0L,
        locationAccuracy = locationAccuracy ?: 0f,
        severityCode = when (severityCode) {
            3 -> "CRITICAL"
            2 -> "HIGH"
            1 -> "MEDIUM"
            else -> "LOW"
        },
        peopleCount = peopleCount,
        medicalRequired = medicalRequired,
        hopCount = hopCount,
        ttl = ttl,
        deliveryState = try { DeliveryState.valueOf(deliveryState) } catch (_: Exception) { DeliveryState.CREATED },
        userMessage = message ?: "",
        userName = userName,
        userPhone = userPhone,
        userEmail = userEmail
    )
}

fun SosEntity.toCanonicalModel(): SosCanonicalModel {
    return SosCanonicalModel(
        sosId = sosId,
        protocolVersion = protocolVersion.toString(),
        createdAt = createdAt,
        deviceIdentifier = sourceDeviceId,
        latitude = if (latitude != 0.0) latitude else null,
        longitude = if (longitude != 0.0) longitude else null,
        locationTimestamp = if (locationTimestamp != 0L) locationTimestamp else null,
        locationAccuracy = if (locationAccuracy != 0f) locationAccuracy else null,
        severityCode = when (severityCode) {
            "CRITICAL" -> 3
            "HIGH" -> 2
            "MEDIUM" -> 1
            else -> 0
        },
        peopleCount = peopleCount,
        medicalRequired = medicalRequired,
        hopCount = hopCount,
        ttl = ttl,
        deliveryState = deliveryState.name,
        message = if (userMessage.isNotEmpty()) userMessage else null,
        userName = userName,
        userPhone = userPhone,
        userEmail = userEmail
    )
}
