package com.pransetu.app.core.network.events

import android.content.Context
import android.util.Log
import com.pransetu.app.core.data.local.EventDao
import com.pransetu.app.core.data.local.LocalEventEntity
import com.pransetu.app.core.data.local.PransetuDatabase
import com.pransetu.app.core.network.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Universal Event Manager for PRANSETU.
 * Emits, persists locally (offline-first), and synchronizes operational events
 * with the central Supabase Realtime Event Bus.
 */
class EventManager private constructor(
    private val eventDao: EventDao,
    private val supabase: SupabaseClient = SupabaseClient
) {
    private val TAG = "EventManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    fun recordEvent(
        eventType: String,
        sosId: String? = null,
        userId: String? = null,
        deviceId: String? = null,
        incidentId: String? = null,
        campaignId: String? = null,
        payload: JSONObject = JSONObject(),
        priority: Int = 1
    ) {
        val eventId = UUID.randomUUID().toString()
        val occurredAt = System.currentTimeMillis()

        scope.launch {
            try {
                val entity = LocalEventEntity(
                    localEventId = eventId,
                    eventType = eventType,
                    eventVersion = 1,
                    occurredAt = occurredAt,
                    userId = userId,
                    deviceId = deviceId,
                    sosId = sosId,
                    incidentId = incidentId,
                    campaignId = campaignId,
                    source = "android",
                    payloadJson = payload.toString(),
                    syncState = "PENDING",
                    priority = priority
                )
                eventDao.insertEvent(entity)
                Log.d(TAG, "Recorded local event: $eventType [ID: $eventId, SOS: $sosId]")

                // Attempt immediate sync
                syncPendingEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record local event: $eventType", e)
            }
        }
    }

    suspend fun syncPendingEvents(): Int = syncMutex.withLock {
        try {
            val pending = eventDao.getPendingEvents(limit = 25)
            if (pending.isEmpty()) return 0

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val jsonArray = JSONArray()
            val eventIds = mutableListOf<String>()

            for (e in pending) {
                val payloadObj = try { JSONObject(e.payloadJson) } catch (_: Exception) { JSONObject() }
                val eventJson = JSONObject().apply {
                    put("event_id", e.localEventId)
                    put("event_type", e.eventType)
                    put("event_version", e.eventVersion)
                    put("occurred_at", isoFormat.format(Date(e.occurredAt)))
                    e.userId?.let { put("user_id", it) }
                    e.deviceId?.let { put("device_id", it) }
                    e.sosId?.let { put("sos_id", it) }
                    e.incidentId?.let { put("incident_id", it) }
                    e.campaignId?.let { put("campaign_id", it) }
                    put("source", e.source)
                    put("payload", payloadObj)
                }
                jsonArray.put(eventJson)
                eventIds.add(e.localEventId)
            }

            val result = supabase.post("realtime_events", jsonArray.toString())
            if (result.isSuccess) {
                eventDao.markEventsSynced(eventIds)
                Log.d(TAG, "Successfully synced ${eventIds.size} events to realtime_events table!")
                return eventIds.size
            } else {
                val err = result.exceptionOrNull()
                Log.w(TAG, "Event sync delayed: ${err?.message}")
                for (id in eventIds) {
                    eventDao.updateSyncState(id, "FAILED")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending events", e)
        }
        return 0
    }

    companion object {
        @Volatile
        private var INSTANCE: EventManager? = null

        fun getInstance(context: Context): EventManager {
            return INSTANCE ?: synchronized(this) {
                val db = PransetuDatabase.getInstance(context)
                val instance = EventManager(db.eventDao())
                INSTANCE = instance
                instance
            }
        }

        fun get(): EventManager? = INSTANCE
    }
}
