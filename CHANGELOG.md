# CHANGELOG.md
(Urutan DESCENDING - entri terbaru di paling atas)

## [v1.0.47-batch84] - 2026-08-30
### Fixed
- Notifikasi bar monitoring persisten ("Memantau baterai...") kini ikut kebal saat proses app dikill total: `AlarmCheckReceiver.kt` (safety net independen-proses, fire tiap ~60 detik) sekarang juga "ping" `BatteryMonitorService` via `startForegroundService()` - no-op kalau service masih hidup, restart otomatis (notifikasi pulih) kalau ternyata sudah mati, tanpa perlu user buka app manual.

## [v1.0.46-batch83] - 2026-08-30
### Fixed
- Root cause reminder notifikasi (tab Aturan) tidak trigger setelah app dikill: `AlarmCheckReceiver.kt` (safety net independen-proses) diam-diam skip total rule beraksi "Notifikasi saja" (NOTIFY), hanya rule ALARM yang pernah dievaluasi. Sekarang semua rule aktif dievaluasi & notifikasi selalu diposting saat kondisi terpenuhi.
### Changed
- Rombak tab Riwayat: grafik dapat label sumbu-X/Y + gridline (Pending Queue #35 resolved), rentang Y dinamis (bukan 0-100 tetap - dulu bikin grafik Health% terlihat rata kosong), agregasi adaptif per jam/hari, kartu ringkasan direword ke bahasa awam + status berwarna, insight kalimat polos, label rentang data mengikuti data riil (bukan "30 hari" statis).

## [v1.0.45-batch82] - 2026-08-21
### Added
- Restyle iOS/Cupertino tahap 1/N (fondasi): `Color.kt` diganti ke token Apple HIG System Colors (nama variabel lama dipertahankan, cuma nilai hex diperbarui). `Theme.kt`: ColorScheme M3 lengkap (semua "on*" eksplisit) + `VcShapes` (sudut membulat generous ala Cupertino, efek otomatis ke Card/Dialog/TextField semua layar).
### Fixed
- Pending Queue #32 (HIGH, `UX_AUDIT.md` Batch 80): kontras onPrimary/onSecondary/onError gagal WCAG AA (~2.1:1, teks putih M3 default) - diganti teks gelap (`VcOnAccent`), diverifikasi matematis lolos AA (5.2-9.1:1) di semua 6 kombinasi warna x mode.

## [v1.0.44-batch81] - 2026-08-21
### Fixed
- Pending Queue #31 (HIGH, dari `UX_AUDIT.md` Batch 80): label lingkaran day-picker `RulesScreen.kt` (`M/S/S/R/K/J/S`) - 3 huruf "S" (Senin/Selasa/Sabtu) tak terbedakan. Diganti 2 huruf unik: `Mi/Sn/Sl/Rb/Km/Jm/Sb` + `labelSmall` biar tetap muat rapi di lingkaran 32dp.

## [v1.0.43-batch80] - 2026-08-21
### Docs
- Audit UX mendalam 100% (audit-only, 0 kode diubah). Temuan lengkap: `UX_AUDIT.md` (baru). 2 HIGH, 3 MEDIUM, 3 LOW — semua masuk Pending Queue #31-#37.

## [v1.0.42-batch79] - 2026-08-21
### Fixed
- Docs-only: `FEATURE_PARITY_GOALS.md` desync lagi — item #3 "estimasi sisa waktu pakai" masih ❌ padahal sudah ✅ selesai sejak Batch 43. Tidak ada perubahan kode/APK.

## [v1.0.41-batch78] - 2026-08-21
### Fixed
- Pending Queue #27 (lama, akhirnya diverifikasi & RESOLVED): `RulesViewModel.saveRule()` hardcode `isEnabled = true` - rule yang di-nonaktifin manual via Switch list ke-reset AKTIF lagi tiap kali di-edit & tekan Simpan. Fix: param baru `existingEnabled` dikirim dari `editingRule?.isEnabled` di call-site.

## [v1.0.40-batch77] - 2026-08-21
### Added
- Row "Hari Aktif" kini nampilin ringkasan teks di bawah circle toggle: "Aktif setiap hari" / "Aktif: Senin, Selasa, ..." / warning merah kalau 0 hari dipilih (bakal auto-fallback ke semua hari saat disimpan). Sebelumnya circle doang tanpa feedback teks, user gak yakin status tersimpan.

## [v1.0.39-batch76] - 2026-08-21
### Fixed
- Regresi Batch 75 belum tuntas (screenshot user): Switch di row "Ulangi terus..." masih overflow lewat border kanan dialog krn Text row itu gak punya `weight(1f)` (diukur lebar penuh duluan saat wrap 2 baris, dorong Switch keluar). Fix: `weight(1f)` di Text + `fillMaxWidth()` defensif di TextButton "Nada Alarm" & Column pembungkus.

## [v1.0.38-batch75] - 2026-08-21
### Fixed
- Dialog `RuleFormDialog` (tab Aturan) overflow/truncation di layar pendek sejak konten nambah panjang (activeDays, Batch 74) - `Column` body gak scroll, tabrakan sama tombol Simpan/Batal & row Hari Aktif kepotong (screenshot user). Fix: `verticalScroll` + `heightIn(max=480.dp)`.

## [v1.0.37-batch74] - 2026-08-21
### Added
- Pending Queue #30 tuntas: wiring `activeDays` end-to-end. UI toggle 7 lingkaran hari (M/S/S/R/K/J/S, mirip Google Clock) di `RuleFormDialog`, `RulesViewModel.saveRule()` param baru, `AlarmCheckReceiver.kt` (safety net) tambah cek hari sama persis dgn `BatteryMonitorService.checkRule()` - konsisten, tidak ada window inkonsistensi.

## [v1.0.36-batch73] - 2026-08-21
### Added
- Jadwal hari aktif rule mirip Google Clock (core engine): `RuleEntity.kt` +kolom `activeDays` (Migration 3->4, non-destruktif, default semua hari). `BatteryMonitorService.kt` `checkRule()` skip hari tidak aktif. Belum diwiring ke UI/safety net (default = perilaku lama tidak berubah). Lihat `PROJECT_STATE.md` Batch 73.
### Queued
- #30: UI toggle 7 hari di `RuleFormDialog` + `RulesViewModel.saveRule()` + `AlarmCheckReceiver.kt` (safety net) - wajib tuntas sekaligus.

## [v1.0.35-batch72] - 2026-08-21
### Added
- Prompt eksplisit izin exact alarm (Pending Queue #29): `MainActivity.kt` `requestExactAlarmPermission()` buka `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` kalau belum granted. Re-prompt tiap launch (bukan sekali saja) - jaga kalau user cabut izin manual. Lihat `PROJECT_STATE.md` Batch 72.

## [v1.0.34-batch71] - 2026-08-21
### Added
- Safety net independen proses: `AlarmCheckReceiver.kt` (baru) dipicu `AlarmManager.setExactAndAllowWhileIdle()`, tidak bergantung service tetap hidup. Wiring `BatteryMonitorService.kt` + permission/registrasi `AndroidManifest.xml`. Lihat `PROJECT_STATE.md` Batch 71.
### Queued
- #29: prompt eksplisit izin `SCHEDULE_EXACT_ALARM` (skrng silent fallback ke inexact).

## [v1.0.33-batch70] - 2026-08-21
### Fixed
- Suara alarm terpotong walau notifikasi tetap tampil: `AlarmPlayer.kt` tidak pernah acquire wake lock, CPU suspend saat Doze/layar mati memotong playback. Tambah `PARTIAL_WAKE_LOCK` (timeout 5 menit) di `play()`/`stop()`. Lihat `PROJECT_STATE.md` Batch 70.

## [v1.0.32-batch69] - 2026-08-21
### Fixed
- Force-stop masih terjadi setelah battery optimization exemption: gap OEM Autostart Manager (di luar API standar Android). `AutostartHelper.kt` (baru) + `MainActivity.kt` `promptAutostartIfNeeded()` (sekali, via SharedPreferences flag). Limitasi jujur: ComponentName OEM bisa berubah/tidak akurat 100%, fallback ke App Details settings. Lihat `PROJECT_STATE.md` Batch 69.

## [v1.0.31-batch68] - 2026-08-21
### Fixed
- Klaim force-stop Batch 64 tidak lengkap: `MainActivity.kt` +`requestIgnoreBatteryOptimization()` (exemption OEM battery manager via `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), `AndroidManifest.xml` +permission. Limitasi: OEM Autostart Manager tetap butuh aktivasi manual user, tidak ada API publik. Lihat `PROJECT_STATE.md` Batch 68.

## [v1.0.30-batch67] - 2026-08-21
### Added
- Wiring alarm loop end-to-end (Pending Queue #28): Switch "Ulangi terus sampai dimatikan manual" di `RuleFormDialog` (`RulesScreen.kt`), `RulesViewModel.saveRule()` +param `alarmLoop`, `BatteryMonitorService.fireAlert()` teruskan ke `AlarmPlayer.play()`. Lihat `PROJECT_STATE.md` Batch 67.

## [v1.0.29-batch66] - 2026-08-21
### Added
- **Opsi Loop Alarm (core engine)**: `RuleEntity.kt` +kolom `alarmLoop` (Migration 2->3, non-destruktif). `AlarmPlayer.kt` `play()` +param `loop` - nada+getar diulang terus sampai `stop()` dipanggil manual. Belum diwiring ke form Aturan/service (default false, perilaku existing tidak berubah).

## [v1.0.28-batch65] - 2026-08-21
### Fixed
- Konfirmasi: "alarm loop/reset sebelum lagu kelar saat charging" = root cause #2 yang sudah selesai di Batch 64 (edge-triggered firing). Tidak ada kode baru.
### Changed
- **Revisi rule versioning**: `versionCode` di `app/build.gradle.kts` kini auto dari `GITHUB_RUN_NUMBER` (tidak manual lagi). `versionName` bump hanya saat ada perubahan nyata yg di-present ke user.
### Process
- Mulai batch ini, tiap `present_files` ZIP WAJIB disertai `versionName` + daftar fitur/fix yg di-present di balasan chat.

## [v1.0.27-batch64] - 2026-08-21
### Fixed
- **Alarm Reliability (3 root cause sekaligus, terverifikasi source)**: (1) service mati saat app di-swipe dari Recents -> `onTaskRemoved()` restart + manifest `stopWithTask="false"`; (2) alarm looping selama charger belum dicopot -> `checkRule()` kini edge-triggered (`firedRuleIds`, fire 1x per episode, re-arm otomatis saat kondisi reset); (3) tidak ada cara batalkan alarm -> tombol notifikasi "Matikan Alarm" (`ACTION_DISMISS_ALARM`) stop suara/getar saat itu juga.
### Note
- Beberapa OEM (Xiaomi/Oppo/Vivo/Samsung) tetap bisa membunuh background service via battery manager sendiri di luar kendali kode - perlu izin "Autostart" manual dari user di device tsb.

## [v1.0.26-batch63] - 2026-08-21
### Fixed
- **Root cause "alarm gak ke-trigger"**: default `requireCharging` di form Aturan selalu `true` termasuk utk kondisi "Persen di bawah" (baterai lemah) - kombinasi "lemah SAAT charging" nyaris mustahil terpenuhi. Default kini kontekstual (`PERCENT_BELOW`->false) + peringatan amber inline saat kombinasi kontradiktif. Audit eksternal (klaim Handler/BroadcastReceiver/AlarmManager) ditolak - tidak cocok arsitektur nyata (Service+coroutine).
### Note
- Aturan lama dgn kombinasi jebakan ini TIDAK auto-migrasi - buka Edit manual & matikan switch "Hanya saat charging" kalau perlu.

## [v1.0.25-batch62] - 2026-08-20
### Fixed
- Docs-only: `FEATURE_PARITY_GOALS.md` desync — item #9 "Hemat daya otomatis" masih ditandai ❌ padahal sudah ✅ selesai sejak Batch 44 (Auto-Hibernate Terjadwal). Tidak ada perubahan kode/APK.

## [v1.0.24-batch61] - 2026-08-20
### Fixed
- Tombol nada alarm cuma nampilin "Custom terpilih ✓" generik, judul sound asli gak kebaca. Sekarang `RuleFormDialog` resolve judul via `RingtoneManager.getRingtone(context, uri).getTitle(context)` (LaunchedEffect, fallback "Custom" kalau gagal resolve). Lihat `PROJECT_STATE.md` Batch 61.

## [v1.0.23-batch60] - 2026-08-20
### Added
- UI pilih nada alarm custom (Pending Queue #26): tombol di `RuleFormDialog` (`RulesScreen.kt`) buka `RingtoneManager.ACTION_RINGTONE_PICKER`, tersimpan via `RulesViewModel.saveRule()` ke `RuleEntity.alarmSoundUri`. Lihat `PROJECT_STATE.md` Batch 60.
### Queued
- Bug `isEnabled` ke-reset `true` saat edit rule — Pending Queue #27.

## [v1.0.23-batch59] - 2026-08-20
### Fixed
- Alarm tidak bunyi saat threshold rule tercapai: wiring `AlarmPlayer.play()` ke `BatteryMonitorService.fireAlert()` (sebelumnya cuma posting notifikasi pasif). Lihat `PROJECT_STATE.md` Batch 59.

## [v1.0.23-batch58] - 2026-08-20
### Added
- **Custom Alarm (core engine)**: `RuleEntity.kt` +kolom `alarmSoundUri` (nullable, DB Migration 1->2 non-destruktif di `AppDatabase.kt`). `AlarmPlayer.kt` (baru) - putar nada alarm custom/default sistem + getar, fail-safe. Belum diwiring ke `BatteryMonitorService`/UI Aturan.
### Queued
- Wiring `AlarmPlayer` ke aksi ALARM di service pemantau, tombol pilih nada custom di form Aturan — Pending Queue #25-26 di `PROJECT_STATE.md`.

## [v1.0.22-batch57] - 2026-08-20
### Fixed
- 3 bug P0 dari audit UX eksternal (diverifikasi manual thd source code dulu, bukan trust dokumen): (1) `DashboardScreen.kt` tidak scrollable -> tambah `verticalScroll`; (2) `RulesScreen.kt` pola Column+nested-LazyColumn sama persis dgn bug `DrainScreen.kt` -> diflatkan jadi 1 LazyColumn (tanpa import `item` yang salah, pelajaran dari Batch 56); (3) `StressTestScreen.kt` WakeLock diambil sejak layar dibuka (bukan saat tes mulai) -> dipindah ke `LaunchedEffect(testState==RUNNING)`.
### Rejected
- Item P1 audit (copy/tooltip/onboarding) sengaja tidak dikerjakan - bukan bug terverifikasi, di luar scope "debugging" yang diminta user. Dicatat sbg Pending Queue #21-25 (opsional, perlu diminta eksplisit).

## [v1.0.21-batch56] - 2026-08-20
### Fixed
- **Regresi Batch 55**: build CI gagal (`compileReleaseKotlin FAILED: Unresolved reference: item`) krn import `androidx.compose.foundation.lazy.item` yang tidak valid (`item{}` itu member function `LazyListScope`, bukan top-level import spt `items`). Baris import salah dihapus dari `DrainScreen.kt`. Ditemukan dari log GitHub Actions yang di-upload user (guard Batch 4 mencegah APK rusak ke-publish, sesuai desain).

## [v1.0.20-batch55] - 2026-08-20
### Fixed
- Drain Analyzer "kurang fleksibel dan scrollable" (laporan user via screenshot): `DrainScreen.kt` sebelumnya `Column` non-scroll berisi `LazyColumn` bersarang tanpa weight, konten bisa ke-clip di bawah tanpa cara scroll (makin mungkin sejak toggle "Semua App" Batch 54 bisa nampilin sampai 50 app). Diganti jadi SATU `LazyColumn` datar (`item{}` untuk header/card, `items(apps){}` untuk daftar) — pola idiomatic Compose, seluruh layar sekarang scroll normal.

## [v1.0.19-batch54] - 2026-08-20
### Added
- Mode "Tampilkan Semua App" di Drain Analyzer (permintaan user): toggle baru menampilkan SEMUA app dgn data mAh riil dari dumpsys, tidak dibatasi top-15 waktu pemakaian. `UsageStatsHelper.fullDrainAppList()` + `rawForegroundMsByPackage()` (baru), `DrainScreen.kt` dapat Card toggle + gating izin disesuaikan.

## [v1.0.18-batch53] - 2026-08-20
### Confirmed
- Pending #19 ditutup 100%: screenshot device nyata (Shizuku aktif) konfirmasi kolom mAh riil tampil benar di Drain Analyzer, UI tidak freeze, hint teks sesuai.

## [v1.0.17-batch52] - 2026-08-20
### Added
- **Pending #19 SELESAI (2/2)**: wiring `BatteryStatsParser` + `ShizukuManager` ke Drain Analyzer. `UsageStatsHelper.AppUsageInfo` dapat field `mahEstimate` (nullable), fungsi baru `fetchDrainMahByPackage()` (exec dumpsys via Shizuku + parse + resolve UID->package) & `mergeDrainData()` (gabung ke daftar existing, re-sort).
### Changed
- `DrainScreen.kt`: fetch data mAh riil dibungkus `withContext(Dispatchers.IO)` (shell exec blocking), tampilkan baris mAh riil per app + hint teks dinamis (real data vs proxy).

## [v1.0.16-batch51] - 2026-08-20
### Fixed
- **Bug baris tergabung `BatteryStatsParser.kt`** (Pending #19, 1.9/2): capture dumpsys panjang nyata dari user (13 app UID + 5 sistem) ungkap parser lama cuma nangkep 1/18 baris UID krn banyak baris konseptual dumpsys tergabung jadi 1 baris fisik & regex pakai anchor `^` ketat. Fix: `(?:^|\s)` boundary-aware + `findAll` (bukan `find` tunggal). Hasil setelah fix: 13 app + 5 sistem = 18 total, cocok 100% manual-count. Parser sekarang confidence 96%, tervalidasi penuh dari data nyata.
### Changed
- Bump versi `1.0.15` -> `1.0.16` (`versionCode` 16->17) sesuai RULE WAJIB Batch 37.

## [v1.0.15-batch50] - 2026-08-20
### Fixed
- **Bug casing regex `BatteryStatsParser.kt`** (Pending #19, 1.5/2): ditemukan dari validasi output `adb shell dumpsys batterystats` NYATA yang ditempel user — device pakai `"UID"` (kapital semua), regex Batch 49 cuma cocok `"Uid"` (case-sensitive) -> selalu return list kosong. Fix: `RegexOption.IGNORE_CASE`. Bagian `u0aXX` (UID aplikasi) masih belum tervalidasi data nyata — wiring UI masih ditahan.
### Changed
- Bump versi `1.0.14` -> `1.0.15` (`versionCode` 15->16) sesuai RULE WAJIB Batch 37.

## [v1.0.14-batch49] - 2026-08-20
### Added
- **`BatteryStatsParser.kt`** (Pending #19, langkah 1/2): parser murni utk section "Estimated power use (mAh):" dari `dumpsys batterystats`. Belum di-wiring ke UI — lihat catatan confidence 85% di `PROJECT_STATE.md`, WAJIB verifikasi output dumpsys nyata sebelum langkah 2/2 (wiring).
### Changed
- Bump versi `1.0.13` -> `1.0.14` (`versionCode` 14->15) sesuai RULE WAJIB Batch 37.

## [v1.0.13-batch48] - 2026-08-20
### Added
- **Shortcut "Pengaturan App" di Drain Analyzer** (Pending #13): tombol baru per-app buka dialog App Info bawaan Android (`UsageStatsHelper.openAppDetailsSettings`) — best-effort, user set battery restriction/manage-background manual sendiri (tidak ada API generik non-root utk cegah auto-launch otomatis). Tampil utk semua app (beda dgn Force Stop yg tetap digate 4 package sistem kritis).
### Changed
- Layout `DrainAppRow` (Drain Analyzer): dari 1 baris datar jadi 2 baris (info+checkbox di atas, tombol aksi di bawah) supaya tombol baru tidak sesak.
- Bump versi `1.0.12` -> `1.0.13` (`versionCode` 13->14) sesuai RULE WAJIB Batch 37.

## [v1.0.12-batch47] - 2026-08-20
### Removed
- **4 file dokumentasi orphan project lain "PromptVault"** (manifest desync, terdeteksi saat audit awal sesi): `ROADMAP.md`, `TROUBLESHOOTING.md`, `MAINTENANCE.md`, `scripts/preflight_check.sh` — bukan milik VoltCare, 0 referensi silang, tidak tercatat `FILE_MANIFEST.txt`, tidak dipanggil `release.yml`. Izin hapus dikonfirmasi eksplisit user. Pola sama dgn insiden Batch 25 (source `.kt` orphan), kali ini dokumentasi.
### Changed
- Bump versi `1.0.11` -> `1.0.12` (`versionCode` 12->13) sesuai RULE WAJIB Batch 37.

## [v1.0.11-batch46] - 2026-08-20
### Fixed
- **Tombol "Force Stop" tidak ada feedback** (lanjutan laporan user setelah Batch 45): `DrainScreen.kt` sekarang tampilkan Snackbar sukses/gagal (pola sama seperti `HistoryScreen.kt`) — sebelumnya return value `killBackgroundApp()` dibuang & list tidak pernah berubah visual (representasi historis, bukan proses live), jadi klik kelihatan "tidak ngefek".
### Changed
- Bump versi `1.0.10` -> `1.0.11` (`versionCode` 11->12) sesuai RULE WAJIB Batch 37.

## [v1.0.10-batch45] - 2026-08-20
### Fixed
- **Drain Analyzer: semua row tidak clickable di ROM OEM tertentu** (dilaporkan user via screenshot, device Transsion XOS) — gate `!app.isSystemApp` kelewat luas (nyembunyiin app OEM biasa spt Launcher/Jam yg ditandai `FLAG_SYSTEM`). Diganti blocklist eksplisit 4 package sistem kritis (`android`, `systemui`, `settings`, `phone`) — app lain (termasuk `FLAG_SYSTEM`) sekarang actionable (checkbox whitelist + Force Stop tampil).
### Changed
- Bump versi `1.0.9` -> `1.0.10` (`versionCode` 10->11) sesuai RULE WAJIB Batch 37.

## [v1.0.9-batch44] - 2026-08-20
### Added
- Pending #12: Auto-Hibernate Terjadwal — `HibernateWorker.kt` (baru, WorkManager pemakaian PERTAMA sejak dependency ditambahkan Batch 1) force-stop app whitelist tiap 30 menit. `DrainScreen.kt`: Switch master + checkbox whitelist per app (hanya app yang di-approve eksplisit user, bukan semua app).
### Changed
- Bump versi `1.0.8` -> `1.0.9` (`versionCode` 9->10) sesuai RULE WAJIB Batch 37.

## [v1.0.8-batch43] - 2026-08-20
### Added
- Pending #10: Estimasi sisa waktu pakai (discharge) di Dashboard — `DashboardViewModel` hitung drain rate rata-rata 24 jam terakhir dari `battery_log` existing (tanpa DB baru), tampil di kartu "Sisa Pakai" (reuse slot "Estimasi" existing, label dinamis sesuai status charging).
### Changed
- Bump versi `1.0.7` -> `1.0.8` (`versionCode` 8->9) sesuai RULE WAJIB Batch 37.

## [v1.0.7-batch42] - 2026-08-20
### Added
- Pending #11: Preset Cepat "Alarm Batas Charge" di tab Aturan — dialog 1-field (persen ambang) auto-create `RuleEntity(PERCENT_ABOVE, requireCharging=true, ALARM)` lewat `RulesViewModel.saveChargeLimitPreset()`, tanpa perlu isi form 5 field manual. Engine evaluasi & skema DB tidak berubah.
### Changed
- Bump versi `1.0.6` -> `1.0.7` (`versionCode` 7->8) sesuai RULE WAJIB Batch 37.

## [v1.0.6-batch41] - 2026-08-20
### Added
- Pending #20: `ShizukuManager.autoGrantUsageAccess()` — auto-grant Akses Penggunaan via `appops set <pkg> GET_USAGE_STATS allow` (Shizuku), diverifikasi ulang lewat `AppOpsManager` sebelum dianggap sukses. Tombol "Izinkan Otomatis via Shizuku" muncul di Drain Analyzer kalau Shizuku aktif — jalur manual buka Settings tetap ada sbg fallback.
### Changed
- Bump versi `1.0.5` -> `1.0.6` (`versionCode` 6->7) sesuai RULE WAJIB Batch 37.

## [v1.0.5-batch40] - 2026-08-20
### Fixed
- Body release GitHub cuma nampilin `**Full Changelog**: <link compare>` tanpa isi (root cause: `generate_release_notes: true` GitHub butuh alur PR+label, repo ini push langsung ke main) — diganti body custom dari `git log` (bullet list pesan commit riil sejak tag sebelumnya) via `body_path` di `release.yml`.
### Changed
- Bump versi `1.0.4` -> `1.0.5` (`versionCode` 5->6) sesuai RULE WAJIB Batch 37.

## [v1.0.4-batch39] - 2026-08-20
### Added
- Pending #18: `UsageStatsHelper.killBackgroundApp()` sekarang pakai `am force-stop <pkg>` via Shizuku (`ShizukuManager.execShellCommand()`) jika izin Shizuku aktif — jauh lebih kuat dari `killBackgroundProcesses` lama. Fallback otomatis ke jalur lama kalau Shizuku tidak aktif/gagal. Signature fungsi tidak berubah, `DrainScreen.kt` tidak perlu diedit.
### Changed
- Bump versi `1.0.3` -> `1.0.4` (`versionCode` 4->5) sesuai RULE WAJIB Batch 37.

## [v1.0.3-batch38] - 2026-08-20
### Fixed
- Pending #23: dialog "Update Tersedia" sekarang nampilin label `(build N)` di sebelah versi (`UpdateInfo.latestRunNumber` diteruskan dari `UpdateManager.kt` ke `UpdateScreen.kt`/`strings.xml`) — mencegah kebingungan user saat fallback run_number (Batch 36) trigger update walau `versionName` sama persis dgn yang terpasang.
### Changed
- Bump versi `1.0.2` -> `1.0.3` (`versionCode` 3->4) sesuai RULE WAJIB Batch 37.

## [v1.0.1-batch33] - 2026-08-19
### Fixed
- Pending #21: `checkForUpdate()` sekarang return sealed `UpdateCheckResult` (`UpToDate`/`Available`/`CheckFailed`) bukan `UpdateInfo?` — cek yang GAGAL (404/network) tidak lagi ditampilkan sebagai "Sudah Versi Terbaru" palsu, tapi masuk pesan error jelas. Kemungkinan besar penyebab awal: repo `VoltCare` belum punya Release sama sekali (cek Actions + secrets, lihat catatan Batch 33 di PROJECT_STATE.md).

## [v1.0.1] - 2026-08-19
### Changed
- Bump versi `1.0.0` → `1.0.1` (`versionCode` 1→2) di `app/build.gradle.kts`. Merangkum fix Batch 27-31 (insets, update-checker 404, icon overlap dashboard) jadi 1 rilis baru — sebelumnya Info Aplikasi Android masih nunjuk 1.0.0 meski sudah banyak fix numpuk.

## [v1.0.0-batch31] - 2026-08-19
### Fixed
- Icon shield (`ShizukuStatusAction`) numpuk sama judul "Dashboard": hapus `Scaffold` redundan di `DashboardScreen.kt` (sudah dibungkus Scaffold di `NavGraph.kt`, lolos audit Batch 27) + naikkan padding top overlay icon `8dp`→`64dp` di `NavGraph.kt`.

## [v1.0.0-batch30] - 2026-08-19
### Fixed
- **Bug kritis**: cek update selalu bilang "Sudah Versi Terbaru" meski sebenarnya gagal cek — `UpdateManager.kt` masih hardcode `GITHUB_REPO = "PowerVaultHealthPro"` (repo lama, 404 sejak rename Batch 28/29). Diganti ke `"VoltCare"`.

## [v1.0.0-batch29] - 2026-08-19
### Changed
- Rename folder lokal Termux `~/projects/PowerVaultHealthPro` → `~/projects/VoltCare` (permintaan eksplisit user). Semua skrip Termux ke depan pakai path baru. Tidak ada perubahan kode/source — murni operasi filesystem + update dokumentasi konvensi.

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
