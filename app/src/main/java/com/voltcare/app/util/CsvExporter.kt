package com.voltcare.app.util

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.voltcare.app.data.db.entity.BatteryLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export riwayat `battery_log` ke CSV, disimpan via MediaStore (API 29+, tanpa permission
 * legacy) ke `Documents/VoltCare/exports/`. Pola penulisan file mengikuti `CrashLogger.kt`
 * agar konsisten satu app satu cara akses storage.
 */
object CsvExporter {

    private const val APP_FOLDER = "VoltCare"
    private const val CSV_HEADER = "timestamp_iso,percent,temperature_c,voltage,current_ma,is_charging,health_percent"

    sealed class ExportResult {
        data class Success(val fileName: String, val rowCount: Int) : ExportResult()
        data class Failure(val reason: String) : ExportResult()
    }

    fun exportHistoryToCsv(context: Context, logs: List<BatteryLogEntity>): ExportResult {
        if (logs.isEmpty()) return ExportResult.Failure("Tidak ada data riwayat untuk diekspor.")

        return try {
            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "history_$fileTimestamp.csv"
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

            val csvBody = buildString {
                appendLine(CSV_HEADER)
                logs.forEach { log ->
                    appendLine(
                        listOf(
                            isoFormat.format(Date(log.timestamp)),
                            log.percent,
                            log.temperatureC,
                            log.voltage,
                            log.currentMa,
                            log.isCharging,
                            log.healthPercent
                        ).joinToString(",")
                    )
                }
            }

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/$APP_FOLDER/exports")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: return ExportResult.Failure("Gagal membuat file (resolver.insert null).")

            resolver.openOutputStream(uri)?.use { out ->
                out.write(csvBody.toByteArray())
            } ?: return ExportResult.Failure("Gagal membuka output stream.")

            ExportResult.Success(fileName, logs.size)
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "Kesalahan tidak diketahui saat ekspor.")
        }
    }
}
