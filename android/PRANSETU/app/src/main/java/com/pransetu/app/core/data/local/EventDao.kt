package com.pransetu.app.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: LocalEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvents(events: @JvmSuppressWildcards List<LocalEventEntity>)

    @Query("SELECT * FROM local_events WHERE syncState = 'PENDING' ORDER BY priority DESC, occurredAt ASC LIMIT :limit")
    fun getPendingEvents(limit: Int): List<LocalEventEntity>

    @Query("UPDATE local_events SET syncState = :state, attemptCount = attemptCount + 1 WHERE localEventId = :eventId")
    fun updateSyncState(eventId: String, state: String): Int

    @Query("UPDATE local_events SET syncState = 'SYNCED' WHERE localEventId IN (:eventIds)")
    fun markEventsSynced(eventIds: @JvmSuppressWildcards List<String>): Int

    @Query("DELETE FROM local_events WHERE syncState = 'SYNCED' AND occurredAt < :cutoffTimestamp")
    fun purgeOldSyncedEvents(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM local_events WHERE syncState = 'PENDING'")
    fun observePendingEventCount(): Flow<Int>

    @Query("SELECT * FROM local_events ORDER BY occurredAt DESC LIMIT 100")
    fun observeRecentEvents(): Flow<List<LocalEventEntity>>
}
