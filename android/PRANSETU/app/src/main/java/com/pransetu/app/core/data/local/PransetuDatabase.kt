package com.pransetu.app.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.pransetu.app.core.sos.DeliveryState

/**
 * Type converters for Room to handle enum persistence.
 */
class DeliveryStateConverter {
    @TypeConverter
    fun fromDeliveryState(state: DeliveryState): String = state.name

    @TypeConverter
    fun toDeliveryState(value: String): DeliveryState = DeliveryState.valueOf(value)
}

/**
 * The main Room database for PRANSETU.
 * 
 * Stores SOS records locally for offline-first persistence.
 * Data is written here before any transmission attempt.
 */
@Database(
    entities = [SosEntity::class, AlertEntity::class, EmergencyContactEntity::class, FamilyMemberEntity::class, MeshPacketEntity::class, LocalEventEntity::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(DeliveryStateConverter::class)
abstract class PransetuDatabase : RoomDatabase() {

    abstract fun sosDao(): SosDao
    abstract fun alertDao(): AlertDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun familyDao(): FamilyDao
    abstract fun meshPacketDao(): MeshPacketDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: PransetuDatabase? = null

        fun getInstance(context: Context): PransetuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PransetuDatabase::class.java,
                    "pransetu_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
