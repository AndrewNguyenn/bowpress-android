package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.andrewnguyen.bowpress.core.model.ThemePreference

/**
 * Build an M3 [ColorScheme] from a Kenrokuen palette. Same channel mapping
 * for light + dark — only the underlying [palette] differs. We always
 * route through [lightColorScheme] because the channel mapping is identical;
 * [darkColorScheme]'s defaults bake in different fallbacks that we don't
 * want overriding our explicit Kenrokuen mapping.
 */
private fun schemeFor(palette: KenrokuenPalette): ColorScheme = lightColorScheme(
    primary = palette.pondDk,
    onPrimary = palette.paper,
    primaryContainer = palette.pondLt,
    onPrimaryContainer = palette.deep,
    inversePrimary = palette.pondLt,

    secondary = palette.moss,
    onSecondary = palette.paper,
    secondaryContainer = palette.line2,
    onSecondaryContainer = palette.pine,

    tertiary = palette.maple,
    onTertiary = palette.paper,
    tertiaryContainer = palette.paper2,
    onTertiaryContainer = palette.maple,

    background = palette.paper,
    onBackground = palette.ink,

    surface = palette.paper,
    onSurface = palette.ink,
    surfaceVariant = palette.paper2,
    onSurfaceVariant = palette.ink2,
    surfaceTint = palette.pond,

    inverseSurface = palette.ink,
    inverseOnSurface = palette.paper,

    error = palette.maple,
    onError = palette.paper,
    errorContainer = palette.paper2,
    onErrorContainer = palette.danger,

    outline = palette.line,
    outlineVariant = palette.line2,

    scrim = palette.ink,
)

/**
 * Resolves the active palette from the user's [ThemePreference] +
 * `isSystemInDarkTheme()`. Mirrors iOS `prefs.colorScheme` which returns
 * nil (follow OS), .light, or .dark.
 */
@Composable
private fun resolvePalette(preference: ThemePreference): KenrokuenPalette {
    val isDark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    return if (isDark) DarkKenrokuen else LightKenrokuen
}

/**
 * Root theme. Provides [LocalKenrokuen] + [LocalThemePreference] +
 * [LocalThemePreferenceSetter] so any composable can read tokens (`AppPaper`
 * etc.) and the Settings Appearance row can cycle the preference.
 *
 * iOS 04e79bc parity: System / Light / Dark with the Yofuke palette for
 * dark. Default arguments give a SYSTEM-following theme so unit-test /
 * preview callers don't need to wire DataStore.
 */
@Composable
fun BowPressTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    onPreferenceChange: (ThemePreference) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val palette = resolvePalette(preference)
    val scheme = remember(palette) { schemeFor(palette) }
    CompositionLocalProvider(
        LocalThemePreference provides preference,
        LocalThemePreferenceSetter provides onPreferenceChange,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BowPressTypography,
            shapes = BPShapes,
            content = content,
        )
    }
}
