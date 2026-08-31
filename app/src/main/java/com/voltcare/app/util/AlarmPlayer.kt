package com.voltcare.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * AlarmPlayer (Batch 58 - core engine Custom Alarm; Batch 66 - opsi loop terus-menerus)
 *
 * Memutar nada alarm (custom pilihan user via RingtoneManager.ACTION_RINGTONE_PICKER, atau
 * default sistem TYPE_ALARM jika belum diatur) + getar, dipicu saat RuleAction.ALARM
 * terpenuhi. Fail-safe total (try-catch, tidak pernah throw ke caller) - konsisten pola
 * CrashLogger.kt/ShizukuManager.kt/UpdateManager.kt di project ini.
 *
 * Belum diwiring (Pending Queue baru, Batch 66): `RulesViewModel.saveRule()`/`RuleFormDialog`
 * (RulesScreen.kt) belum punya toggle "Ulangi terus" utk isi `RuleEntity.alarmLoop`, dan
 * `BatteryMonitorService.fireAlert()` masih manggil `play()` tanpa param [loop] (default false,
 * perilaku lama tidak berubah sampai wiring selesai). Pola sama seperti Batch 58 -> 25/26.
 *
 * Batch 89 (fix bug laporan user - "tombol Matikan Alarm cuma matiin notifikasi stale, suara
 * tetap ada; notifikasi fresh yang bisa matiin suaranya"): root cause GANDA, keduanya di sini,
 * bukan di layer notifikasi:
 * 1. [play]/[stop] SEBELUMNYA tidak `@Synchronized` - dipanggil dari 2 jalur independen yang
 *    bisa hampir bersamaan (`BatteryMonitorService.fireAlert()` langsung di coroutine service,
 *    VS `AlarmCheckReceiver` -> ACTION_FIRE_ALARM -> `handleFireAlarmRequest()` di thread
 *    onStartCommand()) - race klasik: kedua panggilan `play()` bisa sama-sama baca
 *    `activeRingtone` masih null saat masing-masing memanggil stop() di awal, lalu SAMA-SAMA
 *    lanjut buat+putar Ringtone baru & assign `activeRingtone` - siapa assign TERAKHIR yang
 *    "menang" jadi satu-satunya yang bisa distop, Ringtone yang KALAH jadi orphan tanpa
 *    referensi - terus bunyi tanpa bisa dihentikan tombol apa pun (persis gejala laporan user).
 * 2. Notifikasi/tombol dismiss dilacak PER rule.id (ID notifikasi beda tiap rule), TAPI
 *    AlarmPlayer ini singleton GLOBAL (cuma 1 suara bisa aktif). Kalau 2 rule ALARM ke-trigger
 *    berdekatan, rule kedua diam-diam menggantikan suara rule pertama (play() manggil stop()
 *    dulu) - notifikasi rule PERTAMA masih nongkrong (ongoing sejak Batch 88, tak bisa
 *    di-swipe) padahal suaranya sendiri sudah mati duluan ("stale"), sedangkan notifikasi rule
 *    KEDUA ("fresh") itulah pemilik suara yang sungguh masih bunyi. handleDismissAlarm()
 *    sebelumnya manggil `AlarmPlayer.stop()` TANPA syarat siapa pemiliknya - tombol notifikasi
 *    stale jadi tidak berefek ke suara (sudah mati duluan), sementara efeknya BISA salah
 *    matikan suara rule lain kalau urutan kebalik. Fix: [activeRuleId] melacak rule pemilik
 *    suara saat ini; [stop] sekarang terima [ruleId] opsional - hanya benar-benar stop kalau
 *    ruleId cocok pemilik saat ini (atau dipanggil tanpa ruleId = force-stop total, dipakai
 *    internal oleh [play] sendiri sebelum mulai suara baru).
 */
object AlarmPlayer {

    /** Ringtone aktif saat ini (kalau ada) supaya bisa distop manual/saat notifikasi di-dismiss. */
    @Volatile private var activeRingtone: Ringtone? = null

    /** Batch 89: rule.id pemilik suara yang SEDANG aktif (-1L = tidak ada/tidak diketahui). */
    @Volatile private var activeRuleId: Long = -1L

    /** Vibrator yang sedang menjalankan pola getar (kalau ada) - dipakai [stopVibration]. */
    @Volatile private var activeVibrator: Vibrator? = null

    /**
     * Root cause "notifikasi tetap ada tapi suara berhenti": notifikasi statis tidak
     * butuh CPU, sedangkan playback Ringtone butuh CPU aktif - tanpa wake lock, CPU
     * suspend (Doze/deep sleep) begitu layar mati dan suara terpotong di tengah jalan.
     * Timeout wajib diisi (bukan acquire tanpa batas) sbg fail-safe kalau [stop] gagal
     * terpanggil - cegah wake lock leak yang malah nguras baterai terus-menerus.
     */
    @Volatile private var wakeLock: PowerManager.WakeLock? = null
    private const val WAKE_LOCK_TIMEOUT_MS = 5 * 60 * 1000L // 5 menit, cukup utk nada terpanjang + loop wajar

    /**
     * Mulai putar alarm. [customSoundUri] dari RuleEntity.alarmSoundUri (nullable - null berarti
     * pakai nada alarm default sistem). [loop] dari RuleEntity.alarmLoop - true = nada + getar
     * diulang terus sampai [stop] dipanggil manual (tombol "Matikan Alarm"); false (default) =
     * main 1x sampai selesai lalu berhenti sendiri. [ruleId] (Batch 89) = rule.id pemilik suara
     * ini, dicatat ke [activeRuleId] supaya [stop] bisa tahu siapa pemilik suara saat ini -
     * default -1L (tidak diketahui) untuk pemanggil lama yang belum sempat dikinikan.
     */
    @Synchronized
    fun play(context: Context, customSoundUri: String?, loop: Boolean = false, ruleId: Long = -1L) {
        try {
            stopInternal() // pastikan tidak ada ringtone lama yang masih diputar bersamaan
            acquireWakeLock(context)
            val uri: Uri = resolveUri(customSoundUri)
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = loop
            }
            ringtone.play()
            activeRingtone = ringtone
            activeRuleId = ruleId
            vibrate(context, loop)
        } catch (e: Throwable) {
            // Fail-safe, sesuai konvensi project: tidak pernah throw ke caller.
        }
    }

    /**
     * Hentikan ringtone yang sedang diputar (kalau ada). Aman dipanggil kapan pun.
     * Batch 89: [ruleId] opsional - kalau diisi (>= 0) DAN ada pemilik suara saat ini yang
     * BEDA rule (bukan -1L/tidak diketahui), panggilan ini di-skip total (rule ini bukan
     * pemilik suara yang sedang aktif, jangan matikan suara rule lain secara tidak sengaja).
     * Dipanggil tanpa argumen (dari [play] sendiri, lewat [stopInternal]) = force-stop total
     * tanpa syarat, dipakai internal sebelum mulai suara baru.
     */
    @Synchronized
    fun stop(ruleId: Long = -1L) {
        if (ruleId >= 0 && activeRuleId >= 0 && ruleId != activeRuleId) return
        stopInternal()
    }

    private fun stopInternal() {
        try {
            activeRingtone?.let { if (it.isPlaying) it.stop() }
        } catch (e: Throwable) {
            // Fail-safe.
        } finally {
            activeRingtone = null
            activeRuleId = -1L
        }
        stopVibration()
        releaseWakeLock()
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoltCare:AlarmWakeLock")
            lock.setReferenceCounted(false)
            lock.acquire(WAKE_LOCK_TIMEOUT_MS)
            wakeLock = lock
        } catch (e: Throwable) {
            // Fail-safe: gagal acquire wake lock tidak boleh gagalkan alarm secara total.
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Throwable) {
            // Fail-safe.
        } finally {
            wakeLock = null
        }
    }

    /** Uri custom kalau valid & bisa di-parse, fallback ke TYPE_ALARM default sistem. */
    private fun resolveUri(customSoundUri: String?): Uri {
        if (!customSoundUri.isNullOrBlank()) {
            try {
                return Uri.parse(customSoundUri)
            } catch (e: Throwable) {
                // Fallback ke default di bawah.
            }
        }
        return RingtoneManager.getActualDefaultRingtoneUri(null, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    private fun currentVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrate(context: Context, loop: Boolean) {
        try {
            val vibrator = currentVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            activeVibrator = vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (loop) {
                    // Pola getar-jeda diulang terus (repeat index 0) selaras nada yang looping,
                    // dihentikan bareng stop() saat user pencet "Matikan Alarm".
                    VibrationEffect.createWaveform(longArrayOf(700, 300), 0)
                } else {
                    VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                if (loop) vibrator.vibrate(longArrayOf(0, 700, 300), 0) else vibrator.vibrate(700)
            }
        } catch (e: Throwable) {
            // Fail-safe.
        }
    }

    private fun stopVibration() {
        try {
            activeVibrator?.cancel()
        } catch (e: Throwable) {
            // Fail-safe.
        } finally {
            activeVibrator = null
        }
    }
}
