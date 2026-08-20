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
    val estimateLabel: String = "Estimasi",
    val isCharging: Boolean = false,
    val chargerSpeedLabel: String = "-"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _calibrationInProgress = MutableStateFlow(
        BatteryUtils.CalibrationStore.isActive(application)
    )
    val calibrationInProgress: StateFlow<Boolean> = _calibrationInProgress.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                db.batteryLogDao().latest(),
                db.cycleDao().count()
            ) { log, cycles -> log to cycles }
                .collect { (log, cycles) ->
                    if (log == null) return@collect
                    val estimate: Int
                    val label: String
                    if (log.isCharging) {
                        val snapshot = BatteryUtils.readSnapshot(getApplication())
                        estimate = BatteryUtils.estimateMinutesToFull(snapshot, BatteryUtils.DEFAULT_DESIGN_CAPACITY_MAH)
                        label = "Estimasi Penuh"
                    } else {
                        // Pending #10 (FEATURE_PARITY_GOALS.md): sisa waktu pakai dari rata-rata
                        // drain rate 24 jam terakhir, murni agregasi data existing (tanpa
                        // tabel/kolom DB baru, DAO.sinceOnce() sudah ada sejak Batch 1).
                        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                        val recentLogs = db.batteryLogDao().sinceOnce(since)
                        estimate = estimateRemainingMinutes(recentLogs, log.percent)
                        label = if (estimate >= 0) "Sisa Pakai" else "Estimasi"
                    }

                    _uiState.value = DashboardUiState(
                        healthPercent = log.healthPercent,
                        healthLabel = BatteryUtils.healthLabel(log.healthPercent),
                        temperatureC = log.temperatureC,
                        voltage = log.voltage,
                        currentMa = log.currentMa,
                        cycleCount = cycles,
                        estimateMinutes = estimate,
                        estimateLabel = label,
                        isCharging = log.isCharging,
                        chargerSpeedLabel = BatteryUtils.classifyChargerSpeed(log.currentMa)
                    )
                    // Sumber kebenaran status kalibrasi ada di BatteryMonitorService (via
                    // CalibrationStore/SharedPreferences); sinkronkan tiap sample baru masuk
                    // supaya tombol otomatis balik ke "Mulai Kalibrasi" saat 3 siklus selesai
                    // atau sesi dibatalkan (drop/charger dicabut).
                    _calibrationInProgress.value = BatteryUtils.CalibrationStore.isActive(getApplication())
                }
        }
    }

    /**
     * Pending #10: rata-rata drain rate (%/menit) dari sample discharge berturut-turut dalam
     * [logs] (24 jam terakhir), lalu proyeksikan [currentPercent] habis dalam berapa menit.
     * Hanya pasangan sample isCharging=false & percent menurun yang dihitung (melompati jeda
     * charging supaya rate tidak bias). Return -1 kalau data kurang/rate tidak valid (belum
     * ada histori discharge cukup, mis. HP baru terpasang atau baru selesai charge penuh).
     */
    private fun estimateRemainingMinutes(logs: List<com.voltcare.app.data.db.entity.BatteryLogEntity>, currentPercent: Int): Int {
        if (logs.size < 2) return -1
        var totalDropPercent = 0
        var totalElapsedMinutes = 0.0
        for (i in 1 until logs.size) {
            val prev = logs[i - 1]
            val curr = logs[i]
            if (prev.isCharging || curr.isCharging) continue
            val drop = prev.percent - curr.percent
            if (drop <= 0) continue // charger dicabut lalu naik lagi / noise, lewati
            val elapsedMinutes = (curr.timestamp - prev.timestamp) / 60000.0
            if (elapsedMinutes <= 0) continue
            totalDropPercent += drop
            totalElapsedMinutes += elapsedMinutes
        }
        if (totalDropPercent <= 0 || totalElapsedMinutes <= 0) return -1
        val ratePerMinute = totalDropPercent / totalElapsedMinutes
        if (ratePerMinute <= 0.0) return -1
        return (currentPercent / ratePerMinute).toInt().coerceAtLeast(0)
    }

    /**
     * Memulai sesi Kalibrasi: alur wajib 3x siklus charge 0-100% berturut-turut dengan
     * validasi non-drop, dieksekusi & dipantau oleh BatteryMonitorService di background
     * (lihat BatteryUtils.CalibrationStore) - tahan proses ViewModel/Activity mati.
     */
    fun startCalibration() {
        BatteryUtils.CalibrationStore.activate(getApplication())
        _calibrationInProgress.value = true
    }
}
