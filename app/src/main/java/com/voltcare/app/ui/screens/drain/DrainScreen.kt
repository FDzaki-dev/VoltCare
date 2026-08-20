package com.voltcare.app.ui.screens.drain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltcare.app.util.AppUsageInfo
import com.voltcare.app.util.ShizukuManager
import com.voltcare.app.util.UsageStatsHelper

/**
 * Tab Penguras (Drain Analyzer): top app berdasarkan waktu pemakaian foreground 24 jam
 * terakhir (via UsageStatsManager) + aksi "Force Stop" best-effort per app.
 * Lihat catatan keterbatasan API publik di UsageStatsHelper.kt.
 */
@Composable
fun DrainScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.hasUsageAccessPermission(context)) }
    var apps by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
        apps = if (hasPermission) UsageStatsHelper.topAppsByForegroundUsage(context) else emptyList()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Drain Analyzer", style = MaterialTheme.typography.headlineMedium)

            if (!hasPermission) {
                Text(
                    "Butuh izin \"Akses Penggunaan\" untuk melihat app paling banyak menyita waktu " +
                        "layar 24 jam terakhir (indikator kandidat penguras baterai).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { UsageStatsHelper.openUsageAccessSettings(context) }) {
                    Text("Buka Pengaturan Akses Penggunaan")
                }
                // Batch 41 (Pending #20): shortcut kalau Shizuku sudah aktif & diizinkan -
                // langsung "appops set ... allow" tanpa perlu buka Settings manual sama sekali.
                // Tombol cuma muncul kalau Shizuku Ready (hasPermission()), TIDAK mengubah
                // alur existing untuk user yang belum/tidak pakai Shizuku (tetap harus buka
                // Settings manual via tombol di atas, jalur lama 100% dipertahankan).
                if (ShizukuManager.hasPermission()) {
                    OutlinedButton(onClick = {
                        ShizukuManager.autoGrantUsageAccess(context)
                        refreshTrigger++
                    }) {
                        Text("Izinkan Otomatis via Shizuku")
                    }
                }
                OutlinedButton(onClick = { refreshTrigger++ }) {
                    Text("Sudah diizinkan, muat ulang")
                }
            } else if (apps.isEmpty()) {
                Text(
                    "Belum ada data pemakaian signifikan dalam 24 jam terakhir.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Diurutkan dari waktu pemakaian tertinggi. \"Force Stop\" bersifat best-effort " +
                        "(mematikan proses background cached, tidak sekuat Force Stop di Pengaturan).",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        DrainAppRow(
                            app = app,
                            onForceStop = {
                                UsageStatsHelper.killBackgroundApp(context, app.packageName)
                                refreshTrigger++
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrainAppRow(app: AppUsageInfo, onForceStop: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appLabel, fontWeight = FontWeight.Bold)
                Text(
                    UsageStatsHelper.formatDuration(app.totalForegroundMs),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!app.isSystemApp) {
                OutlinedButton(onClick = onForceStop) {
                    Text("Force Stop")
                }
            }
        }
    }
}
