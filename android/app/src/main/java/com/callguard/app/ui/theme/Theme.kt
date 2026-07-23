package com.callguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dynamic (Material You) color is intentionally OFF: CallGuard needs a
// consistent brand identity across every phone, not the device wallpaper's hues.

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyMuted,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = TealDeep,
    secondaryContainer = TealSoft,
    onSecondaryContainer = TealDeep,
    tertiary = Teal,
    background = PageBg,
    onBackground = Navy,
    surface = CardBg,
    onSurface = Navy,
    surfaceVariant = PageBg,
    onSurfaceVariant = Slate,
    error = FlagRed,
    onError = Color.White,
    errorContainer = FlagRedSoft,
    onErrorContainer = FlagRed,
    outline = HairLine,
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = NavyDeep,
    primaryContainer = NavyMuted,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = NavyDeep,
    secondaryContainer = NavyMuted,
    onSecondaryContainer = TealSoft,
    tertiary = Teal,
    background = PageBgDark,
    onBackground = Color(0xFFE6ECF4),
    surface = CardBgDark,
    onSurface = Color(0xFFE6ECF4),
    surfaceVariant = NavySurfaceDark,
    onSurfaceVariant = SlateLight,
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF5C1A22),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = NavyMuted,
)

@Composable
fun CallGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
