package com.voltcare.app.util

/**
 * Hasil parsing 1 baris "Uid" di section "Estimated power use (mAh):".
 * [uid] sudah didekode ke UID Android asli (bukan string "u0aXXX" mentah).
 */
data class UidPowerUsage(val uid: Int, val mah: Double)

/**
 * Parser MURNI (pure Kotlin, TANPA Android API, TANPA I/O) untuk section
 * "Estimated power use (mAh):" dari stdout `dumpsys batterystats --charged`
 * (dieksekusi via [ShizukuManager.execShellCommand], BUKAN tanggung jawab file ini).
 *
 * Langkah 1/2 Pending #19 (`FEATURE_PARITY_GOALS.md` Batch 18, gap GSam "lacak app
 * paling banyak menguras CPU & sinyal") — upgrade Drain Analyzer dari proxy waktu
 * pemakaian foreground ([UsageStatsHelper.topAppsByForegroundUsage]) ke data mAh
 * riil per app. Batch ini SENGAJA cuma logic parsing murni, BELUM wiring ke UI/
 * ShizukuManager — sesuai catatan Pending Queue sejak Batch 18 ("kompleksitas parsing
 * tinggi -> mungkin perlu dipecah lagi"), supaya bagian paling berisiko (parsing teks
 * tidak terstruktur) bisa diaudit terpisah dari perubahan UI.
 *
 * ⚠️ STATUS VALIDASI (update Batch 51): TERVERIFIKASI PENUH dari 2 capture nyata
 * `adb shell dumpsys batterystats --charged` (device user, Transsion XOS):
 * - Batch 50 (capture pendek, `grep -A 30`): ketemu bug casing "UID" vs "Uid" -> fixed.
 * - Batch 51 (capture panjang, `grep -A 100`, berisi 13 baris `u0aXX` app + 5 UID sistem):
 *   ketemu bug KEDUA yang lebih signifikan - device/terminal user MENGGABUNGKAN banyak
 *   baris konseptual dumpsys jadi 1 baris fisik sangat panjang (bukan 1 baris = 1 entri
 *   UID spt asumsi awal). Regex lama (anchor `^` ketat) cuma nangkep 1 dari 15 baris UID
 *   di capture ini. FIXED: `UID_LINE` dari anchor `^` (start-of-line) jadi boundary-aware
 *   `(?:^|\s)` + [findAll] (bukan [find] tunggal) per baris, supaya SEMUA kemunculan
 *   "UID ..." di 1 baris fisik ikut tertangkap. Hasil setelah fix: 13 app UID + 5 UID
 *   sistem = 18 total, cocok 100% dgn hitung manual dari teks mentah (5 sistem otomatis
 *   ter-filter [minUid], sisa 13 app terurut descending sesuai mAh).
 * - `decodeUid()` (`u0aXX` -> UID Android asli) TERVALIDASI: pola `u0a41`, `u0a125`,
 *   `u0a3`, dst semua ter-decode benar (userId*100000+10000+appId), termasuk format
 *   1-digit appId (`u0a3`) yang sebelumnya belum ada contoh nyatanya.
 * - Heuristik akhir section (`NEXT_TOP_LEVEL_SECTION`) TIDAK berubah & TETAP valid -
 *   baris penutup (prompt shell `~/projects/VoltCare $` di capture user) tetap terdeteksi
 *   benar sbg akhir section walau baris UID di atasnya tergabung.
 */
object BatteryStatsParser {

    // Header section, contoh baris nyata: "Estimated power use (mAh):"
    private val SECTION_HEADER = Regex("""Estimated power use \(mAh\)""")

    // Baris per-UID di dalam section, contoh: "    Uid u0a55: 45.678" atau
    // "    UID 1000: 4.81 bg: 4.81 cpu=... UID 0: 1.59 bg: 1.59 cpu=... UID u0a41: ..."
    // - breakdown tambahan di akhir (dalam kurung ATAU token polos spt "bg:"/"cpu=...")
    // diabaikan (tidak masuk grup regex). PENTING (Batch 51, dari capture dumpsys PANJANG
    // nyata milik user): SATU baris fisik (dipisah newline sungguhan) bisa berisi BANYAK
    // entri "UID ..." sekaligus - device/terminal user (Termux, layar sempit) menggabungkan
    // banyak baris konseptual dumpsys jadi 1 baris sangat panjang saat di-capture/paste,
    // walau baris file/breakdown lain (spt prompt shell penutup) tetap baris sendiri.
    // Makanya pola TIDAK di-anchor `^` (start-of-line ketat) lagi - pakai boundary
    // `(?:^|\s)` (awal baris ATAU didahului spasi) + [findAll] per baris (bukan [find]
    // tunggal) supaya SEMUA kemunculan "UID ..." di 1 baris fisik ikut tertangkap, bukan
    // cuma yang pertama/yang persis di awal baris.
    private val UID_LINE = Regex("""(?:^|\s)uid\s+(\S+):\s+([\d.]+)""", RegexOption.IGNORE_CASE)

    // Heuristik akhir section: dumpsys batterystats secara konsisten membuat header
    // section besar (mis. "Estimated power use (mAh):", "Discharge step durations:")
    // rata kolom 0 (tanpa indentasi), sedangkan SEMUA baris isi section (Screen/Wifi/
    // Uid/dll) terindentasi minimal 2 spasi. Baris kosong TIDAK dianggap akhir section
    // (beberapa dump menyisipkan baris kosong antar kategori di section yang sama).
    private val NEXT_TOP_LEVEL_SECTION = Regex("""^\S""")

    /**
     * @param dumpsysOutput stdout mentah dari `dumpsys batterystats --charged`.
     * @param minUid ambang bawah UID yang dianggap "app" (default 10000 = `Process.FIRST_APPLICATION_UID`,
     * mengecualikan UID sistem spt radio/root yang tidak actionable via Force Stop biasa).
     * @return list UID app + mAh, descending berdasarkan mAh. List kosong (BUKAN exception)
     * kalau section tidak ditemukan atau tidak ada baris yang cocok.
     */
    fun parseEstimatedPowerUse(dumpsysOutput: String, minUid: Int = 10000): List<UidPowerUsage> {
        val lines = dumpsysOutput.lines()
        val headerIndex = lines.indexOfFirst { SECTION_HEADER.containsMatchIn(it) }
        if (headerIndex == -1) return emptyList()

        val result = mutableListOf<UidPowerUsage>()
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isNotBlank() && NEXT_TOP_LEVEL_SECTION.containsMatchIn(line)) break

            for (match in UID_LINE.findAll(line)) {
                val mah = match.groupValues[2].toDoubleOrNull() ?: continue
                val uid = decodeUid(match.groupValues[1]) ?: continue
                if (uid >= minUid) result.add(UidPowerUsage(uid, mah))
            }
        }
        return result.sortedByDescending { it.mah }
    }

    /**
     * Dekode identifier UID dumpsys ke UID Android asli:
     * - Format app "uXaYYYYY" (mis. "u0a55") -> userId*100000 + 10000 + appId (konvensi
     *   `UserHandle`/`Process.FIRST_APPLICATION_UID` di AOSP, `100000` = `UserHandle.PER_USER_RANGE`).
     * - Angka polos (mis. "1000" untuk radio, "0" untuk root) -> dipakai apa adanya, biasanya
     *   ter-filter oleh [minUid] di [parseEstimatedPowerUse] karena <10000.
     * Return null (bukan exception) kalau format tidak dikenali sama sekali - baris tsb dilewati.
     */
    private fun decodeUid(raw: String): Int? {
        val encoded = Regex("""^u(\d+)a(\d+)$""").find(raw)
        if (encoded != null) {
            val userId = encoded.groupValues[1].toIntOrNull() ?: return null
            val appId = encoded.groupValues[2].toIntOrNull() ?: return null
            return userId * 100000 + 10000 + appId
        }
        return raw.toIntOrNull()
    }
}
