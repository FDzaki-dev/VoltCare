# VoltCare

(dulu "PowerVault Health Pro". Sejak Batch 5, rename diterapkan total: applicationId/namespace
`com.voltcare.app`, semua nama kelas/paket, nama DB, folder crash log, dan seluruh dokumen.)

Aplikasi monitoring & kesehatan baterai Android, 100% lokal/offline.
Kotlin + Jetpack Compose + Room + Foreground Service.

Lihat `PROJECT_STATE.md` untuk status fitur & antrian pengerjaan (Pending Queue),
dan `.github/workflows/release.yml` untuk alur build+sign+release otomatis.

## Tab
Dashboard • Riwayat • Penguras • Aturan

## Build
Release APK dibangun & ditandatangani otomatis oleh GitHub Actions setiap push ke `main`,
lalu dipublikasikan sebagai GitHub Release (APK muncul di sidebar repo).
