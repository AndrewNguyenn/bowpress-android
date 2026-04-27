package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Kenrokuen is light-mode only — we deliberately ignore the system dark-mode
// signal here. Dark mode support can return later but needs a whole parallel
// palette, which is out of scope.
private val KenrokuenLightScheme = lightColorScheme(
    primary = AppPondDk,
    onPrimary = AppPaper,
    primaryContainer = AppPondLt,
    onPrimaryContainer = AppDeep,

    secondary = AppMoss,
    onSecondary = AppPaper,
    secondaryContainer = AppLine2,
    onSecondaryContainer = AppPine,

    tertiary = AppMaple,
    onTertiary = AppPaper,
    tertiaryContainer = AppPaper2,
    onTertiaryContainer = AppMaple,

    background = AppPaper,
    onBackground = AppInk,

    surface = AppPaper,
    onSurface = AppInk,
    surfaceVariant = AppPaper2,
    onSurfaceVariant = AppInk2,
    surfaceTint = AppPond,

    inverseSurface = AppInk,
    inverseOnSurface = AppPaper,
    inversePrimary = AppPondLt,

    outline = AppLine,
    outlineVariant = AppLine2,

    error = AppMaple,
    onError = AppPaper,
    errorContainer = AppPaper2,
    onErrorContainer = AppDanger,

    scrim = AppInk,
)

@Composable
fun BowPressTheme(
    content: @Composable () -> Unit,
) {
    // Always light — see comment on KenrokuenLightScheme.
    MaterialTheme(
        colorScheme = KenrokuenLightScheme,
        typography = BowPressTypography,
        shapes = BPShapes,
        content = content,
    )
}
