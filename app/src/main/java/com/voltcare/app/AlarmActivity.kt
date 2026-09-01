package com.voltcare.app

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltcare.app.service.BatteryMonitorService
import com.voltcare.app.ui.theme.VoltCareTheme
import com.voltcare.app.util.AlarmPlayer

/**
 * Pending Queue #43 (Batch 93): Activity dedicated "Alarm Berbunyi!" ala alarm clock bawaan
 * Android - dipicu via `NotificationCompat.setFullScreenIntent()` (lihat KDoc
 * `BatteryMonitorService.fireAlert()` / `AlarmCheckReceiver.postAlertNotification()`, keduanya
 * kirim PendingIntent ke Activity ini HANYA utk rule beraksi ALARM, bukan NOTIFY). Tampil DI
 * ATAS lockscreen (`showWhenLocked`/`turnScreenOn` di manifest - minSdk 29 project ini sudah >
 * API 27 minimum kedua atribut ini, jadi TIDAK perlu fallback `WindowManager.LayoutParams` versi
 * lama) TANPA ikut membuka kunci device - sengaja, beda dari tap notifikasi biasa (buka
 * MainActivity via alur unlock normal Android); layar ini murni "tampil di depan lockscreen",
 * persis pola app Jam/Alarm bawaan (alarm dering tetap kelihatan+bisa dimatikan walau device
 * masih terkunci, user tetap harus unlock manual sendiri kalau mau pakai HP setelahnya).
 *
 * `launchMode="singleTask"` (manifest) + [onNewIntent] di bawah: kalau rule ALARM lain fire SAAT
 * layar ini masih terbuka (mis. 2 rule berdekatan), instance yang SAMA di-reuse & konten
 * diperbarui ke rule terbaru - bukan numpuk beberapa AlarmActivity di back stack.
 *
 * Back-press SENGAJA di-no-op (lihat [OnBackPressedCallback] di bawah) - pola sama dgn notifikasi
 * `setOngoing(true)` sejak Batch 88: alarm cuma boleh hilang lewat tombol "Matikan Alarm" yang
 * eksplisit, bukan tidak sengaja ke-back saat device baru menyala.
 *
 * Tombol "Matikan Alarm" di sini SENGAJA reuse persis `AlarmPlayer.stop(ruleId)` + cancel
 * notifikasi yang sama dgn `BatteryMonitorService.handleDismissAlarm()` - konsisten, bukan
 * implementasi kedua yang bisa drift beda perilaku dari tombol aksi di notifikasi.
 *
 * Sengaja TIDAK diubah/ditambah di batch ini: prompt izin `USE_FULL_SCREEN_INTENT` API 34
 * (`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`) - pemanggil (`fireAlert()`/
 * `postAlertNotification()`) sudah fail-safe via `canUseFullScreenIntent()` (fallback diam-diam
 * ke notifikasi biasa kalau dicabut user), prompt eksplisit ala `requestDndAccessIfNeeded()`
 * sengaja ditunda ke Pending Queue terpisah biar batch ini tidak melebihi 3 file kode
 * (`MainActivity.kt` akan jadi file ke-4 kalau ditambah sekarang).
 */
class AlarmActivity : ComponentActivity() {

    private var ruleId by mutableStateOf(-1L)
    private var ruleLabel by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        // Wajib SEBELUM super.onCreate() (kontrak API showWhenLocked/turnScreenOn) - pola sama
        // dgn enableEdgeToEdge() di MainActivity.onCreate().
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        super.onCreate(savedInstanceState)

        readIntentExtras(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Sengaja no-op - lihat KDoc class.
            }
        })

        setContent {
            VoltCareTheme {
                AlarmScreen(ruleLabel = ruleLabel, onDismiss = { dismissAlarm() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntentExtras(intent)
    }

    private fun readIntentExtras(intent: Intent) {
        ruleId = intent.getLongExtra(BatteryMonitorService.EXTRA_RULE_ID, -1L)
        ruleLabel = intent.getStringExtra(BatteryMonitorService.EXTRA_RULE_LABEL)
            ?: getString(R.string.app_name)
    }

    private fun dismissAlarm() {
        try {
            AlarmPlayer.stop(ruleId)
            if (ruleId >= 0) {
                getSystemService(NotificationManager::class.java)
                    ?.cancel(ALERT_NOTIF_BASE_ID + ruleId.toInt())
            }
        } catch (e: Throwable) {
            // Fail-safe: gagal stop/cancel tidak boleh menjebak user di layar ini.
        } finally {
            finish()
        }
    }

    companion object {
        // Sengaja SAMA persis dgn ALERT_NOTIF_BASE_ID di BatteryMonitorService.kt/
        // AlarmCheckReceiver.kt (duplikasi kecil disengaja, pola sudah ada di 2 file itu) - ID
        // notifikasi identik per rule.id, wajib konsisten biar cancel() di sini benar sasaran.
        private const val ALERT_NOTIF_BASE_ID = 2000
    }
}

/**
 * Sengaja pakai token `error`/`onError`/`surface`/`onSurface` (SEMUA eksplisit dipetakan +
 * lolos verifikasi kontras WCAG AA sejak Batch 82 - lihat KDoc Theme.kt), BUKAN
 * `errorContainer`/`onErrorContainer` - dua token itu TIDAK dipetakan eksplisit di
 * [VoltCareTheme] (`lightColorScheme()`/`darkColorScheme()` diam-diam fallback ke palet
 * baseline M3 generik utk token yang tidak diisi), yang berarti keluar dari palet
 * custom iOS/Cupertino app ini. Menambah pemetaan itu ke Theme.kt di luar scope Pending
 * Queue #43 (cuma butuh 1 layar ini, bukan alasan sentuh file shared lintas-app).
 */
@Composable
private fun AlarmScreen(ruleLabel: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Alarm Berbunyi!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = ruleLabel,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onError
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = "Matikan Alarm", fontSize = 18.sp)
            }
        }
    }
}
