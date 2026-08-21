package com.voltcare.app

import android.Manifest
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

    private fun startMonitorService() {
        val intent = Intent(this, BatteryMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
