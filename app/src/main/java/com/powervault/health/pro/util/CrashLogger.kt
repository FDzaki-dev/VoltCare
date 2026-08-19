package com.powervault.health.pro.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Crash logger bawaan PowerVault.
 * - Menyimpan exception ke Documents/PowerVaultHealthPro/logs/ via MediaStore (API 29+), tanpa
 *   permission legacy WRITE_EXTERNAL_STORAGE.
 * - Fail-safe: seluruh proses penulisan dibungkus try-catch agar logger sendiri tidak pernah
 *   menyebabkan crash tambahan.
 * - FIFO retention: menyimpan maksimal 50 file log, menghapus yang tertua saat melebihi batas.
 */
object CrashLogger {

    private const val APP_FOLDER = "PowerVaultHealthPro"
    private const val MAX_LOGS = 50
    private const val TAG = "CrashLogger"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(appContext, thread, throwable)
            } catch (loggingError: Throwable) {
                Log.e(TAG, "Gagal menulis crash log", loggingError)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val uuid = UUID.randomUUID().toString().take(8)
        val fileName = "crash_${timestamp}_$uuid.txt"

        val content = buildString {
            appendLine("=== PowerVault Health Pro Crash Report ===")
            appendLine("Timestamp   : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("App Version : ${getVersionName(context)}")
            appendLine("OS Version  : Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device      : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread      : ${thread.name}")
            appendLine()
            appendLine("--- Stack Trace ---")
            appendLine(Log.getStackTraceString(throwable))
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/$APP_FOLDER/logs")
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray())
            }
        }

        enforceFifoRetention(context)
    }

    /** Menjaga agar jumlah log tidak melebihi [MAX_LOGS], menghapus yang paling lama. */
    private fun enforceFifoRetention(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%Documents/$APP_FOLDER/logs%", "crash_%.txt")

        val entries = mutableListOf<Pair<Long, Long>>() // id to dateAdded
        resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} ASC")
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                while (cursor.moveToNext()) {
                    entries.add(cursor.getLong(idCol) to cursor.getLong(dateCol))
                }
            }

        if (entries.size > MAX_LOGS) {
            val toDelete = entries.size - MAX_LOGS
            entries.take(toDelete).forEach { (id, _) ->
                try {
                    val uri = MediaStore.Files.getContentUri("external", id)
                    resolver.delete(uri, null, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal hapus log lama id=$id", e)
                }
            }
        }
    }

    private fun getVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}
