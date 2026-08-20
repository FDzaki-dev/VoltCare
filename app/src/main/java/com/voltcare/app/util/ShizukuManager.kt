package com.voltcare.app.util

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShizukuManager (Batch 23 - core integration)
 *
 * Wrapper tipis di atas Shizuku API untuk menjalankan perintah shell dengan privilege shell UID
 * (via ADB pairing wireless / root activator) TANPA VoltCare pernah minta akses root langsung.
 * Semua fungsi fail-safe (try-catch total, tidak pernah throw ke caller) - konsisten dengan pola
 * CrashLogger.kt & UpdateManager.kt di project ini.
 *
 * PRASYARAT (di luar kendali VoltCare):
 * - User install app Shizuku terpisah (github.com/RikkaApps/Shizuku atau Play Store).
 * - User aktifkan Shizuku sendiri (adb pairing wireless Android 11+, atau root activator).
 * - User approve dialog izin Shizuku untuk VoltCare (lewat requestPermission() di bawah).
 * Tanpa salah satu di atas terpenuhi, seluruh fungsi di sini mengembalikan hasil "tidak tersedia"
 * secara aman - TIDAK PERNAH crash & TIDAK mengubah perilaku fitur existing yang sudah jalan
 * dengan izin Android biasa (mis. UsageStatsHelper.killBackgroundApp tetap dipakai sbg fallback).
 *
 * Fitur konkret yang akan dibangun di atas wrapper ini (lihat Pending Queue PROJECT_STATE.md):
 * - Force Stop sungguhan (`am force-stop <pkg>`, jauh lebih kuat dari killBackgroundProcesses).
 * - Baca statistik drain per-app riil (`dumpsys batterystats`), bukan proxy waktu pemakaian.
 * - Auto-grant PACKAGE_USAGE_STATS via `appops set` (tanpa user buka Settings manual).
 * - Auto-hibernate app terjadwal (`am set-standby-bucket` / `pm suspend`).
 */
object ShizukuManager {

    const val PERMISSION_REQUEST_CODE = 9001

    /** State ringkas untuk ditampilkan di UI (Pending Queue - belum diwiring ke layar mana pun). */
    sealed class State {
        data object NotInstalled : State()
        data object NotRunning : State()
        data object PermissionDenied : State()
        data object Ready : State()
    }

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val isSuccess: Boolean get() = exitCode == 0
    }

    /** True jika binder Shizuku aktif (app Shizuku terpasang & service jalan). */
    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    /** True jika binder aktif DAN VoltCare sudah diberi izin oleh user lewat dialog Shizuku. */
    fun hasPermission(): Boolean = try {
        isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    fun currentState(): State = try {
        when {
            !isBinderAlive() -> State.NotRunning
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> State.PermissionDenied
            else -> State.Ready
        }
    } catch (e: Throwable) {
        State.NotInstalled
    }

    /**
     * Trigger dialog izin Shizuku (async - hasil ditangkap via addPermissionResultListener).
     * Aman dipanggil dari mana pun (tidak butuh Activity context), no-op fail-safe jika binder
     * belum aktif atau versi Shizuku terlalu lama (pre-v11, sudah deprecated upstream).
     */
    fun requestPermission() {
        try {
            if (isBinderAlive() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (e: Throwable) {
            // Fail-safe, sesuai konvensi project: tidak pernah throw ke caller.
        }
    }

    /** Daftarkan listener siklus hidup binder (Pending Queue: dipanggil dari VoltCareApplication). */
    fun addBinderListeners(onReceived: () -> Unit, onDead: () -> Unit) {
        try {
            Shizuku.addBinderReceivedListenerSticky { onReceived() }
            Shizuku.addBinderDeadListener { onDead() }
        } catch (e: Throwable) {
            // Fail-safe.
        }
    }

    /** Daftarkan listener hasil dialog izin (Pending Queue: dipanggil dari UI wiring batch berikutnya). */
    fun addPermissionResultListener(onResult: (granted: Boolean) -> Unit) {
        try {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == PERMISSION_REQUEST_CODE) {
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        } catch (e: Throwable) {
            // Fail-safe.
        }
    }

    /**
     * Eksekusi 1 perintah shell dengan privilege Shizuku (shell UID, atau root jika user
     * aktifkan Shizuku via root activator). Memakai Shizuku.newProcess() via reflection -
     * metode hidden-tapi-didukung resmi oleh Shizuku API 11.x-13.x untuk kompatibilitas
     * (dipakai luas oleh app pihak ketiga berbasis Shizuku, lihat catatan verifikasi di
     * PROJECT_STATE.md). BUKAN eksploitasi/root langsung - murni memakai binder yang sudah
     * diberi izin eksplisit oleh user lewat app Shizuku itu sendiri.
     *
     * Selalu cek hasResult.isSuccess sebelum dianggap berhasil - tidak pernah throw ke caller.
     */
    fun execShellCommand(command: Array<String>): ShellResult {
        if (!hasPermission()) {
            return ShellResult(-1, "", "Shizuku belum aktif atau izin belum diberikan")
        }
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val process = newProcessMethod.invoke(null, command, null, null) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Throwable) {
            ShellResult(-1, "", "Shizuku exec gagal: ${e.message}")
        }
    }

    /**
     * Batch 41 (Pending #20): auto-grant `PACKAGE_USAGE_STATS` untuk VoltCare via
     * `appops set <pkg> GET_USAGE_STATS allow` — menghilangkan langkah manual buka
     * Settings > Akses Penggunaan di Drain Analyzer SAAT Shizuku aktif & diizinkan.
     * Return true HANYA jika command shell benar-benar sukses (exit code 0) DAN
     * verifikasi ulang [UsageStatsHelper.hasUsageAccessPermission] mengonfirmasi izin
     * sudah aktif (defense in depth - `appops set` bisa "sukses" secara exit code tapi
     * tidak benar-benar berefek di device/ROM tertentu, jadi tidak dipercaya buta).
     */
    fun autoGrantUsageAccess(context: Context): Boolean {
        val result = execShellCommand(
            arrayOf("appops", "set", context.packageName, "GET_USAGE_STATS", "allow")
        )
        if (!result.isSuccess) return false
        return UsageStatsHelper.hasUsageAccessPermission(context)
    }
}
