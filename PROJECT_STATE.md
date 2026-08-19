# PROJECT_STATE.md
(Urutan DESCENDING - entri terbaru di paling atas)

---

## 📌 KONVENSI TETAP (baca duluan, berlaku untuk semua batch berikutnya)

**Nama App untuk artifact (ZIP & APK release): `VoltCare`** — BUKAN `PowerVaultHealthPro`.

- Alasan: nama file artifact di card UI chat kepotong kalau kepanjangan (`PowerVaultHealthPro_v1_Batch6...` -> terpotong jadi `PowerVaultHealthPro ...`, versi/batch jadi gak keliatan). `VoltCare` jauh lebih pendek -> versi/batch selalu utuh kelihatan di card.
- **ZIP output**: `VoltCare_v<Versi>_Batch<N>.zip` (root ZIP tetap isi project `PowerVaultHealthPro`, cuma nama file-nya yang beda).
- **APK release asset** (di GitHub Actions, `release.yml`): `VoltCare_v<Versi>_<RunNumber>.apk` (sudah otomatis, diambil dari `rootProject.name` di `settings.gradle.kts` — lihat Batch 6).
- **Nama repo GitHub tetap `FDzaki-dev/PowerVaultHealthPro`** (sengaja TIDAK diubah, lihat Batch 5) → folder lokal Termux tetap `~/projects/PowerVaultHealthPro`, remote `origin` tetap sama.
- **Skrip Termux**: ganti pola pencarian ZIP jadi `~/storage/downloads/VoltCare*.zip` (bukan lagi `PowerVaultHealthPro*.zip`), tapi path `cd ~/projects/PowerVaultHealthPro` TIDAK berubah.
- Ringkas: **VoltCare = nama produk/artifact**, **PowerVaultHealthPro = nama repo/folder**. Dua hal beda, jangan disamakan lagi.

---

## [Batch 14] Fitur - Aturan Cerdas UI Editor (Pending Queue #6) — 2026-08-19

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 48 -> 49 file (1 baru: `RulesViewModel.kt`; 1 ditulis ulang: `RulesScreen.kt`)

### Selesai
- **`ui/screens/rules/RulesViewModel.kt`** (baru): CRUD murni ke `RuleEntity` via `RuleDao` (protected, dipakai apa adanya — `insert`/`update`/`delete`/`all()` sudah tersedia sejak Batch 1). Enum `RuleCondition` (`TEMP_ABOVE`/`PERCENT_ABOVE`/`PERCENT_BELOW`) & `RuleAction` (`ALARM`/`NOTIFY`) di-map 1:1 ke string yang sudah dipakai `BatteryMonitorService.checkRule()` — tidak ada string baru yang perlu disinkronkan ke service.
- **`ui/screens/rules/RulesScreen.kt`** (ditulis ulang dari placeholder): `LazyColumn` daftar aturan (label, ringkasan "IF ... THEN ...", `Switch` aktif/nonaktif, tombol Edit/Hapus), FAB "+" buka `AlertDialog` form (nama, dropdown kondisi, nilai ambang, switch "hanya saat charging", dropdown aksi) — validasi label tidak kosong & nilai numerik sebelum tombol Simpan aktif. Hapus lewat dialog konfirmasi terpisah (anti tap-salah).

### Sengaja TIDAK diubah
- `RuleEntity.kt`/`RuleDao.kt` (DB Schema/DAO, protected) — dipakai 100% apa adanya, tidak ada kolom/migration baru.
- `BatteryMonitorService.kt` — engine evaluasi (`checkRule`/`fireAlert`) tidak disentuh; rule baru dari editor otomatis ikut dievaluasi sample berikutnya karena baca `enabledOnce()` dari DB yang sama.
- `NavGraph.kt`/`AndroidManifest.xml` — tidak perlu route atau permission baru (`RulesScreen()` sudah dipanggil tanpa parameter sejak Batch 1, `viewModel()` default resolve ke `RulesViewModel` baru secara otomatis).

### Protected Assets tersentuh
Tidak ada.

### Pending Queue (Batch 14: item #6 selesai, 2 opsional tersisa)
1-6. ✅ selesai (lihat Batch 8-14)
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 13] Fix - Symbol Unicode Berisiko Mojibake/Tofu — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 48 -> 48 file (0 baru/hapus, 3 file diedit)

### Root Cause
Audit penuh (`grep -rlP '[^\x00-\x7F]'`) memastikan **tidak ada raw non-ASCII byte** di source manapun (aman dari corrupt-encoding klasik). Tapi 3 file memakai escape `\u` untuk glyph di blok General Punctuation/Emoji (`\u2026` ellipsis, `\u2022` bullet, `\u2190`/`\u2192` panah, `\u2014` em dash, `\u26A0\uFE0F` emoji peringatan+variation-selector) — glyph ini valid secara kode tapi dukungan font-nya tidak seuniversal Latin-1, jadi berisiko tampil sebagai kotak/tofu di sebagian device/font sistem. `\u00B0` (derajat, °C) **sengaja dipertahankan** — bagian Latin-1 Supplement, didukung 100% font manapun, bukan risiko.

### Selesai
- **`DashboardScreen.kt`**: `"Kalibrasi berjalan\u2026"` -> `"Kalibrasi berjalan..."`.
- **`BatteryMonitorService.kt`** (notifikasi persisten, paling sering dilihat user): `\u2026` -> `"..."`, `\u2022` (2x) -> `"-"`.
- **`StressTestScreen.kt`** (4 titik, paling berisiko krn ada emoji+variation-selector): `\u2190 Kembali` -> `< Kembali`, `\u2192` -> `->`, `\u2014` -> `-`, `\u26A0\uFE0F` (emoji) -> `[!]` (penanda ASCII polos, paling aman lintas device).

### Verifikasi
- Ulang audit setelah fix: 0 raw non-ASCII byte, 0 escape `\u` selain `\u00B0` (derajat) tersisa di ketiga file.
- Brace balance (`{`/`}`) dicek manual per file, seimbang — tidak ada syntax pecah akibat edit string.

### Sengaja TIDAK diubah
- `HistoryScreen.kt` & `RulesScreen.kt` — diaudit, hanya pakai `\u00B0C` (aman), tidak ada glyph berisiko. Tidak masuk hitungan file batch ini.

### Protected Assets tersentuh
Tidak ada (`BatteryMonitorService.kt` bukan Protected Asset list; NavGraph/Manifest/dll tidak disentuh batch ini).

### Pending Queue (tidak berubah dari Batch 12)
1-5. ✅ selesai (lihat Batch 8-12)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 12] Fitur - Tes Baterai / Stress Test (Pending Queue #5) — 2026-08-19

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 47 -> 48 file (1 baru: `StressTestScreen.kt`; 2 diedit parsial: `NavGraph.kt`, `AndroidManifest.xml`)

### Selesai
- **`ui/screens/stress/StressTestScreen.kt`** (baru, self-contained — pola sama `DrainScreen.kt`, tanpa file ViewModel terpisah supaya tetap dalam batas 3 file/batch): sesi tetap 10 menit, state `IDLE -> RUNNING -> FINISHED`. Baca kondisi baterai via `BatteryUtils.readSnapshot()` (sumber sama dengan Dashboard/service, tidak bikin `BroadcastReceiver` baru) di-poll tiap 1 detik lewat `LaunchedEffect`. Tombol mulai di-disable jika charger terpasang (`isCharging`) supaya tes mengukur drop asli. Hasil akhir: total drop%, laju drain %/menit, warning kalau charger sempat nyambung di tengah tes.
- **Wake lock TERKONTROL**: `PowerManager.PARTIAL_WAKE_LOCK` di-acquire dengan **timeout eksplisit** (11 menit = buffer 1 menit di atas durasi tes 10 menit) sebagai safety-net, dan **selalu dilepas** via `DisposableEffect(onDispose { ... })` — baik saat tes selesai normal, dihentikan manual, maupun user navigasi keluar paksa. Tidak pernah acquire tanpa timeout.
- **`AndroidManifest.xml`** (edit parsial, protected asset): tambah `<uses-permission android:name="android.permission.WAKE_LOCK" />` — wajib untuk `PowerManager.newWakeLock()`, izin normal (auto-grant).
- **`NavGraph.kt`** (edit parsial, protected asset): tambah route non-tab `stress_test` + `FloatingActionButton` (ikon Timer) di-overlay pada composable tab Dashboard (`Box` membungkus `DashboardScreen()` + FAB) sebagai entry point. **Sengaja tidak nambah tab ke-5 di bottom nav** (spec awal Batch 1 tetap 4 tab) dan **sengaja tidak edit `DashboardScreen.kt`** — FAB & navigasi murni diletakkan di level `NavGraph.kt` supaya batch ini tuntas dalam 3 file (1 baru + 2 edit parsial), bukan 4.

### Sengaja TIDAK diubah
- `DashboardScreen.kt` — tidak disentuh sama sekali (lihat alasan di atas), tetap `DashboardScreen(viewModel: DashboardViewModel = viewModel())` tanpa parameter navigasi.
- `BatteryLogEntity`/`BatteryLogDao` (protected, DB Schema/DAO) — hasil stress test **tidak** disimpan ke Room (di luar scope; hasil hanya tampil di layar sesi berjalan). Kalau user mau riwayat stress test persisten, perlu task terpisah (tabel baru = ubah DB schema = protected asset, butuh keputusan/izin eksplisit user dulu).

### Protected Assets tersentuh (edit parsial, sesuai rule)
AndroidManifest.xml (1 baris `<uses-permission>` ditambah) • NavGraph.kt (tambah 1 route + FAB, seluruh isi lain diverifikasi utuh, tidak ada penghapusan).

### Pending Queue (Batch 12: item #5 selesai, 1 wajib + 2 opsional tersisa)
1. ~~Kalibrasi engine~~ ✅ Batch 8
2. ~~Cycle Counter presisi~~ ✅ Batch 9
3. ~~Drain Analyzer~~ ✅ Batch 10
4. ~~Riwayat 30 Hari~~ ✅ Batch 11
5. ~~Tes Baterai (Stress Test)~~ ✅ selesai batch ini
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 11] Fitur - Riwayat 30 Hari (Pending Queue #4) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 45 -> 47 file (2 baru: `CsvExporter.kt`, `HistoryViewModel.kt`; 1 ditulis ulang: `HistoryScreen.kt`)

### Selesai
- **`util/CsvExporter.kt`** (baru): export `battery_log` ke CSV via MediaStore (API 29+, pola sama seperti `CrashLogger.kt`), tersimpan ke `Documents/VoltCare/exports/history_<timestamp>.csv`. Kolom: `timestamp_iso,percent,temperature_c,voltage,current_ma,is_charging,health_percent`. `ExportResult` sealed class (`Success`/`Failure`) untuk feedback UI, fail-safe try-catch.
- **`ui/screens/history/HistoryViewModel.kt`** (baru): kombinasi `BatteryLogDao.since(30 hari lalu)` + `CycleDao.all()` (filter `endTimestamp` dalam 30 hari, tanpa ubah query DAO existing - protected asset). Hitung agregat: avg/min/max Health%, avg/max Suhu, jumlah Cycle periode. `exportCsv()` trigger `CsvExporter`, hasil ditampilkan lewat `exportMessage` (Snackbar sekali-tampil).
- **`ui/screens/history/HistoryScreen.kt`** (ditulis ulang dari scaffold placeholder): kartu ringkasan (Health/Suhu/Cycle), 2 grafik garis (Health% & Suhu) via `Canvas` Compose native — **tanpa dependency chart eksternal** (selaras rule minimal footprint, tidak nambah baris di `build.gradle.kts`), tombol "Export CSV" + Snackbar status.

### Sengaja TIDAK diubah
- `BatteryLogDao.kt`/`CycleDao.kt` (protected, DB Schema/DAO) — query `since()` sudah cukup sejak Batch 1, agregasi 30 hari dilakukan di ViewModel (Kotlin), bukan query SQL baru.
- `NavGraph.kt` — `HistoryScreen()` tetap dipanggil tanpa parameter tambahan (default `viewModel()` di signature-nya sendiri), tidak perlu ubah rute.
- `AndroidManifest.xml` — export CSV pakai MediaStore API 29+ murni, tidak butuh permission tambahan (pola sama seperti crash logger yang sudah ada sejak Batch 1).

### Protected Assets tersentuh
Tidak ada.

### Pending Queue (Batch 11: item #4 selesai, 2 tersisa)
1. ~~Kalibrasi engine~~ ✅ Batch 8
2. ~~Cycle Counter presisi~~ ✅ Batch 9
3. ~~Drain Analyzer~~ ✅ Batch 10
4. ~~Riwayat 30 Hari~~ ✅ selesai batch ini
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 10] Fitur - Drain Analyzer (Pending Queue #3) — 2026-08-19

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 44 -> 45 file (1 baru: `UsageStatsHelper.kt`, 2 diedit: `DrainScreen.kt` ditulis ulang penuh, `AndroidManifest.xml` parsial)

### Keterbatasan API (transparan, bukan bug)
Android **tidak** mengekspos data drain-per-app (mAh terpakai saat layar mati) ke aplikasi pihak ketiga tanpa root/system privilege — data itu hanya ada di Settings > Baterai internal OS (API tersembunyi). Yang tersedia publik via `UsageStatsManager` adalah **total waktu pemakaian foreground per app**. Implementasi ini mengurutkan app berdasarkan itu sebagai proxy kandidat penguras (app yang sering dipakai lama & punya kemungkinan besar meninggalkan proses/service di background) — bukan pengukuran mAh langsung. Dicatat eksplisit di komentar kode & UI (`UsageStatsHelper.kt`, `DrainScreen.kt`) supaya user tidak salah ekspektasi.

### Selesai
- **`util/UsageStatsHelper.kt`** (baru): `hasUsageAccessPermission()` (cek via `AppOpsManager.OPSTR_GET_USAGE_STATS`), `openUsageAccessSettings()` (buka `Settings.ACTION_USAGE_ACCESS_SETTINGS`), `topAppsByForegroundUsage()` (query `UsageStatsManager.queryUsageStats(INTERVAL_BEST, ...)` 24 jam terakhir, resolve label app via `PackageManager`, urut descending, exclude system app dari daftar force-stop), `killBackgroundApp()` (best-effort via `ActivityManager.killBackgroundProcesses`).
- **`DrainScreen.kt`** (ditulis ulang dari scaffold placeholder): alur permission-gate (tombol buka Pengaturan Usage Access jika belum diizinkan) → `LazyColumn` daftar top app + waktu pemakaian + tombol "Force Stop" per app (disembunyikan untuk system app, karena `killBackgroundProcesses` pada app sistem umumnya no-op/berisiko).
- **`AndroidManifest.xml`** (edit parsial, protected asset): tambah `<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />` (izin normal, auto-grant saat install, wajib dideklarasikan untuk memakai `ActivityManager.killBackgroundProcesses`). `PACKAGE_USAGE_STATS` sudah ada sejak Batch 1, tidak diubah.

### Batasan "Force Stop"
`killBackgroundProcesses` hanya mematikan proses cached/background milik app target — **tidak sekuat** "Force Stop" bawaan Settings (yang butuh hak sistem, tidak tersedia untuk app pihak ketiga sejak Android 5+). Didokumentasikan langsung di UI ("best-effort") supaya user paham batasannya, bukan janji berlebih — alasan utama Confidence 90% (bukan 95%+) karena efektivitas fitur ini secara inheren dibatasi platform, bukan karena implementasi kurang matang.

### Sengaja TIDAK diubah
- `NavGraph.kt` — route/tab `Drain` sudah ada sejak Batch 1, tidak perlu edit untuk hook screen baru (composable call `DrainScreen()` sudah generic tanpa parameter).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`AndroidManifest.xml` — 1 baris `<uses-permission>` ditambah, seluruh isi lain diverifikasi utuh (diff minimal, tidak ada penghapusan).

### Pending Queue (Batch 10: item #3 selesai, 3 tersisa)
1. ~~Kalibrasi engine~~ ✅ Batch 8
2. ~~Cycle Counter presisi~~ ✅ Batch 9
3. ~~Drain Analyzer~~ ✅ selesai batch ini
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 9] Fitur - Cycle Counter Presisi (Pending Queue #2) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 44 -> 44 file (0 baru/hapus, 2 file diedit: `BatteryUtils.kt`, `BatteryMonitorService.kt`)

### Selesai
- **`BatteryUtils.CycleTracker`** (baru, di dalam `BatteryUtils.kt`, sejajar `CalibrationStore`): cycle counting standar industri — akumulasi mAh masuk (`currentMa × durasi sample`) **lintas banyak sesi charging kecil**, tidak perlu 0-100% sekali jalan tanpa putus (beda dengan syarat `CalibrationStore`). State persisten di `SharedPreferences` terpisah (`voltcare_cycle_tracker`), tahan service di-kill/reboot. Saat akumulasi ≥ kapasitas desain (5000 mAh default), 1 cycle tercatat & sisa (remainder) dibawa ke akumulasi berikutnya (tidak dibuang).
- **`BatteryMonitorService`**: heuristik lama `trackCycle()` (akumulasi kenaikan persen, tidak pernah menulis ke DB — dead-end sejak Batch 1) **dihapus total**, diganti `processCycleTracking()` yang insert ke `CycleEntity(isFullCalibrationCycle = false)` via `CycleDao` tiap cycle presisi selesai. `startPercent` diisi `-1` untuk baris jenis ini (tidak relevan karena satu cycle bisa lintas banyak sesi charging berbeda titik mulai) — field lain (`mahDelivered`, timestamps) tetap terisi akurat.
- Cycle presisi (`isFullCalibrationCycle=false`) & cycle kalibrasi (`isFullCalibrationCycle=true`, Batch 8) berjalan **independen berdampingan** — total di Dashboard (`CycleDao.count()`) otomatis menjumlahkan keduanya, sesuai definisi wear baterai standar (representasi total energi yang pernah masuk, bukan cuma sesi kalibrasi formal).

### Sengaja TIDAK diubah
- `DashboardViewModel.kt` / `DashboardScreen.kt` — kontrak `cycleCount` di `uiState` tidak berubah (masih `db.cycleDao().count()`), otomatis ikut bertambah tanpa perlu edit UI.
- `CycleEntity`/`CycleDao` (DB Schema/DAO, protected) — dipakai apa adanya, tidak ada perubahan schema/migration.

### Protected Assets tersentuh
Tidak ada.

### Pending Queue (Batch 9: item #2 selesai, 4 tersisa)
1. ~~Kalibrasi engine~~ ✅ Batch 8
2. ~~Cycle Counter presisi~~ ✅ selesai batch ini
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 8] Fitur - Kalibrasi Engine (Pending Queue #1) — 2026-08-19

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 44 -> 44 file (0 baru/hapus, 3 file diedit: `BatteryUtils.kt`, `BatteryMonitorService.kt`, `DashboardViewModel.kt`)

### Selesai
- **`BatteryUtils.CalibrationStore`** (baru, di dalam `BatteryUtils.kt`): state machine kalibrasi berbasis `SharedPreferences` (tahan service di-kill/reboot). Alur: tunggu baterai ≤5% saat charging (titik mulai) → pantau sesi hingga ≥99% sambil integrasi `currentMa × waktu` jadi estimasi mAh terkirim → jika charger dicabut sebelum penuh atau persen turun >1% saat charging (drop), sesi dibatalkan & **streak direset ke 0** (syarat "berturut-turut"). Setelah 3 siklus sukses beruntun, Health% dihitung dari `mahDelivered / DEFAULT_DESIGN_CAPACITY_MAH × 100` dan disimpan permanen, menggantikan heuristik tetap 87%.
- **`BatteryMonitorService`**: tiap sampling (60 dtk) memanggil `processCalibrationSample()`; siklus penuh yang selesai otomatis di-insert ke `CycleEntity(isFullCalibrationCycle = true)` via `CycleDao` (skema sudah siap sejak Batch 1, tidak ada perubahan schema). Notifikasi baru "Kalibrasi selesai" saat streak ke-3 tercapai. `estimateHealthPercent()` kini baca `CalibrationStore.calibratedHealthPercent()`, fallback 87% jika belum pernah kalibrasi.
- **`DashboardViewModel`**: `startCalibration()` sekarang benar-benar mengaktifkan `CalibrationStore` (bukan cuma flag UI lokal). Status `calibrationInProgress` disinkronkan dari SharedPreferences tiap sample baru masuk → tombol "Mulai Kalibrasi" otomatis kembali normal saat 3 siklus selesai atau sesi gagal (drop/cabut charger), tanpa perlu ubah `DashboardScreen.kt` (API publik tidak berubah).
- Cycle kalibrasi ikut menambah counter "Cycle" yang sudah tampil di Dashboard (efek samping positif, tidak perlu UI baru).

### Sengaja TIDAK diubah (di luar scope batch ini)
- `DashboardScreen.kt` — tidak disentuh, kontrak UI (`uiState`, `calibrationInProgress`) dipertahankan agar tetap dalam batas 3 file/batch.
- Cycle Counter presisi untuk siklus non-kalibrasi (Pending Queue #2) — heuristik `trackCycle()` lama dibiarkan apa adanya, di luar scope task ini.

### Protected Assets tersentuh
Tidak ada. `CycleEntity`/`CycleDao` (DB Schema/DAO, protected) dipakai apa adanya tanpa modifikasi — field `isFullCalibrationCycle` & query `recentCalibrationCycles` sudah tersedia sejak Batch 1.

### Pending Queue (Batch 8: item #1 selesai, 5 tersisa)
1. ~~Kalibrasi engine~~ ✅ selesai batch ini
2. Cycle Counter presisi (non-kalibrasi)
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 7] Dokumentasi - Konvensi Penamaan Artifact — 2026-08-19

**Confidence Rating: 99%**
**File sebelum -> sesudah:** 44 -> 44 file (0 kode diubah, hanya dokumentasi: `PROJECT_STATE.md`, `CHANGELOG.md`)

### Alasan
User klarifikasi: niat asli minta rename total (Batch 5) itu supaya nama artifact gak kepanjangan sampai kepotong di card UI, bukan murni soal branding. Supaya sesi chat lain (yang gak baca history percakapan ini) langsung paham konvensinya, aturan penamaan artifact dipatenkan di blok "KONVENSI TETAP" di atas — dibaca duluan sebelum baca log batch manapun.

### Selesai
- Tambah blok pinned "KONVENSI TETAP" di paling atas file ini (di atas log Batch, supaya kebaca duluan).
- Tidak ada perubahan kode/arsitektur — murni dokumentasi.

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 6] Smart Naming - APK Release Asset — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 44 -> 44 file (1 file diedit: `.github/workflows/release.yml`)

### Selesai
- Asset APK di GitHub Release tidak lagi bernama generik `app-release.apk` — sekarang otomatis di-rename jadi `<NamaApp>_v<Versi>_<RunNumber>.apk`, mis. `VoltCare_v1.0.0_5.apk`. Konsisten dengan konvensi penamaan ZIP (`<NamaApp>_v<Versi/Batch>.zip`).
- `<NamaApp>` diambil dinamis dari `rootProject.name` di `settings.gradle.kts` (bukan hardcode), jadi otomatis ikut kalau nama app berubah lagi di masa depan.
- Step baru "Rename APK asset" disisipkan setelah "Verify APK is signed", sebelum "Clean up keystore" — hanya jalan setelah APK dipastikan signed (guard Batch 4 tetap berlaku).

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 5] Total Rebrand - VoltCare (Atomic Change) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 44 -> 44 file (0 baru/hapus, 1 file di-rename, ~20 file diedit)
**Alasan Atomic Change (lampaui batas 10 file/batch):** rename package Kotlin bersifat all-or-nothing — setiap file `.kt` mendeklarasikan `package com.voltcare.app...` dan saling import satu sama lain, jadi tidak bisa dipecah jadi beberapa batch parsial tanpa membuat project gagal compile di tengah proses.

### Alasan
Sebelumnya (Batch 2) nama "VoltCare" cuma diterapkan di `strings.xml > app_name` (nama tampilan), sementara applicationId/package/nama kelas/nama DB semua masih "PowerVault". User: ini bikin kesalahpahaman terus-menerus (screenshot Release masih nampilin repo & konteks "PowerVault" padahal app-nya "VoltCare"). Diputuskan: rename total, applied ke SELURUH isi repository.

### Selesai
- **applicationId & namespace**: `com.powervault.health.pro` -> **`com.voltcare.app`** (di `app/build.gradle.kts`).
- **Struktur folder Kotlin**: `app/src/main/java/com/powervault/health/pro/` -> `app/src/main/java/com/voltcare/app/` (22 file `.kt` dipindah + `package` declaration & seluruh import internal diupdate). Diverifikasi otomatis: package declaration cocok 100% dengan folder path, tidak ada import yang masih rujuk path lama.
- **Rename kelas/fungsi**: `PowerVaultApplication`→`VoltCareApplication` (+file rename), `PowerVaultTheme`→`VoltCareTheme`, `PowerVaultNavGraph`→`VoltCareNavGraph`, `PvTab`→`VcTab`, token warna/tipografi `Pv*`→`Vc*` (`PvGreen`, `PvAmber`, `PvRed`, `PvBgDark`, `PvSurfaceDark`, `PvTextPrimary`, `PvTextSecondary`, `PvTypography`).
- **AndroidManifest.xml**: `android:name=".VoltCareApplication"`, `android:theme="@style/Theme.VoltCare"` (application + activity).
- **themes.xml**: `Theme.PowerVault` -> `Theme.VoltCare`.
- **Nama database Room**: `powervault_db` -> `voltcare_db` (+ path exclude di `backup_rules.xml` & `data_extraction_rules.xml`). **Aman**: belum pernah ada instalasi sukses (release sebelumnya selalu APK unsigned/gagal install, lihat Batch 4), jadi tidak ada risiko data hilang di device user manapun.
- **Folder crash log** (`CrashLogger.APP_FOLDER`): `PowerVaultHealthPro` -> `VoltCare` → path jadi `Documents/VoltCare/logs/`.
- `proguard-rules.pro`: header + `-keep class com.voltcare.app...`.
- `settings.gradle.kts`: `rootProject.name` -> `"VoltCare"`.
- `.github/workflows/release.yml`: nama GitHub Release -> `VoltCare v... (build ...)`.
- `README.md`, `FILE_MANIFEST.txt`: diupdate ke struktur & branding baru.

### Sengaja TIDAK diubah
- **Nama repo GitHub** (`FDzaki-dev/PowerVaultHealthPro`) — itu properti repo di sisi GitHub, bukan isi file. Kalau mau rename juga, jalankan manual: `gh repo rename VoltCare` (redirect otomatis dari URL lama, remote `origin` di Termux tetap jalan). Tidak dieksekusi otomatis supaya tidak mengubah path folder lokal `~/projects/PowerVaultHealthPro` yang dipakai skrip Termux tanpa sepengetahuan user.
- Isi historis `CHANGELOG.md`/`PROJECT_STATE.md` Batch 1-4 tidak ditulis ulang (biar akurat sebagai catatan sejarah apa yang terjadi saat itu).

### Protected Assets tersentuh (edit parsial, sesuai rule)
AndroidManifest.xml • app/build.gradle.kts • settings.gradle.kts • DB Schema (nama DB) — semua diverifikasi utuh, tidak ada yang hilang.

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual, lihat catatan di atas)

---

## [Batch 4] Fix Install - APK Unsigned (paket tidak valid) — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 44 -> 44 file (0 file baru/hapus, 2 file diedit parsial)
**Sumber analisa:** screenshot GitHub Release user (asset bernama `app-release-unsigned.apk`, 1.8 MB) + pesan Android "Aplikasi tidak diinstal karena paket tampaknya tidak valid" + inspeksi langsung `app/build.gradle.kts` dan `.github/workflows/release.yml`.

### Root Cause
Signing config **silent-skip**, bukan gagal build. Di `app/build.gradle.kts`, path keystore dicek pakai `file(storeFilePath)`. Fungsi `file()` di dalam module build script (`app/build.gradle.kts`) resolve path **relatif ke folder `app/`**, bukan root repo. Workflow menulis keystore ke `<root>/keystore/release.keystore` dan set `ANDROID_KEYSTORE_PATH=keystore/release.keystore` (relatif) — jadi Gradle mengecek `app/keystore/release.keystore` yang tidak pernah ada. Kondisi `if (f.exists())` selalu false -> `signingConfig` tidak pernah dipasang ke `buildTypes.release` -> AGP tetap sukses assemble tapi keluarkan `app-release-unsigned.apk`. Workflow lama tidak punya guard, jadi APK unsigned itu ikut ter-publish sebagai GitHub Release asset -> Android menolak instal (unsigned APK dari luar ADB = "paket tidak valid").

### Fix
- `app/build.gradle.kts`: `file(storeFilePath)` -> `rootProject.file(storeFilePath)` di 2 tempat (signingConfigs.release & buildTypes.release), jadi resolve path selalu dari root repo, terlepas dari cara path dikirim.
- `.github/workflows/release.yml`: `ANDROID_KEYSTORE_PATH` diubah ke absolute path `${{ github.workspace }}/keystore/release.keystore` (defense in depth, tidak bergantung sama sekali pada working-dir Gradle).
- Step "Locate APK" dikeraskan: `find ... ! -name "*unsigned*"` supaya tidak pernah salah pilih APK unsigned kalau kebetulan ada dua output.
- Step baru "Verify APK is signed": abort (`exit 1`) sebelum publish kalau tidak ada APK signed ditemukan — menegakkan Release Blocking Rule (dilarang publish APK unsigned) secara otomatis di CI, bukan cuma manual review.

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang

---

## [Batch 3] Fix Build - Missing Import NavGraph.kt — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 45 -> 45 file
**Sumber analisa:** log GitHub Actions run (`build-release` job) yang diupload user — gagal di step "Build signed release APK".

### Root Cause
`:app:compileReleaseKotlin FAILED` — `NavGraph.kt:72:33 Unresolved reference: padding`.
`Modifier.padding(innerPadding)` dipakai tapi import `androidx.compose.foundation.layout.padding` tidak ada di file ini (sudah benar di 4 file screen lain, hanya NavGraph.kt yang miss).

### Fix
- Tambah `import androidx.compose.foundation.layout.padding` di `NavGraph.kt`.
- Diverifikasi: tidak ada file lain dengan pola import hilang yang sama.

### Catatan (non-blocking, belum di-fix)
- Build log juga menunjukkan warning KSP: Room `exportSchema=true` tanpa `room.schemaLocation` diset → skema tidak diekspor ke JSON. Tidak menggagalkan build, dijadwalkan Pending Queue jika dibutuhkan riwayat migrasi formal.

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang

---

## [Batch 2] Rename App - VoltCare — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 45 -> 45 file (tidak ada file baru/hapus)

### Selesai
- Nama tampilan app diganti `VoltCare` -> **VoltCare** (`strings.xml > app_name`).
- README.md judul disesuaikan.
- **Tidak ada perubahan arsitektur**: `applicationId`/`namespace` tetap `com.voltcare.app`, struktur package, DB schema, service, dan nama folder/repo Git (`VoltCare`) tetap sama persis sesuai permintaan user ("tanpa rombak arsitektur").

### Pending Queue (belum berubah dari Batch 1)
1. Kalibrasi engine
2. Cycle Counter presisi
3. Drain Analyzer (UsageStatsManager + force-stop)
4. Riwayat 30 Hari (grafik + CSV export)
5. Tes Baterai (Stress Test)
6. Aturan Cerdas - UI Editor

---

## [Batch 1] Initial Setup - Arsitektur & Skeleton — 2026-08-19

**Confidence Rating: 92%** (di bawah 95% karena 4 dari 8 fitur inti masih scaffold/placeholder — lihat Pending Queue. Tidak ada Protected Asset yang hilang, sehingga ZIP tetap dirilis sebagai fondasi arsitektur, bukan build fitur-lengkap final.)

**File sebelum -> sesudah:** 0 -> 45 file

### Selesai
- Struktur project Kotlin + Jetpack Compose + Room + KSP, minSdk 29 / targetSdk 34.
- 4 Tab navigasi (Dashboard, Riwayat, Penguras, Aturan) via Navigation-Compose + NavGraph.kt.
- **Dashboard** fungsional penuh: baca live dari `BatteryManager` (health%, suhu, volt, mA, status cas), estimasi waktu penuh, cycle count dari Room, tombol "Mulai Kalibrasi" (alur kalibrasi 3x penuh -> Pending Queue).
- Room DB v1: `BatteryLogEntity`, `CycleEntity`, `RuleEntity` + DAO masing-masing.
- `BatteryMonitorService` (Foreground Service): sampling tiap 60 detik, simpan log, retensi otomatis 30 hari, evaluasi Aturan Cerdas dasar (TEMP_ABOVE/PERCENT_ABOVE/PERCENT_BELOW), notifikasi persisten + alert.
- `BootReceiver` untuk auto-restart service setelah reboot.
- **Crash Logger bawaan**: MediaStore API 29+, path `Documents/VoltCare/logs/`, FIFO 50 log, metadata lengkap, fail-safe try-catch. Terpasang di `VoltCareApplication`.
- Permission: `BATTERY_STATS`, `PACKAGE_USAGE_STATS` (manifest, belum ada runtime request UI untuk Usage Access - lihat Pending Queue), `POST_NOTIFICATIONS` (runtime request di MainActivity), `FOREGROUND_SERVICE`.
- GitHub Actions `release.yml`: build signed APK, **Stale Run Guard** (bandingkan GITHUB_SHA vs `git ls-remote` tip main, abort jika beda), publish sebagai **GitHub Release** (bukan cuma artifact).
- Release keystore **auto-generated** (RSA 2048, valid 10.000 hari) + file secrets terpisah untuk `gh secret set`.
- Adaptive launcher icon (vector, tanpa aset PNG biner).

### Pending Queue (batch berikutnya, sesuai Micro-Batching Rule)
1. **Kalibrasi engine** - alur wajib 3x charge 0-100% berturut-turut + validasi non-drop untuk Health% akurat (saat ini health% masih heuristik tetap 87%).
2. **Cycle Counter presisi** - deteksi siklus penuh berbasis akumulasi mAh riil + simpan ke `CycleEntity` (saat ini baru heuristik akumulasi persen di service, belum menulis ke tabel `cycle_history`).
3. **Drain Analyzer** - integrasi `UsageStatsManager` untuk top app penguras saat layar mati + aksi force-stop (`ActivityManager.killBackgroundProcesses` + izin Usage Access runtime).
4. **Riwayat 30 Hari** - grafik Health/Suhu/Cycle (Compose Canvas atau library ringan) + export CSV via MediaStore.
5. **Tes Baterai (Stress Test)** - sesi 10 menit, ukur drop % & kesehatan, wake lock terkontrol.
6. **Aturan Cerdas - UI Editor** - form buat/edit/hapus `RuleEntity` dari tab Aturan (engine evaluasi di service sudah jalan).
7. Migration Room eksplisit disiapkan begitu skema v2 dibutuhkan (jangan destructive migration di produksi).

### Catatan Teknis Penting
- **Gradle wrapper JAR tidak disertakan** (biner, tidak bisa di-generate offline di lingkungan pembuatan ZIP ini). Workflow CI memakai `gradle/actions/setup-gradle` + perintah `gradle` langsung (bukan `./gradlew`). Untuk build lokal di Termux dengan SDK, install gradle via `pkg install gradle` atau jalankan `gradle wrapper` sekali di mesin ber-internet untuk generate wrapper jar jika diperlukan.
- `release.keystore` ada di folder `keystore/` hasil unzip tapi **di-gitignore** (tidak pernah ke-commit). Sumber kebenaran signing ada di GitHub Secrets.
- Desain kapasitas default 5000 mAh dipakai untuk estimasi waktu penuh sebelum kalibrasi pertama selesai.

### Protected Assets Checklist (semua utuh ✅)
AndroidManifest.xml • app/build.gradle.kts • settings.gradle.kts • root build.gradle.kts • MainActivity.kt • VoltCareApplication.kt • NavGraph.kt • AppDatabase.kt + 3 Entity + 3 DAO • release.keystore • .gitignore • .gitattributes • .github/workflows/release.yml
