package com.voltcare.app.ui.screens.stress

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltcare.app.util.BatterySnapshot
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.delay

private const val TEST_DURATION_SEC = 10 * 60
private const val WAKELOCK_TAG = "VoltCare:StressTest"
private const val WAKELOCK_TIMEOUT_MS = 11 * 60 * 1000L // buffer 1 menit di atas durasi tes

private enum class StressState { IDLE, RUNNING, FINISHED }

/**
 * Tab Tes Baterai (Stress Test): sesi tetap 10 menit, mengukur drop persen baterai
 * dari BatteryUtils.readSnapshot() (polling 1 detik, sumber sama dengan Dashboard/service,
 * tidak ada BroadcastReceiver baru). Wake lock PARTIAL dipakai TERKONTROL: diambil TEPAT saat
 * tes benar-benar mulai (bukan saat layar dibuka), dengan timeout eksplisit (buffer 1 menit di
 * atas durasi tes) sebagai safety-net, dan selalu dilepas via LaunchedEffect+DisposableEffect
 * saat tes selesai/dibatalkan/layar ditinggalkan.
 */
@Composable
fun StressTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var testState by remember { mutableStateOf(StressState.IDLE) }
    var remainingSec by remember { mutableStateOf(TEST_DURATION_SEC) }
    var startSnapshot by remember { mutableStateOf<BatterySnapshot?>(null) }
    var currentSnapshot by remember { mutableStateOf(BatteryUtils.readSnapshot(context)) }
    var pluggedDuringTest by remember { mutableStateOf(false) }
    var wakeLock by remember { mutableStateOf<PowerManager.WakeLock?>(null) }

    // Fix (audit UX, terverifikasi lewat baca kode langsung): SEBELUMNYA
    // `val wakeLock = remember { acquirePartialWakeLock(context) }` mengambil wake lock SAAT
    // LAYAR INI PERTAMA KALI DI-COMPOSE - CPU tetap terjaga sia-sia begitu user cuma MEMBUKA
    // layar (IdleCard tampil), walau belum tentu jadi tekan "Mulai Tes". Sekarang lock diambil
    // TEPAT saat testState pindah ke RUNNING, dan dilepas begitu keluar dari RUNNING (baik
    // selesai alami di loop bawah maupun "Hentikan Lebih Awal" dari RunningCard) - satu sumber
    // kebenaran, tidak bergantung jalur mana yang men-trigger transisi state.
    LaunchedEffect(testState) {
        if (testState == StressState.RUNNING) {
            wakeLock = acquirePartialWakeLock(context)
        } else {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        }
    }

    // Safety-net: wake lock TIDAK PERNAH dibiarkan menyala setelah layar ini ditinggalkan,
    // apapun alasannya (tes selesai, dibatalkan, atau user navigasi keluar paksa).
    DisposableEffect(Unit) {
        onDispose { wakeLock?.let { if (it.isHeld) it.release() } }
    }

    LaunchedEffect(testState) {
        if (testState == StressState.RUNNING) {
            while (remainingSec > 0 && testState == StressState.RUNNING) {
                delay(1000)
                remainingSec--
                currentSnapshot = BatteryUtils.readSnapshot(context)
                if (currentSnapshot.isCharging) pluggedDuringTest = true
            }
            if (testState == StressState.RUNNING) testState = StressState.FINISHED
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("< Kembali") }
            Text("Tes Baterai (Stress Test)", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Sesi tetap 10 menit. Mengukur laju drop persen baterai sebagai indikator " +
                    "kesehatan relatif (bukan pengukuran mAh presisi) - lepas charger & " +
                    "biarkan layar menyala untuk hasil paling representatif.",
                style = MaterialTheme.typography.bodyMedium
            )

            when (testState) {
                StressState.IDLE -> IdleCard(
                    isCharging = currentSnapshot.isCharging,
                    onStart = {
                        startSnapshot = currentSnapshot
                        pluggedDuringTest = false
                        remainingSec = TEST_DURATION_SEC
                        testState = StressState.RUNNING
                    }
                )
                StressState.RUNNING -> RunningCard(
                    remainingSec = remainingSec,
                    startPercent = startSnapshot?.percent ?: currentSnapshot.percent,
                    currentPercent = currentSnapshot.percent,
                    onStop = { testState = StressState.FINISHED }
                )
                StressState.FINISHED -> ResultCard(
                    startPercent = startSnapshot?.percent ?: currentSnapshot.percent,
                    endPercent = currentSnapshot.percent,
                    elapsedSec = TEST_DURATION_SEC - remainingSec,
                    pluggedDuringTest = pluggedDuringTest,
                    onRestart = { testState = StressState.IDLE }
                )
            }
        }
    }
}

@Composable
private fun IdleCard(isCharging: Boolean, onStart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCharging) {
                Text(
                    "Charger terpasang. Lepas charger dulu supaya tes mengukur drop asli, bukan sesi cas.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = onStart,
                enabled = !isCharging,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mulai Tes (10 menit)")
            }
        }
    }
}

@Composable
private fun RunningCard(
    remainingSec: Int,
    startPercent: Int,
    currentPercent: Int,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sisa waktu: ${formatMmSs(remainingSec)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Awal: $startPercent%  ->  Sekarang: $currentPercent%")
            Text(
                "Drop sejauh ini: ${(startPercent - currentPercent).coerceAtLeast(0)}%",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Hentikan Lebih Awal")
            }
        }
    }
}

@Composable
private fun ResultCard(
    startPercent: Int,
    endPercent: Int,
    elapsedSec: Int,
    pluggedDuringTest: Boolean,
    onRestart: () -> Unit
) {
    val drop = (startPercent - endPercent).coerceAtLeast(0)
    val minutes = elapsedSec / 60.0
    val ratePerMin = if (minutes > 0) drop / minutes else 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Hasil Tes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Durasi: ${formatMmSs(elapsedSec)}")
            Text("Persen awal: $startPercent%   Persen akhir: $endPercent%")
            Text("Total drop: $drop%")
            Text("Laju drain: ${"%.2f".format(ratePerMin)}%/menit")
            if (pluggedDuringTest) {
                Text(
                    "[!] Charger sempat terpasang selama tes - hasil kemungkinan tidak akurat.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "Catatan: indikator relatif untuk dibandingkan antar sesi tes dari waktu ke waktu, " +
                    "bukan pengukuran health% mAh presisi (lihat Kalibrasi di Dashboard untuk itu).",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text("Tes Lagi")
            }
        }
    }
}

private fun formatMmSs(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

private fun acquirePartialWakeLock(context: Context): PowerManager.WakeLock {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
    lock.acquire(WAKELOCK_TIMEOUT_MS)
    return lock
}
