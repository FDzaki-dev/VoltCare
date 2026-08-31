package com.voltcare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.voltcare.app.util.CrashLogger

/**
 * Batch 91 (Pending Queue #42 - permintaan user, lanjutan Batch 90 "konfigurasi umum
 * alarm/charger-trigger app"): `CHANNEL_ALERT` lama (dipakai BERSAMA rule ALARM & NOTIFY)
 * dipecah jadi 2 channel supaya DND bypass bisa diterapkan KHUSUS ke rule ALARM tanpa
 * menyeret rule NOTIFY ikut-ikutan bypass DND (rule NOTIFY memang bukan "alarm sungguhan").
 * SENGAJA channel ID baru (bukan reuse `battery_alert`) - properti channel Android
 * (importance/sound/bypassDnd) TERKUNCI sejak pembuatan pertama & tidak bisa diubah lewat
 * update APK biasa; user existing yang sudah pernah buka app versi lama tidak akan dapat
 * efek apa pun kalau cuma mengubah channel LAMA di kode. Channel lama dihapus eksplisit
 * (`deleteNotificationChannel`, aman/no-op kalau memang sudah tidak ada) - best practice,
 * bukan wajib, tapi menghindari channel "hantu" nongkrong di Settings app.
 *
 * Bonus efek samping (sengaja, bukan scope creep): channel ALARM sekarang `setSound(null,
 * null)` - sebelumnya (channel gabungan) tidak bisa disuppress krn rule NOTIFY butuh suara
 * default channel itu (tidak punya AlarmPlayer sendiri). Sekarang keduanya independen, jadi
 * channel ALARM aman disuppress (AlarmPlayer sudah handle suara sendiri, custom URI + loop +
 * USAGE_ALARM - default channel sound cuma bikin dobel bunyi "ding" di depan nada alarm),
 * sedangkan channel NOTIFY tetap pakai default sound seperti perilaku lama (satu-satunya
 * sumber suara utk rule NOTIFY, tidak boleh disuppress).
 *
 * PENTING - belum lengkap sendirian: `setBypassDnd(true)` di channel ALARM TIDAK berefek
 * apa pun sampai user grant izin "Do Not Disturb access" manual (`Settings.
 * ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`) - prompt otomatis utk izin ini BELUM
 * ditambahkan di batch ini (scope terpisah, lihat PROJECT_STATE.md Batch 91 Pending Queue).
 * Sementara itu user bisa grant manual: Settings > Apps > VoltCare > Notifications, atau
 * cari "Do Not Disturb access" > VoltCare > Allow.
 */
class VoltCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            getString(R.string.notif_channel_monitor),
            NotificationManager.IMPORTANCE_LOW
        )

        val alertAlarmChannel = NotificationChannel(
            CHANNEL_ALERT_ALARM,
            getString(R.string.notif_channel_alert_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setBypassDnd(true) // butuh izin manual user, lihat KDoc class
            setSound(null, null) // AlarmPlayer handle suara sendiri, hindari dobel bunyi
        }

        val alertNotifyChannel = NotificationChannel(
            CHANNEL_ALERT_NOTIFY,
            getString(R.string.notif_channel_alert),
            NotificationManager.IMPORTANCE_HIGH
        ) // default sound TETAP aktif - satu-satunya sumber suara utk rule NOTIFY

        manager.createNotificationChannel(monitorChannel)
        manager.createNotificationChannel(alertAlarmChannel)
        manager.createNotificationChannel(alertNotifyChannel)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ALERT) // channel gabungan lama, digantikan 2 di atas
    }

    companion object {
        const val CHANNEL_MONITOR = "battery_monitor"

        /** Batch 91: channel ID BARU (bukan reuse) - lihat KDoc class utk alasan wajib baru. */
        const val CHANNEL_ALERT_ALARM = "battery_alert_alarm"
        const val CHANNEL_ALERT_NOTIFY = "battery_alert_notify"

        /** ID channel gabungan lama (Batch 1-90) - HANYA dipakai utk deleteNotificationChannel()
         *  di atas, jangan dipakai post notifikasi baru lagi. */
        private const val LEGACY_CHANNEL_ALERT = "battery_alert"
    }
}
