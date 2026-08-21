package com.voltcare.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Gap tambahan dari klaim Batch 64/68: `stopWithTask=false` + `onTaskRemoved()` +
 * battery optimization exemption SEMUA API standar Android - tidak ada satu pun
 * yang menjangkau "Autostart Manager" OEM (MIUI, ColorOS, Funtouch OS, EMUI, dst).
 * OEM ini kill proses lewat mekanisme sendiri di luar lifecycle Service standar,
 * dan TIDAK ADA API publik utk toggle-nya otomatis dari kode - satu-satunya cara
 * legal adalah arahkan user ke halaman Settings OEM yang benar (best-effort,
 * berbasis daftar komponen yang diketahui publik, bisa berubah tiap versi ROM).
 */
object AutostartHelper {

    private val knownIntents: List<Intent> = listOf(
        // Xiaomi / Redmi / POCO (MIUI)
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        // Oppo / Realme (ColorOS)
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
        ),
        // Vivo (Funtouch OS)
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        // Huawei / Honor (EMUI)
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        // Samsung (One UI - "Sleeping apps" biasanya cukup lewat battery optimization,
        // tapi sebagian model masih punya menu App Power Management terpisah)
        Intent().setComponent(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
    )

    /**
     * Coba buka halaman Autostart/App Power Management OEM. Return true kalau
     * berhasil dibuka, false kalau tidak ada yang cocok (fallback ke App Details
     * settings standar di pemanggil).
     */
    fun openIfKnownOem(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val relevant = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
                manufacturer.contains("poco") -> knownIntents.filter {
                it.component?.packageName == "com.miui.securitycenter"
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> knownIntents.filter {
                it.component?.packageName?.contains("coloros") == true ||
                    it.component?.packageName?.contains("oppo") == true
            }
            manufacturer.contains("vivo") -> knownIntents.filter {
                it.component?.packageName == "com.vivo.permissionmanager"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> knownIntents.filter {
                it.component?.packageName == "com.huawei.systemmanager"
            }
            manufacturer.contains("samsung") -> knownIntents.filter {
                it.component?.packageName == "com.samsung.android.lool"
            }
            else -> emptyList()
        }
        for (intent in relevant) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Throwable) {
                // Komponen tidak ada di versi ROM ini - coba kandidat berikutnya.
            }
        }
        return false
    }

    /** Fallback universal: halaman detail app standar Android, selalu ada di semua device. */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            // Fail-safe total: kalau ini pun gagal, tidak ada lagi yang bisa dilakukan dari kode.
        }
    }
}
