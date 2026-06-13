package com.jehutyno.yomikata.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val YomikataDarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = AccentOnOrange,
    secondary = AccentOrange,
    onSecondary = AccentOnOrange,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfacePrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = BorderSubtle,
    error = Wrong,
    onError = BackgroundWrong,
)

@Composable
fun YomikataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YomikataDarkColorScheme,
        shapes = YomikataShapes,
        typography = YomikataTypography,
        content = content,
    )
}
