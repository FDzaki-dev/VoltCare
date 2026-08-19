package com.voltcare.app.ui.screens.rules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pilihan tetap untuk conditionType, cocok dengan BatteryMonitorService.checkRule(). */
enum class RuleCondition(val stored: String, val label: String, val unit: String) {
    TEMP_ABOVE("TEMP_ABOVE", "Suhu di atas", "\u00B0C"),
    PERCENT_ABOVE("PERCENT_ABOVE", "Persen di atas", "%"),
    PERCENT_BELOW("PERCENT_BELOW", "Persen di bawah", "%");

    companion object {
        fun fromStored(value: String) = entries.find { it.stored == value } ?: TEMP_ABOVE
    }
}

/** Pilihan tetap untuk actionType, cocok dengan RuleEntity.actionType. */
enum class RuleAction(val stored: String, val label: String) {
    ALARM("ALARM", "Alarm (getar + suara)"),
    NOTIFY("NOTIFY", "Notifikasi saja");

    companion object {
        fun fromStored(value: String) = entries.find { it.stored == value } ?: NOTIFY
    }
}

/**
 * ViewModel tab Aturan Cerdas. CRUD murni ke RuleEntity via RuleDao (protected asset,
 * dipakai apa adanya - tidak ada perubahan schema/query). Engine evaluasi rule sudah
 * berjalan di BatteryMonitorService sejak Batch 1, tidak disentuh.
 */
class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val rules: StateFlow<List<RuleEntity>> = db.ruleDao().all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(
        existingId: Long?,
        label: String,
        condition: RuleCondition,
        conditionValue: Float,
        requireCharging: Boolean,
        action: RuleAction
    ) {
        viewModelScope.launch {
            val rule = RuleEntity(
                id = existingId ?: 0,
                label = label,
                conditionType = condition.stored,
                conditionValue = conditionValue,
                requireCharging = requireCharging,
                actionType = action.stored,
                isEnabled = true
            )
            if (existingId == null) db.ruleDao().insert(rule) else db.ruleDao().update(rule)
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch { db.ruleDao().delete(rule) }
    }

    fun setEnabled(rule: RuleEntity, enabled: Boolean) {
        viewModelScope.launch { db.ruleDao().update(rule.copy(isEnabled = enabled)) }
    }
}
