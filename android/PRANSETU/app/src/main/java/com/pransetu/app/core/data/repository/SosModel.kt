package com.pransetu.app.core.data.repository


import org.json.JSONObject
import java.util.UUID

data class SosCanonicalModel(
    val sosId: String = UUID.randomUUID().toString(),
    val protocolVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "android_app",
    val deviceIdentifier: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationTimestamp: Long? = null,
    val locationAccuracy: Float? = null,
    val severityCode: Int = 1, // 1 = standard emergency
    val peopleCount: Int = 1,
    val medicalRequired: Boolean = false,
    val hopCount: Int = 0,
    val ttl: Int = 64,
    val deliveryState: String = "CREATED",
    val message: String? = null, // For Voice-to-Text SOS
    val userName: String? = null,
    val userPhone: String? = null,
    val userEmail: String? = null,
    val batteryPercent: Int? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("sosId", sosId)
        json.put("protocolVersion", protocolVersion)
        json.put("createdAt", createdAt)
        json.put("source", source)
        json.put("deviceIdentifier", deviceIdentifier)
        latitude?.let { json.put("latitude", it) }
        longitude?.let { json.put("longitude", it) }
        locationTimestamp?.let { json.put("locationTimestamp", it) }
        locationAccuracy?.let { json.put("locationAccuracy", it) }
        json.put("severityCode", severityCode)
        json.put("peopleCount", peopleCount)
        json.put("medicalRequired", medicalRequired)
        json.put("hopCount", hopCount)
        json.put("ttl", ttl)
        json.put("deliveryState", deliveryState)
        message?.let { json.put("message", it) }
        userName?.let { json.put("userName", it) }
        userPhone?.let { json.put("userPhone", it) }
        userEmail?.let { json.put("userEmail", it) }
        batteryPercent?.let { json.put("batteryPercent", it) }
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): SosCanonicalModel? {
            return try {
                val json = JSONObject(jsonString)
                SosCanonicalModel(
                    sosId = json.getString("sosId"),
                    protocolVersion = json.optString("protocolVersion", "1.0"),
                    createdAt = json.getLong("createdAt"),
                    source = json.optString("source", "android_app"),
                    deviceIdentifier = json.optString("deviceIdentifier", ""),
                    latitude = if (json.has("latitude")) json.getDouble("latitude") else null,
                    longitude = if (json.has("longitude")) json.getDouble("longitude") else null,
                    locationTimestamp = if (json.has("locationTimestamp")) json.getLong("locationTimestamp") else null,
                    locationAccuracy = if (json.has("locationAccuracy")) json.getDouble("locationAccuracy").toFloat() else null,
                    severityCode = json.optInt("severityCode", 1),
                    peopleCount = json.optInt("peopleCount", 1),
                    medicalRequired = json.optBoolean("medicalRequired", false),
                    hopCount = json.optInt("hopCount", 0),
                    ttl = json.optInt("ttl", 64),
                    deliveryState = json.optString("deliveryState", "CREATED"),
                    message = if (json.has("message")) json.getString("message") else null,
                    userName = if (json.has("userName")) json.getString("userName") else null,
                    userPhone = if (json.has("userPhone")) json.getString("userPhone") else null,
                    userEmail = if (json.has("userEmail")) json.getString("userEmail") else null,
                    batteryPercent = if (json.has("batteryPercent")) json.getInt("batteryPercent") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

interface SosRepository {
    suspend fun submitSos(sos: SosCanonicalModel): Result<Unit>
    suspend fun hasSos(sosId: String): Boolean
}


