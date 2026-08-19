# CHANGELOG.md
(Urutan DESCENDING - entri terbaru di paling atas)

## [v1.0.0-batch28] - 2026-08-19
### Fixed
- Remote `origin` nyasar ke URL repo lama (`PowerVaultHealthPro`) yang sudah tidak ada / sudah di-rename user jadi `FDzaki-dev/VoltCare` → `remote: Repository not found` saat push. Perbaikan: `git remote set-url origin` ke URL baru (fix Termux, bukan perubahan kode). Update konvensi nama repo di `PROJECT_STATE.md`, Pending Queue #8 ditandai selesai.

## [v1.0.0-batch27] - 2026-08-19
### Fixed
- Edge-to-edge insets tidak konsisten lintas versi OS (< Android 14): `MainActivity.kt` +`enableEdgeToEdge()` sebelum `super.onCreate()`. Adaptasi dari `dokumentasi_insets_targetsdk34.md` (dokumen berbasis View/XML) ke Compose — sengaja TIDAK pasang `ViewCompat.setOnApplyWindowInsetsListener` manual (bisa bentrok listener internal `AndroidComposeView`), konsumsi insets diserahkan ke `Scaffold`+`NavigationBar` Material3 yang sudah edge-to-edge aware.

## [v1.0.0-batch26] - 2026-08-19
### Added
- **Shizuku UI Wiring** (lanjutan Batch 23): `ShizukuStatusAction.kt` (baru) - ikon status Shizuku (NotInstalled/NotRunning/PermissionDenied/Ready) + dialog + tombol "Minta Izin" di overlay Dashboard (`TopStart`). `strings.xml`: +11 string `shizuku_*`. `NavGraph.kt`: pasang `ShizukuStatusAction()`.
### Queued
- Force Stop nyata via Shizuku (Drain Analyzer), statistik drain per-app riil (`dumpsys batterystats`), auto-grant Usage Access — Pending Queue #18-20 di `PROJECT_STATE.md`.

## [v1.0.0-batch25] - 2026-08-19
### Removed
- `.github/workflows/build.yml` ("Build PromptVault APK", workflow sisa app lain, non-protected).
- 65 file source/test orphan `com/elprompter/promptvault/*` di module `:app` (0 referensi ke/dari VoltCare, penyebab `compileDebugKotlin` gagal & berisiko ikut gagalkan `release.yml`).

## [v1.0.0-batch24] - 2026-08-19
### Fixed
- Update checker (`UpdateManager.kt`) salah baca `tag_name` GitHub Release (`v{version}-{run_number}`) sehingga segmen versi terakhir gagal parse & dibuang → app selalu bilang "Sudah Versi Terbaru" meski rilis baru sudah live/compile hijau. Fix: strip suffix run_number sebelum compare versi.

## [v1.0.0-batch23] - 2026-08-19
### Added
- **Shizuku Core Integration (engine)**: `ShizukuManager.kt` (baru) - wrapper fail-safe (`hasPermission`, `requestPermission`, `execShellCommand` via `Shizuku.newProcess()` reflection, dll). `app/build.gradle.kts`: +2 dependency `dev.rikka.shizuku:api`/`:provider` 13.1.5. `AndroidManifest.xml`: +`<provider>` `ShizukuProvider`. Tanpa Shizuku aktif, semua fitur existing tetap 100% jalan seperti biasa (graceful fallback), belum diwiring ke UI/fitur manapun.
### Queued
- UI trigger izin, rewire Force Stop (Drain Analyzer) ke `am force-stop`, statistik drain per-app riil via `dumpsys batterystats`, auto-grant Usage Access — lihat Pending Queue #17-20 di `PROJECT_STATE.md`.

## [v1.0.0-batch22] - 2026-08-19
### Fixed
- **Build gagal** (regresi Batch 20, ditemukan via `log_fail` asli yang diupload user): `UpdateManager.kt:49` — Kotlin melarang `const val` bertipe nullable (`String?`). `private const val GITHUB_TOKEN: String? = null` -> `private val GITHUB_TOKEN: String? = null`. Fungsional identik, hanya bukan compile-time constant lagi.
### Changed
- `FILE_MANIFEST.txt` (Pending Queue #14): tambah 4 entri file baru Batch 18-21 (`FEATURE_PARITY_GOALS.md`, `file_paths.xml`, `UpdateManager.kt`, `UpdateScreen.kt`).

## [v1.0.0-batch21] - 2026-08-19
### Added
- **In-App Updater - UI Wiring (Pending Queue #15, selesai)**: `UpdateScreen.kt` (baru, self-contained) — `UpdateViewModel` + `UpdateCheckAction()` composable (tombol ikon + dialog cek/download progress/instal), dipasang di `NavGraph.kt` (TopEnd overlay Dashboard, tanpa edit `DashboardScreen.kt`). `strings.xml`: +13 string label dialog. Fitur "update langsung dari aplikasi" kini lengkap end-to-end (cek rilis GitHub -> download progress -> instal via FileProvider).

## [v1.0.0-batch20] - 2026-08-19
### Changed
- **In-App Updater: swap ke OkHttp/Okio literal** (permintaan user, benefit: API streaming lebih ringkas & robust): `UpdateManager.kt` ditulis ulang pakai `OkHttpClient` + `Okio BufferedSink` (`source.read(sink.buffer, 8192)` + `sink.emit()` per chunk) menggantikan `HttpURLConnection` manual. `app/build.gradle.kts`: +1 dependency `com.squareup.okhttp3:okhttp:4.12.0`. Signature publik `UpdateManager` tidak berubah — aman utk UI wiring Batch 21.

## [v1.0.0-batch19] - 2026-08-19
### Added
- **In-App Updater - Core Engine** (fitur "update langsung dari aplikasi", diminta user): `UpdateManager.kt` (baru) — cek rilis terbaru GitHub (`FDzaki-dev/PowerVaultHealthPro`), download APK streaming chunk-by-chunk ke disk (anti-freeze/anti-OOM, tanpa `readBytes()`), timeout connect 15s/read 20s, `followRedirects(true)`, trigger install via `FileProvider`. `file_paths.xml` (baru). `AndroidManifest.xml`: tambah `INTERNET`+`REQUEST_INSTALL_PACKAGES` permission + `<provider>` FileProvider.
### Queued
- UI trigger (tombol/progress/install flow) belum dikerjakan — engine masih standalone, akan diwiring di Batch 20 (lihat Pending Queue #15 di `PROJECT_STATE.md`).

## [v1.0.0-batch18] - 2026-08-19
### Added
- **`FEATURE_PARITY_GOALS.md`** (dokumentasi baru): matrix gap-analysis vs AccuBattery/GSam Battery Monitor/Greenify (sumber: 2 screenshot Google AI Overview diupload user). Hasil audit source code: 3 fitur Done, 3 Partial, 2 belum ada. 4 item Pending Queue baru (#10-#13) ditambahkan ke `PROJECT_STATE.md`.

## [v1.0.0-batch17] - 2026-08-19
### Fixed
- **Warning KSP Room hilang** (Pending Queue #7): `app/build.gradle.kts` tambah `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. `AppDatabase.kt` (`exportSchema=true`, tidak diubah) sekarang benar-benar menulis JSON skema ke `app/schemas/` tiap build — dasar formal untuk `Migration` saat `version` naik.

## [v1.0.0-batch16] - 2026-08-19
### Added
- **Artifact `log_fail_<version>_<run-number>` otomatis** (Pending Queue #9, diminta user di Batch 15): `release.yml` — step `Build signed release APK` kini `continue-on-error` + rekam log ke `gradle-build.log` (`tee`), kalau gagal langsung upload sbg GitHub Actions artifact `log_fail_<version>_<run-number>` (retensi 14 hari) lalu abort job sebelum tahap Release. Ekstraksi versi dipindah ke step tersendiri lebih awal (`Extract version name`) supaya tetap tersedia walau build gagal.

## [v1.0.0-batch15] - 2026-08-19
### Fixed
- **Build gagal** (regresi Batch 14): `RulesScreen.kt` pakai `ExposedDropdownMenu` yang belum ada di material3 1.2.1 (baru di 1.3.0+) + 3 API experimental tanpa opt-in. Diganti `DropdownMenu` biasa + `@OptIn(ExperimentalMaterial3Api::class)`. Sumber: log GitHub Actions job `build-release` yang diupload user.
### Queued
- Fitur baru diminta user: artifact `log_fail_<version>_<run-number>` otomatis di `release.yml` saat compile gagal — masuk Pending Queue #9, dikerjakan batch terpisah.

## [v1.0.0-batch14] - 2026-08-19
### Added
- **Aturan Cerdas UI Editor** (Pending Queue #6, item terakhir dari roadmap Batch 1): `RulesViewModel.kt` (baru, CRUD ke `RuleEntity`/`RuleDao` apa adanya). `RulesScreen.kt` ditulis ulang: daftar aturan + toggle aktif + form tambah/edit (`AlertDialog`: nama, dropdown kondisi, nilai ambang, switch charging, dropdown aksi) + hapus dengan konfirmasi.

## [v1.0.0-batch13] - 2026-08-19
### Fixed
- **Symbol Unicode berisiko mojibake/tofu**: `DashboardScreen.kt`, `BatteryMonitorService.kt` (notifikasi persisten), `StressTestScreen.kt` — ellipsis/bullet/panah/em-dash/emoji-peringatan (`\u2026`,`\u2022`,`\u2190`,`\u2192`,`\u2014`,`\u26A0\uFE0F`) diganti ASCII polos (`...`,`-`,`<`,`->`,`-`,`[!]`). `\u00B0C` (derajat) dipertahankan (aman, Latin-1). Diverifikasi: 0 raw non-ASCII byte di seluruh source.

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
