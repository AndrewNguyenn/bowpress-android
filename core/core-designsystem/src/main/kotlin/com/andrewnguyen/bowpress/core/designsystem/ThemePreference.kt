package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.andrewnguyen.bowpress.core.model.ThemePreference

// Re-export so existing callsites that pull `ThemePreference` from this
// package (BowPressTheme + Settings AppearanceRow) keep compiling. The
// canonical declaration lives in core-model so core-data can persist it
// without taking a Compose dependency.
typealias ThemePreferenceUi = ThemePreference

/**
 * Provided by `BowPressTheme` so any composable can read the *resolved*
 * preference. Default is SYSTEM so previews behave.
 */
val LocalThemePreference: ProvidableCompositionLocal<ThemePreference> =
    staticCompositionLocalOf { ThemePreference.SYSTEM }

/**
 * Setter handed down by `BowPressTheme`. The Settings Appearance row calls
 * this to cycle preferences; the persistence layer routes through the
 * repository injected at the app root.
 */
val LocalThemePreferenceSetter: ProvidableCompositionLocal<(ThemePreference) -> Unit> =
    staticCompositionLocalOf { { /* no-op for previews */ } }
