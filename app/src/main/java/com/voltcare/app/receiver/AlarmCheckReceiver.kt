package com.voltcare.app.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.voltcare.app.VoltCareApplication
import com.voltcare.app.data.db.AppDatabase
import com.voltcare.app.data.db.entity.RuleEntity
import com.voltcare.app.service.BatteryMonitorService
import com.voltcare.app.util.AlarmPlayer
import com.voltcare.app.util.BatteryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

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
 *
 * Batch 83 (fix bug laporan user - root cause ditemukan): SEBELUMNYA fungsi
 * checkAndFire() punya baris `if (rule.actionType != "ALARM") return@forEach`
 * di paling awal loop - rule beraksi "Notifikasi saja" (NOTIFY, lihat RuleAction
 * di RulesViewModel.kt) DIAM-DIAM TIDAK PERNAH dievaluasi sama sekali oleh
 * safety net independen-proses ini. Selama proses BatteryMonitorService masih
 * hidup, tidak kelihatan (monitorLoop in-process masih jalan) - TAPI begitu OS
 * benar-benar mematikan proses (yang justru jadi alasan safety net ini dibuat,
 * Batch 71), rule NOTIFY berhenti total dievaluasi, dan baru aktif lagi begitu
 * user buka app manual (MainActivity.startMonitorService() restart service).
 * Persis gejala laporan user: "reminder notifikasi ... gak ke-trigger kecuali
 * app dibuka lagi". Fix: SEMUA rule aktif dievaluasi tanpa memandang actionType
 * - notifikasi SELALU diposting saat kondisi terpenuhi (postAlertNotification,
 * meniru BatteryMonitorService.fireAlert()), suara/getar (AlarmPlayer) tetap
 * eksklusif utk actionType == "ALARM" (tidak berubah).
 *
 * Batch 84 (permintaan user - notifikasi bar persisten juga wajib kebal saat app dikill):
 * SEBELUMNYA safety net ini HANYA mengevaluasi rule (alarm/notifikasi), tidak pernah
 * menyentuh notifikasi monitoring persisten ("Memantau baterai...") - itu murni tanggung
 * jawab `startForeground()` di `BatteryMonitorService.onCreate()`. Kalau proses service
 * benar-benar mati (bukan cuma task di-swipe - kasus itu sudah ditangani `onTaskRemoved()`
 * + `stopWithTask=false` sejak Batch 64), notifikasi persisten ikut hilang total & TIDAK
 * ADA mekanisme independen-proses yang menghidupkannya lagi selain user buka app manual.
 * Sekarang `ensureMonitorServiceAlive()` dipanggil tiap kali alarm ini fire (~60 detik
 * sekali, selaras interval sampling) - `startForegroundService()` ke service yang SUDAH
 * hidup cuma jadi no-op `onStartCommand()` tambahan (aman, pola sama persis dgn
 * `MainActivity.startMonitorService()` yang sudah dipanggil tiap `onCreate()` sejak app
 * pertama dibuat tanpa masalah), TAPI kalau service ternyata sudah mati, panggilan ini
 * me-restart-nya dari nol (`onCreate()` jalan lagi -> `startForeground()` lagi -> notifikasi
 * persisten pulih) - dalam waktu maksimal ~60 detik, independen dari kapan user buka app.
 */
class AlarmCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        schedule(appContext) // reschedule dulu (one-shot, bukan repeating - wajib rantai ulang tiap fire)
        ensureMonitorServiceAlive(appContext) // Batch 84: pulihkan notifikasi bar persisten kalau service mati
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

    /**
     * Batch 84: "ping" BatteryMonitorService tiap kali safety net ini fire. Kalau service
     * masih hidup, ini cuma onStartCommand() tambahan tanpa efek (harmless no-op - pola
     * identik MainActivity.startMonitorService(), sudah terbukti aman sejak Batch 1). Kalau
     * service ternyata sudah mati total, ini me-restart-nya dari nol - notifikasi monitoring
     * persisten ("Memantau baterai...") pulih otomatis tanpa perlu user buka app.
     */
    private fun ensureMonitorServiceAlive(context: Context) {
        try {
            val serviceIntent = Intent(context, BatteryMonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Throwable) {
            // Fail-safe: gagal restart tidak boleh mengganggu evaluasi rule/alarm di bawah.
        }
    }

    private suspend fun checkAndFire(context: Context) {
        val db = AppDatabase.getInstance(context)
        val snapshot = BatteryUtils.readSnapshot(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString()
        db.ruleDao().enabledOnce().forEach { rule ->
            // Batch 74 (Pending #30): samakan persis dgn BatteryMonitorService.checkRule() -
            // skip total (bukan reset firedKey) di hari non-aktif, biar edge-detection konsisten.
            if (!rule.activeDays.split(",").map { it.trim() }.contains(today)) return@forEach
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
                    // Batch 83: dulu baris ini digerbang `actionType == "ALARM"` di level ATAS
                    // loop (rule NOTIFY skip total, lihat KDoc class). Sekarang suara/getar tetap
                    // eksklusif ALARM, TAPI notifikasi (postAlertNotification) jalan utk KEDUANYA.
                    if (rule.actionType == "ALARM") {
                        AlarmPlayer.play(context, rule.alarmSoundUri, rule.alarmLoop)
                    }
                    postAlertNotification(context, rule)
                }
            } else {
                prefs.edit().remove(key).apply()
            }
        }
    }

    /**
     * Batch 83: posting notifikasi mandiri, independen proses Service - meniru persis
     * BatteryMonitorService.fireAlert() (channel, ikon, tombol dismiss balik ke Service)
     * supaya perilaku konsisten baik saat dipicu service yang masih hidup maupun safety
     * net ini saat proses sudah mati total. Dipanggil utk SEMUA rule triggered (ALARM
     * maupun NOTIFY) - beda dari AlarmPlayer.play() yang cuma jalan utk actionType ALARM.
     */
    private fun postAlertNotification(context: Context, rule: RuleEntity) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val dismissIntent = Intent(context, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_DISMISS_ALARM
            putExtra(BatteryMonitorService.EXTRA_RULE_ID, rule.id)
        }
        val dismissPendingIntent = PendingIntent.getService(
            context, rule.id.toInt(), dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, VoltCareApplication.CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Peringatan: ${rule.label}")
            .setContentText("Kondisi aturan cerdas terpenuhi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan Alarm", dismissPendingIntent)
            .build()
        manager.notify(ALERT_NOTIF_BASE_ID + rule.id.toInt(), notification)
    }

    private fun firedKey(ruleId: Long) = "fired_$ruleId"

    companion object {
        private const val PREFS_NAME = "voltcare_alarm_safetynet"
        private const val REQUEST_CODE = 9001

        /** Sengaja SAMA dgn ALERT_NOTIF_BASE_ID di BatteryMonitorService.kt (duplikasi kecil
         *  disengaja, lihat KDoc class) - ID notifikasi identik per rule.id, jadi kalau service
         *  & safety net ini kebetulan fire beruntun, notifikasi saling menimpa (bukan menumpuk). */
        private const val ALERT_NOTIF_BASE_ID = 2000

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
