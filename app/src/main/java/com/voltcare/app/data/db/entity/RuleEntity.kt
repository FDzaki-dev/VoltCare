package com.voltcare.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aturan cerdas: IF <kondisi> THEN <aksi>.
 * conditionType: "TEMP_ABOVE" | "PERCENT_ABOVE" | "PERCENT_BELOW"
 * actionType: "ALARM" | "NOTIFY"
 * alarmSoundUri: URI nada custom (dari RingtoneManager.ACTION_RINGTONE_PICKER) untuk aksi ALARM.
 *   null = pakai nada alarm sistem default (Batch 58, kolom baru - lihat AppDatabase Migration 1->2).
 */
@Entity(tableName = "smart_rule")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val conditionType: String,
    val conditionValue: Float,
    val requireCharging: Boolean,
    val actionType: String,
    val isEnabled: Boolean = true,
    val alarmSoundUri: String? = null
)
