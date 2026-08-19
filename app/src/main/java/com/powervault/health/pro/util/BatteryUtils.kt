package com.powervault.health.pro.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** Snapshot kondisi baterai pada satu titik waktu. */
data class BatterySnapshot(
    val percent: Int,
    val temperatureC: Float,
    val voltage: Float,
    val currentMa: Int,
    val isCharging: Boolean,
    val chargePlug: Int, // BatteryManager.BATTERY_PLUGGED_*
    val health: Int,     // BatteryManager.BATTERY_HEALTH_*
    val technology: String
)

object BatteryUtils {

    /** Estimasi kapasitas desain (mAh). Nilai default umum untuk HP menengah; dikalibrasi user. */
    const val DEFAULT_DESIGN_CAPACITY_MAH = 5000

    fun readSnapshot(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        // BATTERY_PROPERTY_CURRENT_NOW returns microamps (can be negative when discharging on some OEMs)
        val currentMicroA = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = currentMicroA / 1000

        return BatterySnapshot(
            percent = percent,
            temperatureC = tempTenths / 10f,
            voltage = voltageMv / 1000f,
            currentMa = kotlin.math.abs(currentMa),
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            chargePlug = plugged,
            health = health,
            technology = tech
        )
    }

    /** Estimasi waktu penuh (menit) berdasarkan arus masuk saat ini & sisa kapasitas. */
    fun estimateMinutesToFull(snapshot: BatterySnapshot, designCapacityMah: Int): Int {
        if (!snapshot.isCharging || snapshot.currentMa <= 0 || snapshot.percent < 0) return -1
        val remainingPercent = 100 - snapshot.percent
        val remainingMah = designCapacityMah * remainingPercent / 100f
        val hours = remainingMah / snapshot.currentMa
        return (hours * 60).toInt().coerceAtLeast(0)
    }

    /** Klasifikasi kecepatan charger berdasarkan arus masuk (mA). */
    fun classifyChargerSpeed(currentMa: Int): String = when {
        currentMa <= 0 -> "Tidak mengecas"
        currentMa < 500 -> "Lambat / Charger KW"
        currentMa in 500..1200 -> "Normal"
        currentMa in 1201..2500 -> "Fast Charging"
        else -> "Super Fast Charging"
    }

    fun healthLabel(healthPercent: Int): String = when {
        healthPercent >= 85 -> "Baik"
        healthPercent >= 70 -> "Cukup"
        healthPercent >= 50 -> "Menurun"
        else -> "Buruk"
    }
}
