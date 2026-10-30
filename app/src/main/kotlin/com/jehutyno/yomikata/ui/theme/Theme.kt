package com.jehutyno.yomikata.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.util.Prefs

val LocalJapaneseFontFamily = staticCompositionLocalOf { FontFamily.Default }

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
fun YomikataTheme(
    useSerif: Boolean = if (LocalInspectionMode.current) false else {
        val context = LocalContext.current
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Prefs.FONT_SERIF.pref, false)
    },
    content: @Composable () -> Unit,
) {
    val japaneseFontFamily = if (useSerif) FontFamily.Serif else FontFamily.Default
    CompositionLocalProvider(
        LocalJapaneseFontFamily provides japaneseFontFamily,
    ) {
        MaterialTheme(
            colorScheme = YomikataDarkColorScheme,
            shapes = YomikataShapes,
            typography = YomikataTypography,
            content = content,
        )
    }
}
