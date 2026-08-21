package com.voltcare.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
 */
object AlarmPlayer {

    /** Ringtone aktif saat ini (kalau ada) supaya bisa distop manual/saat notifikasi di-dismiss. */
    @Volatile private var activeRingtone: Ringtone? = null

    /** Vibrator yang sedang menjalankan pola getar (kalau ada) - dipakai [stopVibration]. */
    @Volatile private var activeVibrator: Vibrator? = null

    /**
     * Mulai putar alarm. [customSoundUri] dari RuleEntity.alarmSoundUri (nullable - null berarti
     * pakai nada alarm default sistem). [loop] dari RuleEntity.alarmLoop - true = nada + getar
     * diulang terus sampai [stop] dipanggil manual (tombol "Matikan Alarm"); false (default) =
     * main 1x sampai selesai lalu berhenti sendiri.
     */
    fun play(context: Context, customSoundUri: String?, loop: Boolean = false) {
        try {
            stop() // pastikan tidak ada ringtone lama yang masih diputar bersamaan
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
            vibrate(context, loop)
        } catch (e: Throwable) {
            // Fail-safe, sesuai konvensi project: tidak pernah throw ke caller.
        }
    }

    /** Hentikan ringtone yang sedang diputar (kalau ada). Aman dipanggil kapan pun. */
    fun stop() {
        try {
            activeRingtone?.let { if (it.isPlaying) it.stop() }
        } catch (e: Throwable) {
            // Fail-safe.
        } finally {
            activeRingtone = null
        }
        stopVibration()
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
