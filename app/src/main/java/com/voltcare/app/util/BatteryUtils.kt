package com.voltcare.app.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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

    /**
     * Kalibrasi Health%: alur wajib 3x siklus charge 0%->100% berturut-turut tanpa "drop"
     * (persen turun saat charging = sesi tidak stabil/terputus, siklus dibatalkan & streak
     * direset ke 0 karena syaratnya berturut-turut). State disimpan di SharedPreferences agar
     * tahan proses BatteryMonitorService di-kill / device reboot di tengah sesi.
     * Dipanggil dari BatteryMonitorService tiap sampling; UI (DashboardViewModel) hanya baca
     * status via isActive()/calibratedHealthPercent().
     */
    object CalibrationStore {
        private const val PREFS = "voltcare_calibration"
        private const val KEY_ACTIVE = "active"
        private const val KEY_STAGE = "stage" // 0 = menunggu titik mulai (~0%), 1 = memantau sesi charging
        private const val KEY_START_PERCENT = "start_percent"
        private const val KEY_START_TS = "start_ts"
        private const val KEY_LAST_PERCENT = "last_percent"
        private const val KEY_MAH_ACCUM = "mah_accum"
        private const val KEY_STREAK = "streak"
        private const val KEY_CALIBRATED_HEALTH = "calibrated_health"

        private const val MAX_START_PERCENT = 5
        private const val DROP_TOLERANCE_PERCENT = 1
        private const val TARGET_STREAK = 3

        data class PendingCycleResult(
            val startTimestamp: Long,
            val endTimestamp: Long,
            val startPercent: Int,
            val mahDelivered: Float,
            val calibrationComplete: Boolean,
            val resultHealthPercent: Int?
        )

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

        fun calibratedHealthPercent(context: Context): Int? {
            val v = prefs(context).getInt(KEY_CALIBRATED_HEALTH, -1)
            return if (v in 0..100) v else null
        }

        /** User menekan "Mulai Kalibrasi": aktifkan alur & reset progres sesi berjalan (streak lama tetap). */
        fun activate(context: Context) {
            prefs(context).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putInt(KEY_STAGE, 0)
                .putFloat(KEY_MAH_ACCUM, 0f)
                .apply()
        }

        /**
         * Proses 1 sample baterai. Return non-null hanya pada tick yang menyelesaikan 1 siklus
         * penuh (percent >= 99 setelah sesi charging valid tanpa drop) -> caller WAJIB insert
         * ke CycleEntity(isFullCalibrationCycle = true).
         */
        fun processSample(
            context: Context,
            percent: Int,
            isCharging: Boolean,
            currentMa: Int,
            timestampMs: Long,
            sampleIntervalMs: Long
        ): PendingCycleResult? {
            val p = prefs(context)
            if (!p.getBoolean(KEY_ACTIVE, false)) return null

            val stage = p.getInt(KEY_STAGE, 0)
            val lastPercent = p.getInt(KEY_LAST_PERCENT, -1)
            p.edit().putInt(KEY_LAST_PERCENT, percent).apply()

            if (stage == 0) {
                // Menunggu titik mulai: baterai harus nyaris habis & sedang charging.
                if (isCharging && percent in 0..MAX_START_PERCENT) {
                    p.edit()
                        .putInt(KEY_STAGE, 1)
                        .putInt(KEY_START_PERCENT, percent)
                        .putLong(KEY_START_TS, timestampMs)
                        .putFloat(KEY_MAH_ACCUM, 0f)
                        .apply()
                }
                return null
            }

            // stage == 1: sedang memantau sesi charging menuju 100%.
            if (!isCharging) {
                resetProgress(p) // charger dicabut sebelum penuh -> siklus gagal, streak reset
                return null
            }
            if (lastPercent in 0..100 && percent < lastPercent - DROP_TOLERANCE_PERCENT) {
                resetProgress(p) // persen turun signifikan saat charging -> sesi tidak stabil
                return null
            }

            val mahAccum = p.getFloat(KEY_MAH_ACCUM, 0f) +
                (currentMa.toFloat() * (sampleIntervalMs / 3_600_000f))
            p.edit().putFloat(KEY_MAH_ACCUM, mahAccum).apply()

            if (percent >= 99) {
                val newStreak = p.getInt(KEY_STREAK, 0) + 1
                val startTs = p.getLong(KEY_START_TS, timestampMs)
                val startPercent = p.getInt(KEY_START_PERCENT, 0)
                p.edit().putInt(KEY_STREAK, newStreak).putInt(KEY_STAGE, 0).apply()

                var finishedHealthPercent: Int? = null
                if (newStreak >= TARGET_STREAK) {
                    val health = ((mahAccum / DEFAULT_DESIGN_CAPACITY_MAH) * 100f)
                        .toInt().coerceIn(0, 100)
                    p.edit()
                        .putInt(KEY_CALIBRATED_HEALTH, health)
                        .putBoolean(KEY_ACTIVE, false)
                        .putInt(KEY_STREAK, 0)
                        .apply()
                    finishedHealthPercent = health
                }

                return PendingCycleResult(
                    startTimestamp = startTs,
                    endTimestamp = timestampMs,
                    startPercent = startPercent,
                    mahDelivered = mahAccum,
                    calibrationComplete = finishedHealthPercent != null,
                    resultHealthPercent = finishedHealthPercent
                )
            }
            return null
        }

        private fun resetProgress(p: SharedPreferences) {
            p.edit()
                .putInt(KEY_STAGE, 0)
                .putInt(KEY_STREAK, 0)
                .putFloat(KEY_MAH_ACCUM, 0f)
                .apply()
        }
    }

    /**
     * Cycle Counter presisi (standar industri): 1 cycle = total mAh masuk yang terakumulasi
     * setara 1x kapasitas desain, boleh lintas banyak sesi charging kecil (TIDAK harus 0-100%
     * sekali jalan tanpa putus - itu syarat khusus CalibrationStore). Berjalan independen dari
     * kalibrasi; keduanya sengaja dicatat terpisah ke cycle_history via flag
     * isFullCalibrationCycle agar sumber datanya tetap bisa dibedakan.
     */
    object CycleTracker {
        private const val PREFS = "voltcare_cycle_tracker"
        private const val KEY_ACCUM_MAH = "accum_mah"
        private const val KEY_START_TS = "start_ts"

        data class CycleResult(
            val startTimestamp: Long,
            val endTimestamp: Long,
            val mahDelivered: Float
        )

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** Return non-null hanya pada tick yang menggenapkan 1 cycle (akumulasi >= kapasitas desain). */
        fun processSample(
            context: Context,
            isCharging: Boolean,
            currentMa: Int,
            timestampMs: Long,
            sampleIntervalMs: Long,
            designCapacityMah: Int = DEFAULT_DESIGN_CAPACITY_MAH
        ): CycleResult? {
            if (!isCharging || currentMa <= 0) return null
            val p = prefs(context)
            val startTs = if (p.contains(KEY_START_TS)) p.getLong(KEY_START_TS, timestampMs) else timestampMs
            val accumMah = p.getFloat(KEY_ACCUM_MAH, 0f) +
                (currentMa.toFloat() * (sampleIntervalMs / 3_600_000f))

            if (accumMah >= designCapacityMah) {
                val remainder = accumMah - designCapacityMah
                p.edit()
                    .putFloat(KEY_ACCUM_MAH, remainder)
                    .putLong(KEY_START_TS, timestampMs)
                    .apply()
                return CycleResult(startTs, timestampMs, designCapacityMah.toFloat())
            }

            p.edit()
                .putFloat(KEY_ACCUM_MAH, accumMah)
                .putLong(KEY_START_TS, startTs)
                .apply()
            return null
        }
    }
}
