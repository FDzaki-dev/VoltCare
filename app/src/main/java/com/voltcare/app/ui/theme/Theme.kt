package com.voltcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Batch 82 (restyle iOS/Cupertino, tahap 1/N - fondasi): ColorScheme M3 lengkap
 * (bukan cuma primary/secondary/error spt sebelumnya) memakai token iOS System
 * Colors dari Color.kt, + [VcShapes] sudut membulat generous ala Cupertino
 * (card/dialog/textfield - lihat catatan di [VcShapes]).
 *
 * MENUTUP Pending Queue #32 (HIGH, UX_AUDIT.md Batch 80): onPrimary/onSecondary/
 * onError SEKARANG eksplisit pakai [VcOnAccent] (teks gelap), bukan default M3
 * (teks putih, gagal WCAG AA ~2.1:1). Lihat perhitungan kontras di Color.kt.
 */
private val DarkColors = darkColorScheme(
    primary = VcGreenDark,
    onPrimary = VcOnAccent,
    secondary = VcAmberDark,
    onSecondary = VcOnAccent,
    error = VcRedDark,
    onError = VcOnAccent,
    background = VcBgDark,
    onBackground = VcTextPrimary,
    surface = VcSurfaceDark,
    onSurface = VcTextPrimary,
    surfaceVariant = VcSurfaceVariantDark,
    onSurfaceVariant = VcTextSecondary,
    outline = VcOutlineDark
)

private val LightColors = lightColorScheme(
    primary = VcGreen,
    onPrimary = VcOnAccent,
    secondary = VcAmber,
    onSecondary = VcOnAccent,
    error = VcRed,
    onError = VcOnAccent,
    background = VcBackgroundLight,
    onBackground = VcOnSurfaceLight,
    surface = VcSurfaceLight,
    onSurface = VcOnSurfaceLight,
    surfaceVariant = VcSurfaceVariantLight,
    onSurfaceVariant = VcOnSurfaceVariantLight,
    outline = VcOutlineLight
)

/**
 * Sudut membulat generous ala iOS/Cupertino (bandingkan default M3: extraSmall=4dp,
 * small=8dp, medium=12dp, large=16dp, extraLarge=28dp - lebih "kotak"/tegas).
 * Efeknya otomatis ke SEMUA layar tanpa perlu edit tiap file (Card pakai `medium`,
 * AlertDialog/dialog form pakai `extraLarge`, OutlinedTextField pakai `extraSmall` -
 * konsisten di semua versi M3 1.x). Bukan "continuous corner"/squircle asli iOS
 * (butuh custom Shape, di luar scope batch fondasi ini) - antre di Pending Queue.
 */
private val VcShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun VoltCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = VcTypography,
        shapes = VcShapes,
        content = content
    )
}
