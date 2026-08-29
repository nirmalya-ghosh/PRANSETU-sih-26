package com.pransetu.app.core.network

import android.content.Context
import android.util.Log
import com.pransetu.app.core.network.supabase.SupabaseClient
import com.pransetu.app.feature.alert.AlertViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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

    private suspend fun fetchMissedAlerts(effectiveCutoff: Long, now: Long, trySend: (SystemAlert) -> Unit) {
        try {
            // 1. Check for EMERGENCY_BROADCAST_CANCELLED
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
                    if (cancelTime > alertStore.getLastStandDownTime() && (now - cancelTime) < MAX_ALERT_AGE_MS) {
                        Log.d(TAG, "🛑 ALL-CLEAR missed while offline. Silencing.")
                        alertStore.recordStandDownTime(cancelTime)
                        AlertViewModel.globalDismiss()
                        com.pransetu.app.core.network.EmergencyAlertEngine.globalDismiss()
                    }
                }
            }

            // 2. Query realtime_events
            val realtimeResult = supabase.get(
                "realtime_events", 
                "event_type=eq.EMERGENCY_DISASTER_BROADCAST&order=created_at.desc&limit=3"
            )
            if (realtimeResult.isSuccess) {
                val eventsArray = JSONArray(realtimeResult.getOrNull() ?: "[]")
                for (i in 0 until eventsArray.length()) {
                    val eventObj = eventsArray.getJSONObject(i)
                    val eventId = eventObj.optString("event_id").ifBlank { eventObj.optString("id") }
                    if (eventId.isBlank() || alertStore.isAlertAcknowledged(eventId) || seenSessionAlertIds.contains(eventId)) continue

                    val timeStr = eventObj.optString("created_at", eventObj.optString("occurred_at", ""))
                    val eventTime = parseTimestampMillis(timeStr)
                    if (eventTime > 0L && eventTime < effectiveCutoff) continue

                    val payloadObj = eventObj.optJSONObject("payload") ?: JSONObject()
                    val text = payloadObj.optString("disaster_text").ifBlank { eventObj.optString("message") }
                    seenSessionAlertIds.add(eventId)
                    trySend(SystemAlert(eventId, text.ifBlank { "CRITICAL ALERT" }, 5, if (eventTime > 0L) eventTime else now))
                }
            }

            // 3. Query sos_events
            val sosResult = supabase.get(
                "sos_events", 
                "source=eq.SYSTEM_ALERT&deliveryState=neq.CLOSED&order=createdAt.desc&limit=3"
            )
            if (sosResult.isSuccess) {
                val sosArray = JSONArray(sosResult.getOrNull() ?: "[]")
                for (i in 0 until sosArray.length()) {
                    val obj = sosArray.getJSONObject(i)
                    val id = obj.optString("sosId").ifBlank { obj.optString("id") }
                    if (id.isBlank() || alertStore.isAlertAcknowledged(id) || seenSessionAlertIds.contains(id)) continue

                    val createdAtMillis = obj.optLong("createdAt", 0L)
                    if (createdAtMillis > 0L && createdAtMillis < effectiveCutoff) continue

                    val text = obj.optString("message").ifBlank { obj.optString("notes") }
                    seenSessionAlertIds.add(id)
                    trySend(SystemAlert(id, text.ifBlank { "SYSTEM ALERT" }, obj.optInt("severityCode", 5), if (createdAtMillis > 0L) createdAtMillis else now))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching missed alerts", e)
        }
    }

    fun pollForAlerts(intervalMs: Long = 1000L): Flow<SystemAlert> = callbackFlow {
        Log.d(TAG, "Starting System Alert Realtime WebSocket Connection...")
        
        launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cutoffTime = now - MAX_ALERT_AGE_MS
            val effectiveCutoff = maxOf(cutoffTime, alertStore.getLastStandDownTime())
            fetchMissedAlerts(effectiveCutoff, now) { trySend(it) }
        }

        val realtimeClient = createSupabaseClient(
            supabaseUrl = SupabaseClient.supabaseUrl,
            supabaseKey = SupabaseClient.supabaseAnonKey
        ) {
            install(Realtime)
        }

        val channel = realtimeClient.realtime.channel("public-alerts")
        var isConnected = false

        launch(Dispatchers.IO) {
            try {
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "realtime_events"
                }.collect { action ->
                    val recordStr = action.record.toString()
                    try {
                        val eventObj = JSONObject(recordStr)
                        val eventType = eventObj.optString("event_type", "")
                        
                        if (eventType == "EMERGENCY_BROADCAST_CANCELLED") {
                            val cancelTimeStr = eventObj.optString("created_at", eventObj.optString("occurred_at", ""))
                            val cancelTime = parseTimestampMillis(cancelTimeStr)
                            if (cancelTime > alertStore.getLastStandDownTime()) {
                                alertStore.recordStandDownTime(cancelTime)
                                AlertViewModel.globalDismiss()
                                com.pransetu.app.core.network.EmergencyAlertEngine.globalDismiss()
                            }
                        } else if (eventType == "EMERGENCY_DISASTER_BROADCAST") {
                            val eventId = eventObj.optString("event_id").ifBlank { eventObj.optString("id") }
                            if (eventId.isNotBlank() && !alertStore.isAlertAcknowledged(eventId) && !seenSessionAlertIds.contains(eventId)) {
                                val payloadObj = eventObj.optJSONObject("payload") ?: JSONObject()
                                val text = payloadObj.optString("disaster_text").ifBlank { eventObj.optString("message") }
                                seenSessionAlertIds.add(eventId)
                                trySend(SystemAlert(eventId, text.ifBlank { "CRITICAL EMERGENCY" }, 5, System.currentTimeMillis()))
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error parsing realtime_events", e) }
                }
            } catch (e: Exception) { Log.e(TAG, "realtime_events flow failed", e) }
        }

        launch(Dispatchers.IO) {
            try {
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "sos_events"
                }.collect { action ->
                    val recordStr = action.record.toString()
                    try {
                        val eventObj = JSONObject(recordStr)
                        val source = eventObj.optString("source", "")
                        if (source == "SYSTEM_ALERT") {
                            val eventId = eventObj.optString("sosId").ifBlank { eventObj.optString("id") }
                            if (eventId.isNotBlank() && !alertStore.isAlertAcknowledged(eventId) && !seenSessionAlertIds.contains(eventId)) {
                                val text = eventObj.optString("message").ifBlank { "SYSTEM ALERT" }
                                seenSessionAlertIds.add(eventId)
                                trySend(SystemAlert(eventId, text, eventObj.optInt("severityCode", 5), System.currentTimeMillis()))
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error parsing sos_events", e) }
                }
            } catch (e: Exception) { Log.e(TAG, "sos_events flow failed", e) }
        }

        launch(Dispatchers.IO) {
            var backoff = 1000L
            while (true) {
                try {
                    if (!isConnected) {
                        channel.subscribe()
                        realtimeClient.realtime.connect()
                        isConnected = true
                        Log.d(TAG, "✅ Supabase Realtime connected!")
                        backoff = 1000L
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Realtime disconnect. Retry in ${backoff}ms", e)
                    isConnected = false
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(30000L)
                }
                delay(5000L)
            }
        }

        awaitClose {
            Log.d(TAG, "Closing System Alert Realtime WebSocket Connection...")
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    realtimeClient.realtime.disconnect()
                } catch (_: Exception) {}
            }
        }
    }
}
