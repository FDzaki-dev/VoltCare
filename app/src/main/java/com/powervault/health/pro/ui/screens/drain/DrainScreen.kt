package com.powervault.health.pro.ui.screens.drain

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
 * Tab Penguras (Drain Analyzer). Scaffold awal - integrasi UsageStatsManager
 * untuk top app penguras + aksi force-stop dikerjakan di batch berikutnya.
 */
@Composable
fun DrainScreen() {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Drain Analyzer", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Deteksi top app penguras baterai saat layar mati segera hadir.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
