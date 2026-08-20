package com.voltcare.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    startAction: @Composable () -> Unit = {},
    endAction: @Composable () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val calibrating by viewModel.calibrationInProgress.collectAsState()

    // TIDAK pakai Scaffold di sini -- screen ini sudah dibungkus Scaffold di NavGraph.kt.
    // Insets/systemBars sudah dikonsumsi SATU KALI di titik itu (Aturan Emas: hindari
    // redundansi, lihat dokumentasi_insets_targetsdk34.md & PROJECT_STATE Batch 31).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Batch 32 fix: Shizuku & Update dulu di-overlay absolut (Box+padding top=64dp)
            // di NavGraph.kt -> numpuk di atas kartu Health/Suhu di layar kecil/font besar.
            // Sekarang jadi bagian alur Row (bukan overlay), jadi TIDAK BISA overlap lagi.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    startAction()
                    endAction()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Health",
                    value = "${state.healthPercent}%",
                    sub = state.healthLabel
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Suhu",
                    value = "${state.temperatureC}\u00B0C"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Volt",
                    value = "${state.voltage}V"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Cas",
                    value = "${state.currentMa}mA",
                    sub = state.chargerSpeedLabel
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = state.estimateLabel,
                    value = formatEstimate(state.estimateMinutes)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Cycle",
                    value = "${state.cycleCount}x"
                )
            }

            Button(
                onClick = { viewModel.startCalibration() },
                enabled = !calibrating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (calibrating) "Kalibrasi berjalan..." else "Mulai Kalibrasi")
            }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sub: String? = null
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sub != null) {
                Text(text = sub, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatEstimate(minutes: Int): String {
    if (minutes < 0) return "-"
    val h = minutes / 60
    val m = minutes % 60
    return "${h}j ${m}m"
}
