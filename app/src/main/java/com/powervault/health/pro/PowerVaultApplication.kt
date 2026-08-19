package com.powervault.health.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.powervault.health.pro.util.CrashLogger

class PowerVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            getString(R.string.notif_channel_monitor),
            NotificationManager.IMPORTANCE_LOW
        )

        val alertChannel = NotificationChannel(
            CHANNEL_ALERT,
            getString(R.string.notif_channel_alert),
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_MONITOR = "battery_monitor"
        const val CHANNEL_ALERT = "battery_alert"
    }
}
