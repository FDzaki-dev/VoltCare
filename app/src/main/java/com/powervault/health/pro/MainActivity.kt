package com.powervault.health.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.powervault.health.pro.navigation.PowerVaultNavGraph
import com.powervault.health.pro.service.BatteryMonitorService
import com.powervault.health.pro.ui.theme.PowerVaultTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            startMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationPermissionThenStartService()

        setContent {
            PowerVaultTheme {
                PowerVaultNavGraph()
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

    private fun startMonitorService() {
        val intent = Intent(this, BatteryMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
