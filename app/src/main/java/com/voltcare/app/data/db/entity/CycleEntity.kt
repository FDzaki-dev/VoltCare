package com.voltcare.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Satu siklus charge 0%-100% penuh (untuk Cycle Counter). */
@Entity(tableName = "cycle_history")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val startPercent: Int,
    val mahDelivered: Float,
    val isFullCalibrationCycle: Boolean = false
)
