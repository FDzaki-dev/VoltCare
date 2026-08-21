package com.voltcare.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aturan cerdas: IF <kondisi> THEN <aksi>.
 * conditionType: "TEMP_ABOVE" | "PERCENT_ABOVE" | "PERCENT_BELOW"
 * actionType: "ALARM" | "NOTIFY"
 * alarmSoundUri: URI nada custom (dari RingtoneManager.ACTION_RINGTONE_PICKER) untuk aksi ALARM.
 *   null = pakai nada alarm sistem default (Batch 58, kolom baru - lihat AppDatabase Migration 1->2).
 * alarmLoop: true = nada diulang terus-menerus sampai user pencet "Matikan Alarm" manual;
 *   false (default) = main 1x sampai selesai lalu berhenti sendiri (Batch 66, Migration 2->3).
 * activeDays: hari aktif rule ini, comma-separated Calendar.DAY_OF_WEEK (1=Minggu..7=Sabtu),
 *   default "1,2,3,4,5,6,7" = semua hari (Batch 73, Migration 3->4, mirip picker Google Clock).
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
    val alarmSoundUri: String? = null,
    val alarmLoop: Boolean = false,
    val activeDays: String = "1,2,3,4,5,6,7"
)
