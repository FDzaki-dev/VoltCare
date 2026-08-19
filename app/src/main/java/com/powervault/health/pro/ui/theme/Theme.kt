package com.powervault.health.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = PvGreen,
    secondary = PvAmber,
    error = PvRed,
    background = PvBgDark,
    surface = PvSurfaceDark,
    onBackground = PvTextPrimary,
    onSurface = PvTextPrimary
)

private val LightColors = lightColorScheme(
    primary = PvGreen,
    secondary = PvAmber,
    error = PvRed
)

@Composable
fun PowerVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PvTypography,
        content = content
    )
}
