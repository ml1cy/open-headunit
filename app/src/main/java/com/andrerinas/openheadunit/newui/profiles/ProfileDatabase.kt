package com.andrerinas.openheadunit.newui.profiles

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "driver_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val seat: String,
    val volume: Int,
    val themeIsNight: Boolean,
    val presetsSummary: String,
    val lastDriveEpochMs: Long?,
    val isActive: Boolean,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM driver_profiles ORDER BY rowid")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("UPDATE driver_profiles SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE driver_profiles SET isActive = 1, lastDriveEpochMs = :ts WHERE id = :id")
    suspend fun activate(id: String, ts: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Query("SELECT COUNT(*) FROM driver_profiles")
    suspend fun count(): Int
}

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile private var instance: ProfileDatabase? = null

        fun get(context: Context): ProfileDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, ProfileDatabase::class.java, "driver_profiles.db")
                    .build()
                    .also { instance = it }
            }
    }
}
