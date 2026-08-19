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
