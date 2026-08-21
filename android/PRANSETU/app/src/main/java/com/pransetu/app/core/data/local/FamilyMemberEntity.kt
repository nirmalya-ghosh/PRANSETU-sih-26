package com.pransetu.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class FamilySafetyStatus {
    SAFE,
    NEEDS_HELP,
    IN_DANGER,
    UNKNOWN
}

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String, // e.g. "Father", "Mother", "Sister", "Spouse", "Child"
    val phoneNumber: String,
    val status: String = FamilySafetyStatus.UNKNOWN.name,
    val lastLocationLat: Double? = null,
    val lastLocationLon: Double? = null,
    val lastLocationName: String = "Location Pending",
    val lastCheckedInAt: Long = System.currentTimeMillis(),
    val batteryPercent: Int? = null,
    val isSelf: Boolean = false
)

@Dao
interface FamilyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMember(member: FamilyMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(members: List<FamilyMemberEntity>)

    @Update
    fun updateMember(member: FamilyMemberEntity): Int

    @Query("SELECT * FROM family_members ORDER BY isSelf DESC, name ASC")
    fun observeAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :memberId")
    fun getMemberById(memberId: String): FamilyMemberEntity?

    @Query("UPDATE family_members SET status = :status, lastLocationName = :locationName, lastLocationLat = :lat, lastLocationLon = :lon, lastCheckedInAt = :timestamp WHERE isSelf = 1")
    fun updateSelfStatus(status: String, locationName: String, lat: Double?, lon: Double?, timestamp: Long): Int

    @Query("UPDATE family_members SET status = :status, lastCheckedInAt = :timestamp WHERE id = :id")
    fun updateMemberStatus(id: String, status: String, timestamp: Long): Int

    @Query("DELETE FROM family_members WHERE id = :memberId AND isSelf = 0")
    fun deleteMember(memberId: String): Int

    @Query("SELECT COUNT(*) FROM family_members")
    fun count(): Int
}
