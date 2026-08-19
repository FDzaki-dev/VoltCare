package com.voltcare.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.voltcare.app.data.db.entity.BatteryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {
    @Insert
    suspend fun insert(log: BatteryLogEntity)

    @Query("SELECT * FROM battery_log ORDER BY timestamp DESC LIMIT 1")
    fun latest(): Flow<BatteryLogEntity?>

    @Query("SELECT * FROM battery_log WHERE timestamp >= :sinceMillis ORDER BY timestamp ASC")
    fun since(sinceMillis: Long): Flow<List<BatteryLogEntity>>

    @Query("SELECT * FROM battery_log WHERE timestamp >= :sinceMillis ORDER BY timestamp ASC")
    suspend fun sinceOnce(sinceMillis: Long): List<BatteryLogEntity>

    @Query("DELETE FROM battery_log WHERE timestamp < :beforeMillis")
    suspend fun pruneOlderThan(beforeMillis: Long)
}
