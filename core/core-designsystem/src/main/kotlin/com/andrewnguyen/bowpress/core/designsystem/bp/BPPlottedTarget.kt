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
import com.andrewnguyen.bowpress.core.model.MultiSpotGeometry
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
 * For a multi-spot layout the arrows are placed by **position** — each arrow's
 * stored `plotX/plotY` (∈ [-1, 1] of the whole *card*, east/south-positive)
 * is bucketed to its nearest spot via [MultiSpotGeometry.assignArrows] and
 * drawn at its recentered spot-local position. This matches how a live
 * multi-spot session is scored (nearest-spot, not order-within-end) and
 * mirrors iOS `AggregateMultiSpotFace` / `PerSpotBreakdown`. Plots missing
 * coordinates are skipped.
 */
@Composable
fun BPPlottedTarget(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
    faceType: TargetFaceType = TargetFaceType.TEN_RING,
    layout: TargetLayout = TargetLayout.SINGLE,
) {
    val multiSpot = MultiSpotGeometry.preset(layout)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // The Vegas card is square — both triangle and vertical spot
            // presets are normalised to a 1:1 face square.
            .aspectRatio(1f),
    ) {
        if (multiSpot == null) {
            // Single face — one bullseye, plotX/plotY relative to its centre.
            val center = Offset(size.width / 2f, size.height / 2f)
            val faceRadius = minOf(size.width, size.height) / 2f
            drawFace(center, faceRadius, faceType)
            arrows.forEach { plot ->
                val px = plot.plotX ?: return@forEach
                val py = plot.plotY ?: return@forEach
                drawArrowDot(
                    Offset(center.x + px.toFloat() * faceRadius, center.y + py.toFloat() * faceRadius),
                    faceRadius,
                )
            }
            return@Canvas
        }

        // Multi-spot Vegas card — three 6-ring spots. Spot centres + radius
        // come from the shared geometry (normalised to the face square).
        val minEdge = minOf(size.width, size.height)
        val spotRadius = multiSpot.radiusNorm.toFloat() * minEdge
        val spotCentres = multiSpot.centers.map { c ->
            Offset(c.x.toFloat() * size.width, c.y.toFloat() * size.height)
        }
        spotCentres.forEach { drawFace(it, spotRadius, TargetFaceType.SIX_RING) }
        // Position-based bucketing — each arrow on its actual spot, drawn at
        // its recentered spot-local offset (localX/localY ∈ −1..1 of the
        // spot radius). Mirrors the live-session scoring path.
        multiSpot.assignArrows(arrows).forEach { perSpot ->
            val centre = spotCentres[perSpot.spotIndex]
            drawArrowDot(
                Offset(
                    centre.x + perSpot.localX.toFloat() * spotRadius,
                    centre.y + perSpot.localY.toFloat() * spotRadius,
                ),
                spotRadius,
            )
        }
    }
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
