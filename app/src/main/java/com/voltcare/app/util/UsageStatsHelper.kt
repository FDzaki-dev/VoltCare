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

/**
 * Satu baris hasil Drain Analyzer: 1 app + total waktu pemakaian foreground.
 * [mahEstimate] null secara default (proxy waktu pemakaian saja, jalur lama sejak Batch 1) -
 * terisi HANYA jika Shizuku aktif & `dumpsys batterystats` berhasil di-parse (Pending #19 2/2,
 * lihat [fetchDrainMahByPackage]/[mergeDrainData] di bawah). Nullable, BUKAN default 0.0, supaya
 * UI bisa membedakan "belum ada data riil" vs "data riil = 0 mAh" secara eksplisit.
 */
data class AppUsageInfo(
    val packageName: String,
    val appLabel: String,
    val totalForegroundMs: Long,
    val isSystemApp: Boolean,
    val mahEstimate: Double? = null
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

    /**
     * Pending #19 (2/2, Batch 52): wiring [BatteryStatsParser] (logic parsing, Batch 49-51,
     * TERVALIDASI PENUH terhadap data nyata) + [ShizukuManager.execShellCommand] jadi data
     * mAh riil per app. Return null (BUKAN map kosong) kalau Shizuku belum aktif/diizinkan,
     * command shell gagal, atau parsing tidak menemukan section sama sekali - caller WAJIB
     * treat null sebagai "data riil tidak tersedia, tetap pakai proxy waktu pemakaian" (fallback
     * graceful, TIDAK pernah membuat Drain Analyzer kosong/crash hanya karena Shizuku absen).
     *
     * Catatan jujur soal jendela waktu: `dumpsys batterystats --charged` mengukur SEJAK CHARGE
     * PENUH TERAKHIR (bukan window 24 jam spt [topAppsByForegroundUsage]) - dua sumber data ini
     * TIDAK diklaim mengukur periode yang identik, murni dikombinasikan di [mergeDrainData] utk
     * memberi estimasi mAh riil pada app yang sudah tersaring dari daftar 24 jam.
     *
     * 1 UID Android bisa dipakai bareng oleh beberapa package (shared UID, jarang tapi valid
     * di AOSP) - `getPackagesForUid` mengembalikan semua package tsb & masing-masing diberi
     * nilai mAh yang sama (nilai mAh memang milik UID, bukan per-package individual).
     */
    fun fetchDrainMahByPackage(context: Context): Map<String, Double>? {
        if (!ShizukuManager.hasPermission()) return null
        val result = ShizukuManager.execShellCommand(arrayOf("dumpsys", "batterystats", "--charged"))
        if (!result.isSuccess) return null

        val parsed = BatteryStatsParser.parseEstimatedPowerUse(result.stdout)
        if (parsed.isEmpty()) return null

        val pm = context.packageManager
        val mahByPackage = mutableMapOf<String, Double>()
        for (entry in parsed) {
            val packages = try {
                pm.getPackagesForUid(entry.uid)
            } catch (e: Exception) {
                null
            } ?: continue

            for (pkg in packages) {
                // Defensif: kalau (jarang) ada >1 entri UidPowerUsage utk uid yang sama,
                // simpan nilai tertinggi - bukan ditimpa/dijumlah begitu saja.
                val existing = mahByPackage[pkg]
                if (existing == null || entry.mah > existing) mahByPackage[pkg] = entry.mah
            }
        }
        return mahByPackage.ifEmpty { null }
    }

    /**
     * Gabungkan hasil [fetchDrainMahByPackage] ke daftar [apps] existing (dari
     * [topAppsByForegroundUsage]) - TIDAK mengganti daftar app, hanya mengisi [AppUsageInfo.mahEstimate]
     * kalau ada match `packageName`, lalu urutkan ulang: app dengan data mAh riil di atas
     * (descending mAh), app tanpa match tetap di bawah dgn urutan waktu pemakaian semula
     * (BUKAN dihapus dari daftar - tetap actionable via Force Stop/Pengaturan App spt biasa).
     * [mahByPackage] null/kosong -> return [apps] apa adanya (no-op, jalur lama utuh).
     */
    fun mergeDrainData(apps: List<AppUsageInfo>, mahByPackage: Map<String, Double>?): List<AppUsageInfo> {
        if (mahByPackage.isNullOrEmpty()) return apps
        return apps
            .map { app -> mahByPackage[app.packageName]?.let { mah -> app.copy(mahEstimate = mah) } ?: app }
            .sortedWith(
                compareByDescending<AppUsageInfo> { it.mahEstimate ?: -1.0 }
                    .thenByDescending { it.totalForegroundMs }
            )
    }

    /**
     * Varian "Tampilkan Semua App" (Batch 54, permintaan eksplisit user setelah screenshot
     * Batch 53 hanya menampilkan 3 app) - BEDA dari [topAppsByForegroundUsage] +
     * [mergeDrainData] (yang membatasi ke 15 app dgn foreground time TERTINGGI dulu, baru
     * diisi mAh kalau match). Fungsi ini membangun daftar LANGSUNG dari [mahByPackage] - jadi
     * app yang tercatat `dumpsys batterystats` TAPI foreground time-nya rendah/0 dalam 24 jam
     * terakhir (app jarang dibuka user tapi tetap aktif di background/wake lock, JUSTRU
     * kandidat penguras paling relevan) ikut tampil, bukan cuma yang sering dibuka manual.
     *
     * HANYA berguna kalau [mahByPackage] tidak kosong - caller (DrainScreen) menggating lewat
     * `hasRealDrainData`, TIDAK dipanggil sama sekali kalau Shizuku tidak aktif.
     */
    fun fullDrainAppList(
        context: Context,
        mahByPackage: Map<String, Double>,
        hours: Int = 24,
        limit: Int = 50
    ): List<AppUsageInfo> {
        if (mahByPackage.isEmpty()) return emptyList()
        val foregroundMap = rawForegroundMsByPackage(context, hours)
        val pm = context.packageManager
        val selfPackage = context.packageName

        return mahByPackage.keys
            .filter { it != selfPackage }
            .mapNotNull { pkg ->
                val appInfo = try {
                    pm.getApplicationInfo(pkg, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                } ?: return@mapNotNull null

                AppUsageInfo(
                    packageName = pkg,
                    appLabel = pm.getApplicationLabel(appInfo).toString(),
                    totalForegroundMs = foregroundMap[pkg] ?: 0L,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    mahEstimate = mahByPackage[pkg]
                )
            }
            .sortedWith(
                compareByDescending<AppUsageInfo> { it.mahEstimate ?: -1.0 }
                    .thenByDescending { it.totalForegroundMs }
            )
            .take(limit)
    }

    /**
     * Map mentah packageName -> total foreground ms, TANPA batas [limit] 15 spt
     * [topAppsByForegroundUsage] (fungsi itu TIDAK diubah/disentuh - dipertahankan apa adanya
     * utk jalur proxy lama). Dibuat terpisah (bukan refactor fungsi existing) supaya blast
     * radius perubahan Batch 54 tidak menyentuh perilaku [topAppsByForegroundUsage] yang sudah
     * jalan sejak Batch 1. Return map kosong (bukan exception) kalau izin Usage Access belum
     * ada - [fullDrainAppList] tetap jalan, foreground time-nya cuma tampil 0m per app.
     */
    private fun rawForegroundMsByPackage(context: Context, hours: Int): Map<String, Long> {
        if (!hasUsageAccessPermission(context)) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - hours * 60 * 60 * 1000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end) ?: emptyList()

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
    }

    fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}j ${m}m" else "${m}m"
    }
}
