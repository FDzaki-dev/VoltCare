package com.voltcare.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.BatteryLogEntity
import com.voltcare.app.util.BatteryUtils
import com.voltcare.app.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Titik data teragregasi untuk grafik Riwayat (Batch 83, rombak tab Riwayat) - dipakai alih-alih
 * plot per-sample mentah (bisa ribuan titik/30 hari tanpa konteks tanggal sama sekali, penyebab
 * grafik lama terlihat padat/flat & kurang bisa dibaca user awam - lihat komentar
 * computeDailyStats()). Granularitas adaptif: per jam kalau rentang data belum genap 1 hari
 * (mis. baru install, sesuai kasus screenshot user - cuma ~2 jam data), per hari kalender kalau
 * sudah lebih lama.
 */
data class DailyStat(
    val dateLabel: String,
    val avgHealthPercent: Float,
    val avgTemperatureC: Float,
    val maxTemperatureC: Float
)

data class HistoryUiState(
    val logs: List<BatteryLogEntity> = emptyList(),
    val dailyStats: List<DailyStat> = emptyList(),
    val spanLabel: String = "",
    val cyclesInPeriod: Int = 0,
    val avgHealthPercent: Int = 0,
    val minHealthPercent: Int = 0,
    val maxHealthPercent: Int = 0,
    val healthStatusLabel: String = "-",
    val healthInsightText: String = "",
    val avgTemperatureC: Float = 0f,
    val maxTemperatureC: Float = 0f,
    val tempStatusLabel: String = "-",
    val tempInsightText: String = "",
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
                        val avgHealth = logs.map { it.healthPercent }.average().toInt()
                        val avgTemp = logs.map { it.temperatureC }.average().toFloat()
                        val healthLabel = BatteryUtils.healthLabel(avgHealth)
                        val tempLabel = temperatureStatusLabel(avgTemp)
                        HistoryUiState(
                            logs = logs,
                            dailyStats = computeDailyStats(logs),
                            spanLabel = spanLabel(logs),
                            cyclesInPeriod = cyclesInPeriod,
                            avgHealthPercent = avgHealth,
                            minHealthPercent = logs.minOf { it.healthPercent },
                            maxHealthPercent = logs.maxOf { it.healthPercent },
                            healthStatusLabel = healthLabel,
                            healthInsightText = healthInsight(healthLabel),
                            avgTemperatureC = avgTemp,
                            maxTemperatureC = logs.maxOf { it.temperatureC },
                            tempStatusLabel = tempLabel,
                            tempInsightText = temperatureInsight(tempLabel),
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

    /**
     * Agregasi per jam (rentang data < 1 hari) atau per hari kalender (>= 1 hari) - lihat KDoc
     * class DailyStat. Granularitas adaptif memastikan grafik selalu punya konteks waktu yang
     * masuk akal & (kalau memungkinkan) minimal 2 titik untuk digambar sebagai garis, alih-alih
     * grafik lama yang selalu per-hari-tetap (bisa cuma 1 titik/blank kalau data baru beberapa
     * jam - persis skenario screenshot user, 129 baris/~2 jam).
     */
    private fun computeDailyStats(logs: List<BatteryLogEntity>): List<DailyStat> {
        val spanMs = logs.maxOf { it.timestamp } - logs.minOf { it.timestamp }
        return if (spanMs < DAY_MS) {
            val fmt = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            logs.groupBy { (it.timestamp / HOUR_MS) * HOUR_MS }
                .toSortedMap()
                .map { (bucketStart, entries) -> entries.toDailyStat(fmt.format(Date(bucketStart))) }
        } else {
            val fmt = SimpleDateFormat("dd/MM", Locale("id", "ID"))
            logs.groupBy { dayStartMillis(it.timestamp) }
                .toSortedMap()
                .map { (dayStart, entries) -> entries.toDailyStat(fmt.format(Date(dayStart))) }
        }
    }

    private fun List<BatteryLogEntity>.toDailyStat(label: String): DailyStat = DailyStat(
        dateLabel = label,
        avgHealthPercent = map { it.healthPercent }.average().toFloat(),
        avgTemperatureC = map { it.temperatureC }.average().toFloat(),
        maxTemperatureC = maxOf { it.temperatureC }
    )

    private fun dayStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Deskripsi rentang data yang SESUNGGUHNYA ada (bukan asumsi selalu "30 hari terakhir" -
     *  itu bug UX lama: label statis walau data baru beberapa jam, lihat screenshot user). */
    private fun spanLabel(logs: List<BatteryLogEntity>): String {
        val spanMs = logs.maxOf { it.timestamp } - logs.minOf { it.timestamp }
        return when {
            spanMs < HOUR_MS -> "kurang dari 1 jam terakhir"
            spanMs < DAY_MS -> "${(spanMs / HOUR_MS).coerceAtLeast(1)} jam terakhir"
            else -> "${(spanMs / DAY_MS).coerceAtLeast(1)} hari terakhir"
        }
    }

    /** Ambang suhu heuristik umum Li-ion utk label kontekstual ringkasan Riwayat - BEDA dari
     *  threshold ALARM di RuleEntity (itu angka custom user, di luar scope layar ini). */
    private fun temperatureStatusLabel(avgC: Float): String = when {
        avgC < 35f -> "Normal"
        avgC < 40f -> "Hangat"
        else -> "Panas"
    }

    private fun healthInsight(label: String): String = when (label) {
        "Baik" -> "Performa baterai masih optimal, wajar dipakai harian."
        "Cukup" -> "Kapasitas mulai berkurang, masih normal untuk pemakaian sehari-hari."
        "Menurun" -> "Kapasitas sudah berkurang cukup banyak, waktu pakai akan terasa lebih pendek."
        else -> "Kapasitas sudah sangat menurun, pertimbangkan ganti baterai."
    }

    private fun temperatureInsight(label: String): String = when (label) {
        "Normal" -> "Suhu baterai dalam rentang aman."
        "Hangat" -> "Sedikit lebih hangat dari biasanya, umumnya karena fast charging atau pemakaian berat."
        else -> "Suhu cenderung tinggi - hindari main game sambil charging dalam waktu lama."
    }

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        private const val HOUR_MS = 60L * 60 * 1000
        private const val DAY_MS = 24L * HOUR_MS
    }
}
