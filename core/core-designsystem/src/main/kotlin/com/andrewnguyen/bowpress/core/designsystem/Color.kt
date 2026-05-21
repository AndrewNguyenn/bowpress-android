package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.ui.graphics.Color

// Kenrokuen palette. The `App*` tokens are the LIGHT ("Day") values used
// by all design-system composables today. The `App*Dark` tokens mirror
// iOS Yofuke (sumi-ink lacquer) and are surfaced via the MaterialTheme
// ColorScheme that BowPressTheme picks based on ThemePreference. Direct
// `App*` references (used in ~39 design-system + feature files) stay
// light-mode regardless — a follow-up should hoist those to a
// CompositionLocal-backed palette, but that's a 50-site refactor and is
// out of scope for the initial dark-mode landing.

// region — Light ("Day") tokens — used by all direct `App*` references

val AppPaper  = Color(0xFFEEF2EC)
val AppPaper2 = Color(0xFFE4EBE3)
val AppCream  = Color(0xFFF6F8F3)

val AppInk  = Color(0xFF1F2A26)
val AppInk2 = Color(0xFF4A5752)
val AppInk3 = Color(0xFF8A9690)

val AppLine  = Color(0xFFC7D2C9)
val AppLine2 = Color(0xFFD9E1D8)

val AppPond   = Color(0xFF4A7989)
val AppPondDk = Color(0xFF2D5A6B)
val AppPondLt = Color(0xFF8FB3BF)
val AppDeep   = Color(0xFF1E3E4A)

val AppMoss  = Color(0xFF6D8551)
val AppPine  = Color(0xFF4A5F3A)
val AppMaple = Color(0xFFB5614A)
val AppStone = Color(0xFF9AA3A0)

// Target face — real World Archery colors. Constant across themes.
val AppTgtWhite  = Color(0xFFF6F8F3)
val AppTgtBlack  = Color(0xFF1F2A26)
val AppTgtBlue   = Color(0xFF4EA8C9)
val AppTgtRed    = Color(0xFFD94B3B)
val AppTgtYellow = Color(0xFFF0D04A)

// WA bar-fill palette — analytics row tints, constant across themes.
val AppWAGoldFill  = Color(0xFFD8A23A)
val AppWARedFill   = Color(0xFFB04A3A)
val AppWABlueFill  = Color(0xFF3A6F8A)
val AppWABlackFill = Color(0xFF2A2A28)
val AppWAWhiteFill = Color(0xFFF4F1EA)

// Scorecard ring-tonal tints — translucent per-ring cell backgrounds for the
// session-detail scorecard table. Mirrors iOS `appRingTint*` (AppTheme.swift).
// Light-only: like the other direct `App*` tokens these stay light regardless
// of theme until the CompositionLocal-palette refactor lands. Each is the WA
// band hue at a hand-tuned alpha so the cell composites over `AppPaper` into a
// readable band cue — deep amber on X, gold on 10, fading to pale yellow on 9,
// red on 8/7, blue on 6/5, muted on the rest.
val AppRingTintX      = Color(0x8CD4A017)
val AppRingTintGold   = Color(0x73F0D04A)
val AppRingTintYellow = Color(0x38F0D04A)
val AppRingTintRed    = Color(0x52D94B3B)
val AppRingTintRedLt  = Color(0x2BD94B3B)
val AppRingTintBlue   = Color(0x4D4EA8C9)
val AppRingTintBlueLt = Color(0x264EA8C9)
val AppRingTintBlack  = Color(0x141F2A26)
val AppRingTintMiss   = Color(0x2EB5614A)

val AppSuccess = AppPine
val AppWarning = AppMaple
val AppDanger  = Color(0xFFA0392A)
val AppInfo    = AppPond

// Argb int form for system chrome on cold launch (status bar paint
// before any Compose state is up). Always the light paper because the
// splash itself is light.
val AppPaperArgb: Int = 0xFFEEF2EC.toInt()

// endregion

// region — Dark ("Yofuke") tokens — used by the dark M3 ColorScheme only

val AppPaperDark   = Color(0xFF161B19)
val AppPaper2Dark  = Color(0xFF1C2220)
val AppCreamDark   = Color(0xFF232A27)

val AppInkDark   = Color(0xFFE7EBE4)
val AppInk2Dark  = Color(0xFFA4ADA7)
val AppInk3Dark  = Color(0xFF6E7872)

val AppLineDark  = Color(0xFF2D352F)
val AppLine2Dark = Color(0xFF242A26)

val AppPondDark    = Color(0xFF79AABC)
val AppPondDkDark  = Color(0xFFA3C8D6)
val AppPondLtDark  = Color(0xFF3A5663)
val AppDeepDark    = Color(0xFFC4DDE4)

val AppMossDark    = Color(0xFF94AD7C)
val AppPineDark    = Color(0xFFA8C08A)
val AppMapleDark   = Color(0xFFD97A5E)
val AppStoneDark   = Color(0xFF8E9893)
val AppDangerDark  = Color(0xFFE08266)

// endregion

/**
 * Snapshot of every theme-adaptive Kenrokuen token. The MaterialTheme
 * ColorScheme is derived from one of [LightKenrokuen] / [DarkKenrokuen]
 * inside `BowPressTheme`. Direct `App*` token references stay light —
 * widening them to be theme-adaptive is a follow-up refactor.
 */
data class KenrokuenPalette(
    val paper: Color, val paper2: Color, val cream: Color,
    val ink: Color, val ink2: Color, val ink3: Color,
    val line: Color, val line2: Color,
    val pond: Color, val pondDk: Color, val pondLt: Color, val deep: Color,
    val moss: Color, val pine: Color,
    val maple: Color, val stone: Color, val danger: Color,
)

val LightKenrokuen = KenrokuenPalette(
    paper = AppPaper, paper2 = AppPaper2, cream = AppCream,
    ink = AppInk, ink2 = AppInk2, ink3 = AppInk3,
    line = AppLine, line2 = AppLine2,
    pond = AppPond, pondDk = AppPondDk, pondLt = AppPondLt, deep = AppDeep,
    moss = AppMoss, pine = AppPine,
    maple = AppMaple, stone = AppStone, danger = AppDanger,
)

val DarkKenrokuen = KenrokuenPalette(
    paper = AppPaperDark, paper2 = AppPaper2Dark, cream = AppCreamDark,
    ink = AppInkDark, ink2 = AppInk2Dark, ink3 = AppInk3Dark,
    line = AppLineDark, line2 = AppLine2Dark,
    pond = AppPondDark, pondDk = AppPondDkDark, pondLt = AppPondLtDark, deep = AppDeepDark,
    moss = AppMossDark, pine = AppPineDark,
    maple = AppMapleDark, stone = AppStoneDark, danger = AppDangerDark,
)

/**
 * Legacy wrapper kept for Wave 2 feature modules. Light-only; the dark
 * counterparts live above as separate tokens and the M3 ColorScheme.
 */
object BowPressColors {
    val Accent = AppPondDk
    val AccentDark = AppDeep
    val AccentContainer = AppPondLt
    val OnAccentContainer = AppPaper

    val Surface = AppPaper
    val SurfaceDark = AppInk
    val OnSurface = AppInk
    val OnSurfaceDark = AppPaper

    val Outline = AppLine
    val Error = AppDanger
}
