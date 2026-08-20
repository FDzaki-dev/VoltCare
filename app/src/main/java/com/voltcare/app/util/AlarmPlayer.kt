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
 * AlarmPlayer (Batch 58 - core engine Custom Alarm, Pending Queue #24)
 *
 * Memutar nada alarm (custom pilihan user via RingtoneManager.ACTION_RINGTONE_PICKER, atau
 * default sistem TYPE_ALARM jika belum diatur) + getar singkat, dipicu saat RuleAction.ALARM
 * terpenuhi. Fail-safe total (try-catch, tidak pernah throw ke caller) - konsisten pola
 * CrashLogger.kt/ShizukuManager.kt/UpdateManager.kt di project ini.
 *
 * Belum diwiring: BatteryMonitorService.fireAlert() masih hanya posting notifikasi (Pending
 * Queue #25), RuleFormDialog di RulesScreen.kt belum punya tombol pilih nada custom (Pending
 * Queue #26). Wrapper ini standalone dulu, meniru pola ShizukuManager Batch 23 (engine dulu,
 * UI wiring batch terpisah).
 */
object AlarmPlayer {

    /** Ringtone aktif saat ini (kalau ada) supaya bisa distop manual/saat notifikasi di-dismiss. */
    @Volatile private var activeRingtone: Ringtone? = null

    /**
     * Mulai putar alarm. [customSoundUri] dari RuleEntity.alarmSoundUri (nullable - null berarti
     * pakai nada alarm default sistem). Tidak loop otomatis (Ringtone sistem tidak looping
     * bawaan) - cukup untuk notifikasi sesaat, bukan pengganti jam alarm penuh.
     */
    fun play(context: Context, customSoundUri: String?) {
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
            ringtone.play()
            activeRingtone = ringtone
            vibrate(context)
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

    private fun vibrate(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(700)
            }
        } catch (e: Throwable) {
            // Fail-safe.
        }
    }
}
