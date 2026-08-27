package com.pransetu.app.core.network

import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

data class SystemAlert(
    val sosId: String,
    val message: String,
    val severityCode: Int,
    val createdAt: Long
)

class SystemAlertService(
    private val supabase: SupabaseClient = SupabaseClient
) {
    private val TAG = "SystemAlertService"
    
    fun pollForAlerts(intervalMs: Long = 10000L): Flow<SystemAlert> = flow {
        var lastCheckedTimeISO = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        
        Log.d(TAG, "Starting System Alert Polling. Initial time: $lastCheckedTimeISO")
        
        while (true) {
            try {
                val queryParams = "source=eq.SYSTEM_ALERT&created_at=gt.$lastCheckedTimeISO&order=created_at.asc"
                val result = supabase.get("sos_events", queryParams)
                
                if (result.isSuccess) {
                    val jsonArray = JSONArray(result.getOrNull() ?: "[]")
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val createdAtStr = obj.optString("created_at", "")
                        
                        var createdAtMillis = 0L
                        if (createdAtStr.isNotEmpty()) {
                            try {
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.parse(createdAtStr)
                                createdAtMillis = date?.time ?: 0L
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse date: $createdAtStr", e)
                            }
                        }

                        val alert = SystemAlert(
                            sosId = obj.optString("id", ""),
                            message = obj.optString("notes", "Public Safety Alert: High-priority disaster notification issued by state authorities. Please follow official safety advisories."),
                            severityCode = when (obj.optString("severity", "MEDIUM")) {
                                "CRITICAL" -> 5
                                "HIGH" -> 4
                                else -> 3
                            },
                            createdAt = createdAtMillis
                        )
                        
                        lastCheckedTimeISO = createdAtStr
                        emit(alert)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error polling system alerts", e)
            }
            
            delay(intervalMs)
        }
    }
}
