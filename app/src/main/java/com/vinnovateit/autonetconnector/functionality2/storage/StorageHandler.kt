package com.vinnovateit.autonetconnector.functionality2.storage

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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vinnovateit.autonetconnector.functionality2.manager.DataUsage
import com.vinnovateit.autonetconnector.functionality2.manager.LiveDataPoint
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//================================================================================
// 1. Database Entities
//================================================================================

@Entity(tableName = "sessions")
data class SessionSummaryEntity(
  @PrimaryKey val startTimestamp: Long,
  val ssid: String,
  val endTimestamp: Long,
  val totalData: DataUsage,
  val history: List<LiveDataPoint>
)

//================================================================================
// 2. Type Converters for Complex Data
//================================================================================

class Converters {
  private val gson = Gson()

  @TypeConverter
  fun fromDataUsage(dataUsage: DataUsage): String {
    return gson.toJson(dataUsage)
  }

  @TypeConverter
  fun toDataUsage(data: String): DataUsage {
    return gson.fromJson(data, DataUsage::class.java)
  }

  @TypeConverter
  fun fromLiveDataPointList(list: List<LiveDataPoint>): String {
    return gson.toJson(list)
  }

  @TypeConverter
  fun toLiveDataPointList(data: String): List<LiveDataPoint> {
    val type = object : TypeToken<List<LiveDataPoint>>() {}.type
    return gson.fromJson(data, type)
  }
}

//================================================================================
// 3. Data Access Objects (DAOs)
//================================================================================

@Dao
interface SessionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: SessionSummaryEntity)

  @Query("SELECT * FROM sessions ORDER BY startTimestamp DESC")
  fun getAllSessions(): Flow<List<SessionSummaryEntity>>

  @Query("DELETE FROM sessions")
  suspend fun clearAllSessions()
}

//================================================================================
// 4. Unified Room Database
//================================================================================

@Database(
  entities = [CredentialEntity::class, SessionSummaryEntity::class],
  version = 2, // Incremented version for migration
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

  abstract fun credentialDao(): CredentialDao
  abstract fun sessionDao(): SessionDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "autonet_app_db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

//================================================================================
// 5. Session Repository
//================================================================================

class SessionRepository(context: Context) {
  private val sessionDao = AppDatabase.getInstance(context).sessionDao()

  fun getAllSessions(): Flow<List<SessionSummary>> {
    return sessionDao.getAllSessions().map { entities ->
      entities.map { it.toSessionSummary() }
    }
  }

  suspend fun insertSession(session: SessionSummary) {
    sessionDao.insertSession(session.toEntity())
  }

  suspend fun clearHistory() {
    sessionDao.clearAllSessions()
  }

  private fun SessionSummary.toEntity(): SessionSummaryEntity {
    return SessionSummaryEntity(
      startTimestamp = this.startTimestamp,
      ssid = this.ssid,
      endTimestamp = this.endTimestamp,
      totalData = this.totalData,
      history = this.history
    )
  }

  private fun SessionSummaryEntity.toSessionSummary(): SessionSummary {
    return SessionSummary(
      startTimestamp = this.startTimestamp,
      ssid = this.ssid,
      endTimestamp = this.endTimestamp,
      totalData = this.totalData,
      history = this.history
    )
  }
}
