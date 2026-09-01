package com.voltcare.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.voltcare.app.navigation.VoltCareNavGraph
import com.voltcare.app.service.BatteryMonitorService
import com.voltcare.app.ui.theme.VoltCareTheme
import com.voltcare.app.util.AutostartHelper

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            startMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paksa perilaku edge-to-edge yang konsisten lintas versi OS (min SDK 29 - target SDK 34).
        // WAJIB dipanggil SEBELUM super.onCreate()/setContent() - lihat dokumentasi insets targetSdk34 Langkah A.
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        ensureNotificationPermissionThenStartService()
        requestIgnoreBatteryOptimization()
        promptAutostartIfNeeded()
        requestExactAlarmPermission()
        requestDndAccessIfNeeded()

        setContent {
            VoltCareTheme {
                VoltCareNavGraph()
            }
        }
    }

    private fun ensureNotificationPermissionThenStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startMonitorService()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startMonitorService()
        }
    }

    /**
     * Root cause klaim "gak force-stop walau di-swipe" ternyata TIDAK cukup dgn
     * stopWithTask=false + onTaskRemoved() saja (Batch 64) - OEM battery manager
     * (MIUI/ColorOS/OneUI/EMUI) tetap bunuh proses di level scheduler-nya sendiri
     * kalau app belum di-whitelist dari battery optimization. Ini exemption resmi
     * via API standar Android (bukan intent OEM-spesifik yg tidak reliable).
     */
    private fun requestIgnoreBatteryOptimization() {
        try {
            val pm = getSystemService(PowerManager::class.java) ?: return
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (e: Throwable) {
            // Fail-safe: sebagian OEM custom ROM tolak/tidak dukung intent ini - jangan crash.
        }
    }

    /**
     * Gap lanjutan dari klaim force-stop: battery optimization exemption TIDAK
     * menjangkau Autostart Manager OEM. Prompt sekali saja (bukan tiap onCreate)
     * biar tidak nag - user tetap bisa buka manual dari Settings OEM kapan pun.
     */
    private fun promptAutostartIfNeeded() {
        val prefs = getSharedPreferences("voltcare_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("autostart_prompted", false)) return
        prefs.edit().putBoolean("autostart_prompted", true).apply()
        AutostartHelper.openIfKnownOem(this)
    }

    /**
     * Pending Queue #29 (Batch 71): AlarmCheckReceiver.schedule() sudah fallback diam2
     * ke inexact kalau izin ini belum ada - di sini prompt EKSPLISIT ke user via halaman
     * sistem `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (API 31+), biar safety net alarm
     * seakurat mungkin, bukan nunggu user ke-Doze lalu telat 60s berkali-kali dulu.
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val am = getSystemService(AlarmManager::class.java) ?: return
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (e: Throwable) {
            // Fail-safe: gagal minta izin tidak boleh crash app - AlarmCheckReceiver ttp fallback inexact.
        }
    }

    /**
     * Pending Queue #44 (Batch 91): channel `battery_alert_alarm` sudah `setBypassDnd(true)`
     * TAPI properti itu TIDAK berefek apa pun sampai user grant izin "Do Not Disturb access"
     * di level sistem (beda dari izin biasa - tidak ada runtime permission dialog, harus lewat
     * halaman Settings khusus). Pola sama persis dgn requestIgnoreBatteryOptimization()/
     * requestExactAlarmPermission() di atas - re-prompt tiap launch selama belum granted
     * (isNotificationPolicyAccessGranted() reliable dicek, beda dari Autostart OEM yang
     * tidak ada API cek statusnya makanya promptAutostartIfNeeded() sengaja cuma sekali).
     */
    private fun requestDndAccessIfNeeded() {
        try {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (!manager.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        } catch (e: Throwable) {
            // Fail-safe: sebagian OEM custom ROM tolak/tidak dukung intent ini - jangan crash.
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, BatteryMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
