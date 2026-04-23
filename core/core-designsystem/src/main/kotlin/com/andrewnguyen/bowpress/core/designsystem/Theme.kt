package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BowPressColors.Accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BowPressColors.AccentContainer,
    onPrimaryContainer = BowPressColors.OnAccentContainer,
    surface = BowPressColors.Surface,
    onSurface = BowPressColors.OnSurface,
    outline = BowPressColors.Outline,
    error = BowPressColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = BowPressColors.Accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BowPressColors.AccentDark,
    onPrimaryContainer = BowPressColors.AccentContainer,
    surface = BowPressColors.SurfaceDark,
    onSurface = BowPressColors.OnSurfaceDark,
    outline = BowPressColors.Outline,
    error = BowPressColors.Error,
)

@Composable
fun BowPressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BowPressTypography,
        content = content,
    )
}
