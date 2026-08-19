package com.voltcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = VcGreen,
    secondary = VcAmber,
    error = VcRed,
    background = VcBgDark,
    surface = VcSurfaceDark,
    onBackground = VcTextPrimary,
    onSurface = VcTextPrimary
)

private val LightColors = lightColorScheme(
    primary = VcGreen,
    secondary = VcAmber,
    error = VcRed
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
        content = content
    )
}
