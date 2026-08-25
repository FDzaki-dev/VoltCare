package com.voltcare.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet warna (Batch 82 - restyle iOS/Cupertino, tahap 1/N: fondasi warna+shape).
 * Diambil dari Apple Human Interface Guidelines "System Colors" (systemGreen/
 * systemOrange/systemRed/systemGray/systemBackground/systemGroupedBackground),
 * BUKAN sekadar rebrand nama - nilai HEX diganti ke tone resmi iOS light & dark.
 *
 * PENTING: nama variabel LAMA (VcGreen/VcAmber/VcRed/VcBgDark/VcSurfaceDark/
 * VcTextPrimary/VcTextSecondary) SENGAJA DIPERTAHANKAN APA ADANYA - dipakai langsung
 * di RulesScreen.kt & ShizukuStatusAction.kt (Zero-Unnecessary-Refactor, di luar scope
 * batch ini). Hanya NILAI hex yang diperbarui ke tone iOS; makna semantik (hijau=OK,
 * amber=warning, merah=kritis, teks sekunder=abu-abu) tidak berubah sama sekali.
 *
 * Sekaligus MENUTUP Pending Queue #32 (HIGH, UX_AUDIT.md Batch 80): [VcOnAccent] dipakai
 * sbg onPrimary/onSecondary/onError di Theme.kt - teks GELAP di atas aksen terang (bukan
 * putih), lolos WCAG AA (dihitung manual, kontras >=5.2:1 utk ketiga warna aksen,
 * jauh di atas ambang 4.5:1) - beda dari sebelumnya (teks putih M3 default, cuma ~2.1:1).
 */

// --- Warna aksen semantik (dipakai lintas layar - nama variabel JANGAN diubah) ---
val VcGreen = Color(0xFF34C759) // iOS systemGreen (light) - status OK/Ready/Aktif
val VcAmber = Color(0xFFFF9500) // iOS systemOrange (light) - status warning/perlu perhatian
val VcRed = Color(0xFFFF3B30) // iOS systemRed (light) - status kritis/error/hapus

// Varian mode gelap (Apple HIG: warna sistem sedikit beda terang/gelap demi kontras optimal
// di background gelap) - dipakai Theme.kt utk DarkColors, TIDAK dipakai file lain.
val VcGreenDark = Color(0xFF30D158) // iOS systemGreen (dark)
val VcAmberDark = Color(0xFFFF9F0A) // iOS systemOrange (dark)
val VcRedDark = Color(0xFFFF453A) // iOS systemRed (dark)

/**
 * Teks DI ATAS warna aksen (VcGreen/VcAmber/VcRed, kedua mode) - satu warna gelap dipakai
 * bersama krn ketiga aksen py luminance mirip (~0.42-0.43, kecuali merah ~0.25 - tetap lolos
 * AA dgn margin besar di ketiganya). Menggantikan default M3 (teks putih) yang GAGAL WCAG AA.
 */
val VcOnAccent = Color(0xFF14140F)

// --- Background & surface iOS "grouped" style (Settings-app look: card putih di atas
// background abu-abu lembut, bukan flat sewarna spt Material default) ---
val VcBackgroundLight = Color(0xFFF2F2F7) // iOS systemGroupedBackground (light)
val VcSurfaceLight = Color(0xFFFFFFFF) // kartu putih bersih
val VcOnSurfaceLight = Color(0xFF1C1C1E) // iOS label (light) - teks utama
val VcSurfaceVariantLight = Color(0xFFE5E5EA) // iOS systemGray5 - divider/surface sekunder
val VcOnSurfaceVariantLight = Color(0xFF3C3C43) // iOS secondaryLabel (light)
val VcOutlineLight = Color(0xFFC6C6C8) // iOS separator (light)

val VcBgDark = Color(0xFF000000) // iOS systemGroupedBackground (dark) - true black
val VcSurfaceDark = Color(0xFF1C1C1E) // iOS secondarySystemGroupedBackground (dark)
val VcTextPrimary = Color(0xFFFFFFFF) // iOS label (dark)
val VcTextSecondary = Color(0xFFAEAEB2) // iOS secondaryLabel (dark)
val VcSurfaceVariantDark = Color(0xFF2C2C2E) // iOS systemGray5 (dark)
val VcOutlineDark = Color(0xFF38383A) // iOS separator (dark)
