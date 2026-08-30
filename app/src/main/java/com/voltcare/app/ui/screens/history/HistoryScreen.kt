package com.voltcare.app.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltcare.app.ui.theme.VcAmber
import com.voltcare.app.ui.theme.VcGreen
import com.voltcare.app.ui.theme.VcRed
import kotlinx.coroutines.launch

/**
 * Tab Riwayat (Batch 83 - rombak total, laporan user "kurang useful bagi user awam"):
 * - Kartu ringkasan direword ke bahasa awam + status berwarna (Baik/Cukup/Menurun/Buruk,
 *   Normal/Hangat/Panas) pakai token warna semantik yang sama dgn seluruh app (VcGreen/Amber/Red).
 * - Insight kalimat polos di bawah kartu (bukan cuma angka mentah tanpa konteks).
 * - Grafik diagregasi per-jam/per-hari (HistoryViewModel.computeDailyStats) + Y-axis 3 label +
 *   gridline + X-axis label tanggal/jam awal-akhir - grafik lama TANPA label sama sekali
 *   (Pending Queue #35, UX_AUDIT.md Batch 80, ditutup lewat rombakan ini) dan Health% selalu
 *   dipetakan ke rentang tetap 0-100 (baterai jarang berubah jauh dari itu -> selalu kelihatan
 *   garis rata nyaris kosong, persis di screenshot user) - sekarang rentang dinamis mengikuti
 *   data riil, jadi variasi kecil pun kelihatan.
 * - Label rentang data ("30 hari terakhir" dulu statis walau data baru ~2 jam, sekarang sesuai
 *   data riil - CsvExporter/DB TIDAK diubah, murni tampilan).
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeExportMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Riwayat Baterai", style = MaterialTheme.typography.headlineMedium)

            when {
                state.isLoading -> Text("Memuat...", style = MaterialTheme.typography.bodyMedium)
                state.logs.isEmpty() -> Text(
                    "Belum ada data. Riwayat terisi otomatis begitu monitoring berjalan.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> {
                    Text(
                        "Menampilkan data dari ${state.spanLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SummaryRow(state)

                    Text(
                        "Kesehatan baterai kamu ${state.healthStatusLabel.lowercase()} (${state.avgHealthPercent}%). ${state.healthInsightText}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Suhu ${state.tempStatusLabel.lowercase()} (rata-rata ${"%.1f".format(state.avgTemperatureC)}\u00B0C). ${state.tempInsightText}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text("Tren Kesehatan Baterai", style = MaterialTheme.typography.labelLarge)
                    val healthMin = ((state.dailyStats.minOfOrNull { it.avgHealthPercent } ?: 0f) - 3f).coerceAtLeast(0f)
                    val healthMax = ((state.dailyStats.maxOfOrNull { it.avgHealthPercent } ?: 100f) + 3f).coerceAtMost(100f)
                    HistoryLineChart(
                        dailyStats = state.dailyStats,
                        valueSelector = { it.avgHealthPercent },
                        minValue = healthMin,
                        maxValue = healthMax,
                        color = statusColor(state.healthStatusLabel),
                        valueFormat = { "${it.toInt()}%" }
                    )

                    Text("Tren Suhu", style = MaterialTheme.typography.labelLarge)
                    val tempMin = ((state.dailyStats.minOfOrNull { it.avgTemperatureC } ?: 0f) - 2f).coerceAtLeast(0f)
                    val tempMax = (state.dailyStats.maxOfOrNull { it.avgTemperatureC } ?: 40f) + 2f
                    HistoryLineChart(
                        dailyStats = state.dailyStats,
                        valueSelector = { it.avgTemperatureC },
                        minValue = tempMin,
                        maxValue = tempMax,
                        color = statusColor(state.tempStatusLabel),
                        valueFormat = { "%.0f\u00B0C".format(it) }
                    )

                    Button(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export CSV (${state.logs.size} baris)")
                    }
                }
            }
        }
    }
}

/** Warna status semantik yang sama dipakai lintas app (VcGreen=OK, VcAmber=perlu perhatian,
 *  VcRed=kritis) - dipetakan dari label bahasa Indonesia yang sudah ada (BatteryUtils.healthLabel
 *  & HistoryViewModel.temperatureStatusLabel), bukan ambang baru. */
private fun statusColor(label: String): Color = when (label) {
    "Baik", "Normal" -> VcGreen
    "Cukup", "Hangat" -> VcAmber
    else -> VcRed
}

@Composable
private fun SummaryRow(state: HistoryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Kesehatan Baterai",
            value = "${state.avgHealthPercent}%",
            sub = "min ${state.minHealthPercent}% - maks ${state.maxHealthPercent}%",
            statusText = state.healthStatusLabel,
            statusColor = statusColor(state.healthStatusLabel)
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Suhu Rata-rata",
            value = "${"%.1f".format(state.avgTemperatureC)}\u00B0C",
            sub = "puncak ${"%.1f".format(state.maxTemperatureC)}\u00B0C",
            statusText = state.tempStatusLabel,
            statusColor = statusColor(state.tempStatusLabel)
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Siklus Charge",
            value = "${state.cyclesInPeriod}x",
            sub = state.spanLabel.replaceFirstChar { it.uppercase() }
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    sub: String,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (statusText != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Grafik garis dgn konteks (Batch 83): 3 label sumbu-Y (maks/tengah/min) + gridline halus +
 * 2 label sumbu-X (titik waktu awal & akhir) - grafik LAMA (Canvas polos tanpa label apa pun,
 * Pending Queue #35) diganti total. Kalau titik data < 2 (mis. app baru dipasang beberapa menit),
 * tampilkan pesan alih-alih kanvas kosong yang membingungkan.
 */
@Composable
private fun HistoryLineChart(
    dailyStats: List<DailyStat>,
    valueSelector: (DailyStat) -> Float,
    minValue: Float,
    maxValue: Float,
    color: Color,
    valueFormat: (Float) -> String
) {
    val values = dailyStats.map(valueSelector)
    if (values.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Belum cukup data untuk grafik (minimal 2 titik waktu).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val yAxisWidth = 42.dp
    val spacerWidth = 8.dp
    val range = (maxValue - minValue).coerceAtLeast(0.01f)

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .width(yAxisWidth)
                .height(120.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(valueFormat(maxValue), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(valueFormat(minValue + range / 2f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(valueFormat(minValue), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Spacer(modifier = Modifier.width(spacerWidth))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(120.dp)
        ) {
            val gridColor = Color.Gray.copy(alpha = 0.25f)
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = size.height * (1f - fraction)
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }

            val stepX = size.width / (values.size - 1)
            val path = androidx.compose.ui.graphics.Path()
            values.forEachIndexed { index, v ->
                val x = index * stepX
                val normalized = ((v - minValue) / range).coerceIn(0f, 1f)
                val y = size.height - (normalized * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = yAxisWidth + spacerWidth),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(dailyStats.first().dateLabel, style = MaterialTheme.typography.labelSmall)
        Text(dailyStats.last().dateLabel, style = MaterialTheme.typography.labelSmall)
    }
}
