package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone

/**
 * Canvas that draws the target face and captures drag-to-plot gestures. Mirrors
 * bowpress-ios/Sources/BowPress/Session/TargetPlotView.swift — the exact line refs for
 * the scoring logic are on [TargetGeometry]. UI-side references:
 *
 *   - Ring rendering: iOS lines 83–86 (target face is a pre-rendered image on iOS;
 *     Android draws vector circles).
 *   - Drag-gesture touch-offset trick (thumb sits ~80pt below the dot): iOS lines
 *     73–74 and 128–133. We mirror the same offset on Android so placed dots don't
 *     disappear under the user's finger.
 *   - Classification (ring from radius, zone from atan2): iOS lines 148–174. Android
 *     uses [TargetGeometry.classifyWithDotRadius] so a dot that visibly touches a
 *     higher ring scores it (iOS lines 155–158).
 *   - Normalized (plotX, plotY) storage convention (positive Y = screen-down): iOS
 *     lines 163–165.
 */
const val TARGET_PLOT_TEST_TAG = "target_plot_canvas"
private val TOUCH_OFFSET_DP = 80.dp

@Composable
fun TargetPlot(
    arrows: List<ArrowPlot>,
    onArrowPlotted: (plotX: Double, plotY: Double, ring: Int, zone: Zone) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    arrowDiameterMm: Double = 5.0,
    faceType: TargetFaceType = TargetFaceType.SIX_RING,
) {
    val density = LocalDensity.current
    val touchOffsetPx = with(density) { TOUCH_OFFSET_DP.toPx() }
    var dragPreview by remember { mutableStateOf<Offset?>(null) }
    val geometry = TargetGeometry.forFace(faceType)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(TARGET_PLOT_TEST_TAG)
            .pointerInput(isEnabled, touchOffsetPx, faceType) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        dragPreview = Offset(start.x, start.y - touchOffsetPx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragPreview = Offset(
                            change.position.x,
                            change.position.y - touchOffsetPx,
                        )
                    },
                    onDragEnd = {
                        val placement = dragPreview ?: return@detectDragGestures
                        dragPreview = null
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radiusPx = (minOf(size.width, size.height) / 2f).toDouble()
                        if (radiusPx <= 0.0) return@detectDragGestures
                        // iOS stores plotX = dx/radius, plotY = (point.y - center.y)/radius.
                        // Positive plotY = south. (TargetPlotView.swift lines 164–165.)
                        val plotX = (placement.x - centerX).toDouble() / radiusPx
                        val plotY = (placement.y - centerY).toDouble() / radiusPx
                        // Arrow dot radius in normalized units — iOS line 81 uses the
                        // same scaling via MM_PER_NORM_UNIT.
                        val dotNormRadius =
                            (arrowDiameterMm / 2.0) / geometry.mmPerNormUnit
                        val result = geometry.classifyWithDotRadius(
                            plotX = plotX,
                            plotY = plotY,
                            dotNormRadius = dotNormRadius,
                        )
                        val ring = result.ring ?: return@detectDragGestures
                        onArrowPlotted(plotX, plotY, ring, result.zone)
                    },
                    onDragCancel = { dragPreview = null },
                )
            },
    ) {
        drawTargetFace(faceType)
        drawExistingArrows(arrows = arrows, arrowDiameterMm = arrowDiameterMm, geometry = geometry)
        drawDragPreview(preview = dragPreview, arrowDiameterMm = arrowDiameterMm, geometry = geometry)
    }
}

// ---- Drawing helpers ----

private fun DrawScope.drawTargetFace(faceType: TargetFaceType) {
    when (faceType) {
        TargetFaceType.SIX_RING -> drawSixRingFace()
        TargetFaceType.TEN_RING -> drawTenRingFace()
    }
}

/** Inner-6 indoor face — keeps the legacy Android look (blue→red→yellow plus X). */
private fun DrawScope.drawSixRingFace() {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val g = TargetGeometry.SixRing

    // Target face colors approximating the WA 10-ring face. Draw outermost → innermost
    // so inner rings paint on top.
    val rings: List<Pair<Float, Color>> = listOf(
        1.0f to WHITE,
        g.R7_RADIUS.toFloat() to BLUE,
        g.R8_RADIUS.toFloat() to RED,
        g.R10_RADIUS.toFloat() to YELLOW,
    )
    for ((normRadius, color) in rings) {
        drawCircle(color = color, radius = normRadius * radiusPx, center = center)
    }

    // Ring lines at every threshold so the archer can see the scoring boundaries.
    val thresholds = listOf(
        g.X_RADIUS,
        g.R10_RADIUS,
        g.R9_RADIUS,
        g.R8_RADIUS,
        g.R7_RADIUS,
        g.R6_RADIUS,
    )
    drawRingDividers(thresholds, radiusPx, center)
    drawOuterEdge(radiusPx, center)
    drawXTick(g.X_RADIUS, radiusPx, center)
}

/**
 * Full WA 10-ring face. Ten concentric fills outer→inner alternating the canonical WA
 * colors (white, white, black, black, blue, blue, red, red, yellow, yellow) plus an X
 * tick at the centre.
 */
private fun DrawScope.drawTenRingFace() {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val g = TargetGeometry.TenRing

    // Paired (outer edge threshold, fill color) — outermost first so each subsequent
    // draw paints over the previous. Colors go white, white, black, black, blue, blue,
    // red, red, yellow, yellow (outer→inner).
    val ringFills: List<Pair<Double, Color>> = listOf(
        g.R1_RADIUS to WHITE,
        g.R2_RADIUS to WHITE,
        g.R3_RADIUS to BLACK,
        g.R4_RADIUS to BLACK,
        g.R5_RADIUS to BLUE,
        g.R6_RADIUS to BLUE,
        g.R7_RADIUS to RED,
        g.R8_RADIUS to RED,
        g.R9_RADIUS to YELLOW,
        g.R10_RADIUS to YELLOW,
    )
    for ((edge, color) in ringFills) {
        drawCircle(color = color, radius = (edge * radiusPx).toFloat(), center = center)
    }

    // Thin dividers on every ring boundary.
    val thresholds = listOf(
        g.X_RADIUS,
        g.R10_RADIUS,
        g.R9_RADIUS,
        g.R8_RADIUS,
        g.R7_RADIUS,
        g.R6_RADIUS,
        g.R5_RADIUS,
        g.R4_RADIUS,
        g.R3_RADIUS,
        g.R2_RADIUS,
        g.R1_RADIUS,
    )
    drawRingDividers(thresholds, radiusPx, center)
    drawOuterEdge(radiusPx, center)
    drawXTick(g.X_RADIUS, radiusPx, center)
}

private fun DrawScope.drawRingDividers(
    thresholds: List<Double>,
    radiusPx: Float,
    center: Offset,
) {
    val ringLineColor = Color(0xFF333333)
    for (t in thresholds) {
        drawCircle(
            color = ringLineColor,
            radius = (t * radiusPx).toFloat(),
            center = center,
            style = Stroke(width = 1.5f),
        )
    }
}

private fun DrawScope.drawOuterEdge(radiusPx: Float, center: Offset) {
    drawCircle(
        color = Color(0xFF333333),
        radius = radiusPx,
        center = center,
        style = Stroke(width = 2f),
    )
}

/**
 * Small cross-hair at the centre of the X ring so the archer can see where to aim
 * when the X is drawn as part of the innermost yellow fill.
 */
private fun DrawScope.drawXTick(xRadius: Double, radiusPx: Float, center: Offset) {
    val tickHalf = (xRadius * radiusPx * 0.35f).toFloat()
    val tickColor = Color(0xFF333333)
    drawLine(
        color = tickColor,
        start = Offset(center.x - tickHalf, center.y),
        end = Offset(center.x + tickHalf, center.y),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = tickColor,
        start = Offset(center.x, center.y - tickHalf),
        end = Offset(center.x, center.y + tickHalf),
        strokeWidth = 1.5f,
    )
}

private fun DrawScope.drawExistingArrows(
    arrows: List<ArrowPlot>,
    arrowDiameterMm: Double,
    geometry: TargetGeometry,
) {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val dotDiameterPx = (arrowDiameterMm / geometry.mmPerNormUnit * radiusPx)
        .toFloat()
        .coerceAtLeast(8f)

    arrows.forEachIndexed { index, arrow ->
        val px = arrow.plotX ?: return@forEachIndexed
        val py = arrow.plotY ?: return@forEachIndexed
        // plotY is stored south-positive (iOS line 165), so center.y + py*radius is the
        // correct screen position — no flip needed. Matches iOS line 201.
        val cx = (center.x + px * radiusPx).toFloat()
        val cy = (center.y + py * radiusPx).toFloat()
        drawCircle(
            color = dotColor(arrow.ring),
            radius = dotDiameterPx / 2f,
            center = Offset(cx, cy),
        )
        // Outline — white for dark fills, black for light fills — so the dot is
        // always visible on its own ring colour.
        drawCircle(
            color = dotOutline(arrow.ring),
            radius = dotDiameterPx / 2f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f),
        )
        if (index == arrows.size - 1) {
            // Emphasize the most recent plot.
            drawCircle(
                color = Color.White,
                radius = dotDiameterPx / 2f + 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f),
            )
        }
    }
}

private fun DrawScope.drawDragPreview(
    preview: Offset?,
    arrowDiameterMm: Double,
    geometry: TargetGeometry,
) {
    if (preview == null) return
    val radiusPx = minOf(size.width, size.height) / 2f
    val dotDiameterPx = (arrowDiameterMm / geometry.mmPerNormUnit * radiusPx)
        .toFloat()
        .coerceAtLeast(8f)
    drawCircle(
        color = Color.White,
        radius = dotDiameterPx / 2f,
        center = preview,
        style = Stroke(width = 1.5f),
    )
}

/**
 * Arrow-dot fill colour. Rings 6..X are unchanged from the legacy Android renderer so
 * historical sessions keep their look; rings 1..5 extend the palette to match the
 * canonical WA face colours (ring 5 on blue, rings 3–4 on black, rings 1–2 on white).
 */
private fun dotColor(ring: Int): Color = when (ring) {
    11 -> GOLD
    10, 9 -> YELLOW
    8, 7 -> RED
    6, 5 -> BLUE
    4, 3 -> BLACK
    2, 1 -> WHITE
    else -> Color.Gray
}

/** Outline colour that contrasts with the arrow-dot fill so the dot stays legible. */
private fun dotOutline(ring: Int): Color = when (ring) {
    // Light fills need a dark outline.
    2, 1 -> Color.Black
    // Everything else gets a thin white outline.
    else -> Color.White
}

private val WHITE = Color(0xFFFFFDF7)
private val BLACK = Color(0xFF1B1B1B)
private val GOLD = Color(0xFFFFD900)
private val YELLOW = Color(0xFFFFEE33)
private val RED = Color(0xFFE04738)
private val BLUE = Color(0xFF00BAE3)

/** Accent color the task brief calls out. Exposed so screens can re-use it if needed. */
@Suppress("unused")
internal val SESSION_ACCENT = Color(0xFFD14B2E)
