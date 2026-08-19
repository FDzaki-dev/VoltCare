# CHANGELOG.md
(Urutan DESCENDING - entri terbaru di paling atas)

## [v1.0.0-batch12] - 2026-08-19
### Added
- **Tes Baterai / Stress Test** (Pending Queue #5): `StressTestScreen.kt` (baru, self-contained) — sesi tetap 10 menit, poll `BatteryUtils.readSnapshot()` tiap 1 detik, hasil drop% & laju drain %/menit. Wake lock `PARTIAL` terkontrol (timeout eksplisit 11 menit, selalu dilepas via `DisposableEffect`).
### Changed
- `AndroidManifest.xml`: tambah `WAKE_LOCK` (izin normal, untuk wake lock terkontrol di atas).
- `NavGraph.kt`: tambah route `stress_test` + FAB entry point di tab Dashboard (tanpa tab ke-5, tanpa edit `DashboardScreen.kt`).

## [v1.0.0-batch11] - 2026-08-19
### Added
- **Riwayat 30 Hari** (Pending Queue #4): `CsvExporter.kt` (export battery_log ke CSV via MediaStore), `HistoryViewModel.kt` (agregasi Health/Suhu/Cycle 30 hari). `HistoryScreen.kt` ditulis ulang: kartu ringkasan + 2 grafik garis (Compose Canvas native, tanpa dependency baru) + tombol Export CSV.

## [v1.0.0-batch10] - 2026-08-19
### Added
- **Drain Analyzer** (Pending Queue #3): `UsageStatsHelper.kt` (baca top app via `UsageStatsManager`, cek/buka izin Usage Access, force-stop best-effort via `killBackgroundProcesses`). `DrainScreen.kt` ditulis ulang dari scaffold jadi fungsional (permission-gate + daftar app + tombol Force Stop).
### Changed
- `AndroidManifest.xml`: tambah `KILL_BACKGROUND_PROCESSES` (izin normal, diperlukan fitur Force Stop).
### Note
- Data drain per-app (mAh saat layar mati) tidak tersedia di API publik non-root; fitur ini pakai proxy waktu pemakaian foreground 24 jam, didokumentasikan transparan di UI & kode.

## [v1.0.0-batch9] - 2026-08-19
### Added
- **Cycle Counter Presisi** (Pending Queue #2): `BatteryUtils.CycleTracker` — akumulasi mAh lintas sesi charging (standar industri), 1 cycle = setara 1x kapasitas desain. Insert ke `cycle_history` (`isFullCalibrationCycle=false`), berjalan independen dari kalibrasi (Batch 8).
### Removed
- Heuristik `trackCycle()` lama di `BatteryMonitorService` (akumulasi persen, tidak pernah menulis ke DB) — dead code, dihapus & digantikan `processCycleTracking()`.

## [v1.0.0-batch8] - 2026-08-19
### Added
- **Kalibrasi Engine** (Pending Queue #1): state machine 3x siklus charge 0-100% berturut-turut dengan validasi anti-drop (`BatteryUtils.CalibrationStore`, persisted SharedPreferences). Health% otomatis dihitung dari mAh terkirim setelah 3 siklus sukses, menggantikan heuristik tetap 87%.
- Notifikasi "Kalibrasi selesai" saat streak ke-3 tercapai.
### Changed
- `BatteryMonitorService`: sampling loop kini juga proses kalibrasi & insert `CycleEntity` untuk siklus penuh.
- `DashboardViewModel.startCalibration()`: aktivasi nyata (bukan flag UI kosong), status disinkronkan tiap sample.

## [v1.0.0-batch7] - 2026-08-19
### Docs
- Patenkan konvensi penamaan artifact di `PROJECT_STATE.md` (blok pinned di paling atas): nama artifact ZIP/APK = `VoltCare`, nama repo/folder tetap `PowerVaultHealthPro`. Tidak ada perubahan kode.

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
