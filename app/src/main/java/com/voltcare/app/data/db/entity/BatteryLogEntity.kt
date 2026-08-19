package com.voltcare.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Satu titik data monitoring baterai (dicatat berkala oleh BatteryMonitorService). */
@Entity(tableName = "battery_log")
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val percent: Int,
    val temperatureC: Float,
    val voltage: Float,
    val currentMa: Int,
    val isCharging: Boolean,
    val healthPercent: Int
)
