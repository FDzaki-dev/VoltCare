package com.voltcare.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.BatteryLogEntity
import com.voltcare.app.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HistoryUiState(
    val logs: List<BatteryLogEntity> = emptyList(),
    val cyclesInPeriod: Int = 0,
    val avgHealthPercent: Int = 0,
    val minHealthPercent: Int = 0,
    val maxHealthPercent: Int = 0,
    val avgTemperatureC: Float = 0f,
    val maxTemperatureC: Float = 0f,
    val isLoading: Boolean = true
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /** Pesan status ekspor CSV sekali-tampil (null = tidak ada pesan aktif). */
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    init {
        val sinceMillis = System.currentTimeMillis() - RETENTION_MS
        viewModelScope.launch {
            combine(
                db.batteryLogDao().since(sinceMillis),
                db.cycleDao().all()
            ) { logs, cycles -> logs to cycles }
                .collect { (logs, cycles) ->
                    val cyclesInPeriod = cycles.count { it.endTimestamp >= sinceMillis }
                    _uiState.value = if (logs.isEmpty()) {
                        HistoryUiState(isLoading = false)
                    } else {
                        HistoryUiState(
                            logs = logs,
                            cyclesInPeriod = cyclesInPeriod,
                            avgHealthPercent = logs.map { it.healthPercent }.average().toInt(),
                            minHealthPercent = logs.minOf { it.healthPercent },
                            maxHealthPercent = logs.maxOf { it.healthPercent },
                            avgTemperatureC = logs.map { it.temperatureC }.average().toFloat(),
                            maxTemperatureC = logs.maxOf { it.temperatureC },
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun exportCsv() {
        val logs = _uiState.value.logs
        viewModelScope.launch {
            val result = CsvExporter.exportHistoryToCsv(getApplication(), logs)
            _exportMessage.value = when (result) {
                is CsvExporter.ExportResult.Success ->
                    "Berhasil: ${result.fileName} (${result.rowCount} baris) -> Documents/VoltCare/exports/"
                is CsvExporter.ExportResult.Failure ->
                    "Gagal ekspor: ${result.reason}"
            }
        }
    }

    fun consumeExportMessage() {
        _exportMessage.value = null
    }

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
