package com.pransetu.app.core.data.repository

import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import org.json.JSONArray
import org.json.JSONObject

class SupabaseSosRepository(
    private val supabase: SupabaseClient = SupabaseClient
) : SosRepository {

    private val TAG = "SupabaseSosRepository"

    override suspend fun hasSos(sosId: String): Boolean {
        return try {
            val result = supabase.get("sos_events", "sosId=eq.$sosId&select=sosId")
            if (result.isSuccess) {
                val jsonArray = JSONArray(result.getOrNull() ?: "[]")
                jsonArray.length() > 0
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking SOS existence in Supabase", e)
            false
        }
    }

    override suspend fun submitSos(sos: SosCanonicalModel): Result<Unit> {
        return try {
            if (sos.ttl <= 0) return Result.failure(Exception("TTL expired"))

            val payload = JSONObject().apply {
                put("sosId", sos.sosId)
                put("protocolVersion", sos.protocolVersion)
                put("createdAt", sos.createdAt)
                put("source", sos.source)
                put("deviceIdentifier", sos.deviceIdentifier)
                sos.latitude?.let { put("latitude", it) }
                sos.longitude?.let { put("longitude", it) }
                sos.locationTimestamp?.let { put("locationTimestamp", it) }
                sos.locationAccuracy?.let { put("locationAccuracy", it.toDouble()) }
                put("severityCode", sos.severityCode)
                put("peopleCount", sos.peopleCount)
                put("medicalRequired", sos.medicalRequired)
                put("hopCount", sos.hopCount)
                put("ttl", sos.ttl)
                put("deliveryState", "SERVER_RECEIVED")
                sos.message?.let { put("message", it) }
                sos.userName?.let { put("userName", it) }
                sos.userPhone?.let { put("userPhone", it) }
                sos.userEmail?.let { put("userEmail", it) }
            }

            val postResult = supabase.post("sos_events", payload.toString())
            if (postResult.isSuccess) {
                Log.d(TAG, "Successfully submitted SOS ${sos.sosId} to Supabase PostgreSQL!")
                Result.success(Unit)
            } else {
                val err = postResult.exceptionOrNull()
                Log.w(TAG, "Supabase SOS submission issue: ${err?.message}")
                // If it was already received, return success so retries are bounded
                if (err?.message?.contains("duplicate", ignoreCase = true) == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(err ?: Exception("Unknown Supabase error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit SOS to Supabase", e)
            Result.failure(e)
        }
    }
}
