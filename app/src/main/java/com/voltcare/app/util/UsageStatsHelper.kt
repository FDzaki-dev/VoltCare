package com.voltcare.app.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
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

    /**
     * Pending #13 (`FEATURE_PARITY_GOALS.md`, gap Greenify "cegah app berjalan sendiri tanpa
     * izin"): Android TIDAK punya API generik non-root untuk 3rd-party app mengontrol App
     * Standby Bucket/auto-launch app LAIN (`UsageStatsManager.setAppStandbyBucket` dibatasi
     * hanya utk app sistem sejak API 30) — best-effort satu-satunya yang realistis adalah
     * arahkan USER SENDIRI ke dialog "App Info" bawaan Android utk app target, tempat user
     * bisa atur battery restriction/manage-background manual. Ini BUKAN otomatis dari
     * VoltCare — murni shortcut navigasi, sama pola honesty seperti catatan `killBackgroundApp`
     * di atas & `topAppsByForegroundUsage` (proxy, bukan data sebenarnya).
     */
    fun openAppDetailsSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
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
     * "Force stop" — Batch 39 (Pending #18): SEKARANG prioritas pakai Shizuku (`am force-stop
     * <pkg>`, hak sistem, PERSIS sama seperti "Force Stop" bawaan Settings) jika
     * [ShizukuManager.hasPermission] true. Kalau Shizuku tidak aktif/belum diizinkan ATAU
     * perintah shell gagal, otomatis fallback ke `killBackgroundProcesses` lama (izin normal,
     * lebih lemah — hanya proses cached/background). Fitur existing TIDAK berubah perilaku
     * untuk user yang belum pakai Shizuku sama sekali (graceful fallback, sesuai desain
     * ShizukuManager Batch 23). Return true jika salah satu jalur terkirim (bukan jaminan
     * proses benar-benar berhenti untuk jalur fallback).
     */
    fun killBackgroundApp(context: Context, packageName: String): Boolean {
        if (ShizukuManager.hasPermission()) {
            val result = ShizukuManager.execShellCommand(arrayOf("am", "force-stop", packageName))
            if (result.isSuccess) return true
            // Perintah shell gagal walau permission ada (mis. paket invalid) - tetap coba
            // fallback di bawah alih-alih langsung return false, supaya UX tetap konsisten
            // dengan perilaku sebelum Batch 39.
        }
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
