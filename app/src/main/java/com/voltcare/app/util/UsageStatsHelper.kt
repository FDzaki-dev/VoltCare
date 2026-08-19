package com.voltcare.app.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

/** Satu baris hasil Drain Analyzer: 1 app + total waktu pemakaian foreground. */
data class AppUsageInfo(
    val packageName: String,
    val appLabel: String,
    val totalForegroundMs: Long,
    val isSystemApp: Boolean
)

/**
 * Drain Analyzer berbasis `UsageStatsManager` (API publik non-root).
 *
 * Catatan jujur soal keterbatasan: Android TIDAK mengekspos data drain-per-app (mAh terpakai
 * saat layar mati) ke aplikasi pihak ketiga tanpa root/system privilege - itu hanya tersedia
 * di Settings > Baterai (API tersembunyi/sistem). Yang tersedia publik adalah total waktu
 * pemakaian foreground per app (`queryUsageStats`). Aplikasi dengan waktu foreground tinggi
 * namun sering meninggalkan proses/service berjalan di background adalah kandidat paling
 * masuk akal untuk "penguras baterai" dari data yang bisa diakses secara legal - itulah dasar
 * pengurutan di bawah ini, BUKAN pengukuran mAh langsung per app.
 */
object UsageStatsHelper {

    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Top app berdasarkan total waktu foreground dalam `hours` jam terakhir, turun (descending). */
    fun topAppsByForegroundUsage(context: Context, hours: Int = 24, limit: Int = 15): List<AppUsageInfo> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val end = System.currentTimeMillis()
        val start = end - hours * 60 * 60 * 1000L

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end) ?: emptyList()
        val pm = context.packageManager
        val selfPackage = context.packageName

        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != selfPackage }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .mapNotNull { stat ->
                val appInfo = try {
                    pm.getApplicationInfo(stat.packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                } ?: return@mapNotNull null

                AppUsageInfo(
                    packageName = stat.packageName,
                    appLabel = pm.getApplicationLabel(appInfo).toString(),
                    totalForegroundMs = stat.totalTimeInForeground,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
    }

    /**
     * "Force stop" best-effort. `killBackgroundProcesses` (izin normal `KILL_BACKGROUND_PROCESSES`)
     * hanya mematikan proses cached/background milik app target - TIDAK sekuat "Force Stop" bawaan
     * Settings (yang butuh hak sistem, tidak tersedia untuk app pihak ketiga sejak Android 5+).
     * Return true jika perintah terkirim (bukan jaminan proses benar-benar berhenti).
     */
    fun killBackgroundApp(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
            am.killBackgroundProcesses(packageName)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}j ${m}m" else "${m}m"
    }
}
