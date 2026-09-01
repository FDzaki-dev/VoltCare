package com.voltcare.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.voltcare.app.AlarmActivity
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
 *
 * Batch 88 (fix bug laporan user - "bunyi, tapi setelah tab notifikasi ditarik langsung ilang"):
 * Notifikasi alert (`fireAlert()`/`AlarmCheckReceiver.postAlertNotification()`) sebelumnya
 * `setAutoCancel(true)` TANPA `setDeleteIntent()` - secara arsitektur SWIPE-dismiss TIDAK
 * pernah memanggil PendingIntent apa pun (beda dari tap tombol "Matikan Alarm"), jadi swipe
 * murni tidak bisa menghentikan AlarmPlayer via kode. Kemungkinan besar penyebab sebenarnya:
 * (a) jari user tidak sengaja kena tombol "Matikan Alarm" saat mencoba menggeser notifikasi
 * (tombol ada persis di notifikasi yang sama), ATAU (b) `rule.alarmLoop` default `false` (main
 * 1x sampai selesai lalu berhenti sendiri, lihat RuleEntity.kt) - nada berhenti wajar sekitar
 * waktu yang sama user menarik notification shade, bukan krn ditarik. Fix defensif (menutup
 * kemungkinan (a) sepenuhnya, independen dari root cause pasti): notifikasi ALARM sekarang
 * `setOngoing(true)` (TIDAK bisa di-swipe sama sekali) - satu-satunya cara hilang adalah tap
 * eksplisit "Matikan Alarm" (yang tetap `manager.cancel()` notifikasi via kode di
 * handleDismissAlarm(), independen dari flag ongoing/autoCancel). Rule NOTIFY (tanpa suara)
 * TIDAK diubah - tetap `setAutoCancel(true)`/swipeable seperti sebelumnya.
 *
 * Batch 93 (Pending Queue #43 - full-screen intent ala alarm clock): `fireAlert()` sekarang
 * `setFullScreenIntent()` ke [AlarmActivity] baru, HANYA utk rule ALARM, guarded
 * `canUseFullScreenIntent()` (API 34+, fail-safe fallback ke notifikasi biasa kalau izin
 * dicabut user). Lihat KDoc lengkap di [AlarmActivity] & [canUseFullScreenIntent].
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

    /**
     * Batch 89 (fix bug laporan user - "rule yang kondisinya udah kepenuhan pas dibikin gak
     * bunyi sendiri, harus buka app dulu baru bunyi"): SEBELUMNYA evaluateRules() cuma
     * dipanggil dari sampleAndPersist() di dalam monitorLoop() (siklus SAMPLE_INTERVAL_MS,
     * 60 detik) - rule baru/diedit dari RulesViewModel (proses SAMA, lihat AppDatabase
     * singleton getInstance()) memang ke-pickup di siklus BERIKUTNYA (maks ~60 detik), TAPI
     * user yang keburu tutup app sebelum siklus itu lewat mengira rule tidak jalan sama
     * sekali - satu-satunya momen "kelihatan jalan" adalah pas app dibuka lagi (kalau proses
     * service kebetulan sempat mati, onCreate() -> monitorLoop() sampling PERTAMA langsung
     * tanpa nunggu). Fix: Room InvalidationTracker (API resmi, BUKAN polling tambahan) -
     * begitu tabel `smart_rule` berubah, evaluateRulesNow() jalan SAAT ITU JUGA, independen
     * dari kapan siklus 60 detik berikutnya jatuh. Sengaja BUKAN sampleAndPersist() penuh -
     * itu ikut insert ke battery_log/cycle_history tiap panggil, padahal rule baru cuma butuh
     * baca kondisi terkini, bukan menambah catatan riwayat di luar jadwal sampling normal.
     */
    private val ruleTableObserver = object : androidx.room.InvalidationTracker.Observer("smart_rule") {
        override fun onInvalidated(tables: Set<String>) {
            scope.launch { evaluateRulesNow() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(applicationContext)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        db.invalidationTracker.addObserver(ruleTableObserver)
        startForeground(NOTIF_ID, buildNotification("Memantau baterai..."))
        scope.launch { monitorLoop() }
        // Jaring pengaman independen proses (lihat AlarmCheckReceiver) - dijadwalkan di sini
        // biar aktif tiap kali service start (first launch via MainActivity & tiap boot via BootReceiver).
        com.voltcare.app.receiver.AlarmCheckReceiver.schedule(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS_ALARM -> handleDismissAlarm(intent.getLongExtra(EXTRA_RULE_ID, -1L))
            ACTION_FIRE_ALARM -> handleFireAlarmRequest(intent)
        }
        return START_STICKY
    }

    /**
     * Root cause #3: hentikan suara/getar yang sedang jalan tanpa perlu tunggu kondisi reset.
     * Batch 89: `AlarmPlayer.stop(ruleId)` (bukan lagi `stop()` tanpa syarat) - lihat KDoc
     * AlarmPlayer.kt. Notifikasi rule ini SELALU tetap di-cancel di bawah apa pun hasilnya
     * (rule ini boleh saja bukan pemilik suara aktif - notifikasinya tetap wajib hilang saat
     * user tap "Matikan Alarm").
     */
    private fun handleDismissAlarm(ruleId: Long) {
        try {
            AlarmPlayer.stop(ruleId)
            if (ruleId >= 0) {
                getSystemService(NotificationManager::class.java)
                    ?.cancel(ALERT_NOTIF_BASE_ID + ruleId.toInt())
            }
        } catch (e: Throwable) {
            // Fail-safe: dismiss gagal tidak boleh crash service pemantauan utama.
        }
    }

    /**
     * Batch 86 (fix bug laporan user: "notifikasi doang nyantol, nada dering mati saat app
     * dikill"). SEBELUMNYA AlarmCheckReceiver (safety net independen proses) memanggil
     * AlarmPlayer.play() LANGSUNG di proses BroadcastReceiver-nya sendiri - proses itu
     * ephemeral (dijamin hidup cuma sampai goAsync() selesai, ~beberapa detik), TIDAK ada
     * jaminan prioritas foreground service saat itu, jadi OS bisa reclaim proses & memotong
     * ringtone yang baru mulai diputar - padahal notifikasi (sudah terkirim ke
     * NotificationManagerService, hidup di luar proses app) tetap nyantol. Root cause PERSIS
     * seperti laporan user.
     *
     * Sekarang AlarmCheckReceiver TIDAK lagi memutar alarm sendiri - ia startForegroundService()
     * ke SINI dgn ACTION_FIRE_ALARM (pola sama persis dgn ACTION_DISMISS_ALARM yang sudah ada
     * sejak Batch 64). onCreate() SELALU memanggil startForeground() sebelum onStartCommand()
     * jalan (kontrak Android standar utk service baru), jadi begitu handleFireAlarmRequest()
     * ini dieksekusi, proses SUDAH DIJAMIN berstatus foreground service resmi - prioritas jauh
     * lebih tinggi & stabil drpd proses ephemeral BroadcastReceiver, ringtone tidak lagi ikut
     * mati di tengah jalan.
     */
    private fun handleFireAlarmRequest(intent: Intent) {
        try {
            val soundUri = intent.getStringExtra(EXTRA_ALARM_SOUND_URI)
            val loop = intent.getBooleanExtra(EXTRA_ALARM_LOOP, false)
            // Batch 89: ruleId diteruskan ke AlarmPlayer supaya dismiss tahu pemilik suara ini.
            val ruleId = intent.getLongExtra(EXTRA_RULE_ID, -1L)
            AlarmPlayer.play(applicationContext, soundUri, loop, ruleId)
        } catch (e: Throwable) {
            // Fail-safe: gagal putar alarm tidak boleh crash service pemantauan utama.
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
        try {
            db.invalidationTracker.removeObserver(ruleTableObserver) // Batch 89: simetri dgn addObserver()
        } catch (e: Exception) {
            // Fail-safe: gagal remove tidak boleh cegah service berhenti bersih.
        }
        job.cancel()
        super.onDestroy()
    }

    /**
     * Batch 85 (audit pola "persistent"): SEBELUMNYA loop ini TIDAK dibungkus try-catch -
     * satu kegagalan sesaat di sampleAndPersist() (mis. SQLite lock/storage penuh saat
     * pruneOlderThan(), atau bug getIntProperty() OEM tertentu di BatteryUtils.readSnapshot())
     * akan lolos sebagai unhandled exception. CrashLogger.install() (VoltCareApplication)
     * TETAP meneruskan ke defaultHandler?.uncaughtException() setelah logging - artinya
     * proses TETAP mati, notifikasi persisten ikut hilang, kontras dgn hampir semua kode lain
     * di app ini yang konsisten fail-safe (AlarmPlayer/ShizukuManager/AutostartHelper/semua
     * receiver). Sekarang 1 siklus gagal di-skip, loop lanjut ke siklus berikutnya tanpa
     * menjatuhkan proses - notifikasi & monitoring tetap hidup.
     */
    private suspend fun monitorLoop() {
        while (job.isActive) {
            try {
                sampleAndPersist()
            } catch (e: Throwable) {
                // Fail-safe: 1 sample gagal tidak boleh menjatuhkan seluruh service persisten.
            }
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

    /** Batch 91: `CHANNEL_ALERT` lama dihapus (lihat KDoc VoltCareApplication) - notifikasi
     *  info non-alarm ini pindah ke `CHANNEL_ALERT_NOTIFY` (bukan channel ALARM, tidak perlu
     *  bypass DND utk sekadar info "kalibrasi selesai"). */
    private fun notifyCalibrationDone(healthPercent: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, VoltCareApplication.CHANNEL_ALERT_NOTIFY)
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

    /** Batch 89: dipicu [ruleTableObserver] saat `smart_rule` berubah - baca kondisi terkini
     *  lalu evaluasi SAAT ITU JUGA, tanpa nunggu siklus monitorLoop() berikutnya (lihat KDoc
     *  [ruleTableObserver] utk root cause lengkap). */
    private suspend fun evaluateRulesNow() {
        val snapshot = BatteryUtils.readSnapshot(applicationContext)
        if (snapshot.percent < 0) return
        evaluateRules(snapshot.temperatureC, snapshot.percent, snapshot.isCharging)
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
            // Batch 89: rule.id diteruskan supaya AlarmPlayer tahu pemilik suara ini.
            AlarmPlayer.play(applicationContext, rule.alarmSoundUri, rule.alarmLoop, rule.id)
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
        // Batch 91: channel ALARM (DND bypass, suara disuppress - AlarmPlayer handle sendiri)
        // vs channel NOTIFY (default sound, tanpa bypass) - lihat KDoc VoltCareApplication.
        val channelId = if (rule.actionType == "ALARM") {
            VoltCareApplication.CHANNEL_ALERT_ALARM
        } else {
            VoltCareApplication.CHANNEL_ALERT_NOTIFY
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan: ${rule.label}")
            .setContentText("Kondisi aturan cerdas terpenuhi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Batch 90: kategori standar OS utk notifikasi bertipe alarm vs reminder pasif -
            // permintaan user "terapkan konfigurasi umum alarm/charger-trigger app".
            .setCategory(if (rule.actionType == "ALARM") NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            // Batch 88: rule ALARM -> ongoing (TIDAK bisa di-swipe), rule NOTIFY -> autoCancel
            // seperti sebelumnya (swipeable, tidak ada suara yang perlu dijaga). Lihat KDoc class.
            .setOngoing(rule.actionType == "ALARM")
            .setAutoCancel(rule.actionType != "ALARM")
            // Root cause #3: aksi eksplisit hentikan alarm yang sedang bunyi, tanpa perlu
            // tunggu kondisi reset sendiri (mis. cabut charger).
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan Alarm", dismissPendingIntent)
        // Pending Queue #43 (Batch 93): full-screen intent ala alarm clock - HANYA rule ALARM
        // (rule NOTIFY tetap notifikasi biasa, tidak butuh perhatian sebesar layar penuh).
        if (rule.actionType == "ALARM" && canUseFullScreenIntent(manager)) {
            builder.setFullScreenIntent(fullScreenAlarmPendingIntent(rule), true)
        }
        manager.notify(ALERT_NOTIF_BASE_ID + rule.id.toInt(), builder.build())
    }

    /**
     * API 34+ (Android 14 "UpsideDownCake"): `USE_FULL_SCREEN_INTENT` kini bisa DICABUT user
     * lewat Settings meski sudah dideklarasikan di manifest (beda dari API 33 ke bawah yang
     * selalu granted otomatis begitu dideklarasikan). Cek eksplisit - kalau dicabut, fallback
     * diam-diam ke notifikasi biasa (TANPA full-screen), BUKAN skip notifikasi sama sekali;
     * di bawah API 34 method ini belum ada sama sekali, permission lama selalu berefek.
     */
    private fun canUseFullScreenIntent(manager: NotificationManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    /** Pending Queue #43: PendingIntent ke [AlarmActivity] dedicated - lihat KDoc class di sana. */
    private fun fullScreenAlarmPendingIntent(rule: RuleEntity): PendingIntent {
        val intent = Intent(applicationContext, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_RULE_ID, rule.id)
            putExtra(EXTRA_RULE_LABEL, rule.label)
        }
        return PendingIntent.getActivity(
            applicationContext, ALERT_NOTIF_BASE_ID + rule.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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

        /** Pending Queue #43 (Batch 93): label rule utk ditampilkan di [AlarmActivity]
         *  full-screen - dipakai bersama oleh fireAlert() di sini & postAlertNotification()
         *  di AlarmCheckReceiver.kt (samakan persis, lihat KDoc masing-masing). */
        const val EXTRA_RULE_LABEL = "extra_rule_label"

        /** Batch 86: lihat KDoc handleFireAlarmRequest() - alarm SELALU diputar di proses
         *  service ini (bukan di proses AlarmCheckReceiver), supaya ringtone tidak ikut
         *  mati saat app dikill. */
        const val ACTION_FIRE_ALARM = "com.voltcare.app.action.FIRE_ALARM"
        const val EXTRA_ALARM_SOUND_URI = "extra_alarm_sound_uri"
        const val EXTRA_ALARM_LOOP = "extra_alarm_loop"
    }
}
