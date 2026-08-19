package com.voltcare.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DashboardUiState(
    val healthPercent: Int = 0,
    val healthLabel: String = "-",
    val temperatureC: Float = 0f,
    val voltage: Float = 0f,
    val currentMa: Int = 0,
    val cycleCount: Int = 0,
    val estimateMinutes: Int = -1,
    val isCharging: Boolean = false,
    val chargerSpeedLabel: String = "-"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _calibrationInProgress = MutableStateFlow(false)
    val calibrationInProgress: StateFlow<Boolean> = _calibrationInProgress.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                db.batteryLogDao().latest(),
                db.cycleDao().count()
            ) { log, cycles -> log to cycles }
                .collect { (log, cycles) ->
                    if (log == null) return@collect
                    val estimate = if (log.isCharging) {
                        val snapshot = BatteryUtils.readSnapshot(getApplication())
                        BatteryUtils.estimateMinutesToFull(snapshot, BatteryUtils.DEFAULT_DESIGN_CAPACITY_MAH)
                    } else -1

                    _uiState.value = DashboardUiState(
                        healthPercent = log.healthPercent,
                        healthLabel = BatteryUtils.healthLabel(log.healthPercent),
                        temperatureC = log.temperatureC,
                        voltage = log.voltage,
                        currentMa = log.currentMa,
                        cycleCount = cycles,
                        estimateMinutes = estimate,
                        isCharging = log.isCharging,
                        chargerSpeedLabel = BatteryUtils.classifyChargerSpeed(log.currentMa)
                    )
                }
        }
    }

    /**
     * Memulai sesi Kalibrasi. Alur penuh (3x siklus 0-100% berturut-turut dengan validasi non-drop)
     * ada di Pending Queue - saat ini hanya menandai status UI agar tombol tidak dobel-tap.
     */
    fun startCalibration() {
        _calibrationInProgress.value = true
    }
}
