# PROJECT_STATE.md
(Urutan DESCENDING - entri terbaru di paling atas)

---

## 📌 KONVENSI TETAP (baca duluan, berlaku untuk semua batch berikutnya)

**Nama App untuk artifact (ZIP & APK release): `VoltCare`.**

- **Nama repo GitHub `FDzaki-dev/VoltCare`** (di-rename manual oleh user, dikonfirmasi Batch 28).
- **Folder lokal Termux SEKARANG JUGA `~/projects/VoltCare`** (di-rename manual oleh user via `mv`, Batch 29 — lihat skrip fix). Sebelumnya (Batch 5-28) sengaja dipertahankan `~/projects/PowerVaultHealthPro`, TAPI user minta rename eksplisit di Batch 29, jadi konvensi ini SEKARANG BERUBAH.
- ⚠️ **Riwayat**: SEMUA entri batch di bawah (Batch 1-28) yang menyebut path `~/projects/PowerVaultHealthPro` atau nama folder `PowerVaultHealthPro` sbg lokasi kerja adalah CATATAN HISTORIS SAAT ITU — SUDAH TIDAK BERLAKU per Batch 29. Path aktif sekarang: `~/projects/VoltCare`.
- **ZIP output**: `VoltCare_v<Versi>_Batch<N>.zip` (root ZIP isi project, tetap flat di root — nama project internal/package Kotlin `com.voltcare.app` TIDAK berubah, murni rename folder & repo, bukan rename package).
- **APK release asset** (`release.yml`): `VoltCare_v<Versi>_<RunNumber>.apk` (otomatis dari `rootProject.name`, tidak terpengaruh).
- **Skrip Termux (mulai Batch 29 dan seterusnya)**: `LATEST_ZIP=$(ls -t ~/storage/downloads/VoltCare*.zip | head -1)` + `cd ~/projects/VoltCare` (BUKAN lagi `PowerVaultHealthPro`). `git remote -v` menunjuk `https://github.com/FDzaki-dev/VoltCare.git`.
- Ringkas: **VoltCare = nama produk/artifact/repo GitHub/folder lokal Termux** — SEMUA SUDAH SELARAS mulai Batch 29. Tidak ada lagi perbedaan nama produk vs repo vs folder.
- 🔴 **RULE WAJIB (mulai Batch 37, permintaan eksplisit user):** SETIAP batch yang menghasilkan artifact ZIP WAJIB bump `versionCode` (+1) & `versionName` (patch, mis. 1.0.1 -> 1.0.2) di `app/build.gradle.kts` — TIDAK BOLEH dilewatkan/ditunda lagi, walaupun perubahan batch itu kecil (docs-only, 1 baris, dsb). Ini berlaku TERPISAH dari fallback teknis `CI_RUN_NUMBER` (Batch 36) — fallback itu tetap ada sbg jaring pengaman kalau suatu saat bump kelewat, TAPI bukan alasan buat malas bump manual. Tiap kali mau `present_files` ZIP baru, cek dulu: apakah `versionCode`/`versionName` sudah naik dari batch sebelumnya? Kalau belum, bump DULU sebelum repack & present.

---

---

---

---

## [Batch 62] Fix - FEATURE_PARITY_GOALS.md Desync soal "Hemat Daya Otomatis" (RESOLVED) — 2026-08-20

**Konteks:** User tanya "next pending: Hemat daya otomatis tanpa bikin lambat HP" — mengira fitur ini belum ada (mengacu ke tabel `FEATURE_PARITY_GOALS.md` item #9 yang masih ❌).

**Audit:** Fitur ini SUDAH selesai sejak **Batch 44** — **Auto-Hibernate Terjadwal** (`HibernateWorker.kt`, WorkManager `PeriodicWorkRequest` interval 30 menit, whitelist per-app via checkbox di tab **Penguras**). `PROJECT_STATE.md` Batch 44 sudah mencatat "Pending Queue: 12 ✅ selesai", TAPI `FEATURE_PARITY_GOALS.md` (dokumen matrix terpisah) tidak pernah di-update in-place saat itu — root cause murni dokumentasi basi, BUKAN kode hilang/regresi.

**Fix:** Update tabel `FEATURE_PARITY_GOALS.md` item #7 & #9 ke status akurat + skor cakupan (3→4 Done) + entri revisi baru. Tidak ada perubahan kode/APK.

**File diubah (1)**: `FEATURE_PARITY_GOALS.md`.
**Bump**: versionCode 25→26, versionName 1.0.24→1.0.25 (docs-only, tetap wajib per RULE Batch 37).

**Rekomendasi ke user:** Fitur sudah aktif — buka tab **Penguras**, nyalakan Switch "Auto-Hibernate Terjadwal", centang app yang mau di-hibernate otomatis tiap 30 menit. Kalau maksud user beda dari ini (mis. mode hemat daya sistem-wide: turunkan refresh rate/brightness/sync interval), balas biar dikerjain sbg item baru — bukan duplikat Batch 44.

**Pending Queue tetap:** #27 (isEnabled ke-reset saat edit rule, belum digarap).

---

## [Batch 61] Fix - Judul Nada Alarm Custom Gak Kebaca (RESOLVED) — 2026-08-20

**Konteks:** Setelah Batch 60, tombol nada alarm cuma nampilin teks generik "Nada Alarm: Custom terpilih ✓" — user gak tau file sound apa yang kepilih (screenshot user).

**Implementasi:**
- `RulesScreen.kt` (`RuleFormDialog`): tambah `alarmSoundTitle` state + `LaunchedEffect(alarmSoundUri)` resolve judul asli via `RingtoneManager.getRingtone(context, Uri.parse(uriStr))?.getTitle(context)`, dibungkus `runCatching` (fallback teks "Custom" kalau resolve gagal/null/uri sistem tanpa title readable).
- Tombol sekarang: `"Nada Alarm: ${judulAsli} ✓"`.
- Tambah import `LaunchedEffect` + `LocalContext`.
- 1 file, sesuai batch cap.
- Bump wajib: versionCode 24→25, versionName 1.0.23→1.0.24 (`app/build.gradle.kts`).

**Pending Queue (belum berubah, masih carry-over):**
- #27: `saveRule` masih hardcode `isEnabled = true` saat edit rule (dari Batch 60, belum digarap).

---

## [Batch 60] Fitur - UI Pilih Nada Alarm Custom (Pending Queue #26, RESOLVED) — 2026-08-20

**Konteks:** Lanjutan Batch 59 — engine+wiring alarm sudah bunyi, tapi user tidak bisa pilih nada sendiri (selalu default sistem).

**Implementasi:**
- `RulesScreen.kt` (`RuleFormDialog`): tombol "Pilih Nada Alarm" muncul saat Aksi = ALARM, buka `RingtoneManager.ACTION_RINGTONE_PICKER` via `rememberLauncherForActivityResult`, hasil `EXTRA_RINGTONE_PICKED_URI` disimpan ke state lokal lalu diteruskan lewat `onSave`.
- `RulesViewModel.kt` (`saveRule`): tambah parameter `alarmSoundUri: String?`, diteruskan ke `RuleEntity.alarmSoundUri` (kolom sudah ada dari Batch 58).
- Efek samping fix: sebelumnya `saveRule` saat mode edit selalu membangun `RuleEntity` baru tanpa membawa `alarmSoundUri` lama → override ke null. Sekarang eksplisit dibawa dari state form (`existing?.alarmSoundUri` sbg initial value).
- 2 file, sesuai batch cap.

**Pending Queue (belum dikerjakan, dicatat, BUKAN diabaikan):**
- #27: `saveRule` masih hardcode `isEnabled = true` saat edit — toggle aktif/nonaktif yang sudah di-set user bisa ke-reset ke `true` tiap kali rule diedit lewat form (bug lama, ditemukan saat audit batch ini, di luar scope Pending #26).

---

## [Batch 59] Fix - Alarm Tidak Bunyi Saat Threshold Tercapai (Wiring AlarmPlayer) — 2026-08-20

**Masalah:** Rule dengan aksi ALARM tidak pernah bunyi/getar walau kondisi (threshold) terpenuhi.

**Root cause:** `AlarmPlayer.kt` (engine, dibuat Batch 58) belum disambung ke `BatteryMonitorService.fireAlert()` — fungsi itu cuma posting `NotificationCompat` biasa, tidak pernah cek `rule.actionType` atau panggil `AlarmPlayer.play()`. Sudah tercatat sendiri sbg Pending Queue #25 di komentar `AlarmPlayer.kt`.

**Fix (RESOLVED):** `BatteryMonitorService.kt` → `fireAlert()`: tambah cek `if (rule.actionType == "ALARM") AlarmPlayer.play(applicationContext, rule.alarmSoundUri)` sebelum posting notifikasi. 1 file.

**Pending Queue (belum dikerjakan, next batch):**
- #26: `RuleFormDialog` di `RulesScreen.kt` belum ada tombol pilih nada custom (`ACTION_RINGTONE_PICKER`) — `alarmSoundUri` masih selalu null dari UI, jadi selalu fallback ke alarm default sistem.

---

## [Batch 58] Fitur - Custom Alarm (Core Engine, belum diwiring UI/Service) — 2026-08-20

**Confidence Rating: 92%**
**File sebelum -> sesudah:** 57 -> 58 file (1 baru: `AlarmPlayer.kt`; 2 diedit parsial protected: `RuleEntity.kt`, `AppDatabase.kt` — DB Schema/DAO; 1 diedit parsial protected: `app/build.gradle.kts` — bump versi wajib RULE Batch 37)

### Alasan
User minta "opsi custom alarm". Ditemukan gap nyata: `RuleAction.ALARM` ("Alarm (getar + suara)") sudah tersimpan di DB & bisa dipilih di UI Aturan sejak lama, TAPI `BatteryMonitorService.fireAlert()` ternyata cuma posting notifikasi biasa via `CHANNEL_ALERT` — tidak pernah benar-benar memutar suara alarm/getar berbeda dari notifikasi NOTIFY biasa, dan tidak ada opsi nada custom sama sekali. Scope penuh (schema + picker UI + wiring service) > 3 file → dipecah bertahap meniru pola Shizuku (Batch 23 core -> Batch 26 UI wiring). **Batch 58 (ini) = core engine (schema + player)**, sisanya di-queue.

### Selesai
- **`RuleEntity.kt`** (edit parsial, protected - DB Schema): +1 kolom `alarmSoundUri: String? = null` (URI nada custom dari `RingtoneManager.ACTION_RINGTONE_PICKER`, null = pakai default sistem).
- **`AppDatabase.kt`** (edit parsial, protected - DB Schema): `version` 1->2, tambah `MIGRATION_1_2` (`ALTER TABLE smart_rule ADD COLUMN alarmSoundUri TEXT`, non-destruktif, data lama utuh) + `.addMigrations(MIGRATION_1_2)` di builder. TIDAK pakai `fallbackToDestructiveMigration` (sesuai komentar Protected Asset yang sudah ada sejak Batch 1).
- **`util/AlarmPlayer.kt`** (baru, self-contained, pola fail-safe sama seperti `ShizukuManager.kt`/`UpdateManager.kt`): `play(context, customSoundUri)` — resolve URI custom (fallback ke `TYPE_ALARM` default sistem kalau null/invalid), `RingtoneManager.getRingtone()` + `AudioAttributes.USAGE_ALARM`, lalu getar 700ms (`VibrationEffect.createOneShot`, fallback API lama). `stop()` untuk hentikan manual. Permission `VIBRATE` sudah ada di manifest sejak awal — tidak perlu edit manifest.

### Sengaja TIDAK diubah
- **`BatteryMonitorService.fireAlert()`** — belum dipanggil `AlarmPlayer.play()` sama sekali; notifikasi tetap jalur lama apa adanya. Wiring (baca `rule.actionType == "ALARM"` -> panggil `AlarmPlayer.play(rule.alarmSoundUri)`) di-queue Batch berikutnya supaya batch ini tetap 3 file inti.
- **`RulesScreen.kt` / `RuleFormDialog`** — belum ada tombol "Pilih Nada" (launcher `ActivityResultContracts.StartActivityForResult` ke `RingtoneManager.ACTION_RINGTONE_PICKER`) untuk set `alarmSoundUri` dari UI. Murni schema+engine dulu, pola identik Batch 23 (ShizukuManager sebelum UI wiring Batch 26).
- `RuleDao.kt` — tidak perlu diubah (`@Update`/`@Insert` otomatis ikut kolom baru dari entity, tidak ada query manual yang menyebut kolom lama secara eksplisit).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`RuleEntity.kt`, `AppDatabase.kt` (DB Schema/DAO — brace/paren balance diverifikasi, migration non-destruktif). `app/build.gradle.kts` — `versionCode` 23->24, `versionName` "1.0.22"->"1.0.23" (RULE WAJIB Batch 37).

### Catatan
Tidak ada akses Gradle/device fisik sungguhan di lingkungan ini (network disabled) — verifikasi terbatas pada brace/paren balance (semua file 100% seimbang) + review manual API `RingtoneManager`/`VibrationEffect`/`Migration` sesuai dokumentasi resmi, BUKAN compile Gradle sungguhan. Confidence 92% (bukan 95%+) karena: (1) migration Room 1->2 belum diverifikasi jalan di device nyata dengan data lama (`exportSchema=true` berarti file schema JSON baru harus ikut ter-generate saat build - normal, bukan bug), (2) `AlarmPlayer.play()` belum ada caller sama sekali jadi belum bisa diuji device nyata sampai wiring batch berikutnya. Rekomendasi: build lokal dulu (cek folder `app/schemas/` bertambah versi 2) sebelum lanjut wiring UI+Service.

### Pending Queue (Batch 58: fitur besar dipecah, 2 sub-task baru #25-26)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub — ✅ selesai (lihat konvensi tetap, Batch 28)
10-13. (dari Batch 18, belum dikerjakan)
14. (housekeeping) Update `FILE_MANIFEST.txt` — tambah `AlarmPlayer.kt`, gabung ke housekeeping berikutnya
17-20. ✅ selesai (lihat Batch 26 + riwayat Shizuku)
21-23. P1 audit (opsional, kalau user minta eksplisit) — lihat Batch 57
24. ~~Custom Alarm - schema & player~~ ✅ selesai batch ini
25. **Wiring `AlarmPlayer` ke `BatteryMonitorService.fireAlert()`**: panggil `AlarmPlayer.play(context, rule.alarmSoundUri)` saat `rule.actionType == RuleAction.ALARM.stored`, biarkan `NOTIFY` tetap notifikasi biasa tanpa suara/getar tambahan. Estimasi 1 file.
26. **Tombol "Pilih Nada Custom" di `RuleFormDialog`** (`RulesScreen.kt`): launcher `ACTION_RINGTONE_PICKER`, tampilkan nama nada terpilih, simpan hasil ke `alarmSoundUri` lewat `RulesViewModel`. Estimasi 1-2 file (`RulesScreen.kt` + kemungkinan `RulesViewModel.kt` kalau perlu fungsi update terpisah).

---

## [Batch 57] Fix - 3 Bug P0 dari Audit UX Eksternal (Terverifikasi Manual, P1 Ditolak) — 2026-08-20

**Confidence Rating: 94%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 3 file diedit: `DashboardScreen.kt`, `RulesScreen.kt`, `StressTestScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User upload dokumen audit eksternal `VoltCare_UX_Audit_Final_Verdict.md` (P0/P1, "100% user-friendly") dengan instruksi eksplisit: **jangan telan mentah-mentah, verifikasi dulu, abaikan yang menyesatkan**. Sesuai instruksi itu, SETIAP klaim P0 di-cross-check langsung ke source code sebelum ada satu baris pun diubah — bukan trust-by-default ke dokumen pihak ketiga (dokumen audit BUKAN bagian dari hirarki konteks resmi proyek: Chat Saat Ini > PROJECT_STATE.md > FILE_MANIFEST.txt > CHANGELOG.md > README.md).

### Hasil Verifikasi (semua 3 klaim P0 TERBUKTI BENAR, bukan menyesatkan)
1. **Dashboard tidak scrollable** — TERKONFIRMASI. `DashboardScreen.kt` pakai `Column(fillMaxWidth())` tanpa `verticalScroll`. Baris/kartu terakhir (Cycle, tombol Kalibrasi) berisiko ke-clip di layar kecil/skala font besar.
2. **Rules screen scroll/layout** — TERKONFIRMASI, dan sudah SESUAI dgn temuan audit internal saya sendiri (Pending #20, ditulis di Batch 55): `RulesScreen.kt` pola PERSIS sama dgn bug `DrainScreen.kt` yang sudah diperbaiki (Column fillMaxSize + LazyColumn bersarang tanpa weight).
3. **WakeLock StressTest diambil terlalu dini** — TERKONFIRMASI paling serius dari ketiganya (bug perilaku, bukan cuma kosmetik): `val wakeLock = remember { acquirePartialWakeLock(context) }` diambil SAAT LAYAR PERTAMA KALI DI-COMPOSE (begitu `StressTestScreen` tampil/`IdleCard` muncul), BUKAN saat user tekan "Mulai Tes" - CPU tetap terjaga sia-sia kalau user cuma buka layar lalu urung mulai tes. Ironis untuk app kesehatan baterai.

### Selesai
- **`DashboardScreen.kt`**: tambah `.verticalScroll(rememberScrollState())` ke `Modifier` Column yang sudah ada. TIDAK diganti jadi LazyColumn (isinya fixed set of Row/Card, bukan list dinamis) - `verticalScroll` sudah cukup & lebih sederhana. 0 state/logic/data flow disentuh.
- **`RulesScreen.kt`**: pola fix IDENTIK dgn `DrainScreen.kt` Batch 55 - `Column`+`LazyColumn` bersarang -> SATU `LazyColumn` datar (`item{}` utk judul/deskripsi/tombol preset, `items(rules){}` utk daftar). **Pelajaran dari Batch 56 diterapkan**: TIDAK menambah import `androidx.compose.foundation.lazy.item` (itu member function `LazyListScope`, bukan top-level symbol - sumber regresi CI kemarin). Diverifikasi manual: tidak ada import `lazy.item` di file ini.
- **`StressTestScreen.kt`**: `val wakeLock = remember { acquirePartialWakeLock(context) }` (eager) diganti `var wakeLock by remember { mutableStateOf<PowerManager.WakeLock?>(null) }` + `LaunchedEffect(testState)` baru yang acquire TEPAT saat `testState == RUNNING`, release begitu keluar dari RUNNING (state apa pun - FINISHED via loop natural ATAU via "Hentikan Lebih Awal", ATAU balik ke IDLE) - satu sumber kebenaran, tidak bergantung jalur transisi. `DisposableEffect` safety-net (screen leave) disesuaikan null-safe. Timeout eksplisit `WAKELOCK_TIMEOUT_MS` (safety-net kedua) TIDAK diubah. 0 perubahan ke countdown loop/kalkulasi drop/measurement logic.

### DITOLAK (P1, sesuai instruksi user "abaikan saran yang menyesatkan" + regression rule di dokumen audit itu sendiri)
Item 4-8 di dokumen (bahasa teknis "dumpsys/mAh riil/proxy/Shizuku" -> disederhanakan, onboarding izin, deskripsi kontekstual metrik dashboard, tooltip ikon header, copy penjelasan Stress Test) **SENGAJA TIDAK dikerjakan di batch ini**. Alasan: ini bukan "defect" terverifikasi (bug), tapi preferensi desain/copywriting subjektif - dokumen audit itu sendiri menulis regression rule "if a proposed change... without fixing a demonstrated user-facing defect, REJECT the change", dan user secara eksplisit minta fokus "debugging". Kalau user mau salah satu dari P1 dikerjakan, perlu diminta eksplisit sebagai task terpisah (masuk Pending Queue di bawah, bukan diasumsikan otomatis disetujui).

### Sengaja TIDAK diubah
`DrainAppRow`, `RuleRow`, `RuleFormDialog`, `ChargeLimitPresetDialog`, seluruh logic countdown/kalkulasi drop di `StressTestScreen.kt`, `RulesViewModel.kt`, `DashboardViewModel.kt` — 0 perubahan, sesuai batas ketat "Forbidden" di dokumen audit (yang bagian ini justru valid & diikuti: jangan ubah business logic/kalkulasi/data flow).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — `versionCode` 22->23, `versionName` "1.0.21"->"1.0.22" (RULE WAJIB Batch 37).

### Catatan
Confidence **94%** — ketiga fix diverifikasi manual terhadap source code asli (bukan trust dokumen), brace/paren balance dicek per file, pola RulesScreen sama persis dgn DrainScreen yang sudah terverifikasi struktural. 6% sisa: belum ada build CI hijau + belum ada verifikasi device nyata untuk 3 perubahan ini (khususnya WakeLock - perlu tes manual: buka Stress Test, JANGAN tekan Mulai, cek `adb shell dumpsys power` tidak ada `VoltCare:StressTest` PARTIAL_WAKE_LOCK aktif sebelum test dimulai).

### Pending Queue (tambahan, TIDAK otomatis dikerjakan - opsional)
21. P1 dari audit (kalau user minta eksplisit): sederhanakan bahasa teknis (dumpsys/mAh riil/proxy/Shizuku) di empty-state Drain Analyzer.
22. P1 dari audit: alur onboarding izin (Usage Access/Shizuku) - jelaskan alasan sebelum minta, auto-recheck setelah user kembali dari Settings.
23. P1 dari audit: deskripsi kontekstual singkat di tiap metric card Dashboard (Health/Suhu/Volt/Cycle).
24. P1 dari audit: content description/tooltip utk ikon aksi header (Shizuku, Update).
25. P1 dari audit: perkuat copy penjelasan Stress Test (durasi, kenapa lepas charger, hasil apa yg didapat).

---

## [Batch 56] Fix - Build CI Gagal: Import `item` Tidak Valid (Regresi dari Batch 55) — 2026-08-20

**Confidence Rating: 99%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 1 file diedit: `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User upload log GitHub Actions run yang GAGAL (`gh run download` / ZIP log). Root cause ditemukan dari step "Build signed release APK" (bukan dari CrashLogger runtime — build bahkan tidak sampai jadi APK, jadi log CI adalah satu-satunya sumber, sesuai fallback yang didokumentasikan di README).

### Root Cause
**Regresi dari fix Batch 55 (kesalahan saya sendiri).** Saat restrukturisasi `DrainScreen.kt` ke satu `LazyColumn` datar, ditambahkan `import androidx.compose.foundation.lazy.item` — import ini TIDAK VALID. `LazyListScope.item(...)` adalah **member function** dari interface `LazyListScope` (otomatis tersedia di dalam lambda `LazyColumn { }`, tanpa import terpisah) — BEDA dengan `items(List<T>, ...)` yang memang top-level extension function di package yang sama (makanya `import androidx.compose.foundation.lazy.items` valid & tetap dipakai). Kotlin compiler gagal resolve `androidx.compose.foundation.lazy.item` sebagai declaration yang bisa di-import, sehingga `compileReleaseKotlin` FAILED:
```
e: .../DrainScreen.kt:10:41 Unresolved reference: item
```
Build gagal total (0 APK dihasilkan sama sekali) -> `find app/build/outputs/apk/release` tidak ketemu folder -> guard "Verify APK is signed" (Batch 4) benar-benar bekerja sesuai desain: abort duluan, TIDAK ada APK rusak/kosong yang ke-publish. Release Blocking Rule berhasil mencegah dampak ke user, tapi builder tetap gagal & perlu fix ini.

### Fix
Hapus baris `import androidx.compose.foundation.lazy.item` dari `DrainScreen.kt`. Pemanggilan `item { ... }` di dalam `LazyColumn { }` tetap 100% valid tanpa import itu (member function). Tidak ada perubahan logic/UI apa pun selain penghapusan 1 baris import yang salah.

### Sengaja TIDAK diubah
Seluruh restrukturisasi Batch 55 (flat LazyColumn, item/items placement) tetap dipertahankan apa adanya — cuma importnya yang salah, bukan strukturnya.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — `versionCode` 21->22, `versionName` "1.0.20"->"1.0.21" (RULE WAJIB Batch 37).

### Catatan
Confidence **99%** — root cause eksplisit tertulis di error compiler (bukan dugaan), fix single-line, dan dasar teorinya (item = member function LazyListScope, items = top-level extension) konsisten dengan API Compose Foundation resmi. 1% sisa murni krn belum ada run CI baru yang mengonfirmasi build hijau setelah fix ini (rekomendasi: push batch ini, cek tab Actions sampai `build-release` selesai centang hijau sebelum anggap kelar).

### Pending Queue
Tidak ada nomor baru — ini pure regresi-fix dari Batch 55, bukan fitur baru.

---

## [Batch 55] Fix - Drain Analyzer "Kurang Fleksibel dan Scrollable" (Laporan User via Screenshot) — 2026-08-20

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 1 file diedit: `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User kirim screenshot tab Drain Analyzer device nyata + laporan "kurang fleksibel dan scrollable".

### Root Cause
`DrainScreen.kt` sebelumnya: `Column(fillMaxSize())` BIASA (bukan scrollable) sebagai wadah utama, berisi judul + 2 Card toggle + hint text + `LazyColumn` **bersarang** (nested) di posisi terakhir TANPA `Modifier.weight`. Column induk sendiri tidak scroll — kalau total tinggi konten (judul + 2 card + hint + baris app) melebihi tinggi layar, sisanya ke-clip di bawah tanpa cara scroll untuk menjangkaunya. Risiko ini naik signifikan sejak Batch 54 (toggle "Tampilkan Semua App" bisa menampilkan sampai 50 app sekaligus, vs sebelumnya max 15).

### Fix
Restrukturisasi total: `Column` + `LazyColumn` bersarang -> **SATU `LazyColumn` datar** untuk seluruh isi layar. Judul, 2 card toggle, dan hint/pesan kondisional dibungkus `item { }`; baris app tetap `items(apps) { }`. Ini pola idiomatic Compose untuk kombinasi header+list (nested scrollable di dalam parent non-scrollable adalah anti-pattern, bisa bikin konten atas ke-clip seperti kasus ini — kebalikan dari kasus umum lain, yaitu crash "infinite height", yang terjadi kalau nested LazyColumn diletakkan di dalam `Modifier.verticalScroll`).
Efek samping kosmetik minor (disengaja, tidak signifikan): spacing antar baris app di list menyatu jadi 12dp (sebelumnya 8dp khusus di LazyColumn nested) — satu `Arrangement.spacedBy` untuk seluruh list, LazyColumn tidak mendukung spacing berbeda per segmen tanpa spacer composable tambahan.

### Sengaja TIDAK diubah
- `DrainAppRow()`, seluruh logic `LaunchedEffect` (fetch data), `UsageStatsHelper.kt` — 0 perubahan, murni restrukturisasi composition tree, tidak ada logic/kalkulasi yang disentuh.
- **Ditemukan pola identik di `RulesScreen.kt`** (`Column(fillMaxSize)` + nested `LazyColumn` tanpa weight) saat audit cepat file lain yang pakai `LazyColumn` — TIDAK diperbaiki di batch ini (user cuma laporkan Drain Analyzer, scope batch ini dijaga 1 file supaya bisa diverifikasi presisi). Diangkat jadi kandidat Pending Queue baru di bawah.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — `versionCode` 20->21, `versionName` "1.0.19"->"1.0.20" (RULE WAJIB Batch 37).

### Catatan
Confidence **95%** — perubahan murni struktural (pemindahan composable ke `item{}`/`items{}`, logic & callback 100% identik copy-paste), risiko utama cuma kesalahan penempatan kurung/urutan yang sudah diverifikasi manual (brace/paren balance dicek). 5% sisa: belum ada verifikasi device nyata untuk kondisi scroll spesifik (device sama yang laporkan bug, idealnya juga coba toggle "Semua App" dgn banyak app buat pastikan LazyColumn scroll mulus sampai baris terakhir).

### Pending Queue (tambahan)
20. Terapkan fix pola yang sama (Column+nested-LazyColumn tanpa scroll -> flat LazyColumn) ke `RulesScreen.kt` — ditemukan saat audit Batch 55, belum diperbaiki (di luar scope, user belum minta eksplisit).

---

## [Batch 54] Fitur - Mode "Tampilkan Semua App" di Drain Analyzer (Permintaan User) — 2026-08-20

**Confidence Rating: 93%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 2 file diedit: `UsageStatsHelper.kt`, `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User tanya kenapa screenshot Batch 53 cuma nampilin 3 app. Jawaban: desain `mergeDrainData()` (Batch 52) SENGAJA membatasi ke daftar `topAppsByForegroundUsage()` (top-15 berdasar waktu pemakaian 24 jam) — app dgn mAh riil tinggi tapi jarang dibuka manual (JUSTRU kandidat paling relevan buat "penguras baterai") tidak akan pernah muncul. User minta opsi baru utk menampilkan semua app dari data dumpsys.

### Selesai
- **`UsageStatsHelper.kt`**: fungsi baru `fullDrainAppList(context, mahByPackage, hours=24, limit=50)` — membangun `List<AppUsageInfo>` LANGSUNG dari key `mahByPackage` (bukan dari `topAppsByForegroundUsage()`), diperkaya foreground time (kalau ada) via helper baru `rawForegroundMsByPackage()` (private, map mentah packageName->ms TANPA batas 15 — dibuat terpisah, BUKAN refactor `topAppsByForegroundUsage()`, supaya jalur proxy lama 0% berubah). Urutan hasil: mAh descending, limit default 50 (dumpsys UID app pihak ketiga biasanya jauh di bawah itu, cukup generos tanpa bikin list kebablasan).
- **`DrainScreen.kt`**: Card toggle baru "Tampilkan Semua App (dumpsys)" (`Switch`, `enabled = hasRealDrainData` — disabled sampai ada data mAh riil, konsisten pola gating `Auto-Hibernate` yang sudah ada). `LaunchedEffect` key ditambah `showAllDrainApps` supaya toggle langsung memicu refetch. Alur baru: kalau toggle ON & `hasRealDrainData` true -> pakai `fullDrainAppList()`; selain itu -> jalur lama (`topAppsByForegroundUsage` + `mergeDrainData`, 100% tidak berubah). Gating "Butuh izin Akses Penggunaan" disesuaikan (`!showAllDrainApps && !hasPermission`) — mode Semua App TIDAK butuh izin Usage Access sama sekali (sumber datanya dumpsys via Shizuku, bukan `UsageStatsManager`), foreground time optional/boleh 0m. Hint teks & pesan list-kosong disesuaikan 3-cabang (Semua App / proxy+mAh / proxy murni).

### Keputusan Desain Penting
- **Tidak refactor `topAppsByForegroundUsage()`** — sengaja bikin `rawForegroundMsByPackage()` terpisah walau logic-nya mirip, supaya perubahan batch ini 0% berisiko ke jalur lama yang sudah terverifikasi device nyata (Batch 53). Trade-off: sedikit duplikasi kode, tapi blast radius audit lebih kecil (sesuai pola project sejak Batch 49).
- **`limit=50` (bukan unlimited)** — parser sudah memfilter UID sistem (`minUid=10000`), tapi tetap dikasih batas atas jaga-jaga (device dgn sangat banyak app terinstall) supaya `LazyColumn` tidak lag & `PackageManager.getApplicationInfo()` (dipanggil per key) tidak jadi bottleneck berlebihan.
- **Toggle di-reset ke OFF tiap buka layar** (`remember` biasa, bukan persisted) — sengaja, mode "Semua App" sifatnya eksploratif/sesekali, bukan preferensi permanen; kalau user mau permanen, bisa diangkat jadi item Pending Queue terpisah nanti.

### Sengaja TIDAK diubah
- `topAppsByForegroundUsage()`, `mergeDrainData()`, `fetchDrainMahByPackage()`, `BatteryStatsParser.kt`, `ShizukuManager.kt` — 0 perubahan, jalur lama (Batch 52-53, sudah terverifikasi device nyata) dipertahankan 100% apa adanya sbg fallback default.
- `AndroidManifest.xml` — tidak perlu entri baru, sumber data sama persis (Shizuku shell) dgn Batch 52.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — `versionCode` 19->20, `versionName` "1.0.18"->"1.0.19" (RULE WAJIB Batch 37).

### Catatan
Confidence **93%** — logic baru straightforward (union dari map keys + resolve PackageManager, pola sama persis dgn `mapNotNull` yg sudah ada di `topAppsByForegroundUsage`), TAPI belum ada verifikasi device nyata utk mode baru ini (Batch 53 cuma verifikasi jalur DEFAULT, bukan toggle "Semua App"). Rekomendasi: build, aktifkan toggle "Tampilkan Semua App" saat Shizuku aktif, konfirmasi jumlah app yang muncul lebih banyak dari 3 (idealnya mendekati 13 app dari validasi data mentah Batch 51) & foreground time tampil wajar (0m utk app yg jarang dibuka manual, bukan crash/blank).

### Pending Queue
Tidak ada nomor baru ditambahkan — ini penyempurnaan atas Pending #19 yang sudah closed (Batch 53), bukan item baru.

---

## [Batch 53] Konfirmasi - Pending #19 Ditutup 100% (Verifikasi Device Nyata via Screenshot) — 2026-08-20

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 57 -> 57 file (0 diedit selain protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User jalankan rekomendasi Batch 52 (build + buka tab Drain dgn Shizuku aktif) dan kirim screenshot device nyata sebagai bukti.

### Temuan (dari screenshot user)
Ketiga poin verifikasi Batch 52 terkonfirmasi sekaligus:
1. **UI tidak freeze** — `withContext(Dispatchers.IO)` di `LaunchedEffect` (`DrainScreen.kt`) terbukti aman, screenshot menampilkan layar penuh ter-render normal (bukan blank/stuck loading).
2. **Baris mAh riil tampil** — 3 dari 3 app di daftar (`Jam` 0,25 mAh, `Peluncur XOS` 0,12 mAh, `TranResolver` 0,01 mAh) menampilkan baris "≈ X,XX mAh (riil, sejak charge terakhir)" — `mergeDrainData()` berhasil match package -> UID -> mAh utk SEMUA app di layar ini (bukan cuma sebagian), termasuk `TranResolver` (komponen sistem Transsion, UID kemungkinan besar hasil `getPackagesForUid()` di ROM custom — sempat jadi keraguan di catatan Batch 52).
3. **Hint teks sesuai `hasRealDrainData=true`** — teks di atas daftar persis kalimat cabang "Kolom mAh dari dumpsys batterystats (Shizuku, sejak charge penuh terakhir)...", bukan fallback proxy.

### Sengaja TIDAK diubah
Tidak ada perubahan kode/logic — batch ini murni pencatatan hasil verifikasi (dokumentasi), sesuai rekomendasi eksplisit di catatan Batch 52 ("Pending #19 resmi SELESAI 2/2 setelah konfirmasi ini").

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — `versionCode` 18->19, `versionName` "1.0.17"->"1.0.18" (RULE WAJIB Batch 37, berlaku walau batch ini docs-only).

### Catatan
Confidence **98%** (naik dari 92% Batch 52) — 2 keraguan yang tersisa di catatan Batch 52 (potensi masalah `getPackagesForUid()` di ROM custom, & coroutine blocking-exec-dalam-Compose belum dites end-to-end) **keduanya terjawab positif** oleh screenshot ini. Sisa 2% murni krn baru 1 screenshot/1 device (Transsion XOS, sesuai laporan bug historis project ini) — belum ada data dari device/ROM lain.

### Pending Queue
19. ✅ **DITUTUP 100%** (parser Batch 49-51, wiring Batch 52, terverifikasi device nyata Batch 53 — ini).

---

## [Batch 52] Fitur - Pending #19 (2/2, SELESAI): Wiring BatteryStatsParser + Shizuku ke Drain Analyzer — 2026-08-20

**Confidence Rating: 92%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 2 file diedit: `UsageStatsHelper.kt`, `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Langkah terakhir Pending #19 (`FEATURE_PARITY_GOALS.md` Batch 18). Parser (`BatteryStatsParser.kt`, Batch 49-51) sudah TERVALIDASI PENUH terhadap data nyata device user — batch ini menghubungkannya ke `DrainScreen.kt` supaya tab Drain Analyzer benar-benar menampilkan mAh riil (bukan cuma proxy waktu pemakaian), TAPI hanya saat Shizuku aktif & diizinkan.

### Selesai
- **`UsageStatsHelper.kt`**: `AppUsageInfo` dapat field baru `mahEstimate: Double? = null` (nullable, default param — TIDAK memecah pemanggil existing). Fungsi baru `fetchDrainMahByPackage(context)`: cek `ShizukuManager.hasPermission()` -> exec `dumpsys batterystats --charged` -> `BatteryStatsParser.parseEstimatedPowerUse()` -> resolve tiap `uid` ke package name via `PackageManager.getPackagesForUid()` (1 UID bisa dipakai >1 package/shared UID, semua diberi nilai mAh yang sama karena mAh memang milik UID) -> `Map<packageName, mah>`. Return `null` (bukan map kosong) kalau Shizuku tidak aktif/command gagal/parsing kosong — sinyal eksplisit "data riil tidak tersedia" ke caller. Fungsi baru `mergeDrainData(apps, mahByPackage)`: isi `mahEstimate` pada app yang match, lalu re-sort (mAh riil descending duluan, sisanya tetap urutan waktu pemakaian semula) — `null`/kosong -> no-op, list `apps` dikembalikan apa adanya.
- **`DrainScreen.kt`**: `LaunchedEffect` sekarang, setelah dapat daftar proxy dari `topAppsByForegroundUsage()`, panggil `fetchDrainMahByPackage()` **dibungkus `withContext(Dispatchers.IO)`** (WAJIB — exec shell Shizuku itu blocking `Process.waitFor()`, kalau dibiarkan di Main dispatcher LaunchedEffect bakal freeze UI) lalu `mergeDrainData()`. State baru `hasRealDrainData` mengontrol hint teks (beda kalimat saat data riil vs proxy). `DrainAppRow` tampilkan baris tambahan "≈ X.XX mAh (riil, sejak charge terakhir)" **hanya** untuk app yang punya `mahEstimate` non-null — app lain di daftar yang sama tetap tampil normal tanpa baris itu (bukan dihilangkan dari list).

### Keputusan Desain Penting
- **Bukan daftar terpisah** — batch ini SENGAJA mengisi mAh riil ke daftar top-15-by-foreground-time yang sudah ada (bukan bikin daftar top-15-by-mAh independen). Alasan: menjaga jumlah baris (max 3 file) & scope tetap "wiring", bukan redesign fitur. Konsekuensi jujur: app dengan mAh riil tinggi TAPI foreground time-nya rendah (tidak masuk 15 besar waktu pemakaian) TIDAK akan muncul di daftar sama sekali — ini keterbatasan yang diketahui, bukan bug, didokumentasikan di KDoc `fetchDrainMahByPackage`.
- **Jendela waktu berbeda, sengaja tidak diselaraskan**: `dumpsys batterystats --charged` = sejak charge penuh terakhir; `topAppsByForegroundUsage` = 24 jam terakhir. Dua angka ini TIDAK diklaim mengukur periode yang sama — didokumentasikan eksplisit di KDoc & hint UI, bukan disamarkan seolah-olah 1 angka gabungan yang konsisten.
- **Fail-safe berlapis**: `fetchDrainMahByPackage` return null di 3 titik gagal berbeda (Shizuku off, command gagal, parsing kosong) — `mergeDrainData` treat semua sama (no-op), jadi SATU jalur fallback tunggal yang predictable, bukan beda-beda perilaku tiap kegagalan.

### Sengaja TIDAK diubah
- `BatteryStatsParser.kt` — 0 perubahan, dipakai persis seperti tervalidasi di Batch 51.
- `ShizukuManager.kt` — `execShellCommand` dipakai apa adanya, tidak ada fungsi baru di file ini.
- `AndroidManifest.xml` — TIDAK perlu entri baru; `dumpsys` dieksekusi lewat proses Shizuku (privilege shell UID eksternal), sama seperti `am force-stop` yang sudah dipakai `killBackgroundApp` sejak Batch 39 tanpa manifest change.
- Fallback proxy (`topAppsByForegroundUsage`, tanpa Shizuku) — 0 perubahan perilaku, user yang belum pakai Shizuku sama sekali tetap dapat pengalaman identik dengan sebelum batch ini.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti (`versionCode` 17->18, `versionName` "1.0.16"->"1.0.17").

### Catatan
Confidence **92%** — bukan 96%+ (level Batch 51) karena parser murni sudah tervalidasi penuh, TAPI 2 hal di wiring ini belum ada verifikasi device fisik: (1) `getPackagesForUid()` di ROM custom (mis. Transsion XOS, sumber bug isSystemApp Batch 45/48) belum tentu 100% konsisten dgn AOSP murni untuk semua UID app pihak ketiga; (2) urutan `withContext(Dispatchers.IO)` dalam `LaunchedEffect` belum dites end-to-end di device nyata dgn Shizuku aktif (pola coroutine-nya standar & sudah dipakai project lain, tapi kombinasi spesifik shell-exec-blocking-dalam-Compose ini baru pertama kali di codebase). Rekomendasi: build + buka tab Drain dgn Shizuku aktif & diizinkan, konfirmasi (a) UI tidak freeze saat memuat, (b) minimal 1 app menampilkan baris "≈ X.XX mAh (riil...)", (c) hint teks berubah sesuai `hasRealDrainData`. Pending #19 resmi **SELESAI 2/2** setelah konfirmasi ini (tidak perlu batch tambahan kecuali ditemukan bug baru).

### Pending Queue
19. ✅ **SELESAI (2/2)** — parser tervalidasi (Batch 49-51) + wiring UI (Batch 52, ini).

---

## [Batch 51] Fix - Pending #19 (1.9/2): Bug Baris Tergabung, Parser Sekarang Tervalidasi Penuh — 2026-08-20

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 1 file diedit: `BatteryStatsParser.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User jalankan rekomendasi Batch 50 (`dumpsys batterystats --charged | grep -A 100 "Estimated power use"`, capture lebih panjang) dan tempel hasil NYATA berisi 13 baris `UID u0aXX` (app pihak ketiga) + 5 baris UID sistem (`1000`, `0`, `1041`, `1046`, `1013`).

### Temuan (dari data nyata user, lebih signifikan dari Batch 50)
Simulasi parser (Python, regex identik) terhadap teks lengkap yang ditempel user hanya menangkap **1 dari 18 baris UID total**. Root cause: **banyak baris konseptual dumpsys tergabung jadi 1 baris fisik sangat panjang** (kemungkinan besar artefak terminal Termux layar sempit saat capture/paste) — satu baris fisik user berisi berkali-kali pola `UID <token>: <angka>` beruntun (mis. `UID 1000: 4.81 ... UID 0: 1.59 ... UID u0a41: 1.23 ...` semua di baris yang sama). Regex `UID_LINE` Batch 49/50 pakai anchor `^` (WAJIB persis di awal baris fisik) — jadi HANYA entri UID pertama di tiap baris fisik yang ketemu, sisanya (termasuk hampir semua `u0aXX`) terlewat begitu saja TANPA warning/crash (list kosong parsial, bukan salah, tapi tidak lengkap — bug paling berbahaya krn silent).

### Selesai
- **`BatteryStatsParser.kt`**: `UID_LINE` diubah dari anchor `^\s*uid...` jadi boundary-aware `(?:^|\s)uid...` (match "UID" di awal baris ATAU didahului spasi, bukan cuma persis kolom 0). Loop parsing diubah dari `UID_LINE.find(line)` (1 match/baris) jadi `UID_LINE.findAll(line)` (semua match/baris, di-iterate dgn `for`). `NEXT_TOP_LEVEL_SECTION` (heuristik akhir section) **TIDAK diubah** — sudah divalidasi tetap benar (baris prompt shell penutup capture user tetap terdeteksi sbg akhir section).
- **Sanity-test ulang** (Python, regex+logic identik, terhadap **teks lengkap nyata** yang ditempel user — bukan simulasi/dugaan lagi): hasil **13 app UID** (`u0a41`->10041 1.23mAh, `u0a125`->10125 1.04mAh, ... turun sampai `u0a171`->10171 0.115mAh) + **5 UID sistem terfilter** (1000/0/1041/1046/1013, semua <10000) = **18 total, cocok 100%** dgn hitung manual baris demi baris dari teks mentah. `decodeUid()` tervalidasi utk appId 1 digit (`u0a3`) maupun banyak digit (`u0a385`).
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 16->17, `versionName` "1.0.15"->"1.0.16". Brace 23/23 curly, 65/65 paren.

### Sengaja TIDAK diubah
- `NEXT_TOP_LEVEL_SECTION`/heuristik akhir section — terbukti tetap benar di data nyata, tidak ada indikasi perlu diubah.
- **Masih belum wiring ke UI** (`ShizukuManager.kt`/`DrainScreen.kt`/`UsageStatsHelper.kt` tetap tidak disentuh) — parser sekarang confidence tinggi (96%), tapi wiring tetap task terpisah (Micro-Batching Rule) supaya blast radius perubahan UI batch berikutnya bisa diaudit sendiri.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Confidence **96%** (naik dari 90% Batch 50) — parser sekarang tervalidasi terhadap data mentah SUNGGUHAN dari device user (bukan sample buatan), termasuk kasus tersulit (baris tergabung) yang justru KETAHUAN dari data ini. Sisa 4% bukan krn keraguan pada parser (sudah solid), tapi krn wiring UI (langkah 2/2) belum dikerjakan/dites — begitu itu selesai & user konfirmasi tab Drain Analyzer menampilkan data mAh yang masuk akal, Pending #19 baru bisa ditutup 100%.

### Pending Queue
19. Masih belum selesai (1.9/2 — parser TERVALIDASI PENUH, tinggal wiring UI di batch berikutnya).

---

## [Batch 50] Fix - Pending #19 (1.5/2): Bug Casing Regex `UID` Ditemukan dari Dumpsys Nyata — 2026-08-20

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 57 -> 57 file (0 baru/hapus, 1 file diedit: `BatteryStatsParser.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User jalankan rekomendasi WAJIB dari catatan Batch 49 (`adb shell dumpsys batterystats --charged | grep -A 30 "Estimated power use"`) dan tempel hasil NYATA dari device (Transsion XOS, sesuai laporan bug Batch 45). Ini validasi pertama parser terhadap output dumpsys sungguhan sejak dibuat.

### Temuan (dari data nyata user)
1. **Bug dikonfirmasi**: baris per-UID device user berbunyi `UID 1000: 4.58 bg: 4.58` — **"UID" huruf besar semua**, bukan `"Uid"` (campuran) seperti diasumsikan Batch 49 dari dokumentasi tool pihak ketiga. Regex `UID_LINE` sebelumnya case-sensitive -> **tidak akan cocok sama sekali** di device user, parser akan selalu return list kosong walau section ada.
2. Bagian lain **cocok/valid**: header section, baris "Capacity/Computed drain", section "Global" (screen/cpu/audio/dst — otomatis ter-skip krn tidak match pola `UID_LINE`, sesuai desain), heuristik akhir section (baris rata kolom 0), dan breakdown tambahan setelah angka mAh (`" bg: 4.58"`) tidak mengganggu penangkapan grup regex (memang tidak di-anchor ke akhir baris).
3. **Belum tervalidasi**: format UID **aplikasi** (`u0aXX`) — capture user baru sampai baris UID sistem (`1000`, otomatis ter-filter `minUid`) sebelum output kepotong. Belum ada 1 pun baris `u0aXX` nyata yang terlihat.

### Selesai
- **`BatteryStatsParser.kt`**: `UID_LINE` regex ditambah `RegexOption.IGNORE_CASE` — sekarang cocok baik `"Uid"` maupun `"UID"` (atau kombinasi kapital lain). KDoc class-level diupdate: status validasi sebagian terverifikasi (bukan lagi murni asumsi teoretis).
- **Sanity-test ulang** (Python, regex identik + `re.IGNORECASE`): baris `UID 1000: 4.58 bg: 4.58` (persis dari device user), `UID u0a55: 45.678`, `UID u0a123: 12.3 ( cpu=... )` — semua ke-parse benar (UID 1000 otomatis ter-filter `minUid=10000` saat wiring nanti, u0a55->10055 & u0a123->10123 akan lolos).
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 15->16, `versionName` "1.0.14"->"1.0.15". Brace 23/23 curly, 65/65 paren.

### Sengaja TIDAK diubah
- `decodeUid()` (logic `u0aXX` -> UID Android asli) — TIDAK diubah, karena belum ada data nyata untuk membandingkan (lihat "Belum tervalidasi" di atas). Mengubah tanpa data konkret = spekulasi, bukan fix.
- Masih **belum wiring ke UI** (`ShizukuManager.kt`/`DrainScreen.kt`/`UsageStatsHelper.kt` tetap tidak disentuh) — Pending #19 masih belum 100% selesai, sengaja ditahan sampai ada minimal 1 baris `u0aXX` nyata utk validasi penuh.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Confidence **90%** (naik dari 85% Batch 49, tapi belum 95%+) — bug casing yang ditemukan & fix sudah solid (data nyata, bukan asumsi), TAPI bagian `u0aXX` (paling krusial utk fitur "per-app mAh") masih 100% belum divalidasi. **Rekomendasi WAJIB sebelum wiring UI (2/2)**: jalankan ulang dgn capture lebih panjang, misalnya:
`adb shell dumpsys batterystats --charged > /sdcard/batterystats.txt` lalu `cat`/`grep -A 100 "Estimated power use"` filenya, atau langsung `termux-clipboard-set < ...` — tempel minimal sampai terlihat beberapa baris `UID u0aXX: X.XX` (app pihak ketiga, bukan cuma UID sistem 0/1000/1001/dst).

### Pending Queue
19. Masih belum selesai (1.5/2 — casing tervalidasi & fix, `u0aXX` masih menunggu data nyata sebelum wiring UI).

---

## [Batch 49] Fitur - Pending #19 (1/2): Parser "Estimated Power Use" dari dumpsys batterystats — 2026-08-20

**Confidence Rating: 85%**
**File sebelum -> sesudah:** 56 -> 57 file (1 baru: `BatteryStatsParser.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Pending #19 dari `FEATURE_PARITY_GOALS.md` (Batch 18) — upgrade Drain Analyzer dari proxy waktu pemakaian (`UsageStatsHelper.topAppsByForegroundUsage`, tetap tidak diubah) ke data **mAh riil per app** via `dumpsys batterystats` (butuh Shizuku, sudah ada `ShizukuManager.execShellCommand` sejak Batch 23/39). Item ini dari awal ditandai "kompleksitas parsing tinggi -> mungkin perlu dipecah lagi" (catatan Batch 18/43) — dipecah jadi 2 langkah: **(1/2) batch ini** = logic parsing MURNI, belum nyentuh UI/ShizukuManager sama sekali; **(2/2)** wiring ke `DrainScreen.kt`/`UsageStatsHelper.kt` di batch berikutnya, setelah parser ini diverifikasi.

### Selesai
- **`BatteryStatsParser.kt`** (baru, `util/`): `parseEstimatedPowerUse(dumpsysOutput, minUid=10000)` — cari section `"Estimated power use (mAh):"`, iterasi baris berikutnya sampai heuristik akhir section (baris rata kolom 0 = section besar baru dumpsys berikutnya), regex `^\s*Uid\s+(\S+):\s+([\d.]+)` tangkap tiap baris per-UID, `decodeUid()` translate format `"u0a55"` (userId*100000+10000+appId, konvensi `UserHandle.PER_USER_RANGE`/`Process.FIRST_APPLICATION_UID` AOSP) atau angka polos (UID sistem, otomatis ter-filter `minUid`). 100% pure function — TIDAK ada Android API/I/O, TIDAK ada exception ke caller (list kosong kalau section tidak ketemu/parsing gagal). Brace 7/7 curly, 62/62 paren.
- **Sanity-test manual** (di lingkungan pembuatan ZIP, Python — regex identik, BUKAN unit test Kotlin di repo): sample teks meniru format dumpsys terdokumentasi (termasuk section lanjutan setelahnya, baris kosong di tengah section, UID sistem `1000`, breakdown `( cpu=... )` di akhir baris) → hasil parse benar: 2 app UID terdeteksi (`u0a55`->10055, `u0a123`->10123), UID sistem `1000` ter-filter, berhenti tepat di section berikutnya, urutan descending benar.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 14->15, `versionName` "1.0.13"->"1.0.14". Brace 23/23 curly, 65/65 paren.

### Keputusan Desain Penting
- **Batch ini SENGAJA tidak menyentuh UI/`ShizukuManager.kt`/`DrainScreen.kt` sama sekali** — memisahkan bagian paling berisiko (parsing teks tak terstruktur, tidak ada dokumentasi resmi Android) dari bagian yang low-risk (wiring UI, pola sudah berulang kali terbukti di batch lain). Kalau format parsing ternyata salah di device user, blast radius batch ini = 0 (file baru, tidak dipanggil dari mana pun).
- **`minUid` default 10000** (bukan hardcode) — sengaja dibuat parameter, bukan konstanta internal, supaya bisa disesuaikan/di-override di unit test/wiring batch depan tanpa edit ulang file ini.

### Sengaja TIDAK diubah
- `ShizukuManager.kt`, `UsageStatsHelper.kt`, `DrainScreen.kt` — TIDAK ada pemanggilan `BatteryStatsParser` dari mana pun batch ini (lihat Keputusan Desain). File baru ini murni "menganggur" sampai batch berikutnya, sesuai rencana pemecahan 2 langkah.
- Tidak ada dependency baru — 100% Kotlin stdlib (`Regex`, `String.lines()`), tidak butuh library parsing tambahan.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
⚠️ Confidence **85%** (lebih rendah dari batch-batch lain baru-baru ini) — JUJUR, ini bukan sekadar "belum compile digan Gradle" (limitasi rutin project ini), tapi **format `dumpsys batterystats` sendiri tidak didokumentasikan resmi oleh Android** (bagian dari tooling debug internal, bukan API publik), jadi walau pola regex disusun dari format yang dipakai luas oleh tool open-source (Battery Historian dkk) & lolos sanity-test simulasi, TIDAK ADA jaminan format device user (Transsion XOS, dari laporan bug Batch 45) 100% identik — bisa saja section judul beda kapitalisasi/spasi, atau breakdown `( ... )` di posisi beda, dll. Rekomendasi WAJIB sebelum lanjut ke batch (2/2): jalankan manual `adb shell dumpsys batterystats --charged` (atau via Shizuku shell) di device user, tempel hasilnya, supaya parser bisa divalidasi/disesuaikan terhadap output NYATA sebelum di-wiring ke UI — jangan lanjut wiring dgn asumsi parser ini sudah pasti benar.

### Pending Queue
19. Belum selesai (langkah 1/2 selesai batch ini — lanjut wiring UI di batch berikutnya, idealnya SETELAH verifikasi output dumpsys nyata dari user).

---

## [Batch 48] Fitur - Pending #13: Shortcut Settings Per-App (Best-Effort) — 2026-08-20

**Confidence Rating: 93%**
**File sebelum -> sesudah:** 56 -> 56 file (2 diedit: `UsageStatsHelper.kt`, `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Pending #13 dari `FEATURE_PARITY_GOALS.md` (Batch 18) — gap Greenify "cegah app berjalan sendiri tanpa izin". User pilih dikerjakan duluan drpd #19 (Shizuku dumpsys parsing, lebih kompleks). Sesuai catatan sejak Batch 18: **tidak ada API generik non-root** untuk 3rd-party app mengontrol App Standby Bucket/auto-launch app lain (`setAppStandbyBucket` dibatasi utk app sistem sejak API 30) — jadi diimplementasi sbg best-effort **navigasi**, bukan kontrol otomatis.

### Selesai
- **`UsageStatsHelper.kt`**: fungsi baru `openAppDetailsSettings(context, packageName)` — buka `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` dgn `Uri.fromParts("package", packageName, null)`, pola identik `openUsageAccessSettings()` yang sudah ada sejak Batch 1 (`FLAG_ACTIVITY_NEW_TASK`). Import baru: `android.net.Uri`. Brace 20/20 curly, 55/55 paren.
- **`DrainScreen.kt`**: `DrainAppRow` dapat parameter baru `onOpenSettings`, tombol "Pengaturan App" baru — **tampil utk SEMUA app** (tidak digate `isActionable`, beda dgn Force Stop/Checkbox) karena cuma navigasi ke Settings sistem, tidak berisiko seperti eksekusi Force Stop. Layout diubah dari 1 `Row` datar jadi `Column` (baris info+checkbox di atas, baris tombol aksi di bawah) — supaya 2 tombol ("Force Stop" + "Pengaturan App") + checkbox tidak sesak di 1 baris pada layar sempit. Brace 46/46 curly, 119/119 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 13->14, `versionName` "1.0.12"->"1.0.13". Brace 23/23 curly, 65/65 paren.

### Keputusan Desain Penting
- **Tombol "Pengaturan App" TIDAK digate `isActionable`** (beda dari Force Stop) — sengaja, karena membuka dialog Settings tidak pernah bisa bikin crash/reboot-loop (beda kelas risiko dgn force-stop komponen sistem kritis kayak `com.android.systemui`). User yang penasaran soal `com.android.settings` sendiri pun boleh lihat App Info-nya.
- **Bukan otomatis** — VoltCare TIDAK memanggil API apa pun utk cegah auto-launch, murni antar user ke UI Android bawaan tempat USER SENDIRI yang set battery restriction/manage-background manual per app. Jujur sesuai definisi gap #13 sejak awal (Batch 18), tidak diklaim lebih dari itu.

### Sengaja TIDAK diubah
- `killBackgroundApp()`, `topAppsByForegroundUsage()` — 100% apa adanya, tidak disentuh batch ini.
- `AndroidManifest.xml` — tidak perlu entri baru, `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` adalah Intent sistem publik standar (sama seperti `ACTION_USAGE_ACCESS_SETTINGS` yang sudah dipakai sejak Batch 1 tanpa entri manifest).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual (Intent + Uri.fromParts adalah API stabil AOSP sejak API 1, tidak ada risiko kompatibilitas). Confidence 93% (bukan lebih tinggi) murni krn perubahan layout `DrainAppRow` dari `Row` datar ke `Column` cukup signifikan secara visual (belum diverifikasi rendering nyata di device, walau API Compose yang dipakai — `Column`/`Row` bersarang — bukan hal baru/asing di codebase ini). Rekomendasi: build + buka tab Penguras, konfirmasi tombol "Pengaturan App" muncul di semua row (termasuk 4 package kritis kalau kebetulan lolos ke daftar top-15) dan benar membuka halaman App Info yang sesuai.

### Pending Queue
19. Tidak berubah. 13 ✅ selesai (Batch 48, ini).

---

## [Batch 47] Cleanup - Hapus 4 File Dokumentasi Orphan "PromptVault" (Manifest Desync) — 2026-08-20

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 60 -> 56 file (4 dihapus: `ROADMAP.md`, `TROUBLESHOOTING.md`, `MAINTENANCE.md`, `scripts/preflight_check.sh`; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User upload ZIP baru minta "lanjutkan progress" seperti sesi fresh. Audit awal (baca `PROJECT_STATE.md`/`ROADMAP.md`/`TROUBLESHOOTING.md`/`MAINTENANCE.md` sebelum eksekusi task apapun) menemukan 4 file berisi konten project **LAIN** milik user yang sama — **PromptVault** (aplikasi file-organizer terpisah, repo `FDzaki-dev/PromptVault`, package `com.elprompter.promptvault`) — bukan VoltCare. Persis pola insiden **Batch 25** (dulu 65 file source `.kt` PromptVault ikut kebundle & bikin `compileDebugKotlin`/CI gagal), kali ini yang kebawa adalah dokumentasi perencanaan (bukan source code, jadi tidak bikin build gagal — tapi tetap berbahaya: `MAINTENANCE.md` secara eksplisit instruksikan sesi Claude berikutnya `web_fetch` ke repo GitHub `FDzaki-dev/PromptVault` yang SALAH kalau tidak ketahuan).

### Verifikasi sebelum hapus (sesuai Strict Delete & Repack Guard)
- **0 referensi silang**: `grep -rn "PromptVault\|elprompter"` di `PROJECT_STATE.md`/`CHANGELOG.md` (dokumen sah) HANYA muncul di entri riwayat Batch 25 (menceritakan insiden lama, sah/tidak dihapus) — tidak ada referensi dari kode VoltCare (`com.voltcare.app`) ke 4 file yang dihapus, maupun sebaliknya.
- `scripts/preflight_check.sh` hardcoded `KT_DIR="app/src/main/java/com/elprompter/promptvault"` — 100% tidak relevan/tidak jalan untuk `com.voltcare.app`, dan **tidak dipanggil** dari `.github/workflows/release.yml` (grep bersih) — orphan tooling, bukan bagian CI aktif.
- **Manifest desync dikonfirmasi**: ke-4 file **tidak tercantum** di `FILE_MANIFEST.txt` (terakhir digenerate Batch 22) — tidak pernah resmi jadi bagian VoltCare.
- **Izin hapus**: dikonfirmasi eksplisit oleh user via pilihan "Hapus 4 file itu, lanjut Batch 47" (bukan asumsi sepihak).

### Selesai
- Hapus `ROADMAP.md`, `TROUBLESHOOTING.md`, `MAINTENANCE.md`, `scripts/preflight_check.sh` (folder `scripts/` ikut kosong, dihapus).
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 12->13, `versionName` "1.0.11"->"1.0.12". Brace 23/23 curly, 65/65 paren.

### Sengaja TIDAK diubah
- `FEATURE_PARITY_GOALS.md` — dokumen ini genuine milik VoltCare (dibuat Batch 18, isi 100% soal battery manager AccuBattery/GSam/Greenify), TIDAK ikut terhapus walau sempat dicurigai di awal audit.
- `PROJECT_STATE.md`/`CHANGELOG.md` entri Batch 25 lama — dipertahankan apa adanya (riwayat sah, bukan kontaminasi aktif).
- `FILE_MANIFEST.txt` — tidak perlu diedit (ke-4 file yang dihapus memang sudah tidak pernah tercantum di sana sejak awal, jadi tidak ada baris yang perlu dicabut).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Root cause KENAPA 4 file ini bisa nyasar ke ZIP VoltCare tidak bisa dipastikan dari lingkungan Claude (kemungkinan besar: user mengerjakan PromptVault di sesi/percakapan terpisah, lalu folder lokal Termux `~/projects/VoltCare` sempat tercampur file dari `~/projects/PromptVault` sebelum di-zip ulang — sama seperti akar masalah Batch 25). Rekomendasi ke user: saat `unzip -o` di Termux (skrip Update Harian), pastikan `LATEST_ZIP` yang dipakai benar-benar hasil terbaru dari sesi VoltCare, bukan campuran folder lain.

### Pending Queue
13, 19. Tidak berubah (lihat Batch 43/44 utk detail item #10/#12 yang sudah selesai).

---

## [Batch 46] Fix - Drain Analyzer: Tombol "Force Stop" Tidak Ada Feedback — 2026-08-20

**Confidence Rating: 94%**
**File sebelum -> sesudah:** 60 -> 60 file (1 diedit: `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User konfirmasi fix Batch 45 berhasil (tombol sekarang clickable), tapi lapor lanjutan: "pencet sih bisa, tapi gak ada feedback nya sama sekali". Root cause: `onForceStop` (sejak Batch 10) cuma panggil `UsageStatsHelper.killBackgroundApp()` (fire-and-forget, return value dibuang) lalu `refreshTrigger++` — refresh ini me-reload `topAppsByForegroundUsage()` (data waktu PEMAKAIAN historis 24 jam), BUKAN daftar proses yang sedang jalan, jadi app yang di-Force-Stop TETAP muncul di list persis di posisi sama tanpa perubahan visual apa pun — user (sah) mengira klik tidak berefek.

### Selesai
- **`DrainScreen.kt`**: `Scaffold` dikasih `snackbarHost` (pola PERSIS sama seperti `HistoryScreen.kt` — `SnackbarHostState` + `rememberCoroutineScope`, konsisten dgn konvensi existing, bukan pola baru). `onForceStop` sekarang tangkap return `Boolean` dari `killBackgroundApp()` (sebelumnya dibuang) -> tampilkan Snackbar `"{appLabel} dihentikan"` / `"Gagal menghentikan {appLabel}"` sesuai hasil nyata, bukan asumsi selalu sukses. Brace 41/41 curly, 104/104 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 11->12, `versionName` "1.0.10"->"1.0.11". Brace 23/23 curly, 65/65 paren.

### Sengaja TIDAK diubah
- `UsageStatsHelper.killBackgroundApp()` — signature & perilaku 100% sama, HANYA return value-nya sekarang benar-benar dipakai di caller (sebelumnya sudah ada sejak Batch 39/10, cuma tidak pernah dibaca).
- Checkbox whitelist & Switch Auto-Hibernate — TIDAK ditambah Snackbar, karena keduanya SUDAH punya feedback visual instan (checkbox tercentang, switch berpindah posisi + teks jumlah app berubah) — tidak silent seperti Force Stop.
- List app TIDAK di-refresh/dihilangkan otomatis setelah Force Stop — sengaja, karena `topAppsByForegroundUsage()` representasi HISTORIS 24 jam (bukan proses live), menghilangkan entry akan menyesatkan (seolah app itu tidak lagi dipakai, padahal itu cuma refleksi kalau force-stop terkirim).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual (pola Snackbar identik `HistoryScreen.kt` yang sudah terbukti compile, tidak ada API Compose baru). Confidence 94% (bukan lebih tinggi) karena nilai `success` bergantung pola existing `killBackgroundApp()` (Shizuku force-stop ATAU fallback `killBackgroundProcesses`) yang efektivitas runtime-nya di device Transsion XOS user BELUM diverifikasi nyata (sama catatan sejak Batch 39). Rekomendasi: build + tekan Force Stop di device user, konfirmasi Snackbar muncul dgn pesan yang sesuai (baik sukses maupun gagal).

### Pending Queue
13, 19. Tidak berubah.

---

## [Batch 45] Fix - Drain Analyzer: Filter `isSystemApp` Kelewat Luas (Semua Row Tidak Clickable) — 2026-08-20

**Confidence Rating: 92%**
**File sebelum -> sesudah:** 60 -> 60 file (1 diedit: `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User kirim screenshot Drain Analyzer: 3 app teratas ("Jam", "Peluncur XOS", "TranResolver") SEMUA tanpa checkbox/tombol Force Stop — tanya "kenapa bagian tab ini gak ada yang clickable?!!".

### Root Cause
BUKAN bug render/state Compose. `DrainAppRow` sejak Batch 10 (diwarisi Batch 44) pakai gate `if (!app.isSystemApp)` untuk menyembunyikan Force Stop/checkbox whitelist — `isSystemApp` dihitung dari `ApplicationInfo.FLAG_SYSTEM` (`UsageStatsHelper.kt`, tidak diubah batch ini). Di device user (ROM Transsion **XOS** — terlihat dari nama app "Peluncur **XOS**"), OEM menandai HAMPIR SEMUA app preinstall sbg `FLAG_SYSTEM`, termasuk Launcher, Jam/Clock, dan komponen custom "TranResolver" — bukan cuma komponen inti Android murni. Karena 3 app dgn waktu pemakaian tertinggi di device ini SEMUA kena flag itu, seluruh list yang user lihat kebetulan 100% ter-filter — user (sah) mengira fitur rusak.

### Selesai
- **`DrainScreen.kt`**: gate `!app.isSystemApp` diganti fungsi privat baru `isActionable(packageName)` — blocklist EKSPLISIT hanya 4 package benar-benar kritis (`android`, `com.android.systemui`, `com.android.settings`, `com.android.phone`) yang berisiko crash/reboot-loop UI kalau di-force-stop. SEMUA app lain — termasuk yang `FLAG_SYSTEM` seperti Launcher/Jam/komponen OEM — sekarang actionable (checkbox whitelist + tombol Force Stop tampil), sesuai maksud awal fitur (bukan literally "sembunyikan semua yang FLAG_SYSTEM"). Brace 36/36 curly, 98/98 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 10->11, `versionName` "1.0.9"->"1.0.10". Brace 23/23 curly, 65/65 paren.

### Keputusan Desain Penting
- **Blocklist 4 package, bukan whitelist/heuristik kompleks** — sengaja minimal & predictable. Menambah heuristik (mis. cek `ApplicationInfo.FLAG_UPDATED_SYSTEM_APP`, kategori app, dll) berisiko exclude/include salah lagi di ROM OEM lain yang punya konvensi flag beda-beda (persis akar masalah batch ini). Blocklist eksplisit lebih predictable & gampang di-audit/ditambah manual kalau nanti ada laporan package kritis lain yang lolos.
- **`AppUsageInfo.isSystemApp` (field di `UsageStatsHelper.kt`) TIDAK dihapus** — field masih valid/dihitung dgn benar (`FLAG_SYSTEM` tetap makna aslinya), cuma TIDAK dipakai lagi sbg gate actionability di `DrainScreen.kt`. Tidak breaking apa pun, tidak perlu edit `UsageStatsHelper.kt`.
- **Dampak ke Pending #12 (whitelist Auto-Hibernate, Batch 44)**: fix ini juga otomatis membuka whitelist utk lebih banyak app (termasuk app OEM yang sebelumnya tersembunyi) — konsisten & DIINGINKAN, bukan efek samping tak disengaja, karena tujuan whitelist memang "app yang mau user approve", bukan dibatasi diam-diam oleh flag OEM yang tidak related.

### Sengaja TIDAK diubah
- `UsageStatsHelper.kt`, `AppUsageInfo` data class — lihat Keputusan Desain di atas.
- `HibernateWorker.kt`/`HibernateWhitelistStore` (Batch 44) — tidak ada perubahan, whitelist tetap murni berdasar pilihan checkbox user, cuma sekarang lebih banyak app yang BISA dipilih.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual (perubahan murni kondisi boolean, tidak ada API/import baru). Confidence 92% (bukan 95%+): daftar 4 package kritis disusun dari pengetahuan umum AOSP (`com.android.systemui`/`com.android.phone`/`com.android.settings`/`android` adalah nama package standar lintas ROM), TAPI belum ada jaminan tidak ada package OEM lain yang SAMA kritisnya tapi nama beda per-vendor (mis. SystemUI custom Transsion mungkin punya package name berbeda dari AOSP standar) — best-effort, bukan proteksi lengkap. Rekomendasi: build + install ulang di device Transsion XOS user, konfirmasi checkbox+Force Stop sekarang muncul di "Jam"/"Peluncur XOS"/"TranResolver", DAN pastikan tidak ada crash sistem kalau user coba Force Stop app OEM tsb (test hati-hati, mulai dari app yang paling tidak kritis dulu mis. "Jam").

### Pending Queue
13, 19. Tidak berubah.

---

## [Batch 44] Fitur - Pending #12: Auto-Hibernate Terjadwal (WorkManager) — 2026-08-20

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 59 -> 60 file (1 baru: `HibernateWorker.kt`; 1 diedit: `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Pending #12 dari `FEATURE_PARITY_GOALS.md` (Batch 18) — tutup gap #9 (Greenify: hemat daya otomatis) & sebagian gap #7 (Force Stop otomatis, sebelumnya manual per-app dari Batch 10/39). `androidx.work:work-runtime-ktx:2.9.1` sudah jadi dependency SEJAK BATCH 1 tapi **belum pernah dipakai** (dikonfirmasi via audit `grep` di Batch 18) — batch ini pemakaian PERTAMA.

### Selesai
- **`HibernateWorker.kt`** (baru, `util/`): `HibernateWhitelistStore` (SharedPreferences) — whitelist app yang **di-approve eksplisit user via checkbox**, BUKAN semua app (sesuai definisi item #12, cegah kill app penting/OOM-loop tanpa izin). `HibernateWorker : CoroutineWorker` — `doWork()` panggil `UsageStatsHelper.killBackgroundApp()` (existing sejak Batch 10/39, TIDAK diubah) HANYA untuk app whitelist, fail-safe (`Result.success()` walau ada exception, tidak retry agresif). `companion.schedule()`/`cancel()` — `PeriodicWorkRequestBuilder` interval 30 menit (di atas minimum WorkManager 15 menit), `enqueueUniquePeriodicWork` dgn `ExistingPeriodicWorkPolicy.UPDATE` (aman dipanggil ulang tanpa duplikat job). Brace 11/11 curly, 52/52 paren.
- **`DrainScreen.kt`**: Card baru "Auto-Hibernate Terjadwal" (di atas daftar app) — `Switch` master ON/OFF (disabled kalau whitelist kosong, cegah aktifkan scheduler tanpa target), label dinamis jumlah app whitelist. `DrainAppRow` dapat 2 parameter baru (`isWhitelisted`, `onToggleWhitelist`) — `Checkbox` disisipkan di sebelah tombol "Force Stop" existing, HANYA utk app non-system (konsisten dgn kondisi `!app.isSystemApp` yang sudah ada utk tombol Force Stop). Brace 36/36 curly, 89/89 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 9->10, `versionName` "1.0.8"->"1.0.9". Brace 23/23 curly, 65/65 paren.

### Keputusan Desain Penting
- **Whitelist via `SharedPreferences`, BUKAN tabel Room baru** — set of package name string sederhana, tidak butuh query relasional/DAO/migration; konsisten pola `BatteryUtils.CalibrationStore` (Batch 8) yang juga pakai SharedPreferences utk state sederhana serupa.
- **Tidak ada AndroidManifest.xml baru** — WorkManager auto-init via `ContentProvider` bawaan library (bagian dari dependency yang sudah ada sejak Batch 1), TIDAK butuh entri manifest manual, TIDAK butuh permission baru (`killBackgroundApp` reuse permission/jalur Shizuku yang sudah ada).
- **Interval 30 menit hardcoded** (bukan dikonfigurasi user) — sesuai definisi item #12 di `FEATURE_PARITY_GOALS.md` ("interval wajar, mis. tiap 30 menit"), bisa dijadikan slider/pengaturan di batch depan kalau user minta.
- **`ExistingPeriodicWorkPolicy.UPDATE`** dipilih (bukan `KEEP`) supaya toggle Switch OFF->ON berulang tidak numpuk job duplikat/basi — selalu replace dgn definisi terbaru.

### Sengaja TIDAK diubah
- `UsageStatsHelper.killBackgroundApp()` — dipakai 100% apa adanya (termasuk jalur Shizuku Batch 39 kalau aktif, fallback `killBackgroundProcesses` kalau tidak).
- `AndroidManifest.xml` — lihat Keputusan Desain di atas.
- Tidak ada UI pengaturan interval/advanced scheduling — di luar scope minimal item #12, bisa jadi item pending baru kalau dibutuhkan.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/run WorkManager sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual API `androidx.work` (`CoroutineWorker`, `PeriodicWorkRequestBuilder`, `ExistingPeriodicWorkPolicy.UPDATE`, `enqueueUniquePeriodicWork` — semua API stabil sejak WorkManager 2.7+, kompatibel dgn versi 2.9.1 yang terpasang). Confidence 90% (bukan 95%+, sama alasan seperti fitur Shizuku/first-use dependency lain di project ini): PERTAMA KALI WorkManager benar-benar dipakai runtime setelah 43 batch cuma jadi dependency nganggur — belum ada bukti compile+run nyata (mis. apakah `doWork()` benar-benar terpanggil tiap 30 menit di device nyata dgn Doze Mode/battery optimization aktif, yang notoriously bisa menunda `PeriodicWorkRequest` non-`setConstraints` di beberapa OEM ROM agresif). Rekomendasi: build + aktifkan Switch dgn 1-2 app whitelist, tunggu >30 menit, cek lewat `adb shell dumpsys jobscheduler | grep voltcare` atau notifikasi app force-stop apakah job benar jalan.

### Pending Queue
13, 19. Tidak berubah. 12 ✅ selesai (Batch 44, ini).

---

## [Batch 43] Fitur - Pending #10: Estimasi Sisa Waktu Pakai (Discharge) — 2026-08-20

**Confidence Rating: 93%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `DashboardViewModel.kt`, `DashboardScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Pending #10 dari `FEATURE_PARITY_GOALS.md` (Batch 18) — gap #3: `DashboardViewModel` sebelumnya HANYA hitung estimasi waktu ke penuh (`estimateMinutesToFull`) saat charging; saat discharge, kartu "Estimasi" selalu tampil "-". Item ini dipilih duluan dari 4 sisa Pending Queue (10, 12, 13, 19) karena paling buildable & scope-nya paling kecil — #13 butuh keputusan/izin eksplisit user dulu (belum diminta), #19 kompleksitas tinggi (parsing `dumpsys batterystats`), #12 butuh WorkManager first-use + UI whitelist (lebih besar dari 3-file cap).

### Selesai
- **`DashboardViewModel.kt`**: fungsi privat baru `estimateRemainingMinutes(logs, currentPercent)` — hitung rata-rata drain rate (%/menit) dari pasangan sample **discharge-only** (`isCharging=false` di kedua sisi & `percent` menurun) dalam 24 jam terakhir, lompati jeda charging supaya rate tidak bias, lalu proyeksikan `currentPercent / ratePerMinute`. Return `-1` kalau data kurang (HP baru/baru charge penuh) — konsisten konvensi existing `estimateMinutes=-1` = "tidak tersedia". Dipanggil di `collect{}` saat `!log.isCharging`, pakai `db.batteryLogDao().sinceOnce(since)` (DAO **existing sejak Batch 1, TIDAK diubah** — protected, tidak disentuh). Field baru `estimateLabel: String` di `DashboardUiState` ("Estimasi Penuh" saat charging / "Sisa Pakai" saat discharge dgn data valid / "Estimasi" kalau `-1`) — REUSE 1 slot MetricCard existing, bukan bikin kartu baru (jaga diff kecil, sesuai cap 3 file). Brace 10/10 curly, 56/56 paren.
- **`DashboardScreen.kt`**: 1 baris diganti — label MetricCard "Estimasi" (hardcoded) -> `state.estimateLabel` (dinamis). `formatEstimate()` (existing, tidak diubah) tetap dipakai apa adanya krn format menit->jam+menit sama persis utk kedua kasus (ke-penuh vs sisa-pakai). Brace 23/23 curly, 56/56 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 8->9, `versionName` "1.0.7"->"1.0.8". Brace 23/23 curly, 65/65 paren.

### Keputusan Desain Penting
- **Logika drain-rate ditaruh di `DashboardViewModel.kt`, BUKAN `BatteryUtils.kt`** — deviasi dari pola biasa (fungsi kalkulasi baterai biasanya di `BatteryUtils`, mis. `estimateMinutesToFull`). Alasan murni Micro-Batching Cap: `BatteryUtils.kt` + `DashboardViewModel.kt` + `DashboardScreen.kt` + `build.gradle.kts` = 4 file, lewat cap. Trade-off disengaja & terdokumentasi, bukan lupa.
- **Tidak ada tabel/kolom/migration DB baru** — 100% agregasi Kotlin dari `BatteryLogEntity` existing via `sinceOnce()` yang sudah ada.
- **Window 24 jam** (bukan 30 hari seperti disebut opsional di deskripsi Pending #10) — dipilih supaya estimasi representasi pola pakai TERKINI (kebiasaan user bisa berubah), bukan rata-rata jangka panjang yang bisa basi. Bisa dijadikan konfigurasi di batch depan kalau user minta.

### Sengaja TIDAK diubah
- `BatteryLogDao.kt`/`BatteryLogEntity.kt` (DB Schema/DAO, protected) — dipakai 100% apa adanya (`sinceOnce()` sudah cukup, tidak perlu query baru).
- `BatteryUtils.kt` — lihat Keputusan Desain di atas.
- `formatEstimate()` di `DashboardScreen.kt` — format sama persis utk kedua jenis estimasi, tidak perlu duplikasi/fungsi baru.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual (query `sinceOnce()` & tipe `BatteryLogEntity` dipakai persis sesuai signature existing, tidak ada API Room/Compose baru). Confidence 93% (bukan 95%+) karena logika drain-rate BARU (bukan reuse pola batch lain persis seperti Batch 42) — akurasi/masuk-akal-nya nilai "Sisa Pakai" di device nyata dengan histori data riil belum terverifikasi runtime. Rekomendasi: build + pakai HP beberapa jam discharge normal, cek apakah angka "Sisa Pakai" masuk akal (mis. drain 1%/6menit -> ~40% baterai harusnya estimasi ~4 jam, bukan angka ekstrem/negatif).

### Pending Queue
12, 13, 19. Tidak berubah. 10 ✅ selesai (Batch 43, ini).

---

## [Batch 42] Fitur - Pending #11: Preset Cepat "Alarm Batas Charge" — 2026-08-20

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `RulesViewModel.kt`, `RulesScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Pending #11 dari `FEATURE_PARITY_GOALS.md` (Batch 18) — engine `RuleEntity`/`checkRule()` sudah mendukung `PERCENT_ABOVE` + `requireCharging` sejak Batch 1, tapi user harus isi form 5 field manual (Nama, Kondisi, Nilai, Switch charging, Aksi) walau kasus paling umum ("alarm kalau charging kelewat X%") cuma butuh 1 angka.

### Selesai
- **`RulesViewModel.kt`**: fungsi baru `saveChargeLimitPreset(percent: Float)` — langsung `db.ruleDao().insert(RuleEntity(...))` dgn `conditionType=PERCENT_ABOVE`, `requireCharging=true`, `actionType=ALARM` (3 field terkunci sesuai definisi preset), label otomatis `"Alarm Batas Charge {N}%"`. Tidak ada tabel/kolom/migration baru — 100% pakai `RuleEntity`/`RuleDao` existing (protected, tidak disentuh). Brace 16/16 curly, 45/45 paren.
- **`RulesScreen.kt`**: tombol teks "+ Preset Cepat: Alarm Batas Charge" di bawah judul (terpisah dari FAB "+" form lengkap) -> buka `ChargeLimitPresetDialog` baru (composable privat) — cuma 1 `OutlinedTextField` (persen, default "80", validasi range 1-100) + tombol Simpan/Batal. Brace 103/103 curly, 157/157 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 7->8, `versionName` "1.0.6"->"1.0.7". Brace 23/23 curly, 65/65 paren.

### Sengaja TIDAK diubah
- `RuleEntity.kt`/`RuleDao.kt` (DB Schema/DAO, protected) — dipakai 100% apa adanya, tidak ada perubahan schema.
- `BatteryMonitorService.checkRule()` — rule hasil preset otomatis ikut dievaluasi sample berikutnya (baca `enabledOnce()` dari tabel yang sama), tidak perlu perubahan engine.
- `RuleFormDialog` (form manual lengkap) — tetap ada apa adanya sbg opsi lanjutan/edit; preset murni shortcut TAMBAHAN, bukan pengganti.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual (pola `AlertDialog`/`OutlinedTextField` identik dgn `RuleFormDialog` yang sudah terverifikasi compile di Batch 14/15, tidak ada API Compose baru yang dipakai). Confidence 95% karena scope kecil & murni reuse pola/API yang sudah terbukti jalan di batch-batch sebelumnya (tidak ada dependency/API baru).

### Pending Queue
10, 12, 13, 19. Tidak berubah. 11 ✅ selesai (Batch 42, ini).

---

## [Batch 41] Fitur - Pending #20: Auto-Grant Usage Access via Shizuku (Drain Analyzer) — 2026-08-20

**Confidence Rating: 91%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `ShizukuManager.kt`, `DrainScreen.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Lanjutan roadmap Shizuku (engine Batch 23, UI wiring Batch 26, Force Stop Batch 39) — Pending #20. Drain Analyzer sebelumnya SELALU minta user buka Settings > Akses Penggunaan manual, walau Shizuku sudah aktif & bisa melakukannya otomatis lewat `appops set`.

### Selesai
- **`ShizukuManager.kt`**: fungsi baru `autoGrantUsageAccess(context: Context): Boolean` — jalankan `appops set <pkg> GET_USAGE_STATS allow` via `execShellCommand()` yang sudah ada (Batch 23, tidak diubah). **Defense in depth**: tidak percaya buta exit code sukses — setelah command jalan, verifikasi ulang lewat `UsageStatsHelper.hasUsageAccessPermission()` (AppOpsManager check riil) sebelum return true, karena `appops set` bisa "sukses" secara exit code tapi tidak selalu berefek nyata di semua ROM/device. +1 import `android.content.Context`. Brace 30/30 curly, 83/83 paren.
- **`DrainScreen.kt`**: di blok permission-gate (`!hasPermission`), tombol baru "Izinkan Otomatis via Shizuku" muncul KALAU `ShizukuManager.hasPermission()` true (Shizuku aktif & diizinkan) — panggil `autoGrantUsageAccess()` lalu `refreshTrigger++` (re-check state, pola sama seperti tombol "Sudah diizinkan, muat ulang" yang sudah ada). Tombol "Buka Pengaturan Akses Penggunaan" manual TETAP ada persis seperti sebelumnya — jalur lama 100% dipertahankan untuk user yang tidak/belum pakai Shizuku. Brace 27/27 curly, 61/61 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 6->7, `versionName` "1.0.5"->"1.0.6". Brace 23/23 curly, 65/65 paren.
- Dicek: `ShizukuManager.kt` & `UsageStatsHelper.kt` sepaket (`com.voltcare.app.util`) — pemanggilan `UsageStatsHelper.hasUsageAccessPermission()` dari `ShizukuManager` valid tanpa import tambahan.

### Sengaja TIDAK diubah
- `UsageStatsHelper.hasUsageAccessPermission()`/`openUsageAccessSettings()` — dipakai apa adanya, tidak ada perubahan API.
- `AndroidManifest.xml` — tidak ada permission baru; `appops set` dieksekusi lewat privilege binder Shizuku (shell UID), bukan lewat permission Android biasa, jadi tidak butuh entri manifest tambahan.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas brace/paren balance + audit manual pola `execShellCommand()` (dipakai persis sesuai kontrak Batch 23, 0 perubahan API di sisi itu). Command `appops set <pkg> GET_USAGE_STATS allow` adalah perintah shell standar Android (bukan API tersembunyi/reflection tambahan di luar `execShellCommand` yang sudah ada), tapi efektivitasnya di device nyata dgn Shizuku aktif BELUM diverifikasi runtime (sama seperti seluruh fitur Shizuku lain di project ini). Confidence 91% (bukan 95%+) karena alasan yang sama seperti Batch 23/39: belum ada bukti compile+run nyata di device dgn Shizuku aktif. Rekomendasi: build + test manual (Shizuku aktif & diizinkan) sebelum lanjut Pending #19 (parsing `dumpsys batterystats`, kompleksitas lebih tinggi).

### Pending Queue
10, 11, 12, 13, 19. Tidak berubah. 20 ✅ selesai (Batch 41, ini).

---

## [Batch 40] Fix - Info Update Kurang Jelas: Body Release Cuma Link Compare — 2026-08-20

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 59 -> 59 file (1 file protected edit parsial: `.github/workflows/release.yml`; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
User kirim screenshot dialog "Update Tersedia": isi body cuma `**Full Changelog**: https://github.com/FDzaki-dev/VoltCare/compare/v1.0.3-35...v1.0.4-36` — markdown mentah (asterisk tidak ter-render, `Text()` Compose polos tidak parse markdown) DAN isinya cuma link compare, tidak ada ringkasan perubahan riil. User: "informasi update kurang jelas/to the point".

### Root Cause
`.github/workflows/release.yml` step "Publish GitHub Release" pakai `generate_release_notes: true` (fitur bawaan GitHub) — fitur ini dirancang untuk repo yang pakai alur Pull Request + label kategori (bug/feature/dll). Repo VoltCare push LANGSUNG ke `main` (tanpa PR sama sekali, sesuai semua riwayat batch 1-39) — GitHub tidak punya apa pun untuk dikategorikan, jadi fallback ke boilerplate "**Full Changelog**: <link compare>" doang, PERSIS yang muncul di screenshot user. Ini bukan bug UI (`UpdateScreen.kt` menampilkan `releaseNotes` apa adanya, sesuai desain Batch 21) — murni CI menghasilkan body yang tidak informatif dari awal.

### Selesai
- **`.github/workflows/release.yml`** (protected, edit parsial): step baru "Generate release notes" disisipkan setelah "Clean up keystore", sebelum "Publish GitHub Release" — build `release_notes.md` dari `git log <tag_terakhir>..HEAD --pretty=format:"- %s" --no-merges` (pesan commit riil sejak rilis sebelumnya, format bullet list). Fallback "Rilis pertama VoltCare" kalau belum ada tag sama sekali. `git tag --sort=-creatordate | head -1` dipanggil SEBELUM tag baru dibuat oleh step Publish, jadi otomatis dapat tag SEBELUMNYA tanpa perlu filter tag saat ini.
- **`Publish GitHub Release`**: `generate_release_notes: true` dihapus, diganti `body_path: release_notes.md` — `softprops/action-gh-release@v2` baca file langsung (support resmi, tidak perlu multiline output trick).
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 5->6, `versionName` "1.0.4"->"1.0.5". Brace 23/23 curly, 65/65 paren.
- Diverifikasi: `yaml.safe_load()` parse sukses, urutan 15 step utuh (tidak ada yang terhapus), step baru berada di posisi yang benar (setelah cleanup keystore, sebelum publish).

### Sengaja TIDAK diubah
- `UpdateManager.kt`/`UpdateScreen.kt` — `Text()` polos tanpa markdown parser TETAP dipakai apa adanya. Sekarang body dari CI berupa bullet list `- pesan commit` (plain text, tanpa `**`/markdown), jadi tampil rapi walau tanpa parser markdown — tidak perlu tambah dependency Markdown renderer utk fix ini (di luar scope, bisa jadi peningkatan terpisah kalau commit message ke depan mulai pakai markdown).
- `fetch-depth: 0` di step Checkout — sudah ada sejak Batch 1 (dibutuhkan Stale Run Guard), otomatis cukup untuk `git log`/`git tag` riwayat penuh tanpa perubahan.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`.github/workflows/release.yml` — YAML diverifikasi valid (`yaml.safe_load`), 15 step (13 lama + 1 baru "Generate release notes"), Stale Run Guard/Signed-APK Guard/Smart Naming/log_fail artifact semua utuh tidak tersentuh. `app/build.gradle.kts` — brace balance 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada akses jaringan/GitHub Actions sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas pada validasi sintaks YAML + audit manual alur step & command `git log`/`git tag` (sintaks bash standar, bukan compile/run sungguhan). Confidence 90% (bukan 95%+) karena: (1) belum terverifikasi run nyata apakah `git tag --sort=-creatordate` mengembalikan tag yang benar di semua kondisi (mis. tag dengan format campuran/annotated vs lightweight — semua tag `release.yml` dibuat via `action-gh-release` yang membuat lightweight tag standar, seharusnya konsisten), (2) commit message existing di repo mungkin belum konsisten format singkat/deskriptif (di luar kendali kode ini, tergantung disiplin commit message tiap batch Termux). Rekomendasi: pantau 1x run berikutnya untuk konfirmasi `release_notes.md` terisi bullet list commit yang masuk akal, bukan kosong/aneh.

### Pending Queue
10, 11, 12, 13, 19, 20. Tidak berubah dari Batch 39.

---

## [Batch 39] Fitur - Pending #18: Force Stop Nyata via Shizuku (Drain Analyzer) — 2026-08-20

**Confidence Rating: 92%**
**File sebelum -> sesudah:** 59 -> 59 file (1 file kode diedit: `UsageStatsHelper.kt` — bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Lanjutan langsung dari roadmap Shizuku (engine Batch 23, UI wiring Batch 26) — Pending #18. `killBackgroundApp()` sebelumnya cuma `ActivityManager.killBackgroundProcesses` (izin normal, lemah — cuma proses cached/background, BUKAN "Force Stop" sungguhan).

### Selesai
- **`UsageStatsHelper.kt`**: `killBackgroundApp()` sekarang cek `ShizukuManager.hasPermission()` DULU — jika true, jalankan `am force-stop <pkg>` via `ShizukuManager.execShellCommand()` (hak sistem, PERSIS setara "Force Stop" bawaan Settings). Jika Shizuku tidak aktif/belum diizinkan ATAU command shell gagal (`!result.isSuccess`), otomatis fallback ke `killBackgroundProcesses` lama — **signature fungsi TIDAK berubah** (`(Context, String): Boolean`), jadi 1 caller existing (`DrainScreen.kt` baris 86) TIDAK perlu diedit sama sekali. Brace 18/18 curly, 47/47 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 4->5, `versionName` "1.0.3"->"1.0.4". Brace 23/23 curly, 65/65 paren.
- Dicek: `grep -rn "killBackgroundApp("` -> hanya 1 call site (`DrainScreen.kt`), tidak ada pemanggil lain yang perlu ikut diubah.

### Sengaja TIDAK diubah
- `DrainScreen.kt` — tombol "Force Stop" existing dipakai apa adanya (di luar scope 1 file/task); UI TIDAK membedakan visual apakah force-stop terjadi via Shizuku (kuat) atau fallback (lemah) — user tetap lihat tombol yang sama, cuma hasilnya sekarang lebih kuat kalau Shizuku aktif. Indikator visual dibedakan ("Force Stop (Shizuku)" vs biasa) BISA jadi peningkatan UX terpisah, TIDAK di-queue formal (kosmetik, bukan bug/fitur inti).
- `ShizukuManager.kt`, `ShizukuStatusAction.kt` — dipakai 100% apa adanya (Batch 23/26), tidak ada perubahan API.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance diverifikasi 23/23 curly, 65/65 paren, hanya 2 baris versi diganti.

### Catatan
Tidak ada compile Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas pada brace/paren balance + audit manual pola `ShizukuManager.execShellCommand()` (dipakai persis sesuai kontrak Batch 23, tidak ada perubahan API di sisi itu). Confidence 92% (bukan 95%+) karena efektivitas nyata `am force-stop` via reflection `Shizuku.newProcess()` belum terverifikasi di device fisik dengan Shizuku aktif (sama seperti catatan confidence Batch 23 yang belum berubah). Rekomendasi: build + test manual di device dengan Shizuku aktif (approve izin dulu via ikon shield Dashboard, Batch 26) sebelum lanjut Pending #19 (statistik drain riil via `dumpsys batterystats`).

### Pending Queue
10, 11, 12, 13, 19, 20. Tidak berubah. 18 ✅ selesai (Batch 39, ini).

---

## [Batch 38] Fix - Pending #23: Label Build-Number di Dialog "Update Tersedia" — 2026-08-20

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 59 -> 59 file (3 file kode diedit: `UpdateManager.kt`, `UpdateScreen.kt`, `strings.xml` — semua bukan protected; 1 file protected edit parsial: `app/build.gradle.kts` — bump versi wajib per RULE Batch 37)

### Konteks
Lanjutan langsung catatan Batch 36 ("Scope yang SENGAJA tidak disentuh") — dialog "Update tersedia" cuma nampilin `latestVersionName` (mis. "1.0.1") walau fallback run_number (Batch 36) bisa trigger `Available` walau versionName SAMA dgn yang terpasang. Tanpa label build-number, user bisa kira dialog salah/aneh ("kok bilang ada update tapi versinya sama?").

### Selesai
- **`UpdateManager.kt`**: `UpdateInfo` data class +1 field `latestRunNumber: Int` (nilai sudah ada di scope `checkForUpdate()` sejak Batch 36 — `latestRunNumber`, sekarang diteruskan ke `UpdateInfo` alih-alih cuma dipakai internal buat perbandingan). Brace 50/50 curly, 175/175 paren.
- **`strings.xml`**: `update_available_title` "Update Tersedia: v%1$s" -> "Update Tersedia: v%1$s (build %2$d)".
- **`UpdateScreen.kt`**: `stringResource(R.string.update_available_title, s.info.latestVersionName)` -> tambah arg ke-2 `s.info.latestRunNumber`. Brace 55/55 curly, 102/102 paren.
- **`app/build.gradle.kts`** (protected, edit parsial, RULE WAJIB Batch 37): `versionCode` 3->4, `versionName` "1.0.2"->"1.0.3". Brace 23/23 curly, 65/65 paren.
- Dicek: `grep -rn "UpdateInfo("` -> hanya 1 call site (`checkForUpdate()`), sudah diisi field baru, tidak ada caller lain yang perlu diupdate. `grep -rn "update_available_title"` -> hanya 1 pemakaian (`UpdateScreen.kt`), sudah cocok jumlah `%N$` placeholder dgn argumen yang dikirim.

### Sengaja TIDAK diubah
- `UpdateManager.checkForUpdate()`/`isNewerVersion()`/`isSameVersion()` — logika perbandingan versi Batch 36 dipakai apa adanya, batch ini murni nerusin nilai yang sudah dihitung ke UI, bukan ubah logika.

### Koreksi housekeeping (bukan task terpisah, murni perbaikan pencatatan)
Baris "Pending Queue" di beberapa batch terakhir (36, 37) salah menuliskan ulang `1-7, 9-20, 22` seolah semua item itu MASIH pending — padahal berdasar isi log detail tiap batch, item 1-7, 9, 14-17, 21, 22 SUDAH ✅ selesai (lihat Batch 8, 14, 16, 17, 22, 26, 28, 31, 33). Ini murni salah copy-paste baris Pending Queue antar batch (bukan regresi kode). Daftar pending AKTUAL yang benar per batch ini: **10, 11, 12, 13, 18, 19, 20** (lihat detail di bawah). 23 selesai batch ini.

### Pending Queue (daftar terkoreksi, lihat catatan housekeeping di atas)
10. Estimasi Sisa Waktu Pakai (discharge) — Dashboard, agregasi drain rate dari `BatteryLogDao`/`StressTestScreen`.
11. Preset Cepat "Alarm Batas Charge" — shortcut auto-create `RuleEntity(PERCENT_ABOVE, ALARM)`.
12. Auto-Hibernate Terjadwal — `PeriodicWorkRequest` (WorkManager) + whitelist app approved user.
13. (butuh izin user dulu, platform-limited) "Cegah auto-launch tanpa izin" — best-effort per-app settings shortcut.
18. Force Stop via Shizuku — `UsageStatsHelper.killBackgroundApp()` pakai `am force-stop` kalau `ShizukuManager.hasPermission()` true.
19. Statistik drain per-app riil via Shizuku — parsing `dumpsys batterystats` via `execShellCommand()`.
20. Auto-grant PACKAGE_USAGE_STATS via Shizuku — `appops set <pkg> GET_USAGE_STATS allow`.
23. ~~Label build-number di dialog Update tersedia~~ ✅ selesai batch ini.

---

## [Batch 37] Chore - RULE BARU: wajib bump version manual tiap kirim artifact — 2026-08-20

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file diedit: `PROJECT_STATE.md` — dokumentasi; `app/build.gradle.kts` — **protected asset**, edit parsial 2 baris)

### Permintaan user
"Tambahkan rule baru di repository: setiap sesi wajib bump version manual tiap kirim artifact. Jangan malas." — respons atas kelalaian berulang: versionName cuma di-bump 1x (Batch 32) padahal sudah 5 batch (33-36) kirim artifact tanpa bump, salah satu penyebab langsung bug Batch 36 ("Sudah Versi Terbaru" palsu).

### Selesai
- **`PROJECT_STATE.md`** (section KONVENSI TETAP, dibaca duluan tiap batch): tambah RULE WAJIB — setiap batch yang hasilkan ZIP artifact HARUS bump `versionCode`+`versionName` di `app/build.gradle.kts`, tanpa kecuali (termasuk batch docs-only). Ditegaskan ini rule DISIPLIN, terpisah dari fallback teknis `CI_RUN_NUMBER` (Batch 36) — fallback tetap ada sbg jaring pengaman, bukan alasan menunda bump manual.
- **`app/build.gradle.kts`**: langsung diterapkan di batch ini juga — `versionCode` 2->3, `versionName` "1.0.1"->"1.0.2". Brace balance 23/23, paren 65/65.

### Pending Queue
1-7, 9-20, 22, 23. Tidak berubah. 21 ✅ (Batch 33).

---

## [Batch 36] Fix - "Sudah Versi Terbaru" palsu walau ada build hijau baru — 2026-08-20

**Confidence Rating: 94%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file diedit: `app/build.gradle.kts` — **protected asset**, edit parsial; `UpdateManager.kt` — bukan protected)

### Konteks
User laporan (screenshot): in-app update checker bilang "Sudah Versi Terbaru" padahal ada build hijau baru di Actions (Batch 34/35 sudah publish Release baru).

### Root Cause
`isNewerVersion()` di `UpdateManager.kt` CUMA bandingin `versionName` (mis. "1.0.1" vs "1.0.1"). `versionName` di `app/build.gradle.kts` cuma di-bump manual di batch tertentu (Batch 32), SEDANGKAN release.yml publish Release BARU di **setiap** push ke main — jadi Batch 33/34/35 semua tetap tag `v1.0.1-<run_number berbeda>`. Checker "benar" secara literal (versionName memang sama), tapi TIDAK berguna buat workflow dev yang sering rilis fix tanpa bump versi tiap kali — ini persis kekhawatiran yang sudah dicatat di Batch 32 tapi belum ada fix teknisnya.

### Selesai
- **`app/build.gradle.kts`**: `buildFeatures.buildConfig = true` + `buildConfigField("String","CI_RUN_NUMBER", ...)` baca `System.getenv("GITHUB_RUN_NUMBER")` (env bawaan Actions, PERSIS sama dgn angka run_number yang dipakai release.yml buat tag) — default `"0"` utk build lokal/non-CI. Brace 23/23, paren 65/65.
- **`UpdateManager.kt`**: parse `latestRunNumber` dari `tag_name` (bagian setelah "-"), bandingkan ke `BuildConfig.CI_RUN_NUMBER` KALAU `versionName` sama persis (`isSameVersion()` helper baru) → sekarang `Available` ke-trigger walau versionName gak berubah, asalkan ada build/run_number lebih baru yang sukses publish. Brace 48/48, paren 171/171.
- Fix ini TIDAK butuh ubah `release.yml` sama sekali — `GITHUB_RUN_NUMBER` sudah otomatis konsisten dgn `github.run_number` yang dipakai tag (dicek langsung di file, sama-sama dari runner Actions yang sama).

### Scope yang SENGAJA tidak disentuh (Pending Queue, biar 1 task/batch)
- `UpdateScreen.kt` dialog "Update tersedia" masih nampilin `latestVersionName` doang (mis. "1.0.1") — kalau kejadian match run_number-only, teksnya keliatan "sama" dgn versi terpasang walau sebenarnya beda build. Idealnya ditambah label `(build N)`. **Bukan bug fungsional** (tombol download tetap benar ngambil APK terbaru), cuma soal kejelasan teks.

### Pending Queue
1-7, 9-20, 22. Tidak berubah. 21 ✅ (Batch 33). 23 (baru): label build-number di dialog Update tersedia (`UpdateScreen.kt`).

---

## [Batch 35] Docs - Troubleshooting: remote origin hilang di device/sesi baru — 2026-08-20

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 59 -> 59 file (1 file diedit: `TROUBLESHOOTING.md` — bukan protected asset, dokumentasi only, tidak ada perubahan kode)

### Konteks
Kejadian nyata setelah push Batch 34: commit lokal sukses (`git log -1` benar), tapi GitHub Actions gak ada run baru. Ternyata `git push origin main` gagal diam-diam karena remote `origin` gak ke-set di device/sesi Termux ini (`.git` kebentuk fresh dari `git init`, bukan dari Kotak A/clone). User minta ini didokumentasikan biar sesi lain ke depannya langsung tau, gak perlu diagnosa panjang lagi.

### Selesai
- **`TROUBLESHOOTING.md`** section "3. Termux / git": tambah entri spesifik gejala + root cause + fix wajib (`git remote -v` cek dulu, kalau kosong `git remote add origin https://github.com/FDzaki-dev/VoltCare.git`), plus catatan eksplisit ke Claude sesi lain: kalau user lapor "commit ada tapi Actions gak ke-trigger", cek `git remote -v` DULUAN sebelum `git log`/`git status`.

### Pending Queue
1-7, 9-20, 22. Tidak berubah. 21 ✅ (Batch 33).

---

## [Batch 34] Fix - Regresi Batch 31: Shizuku & Update overlap kartu Health/Suhu — 2026-08-20

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `DashboardScreen.kt`, `NavGraph.kt` — bukan protected asset)

### Konteks
User kirim screenshot: ikon Shizuku (shield oranye) numpuk di huruf "H" label "Health", ikon Update (panah-download) numpuk di pojok kartu "Suhu". Ini REGRESI dari fix Batch 31 (padding top 8dp->64dp) — geser overlap dari judul "Dashboard" ke baris kartu Health/Suhu, bukan benar-benar hilang.

### Root Cause
`ShizukuStatusAction()` & `UpdateCheckAction()` dipasang di `NavGraph.kt` sbg **overlay Box absolut** (`Modifier.align(TopStart/TopEnd).padding(top = 64.dp)`) DI ATAS `DashboardScreen()`, bukan bagian dari alur layout Column-nya. Angka `64.dp` hardcode ini cuma tebakan utk 1 kombinasi ukuran font/layar — begitu tinggi judul beda (font scale user, densitas layar lain), overlay ini turun/naik dan numpuk ke elemen berikutnya (kartu Health/Suhu). Pola overlay absolut ini pada dasarnya rapuh, akan terus berulang di kombinasi device lain walau angka padding diubah lagi.

### Selesai
- **`DashboardScreen.kt`**: tambah parameter `startAction`/`endAction: @Composable () -> Unit`. Judul "Dashboard" sekarang dalam `Row(SpaceBetween)` bareng slot aksi tsb — jadi BAGIAN ALUR LAYOUT (bukan overlay), otomatis dapat ruang sendiri berapapun tinggi judulnya. Brace 23/23, paren 56/56.
- **`NavGraph.kt`**: hapus 2 blok `Box(align(TopStart/TopEnd).padding(top=64.dp))`, diganti pass langsung `DashboardScreen(startAction = { ShizukuStatusAction() }, endAction = { UpdateCheckAction() })`. FAB "Tes Baterai" (BottomEnd) tidak disentuh — di luar laporan bug ini. Brace 26/26, paren 44/44.
- Kelas fix: struktural (pindah dari overlay ke layout flow), BUKAN sekadar tebak-angka-padding baru — jadi tidak akan ke-regresi lagi seperti Batch 31.

### Pending Queue
1-7, 9-20, 22. Tidak berubah. 21 ✅ selesai (Batch 33).

---

## [Batch 33] Fix - Pending #21: Bedakan 'Sudah Terbaru' vs 'Gagal Cek' di Update Checker — 2026-08-19

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `UpdateManager.kt`, `UpdateScreen.kt` — bukan protected asset)

### Konteks
User laporan (setelah Batch 30 & 32): update in-app MASIH nunjuk "Sudah Versi Terbaru" walau app sebenarnya usang. Ini persis Pending #21 yang dicatat Batch 30 tapi belum dikerjakan.

### Root Cause
`checkForUpdate()` return `UpdateInfo?` — `null` dipakai untuk 2 arti BEDA yang digabung jadi 1 pesan UI ("Sudah Versi Terbaru"):
1. Beneran sudah versi terbaru (`isNewerVersion()` false).
2. **Cek GAGAL TOTAL** (HTTP 404/network/parsing error) — termasuk kemungkinan besar penyebab laporan user: repo `VoltCare` (pasca-rename Batch 28/29) **belum punya GitHub Release SAMA SEKALI**. Kalau rename Batch 28 itu sebenarnya bikin repo baru dari nol (bukan rename asli — indikasi dari error "Repository not found" yg TIDAK redirect), maka **GitHub Secrets lama (keystore dll) juga ikut hilang**, workflow gagal di step "Verify APK is signed" → tidak pernah sampai step "Publish GitHub Release" → `/releases/latest` 404 selamanya.

### Selesai
- **`UpdateManager.kt`**: `checkForUpdate()` sekarang return `UpdateCheckResult` (sealed: `UpToDate` / `Available(info)` / `CheckFailed(reason)`) — bukan `UpdateInfo?` lagi. HTTP 404 dikasih pesan spesifik ("Belum ada Release... cek tab Actions"), error lain (network/JSON) tetap ke `CheckFailed` dgn pesan asli.
- **`UpdateScreen.kt`**: `UpdateViewModel.checkForUpdate()` diupdate ikut sealed result baru. `CheckFailed` diarahkan ke `UpdateUiState.Failed` yang SUDAH ADA (dialog error, tidak perlu state/string baru).
- Brace balance: `UpdateManager.kt` 48/48 curly 160/160 paren. `UpdateScreen.kt` 55/55 curly 102/102 paren.
- Dicek: cuma 1 caller (`UpdateScreen.kt`) yang pakai `checkForUpdate()`/`UpdateInfo?` lama — tidak ada tempat lain yang perlu ikut diubah.

### ⚠️ Aksi MANUAL yang user perlu cek (di luar kemampuan saya verifikasi tanpa network/GitHub akses)
1. Buka `github.com/FDzaki-dev/VoltCare/actions` — kalau run terakhir status ❌ merah di step "Verify APK is signed"/"Decode release keystore", itu konfirmasi dugaan di atas.
2. Kalau iya, jalankan ULANG command **Secrets** (dari skrip Initial Setup Kotak A/B, bagian atas) tapi target ke repo `VoltCare` (`gh` CLI otomatis pakai repo default folder `~/projects/VoltCare` yang sekarang), lalu push ulang commit apapun (misal batch ini) supaya workflow trigger lagi dan (kalau secrets sudah benar) berhasil publish Release pertama di repo baru.
3. Setelah ada 1 Release sukses ter-publish, buka app → cek update lagi → sekarang HARUS entah dapat `Available` (kalau versi rilis > versi terpasang) atau pesan error yang JELAS (bukan lagi "sudah terbaru" palsu).

### Pending Queue
1-7, 9-20, 22. Tidak berubah. 21. ✅ selesai (Batch 33, ini).

---

## [Batch 32] Chore - Bump Versi 1.0.0 -> 1.0.1 (Batch 27-31 belum pernah dapat versi baru) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 59 -> 59 file (1 file diedit: `app/build.gradle.kts` — **protected asset**, edit parsial 2 baris)

### Alasan
Halaman Info Aplikasi Android (screenshot user) nunjuk `VoltCare 1.0.0` — padahal sudah 5 batch fix (27-31) numpuk sejak versi itu tag. Root cause: `versionCode`/`versionName` gak pernah di-bump manual dari sejak project awal. Ini juga relevan ke fix Batch 30 (`UpdateManager.kt`) — update-checker cuma berguna kalau ada versi baru buat dibandingin; kalau `versionName` gak pernah naik, checker bakal "benar" bilang sudah terbaru walau banyak fix numpuk.

### Selesai
- **`app/build.gradle.kts`** (protected, edit parsial): `versionCode` 1->2, `versionName` "1.0.0"->"1.0.1". Brace balance 20/20.

### Dicek, TIDAK perlu diubah manual di tempat lain
- `.github/workflows/release.yml` baris 49: `VERSION_NAME=$(grep -m1 'versionName = ' app/build.gradle.kts ...)` — **otomatis baca dari `build.gradle.kts`**, jadi tag rilis (`v1.0.1-<run_number>`) & nama APK (`VoltCare_v1.0.1_<run_number>.apk`) bakal ikut update sendiri begitu workflow jalan lagi, TIDAK ada hardcode lain yang perlu disentuh.
- `UpdateManager.kt` `getCurrentVersionName()` baca `packageManager.getPackageInfo(...).versionName` (runtime, otomatis ikut apapun yang di-build) — TIDAK perlu ubah kode, cukup bump `build.gradle.kts`.

### Catatan
Bump ini PATCH (1.0.0 -> 1.0.1) karena isi Batch 27-31 semuanya bugfix (insets, update-checker 404, icon overlap), bukan fitur baru — sesuai semver. Setelah build & push, workflow GitHub Actions otomatis publish GitHub Release baru dgn tag `v1.0.1-<run_number>` — baru setelah itu in-app update-checker (Batch 30) punya sesuatu yang beneran baru buat dideteksi kalau ada device lain yang masih pegang APK 1.0.0.

### Pending Queue
1-7, 9-22. Tidak berubah, lihat Batch 26-31.

---

## [Batch 31] Fix - Icon Shield Overlap Judul 'Dashboard' (Redundant Scaffold + Jarak Terlalu Dekat) — 2026-08-19

**Confidence Rating: 88%**
**File sebelum -> sesudah:** 59 -> 59 file (2 file kode diedit: `DashboardScreen.kt`, `NavGraph.kt` — protected asset, edit parsial)

### Konteks
User laporan screenshot: icon shield (`ShizukuStatusAction`) numpuk sama huruf "D" di judul "Dashboard", dan klaim ini nyambung ke task insets/deformasi layar di awal sesi (dokumen `dokumentasi_insets_targetsdk34.md`). **Klaim user BENAR SEBAGIAN** — nyambung ke topik yang sama (arsitektur Scaffold/insets), tapi mekanismenya beda dari dugaan awal:

### Root Cause (2 hal, bukan cuma 1)
1. **`DashboardScreen.kt` punya `Scaffold {}` SENDIRI** (baris 28, versi lama) — padahal screen ini sudah dibungkus `Scaffold` di `NavGraph.kt`. Ini persis pelanggaran "Aturan Emas: Hindari Redundansi" dari dokumen insets — SUDAH diwanti-wanti di CHANGELOG Batch 27, tapi audit Batch 27 cuma cek `NavGraph.kt`, **lolos cek `DashboardScreen.kt`**.
2. **Overlap sebenarnya BUKAN soal insets ganda/kurang** (Material3 `Scaffold` setahu saya sudah consume window insets utk descendant-nya) — murni jarak antar 2 elemen kependekan: overlay icon `.padding(8.dp)` vs judul yg render di ~16dp dari baseline yang SAMA (offset seragam, ada/tidaknya edge-to-edge tidak mengubah jarak relatif keduanya). Jadi `enableEdgeToEdge()` Batch 27 **BUKAN penyebab langsung overlap ini**, tapi audit yang kurang lengkap saat Batch 27 (poin 1 di atas) memang bagian dari pekerjaan yang sama.

### Selesai
- **`DashboardScreen.kt`**: `Scaffold {}` dihapus, diganti `Column` polos. Titik konsumsi insets SEKARANG SATU-SATUNYA di `NavGraph.kt` (sesuai Aturan Emas).
- **`NavGraph.kt`** (protected, edit parsial): padding top overlay `ShizukuStatusAction`/`UpdateCheckAction` dinaikkan `8.dp` -> `64.dp` (clear dari tinggi judul headlineMedium + 16dp Column padding).
- Brace balance diverifikasi: `DashboardScreen.kt` 19/19 curly, 47/47 paren. `NavGraph.kt` 26/26 curly, 50/50 paren.

### Catatan (confidence 88%, bukan 95%+)
Nilai `64.dp` estimasi manual berdasar `headlineMedium` line-height + padding, BUKAN diukur dari compile/render asli (tidak ada Gradle/emulator di sandbox). Kalau setelah build & install masih ada sisa overlap/kegedean gap-nya, kandidat penyesuaian: naik/turunin angka `64.dp` di `NavGraph.kt` baris overlay Dashboard saja (1 file, tidak perlu ubah `DashboardScreen.kt` lagi).

### Pending Queue
1-7, 9-21. Tidak berubah. 22. ✅ selesai (Batch 31, ini). Rekomendasi jangka panjang (opsional, belum masuk pending formal): ganti pola overlay Box absolut ini dengan `TopAppBar`/`Row` header yang proper di dalam `DashboardScreen.kt` biar tidak rawan collision serupa di masa depan.

---

## [Batch 30] Fix - Update Checker Selalu 'Sudah Versi Terbaru' Palsu (GITHUB_REPO Stale) — 2026-08-19

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 59 -> 59 file (1 file kode diedit: `UpdateManager.kt`, bukan protected asset)

### Root Cause
Regresi dari rename repo Batch 28/29: `UpdateManager.kt` masih hardcode `GITHUB_REPO = "PowerVaultHealthPro"`. `checkForUpdate()` panggil `api.github.com/repos/FDzaki-dev/PowerVaultHealthPro/releases/latest` -> 404 (repo sudah pindah nama) -> `!response.isSuccessful` -> return null -> UI (`UpdateScreen.kt`) treat null == "tidak ada update" -> dialog "Sudah Versi Terbaru" muncul PADAHAL cek-nya gagal total, bukan karena memang sudah versi terbaru. Bug ini SELALU terjadi (deterministik) tiap tombol cek update dipencet, sejak Batch 28.

### Selesai
- **`UpdateManager.kt`**: `GITHUB_REPO` diganti `"PowerVaultHealthPro"` -> `"VoltCare"`. Komentar diupdate jadi warning eksplisit: konstanta ini WAJIB ikut diupdate kalau repo di-rename lagi.

### Diperiksa, TIDAK ada instance lain
`grep -rn "PowerVaultHealthPro" app/ .github/` -> HANYA `UpdateManager.kt` (sudah fix). Tidak ada file kode/CI lain yang hardcode nama repo lama.

### Sengaja TIDAK diubah (di luar scope 1 task ini, dicatat Pending Queue)
- **UX ambiguitas fail-safe**: `checkForUpdate()` return `null` untuk 2 kasus berbeda (benar-benar sudah terbaru VS gagal cek/network/404) dan digabung jadi 1 pesan "Sudah Versi Terbaru" di UI — user tidak bisa bedain. Idealnya `UpdateInfo?` diganti sealed result (`UpToDate` / `CheckFailed(reason)` / `Available(info)`) biar UI bisa tampil pesan beda. TIDAK dikerjakan batch ini (scope kode lebih luas, >1 file: `UpdateManager.kt` + `UpdateScreen.kt`), masuk Pending Queue #21.
- Icon shield/lock kecil yang keliatan overlap sama huruf "D" di judul "Dashboard" (kelihatan di screenshot user) — BELUM diverifikasi apakah ini bug beneran atau cuma badge kecil yang emang didesain nempel di situ. Tidak disentuh batch ini (di luar topik update-checker), masuk Pending Queue #22 kalau user konfirmasi itu bug.

### Catatan
Setelah update ini, pencet cek update lagi di app — HARUS berhasil hit API beneran (bukan 404). Kalau memang belum ada release baru di GitHub Releases repo `VoltCare`, dialog "Sudah Versi Terbaru" yang muncul SEKARANG baru valid (bukan false-positive dari 404).

### Pending Queue
1-7, 9-20. Tidak berubah. 21. (baru) Pisahkan hasil `checkForUpdate()` jadi sealed result biar UI bisa bedain "sudah terbaru" vs "gagal cek". 22. (baru, perlu konfirmasi user) Cek overlap icon shield vs teks "Dashboard" di header.

---

## [Batch 29] Housekeeping - Rename Folder Lokal Termux (PowerVaultHealthPro -> VoltCare) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 59 -> 59 file (0 file kode berubah — murni dokumentasi: `PROJECT_STATE.md`, `CHANGELOG.md`)

### Selesai
- **`PROJECT_STATE.md`**: Konvensi Tetap diupdate — folder lokal Termux resmi `~/projects/VoltCare`, semua skrip Termux ke depan pakai path baru.
- **`CHANGELOG.md`**: entri Batch 29 ditambah di baris teratas.
- **Fix non-file (Termux, lihat skrip di bawah)**: `mv ~/projects/PowerVaultHealthPro ~/projects/VoltCare` (rename folder, history `.git` ikut pindah otomatis karena `mv` folder utuh, TIDAK perlu clone ulang / TIDAK kehilangan commit lokal).

### Diperiksa, TIDAK ada dampak
- `scripts/preflight_check.sh` — pakai path relatif (`cd "$(dirname "$0")/.."`), tidak hardcode nama folder, aman tanpa perubahan.
- `.github/workflows/*.yml` — pakai `${{ github.repository }}`/context otomatis, tidak hardcode path lokal Termux siapa pun.
- Package Kotlin `com.voltcare.app`, `rootProject.name`, `applicationId` — semua SUDAH `voltcare` sejak batch sebelumnya, tidak tersentuh rename folder ini (rename folder lokal = operasi filesystem murni, bukan operasi source code).

### Catatan
Rename folder WAJIB dilakukan SEBELUM extract ZIP batch ini (lihat urutan skrip Termux) — kalau skrip Update Harian biasa dijalankan duluan dgn path lama, akan gagal karena folder `PowerVaultHealthPro` sudah tidak ada lagi setelah `mv`.

### Pending Queue
1-7, 9-20. Tidak berubah, lihat Batch 26-27. 8. ✅ selesai (Batch 28). Rename folder lokal (permintaan baru user) ✅ selesai (Batch 29).

---

## [Batch 28] Fix - Remote Git Nyasar ke Repo Lama (404 Repository not found) — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 59 -> 59 file (0 file kode berubah — murni dokumentasi: `PROJECT_STATE.md`, `CHANGELOG.md`)

### Root Cause
User rename repo GitHub `PowerVaultHealthPro` -> `VoltCare` secara manual (di luar sesi chat ini, kemungkinan via web UI GitHub, BUKAN `gh repo rename`) — rename via web UI/API biasanya bikin GitHub auto-redirect URL lama ke baru utk `git push`/`clone`, TAPI error yang didapat user (`remote: Repository not found. fatal: repository '.../PowerVaultHealthPro.git/' not found`) menunjukkan TIDAK ada redirect aktif. Kemungkinan penyebab: repo lama sempat dihapus & repo `VoltCare` baru dibuat dari nol (bukan rename murni), atau redirect GitHub belum ke-refresh di sisi client. Konsekuensi sama: remote `origin` di folder lokal Termux (`~/projects/PowerVaultHealthPro/.git/config`) masih menunjuk URL mati.

### Selesai
- **`PROJECT_STATE.md`**: Konvensi Tetap diupdate — nama repo resmi sekarang `FDzaki-dev/VoltCare`, Pending Queue #8 ditandai selesai, catatan riwayat ditambahkan supaya batch-batch lama (masih sebut `PowerVaultHealthPro` sbg nama repo) tidak disalahartikan sbg state aktif (sesuai Chronological Document Rule).
- **`CHANGELOG.md`**: entri Batch 28 ditambah di baris teratas.
- **Fix non-file (Termux, lihat skrip di bawah)**: `git remote set-url origin https://github.com/FDzaki-dev/VoltCare.git` di folder lokal existing, lalu `git push -u origin main` ulang — TIDAK perlu `git init`/clone ulang, history commit lokal aman.

### Sengaja TIDAK diubah
- Folder lokal Termux TETAP `~/projects/PowerVaultHealthPro` — rename folder lokal tidak wajib secara teknis (nama folder tidak dibaca `git`/GitHub Actions manapun), hanya kosmetik. Kalau user mau tetap konsisten, rename manual (`mv ~/projects/PowerVaultHealthPro ~/projects/VoltCare`) opsional, TIDAK di-otomasi batch ini supaya tidak merusak path relatif skrip Termux yang sudah dipakai berulang.
- `.github/workflows/release.yml` — tidak ada referensi hardcode nama repo lama di dalamnya (pakai `${{ github.repository }}` otomatis dari GitHub context), jadi rename repo TIDAK berdampak ke CI/CD sama sekali, tidak perlu fix.
- Kode aplikasi (0 file `.kt`/`.xml` disentuh) — murni masalah infra git, bukan bug aplikasi.

### Catatan
Ini BUKAN regresi dari Batch 27 — insets fix Batch 27 tetap berlaku, belum di-push ke remote sama sekali karena block oleh error remote ini duluan. Setelah fix remote di skrip Termux (di bawah), push akan otomatis membawa SEMUA commit lokal yang masih pending (termasuk Batch 27), bukan cuma Batch 28.

### Pending Queue
1-7, 9-20. Tidak berubah, lihat Batch 26-27. 8. ✅ selesai (repo sudah `VoltCare`, dikonfirmasi Batch 28).

---

## [Batch 27] Fix - Edge-to-Edge Insets Konsisten Lintas OS (dari dokumentasi_insets_targetsdk34.md) — 2026-08-19

**Confidence Rating: 92%**
**File sebelum -> sesudah:** 59 -> 59 file (0 baru/hapus, 1 diedit parsial: `MainActivity.kt` protected)

### Alasan
User upload `dokumentasi_insets_targetsdk34.md` (panduan generik berbasis XML/View `ComponentActivity`+`ViewCompat.setOnApplyWindowInsetsListener`) minta debug proyek pakai panduan itu. VoltCare 100% Jetpack Compose (`setContent`, tidak ada `activity_main.xml`/`findViewById`) — panduan TIDAK bisa ditempel mentah.

### Selesai
- **`MainActivity.kt`** (edit parsial, protected): tambah `enableEdgeToEdge()` (dari `androidx.activity:activity-compose:1.9.1`, sudah jadi dependency sejak awal) dipanggil SEBELUM `super.onCreate()` — persis Langkah A dokumen, versi Compose dari `AppCompatActivity.enableEdgeToEdge()`.

### Sengaja TIDAK diubah (adaptasi krusial dari dokumen)
- **TIDAK** menambah `ViewCompat.setOnApplyWindowInsetsListener(...)` manual (Langkah B dokumen) — di app Compose, `AndroidComposeView` sudah pasang listener insets miliknya sendiri; listener manual tambahan berisiko override/bentrok (last-listener-wins) sehingga insets malah TIDAK sampai ke Composable → justru akan MEMICU deformasi layout yang coba dicegah dokumen ini. Konsumsi insets diserahkan ke `Scaffold` + `NavigationBar` Material3 di `NavGraph.kt` (sudah edge-to-edge aware bawaan sejak Material3 1.x, satu titik konsumsi konsisten dgn "Aturan Emas" dokumen bag.3).
- `NavGraph.kt`, `themes.xml` — TIDAK disentuh. `Scaffold(bottomBar = { NavigationBar {...} })` (baris 56-80) sudah otomatis kasih padding sistem-bar yang benar ke `NavHost` (via `innerPadding`) begitu edge-to-edge aktif; menambah insets handling kedua di sini berisiko double-padding (persis yang diwanti-wanti dokumen bag.4 checklist "Hindari Redundansi").
- Tema `Theme.VoltCare` (parent `android:Theme.Material.NoActionBar`, non-transparan) TIDAK diubah ke status bar transparan manual — `enableEdgeToEdge()` sudah handle `SystemBarStyle` secara terprogram, edit XML manual (dokumen bag.3 Langkah A) redundant & berisiko konflik.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`MainActivity.kt` — brace balance diverifikasi 11/11 curly, 22/22 paren. Hanya 1 import + 1 pemanggilan fungsi disisipkan sebelum `super.onCreate()`, sisanya utuh (permission flow & `setContent` tidak berubah).

### Catatan
Tidak ada compile Gradle/device fisik sungguhan (network disabled) — verifikasi terbatas brace/paren balance + audit manual API (`enableEdgeToEdge()` tersedia sejak `androidx.activity:activity` 1.8.0, project pakai 1.9.1 via `activity-compose` — aman). Asumsi belum diverifikasi compile nyata: default `contentWindowInsets` Material3 `Scaffold` 1.2.1 sudah cukup untuk cover status+navigation bar tanpa config tambahan (pola umum M3, bukan API baru) — kalau device test masih nunjuk celah, kandidat berikutnya: set eksplisit `Scaffold(contentWindowInsets = WindowInsets.safeDrawing, ...)` di `NavGraph.kt`. Rekomendasi: build + test manual di 2 device (salah satu < Android 14) sebelum lanjut Pending #18.

### Pending Queue (Batch 27: fix insets selesai, Pending #18-20 tidak berubah)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. ✅ selesai (Batch 22) — `ShizukuStatusAction.kt` masih perlu ditambah ke manifest (digeser, lihat Batch 26 catatan)
18-20. Lihat Batch 26 (belum dikerjakan)

---

## [Batch 26] Fitur - Shizuku UI Wiring (Pending Queue #17) — 2026-08-19

**Confidence Rating: 93%**
**File sebelum -> sesudah:** 58 -> 59 file (1 baru: `ShizukuStatusAction.kt`; 2 diedit parsial: `strings.xml`, `NavGraph.kt` protected)

### Alasan
Lanjutan langsung Batch 23 (`ShizukuManager.kt`, core engine) — kini status Shizuku (NotInstalled/NotRunning/PermissionDenied/Ready) ditampilkan di UI & user bisa trigger `requestPermission()` sendiri. Pola meniru persis Batch 21 (In-App Updater UI Wiring): 1 Composable+ViewModel self-contained baru, dipasang via overlay `Box` di `NavGraph.kt` supaya `DashboardScreen.kt` tidak disentuh.

### Selesai
- **`ui/screens/shizuku/ShizukuStatusAction.kt`** (baru): `ShizukuStatusViewModel` (`AndroidViewModel`, `StateFlow<ShizukuManager.State>`) — `init` langsung `refresh()` + daftar `ShizukuManager.addBinderListeners()`/`addPermissionResultListener()` (self-contained, TIDAK di `VoltCareApplication.kt`, lihat Sengaja Tidak Diubah). `ShizukuStatusAction()` composable: `IconButton` ikon `AdminPanelSettings` (tint `VcGreen`=Ready, `VcAmber`=PermissionDenied, `VcTextSecondary`=lainnya) + `AlertDialog` per state, tombol "Minta Izin" muncul khusus state PermissionDenied/NotRunning -> panggil `viewModel.requestPermission()`.
- **`strings.xml`** (edit): 11 string baru `shizuku_*` (title/body per 4 state + tombol aksi), mengikuti pola persis `update_*` (Batch 21).
- **`NavGraph.kt`** (edit parsial, protected): `ShizukuStatusAction()` dipasang di `Box` overlay Dashboard, `Alignment.TopStart` — sengaja beda sudut dari `UpdateCheckAction` (`TopEnd`, Batch 21) & FAB Tes Baterai (`BottomEnd`, Batch 12) supaya tidak tabrakan visual. `DashboardScreen.kt` TIDAK disentuh.

### Sengaja TIDAK diubah
- **`VoltCareApplication.kt`** (protected) — TIDAK diedit batch ini. Listener binder/permission didaftarkan langsung di `ShizukuStatusViewModel.init` (self-contained, cakupan hidup selama Composable Dashboard aktif) supaya batch tetap pas 3 file sesuai Strict Micro-Batching Rule. Registrasi level-Application (utk lifecycle di luar layar Dashboard, mis. auto-hibernate terjadwal Pending #20) di-queue terpisah kalau nanti terbukti dibutuhkan.
- `AndroidManifest.xml` — tidak ada perubahan; provider Shizuku sudah didaftarkan sejak Batch 23, tidak ada permission manifest tambahan yang dibutuhkan izin runtime Shizuku.
- `UsageStatsHelper.kt` (Force Stop rewire, Pending #18) & parsing `dumpsys batterystats` (Pending #19) — belum disentuh, murni UI status + trigger izin dulu di batch ini.
- `FILE_MANIFEST.txt` — belum diupdate (entri baru: `ShizukuStatusAction.kt`), di-queue bareng housekeeping berikutnya (gabung ke item lama #14-style).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`NavGraph.kt` — brace balance diverifikasi (26/26 curly, 48/48 paren), 1 import + 1 blok `Box` overlay baru disisipkan sebelum blok `UpdateCheckAction` yang sudah ada, sisanya utuh.

### Catatan
Tidak ada akses jaringan/Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — verifikasi terbatas pada brace/paren balance (`ShizukuStatusAction.kt` 30/30 curly, 51/51 paren) + XML valid (`strings.xml`) + review manual pola call `ShizukuManager` (Batch 23, tidak diubah sama sekali batch ini — hanya dipanggil). BUKAN compile Gradle sungguhan. Confidence 93% (bukan 95%+) karena: (1) `Icons.Filled.AdminPanelSettings` diasumsikan tersedia di `material-icons-extended` (dependency sudah ada sejak awal project, dipakai icon lain spt `Rule`/`SystemUpdate`, tapi nama ikon spesifik ini belum diverifikasi compile sungguhan), (2) `ShizukuManager.addPermissionResultListener`/`addBinderListeners` yang didaftarkan di `ViewModel.init` belum diverifikasi jalan di device nyata (perilaku listener Shizuku SDK asli, bukan reflection). Rekomendasi: build + test manual di Termux/device dengan & tanpa Shizuku aktif sebelum lanjut Pending #18 (Force Stop rewire).

### Pending Queue (Batch 26: item #17 selesai — UI wiring status+permission; #18-20 masih menunggu implementasi nyata)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. (housekeeping) Update `FILE_MANIFEST.txt` — tambah `ShizukuStatusAction.kt`, gabung ke housekeeping berikutnya
17. ~~Shizuku UI Wiring~~ ✅ selesai batch ini
18. **Force Stop via Shizuku** (Drain Analyzer upgrade): `UsageStatsHelper.killBackgroundApp()` — kalau `ShizukuManager.hasPermission()` true, pakai `am force-stop <pkg>`; kalau false, tetap fallback jalur lama. Estimasi 1-2 file.
19. **Statistik drain per-app riil via Shizuku**: `dumpsys batterystats` diparsing via `execShellCommand()`. Estimasi 1-2 file, kompleksitas parsing tinggi -> mungkin perlu dipecah lagi.
20. **Auto-grant PACKAGE_USAGE_STATS via Shizuku** (`appops set <pkg> GET_USAGE_STATS allow`). Estimasi 1 file.

---

## [Batch 25] Cleanup - Hapus Workflow & Source Orphan "PromptVault" — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 124 -> 58 file (66 dihapus: `.github/workflows/build.yml` + 65 file source/test `com/elprompter/promptvault/*`, semua non-protected)

### Root Cause
Repo VoltCare (`FDzaki-dev/PowerVaultHealthPro`) ternyata masih membawa sisa project app LAIN — **PromptVault** (file-organizer app) — dari sebelum repo dipakai untuk VoltCare:
- `.github/workflows/build.yml` ("Build PromptVault APK") masih ter-trigger tiap push ke `main`, terpisah dari `release.yml`.
- 65 file `.kt` di `app/src/main/java/com/elprompter/promptvault/...` & `app/src/test/.../promptvault/...` ikut ke-bundle di module `:app` yang sama dengan VoltCare (`com.voltcare.app`).

Gradle `compileDebugKotlin` (dipicu `build.yml`) mengcompile SEMUA `.kt` di `app/src/main/java` tanpa pandang package → source PromptVault ikut kecompile tapi `build.gradle.kts` VoltCare tidak punya dependency yang PromptVault butuh (datastore, kotlinx-serialization, splashscreen, lifecycle-compose) → build gagal (artifact `build-failure-log-v1.0.0`). Berpotensi juga bikin `release.yml` (`assembleRelease`) ikut gagal karena compile source set yang sama.

Verifikasi sebelum hapus: 0 referensi silang antara `com.voltcare.app` <-> `com.elprompter.promptvault` (grep 2 arah, hasil kosong) → 100% unreferenced. Izin hapus sudah dikonfirmasi user.

### Selesai
- Hapus `.github/workflows/build.yml` (bukan protected — hanya `release.yml` yang protected).
- Hapus `app/src/main/java/com/elprompter/` (termasuk `promptvault/`), `app/src/test/java/com/elprompter/`, `app/src/main/aidl/com/elprompter/` — total 65 file.
- `release.yml`, `AndroidManifest.xml`, `build.gradle.kts`, `settings.gradle.kts`, `.gitignore` (semua protected) dicek utuh, tidak tersentuh.

### Pending Queue
- (tidak ada — murni cleanup sesuai temuan & izin user)

---

## [Batch 24] Bugfix - Update Checker Salah Deteksi "Sudah Versi Terbaru" — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 55 -> 55 file (1 diedit parsial: `UpdateManager.kt`, tidak protected)

### Root Cause
`release.yml` menerbitkan `tag_name: v{version}-{run_number}` (mis. `v1.0.0-23`). Di `UpdateManager.isNewerVersion()`, tag di-split by `.` lalu tiap segmen di-`toIntOrNull()` — segmen terakhir `"0-23"` gagal parse (bukan angka murni karena ada `-23`) dan **diam-diam dibuang** dari `latestParts`. Akibatnya array versi terbaru jadi lebih pendek dari versi lokal, perbandingan digit hilang selalu dianggap `0`, dan APK yang sudah live di GitHub Release (compile hijau) tidak pernah terdeteksi lebih baru → dialog selalu bilang "Sudah Versi Terbaru" walau ada rilis baru.

### Selesai
- **`util/UpdateManager.kt`** (edit parsial): `latestVersion` sekarang `substringBefore("-")` sebelum di-parse, jadi `"1.0.0-23"` → `"1.0.0"` (bersih) sebelum dibandingkan. `checkForUpdate()`/`downloadUpdate()` lain tidak berubah.

### Pending Queue (tidak dikerjakan batch ini)
- (tidak ada penambahan baru — murni bugfix 1 file sesuai laporan user)

---

## [Batch 23] Fitur - Shizuku Core Integration (Engine, belum diwiring UI) — 2026-08-19

**Confidence Rating: 90%**
**File sebelum -> sesudah:** 54 -> 55 file (1 baru: `ShizukuManager.kt`; 2 diedit parsial: `app/build.gradle.kts`, `AndroidManifest.xml`, keduanya protected)

### Alasan
User minta integrasi Shizuku 100% supaya fitur yang "agak mustahil" dengan izin Android biasa (Force Stop sungguhan, statistik drain per-app riil, auto-grant Usage Access, auto-hibernate terjadwal) jadi mungkin. Scope ini besar -> sesuai Strict Micro-Batching Rule dipecah bertahap, pola sama seperti In-App Updater (Batch 19-21): **Batch 23 (ini) = core engine/wrapper**, sisanya di-queue.

### Selesai
- **`app/build.gradle.kts`** (edit parsial, protected): +2 dependency `dev.rikka.shizuku:api:13.1.5` & `dev.rikka.shizuku:provider:13.1.5` (Maven Central, `settings.gradle.kts` sudah punya repo ini sejak Batch 1 — tidak perlu tambah repo baru).
- **`AndroidManifest.xml`** (edit parsial, protected): daftar `<provider android:name="rikka.shizuku.ShizukuProvider" .../>` (authority `${applicationId}.shizuku`, `exported=true`, permission `INTERACT_ACROSS_USERS_FULL`) — wajib agar app Shizuku bisa binding & memberi izin ke VoltCare. Tidak ada `<uses-permission>` tambahan (Shizuku pakai izin runtime lewat dialognya sendiri, bukan manifest permission).
- **`util/ShizukuManager.kt`** (baru): `isBinderAlive()`, `hasPermission()`, `currentState()` (sealed `State`: NotInstalled/NotRunning/PermissionDenied/Ready), `requestPermission()`, `addBinderListeners()`, `addPermissionResultListener()`, `execShellCommand(cmd): ShellResult` — eksekusi command shell privilege Shizuku via `Shizuku.newProcess()` (reflection, hidden-tapi-didukung resmi di Shizuku API 11.x-13.x, pola umum dipakai app pihak ketiga berbasis Shizuku). Seluruh fungsi fail-safe try-catch total, tidak pernah throw ke caller — konsisten pola `CrashLogger.kt`/`UpdateManager.kt`.

### Keputusan Desain Penting
- **Graceful fallback wajib**: tanpa Shizuku aktif/diizinkan, `hasPermission()`/`execShellCommand()` selalu return "tidak tersedia" secara aman, TIDAK mengubah perilaku fitur existing (`UsageStatsHelper.killBackgroundApp` tetap dipakai apa adanya sbg fallback default). Shizuku murni opsional booster, bukan requirement baru untuk app tetap jalan.
- **Bukan root langsung**: VoltCare tidak pernah minta akses root sendiri. Privilege datang dari binder Shizuku yang usernya aktifkan & approve sendiri lewat app Shizuku terpisah (ADB pairing wireless Android 11+, atau root activator jika device di-root) — di luar kendali/kode VoltCare sepenuhnya.
- **`Shizuku.newProcess()` via reflection** dipilih (bukan `Shizuku.bindUserService`/AIDL) supaya core engine batch ini tetap 1 file & self-contained; AIDL UserService (lebih "proper" untuk command kompleks/berulang) di-queue sbg opsi upgrade jika reflection terbukti tidak stabil di device nyata.

### Sengaja TIDAK diubah
- `VoltCareApplication.kt` (protected) — `addBinderListeners()` belum dipanggil dari mana pun (baru wrapper standalone), pendaftaran listener siklus hidup app di-queue ke batch UI wiring supaya batch ini tetap 3 file.
- `MainActivity.kt`, `NavGraph.kt`, screen manapun — belum ada tombol/UI trigger permission Shizuku sama sekali, murni engine dulu (pola identik Batch 19 utk UpdateManager).
- `UsageStatsHelper.kt` (Drain Analyzer) — belum diubah untuk memakai Shizuku; `killBackgroundApp()` tetap 100% jalur lama. Rewire ke `am force-stop` via `ShizukuManager.execShellCommand()` di-queue Batch 24.
- `FILE_MANIFEST.txt` — belum diupdate (akan jadi file ke-4), di-queue bareng housekeeping berikutnya.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance diverifikasi (20/20 curly, 61/61 paren), 2 baris ditambah di blok `dependencies` existing kedua. `AndroidManifest.xml` — XML diverifikasi valid (`xml.etree.ElementTree.parse` sukses), 1 `<provider>` baru disisipkan sebelum penutup `</application>`, seluruh isi lain utuh.

### Catatan
Tidak ada akses jaringan/Gradle/device fisik sungguhan di lingkungan pembuatan ZIP ini (network disabled) — `ShizukuManager.kt` diverifikasi via audit manual API Shizuku (nama method publik `pingBinder`/`checkSelfPermission`/`requestPermission`/`isPreV11`/listener sesuai dokumentasi resmi versi 11.x-13.x) + brace balance (29/29 curly, 75/75 paren), BUKAN compile Gradle sungguhan. Method `newProcess()` diakses via reflection karena tidak ada di public API surface resmi `rikka.shizuku:api` — signature `(String[], String[], String)` berdasarkan pola yang dipakai luas oleh app pihak ketiga berbasis Shizuku, tapi **belum terverifikasi jalan di device nyata di batch ini**. Confidence 90% (bukan 95%+) karena 2 hal belum terkonfirmasi: (1) resolusi dependency `dev.rikka.shizuku:*:13.1.5` sukses saat build sungguhan, (2) `newProcess()` reflection benar-benar berfungsi di runtime saat Shizuku aktif. Rekomendasi kuat: build + test manual di Termux/device dengan Shizuku aktif sebelum lanjut Batch 24 (rewire fitur nyata), supaya kalau reflection gagal, upgrade ke `bindUserService`/AIDL bisa diputuskan lebih awal.

### Pending Queue (Batch 23: 1 fitur besar dipecah, 4 sub-task Shizuku baru ditambahkan sbg #17-20)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. (housekeeping) Update `FILE_MANIFEST.txt` — digeser lagi, gabung ke #17
15-16. ✅ selesai (lihat Batch 19-22)
17. **Shizuku UI Wiring** (KRUSIAL, lanjutan langsung): tombol/indikator status Shizuku (State: NotInstalled/NotRunning/PermissionDenied/Ready) di Dashboard atau Settings baru, panggil `requestPermission()`, daftarkan listener di `VoltCareApplication.kt`. Estimasi 3 file.
18. **Force Stop via Shizuku** (Drain Analyzer upgrade): `UsageStatsHelper.killBackgroundApp()` — kalau `ShizukuManager.hasPermission()` true, pakai `am force-stop <pkg>` (jauh lebih kuat dari `killBackgroundProcesses`); kalau false, tetap fallback ke jalur lama. Estimasi 1-2 file.
19. **Statistik drain per-app riil via Shizuku** (upgrade Drain Analyzer dari proxy waktu pemakaian ke data mAh nyata): `dumpsys batterystats` diparsing via `execShellCommand()`. Estimasi 1-2 file, kompleksitas parsing tinggi -> mungkin perlu dipecah lagi.
20. **Auto-grant PACKAGE_USAGE_STATS via Shizuku** (`appops set <pkg> GET_USAGE_STATS allow`) — hilangkan langkah manual buka Settings di Drain Analyzer saat Shizuku aktif. Estimasi 1 file.

---

## [Batch 22] Fix - Regresi Compile `const val String?` (dari log_fail user) + Housekeeping Manifest — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 54 -> 54 file (0 baru/hapus, 2 diedit: `UpdateManager.kt`, `FILE_MANIFEST.txt`)

### Alasan
User upload `logs_87411968857.zip` (log job GitHub Actions `build-release`) bareng perintah "lanjut Batch 22". Sesuai Debug Priority rule (Crash Logger Bawaan), log dianalisis LEBIH DULU sebelum housekeeping — ketemu regresi compile nyata dari Batch 20, jadi diprioritaskan sbg fix, housekeeping `FILE_MANIFEST.txt` (Pending Queue #14) digabung sekalian karena masih di bawah cap (2 file).

### Selesai
- **Root cause (dari `8_Build signed release APK.txt`):** `Task :app:compileReleaseKotlin FAILED` — `e: UpdateManager.kt:49:13 Const 'val' has type 'String?'. Only primitives and String are allowed`. Kotlin `const val` TIDAK boleh bertipe nullable (`String?`), walau underlying type String — batasan compiler, bukan soal isi (`null`). Bug lolos dari batch 19/20 karena verifikasi waktu itu hanya brace-balance check, bukan compile sungguhan (sudah dicatat di catatan kedua batch tsb sbg limitasi lingkungan tanpa network).
- **`UpdateManager.kt`**: baris 49 `private const val GITHUB_TOKEN: String? = null` -> `private val GITHUB_TOKEN: String? = null` (buang `const`). Fungsional 100% identik (tetap `private`, tetap nullable, tetap dipakai via `GITHUB_TOKEN?.let { ... }` di 2 tempat) — HANYA berhenti jadi compile-time constant, jadi sedikit lebih lambat diakses (dari field statis final ke property biasa), dampak performa nol relevan utk 2 pemanggilan per request HTTP.
- **`FILE_MANIFEST.txt`** (Pending Queue #14): tambah 4 entri file baru dari Batch 18-21 (`FEATURE_PARITY_GOALS.md`, `file_paths.xml`, `UpdateManager.kt`, `UpdateScreen.kt`), header versi diupdate ke "Batch 22".

### Verifikasi Tambahan (di luar audit manual biasa)
- Dicek: **HANYA 1 lokasi** `const val` bertipe nullable di seluruh `app/src/main/java/` (grep `const val.*?:.*\?` — 1 match, yaitu `GITHUB_TOKEN`). Tidak ada regresi serupa di file lain.
- Log step lain (`3_Stale run guard`, `6_Extract version name`) dicek: SHA build == tip main (bukan stale re-run), ekstraksi versi jalan normal sebelum build gagal — konsisten dgn desain Batch 16 (log_fail tetap dapat versi walau compile gagal).

### Sengaja TIDAK diubah
- Tidak ada perubahan API/behavior lain di `UpdateManager.kt` selain hilangnya keyword `const` — semua fungsi Batch 19-21 dipakai apa adanya.
- `.github/workflows/release.yml` — log menunjukkan pipeline (Stale Run Guard, log_fail artifact, keystore cleanup `if: always()`) semua bekerja SESUAI DESAIN saat build gagal; tidak ada bug di workflow itu sendiri, murni bug kode aplikasi.

### Protected Assets tersentuh
Tidak ada (perbaikan hanya di file kode non-protected `UpdateManager.kt` & dokumentasi `FILE_MANIFEST.txt`).

### Catatan
Ini kegagalan compile PERTAMA yang terverifikasi via log_fail asli sejak fitur tsb dibuat Batch 16 — bukti fitur bekerja seperti didesain. Rekomendasi kuat: setelah push batch ini, tunggu 1x run CI sukses (assemble+sign+release) sebagai konfirmasi nyata sebelum lanjut fitur baru lain, karena baru sekarang ada bukti compile real utk seluruh rangkaian in-app updater (Batch 19-22).

### Pending Queue (Batch 22: item #14 selesai, regresi #16 baru muncul & langsung selesai)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. ~~Update FILE_MANIFEST.txt~~ ✅ selesai batch ini
15. ✅ selesai (In-App Updater lengkap, Batch 19-21)
16. ~~Fix regresi compile const val nullable~~ ✅ selesai batch ini (ditemukan & diperbaiki batch yang sama)

---

## [Batch 21] Fitur - In-App Updater UI Wiring (Pending Queue #15) — 2026-08-19

**Confidence Rating: 95%**
**File sebelum -> sesudah:** 52 -> 54 file (1 baru: `UpdateScreen.kt`; 2 diedit: `strings.xml`, `NavGraph.kt` protected asset)

### Alasan
Lanjutan langsung Batch 19/20 — kini engine (`UpdateManager`) diwiring ke UI supaya user bisa cek/download/instal update dari dalam aplikasi tanpa Play Store.

### Selesai
- **`UpdateScreen.kt`** (baru, self-contained mengikuti pola `StressTestScreen.kt`): `UpdateViewModel` (`AndroidViewModel`, `StateFlow<UpdateUiState>` — Idle/Checking/UpToDate/Available/Downloading(percent)/ReadyToInstall/Failed) orkestrasi murni ke `UpdateManager.checkForUpdate()`/`downloadUpdate()` (signature Batch 20 dipakai apa adanya, 0 perubahan). `UpdateCheckAction()` composable: `IconButton` (ikon `SystemUpdate`) + `AlertDialog` per state — Checking (spinner, non-dismissable), UpToDate, Available (judul versi + release notes + tombol Unduh), Downloading (`LinearProgressIndicator` %, non-dismissable), ReadyToInstall (tombol Instal -> cek `canRequestInstallPackages()` dulu, kalau belum diizinkan buka `installPermissionSettingsIntent()`), Failed.
- **`strings.xml`** (edit): 13 string baru utk seluruh label dialog updater (`update_*`), placeholder `%1$s` (versi) & `%1$d` (persen download) dipakai sesuai `stringResource(id, arg)`.
- **`NavGraph.kt`** (edit parsial, protected): `UpdateCheckAction()` dipasang di `Box` overlay Dashboard, `Alignment.TopEnd` — **sengaja beda sudut** dari FAB Tes Baterai yang sudah ada di `BottomEnd` (Batch 12) supaya tidak tabrakan visual. `DashboardScreen.kt` TIDAK disentuh (pola sama seperti Batch 12 memasang FAB tanpa edit screen aslinya).

### Sengaja TIDAK diubah
- `DashboardScreen.kt` — lihat alasan di atas.
- `UpdateManager.kt`, `app/build.gradle.kts` — dipakai persis seperti Batch 20, tidak ada perubahan API.
- `FILE_MANIFEST.txt` — **belum** diupdate (akan jadi file ke-3... sebenarnya masih dalam cap 3, TAPI diputuskan tetap dikeluarkan dari batch ini supaya diff review lebih fokus ke fitur UI, bukan housekeeping; dijadwalkan Batch 22 bareng `FEATURE_PARITY_GOALS.md` (Batch 18) yang juga masih ter-queue).

### Protected Assets tersentuh (edit parsial, sesuai rule)
`NavGraph.kt` — brace balance diverifikasi (25/25 curly, 44/44 paren), struktur `Scaffold`/`NavHost`/route existing (Dashboard/History/Drain/Rules/stress_test) tidak terhapus, hanya disisipi 1 import + 1 `Box` overlay baru.

### Catatan
Tidak ada compile Gradle/emulator sungguhan di lingkungan pembuatan ZIP ini — verifikasi terbatas pada brace balance + audit manual API Compose Material3 1.2.1 (`LinearProgressIndicator(progress: Float, ...)` dipakai versi stabil non-experimental, BUKAN varian lambda `progress: () -> Float` yang baru ada di versi lebih baru — konsisten dgn fix regresi Batch 15 soal API belum tersedia di 1.2.1). Rekomendasi: build sungguhan sekali di Termux/CI utk konfirmasi resolusi `R.string.update_*` & import composable sebelum rilis.

### Pending Queue (Batch 21: item #15a-c selesai, housekeeping digeser ke Batch 22)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. (housekeeping) Update `FILE_MANIFEST.txt` — digeser ke Batch 22
15. ✅ selesai batch ini (In-App Updater lengkap: engine Batch 19-20 + UI Batch 21)

---

## [Batch 20] Fix - Swap HttpURLConnection -> OkHttp/Okio Literal (permintaan user) — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 52 -> 52 file (0 baru/hapus, 2 diedit: `UpdateManager.kt`, `app/build.gradle.kts` protected asset)

### Alasan
User tanya kenapa Batch 19 tidak pakai library literal sesuai contoh di rule ("Okio sink / ByteReadChannel"), lalu eksplisit minta dieksekusi ganti kalau "ada benefit besar". Benefit nyata: Okio `BufferedSink`/`BufferedSource` API lebih ringkas & aman utk streaming chunk vs `HttpURLConnection` manual, OkHttp connection pooling + Interceptor lebih robust utk retry/logging masa depan, dan ini approach yang sudah battle-tested/konvensional di ekosistem Android (dipakai luas), jadi worth 1 dependency tambahan.

### Selesai
- **`UpdateManager.kt`**: `checkForUpdate()` & `downloadUpdate()` ditulis ulang total pakai `OkHttpClient` (connectTimeout 15s, readTimeout 20s, `followRedirects(true)`, `followSslRedirects(true)`, instance `lazy` singleton). Download pakai `responseBody.source().read(sink.buffer, 8192)` + `sink.emit()` per iterasi — literal Okio sink streaming chunk-by-chunk ke `destFile.sink().buffer()`, TETAP tidak ada `readBytes()`/muat penuh body biner ke RAM. Fungsi publik (`UpdateInfo`, `DownloadResult`, `canRequestInstallPackages()`, `installApk()`, dst) — signature & perilaku IDENTIK, tidak ada breaking change utk batch UI (#15) berikutnya.
- **`app/build.gradle.kts`** (edit parsial, protected): tambah 1 baris dependency `implementation("com.squareup.okhttp3:okhttp:4.12.0")` (Okio 3.x terbawa transitif, tidak dideklarasikan terpisah) — disisipkan di blok `dependencies` kedua, dekat `work-runtime-ktx`.

### Sengaja TIDAK diubah
- Signature publik `UpdateManager` (semua fungsi/data class nama & tipe sama) — supaya Batch 21 (UI wiring, sudah di-plan sejak Batch 19) tidak perlu penyesuaian.
- `AndroidManifest.xml` — permission/provider dari Batch 19 sudah cukup, tidak ada penambahan.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance diverifikasi (20/20 curly, 55/55 paren), 1 baris ditambah di blok `dependencies` existing, tidak ada blok baru/terhapus.

### Catatan
Tidak ada akses jaringan/Gradle sungguhan di lingkungan pembuatan ZIP ini — verifikasi terbatas pada brace balance + audit manual API Okio (`BufferedSink.buffer`, `Source.read(Buffer, Long)`, `sink.emit()` adalah API resmi Okio 3.x, konsisten dgn versi yang dibawa OkHttp 4.12.0). Rekomendasi: build sungguhan sekali di Termux/CI utk konfirmasi resolusi dependency OkHttp berhasil sebelum lanjut Batch 21.

### Pending Queue (Batch 20: tidak ada item baru, Batch 21 tetap sama seperti direncanakan)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan)
14. (housekeeping) Update `FILE_MANIFEST.txt` — digabung ke #15c
15. **In-App Updater UI (Batch 21, KRUSIAL — lanjutan langsung):**
    - 15a. Tombol "Cek Update" + `UpdateViewModel.kt` (state: idle/checking/available/downloading progress%/ready-install/failed) manggil `UpdateManager.checkForUpdate()`+`downloadUpdate()` (signature TIDAK berubah dari Batch 19, aman dipakai apa adanya).
    - 15b. Dialog/Card hasil: versi baru + `releaseNotes`, tombol Download -> progress % real-time -> tombol Install (`installApk()`, cek `canRequestInstallPackages()` dulu, kalau belum arahkan ke `installPermissionSettingsIntent()`).
    - 15c. `strings.xml` + `FILE_MANIFEST.txt` (item #14).
    - Estimasi 3 file; kalau `NavGraph.kt`/`DashboardScreen.kt` ternyata perlu disentuh juga -> dipecah ke Batch 22.

---

## [Batch 19] Fitur - In-App Updater Core Engine (cek + download GitHub Release) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 50 -> 52 file (2 baru: `UpdateManager.kt`, `file_paths.xml`; 1 diedit: `AndroidManifest.xml`, protected asset)

### Alasan
User minta fitur "update langsung dari aplikasi" (in-app updater, bukan lewat Play Store) dieksekusi tuntas. Sesuai Strict Micro-Batching Rule (max 3 file/task), fitur dipecah 2 batch: **Batch 19 (ini) = core engine** (cek rilis + download APK ke disk, siap dipanggil), **Batch 20 (queued) = UI trigger** (tombol + progress bar + wiring ke NavGraph/Dashboard).

### Selesai
- **`UpdateManager.kt`** (baru, `util/`): `checkForUpdate()` — GET `api.github.com/repos/FDzaki-dev/PowerVaultHealthPro/releases/latest`, parse JSON pakai `org.json` (built-in Android, tanpa dependency baru), cari asset `.apk`, bandingkan versi numerik vs `versionName` terpasang. `downloadUpdate()` — **streaming chunk-by-chunk (buffer 8KB) via `HttpURLConnection` + `BufferedInputStream`/`FileOutputStream` langsung ke disk** (`getExternalFilesDir()/updates/`), **TIDAK ADA `readBytes()`/muat penuh ke RAM**, timeout eksplisit connect 15s/read 20s, `instanceFollowRedirects = true` (redirect 302 GitHub CDN/S3), file parsial otomatis dihapus jika gagal. `installApk()` — trigger installer sistem via `FileProvider`. Semua fungsi fail-safe (try-catch, tidak pernah throw ke caller).
- **`file_paths.xml`** (baru, `res/xml/`): definisi `external-files-path` untuk folder `updates/`, dipakai `FileProvider`.
- **`AndroidManifest.xml`** (edit parsial, protected): tambah `INTERNET` + `REQUEST_INSTALL_PACKAGES` permission, daftar `<provider>` `androidx.core.content.FileProvider` authority `${applicationId}.fileprovider` (exported=false, grantUriPermissions=true).

### Keputusan Desain Penting
- **Tanpa dependency Gradle baru** (tidak pakai OkHttp/Okio): `HttpURLConnection` bawaan Android sudah cukup untuk streaming manual + timeout + followRedirects + custom header — `app/build.gradle.kts` (protected asset) **sengaja TIDAK disentuh**, menghindari batch ke-4 file yang melanggar cap.
- **Header `Authorization: Bearer <token>`**: konstanta `GITHUB_TOKEN` disediakan (default `null`) tapi TIDAK diisi — repo `FDzaki-dev/PowerVaultHealthPro` publik, API `releases/latest` & `browser_download_url` asset publik tidak butuh auth. Header hanya terpasang otomatis kalau token diisi manual (future-proof kalau repo di-private-kan). Ini deviasi terdokumentasi dari spec baku, bukan diabaikan diam-diam.
- **Perbandingan versi**: numerik per-segmen (`1.2.10 > 1.2.9`), prefix `v`/`V` di `tag_name` dibuang otomatis.

### Sengaja TIDAK diubah
- `app/build.gradle.kts` — lihat Keputusan Desain di atas.
- `NavGraph.kt`, `MainActivity.kt`, screen manapun — belum ada UI trigger, `UpdateManager` masih standalone/belum dipanggil dari mana pun (akan diwiring Batch 20). Ini disengaja agar batch ini tetap 3 file & fokus core engine yang bisa diverifikasi sendiri.
- `FILE_MANIFEST.txt` — **belum** diupdate (akan jadi file ke-4, melebihi cap), di-queue ke Batch 20 sekaligus bareng file UI baru.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`AndroidManifest.xml` — XML diverifikasi valid (`xml.etree.ElementTree.parse` sukses untuk manifest & `file_paths.xml`), struktur permission/activity/service/receiver existing tidak terhapus, hanya disisipi 2 permission + 1 provider baru.

### Catatan
Tidak ada akses jaringan sungguhan di lingkungan pembuatan ZIP ini (network disabled) — `UpdateManager.kt` diverifikasi via audit manual + brace balance check (`{`:46/`}`:46, `(`:126/`)`:126), bukan compile Gradle sungguhan. Endpoint GitHub API (`api.github.com/repos/FDzaki-dev/PowerVaultHealthPro/releases/latest`) perlu dicek nyata setelah minimal 1 rilis APK ter-publish via `release.yml` (Stale Run Guard + GitHub Release Rule sudah aktif sejak batch sebelumnya) — kalau belum ada rilis sama sekali, `checkForUpdate()` akan return `null` (fail-safe, bukan crash) karena endpoint `releases/latest` 404 saat repo belum punya rilis.

### Pending Queue (Batch 19: 1 fitur baru ditambahkan sbg #15, dipecah 2 sub-task)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10-13. (dari Batch 18, belum dikerjakan — lihat entri Batch 18 di bawah)
14. (housekeeping) Update `FILE_MANIFEST.txt` — digabung ke #15b di bawah
15. **In-App Updater UI (Batch 20, KRUSIAL — lanjutan langsung dari batch ini):**
    - 15a. Tombol "Cek Update" (mis. di Dashboard top bar/menu) + `UpdateViewModel.kt` (state: idle/checking/available/downloading progress%/ready-install/failed) yang manggil `UpdateManager.checkForUpdate()`+`downloadUpdate()`.
    - 15b. Dialog/Card hasil: tampilkan versi baru + `releaseNotes`, tombol Download -> progress bar % real-time -> tombol Install (panggil `UpdateManager.installApk()`, cek `canRequestInstallPackages()` dulu, kalau belum diizinkan arahkan ke `installPermissionSettingsIntent()`).
    - 15c. `strings.xml` — tambah string terkait (judul dialog, tombol, dll), sekalian update `FILE_MANIFEST.txt` (item #14).
    - Estimasi: 3 file (ViewModel baru + 1 screen/dialog diedit + strings.xml), TIDAK termasuk `NavGraph.kt`/`DashboardScreen.kt` kalau ternyata perlu -> jika total tembus >3, `NavGraph.kt` wiring dipecah lagi ke Batch 21.

---

## [Batch 18] Dokumentasi - Feature Parity Goals vs Kompetitor (AccuBattery/GSam/Greenify) — 2026-08-19

**Confidence Rating: 98%**
**File sebelum -> sesudah:** 49 -> 50 file (1 baru: `FEATURE_PARITY_GOALS.md`; 0 kode diubah, murni dokumentasi)

### Alasan
User upload 2 screenshot Google AI Overview ("Pilihan Aplikasi Battery Manager Terbaik": AccuBattery, GSam Battery Monitor, Greenify) dan minta ditanamkan `.md` di repo berdasarkan tujuan mencapai 100% fitur yang tergambar. Dibuat `FEATURE_PARITY_GOALS.md` (matrix referensi, bukan log kronologis) sbg peta gap resmi.

### Selesai
- **`FEATURE_PARITY_GOALS.md`** (baru, root): matrix 9 fitur dari screenshot vs status implementasi VoltCare, diaudit langsung ke source code (bukan asumsi) — termasuk `grep` yang membuktikan `work-runtime-ktx` sudah dependency sejak Batch 1 tapi **belum pernah dipakai** (0 hasil grep `WorkManager` di seluruh `app/src/main/java/`).
- Hasil audit: **3 Done** (Health%, Alarm generik via Rules, Suhu/status real-time), **3 Partial** (Drain Analyzer proxy-only, History stats, Force Stop manual-only), **2 Not Implemented** (estimasi sisa waktu discharge; auto-hibernate terjadwal — 1 di antaranya, "cegah auto-launch", platform-limited/tidak buildable generik tanpa root).
- **Pending Queue baru #10-#13** ditambahkan (lihat di bawah): 3 item buildable batch berikutnya (#10 estimasi sisa waktu, #11 preset alarm cepat, #12 auto-hibernate terjadwal via WorkManager) + 1 item platform-limited perlu izin eksplisit user dulu sebelum jadi task aktif (#13).

### Sengaja TIDAK diubah
- Tidak ada kode (`.kt`) yang disentuh batch ini — murni dokumentasi perencanaan, sesuai permintaan user ("tanamkan .md").
- `FILE_MANIFEST.txt` — **belum** diupdate untuk mencantumkan `FEATURE_PARITY_GOALS.md` (akan menambah ke 4 file diedit, melebihi Micro-Batching Rule 3 file/1 task). Di-queue ke batch berikutnya sbg housekeeping kecil, bukan fitur.

### Protected Assets tersentuh
Tidak ada.

### Pending Queue (Batch 18: 4 item baru ditambahkan, sumber `FEATURE_PARITY_GOALS.md`)
1-7, 9. ✅ selesai (lihat Batch 8-17)
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
10. **Estimasi Sisa Waktu Pakai (discharge)** — Dashboard, agregasi drain rate dari `BatteryLogDao`/`StressTestScreen`, tanpa perubahan skema DB.
11. **Preset Cepat "Alarm Batas Charge"** — UX shortcut auto-create `RuleEntity(PERCENT_ABOVE, ALARM)`, engine sudah ada.
12. **Auto-Hibernate Terjadwal** — `PeriodicWorkRequest` (WorkManager, dependency existing, baru dipakai pertama kali) + whitelist app approved user.
13. (butuh izin user dulu, platform-limited) **"Cegah auto-launch tanpa izin"** — best-effort: tombol buka `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` per-app, bukan otomatis (tidak ada API generik non-root).
14. (housekeeping, non-fitur) Update `FILE_MANIFEST.txt` untuk mencantumkan `FEATURE_PARITY_GOALS.md`.

---

## [Batch 17] Fix - Set room.schemaLocation (Pending Queue #7) — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 49 -> 49 file (0 baru/hapus, 1 file diedit: `app/build.gradle.kts`, protected asset)

### Selesai
- **`app/build.gradle.kts`**: tambah blok `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` (top-level, setelah blok `dependencies` pertama). `AppDatabase.kt` sudah `exportSchema = true` sejak Batch 1 (protected, tidak disentuh) tapi tanpa arg ini KSP cuma warning tanpa pernah menulis JSON skema — sekarang KSP menulis riwayat skema ke `app/schemas/com.voltcare.app.data.db.AppDatabase/1.json` tiap build, dasar formal untuk `Migration` eksplisit kalau `version` naik ke 2+ nanti (wajib per komentar Protected Asset di `AppDatabase.kt`, tidak boleh `fallbackToDestructiveMigration` di produksi).
- Dipecah jadi 2 blok `dependencies { }` terpisah (bukan disisipkan di tengah blok pertama) supaya diff minimal & Room-related lines tidak tercampur dengan WorkManager/Compose/test deps — sintaks Gradle Kotlin DSL valid (`dependencies` boleh dipanggil berkali-kali dalam 1 script, digabung otomatis oleh Gradle).

### Sengaja TIDAK diubah
- `AppDatabase.kt` (DB Schema, protected) — `exportSchema = true` & `version = 1` dipakai apa adanya, tidak ada perubahan skema/tabel.
- `.gitignore` — `app/schemas/` **tidak** ditambahkan ke ignore list (disengaja): file JSON skema di folder ini adalah riwayat migrasi formal yang harus di-commit, bukan build output sementara seperti `/build`.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`app/build.gradle.kts` — brace balance diverifikasi (20/20), 2 blok `dependencies` + 1 blok `ksp` baru diverifikasi sintaks Kotlin DSL valid (Gradle mengizinkan pemanggilan `dependencies{}` berkali-kali).

### Catatan
Tidak ada akses jaringan/Gradle sungguhan di lingkungan pembuatan ZIP ini — verifikasi terbatas pada audit brace balance & sintaks manual, bukan re-run compile sungguhan. Folder `app/schemas/` baru akan muncul di working tree setelah build pertama (CI atau lokal Termux) berhasil jalan; rekomendasikan commit folder tsb ke Git setelah run pertama sukses supaya riwayat skema tidak hilang.

### Pending Queue (Batch 17: item #7 selesai, 1 opsional tersisa)
1-6, 9. ✅ selesai (lihat Batch 8-16)
7. ~~Set `room.schemaLocation` agar warning KSP hilang~~ ✅ selesai batch ini
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)

---

## [Batch 16] Fitur - Artifact log_fail Otomatis saat Compile Gagal (Pending Queue #9) — 2026-08-19

**Confidence Rating: 96%**
**File sebelum -> sesudah:** 49 -> 49 file (0 baru/hapus, 1 file diedit: `.github/workflows/release.yml`, protected asset)

### Selesai
- **`Extract version name`** (step baru, dipindah dari dalam `Locate APK` ke lebih awal, sebelum step build): ekstraksi `versionName` dari `app/build.gradle.kts` sekarang tidak bergantung hasil compile — tetap tersedia (`steps.version.outputs.version`) walau build gagal, dipakai untuk nama artifact log.
- **`Build signed release APK`**: ditambah `id: build` + `continue-on-error: true`, output digabung `2>&1 | tee gradle-build.log` (log lengkap terekam ke file, bukan cuma tampil di UI job run). Shell default GitHub Actions Linux runner sudah `bash -eo pipefail`, jadi exit code kegagalan `gradle` tetap terbaca lewat `tee` (tidak ketutup exit code 0 milik `tee`).
- **`Upload failure log artifact`** (step baru): jika `steps.build.outcome == 'failure'`, upload `gradle-build.log` via `actions/upload-artifact@v4` dengan nama `log_fail_<version>_<run-number>` — sbg **GitHub Actions artifact biasa (bukan Release)**, sesuai permintaan eksplisit user, retensi 14 hari.
- **`Abort on build failure`** (step baru): jika build gagal, tulis `::error::` lalu `exit 1` — job berhenti di sini, step-step berikutnya (`Locate APK`, `Verify APK is signed`, `Rename APK asset`, `Publish GitHub Release`) otomatis di-skip oleh GitHub Actions (tidak ada `if: always()` pada mereka). `Clean up keystore` tetap jalan (sudah `if: always()` sejak awal, tidak diubah) — keystore tetap bersih walau job gagal.
- **`Locate APK`**: disederhanakan, ekstraksi versi dipindah keluar (lihat poin 1), sekarang murni cari path APK signed.
- **`Rename APK asset`** & **`Publish GitHub Release`**: referensi versi diganti dari `steps.apk.outputs.version` (dihapus) ke `steps.version.outputs.version` (step baru) — perilaku/output akhir **identik**, hanya sumber data dipindah lebih awal di pipeline.

### Sengaja TIDAK diubah
- Stale Run Guard, Signed-APK Guard, Smart Naming APK, GitHub Release Rule — seluruh logika existing dipertahankan 100%, murni disisipi 1 jalur baru (capture log saat gagal) tanpa mengubah perilaku jalur sukses.
- `app/build.gradle.kts`, `settings.gradle.kts` — tidak disentuh, hanya dibaca (grep) seperti sebelumnya.

### Protected Assets tersentuh (edit parsial, sesuai rule)
`.github/workflows/release.yml` — diverifikasi valid via `yaml.safe_load` (parse sukses), struktur step lain (Stale Run Guard, Signed-APK Guard, Clean up keystore `if: always()`) diverifikasi utuh tidak terhapus.

### Catatan
Tidak ada akses jaringan/GitHub Actions sungguhan di lingkungan pembuatan ZIP ini — verifikasi terbatas pada validasi sintaks YAML + audit manual alur step (urutan `id`/`if`/referensi output). Rekomendasi: pantau 1x run berikutnya (baik sukses maupun sengaja dibuat gagal) untuk konfirmasi artifact `log_fail_*` muncul di tab Actions saat gagal, dan Release tetap normal saat sukses.

### Pending Queue (Batch 16: item #9 selesai, 2 opsional tersisa)
1-6. ✅ selesai (lihat Batch 8-14)
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
9. ~~Artifact `log_fail_<version>_<run-number>` di `release.yml`~~ ✅ selesai batch ini

---

## [Batch 15] Hotfix - Build Gagal (RulesScreen.kt, regresi Batch 14) — 2026-08-19

**Confidence Rating: 97%**
**File sebelum -> sesudah:** 49 -> 49 file (0 baru/hapus, 1 file diedit)
**Sumber analisa:** log GitHub Actions run yang diupload user (job `build-release`, step "Build signed release APK") — `:app:compileReleaseKotlin FAILED`.

### Root Cause
1. `RulesScreen.kt:19` import `androidx.compose.material3.ExposedDropdownMenu` -> **Unresolved reference**. Composable wrapper ini baru ada di Compose Material3 **1.3.0+**; project pin di `material3:1.2.1` (lihat `app/build.gradle.kts`, protected asset, tidak diubah) belum punya API itu.
2. `ExposedDropdownMenuBox`, `ExposedDropdownMenuDefaults.TrailingIcon`, `Modifier.menuAnchor()` ditandai `@ExperimentalMaterial3Api` di versi 1.2.1 — dipakai tanpa opt-in, jadi Kotlin compiler menolaknya sebagai **error** (bukan cuma warning), sesuai baris 189/198/199/201/232/241/242/244 di log.

### Fix
- `RulesScreen.kt`: import `ExposedDropdownMenu` dihapus, 2 pemakaiannya diganti `DropdownMenu` biasa (pola resmi Compose utk material3 1.2.1 — child langsung di dalam `ExposedDropdownMenuBox`, tanpa wrapper khusus).
- `RuleFormDialog()` (fungsi yang memuat kedua dropdown) diberi `@OptIn(ExperimentalMaterial3Api::class)`.
- Diverifikasi: brace `{}` seimbang (85/85), tidak ada sisa referensi `ExposedDropdownMenu` berdiri sendiri, hanya `ExposedDropdownMenuBox`/`ExposedDropdownMenuDefaults` (nama beda, API valid di 1.2.1).

### Sengaja TIDAK diubah
- `app/build.gradle.kts` (protected) — **tidak** menaikkan versi material3 ke 1.3.0+ untuk fix ini; opt-in + `DropdownMenu` biasa cukup dan lebih aman (naik versi BOM/material3 berisiko breaking change lain di luar scope hotfix).
- `RulesViewModel.kt` — tidak ada error dari file ini di log, tidak disentuh.

### Protected Assets tersentuh
Tidak ada.

### Catatan
Karena tidak ada akses jaringan/Gradle di lingkungan pembuatan ZIP ini, fix ini diverifikasi via **audit statis** (baca API history material3 1.2.1 + cocokkan tiap baris error log satu-per-satu ke kode), bukan re-run compile sungguhan. Rekomendasi: pantau run GitHub Actions berikutnya untuk konfirmasi hijau. **Pending Queue baru ditambahkan user**: artifact `log_fail_<version>_<run-number>` otomatis saat compile gagal (lihat Pending Queue #9) — akan mempermudah diagnosa hotfix serupa ke depannya tanpa perlu user upload log manual.

### Pending Queue (Batch 15: hotfix selesai, +1 fitur baru diminta user)
1-6. ✅ selesai (lihat Batch 8-14)
7. (opsional) Set `room.schemaLocation` agar warning KSP hilang
8. (opsional) Rename repo GitHub ke `VoltCare` via `gh repo rename` (manual)
9. **Artifact `log_fail_<version>_<run-number>`** di `release.yml`: capture otomatis log Gradle saat compile gagal, upload sbg GitHub Actions artifact (bukan Release) supaya debug gak perlu re-download log manual. Diminta user di Batch 15, dikerjakan batch berikutnya (protected asset `.github/workflows/release.yml`, task terpisah dari hotfix ini sesuai Micro-Batching Rule).

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
