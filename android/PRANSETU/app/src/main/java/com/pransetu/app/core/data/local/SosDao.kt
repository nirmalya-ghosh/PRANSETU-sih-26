package com.pransetu.app.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pransetu.app.core.sos.DeliveryState
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SOS records.
 * 
 * All operations are suspend or Flow-based for coroutine integration.
 * Idempotent insert via REPLACE strategy ensures deduplication.
 */
@Dao
interface SosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSos(sos: SosEntity): Long

    @Update
    fun updateSos(sos: SosEntity): Int

    @Query("SELECT * FROM sos_records WHERE sosId = :sosId")
    fun getSosById(sosId: String): SosEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM sos_records WHERE sosId = :sosId)")
    fun hasSos(sosId: String): Boolean

    @Query("SELECT * FROM sos_records ORDER BY createdAt DESC")
    fun observeAllSos(): Flow<List<SosEntity>>

    @Query("SELECT * FROM sos_records WHERE deliveryState IN (:states) ORDER BY createdAt ASC")
    fun observeSosByStates(states: @JvmSuppressWildcards List<DeliveryState>): Flow<List<SosEntity>>

    @Query("""
        SELECT * FROM sos_records 
        WHERE deliveryState IN ('STORED', 'QUEUED', 'FAILED_RETRYING')
        AND retryCount < :maxRetries
        ORDER BY createdAt ASC
    """)
    fun getPendingSos(maxRetries: Int): List<SosEntity>

    @Query("UPDATE sos_records SET deliveryState = :newState WHERE sosId = :sosId")
    fun updateDeliveryState(sosId: String, newState: DeliveryState): Int

    @Query("""
        UPDATE sos_records 
        SET deliveryState = :newState, retryCount = retryCount + 1, lastRetryAt = :timestamp 
        WHERE sosId = :sosId
    """)
    fun incrementRetry(sosId: String, newState: DeliveryState, timestamp: Long): Int

    @Query("UPDATE sos_records SET deliveryState = 'ACKNOWLEDGED', acknowledgedAt = :timestamp WHERE sosId = :sosId")
    fun markAcknowledged(sosId: String, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM sos_records")
    fun count(): Int

    @Query("DELETE FROM sos_records WHERE deliveryState = 'CLOSED' AND createdAt < :beforeTimestamp")
    fun pruneClosedSos(beforeTimestamp: Long): Int
}
