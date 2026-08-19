package com.powervault.health.pro.ui.screens.rules

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
 * Tab Aturan Cerdas. Engine evaluasi rule (IF suhu>40 AND charging THEN alarm) sudah
 * berjalan di BatteryMonitorService; UI builder untuk membuat/edit rule dikerjakan
 * di batch berikutnya.
 */
@Composable
fun RulesScreen() {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Aturan Cerdas", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Contoh: IF suhu>40\u00B0C AND charging THEN alarm. Editor rule segera hadir.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
