package com.powervault.health.pro.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aturan cerdas: IF <kondisi> THEN <aksi>.
 * conditionType: "TEMP_ABOVE" | "PERCENT_ABOVE" | "PERCENT_BELOW"
 * actionType: "ALARM" | "NOTIFY"
 */
@Entity(tableName = "smart_rule")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val conditionType: String,
    val conditionValue: Float,
    val requireCharging: Boolean,
    val actionType: String,
    val isEnabled: Boolean = true
)
