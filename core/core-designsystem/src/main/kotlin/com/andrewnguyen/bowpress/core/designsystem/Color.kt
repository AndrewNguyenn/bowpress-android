package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.ui.graphics.Color

// Kenrokuen palette — light-mode-only per spec. Mirrors iOS AppTheme tokens.
// All values are fixed hex: no dark-mode variants.

// Paper / surface
val AppPaper  = Color(0xFFEEF2EC)
val AppPaper2 = Color(0xFFE4EBE3)
val AppCream  = Color(0xFFF6F8F3)

// Ink / text
val AppInk  = Color(0xFF1F2A26)
val AppInk2 = Color(0xFF4A5752)
val AppInk3 = Color(0xFF8A9690)

// Hairline / divider
val AppLine  = Color(0xFFC7D2C9)
val AppLine2 = Color(0xFFD9E1D8)

// Pond (primary accent)
val AppPond   = Color(0xFF4A7989)
val AppPondDk = Color(0xFF2D5A6B)
val AppPondLt = Color(0xFF8FB3BF)
val AppDeep   = Color(0xFF1E3E4A)

// Moss / pine / maple / stone
val AppMoss  = Color(0xFF6D8551)
val AppPine  = Color(0xFF4A5F3A)
val AppMaple = Color(0xFFB5614A)
val AppStone = Color(0xFF9AA3A0)

// Target face (real WA colors — never reskinned)
val AppTgtWhite  = Color(0xFFF6F8F3)
val AppTgtBlack  = Color(0xFF1F2A26)
val AppTgtBlue   = Color(0xFF4EA8C9)
val AppTgtRed    = Color(0xFFD94B3B)
val AppTgtYellow = Color(0xFFF0D04A)

// WA bar-fill palette — slightly muted variants used by analytics rows
// (Log row arrow strip etc.). Matches iOS appWA*Fill tokens 1:1 so the
// per-ring color reads the same on both platforms.
val AppWAGoldFill  = Color(0xFFD8A23A)
val AppWARedFill   = Color(0xFFB04A3A)
val AppWABlueFill  = Color(0xFF3A6F8A)
val AppWABlackFill = Color(0xFF2A2A28)
val AppWAWhiteFill = Color(0xFFF4F1EA)

// Semantic
val AppSuccess = AppPine
val AppWarning = AppMaple
val AppDanger  = Color(0xFFA0392A)
val AppInfo    = AppPond

// Legacy wrapper — kept for Wave 2 feature modules still referencing
// BowPressColors.*. Fields are aliases of the new Kenrokuen tokens so the
// feature modules continue to compile while Wave 2 rewrites them.
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
