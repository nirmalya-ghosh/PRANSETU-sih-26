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
    private val seenAlertIds = mutableSetOf<String>()
    
    fun pollForAlerts(intervalMs: Long = 2500L): Flow<SystemAlert> = flow {
        Log.d(TAG, "Starting System Alert Polling (Interval: ${intervalMs}ms)...")
        
        while (true) {
            try {
                // 1. Check realtime_events table for emergency broadcasts
                val realtimeResult = supabase.get(
                    "realtime_events", 
                    "event_type=eq.EMERGENCY_DISASTER_BROADCAST&order=created_at.desc&limit=3"
                )
                
                if (realtimeResult.isSuccess) {
                    val eventsArray = JSONArray(realtimeResult.getOrNull() ?: "[]")
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)
                        val id = eventObj.optString("id", eventObj.optString("campaign_id", ""))
                        
                        if (id.isNotEmpty() && !seenAlertIds.contains(id)) {
                            val payload = eventObj.optJSONObject("payload") ?: JSONObject()
                            val text = payload.optString(
                                "disaster_text", 
                                eventObj.optString("notes", "CRITICAL EMERGENCY BROADCAST: Immediate disaster evacuation ordered.")
                            )
                            val severity = payload.optString("severity", "RED_CRITICAL")
                            val severityCode = when (severity) {
                                "RED_CRITICAL" -> 5
                                "ORANGE_WARNING" -> 4
                                else -> 3
                            }
                            
                            seenAlertIds.add(id)
                            emit(
                                SystemAlert(
                                    sosId = id,
                                    message = text,
                                    severityCode = severityCode,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // 2. Also check sos_events table for system alert broadcasts
                val sosResult = supabase.get(
                    "sos_events", 
                    "source=eq.SYSTEM_ALERT&order=created_at.desc&limit=3"
                )
                
                if (sosResult.isSuccess) {
                    val sosArray = JSONArray(sosResult.getOrNull() ?: "[]")
                    for (i in 0 until sosArray.length()) {
                        val obj = sosArray.getJSONObject(i)
                        val id = obj.optString("sosId", obj.optString("id", ""))
                        
                        if (id.isNotEmpty() && !seenAlertIds.contains(id)) {
                            val text = obj.optString("message", obj.optString("notes", "Critical emergency alert issued."))
                            val severityCode = obj.optInt("severityCode", 5)
                            
                            seenAlertIds.add(id)
                            emit(
                                SystemAlert(
                                    sosId = id,
                                    message = text,
                                    severityCode = severityCode,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error polling system alerts from Supabase", e)
            }
            
            delay(intervalMs)
        }
    }
}
