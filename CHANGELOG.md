# CHANGELOG.md
(Urutan DESCENDING - entri terbaru di paling atas)

## [v1.0.0-batch6] - 2026-08-19
### Changed
- Asset APK di GitHub Release di-rename otomatis: `app-release.apk` -> `<NamaApp>_v<Versi>_<RunNumber>.apk` (mis. `VoltCare_v1.0.0_5.apk`). `<NamaApp>` diambil dari `rootProject.name`.

## [v1.0.0-batch5] - 2026-08-19
### Changed (Atomic Change - total rebrand)
- `applicationId`/`namespace`: `com.powervault.health.pro` -> `com.voltcare.app`.
- Semua file `.kt` dipindah ke package baru; kelas/fungsi `PowerVault*` -> `VoltCare*`, token `Pv*` -> `Vc*`.
- Nama DB Room `powervault_db` -> `voltcare_db` (aman, belum pernah ada install sukses).
- Folder crash log `Documents/PowerVaultHealthPro/` -> `Documents/VoltCare/`.
- `Theme.PowerVault` -> `Theme.VoltCare`, `rootProject.name` -> `VoltCare`, nama GitHub Release -> `VoltCare v...`.
- README & FILE_MANIFEST disesuaikan.
### Note
- Nama repo GitHub tidak diubah otomatis (lihat PROJECT_STATE.md Batch 5 untuk cara manual).

## [v1.0.0-batch4] - 2026-08-19
### Fixed
- **Critical**: APK release ter-publish unsigned (`app-release-unsigned.apk`) sehingga tidak bisa diinstal ("paket tampaknya tidak valid"). Penyebab: `file()` di `app/build.gradle.kts` resolve relatif ke `app/`, bukan root repo, jadi keystore tidak pernah ketemu saat cek signing.
- `app/build.gradle.kts`: pakai `rootProject.file()` untuk resolusi path keystore (2 lokasi).
- `.github/workflows/release.yml`: `ANDROID_KEYSTORE_PATH` jadi absolute path; tambah guard "Verify APK is signed" yang abort build kalau APK signed tidak ditemukan, supaya tidak pernah lagi publish APK unsigned.

## [v1.0.0-batch3] - 2026-08-19
### Fixed
- Build gagal (`compileReleaseKotlin`): tambah import `androidx.compose.foundation.layout.padding` yang hilang di `NavGraph.kt` (ditemukan dari analisa log Actions upload user).

## [v1.0.0-batch2] - 2026-08-19
### Changed
- Nama app: `VoltCare` -> `VoltCare` (strings.xml app_name + README).
- Tidak ada perubahan applicationId/package/arsitektur.

## [v1.0.0-batch1] - 2026-08-19
### Added
- Initial project setup: Kotlin + Compose + Room + Foreground Service architecture.
- 4-tab navigation: Dashboard, Riwayat, Penguras, Aturan.
- Dashboard fungsional (Health%, Suhu, Volt, Cas, Estimasi, Cycle, tombol Mulai Kalibrasi).
- BatteryMonitorService: sampling, logging, retensi 30 hari, evaluasi rule dasar.
- Crash logger bawaan (MediaStore, FIFO 50 log).
- GitHub Actions release.yml dengan Stale Run Guard + GitHub Release publish.
- Auto-generated release keystore (RSA 2048) + secrets export file.
### Deferred (lihat PROJECT_STATE.md > Pending Queue)
- Kalibrasi engine, Cycle Counter presisi, Drain Analyzer, Riwayat grafik + CSV,
  Stress Test, Rules UI editor.
