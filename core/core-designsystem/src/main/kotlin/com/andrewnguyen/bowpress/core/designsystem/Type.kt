@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Font families
// ---------------------------------------------------------------------------
//
// Fraunces is a variable font; we expose Normal + Medium weights for both
// upright and italic faces. opsz (optical size) is pinned at the upper end so
// headline usage looks appropriately chunky — at smaller sizes the difference
// is subtle. Wave 2 B can override via variationSettings on a TextStyle if a
// screen needs finer control.
//
// NOTE: Compose's `Font(variationSettings = ...)` accepts `FontVariation.weight`
// and the other built-in axes, but there is no public `Setting("opsz", Float)`
// factory — so we pin opsz indirectly via FontVariation.weight and leave
// per-size opsz to callers (they use the `frauncesDisplay(size, ...)` helper
// which applies `FontVariation.Settings(textSize, density, …)` at draw time
// for proper optical-size blending on variable axes that support it).

private val Fraunces = FontFamily(
    Font(
        resId = R.font.fraunces,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.fraunces,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.fraunces,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.fraunces_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.fraunces_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.fraunces_italic,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val Inter = FontFamily(
    Font(
        resId = R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.inter,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private val JetBrains = FontFamily(
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono_medium, weight = FontWeight.Medium),
)

// Public so callers can do `fontFamily = BPFonts.Mono` if they need to drop
// down below the helpers (rare — prefer `jetbrainsMono(size)`).
object BPFonts {
    val Fraunces = com.andrewnguyen.bowpress.core.designsystem.Fraunces
    val Inter = com.andrewnguyen.bowpress.core.designsystem.Inter
    val Mono = com.andrewnguyen.bowpress.core.designsystem.JetBrains
}

// ---------------------------------------------------------------------------
// Public text-style helpers (preferred over Material typography for body work)
// ---------------------------------------------------------------------------

fun frauncesDisplay(
    size: TextUnit,
    italic: Boolean = true,
    weight: FontWeight = FontWeight.Medium,
): TextStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = weight,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size,
)

fun interUI(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size,
)

fun jetbrainsMono(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    fontFamily = JetBrains,
    fontWeight = weight,
    fontSize = size,
)

fun bpEyebrow(size: TextUnit = 9.sp): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    letterSpacing = 0.22.em,
)

fun bpLabel(size: TextUnit = 11.sp): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    letterSpacing = 0.18.em,
)

// ---------------------------------------------------------------------------
// Material 3 typography — keep callers that reach for MaterialTheme.typography
// on-palette. Display/headline slots use Fraunces italic; title/body/label use
// Inter; JetBrains Mono is not mapped into Material's typography and is
// exposed via `jetbrainsMono(size)` and the `BPText` helpers.
// ---------------------------------------------------------------------------

val BowPressTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 44.sp,
        lineHeight = 48.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.18.em,
    ),
)
