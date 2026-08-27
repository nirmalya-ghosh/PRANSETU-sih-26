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
    
    fun pollForAlerts(intervalMs: Long = 2000L): Flow<SystemAlert> = flow {
        Log.d(TAG, "Starting System Alert Polling loop (Interval: ${intervalMs}ms)...")
        
        while (true) {
            try {
                // 1. Query realtime_events table for EMERGENCY_DISASTER_BROADCAST
                val realtimeResult = supabase.get(
                    "realtime_events", 
                    "event_type=eq.EMERGENCY_DISASTER_BROADCAST&order=created_at.desc&limit=3"
                )
                
                if (realtimeResult.isSuccess) {
                    val rawJson = realtimeResult.getOrNull() ?: "[]"
                    val eventsArray = JSONArray(rawJson)
                    
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)
                        
                        // Check all possible ID keys: event_id, id, campaign_id
                        val eventId = when {
                            eventObj.has("event_id") && eventObj.optString("event_id").isNotBlank() -> eventObj.optString("event_id")
                            eventObj.has("id") && eventObj.optString("id").isNotBlank() -> eventObj.optString("id")
                            eventObj.has("campaign_id") && eventObj.optString("campaign_id").isNotBlank() -> eventObj.optString("campaign_id")
                            else -> "EVENT-${eventObj.optString("created_at")}"
                        }
                        
                        if (eventId.isNotBlank() && !seenAlertIds.contains(eventId)) {
                            Log.d(TAG, "⚡ Found NEW EMERGENCY BROADCAST event: $eventId")
                            
                            val payload = eventObj.optJSONObject("payload") ?: JSONObject()
                            val text = when {
                                payload.has("disaster_text") && payload.optString("disaster_text").isNotBlank() -> payload.optString("disaster_text")
                                eventObj.has("notes") && eventObj.optString("notes").isNotBlank() -> eventObj.optString("notes")
                                eventObj.has("message") && eventObj.optString("message").isNotBlank() -> eventObj.optString("message")
                                else -> "CRITICAL EMERGENCY DISASTER ALERT: Immediate public evacuation and safety measures ordered by state authorities."
                            }
                            
                            val severity = payload.optString("severity", "RED_CRITICAL")
                            val severityCode = when (severity) {
                                "RED_CRITICAL" -> 5
                                "ORANGE_WARNING" -> 4
                                else -> 3
                            }
                            
                            seenAlertIds.add(eventId)
                            emit(
                                SystemAlert(
                                    sosId = eventId,
                                    message = text,
                                    severityCode = severityCode,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // 2. Query sos_events table for SYSTEM_ALERT
                val sosResult = supabase.get(
                    "sos_events", 
                    "source=eq.SYSTEM_ALERT&order=createdAt.desc&limit=3"
                )
                
                if (sosResult.isSuccess) {
                    val rawJson = sosResult.getOrNull() ?: "[]"
                    val sosArray = JSONArray(rawJson)
                    
                    for (i in 0 until sosArray.length()) {
                        val obj = sosArray.getJSONObject(i)
                        val id = when {
                            obj.has("sosId") && obj.optString("sosId").isNotBlank() -> obj.optString("sosId")
                            obj.has("id") && obj.optString("id").isNotBlank() -> obj.optString("id")
                            else -> ""
                        }
                        
                        if (id.isNotBlank() && !seenAlertIds.contains(id)) {
                            Log.d(TAG, "⚡ Found NEW SYSTEM_ALERT in sos_events: $id")
                            val text = when {
                                obj.has("message") && obj.optString("message").isNotBlank() -> obj.optString("message")
                                obj.has("notes") && obj.optString("notes").isNotBlank() -> obj.optString("notes")
                                else -> "Critical emergency alert issued by authorities."
                            }
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
                Log.e(TAG, "Error during emergency alert polling", e)
            }
            
            delay(intervalMs)
        }
    }
}
