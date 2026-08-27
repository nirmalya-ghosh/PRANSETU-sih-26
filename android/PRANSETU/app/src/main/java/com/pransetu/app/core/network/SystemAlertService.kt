package com.pransetu.app.core.network

import android.content.Context
import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import com.pransetu.app.feature.alert.AlertViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SystemAlert(
    val sosId: String,
    val message: String,
    val severityCode: Int,
    val createdAt: Long
)

class SystemAlertService(
    private val context: Context,
    private val supabase: SupabaseClient = SupabaseClient
) {
    private val TAG = "SystemAlertService"
    private val alertStore = EmergencyAlertStore(context)
    private val seenSessionAlertIds = mutableSetOf<String>()
    
    // Valid time window: Only alerts dispatched in the last 3 minutes (180,000 ms) are active
    private val MAX_ALERT_AGE_MS = 180_000L

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseTimestampMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            val cleanStr = if (dateStr.length >= 19) dateStr.substring(0, 19) else dateStr
            isoFormat.parse(cleanStr)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun pollForAlerts(intervalMs: Long = 2000L): Flow<SystemAlert> = flow {
        Log.d(TAG, "Starting System Alert Polling (Safe Time-Window & Exact Message Extraction)...")
        
        while (true) {
            val now = System.currentTimeMillis()
            val cutoffTime = now - MAX_ALERT_AGE_MS
            val lastStandDownTime = alertStore.getLastStandDownTime()
            val effectiveCutoff = maxOf(cutoffTime, lastStandDownTime)

            try {
                // 1. Check for EMERGENCY_BROADCAST_CANCELLED / ALL-CLEAR
                val cancelResult = supabase.get(
                    "realtime_events",
                    "event_type=eq.EMERGENCY_BROADCAST_CANCELLED&order=created_at.desc&limit=1"
                )
                if (cancelResult.isSuccess) {
                    val cancelArray = JSONArray(cancelResult.getOrNull() ?: "[]")
                    if (cancelArray.length() > 0) {
                        val cancelObj = cancelArray.getJSONObject(0)
                        val cancelTimeStr = cancelObj.optString("created_at", cancelObj.optString("occurred_at", ""))
                        val cancelTime = parseTimestampMillis(cancelTimeStr)
                        
                        if (cancelTime > lastStandDownTime && (now - cancelTime) < MAX_ALERT_AGE_MS) {
                            Log.d(TAG, "🛑 ALL-CLEAR / STAND-DOWN received from State Authority! Silencing device.")
                            alertStore.recordStandDownTime(cancelTime)
                            AlertViewModel.globalDismiss()
                        }
                    }
                }

                // 2. Query realtime_events table for EMERGENCY_DISASTER_BROADCAST
                val realtimeResult = supabase.get(
                    "realtime_events", 
                    "event_type=eq.EMERGENCY_DISASTER_BROADCAST&order=created_at.desc&limit=3"
                )
                
                if (realtimeResult.isSuccess) {
                    val rawJson = realtimeResult.getOrNull() ?: "[]"
                    val eventsArray = JSONArray(rawJson)
                    
                    for (i in 0 until eventsArray.length()) {
                        val eventObj = eventsArray.getJSONObject(i)
                        
                        val eventId = when {
                            eventObj.has("event_id") && eventObj.optString("event_id").isNotBlank() -> eventObj.optString("event_id")
                            eventObj.has("id") && eventObj.optString("id").isNotBlank() -> eventObj.optString("id")
                            eventObj.has("campaign_id") && eventObj.optString("campaign_id").isNotBlank() -> eventObj.optString("campaign_id")
                            else -> ""
                        }
                        
                        if (eventId.isBlank()) continue

                        // Check if user already acknowledged this alert on disk
                        if (alertStore.isAlertAcknowledged(eventId) || seenSessionAlertIds.contains(eventId)) {
                            continue
                        }

                        // Check event timestamp age
                        val timeStr = eventObj.optString("created_at", eventObj.optString("occurred_at", ""))
                        val eventTime = parseTimestampMillis(timeStr)
                        
                        // Strict filter: Must be younger than 3 minutes and occurred after any all-clear stand-down
                        if (eventTime > 0L && eventTime < effectiveCutoff) {
                            continue
                        }

                        Log.d(TAG, "🚨 Active LIVE Emergency Broadcast detected: $eventId")
                        
                        val payloadObj = when (val p = eventObj.opt("payload")) {
                            is JSONObject -> p
                            is String -> try { JSONObject(p) } catch (_: Exception) { JSONObject() }
                            else -> JSONObject()
                        }

                        val text = when {
                            payloadObj.has("disaster_text") && payloadObj.optString("disaster_text").isNotBlank() -> payloadObj.optString("disaster_text")
                            payloadObj.has("message") && payloadObj.optString("message").isNotBlank() -> payloadObj.optString("message")
                            payloadObj.has("instructions") && payloadObj.optString("instructions").isNotBlank() -> payloadObj.optString("instructions")
                            eventObj.has("message") && eventObj.optString("message").isNotBlank() -> eventObj.optString("message")
                            eventObj.has("notes") && eventObj.optString("notes").isNotBlank() -> eventObj.optString("notes")
                            else -> "CRITICAL EMERGENCY DISASTER ALERT: Immediate public evacuation and safety measures ordered by state authorities."
                        }
                        
                        val severity = payloadObj.optString("severity", "RED_CRITICAL")
                        val severityCode = when (severity) {
                            "RED_CRITICAL" -> 5
                            "ORANGE_WARNING" -> 4
                            else -> 3
                        }
                        
                        seenSessionAlertIds.add(eventId)
                        emit(
                            SystemAlert(
                                sosId = eventId,
                                message = text,
                                severityCode = severityCode,
                                createdAt = if (eventTime > 0L) eventTime else now
                            )
                        )
                    }
                }

                // 3. Query sos_events table for SYSTEM_ALERT
                val sosResult = supabase.get(
                    "sos_events", 
                    "source=eq.SYSTEM_ALERT&deliveryState=neq.CLOSED&order=createdAt.desc&limit=3"
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
                        
                        if (id.isBlank() || alertStore.isAlertAcknowledged(id) || seenSessionAlertIds.contains(id)) {
                            continue
                        }

                        val createdAtMillis = obj.optLong("createdAt", 0L)
                        if (createdAtMillis > 0L && createdAtMillis < effectiveCutoff) {
                            continue
                        }

                        Log.d(TAG, "🚨 Active LIVE SYSTEM_ALERT detected: $id")
                        val text = when {
                            obj.has("message") && obj.optString("message").isNotBlank() -> obj.optString("message")
                            obj.has("notes") && obj.optString("notes").isNotBlank() -> obj.optString("notes")
                            else -> "Critical emergency disaster alert issued by state authorities."
                        }
                        val severityCode = obj.optInt("severityCode", 5)
                        
                        seenSessionAlertIds.add(id)
                        emit(
                            SystemAlert(
                                sosId = id,
                                message = text,
                                severityCode = severityCode,
                                createdAt = if (createdAtMillis > 0L) createdAtMillis else now
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during emergency alert polling", e)
            }
            
            delay(intervalMs)
        }
    }
}
