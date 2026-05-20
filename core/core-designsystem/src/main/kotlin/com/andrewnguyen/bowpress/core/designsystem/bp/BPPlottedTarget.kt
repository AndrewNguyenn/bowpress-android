package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow
import com.andrewnguyen.bowpress.core.model.ArrowPlot

/**
 * Read-only WA 10-ring target with arrow dots overlaid — the static
 * shot-distribution render.
 *
 * Promoted to core-designsystem so both `feature-analytics`
 * (`SessionDetailScreen`) and `feature-social` (`FriendSessionDetailScreen`)
 * share one component instead of duplicating the geometry. Uses the `AppTgt*`
 * design tokens for the ring fills.
 *
 * Coordinate convention matches the [ArrowPlot] model: `plotX` ∈ [-1, 1]
 * east-positive, `plotY` ∈ [-1, 1] south-positive. Plots missing coordinates
 * (legacy data) are skipped.
 */
@Composable
fun BPPlottedTarget(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val radiusPx = minOf(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // WA 10-ring fills: outer → inner so inner rings paint on top.
        // Each pair is (outer normalized radius, fill color). 10 rings.
        val ringFills = listOf(
            1.0f to AppTgtWhite,
            0.9f to AppTgtWhite,
            0.8f to AppTgtBlack,
            0.7f to AppTgtBlack,
            0.6f to AppTgtBlue,
            0.5f to AppTgtBlue,
            0.4f to AppTgtRed,
            0.3f to AppTgtRed,
            0.2f to AppTgtYellow,
            0.1f to AppTgtYellow,
        )
        for ((edge, color) in ringFills) {
            drawCircle(color = color, radius = edge * radiusPx, center = center)
        }
        // Outer edge stroke + hair-thin ring dividers so boundaries read.
        drawCircle(
            color = DIVIDER,
            radius = radiusPx,
            center = center,
            style = Stroke(width = 1.5f),
        )
        for (edge in listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f)) {
            drawCircle(
                color = DIVIDER,
                radius = edge * radiusPx,
                center = center,
                style = Stroke(width = 0.5f),
            )
        }
        // X tick at centre — small crosshair ring.
        drawCircle(
            color = DIVIDER,
            radius = 0.05f * radiusPx,
            center = center,
            style = Stroke(width = 0.8f),
        )

        // Arrow dots — uniform size; ink fill with a paper hairline so dots
        // stay legible over both the dark and light rings.
        val dotRadius = 0.025f * radiusPx
        for (plot in arrows) {
            val px = plot.plotX ?: continue
            val py = plot.plotY ?: continue
            val dot = Offset(
                x = center.x + px.toFloat() * radiusPx,
                y = center.y + py.toFloat() * radiusPx,
            )
            drawCircle(color = AppInk, radius = dotRadius, center = dot)
            drawCircle(
                color = AppPaper,
                radius = dotRadius,
                center = dot,
                style = Stroke(width = 1f),
            )
        }
    }
}

private val DIVIDER = Color(0x66000000)
