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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltcare.app.util.AppUsageInfo
import com.voltcare.app.util.HibernateWhitelistStore
import com.voltcare.app.util.HibernateWorker
import com.voltcare.app.util.ShizukuManager
import com.voltcare.app.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var hibernateEnabled by remember { mutableStateOf(HibernateWhitelistStore.isEnabled(context)) }
    var whitelist by remember { mutableStateOf(HibernateWhitelistStore.getAll(context)) }
    // Pending #19 (2/2): true hanya kalau Shizuku aktif DAN dumpsys batterystats berhasil
    // di-parse jadi minimal 1 baris mAh riil - menentukan apakah hint "data riil" ditampilkan.
    var hasRealDrainData by remember { mutableStateOf(false) }
    // Batch 54 (permintaan user): toggle "Tampilkan Semua App" - hanya efektif kalau
    // hasRealDrainData true (lihat gating `enabled` di Switch-nya di bawah).
    var showAllDrainApps by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger, showAllDrainApps) {
        hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)

        // Wiring Pending #19 (2/2): dumpsys via Shizuku = shell process blocking (Process.waitFor),
        // WAJIB dijalankan di luar Main dispatcher - LaunchedEffect defaultnya Main, jadi dibungkus
        // withContext(IO) supaya UI tidak freeze menunggu proses shell selesai.
        val mahByPackage = withContext(Dispatchers.IO) {
            UsageStatsHelper.fetchDrainMahByPackage(context)
        }
        hasRealDrainData = !mahByPackage.isNullOrEmpty()

        apps = if (showAllDrainApps && hasRealDrainData) {
            // Batch 54: mode "Semua App" - dibangun langsung dari mahByPackage, TIDAK dibatasi
            // top-15 waktu pemakaian spt jalur default di bawah.
            withContext(Dispatchers.IO) {
                UsageStatsHelper.fullDrainAppList(context, mahByPackage.orEmpty())
            }
        } else {
            val proxyApps = if (hasPermission) UsageStatsHelper.topAppsByForegroundUsage(context) else emptyList()
            UsageStatsHelper.mergeDrainData(proxyApps, mahByPackage)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Drain Analyzer", style = MaterialTheme.typography.headlineMedium)

            // Pending #12 (FEATURE_PARITY_GOALS.md): Auto-Hibernate Terjadwal via WorkManager,
            // HANYA untuk app yang di-approve eksplisit user lewat checkbox per app di bawah -
            // scheduler tidak pernah menyentuh app di luar whitelist.
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Hibernate Terjadwal", fontWeight = FontWeight.Bold)
                        Text(
                            if (whitelist.isEmpty()) "Belum ada app di whitelist (centang di bawah)"
                            else "Tiap 30 menit, force-stop ${whitelist.size} app whitelist",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = hibernateEnabled,
                        enabled = whitelist.isNotEmpty(),
                        onCheckedChange = { checked ->
                            if (checked) HibernateWorker.schedule(context) else HibernateWorker.cancel(context)
                            hibernateEnabled = checked
                        }
                    )
                }
            }

            // Batch 54 (permintaan user): toggle tampilkan SEMUA app dari dumpsys (bukan cuma
            // yang masuk 15 besar waktu pemakaian). Disabled sampai hasRealDrainData true -
            // tanpa data mAh riil, tidak ada apa pun tambahan yang bisa ditampilkan mode ini.
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tampilkan Semua App (dumpsys)", fontWeight = FontWeight.Bold)
                        Text(
                            if (hasRealDrainData) {
                                "Termasuk app dgn waktu pemakaian rendah/0 dalam 24 jam, asal tercatat mAh riil"
                            } else {
                                "Butuh Shizuku aktif & data mAh riil dulu"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = showAllDrainApps,
                        enabled = hasRealDrainData,
                        onCheckedChange = { showAllDrainApps = it }
                    )
                }
            }

            if (!showAllDrainApps && !hasPermission) {
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
                    if (showAllDrainApps) {
                        "Tidak ada app dgn data mAh riil yang cocok ke package terinstall."
                    } else {
                        "Belum ada data pemakaian signifikan dalam 24 jam terakhir."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    when {
                        showAllDrainApps && hasRealDrainData ->
                            "Menampilkan SEMUA app dari dumpsys batterystats (Shizuku, sejak charge penuh " +
                                "terakhir), diurutkan mAh tertinggi - termasuk app dgn waktu pemakaian " +
                                "rendah/0 dalam 24 jam. \"Force Stop\" bersifat best-effort."
                        hasRealDrainData ->
                            "Kolom mAh dari dumpsys batterystats (Shizuku, sejak charge penuh terakhir) - " +
                                "app tanpa data mAh riil tetap diurutkan berdasar waktu pemakaian. " +
                                "\"Force Stop\" bersifat best-effort."
                        else ->
                            "Diurutkan dari waktu pemakaian tertinggi (proxy - lihat catatan di UsageStatsHelper.kt). " +
                                "\"Force Stop\" bersifat best-effort."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        DrainAppRow(
                            app = app,
                            isWhitelisted = whitelist.contains(app.packageName),
                            onToggleWhitelist = {
                                HibernateWhitelistStore.toggle(context, app.packageName)
                                whitelist = HibernateWhitelistStore.getAll(context)
                            },
                            onForceStop = {
                                val success = UsageStatsHelper.killBackgroundApp(context, app.packageName)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "${app.appLabel} dihentikan" else "Gagal menghentikan ${app.appLabel}"
                                    )
                                }
                                refreshTrigger++
                            },
                            onOpenSettings = {
                                UsageStatsHelper.openAppDetailsSettings(context, app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrainAppRow(
    app: AppUsageInfo,
    isWhitelisted: Boolean,
    onToggleWhitelist: () -> Unit,
    onForceStop: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appLabel, fontWeight = FontWeight.Bold)
                    Text(
                        UsageStatsHelper.formatDuration(app.totalForegroundMs),
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Pending #19 (2/2): tampil hanya kalau mergeDrainData() berhasil match
                    // package ini ke UID di dumpsys batterystats - app lain di daftar yang
                    // sama tetap tampil tanpa baris ini (bukan dihilangkan, hanya tidak ada
                    // data riilnya di siklus charge saat ini).
                    app.mahEstimate?.let { mah ->
                        Text(
                            "\u2248 ${"%.2f".format(mah)} mAh (riil, sejak charge terakhir)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (isActionable(app.packageName)) {
                    Checkbox(checked = isWhitelisted, onCheckedChange = { onToggleWhitelist() })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isActionable(app.packageName)) {
                    OutlinedButton(onClick = onForceStop) {
                        Text("Force Stop")
                    }
                }
                // Pending #13 (FEATURE_PARITY_GOALS.md): best-effort shortcut - buka dialog
                // App Info bawaan Android per app, TIDAK otomatis (lihat komentar
                // UsageStatsHelper.openAppDetailsSettings). Selalu tampil utk SEMUA app
                // (TIDAK digate isActionable) karena murni navigasi ke Settings sistem,
                // beda dgn Force Stop yang benar-benar mengeksekusi aksi berisiko.
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Pengaturan App")
                }
            }
        }
    }
}

/**
 * Fix bug (dilaporkan user via screenshot): `AppUsageInfo.isSystemApp` (dari `FLAG_SYSTEM`)
 * menandai HAMPIR SEMUA app bawaan OEM sebagai "system" - termasuk app yang user pakai
 * sehari-hari & aman di-force-stop (Launcher/"Peluncur XOS", "Jam", komponen Transsion
 * "TranResolver"), khususnya di ROM custom (mis. Transsion XOS) yang menandai banyak app
 * preinstall sbg system partition walau fungsinya persis app biasa. Akibatnya SELURUH baris
 * kehilangan checkbox+tombol Force Stop di device seperti itu - bukan bug render, blanket
 * filter yang kelewat luas.
 *
 * Diganti jadi blocklist EKSPLISIT hanya untuk komponen sistem yang benar-benar kritis kalau
 * di-force-stop (bisa bikin UI sistem crash/reboot loop) - selain itu, SEMUA app (termasuk
 * yang FLAG_SYSTEM) tetap actionable, sesuai maksud awal fitur.
 */
private val CRITICAL_SYSTEM_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone"
)

private fun isActionable(packageName: String): Boolean = packageName !in CRITICAL_SYSTEM_PACKAGES
