package com.voltcare.app.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

/**
 * Tab Riwayat: ringkasan 30 hari (Health min/avg/max, Suhu, jumlah Cycle) + grafik garis
 * Health%/Suhu dari `battery_log`, dan export CSV ke Documents/VoltCare/exports/ (via
 * HistoryViewModel -> CsvExporter, MediaStore API 29+).
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
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
            Text("Riwayat 30 Hari", style = MaterialTheme.typography.headlineMedium)

            when {
                state.isLoading -> Text("Memuat...", style = MaterialTheme.typography.bodyMedium)
                state.logs.isEmpty() -> Text(
                    "Belum ada data. Riwayat terisi otomatis begitu monitoring berjalan.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> {
                    SummaryRow(state)

                    Text("Health % (garis hijau)", style = MaterialTheme.typography.labelLarge)
                    LineChart(
                        values = state.logs.map { it.healthPercent.toFloat() },
                        minValue = 0f,
                        maxValue = 100f,
                        color = Color(0xFF43A047)
                    )

                    Text("Suhu \u00B0C (garis oranye)", style = MaterialTheme.typography.labelLarge)
                    LineChart(
                        values = state.logs.map { it.temperatureC },
                        minValue = 0f,
                        maxValue = (state.maxTemperatureC + 5f).coerceAtLeast(40f),
                        color = Color(0xFFFB8C00)
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

@Composable
private fun SummaryRow(state: HistoryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(Modifier.weight(1f), "Health", "${state.avgHealthPercent}%", "min ${state.minHealthPercent} / max ${state.maxHealthPercent}")
        SummaryCard(Modifier.weight(1f), "Suhu rata-rata", "${"%.1f".format(state.avgTemperatureC)}\u00B0C", "puncak ${"%.1f".format(state.maxTemperatureC)}\u00B0C")
        SummaryCard(Modifier.weight(1f), "Cycle", "${state.cyclesInPeriod}x", "30 hari terakhir")
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, label: String, value: String, sub: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Grafik garis ringan berbasis Compose Canvas (tanpa dependency chart eksternal). */
@Composable
private fun LineChart(values: List<Float>, minValue: Float, maxValue: Float, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (values.size < 2) return@Canvas
        val range = (maxValue - minValue).coerceAtLeast(0.01f)
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

