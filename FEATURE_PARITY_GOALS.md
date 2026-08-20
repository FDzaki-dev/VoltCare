# FEATURE_PARITY_GOALS.md
Tujuan: melacak progres VoltCare menuju 100% cakupan fitur yang tampil di
screenshot Google AI Overview "Pilihan Aplikasi Battery Manager Terbaik"
(AccuBattery, GSam Battery Monitor, Greenify) yang diupload user di Batch 18.

(Bukan log kronologis — dokumen referensi/matrix, diperbarui in-place. Revisi
dicatat descending di bawah.)

---

## Skor Cakupan Saat Ini (per Batch 61, direvisi dari per Batch 18)

**4 Done • 3 Partial • 1 Not Implemented (1 platform-limited, 1 buildable) = ~78% penuh, ~94% termasuk partial**

| # | App Rujukan | Fitur (dari gambar) | Status | Bukti / Implementasi VoltCare |
|---|---|---|---|---|
| 1 | AccuBattery | Ukur tingkat keausan & kesehatan baterai | ✅ **Done** | `BatteryUtils.CalibrationStore` — Health% dari akumulasi mAh riil / kapasitas desain, syarat 3x siklus charge 0-100% berturut-turut non-drop (Batch 8) |
| 2 | AccuBattery | Alarm batas isi daya agar baterai lebih awet | ✅ **Done** (generik) | `RuleEntity(conditionType=PERCENT_ABOVE, actionType=ALARM)` via tab Aturan — engine sudah jalan sejak Batch 1, UI editor Batch 14. User bisa buat aturan "alarm saat >80% & charging" sendiri |
| 3 | AccuBattery | Hitung sisa waktu pakai berdasarkan kebiasaan Anda | ❌ **Belum ada** | `DashboardViewModel` **hanya** hitung `estimateMinutesToFull` saat charging (`BatteryUtils.estimateMinutesToFull`). Tidak ada estimasi **sisa waktu pakai saat discharge** berbasis rata-rata drain rate historis pengguna |
| 4 | GSam | Statistik penggunaan baterai mendetail | ✅ **Done** | `HistoryScreen`/`HistoryViewModel` — agregat 30 hari (Health/Suhu/Cycle) + grafik Canvas + export CSV (Batch 11) |
| 5 | GSam | Lacak app paling banyak menguras CPU & sinyal | ✅ **Done** | `UsageStatsHelper.fetchDrainMahByPackage()` + `BatteryStatsParser` (Batch 49-52) — mAh riil per app via `dumpsys batterystats` (Shizuku), digabung ke daftar `topAppsByForegroundUsage()` di `DrainScreen`. Fallback proxy waktu-foreground tetap aktif kalau Shizuku tidak dipakai/tidak tersedia |
| 6 | GSam | Pantau suhu & status pengisian daya real-time | ✅ **Done** | `DashboardScreen` — baca live `BatteryManager` tiap sample (Batch 1) |
| 7 | Greenify | Tidurkan app latar belakang yang tidak dipakai | ⚠️ **Partial** (naik dari force-stop manual) | `killBackgroundApp()` manual per-app (Batch 10) TETAP ada, DITAMBAH jalur otomatis via item #9 di baris bawah — masih Partial krn otomatis hanya jalan utk app whitelist eksplisit, bukan semua app |
| 8 | Greenify | Cegah app berjalan sendiri tanpa izin (auto-launch) | ❌ **Platform-limited** | Android membatasi kontrol App Standby Bucket (`UsageStatsManager.setAppStandbyBucket`) hanya untuk app sistem sejak API 30 — 3rd-party app tanpa root/device-admin tidak bisa cegah auto-launch app lain secara generik. Sama kelas keterbatasan dengan item #5 |
| 9 | Greenify | Hemat daya otomatis tanpa bikin lambat HP | ✅ **Done** (sejak Batch 44) | **Auto-Hibernate Terjadwal** — `HibernateWorker.kt` (`CoroutineWorker` + WorkManager `PeriodicWorkRequest`, interval 30 menit, di atas minimum WorkManager shg tidak membebani device). Card di tab **Penguras**: Switch master ON/OFF + checkbox whitelist per-app (`HibernateWhitelistStore`, SharedPreferences). HANYA kill app yang **di-approve eksplisit user** — bukan semua app, sesuai maksud "tanpa bikin lambat HP" (mencegah OOM-loop/kill app penting otomatis) |

---

## Pending Queue Baru (ditambahkan ke PROJECT_STATE.md, mulai nomor #10)

Item **buildable** (bisa dikerjakan batch berikutnya, sesuai Micro-Batching Rule):

- **#10 — Estimasi Sisa Waktu Pakai (discharge)**: tutup gap #3. Hitung rata-rata drain rate (%/menit) dari `BatteryLogDao` 30 hari terakhir (atau hasil `StressTestScreen` kalau lebih baru), tampilkan "~Xj Ym tersisa" di Dashboard saat tidak charging. Tidak perlu tabel/kolom DB baru — murni agregasi Kotlin dari data existing.
- **#11 — Preset Cepat "Alarm Batas Charge"**: tutup sebagian gap #2 (UX shortcut, engine sudah ada sejak Batch 1/14). Tombol/slider di Dashboard atau Rules yang auto-create `RuleEntity(PERCENT_ABOVE, ALARM)` tanpa user isi form manual dari nol.
- **#12 — Auto-Hibernate Terjadwal**: tutup gap #9, sebagian gap #7. `PeriodicWorkRequest` (WorkManager, dependency sudah ada, baru dipakai pertama kali) panggil `UsageStatsHelper.killBackgroundApp()` otomatis untuk app di **whitelist yang di-approve user secara eksplisit** (bukan semua app — mencegah kill app penting/OOM-loop tanpa izin), interval wajar (mis. tiap 30 menit) supaya tidak bikin device lambat/boros justru karena scheduler-nya sendiri.

Item **platform-limited** (dicatat sbg batasan, bukan buildable penuh — best-effort saja):

- **#13 — "Cegah auto-launch tanpa izin" (gap #8)**: tidak bisa diimplementasi generik tanpa root (lihat tabel di atas). Best-effort yang realistis: tombol di `DrainScreen` per-app yang membuka `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` (dialog "Battery" / "App info" bawaan Android untuk app tsb) supaya **user sendiri** yang set battery restriction manual — bukan otomatis dari VoltCare. Butuh keputusan/izin eksplisit user dulu sebelum masuk Pending Queue aktif (pola sama seperti "Force Stop" yang sudah didokumentasikan sbg best-effort di Batch 10).

---

## Revisi

- **2026-08-20 (Batch 61)**: Fix desync — tabel masih nampilin item #9 "❌ Belum ada" & #7 status lama, padahal Auto-Hibernate (Pending #12) sudah RESOLVED sejak Batch 44 (tidak pernah di-update in-place saat itu). Direvisi ke status akurat, skor naik 3→4 Done.
- **2026-08-19 (Batch 18)**: Dibuat pertama kali. Sumber: 2 screenshot Google AI Overview diupload user ("Pilihan Aplikasi Battery Manager Terbaik" — AccuBattery, GSam Battery Monitor, Greenify). Audit dilakukan terhadap source code aktual (Batch 1-17), bukan asumsi — termasuk `grep` untuk memverifikasi WorkManager belum pernah dipakai walau sudah jadi dependency sejak Batch 1.
