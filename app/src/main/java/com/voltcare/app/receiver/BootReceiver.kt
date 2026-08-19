package com.voltcare.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.voltcare.app.service.BatteryMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, BatteryMonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
