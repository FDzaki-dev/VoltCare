# UX_AUDIT.md
(Urutan DESCENDING - audit terbaru di paling atas)

---

## [Batch 80] Audit UX Mendalam 100% — 2026-08-21

**Scope:** full sweep 6 screen Compose (Dashboard, Drain, Rules, History, StressTest, Update) + ShizukuStatusAction + NavGraph + Theme/Color. Audit-only, **0 perubahan kode fungsional** (hanya bump versionName docs-only, sesuai konvensi).

### 🔴 HIGH
1. **RulesScreen.kt** (day-picker "Hari Aktif", `RuleFormDialog`): label lingkaran `M/S/S/R/K/J/S` — 3 lingkaran (Senin, Selasa, Sabtu) sama-sama berlabel "S", tidak bisa dibedakan secara visual sama sekali. Ringkasan teks di bawah (Batch 77) membantu SETELAH pilih, tapi saat proses memilih di picker-nya sendiri tetap ambigu.
2. **Theme.kt / Color.kt** (LightColors): `primary = VcGreen (#2ECC71)`, `secondary = VcAmber (#F5A623)` tanpa `onPrimary`/`onSecondary` eksplisit → Material3 default pakai teks putih di atasnya. Rasio kontras VcGreen vs putih ≈ **2.1:1** — GAGAL WCAG AA (min 4.5:1 teks normal). Berdampak ke SEMUA tombol primer di Light Mode (Mulai Kalibrasi, Mulai Tes, Simpan, dst) - teks berpotensi nyaris tak terbaca di mode terang.

### 🟠 MEDIUM
3. **StressTestScreen.kt**: tidak ada `BackHandler` saat `testState == RUNNING`. Back gesture/tombol sistem bisa keluar dari tes 10 menit yang berjalan tanpa peringatan apa pun (beda dgn tombol "Hentikan Lebih Awal" yang eksplisit & sadar).
4. **DrainScreen.kt**: Switch "Auto-Hibernate Terjadwal" langsung aktif tanpa dialog konfirmasi konsekuensi (force-stop tiap 30 menit ke seluruh app whitelist, termasuk app yang mungkin sedang expect notifikasi background).
5. **HistoryScreen.kt**: `LineChart` (Canvas custom) tidak ada gridline/label sumbu Y sama sekali — hanya teks warna di atas grafik, user tidak bisa baca nilai presisi di titik manapun pada grafik.

### 🟡 LOW
6. **UpdateScreen.kt**: dialog `Checking` & `Downloading` — `onDismissRequest = {}` + tanpa tombol Batal, user terkunci nunggu modal walau salah pencet "Cek Update".
7. **DrainScreen.kt**: "Force Stop" per app row tanpa konfirmasi (beda pola dgn delete Rule yang pakai `AlertDialog`) — low-risk & reversible, tapi inkonsisten.
8. **NavGraph.kt**: `NavigationBarItem` icon `contentDescription = tab.label` — duplikat dgn `label = { Text(tab.label) }` yang sudah tampil (TalkBack baca dobel, bukan bug tapi bisa dirapikan).

### ✅ Verified OK (dicek eksplisit, bukan temuan baru)
- Scroll 5 screen (Dashboard/Drain/Rules/History/StressTest) — semua reachable, tidak ada clipping (fix Batch 55/78 sudah benar & masih valid).
- Konfirmasi delete Rule sudah ada (`AlertDialog`).
- Preset "Alarm Batas Charge" — validasi input jelas (`isError` + `enabled` gating tombol Simpan).
- Kombinasi kontradiktif PERCENT_BELOW+charging (Batch 63) sudah ada warning inline.
- Semua `IconButton` krusial (Edit/Hapus/Tambah/Shizuku/Update) sudah punya `contentDescription`.

### Pending Queue (baru dari audit ini — BELUM dikerjakan, micro-batching = 1 per batch berikutnya)
- **#31 (HIGH)** Fix label duplikat day-picker RulesScreen (mis. 2 huruf: Mi/Sn/Sl/Rb/Km/Jm/Sb).
- **#32 (HIGH)** Fix kontras `onPrimary`/`onSecondary` di `LightColors` (Theme.kt) sampai lolos WCAG AA.
- **#33 (MED)** `BackHandler` saat StressTest RUNNING (konfirmasi keluar).
- **#34 (MED)** Dialog konfirmasi sebelum aktifkan Auto-Hibernate.
- **#35 (MED)** Label/gridline sumbu Y di `LineChart` History.
- **#36 (LOW)** Tombol Batal di dialog Checking/Downloading Update.
- **#37 (LOW)** Dialog konfirmasi Force Stop (opsional, prioritas rendah).

**Rekomendasi ke user:** mulai dari #31 & #32 (HIGH, dampak paling luas), sebutkan nomor mana yang mau dikerjakan duluan di batch berikutnya.
