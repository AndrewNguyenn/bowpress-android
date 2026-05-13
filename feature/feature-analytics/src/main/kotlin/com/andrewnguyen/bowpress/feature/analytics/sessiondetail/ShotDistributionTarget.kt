package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.andrewnguyen.bowpress.core.model.ArrowPlot

/**
 * Static read-only WA 10-ring target with arrow dots overlaid. Mirrors the iOS
 * SessionDetailSheet shot-distribution view in shape — feature-session already
 * has a richer interactive [TargetPlot] but lives in a different module; for
 * now we duplicate the read-only render here and a follow-up iter can promote
 * the shared geometry/colours to core-designsystem.
 *
 * Coordinate convention matches the ArrowPlot model used in the feature-session
 * port: `plotX` ∈ [-1, 1] east-positive, `plotY` ∈ [-1, 1] south-positive.
 */
@Composable
fun ShotDistributionTarget(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("shot_distribution_target"),
    ) {
        val radiusPx = minOf(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // WA 10-ring fills: outer → inner so inner rings paint on top.
        // Each pair is (outer normalized radius, fill color). 10 rings + X.
        val ringFills = listOf(
            1.0f to WA_WHITE,
            0.9f to WA_WHITE,
            0.8f to WA_BLACK,
            0.7f to WA_BLACK,
            0.6f to WA_BLUE,
            0.5f to WA_BLUE,
            0.4f to WA_RED,
            0.3f to WA_RED,
            0.2f to WA_YELLOW,
            0.1f to WA_YELLOW,
        )
        for ((edge, color) in ringFills) {
            drawCircle(color = color, radius = edge * radiusPx, center = center)
        }
        // Outer edge stroke + hair-thin ring dividers so boundaries are visible.
        drawCircle(
            color = WA_DIVIDER,
            radius = radiusPx,
            center = center,
            style = Stroke(width = 1.5f),
        )
        for (edge in listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f)) {
            drawCircle(
                color = WA_DIVIDER,
                radius = edge * radiusPx,
                center = center,
                style = Stroke(width = 0.5f),
            )
        }
        // X tick at centre — small crosshair.
        val xTickPx = 0.05f * radiusPx
        drawCircle(
            color = WA_DIVIDER,
            radius = xTickPx,
            center = center,
            style = Stroke(width = 0.8f),
        )

        // Arrow dots — same dot size for every plot.
        val dotRadius = 0.025f * radiusPx
        for (plot in arrows) {
            val px = plot.plotX
            val py = plot.plotY
            if (px == null || py == null) continue
            val cx = center.x + px.toFloat() * radiusPx
            val cy = center.y + py.toFloat() * radiusPx
            drawCircle(color = DOT_FILL, radius = dotRadius, center = Offset(cx, cy))
            drawCircle(
                color = DOT_STROKE,
                radius = dotRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1f),
            )
        }
    }
}

private val WA_WHITE = Color(0xFFFFFFFF)
private val WA_BLACK = Color(0xFF222222)
private val WA_BLUE = Color(0xFF3F8FCB)
private val WA_RED = Color(0xFFE24B3F)
private val WA_YELLOW = Color(0xFFF5D33A)
private val WA_DIVIDER = Color(0x66000000)
private val DOT_FILL = Color(0xFF1A2A3D)
private val DOT_STROKE = Color(0xFFFFFFFF)
