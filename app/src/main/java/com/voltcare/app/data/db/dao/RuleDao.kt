package com.voltcare.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.voltcare.app.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Insert
    suspend fun insert(rule: RuleEntity)

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("SELECT * FROM smart_rule ORDER BY id DESC")
    fun all(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM smart_rule WHERE isEnabled = 1")
    suspend fun enabledOnce(): List<RuleEntity>
}
