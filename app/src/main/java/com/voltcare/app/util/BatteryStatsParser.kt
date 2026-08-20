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
 * ⚠️ PERINGATAN JUJUR SOAL AKURASI: format section ini TIDAK didokumentasikan resmi
 * oleh Android (bagian dari `dumpsys`, bukan API publik) — pola di bawah disusun dari
 * format yang secara luas dipakai tool open-source (mis. Battery Historian) & relatif
 * stabil sejak Android 5, TAPI belum diverifikasi terhadap output dumpsys nyata di
 * lingkungan pembuatan file ini (sandbox tanpa device Android). Parser dibuat DEFENSIF
 * (regex + skip baris tak cocok, TIDAK PERNAH throw) supaya kegagalan parsing di 1
 * device/ROM cuma menghasilkan list kosong, bukan crash — tapi "list kosong" ATAU
 * "angka tidak masuk akal" sama-sama mungkin kalau format device user ternyata beda.
 * WAJIB diverifikasi dgn `adb shell dumpsys batterystats --charged` nyata sebelum
 * batch berikutnya (wiring UI) dianggap 100% andal.
 */
object BatteryStatsParser {

    // Header section, contoh baris nyata: "Estimated power use (mAh):"
    private val SECTION_HEADER = Regex("""Estimated power use \(mAh\)""")

    // Baris per-UID di dalam section, contoh: "    Uid u0a55: 45.678" atau
    // "    Uid 1000: 5.678 ( cpu=... wake=... )" - breakdown "( ... )" di akhir diabaikan
    // (tidak masuk grup regex, tidak perlu di-strip manual).
    private val UID_LINE = Regex("""^\s*Uid\s+(\S+):\s+([\d.]+)""")

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

            val match = UID_LINE.find(line) ?: continue
            val mah = match.groupValues[2].toDoubleOrNull() ?: continue
            val uid = decodeUid(match.groupValues[1]) ?: continue
            if (uid >= minUid) result.add(UidPowerUsage(uid, mah))
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
