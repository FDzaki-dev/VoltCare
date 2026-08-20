package com.voltcare.app.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Whitelist app yang di-approve EKSPLISIT oleh user untuk di-hibernate otomatis (Pending #12,
 * FEATURE_PARITY_GOALS.md). SENGAJA whitelist, BUKAN semua app — mencegah kill app penting /
 * OOM-loop tanpa izin user (sesuai definisi item #12).
 */
object HibernateWhitelistStore {
    private const val PREFS = "voltcare_hibernate_prefs"
    private const val KEY_APPS = "whitelist_apps"
    private const val KEY_ENABLED = "scheduler_enabled"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAll(context: Context): Set<String> = prefs(context).getStringSet(KEY_APPS, emptySet()) ?: emptySet()

    fun isWhitelisted(context: Context, packageName: String): Boolean = getAll(context).contains(packageName)

    /** Toggle 1 app masuk/keluar whitelist. Copy set dulu (StringSet SharedPreferences tidak boleh dimutasi langsung). */
    fun toggle(context: Context, packageName: String) {
        val current = getAll(context).toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        prefs(context).edit().putStringSet(KEY_APPS, current).apply()
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}

/**
 * Worker terjadwal (interval 30 menit, minimum WorkManager 15 menit) yang menjalankan
 * `UsageStatsHelper.killBackgroundApp()` HANYA untuk app di [HibernateWhitelistStore] — bukan
 * semua app terpasang, supaya scheduler ini sendiri tidak justru bikin device lambat/boros.
 */
class HibernateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val whitelist = HibernateWhitelistStore.getAll(applicationContext)
            whitelist.forEach { pkg ->
                UsageStatsHelper.killBackgroundApp(applicationContext, pkg)
            }
            Result.success()
        } catch (e: Exception) {
            // Fail-safe: jangan retry agresif kalau ada error tak terduga (mis. app di
            // whitelist sudah di-uninstall) - cukup skip ke siklus 30 menit berikutnya.
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "voltcare_auto_hibernate"
        private const val INTERVAL_MINUTES = 30L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HibernateWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            HibernateWhitelistStore.setEnabled(context, true)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            HibernateWhitelistStore.setEnabled(context, false)
        }
    }
}
