package com.powervault.health.pro.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.powervault.health.pro.data.db.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Insert
    suspend fun insert(cycle: CycleEntity)

    @Query("SELECT * FROM cycle_history ORDER BY endTimestamp DESC")
    fun all(): Flow<List<CycleEntity>>

    @Query("SELECT COUNT(*) FROM cycle_history")
    fun count(): Flow<Int>

    @Query("SELECT * FROM cycle_history WHERE isFullCalibrationCycle = 1 ORDER BY endTimestamp DESC LIMIT :limit")
    suspend fun recentCalibrationCycles(limit: Int): List<CycleEntity>
}
