package com.voltcare.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.util.AlarmPlayer
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Jaring pengaman TERAKHIR, INDEPENDEN dari lifecycle BatteryMonitorService.
 * Semua fix sebelumnya (stopWithTask=false, battery optimization exemption,
 * OEM autostart helper, wake lock playback) tetap bergantung pada PROSES
 * service hidup - kalau OS/OEM akhirnya tetap kill proses paksa, alarm ikut
 * mati total. AlarmManager.setExactAndAllowWhileIdle() beda secara fundamental:
 * SISTEM yang jadwalkan & deliver broadcast ini, restart proses app dari nol
 * kalau perlu - pola arsitektur sama persis dengan app Jam/Alarm bawaan Android.
 *
 * Duplikasi kecil logika evaluasi rule dari BatteryMonitorService.checkRule()
 * SENGAJA, bukan lupa reuse: safety net ini wajib tetap kerja sendiri walau
 * service utama sudah mati total, jadi tidak boleh bergantung state in-memory
 * (firedRuleIds) milik service - edge-detection sendiri via SharedPreferences.
 */
class AlarmCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        schedule(appContext) // reschedule dulu (one-shot, bukan repeating - wajib rantai ulang tiap fire)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAndFire(appContext)
            } catch (e: Throwable) {
                // Fail-safe total: safety net ini tidak boleh crash proses.
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun checkAndFire(context: Context) {
        val db = AppDatabase.getInstance(context)
        val snapshot = BatteryUtils.readSnapshot(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        db.ruleDao().enabledOnce().forEach { rule ->
            if (rule.actionType != "ALARM") return@forEach
            val key = firedKey(rule.id)
            if (rule.requireCharging && !snapshot.isCharging) {
                prefs.edit().remove(key).apply()
                return@forEach
            }
            val triggered = when (rule.conditionType) {
                "TEMP_ABOVE" -> snapshot.temperatureC > rule.conditionValue
                "PERCENT_ABOVE" -> snapshot.percent > rule.conditionValue
                "PERCENT_BELOW" -> snapshot.percent < rule.conditionValue
                else -> false
            }
            if (triggered) {
                if (!prefs.getBoolean(key, false)) {
                    prefs.edit().putBoolean(key, true).apply()
                    AlarmPlayer.play(context, rule.alarmSoundUri, rule.alarmLoop)
                }
            } else {
                prefs.edit().remove(key).apply()
            }
        }
    }

    private fun firedKey(ruleId: Long) = "fired_$ruleId"

    companion object {
        private const val PREFS_NAME = "voltcare_alarm_safetynet"
        private const val REQUEST_CODE = 9001

        /** Selaras interval sampling utama BatteryMonitorService (60s). */
        private const val INTERVAL_MS = 60_000L

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AlarmCheckReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Dipanggil saat BatteryMonitorService start (first launch & tiap boot). */
        fun schedule(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    // SCHEDULE_EXACT_ALARM belum di-grant user (Pending Queue: prompt eksplisit
                    // via Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, batch berikutnya) -
                    // fallback inexact, tetap JAUH lebih baik drpd tanpa safety net sama sekali.
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context))
                    return
                }
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context))
            } catch (e: Throwable) {
                // Fail-safe.
            }
        }
    }
}
