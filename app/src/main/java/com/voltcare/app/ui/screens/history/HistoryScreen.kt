package com.voltcare.app.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tab Riwayat (30 Hari). Scaffold awal - grafik Health/Suhu/Cycle & export CSV
 * dikerjakan di batch berikutnya (lihat PROJECT_STATE.md > Pending Queue).
 */
@Composable
fun HistoryScreen() {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Riwayat 30 Hari", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Grafik Health / Suhu / Cycle & export CSV segera hadir.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
