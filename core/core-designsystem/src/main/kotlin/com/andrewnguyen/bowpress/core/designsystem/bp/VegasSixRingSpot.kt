package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow

/**
 * Canonical 40cm Vegas inner-face renderer — single source of truth for every
 * Vegas SixRing spot the app paints (feed cards via `BPPlottedTarget`, live
 * session + pen lens via `TargetPlot.drawMultiSpotCard`). Mirrors iOS
 * `MultiSpotFaceCanvas.drawSpot`: outer-to-inner fill stack (blue → red →
 * yellow), six hairline ring dividers, and a thin centre cross at the X.
 *
 * Band radii (fractions of [radius]):
 *  - 1.00 blue (ring 6)
 *  - 0.80 red  (rings 7/8)
 *  - 0.40 yellow (rings 9/10/X)
 *
 * Divider radii: 0.075 (X-ring edge), 0.20 (ring 10/9), 0.40 (9/8),
 * 0.60 (8/7), 0.80 (7/6), 1.00 (outer).
 */
fun DrawScope.drawVegasSixRingSpot(center: Offset, radius: Float) {
    drawCircle(color = AppTgtBlue, radius = radius, center = center)
    drawCircle(color = AppTgtRed, radius = radius * 0.80f, center = center)
    drawCircle(color = AppTgtYellow, radius = radius * 0.40f, center = center)

    VEGAS_DIVIDERS.forEach { frac ->
        drawCircle(
            color = VEGAS_DIVIDER_INK,
            radius = radius * frac,
            center = center,
            style = Stroke(width = 0.9f),
        )
    }

    val tick = radius * 0.03f
    drawLine(
        VEGAS_X_TICK_INK,
        Offset(center.x - tick, center.y),
        Offset(center.x + tick, center.y),
        strokeWidth = 0.8f,
    )
    drawLine(
        VEGAS_X_TICK_INK,
        Offset(center.x, center.y - tick),
        Offset(center.x, center.y + tick),
        strokeWidth = 0.8f,
    )
}

private val VEGAS_DIVIDERS = floatArrayOf(0.075f, 0.20f, 0.40f, 0.60f, 0.80f, 1.00f)
private val VEGAS_DIVIDER_INK = Color(0xFF333333)
private val VEGAS_X_TICK_INK = Color(0x80333333)
