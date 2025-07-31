package com.vinnovateit.autonetconnector.data

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
import androidx.room.Update
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class Session(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val startTime: Date,
  val endTime: Date,
  val dataUsed: Long,
)

@Entity(tableName = "daily_usage")
data class DailyUsage(
  @PrimaryKey
  val date: Date,
  val totalDataUsed: Long,
)

@Dao
interface StatsDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: Session)

  @Query("SELECT * FROM sessions ORDER BY startTime DESC")
  fun getAllSessions(): Flow<List<Session>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDailyUsage(dailyUsage: DailyUsage)

  @Update
  suspend fun updateDailyUsage(dailyUsage: DailyUsage)

  @Query("SELECT * FROM daily_usage ORDER BY date ASC")
  fun getDailyUsage(): Flow<List<DailyUsage>>

  @Query("SELECT * FROM daily_usage WHERE date = :day")
  suspend fun getUsageForDay(day: Date): DailyUsage?
}

class Converters {
  @TypeConverter
  fun fromTimestamp(value: Long?): Date? {
    return value?.let { Date(it) }
  }

  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? {
    return date?.time
  }
}

@Database(entities = [Session::class, DailyUsage::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AutonetDatabase : RoomDatabase() {

  abstract fun statsDao(): StatsDao

  companion object {
    @Volatile
    private var INSTANCE: AutonetDatabase? = null

    fun getDatabase(context: Context): AutonetDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AutonetDatabase::class.java,
          "autonet_database"
        ).build()
        INSTANCE = instance
        instance
      }
    }
  }
}

class StatsRepository(private val statsDao: StatsDao) {

  fun getAllSessions(): Flow<List<Session>> = statsDao.getAllSessions()

  fun getDailyUsage(): Flow<List<DailyUsage>> = statsDao.getDailyUsage()

  suspend fun addSession(session: Session) {
    statsDao.insertSession(session)
    val sessionDate = getStartOfDay(session.startTime)
    val dataUsed = session.dataUsed
    val existingDailyUsage = statsDao.getUsageForDay(sessionDate)

    if (existingDailyUsage == null) {
      statsDao.insertDailyUsage(DailyUsage(date = sessionDate, totalDataUsed = dataUsed))
    } else {
      val updatedUsage = existingDailyUsage.totalDataUsed + dataUsed
      statsDao.updateDailyUsage(existingDailyUsage.copy(totalDataUsed = updatedUsage))
    }
  }

  private fun getStartOfDay(date: Date): Date {
    return Calendar.getInstance().apply {
      time = date
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time
  }
}
