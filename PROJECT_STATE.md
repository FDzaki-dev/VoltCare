# PROJECT_STATE.md
(Urutan DESCENDING - entri terbaru di paling atas)

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
- **Crash Logger bawaan**: MediaStore API 29+, path `Documents/PowerVaultHealthPro/logs/`, FIFO 50 log, metadata lengkap, fail-safe try-catch. Terpasang di `PowerVaultApplication`.
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
AndroidManifest.xml • app/build.gradle.kts • settings.gradle.kts • root build.gradle.kts • MainActivity.kt • PowerVaultApplication.kt • NavGraph.kt • AppDatabase.kt + 3 Entity + 3 DAO • release.keystore • .gitignore • .gitattributes • .github/workflows/release.yml
