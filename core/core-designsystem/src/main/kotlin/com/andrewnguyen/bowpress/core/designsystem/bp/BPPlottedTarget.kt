package com.andrewnguyen.bowpress.core.designsystem.bp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout

/**
 * Read-only World-Archery target with arrow dots overlaid — the static
 * shot-distribution render.
 *
 * Promoted to core-designsystem so both `feature-analytics`
 * (`SessionDetailScreen`) and `feature-social` (`FriendSessionDetailScreen`)
 * share one component. Uses the `AppTgt*` design tokens for the ring fills.
 *
 * Respects the session's [faceType] and [layout]:
 *  - [TargetFaceType.TEN_RING] draws the full 10-ring face (5 colour bands);
 *    [TargetFaceType.SIX_RING] draws the inner-6 indoor face (yellow/red/blue
 *    only), matching how it is scored 6..X.
 *  - [TargetLayout.SINGLE] draws one face; [TargetLayout.TRIANGLE] draws the
 *    3-spot Vegas triangle (two faces top, one below); [TargetLayout.VERTICAL]
 *    draws the 3-spot vertical strip (three faces stacked).
 *
 * For multi-spot layouts the arrows are distributed across the spots by their
 * order within each end (arrow 1 → spot 1, arrow 2 → spot 2, …) — the standard
 * 3-spot convention of one arrow per face per end. Each arrow's `plotX/plotY`
 * (∈ [-1, 1] from its face's centre, east/south-positive) is then drawn
 * relative to that spot's centre. Plots missing coordinates are skipped.
 */
@Composable
fun BPPlottedTarget(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
    faceType: TargetFaceType = TargetFaceType.TEN_RING,
    layout: TargetLayout = TargetLayout.SINGLE,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (layout == TargetLayout.VERTICAL) 1f / 3f else 1f),
    ) {
        val spots = spotCenters(layout, size.width, size.height)
        // Each spot's face radius — sized so the spots don't overlap.
        val faceRadius = when (layout) {
            TargetLayout.SINGLE -> minOf(size.width, size.height) / 2f
            TargetLayout.TRIANGLE -> size.width / 4f
            TargetLayout.VERTICAL -> size.width / 2f
        }

        // Deal arrows to spots round-robin within each end (one arrow per
        // face per end is the standard 3-spot convention).
        val arrowsBySpot: Map<Int, List<ArrowPlot>> = arrows
            .groupBy { it.endId }
            .values
            .flatMap { end -> end.mapIndexed { i, plot -> (i % spots.size) to plot } }
            .groupBy({ it.first }, { it.second })

        spots.forEachIndexed { index, center ->
            drawFace(center, faceRadius, faceType)
            arrowsBySpot[index].orEmpty().forEach { plot ->
                val px = plot.plotX ?: return@forEach
                val py = plot.plotY ?: return@forEach
                drawArrowDot(
                    Offset(
                        x = center.x + px.toFloat() * faceRadius,
                        y = center.y + py.toFloat() * faceRadius,
                    ),
                    faceRadius,
                )
            }
        }
    }
}

/** Spot centres for a layout, given the canvas size. */
private fun spotCenters(layout: TargetLayout, w: Float, h: Float): List<Offset> = when (layout) {
    TargetLayout.SINGLE -> listOf(Offset(w / 2f, h / 2f))
    // Vegas triangle — two faces top, one centred below.
    TargetLayout.TRIANGLE -> listOf(
        Offset(w * 0.27f, h * 0.30f),
        Offset(w * 0.73f, h * 0.30f),
        Offset(w * 0.50f, h * 0.72f),
    )
    // Vertical 3-spot — three faces stacked (canvas is 1:3 tall).
    TargetLayout.VERTICAL -> listOf(
        Offset(w / 2f, h / 6f),
        Offset(w / 2f, h / 2f),
        Offset(w / 2f, h * 5f / 6f),
    )
}

/**
 * Ring bands for a face, outer → inner. A 10-ring face has all five colour
 * bands; a 6-ring face is the inner-6 — yellow/red/blue only — so its bands
 * span the full radius.
 */
private fun ringBands(faceType: TargetFaceType): List<Pair<Float, Color>> = when (faceType) {
    TargetFaceType.TEN_RING -> listOf(
        1.0f to AppTgtWhite, 0.9f to AppTgtWhite,
        0.8f to AppTgtBlack, 0.7f to AppTgtBlack,
        0.6f to AppTgtBlue, 0.5f to AppTgtBlue,
        0.4f to AppTgtRed, 0.3f to AppTgtRed,
        0.2f to AppTgtYellow, 0.1f to AppTgtYellow,
    )
    // Inner-6 indoor face: 6 scoring rings (blue/red/yellow), no white/black.
    TargetFaceType.SIX_RING -> listOf(
        1.0f to AppTgtBlue, 0.833f to AppTgtBlue,
        0.667f to AppTgtRed, 0.5f to AppTgtRed,
        0.333f to AppTgtYellow, 0.167f to AppTgtYellow,
    )
}

/** Draw one WA face centred at [center] with the given [radius]. */
private fun DrawScope.drawFace(center: Offset, radius: Float, faceType: TargetFaceType) {
    val bands = ringBands(faceType)
    bands.forEach { (edge, color) ->
        drawCircle(color = color, radius = edge * radius, center = center)
    }
    // Outer edge + hair-thin ring dividers.
    drawCircle(color = DIVIDER, radius = radius, center = center, style = Stroke(width = 1.5f))
    bands.drop(1).map { it.first }.distinct().forEach { edge ->
        drawCircle(
            color = DIVIDER,
            radius = edge * radius,
            center = center,
            style = Stroke(width = 0.5f),
        )
    }
    // X tick at centre.
    drawCircle(color = DIVIDER, radius = 0.05f * radius, center = center, style = Stroke(width = 0.8f))
}

/** Draw one arrow dot — ink fill, paper hairline so it reads on every band. */
private fun DrawScope.drawArrowDot(at: Offset, faceRadius: Float) {
    val dotRadius = 0.05f * faceRadius
    drawCircle(color = AppInk, radius = dotRadius, center = at)
    drawCircle(color = AppPaper, radius = dotRadius, center = at, style = Stroke(width = 1f))
}

private val DIVIDER = Color(0x66000000)
