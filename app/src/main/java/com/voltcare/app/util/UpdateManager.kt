package com.voltcare.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * In-app updater bawaan VoltCare — cek rilis terbaru di GitHub Releases lalu
 * download APK signed langsung dari dalam aplikasi (tanpa Play Store).
 *
 * Kepatuhan Release Downloader Spec (PROJECT_STATE):
 * - Streaming chunk-by-chunk (buffer 8KB) via Okio [okio.BufferedSink] langsung ke disk,
 *   TIDAK PERNAH memuat body APK utuh ke RAM (tidak ada readBytes()/ByteArray besar/
 *   response.body.string() untuk biner).
 * - OkHttpClient timeout eksplisit: connect 15s, read 20s.
 * - followRedirects(true) — browser_download_url GitHub Release redirect (302) ke CDN.
 * - Header Accept: application/octet-stream saat unduh biner; Authorization: Bearer <token>
 *   HANYA disertakan jika [GITHUB_TOKEN] diisi (repo publik VoltCare tidak butuh token untuk
 *   baca release/download asset — lihat catatan PROJECT_STATE Batch 19/20).
 * - Fail-safe: seluruh proses dibungkus try-catch, file parsial dihapus jika gagal/interrupted.
 */
object UpdateManager {

    private const val TAG = "UpdateManager"

    // Sesuai konvensi PROJECT_STATE: nama repo GitHub TETAP FDzaki-dev/PowerVaultHealthPro
    // (artifact/APK release-nya bernama VoltCare, tapi repo & path Termux tidak berubah).
    private const val GITHUB_OWNER = "FDzaki-dev"
    private const val GITHUB_REPO = "PowerVaultHealthPro"
    private const val API_LATEST_RELEASE =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private const val BUFFER_SIZE = 8L * 1024 // 8KB per chunk, sesuai spec

    /** Isi jika suatu saat repo di-private-kan / butuh rate-limit lebih tinggi. Default null. */
    private val GITHUB_TOKEN: String? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    data class UpdateInfo(
        val latestVersionName: String,
        val currentVersionName: String,
        val downloadUrl: String,
        val fileSizeBytes: Long,
        val releaseNotes: String
    )

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Failed(val message: String) : DownloadResult()
    }

    /**
     * Cek rilis terbaru di GitHub. Return null jika: sudah versi terbaru, gagal jaringan,
     * atau tidak ada asset .apk di rilis (fail-safe, tidak pernah throw ke caller).
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(API_LATEST_RELEASE)
                .header("Accept", "application/vnd.github+json")
            GITHUB_TOKEN?.let { requestBuilder.header("Authorization", "Bearer $it") }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Cek update gagal, HTTP ${response.code}")
                    return@withContext null
                }

                // Body JSON metadata rilis berukuran kecil (bukan biner APK) — aman dibaca penuh.
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "")
                val latestVersion = tagName.removePrefix("v").removePrefix("V")
                val releaseNotes = json.optString("body", "").take(2000)

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var apkSize = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", null)
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (apkUrl.isNullOrBlank() || latestVersion.isBlank()) {
                    Log.w(TAG, "Rilis terbaru tidak punya asset .apk atau tag_name kosong")
                    return@withContext null
                }

                val currentVersion = getCurrentVersionName(context)
                if (!isNewerVersion(latestVersion, currentVersion)) {
                    return@withContext null
                }

                UpdateInfo(
                    latestVersionName = latestVersion,
                    currentVersionName = currentVersion,
                    downloadUrl = apkUrl,
                    fileSizeBytes = apkSize,
                    releaseNotes = releaseNotes
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal cek update", e)
            null
        }
    }

    /**
     * Download APK streaming chunk-by-chunk (Okio sink) ke [Context.getExternalFilesDir]
     * (tidak butuh permission storage legacy). [onProgress] dipanggil dengan persen 0-100
     * di thread IO (caller wajib switch ke Main jika update UI langsung).
     */
    suspend fun downloadUpdate(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val destDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val destFile = File(destDir, "VoltCare_${info.latestVersionName}.apk")

        try {
            val requestBuilder = Request.Builder()
                .url(info.downloadUrl)
                .header("Accept", "application/octet-stream")
            GITHUB_TOKEN?.let { requestBuilder.header("Authorization", "Bearer $it") }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Failed("HTTP ${response.code}")
                }

                val responseBody = response.body
                    ?: return@withContext DownloadResult.Failed("Body kosong")

                val totalBytes = responseBody.contentLength().takeIf { it > 0 } ?: info.fileSizeBytes
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                val source = responseBody.source()
                destFile.sink().buffer().use { sink ->
                    while (true) {
                        // Baca max BUFFER_SIZE (8KB) per iterasi ke buffer internal sink,
                        // lalu emit() ke disk — TIDAK PERNAH menampung seluruh body di RAM.
                        val read = source.read(sink.buffer, BUFFER_SIZE)
                        if (read == -1L) break
                        sink.emit()
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    sink.flush()
                }

                DownloadResult.Success(destFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal download update", e)
            if (destFile.exists()) destFile.delete() // buang file parsial, jangan sisakan sampah
            DownloadResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Cek apakah izin "Install unknown apps" sudah diberikan untuk VoltCare (API 26+).
     * Di bawah API 26 tidak perlu izin eksplisit (dikontrol lewat dialog sistem saat install).
     */
    fun canRequestInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Intent untuk membuka halaman izin "Install unknown apps" khusus VoltCare. */
    fun installPermissionSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Trigger installer sistem lewat FileProvider (butuh entry <provider> di AndroidManifest). */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal buka installer APK", e)
        }
    }

    private fun getCurrentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }

    /** Bandingkan dua versi dot-separated secara numerik (mis. 1.2.10 > 1.2.9). */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.trim().toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.trim().toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}
