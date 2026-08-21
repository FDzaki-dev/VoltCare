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
import androidx.core.content.ContextCompat
import com.voltcare.app.MainActivity
import com.voltcare.app.VoltCareApplication
import com.voltcare.app.R
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.BatteryLogEntity
import com.voltcare.app.data.db.entity.CycleEntity
import com.voltcare.app.data.db.entity.RuleEntity
import com.voltcare.app.util.AlarmPlayer
import com.voltcare.app.util.BatterySnapshot
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Foreground service inti: membaca kondisi baterai berkala, menyimpan ke Room,
 * mengevaluasi Aturan Cerdas (smart rules), dan mempertahankan notifikasi persisten
 * dashboard-lite. Cycle counting detail & drain analyzer akan disempurnakan di batch berikutnya
 * (lihat PROJECT_STATE.md > Pending Queue).
 *
 * Batch 64 (Alarm Reliability, 3 root cause dari laporan user):
 * 1. `onTaskRemoved()` + `stopWithTask="false"` (AndroidManifest) - service dulu ikut mati saat
 *    app displit/swipe dari Recents (default Android utk unbound Service), jadi alarm cuma
 *    jalan selama app tidak di-swipe. Sekarang service tetap hidup + auto-restart fail-safe.
 * 2. `firedRuleIds` (edge-triggered, bukan level-triggered) - dulu `fireAlert()` dipanggil ULANG
 *    tiap siklus sampling (60s) selama kondisi tetap true (mis. charger belum dicopot),
 *    menyebabkan alarm bunyi+getar berulang/"looping". Sekarang alarm HANYA bunyi 1x per
 *    episode (saat kondisi baru MULAI terpenuhi), otomatis re-arm saat kondisi kembali false.
 * 3. Tombol aksi "Matikan Alarm" di notifikasi - dulu tidak ada cara hentikan suara/getar yang
 *    sedang jalan selain nunggu kondisi reset sendiri. Sekarang PendingIntent balik ke Service
 *    sendiri (ACTION_DISMISS_ALARM) utk stop AlarmPlayer + cancel notifikasi saat itu juga.
 */
class BatteryMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    private lateinit var db: AppDatabase

    /** Rule ID yang alarmnya SUDAH bunyi & belum re-arm (kondisi belum pernah balik false lagi). */
    private val firedRuleIds = mutableSetOf<Long>()

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
        startForeground(NOTIF_ID, buildNotification("Memantau baterai..."))
        scope.launch { monitorLoop() }
        // Jaring pengaman independen proses (lihat AlarmCheckReceiver) - dijadwalkan di sini
        // biar aktif tiap kali service start (first launch via MainActivity & tiap boot via BootReceiver).
        com.voltcare.app.receiver.AlarmCheckReceiver.schedule(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS_ALARM) {
            handleDismissAlarm(intent.getLongExtra(EXTRA_RULE_ID, -1L))
        }
        return START_STICKY
    }

    /** Root cause #3: hentikan suara/getar yang sedang jalan tanpa perlu tunggu kondisi reset. */
    private fun handleDismissAlarm(ruleId: Long) {
        try {
            AlarmPlayer.stop()
            if (ruleId >= 0) {
                getSystemService(NotificationManager::class.java)
                    ?.cancel(ALERT_NOTIF_BASE_ID + ruleId.toInt())
            }
        } catch (e: Throwable) {
            // Fail-safe: dismiss gagal tidak boleh crash service pemantauan utama.
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Root cause #1: unbound Service default `stopWithTask=true` -> OS mematikan service ini
     * begitu task app di-swipe dari Recents, walau statusnya foreground. Restart diri sendiri
     * di sini sebagai jaring pengaman tambahan (selain atribut manifest `stopWithTask="false"`)
     * utk OEM yang masih agresif membunuh proses background (mis. custom battery manager).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val restartIntent = Intent(applicationContext, BatteryMonitorService::class.java)
            ContextCompat.startForegroundService(applicationContext, restartIntent)
        } catch (e: Throwable) {
            // Fail-safe: kalau restart gagal (mis. dibatasi OS), jangan crash proses.
        }
        super.onTaskRemoved(rootIntent)
    }

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

        processCycleTracking(snapshot, now)
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

    /** Cycle Counter presisi: akumulasi mAh lintas sesi charging (lihat BatteryUtils.CycleTracker). */
    private suspend fun processCycleTracking(snapshot: BatterySnapshot, timestampMs: Long) {
        val result = BatteryUtils.CycleTracker.processSample(
            context = applicationContext,
            isCharging = snapshot.isCharging,
            currentMa = snapshot.currentMa,
            timestampMs = timestampMs,
            sampleIntervalMs = SAMPLE_INTERVAL_MS
        ) ?: return

        db.cycleDao().insert(
            CycleEntity(
                startTimestamp = result.startTimestamp,
                endTimestamp = result.endTimestamp,
                startPercent = -1, // tidak relevan untuk cycle akumulasi (bisa lintas banyak sesi)
                mahDelivered = result.mahDelivered,
                isFullCalibrationCycle = false
            )
        )
    }

    private suspend fun evaluateRules(temperatureC: Float, percent: Int, isCharging: Boolean) {
        val rules = db.ruleDao().enabledOnce()
        rules.forEach { rule -> checkRule(rule, temperatureC, percent, isCharging) }
    }

    private fun checkRule(rule: RuleEntity, temperatureC: Float, percent: Int, isCharging: Boolean) {
        // Batch 73: jadwal hari aktif mirip Google Clock. Hari ini tidak termasuk -> skip
        // total (bukan re-arm firedRuleIds - biar pas hari aktif berikutnya tiba, edge-triggered
        // tetap kerja normal dari kondisi apa pun saat itu, bukan ke-skip krn "sudah pernah fired").
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
        if (!rule.activeDays.split(",").map { it.trim() }.contains(today)) return

        if (rule.requireCharging && !isCharging) {
            firedRuleIds.remove(rule.id) // charger dicopot = kondisi jelas gagal, re-arm langsung
            return
        }
        val triggered = when (rule.conditionType) {
            "TEMP_ABOVE" -> temperatureC > rule.conditionValue
            "PERCENT_ABOVE" -> percent > rule.conditionValue
            "PERCENT_BELOW" -> percent < rule.conditionValue
            else -> false
        }
        if (triggered) {
            // Root cause #2: edge-triggered - hanya bunyi sekali per episode, bukan tiap siklus
            // sampling (60s) selama kondisi tetap true (mis. charger belum dicopot = "looping").
            if (firedRuleIds.add(rule.id)) fireAlert(rule)
        } else {
            firedRuleIds.remove(rule.id) // kondisi balik normal -> re-arm utk episode berikutnya
        }
    }

    private fun fireAlert(rule: RuleEntity) {
        // Wiring AlarmPlayer (Pending Queue #25, Batch 58 sebelumnya belum tersambung):
        // rule.actionType "ALARM" wajib bunyi+getar, bukan cuma notifikasi pasif.
        if (rule.actionType == "ALARM") {
            AlarmPlayer.play(applicationContext, rule.alarmSoundUri, rule.alarmLoop)
        }

        val manager = getSystemService(NotificationManager::class.java) ?: return
        val dismissIntent = Intent(applicationContext, BatteryMonitorService::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_RULE_ID, rule.id)
        }
        val dismissPendingIntent = PendingIntent.getService(
            applicationContext, rule.id.toInt(), dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, VoltCareApplication.CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan: ${rule.label}")
            .setContentText("Kondisi aturan cerdas terpenuhi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Root cause #3: aksi eksplisit hentikan alarm yang sedang bunyi, tanpa perlu
            // tunggu kondisi reset sendiri (mis. cabut charger).
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan Alarm", dismissPendingIntent)
            .build()
        manager.notify(ALERT_NOTIF_BASE_ID + rule.id.toInt(), notification)
    }

    private fun updateNotification(percent: Int, temperatureC: Float, isCharging: Boolean) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val statusText = if (isCharging) "Mengecas" else "Tidak mengecas"
        manager.notify(NOTIF_ID, buildNotification("$percent% - ${temperatureC}\u00B0C - $statusText"))
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
        const val ACTION_DISMISS_ALARM = "com.voltcare.app.action.DISMISS_ALARM"
        const val EXTRA_RULE_ID = "extra_rule_id"
    }
}
