package com.pransetu.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val alertId: String,
    val title: String, // E.g. "CYCLONE", "FLOOD", "TSUNAMI", "LIGHTNING", "HEATWAVE", "EVACUATE"
    val severity: Int, // 3: Critical/Red Alert, 2: High/Orange Alert, 1: Medium/Yellow Watch, 0: Info
    val timestamp: Long,
    val source: String, // E.g. "IMD Bhubaneswar", "OSDMA Control Room", "SRC Odisha", "INCOIS"
    val isRead: Boolean = false,
    val bodyKey: String? = null,
    val category: String = "WEATHER", // "WEATHER", "EVACUATION", "SAFETY"
    val windSpeed: String? = null,
    val rainfall: String? = null,
    val affectedDistricts: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "Odisha Region",
    val impactRadiusKm: Double = 50.0,
    val isUpcoming: Boolean = false, // false = Active/Issued, true = Upcoming/Forecasted
    val expectedImpactTime: Long? = null,
    val actionInstruction: String? = null
)

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAlerts(alerts: @JvmSuppressWildcards List<AlertEntity>)

    @Query("SELECT * FROM alerts ORDER BY isUpcoming ASC, severity DESC, timestamp DESC")
    fun observeAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isUpcoming = 0 ORDER BY severity DESC, timestamp DESC")
    fun observeActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isUpcoming = 1 ORDER BY expectedImpactTime ASC")
    fun observeUpcomingAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isRead = 0 ORDER BY timestamp DESC")
    fun observeUnreadAlerts(): Flow<List<AlertEntity>>

    @Query("UPDATE alerts SET isRead = 1 WHERE alertId = :alertId")
    fun markAsRead(alertId: String)

    @Query("DELETE FROM alerts WHERE timestamp < :beforeTimestamp")
    fun pruneOldAlerts(beforeTimestamp: Long): Int

    @Query("DELETE FROM alerts")
    fun deleteAllAlerts()

    @Query("SELECT COUNT(*) FROM alerts")
    fun count(): Int
}
