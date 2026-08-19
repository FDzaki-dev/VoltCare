package com.voltcare.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voltcare.app.MainActivity
import com.voltcare.app.VoltCareApplication
import com.voltcare.app.R
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.BatteryLogEntity
import com.voltcare.app.data.db.entity.CycleEntity
import com.voltcare.app.data.db.entity.RuleEntity
import com.voltcare.app.util.BatterySnapshot
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service inti: membaca kondisi baterai berkala, menyimpan ke Room,
 * mengevaluasi Aturan Cerdas (smart rules), dan mempertahankan notifikasi persisten
 * dashboard-lite. Cycle counting detail & drain analyzer akan disempurnakan di batch berikutnya
 * (lihat PROJECT_STATE.md > Pending Queue).
 */
class BatteryMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    private lateinit var db: AppDatabase

    // Heuristik cycle counter sederhana: akumulasi kenaikan persen saat charging.
    private var accumulatedChargePercent = 0f
    private var lastPercent: Int? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Sticky intent sudah ditangani lewat BatteryUtils.readSnapshot; receiver ini
            // memicu re-evaluasi cepat saat status berubah (colok/cabut charger dll).
            scope.launch { sampleAndPersist() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        startForeground(NOTIF_ID, buildNotification("Memantau baterai\u2026"))
        scope.launch { monitorLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // receiver mungkin belum terdaftar; abaikan agar service tetap fail-safe
        }
        job.cancel()
        super.onDestroy()
    }

    private suspend fun monitorLoop() {
        while (job.isActive) {
            sampleAndPersist()
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    private suspend fun sampleAndPersist() {
        val snapshot = BatteryUtils.readSnapshot(applicationContext)
        if (snapshot.percent < 0) return
        val now = System.currentTimeMillis()

        // Health%: pakai hasil Kalibrasi (3x siklus 0-100% berturut-turut, lihat
        // BatteryUtils.CalibrationStore) begitu tersedia; sebelum itu masih heuristik placeholder.
        val healthPercent = estimateHealthPercent()

        db.batteryLogDao().insert(
            BatteryLogEntity(
                timestamp = now,
                percent = snapshot.percent,
                temperatureC = snapshot.temperatureC,
                voltage = snapshot.voltage,
                currentMa = snapshot.currentMa,
                isCharging = snapshot.isCharging,
                healthPercent = healthPercent
            )
        )

        trackCycle(snapshot.percent, snapshot.isCharging)
        processCalibrationSample(snapshot, now)
        evaluateRules(snapshot.temperatureC, snapshot.percent, snapshot.isCharging)
        updateNotification(snapshot.percent, snapshot.temperatureC, snapshot.isCharging)

        // FIFO retention data mentah: simpan maksimal 30 hari (selaras fitur Riwayat 30 Hari).
        db.batteryLogDao().pruneOlderThan(now - RETENTION_MS)
    }

    private fun estimateHealthPercent(): Int {
        return BatteryUtils.CalibrationStore.calibratedHealthPercent(applicationContext) ?: 87
    }

    /** Proses 1 sample untuk state machine Kalibrasi; insert CycleEntity saat 1 siklus penuh selesai. */
    private suspend fun processCalibrationSample(snapshot: BatterySnapshot, timestampMs: Long) {
        val result = BatteryUtils.CalibrationStore.processSample(
            context = applicationContext,
            percent = snapshot.percent,
            isCharging = snapshot.isCharging,
            currentMa = snapshot.currentMa,
            timestampMs = timestampMs,
            sampleIntervalMs = SAMPLE_INTERVAL_MS
        ) ?: return

        db.cycleDao().insert(
            CycleEntity(
                startTimestamp = result.startTimestamp,
                endTimestamp = result.endTimestamp,
                startPercent = result.startPercent,
                mahDelivered = result.mahDelivered,
                isFullCalibrationCycle = true
            )
        )

        if (result.calibrationComplete) {
            notifyCalibrationDone(result.resultHealthPercent ?: 87)
        }
    }

    private fun notifyCalibrationDone(healthPercent: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, VoltCareApplication.CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kalibrasi selesai")
            .setContentText("3 siklus penuh tercapai. Health baterai terkalibrasi: $healthPercent%")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(CALIBRATION_DONE_NOTIF_ID, notification)
    }

    private fun trackCycle(percent: Int, isCharging: Boolean) {
        val prev = lastPercent
        if (prev != null && isCharging && percent > prev) {
            accumulatedChargePercent += (percent - prev)
        }
        lastPercent = percent
        // Deteksi siklus penuh disempurnakan di batch berikutnya (perlu start/end timestamp +
        // mAh terintegrasi, dicatat sebagai CycleEntity via CycleDao).
    }

    private suspend fun evaluateRules(temperatureC: Float, percent: Int, isCharging: Boolean) {
        val rules = db.ruleDao().enabledOnce()
        rules.forEach { rule -> checkRule(rule, temperatureC, percent, isCharging) }
    }

    private fun checkRule(rule: RuleEntity, temperatureC: Float, percent: Int, isCharging: Boolean) {
        if (rule.requireCharging && !isCharging) return
        val triggered = when (rule.conditionType) {
            "TEMP_ABOVE" -> temperatureC > rule.conditionValue
            "PERCENT_ABOVE" -> percent > rule.conditionValue
            "PERCENT_BELOW" -> percent < rule.conditionValue
            else -> false
        }
        if (triggered) fireAlert(rule)
    }

    private fun fireAlert(rule: RuleEntity) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, VoltCareApplication.CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan: ${rule.label}")
            .setContentText("Kondisi aturan cerdas terpenuhi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(ALERT_NOTIF_BASE_ID + rule.id.toInt(), notification)
    }

    private fun updateNotification(percent: Int, temperatureC: Float, isCharging: Boolean) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val statusText = if (isCharging) "Mengecas" else "Tidak mengecas"
        manager.notify(NOTIF_ID, buildNotification("$percent% \u2022 ${temperatureC}\u00B0C \u2022 $statusText"))
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, VoltCareApplication.CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val ALERT_NOTIF_BASE_ID = 2000
        private const val CALIBRATION_DONE_NOTIF_ID = 2500
        private const val SAMPLE_INTERVAL_MS = 60_000L
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
