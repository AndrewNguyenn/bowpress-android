package com.andrewnguyen.bowpress.feature.session

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.MultiSpotGeometry
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.Zone
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Canvas that draws the target face and captures drag-to-plot gestures. Mirrors
 * bowpress-ios/Sources/BowPress/Session/TargetPlotView.swift (iOS f2be7ea — pen
 * magnifier path):
 *
 *   - **Pen magnifier (iOS f2be7ea):** finger commits AT the touch point. The
 *     thumb-offset trick is gone — instead [onLensSnapshotChanged] fires on
 *     every drag tick with a [PenLensSnapshot] that the parent renders via
 *     [PenLensOverlay] at the SCREEN ROOT so the lens can extend past the
 *     target's frame (up into header space).
 *   - Classification (ring from radius, zone from atan2): iOS lines 148–174. The
 *     single-face path uses [TargetGeometry.classifyWithDotRadius]; a multi-spot
 *     [TargetLayout] routes scoring through [MultiSpotGeometry] (nearest-spot,
 *     spot-local ring banding) — iOS TargetPlotView.swift:488-507.
 *   - Normalized (plotX, plotY) storage convention (positive Y = screen-down): iOS
 *     lines 163–165.
 */
const val TARGET_PLOT_TEST_TAG = "target_plot_canvas"

@Composable
fun TargetPlot(
    arrows: List<ArrowPlot>,
    onArrowPlotted: (plotX: Double, plotY: Double, ring: Int, zone: Zone) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    arrowDiameterMm: Double = 5.0,
    faceType: TargetFaceType = TargetFaceType.SIX_RING,
    targetLayout: TargetLayout = TargetLayout.SINGLE,
    onLensSnapshotChanged: (PenLensSnapshot?) -> Unit = {},
) {
    val geometry = TargetGeometry.forFace(faceType)
    val multiSpot = MultiSpotGeometry.preset(targetLayout)
    // dragPreview is the live touch position in the canvas's local pixel
    // space. Cleared on drag end / cancel.
    var dragPreview by remember { mutableStateOf<Offset?>(null) }
    // The Canvas's top-left in the screen-root coord space — needed so the
    // lens snapshot is expressed in root coords and the lens can float above.
    var faceOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { faceOriginInWindow = it.positionInWindow() },
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .testTag(TARGET_PLOT_TEST_TAG)
                .pointerInput(isEnabled, faceType, targetLayout, arrowDiameterMm) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            dragPreview = start
                            onLensSnapshotChanged(
                                buildPenLensSnapshot(
                                    touch = start,
                                    faceOriginInWindow = faceOriginInWindow,
                                    faceSizePx = size.width.toFloat(),
                                    arrows = arrows,
                                    arrowDiameterMm = arrowDiameterMm,
                                    geometry = geometry,
                                    faceType = faceType,
                                    targetLayout = targetLayout,
                                ),
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPreview = change.position
                            onLensSnapshotChanged(
                                buildPenLensSnapshot(
                                    touch = change.position,
                                    faceOriginInWindow = faceOriginInWindow,
                                    faceSizePx = size.width.toFloat(),
                                    arrows = arrows,
                                    arrowDiameterMm = arrowDiameterMm,
                                    geometry = geometry,
                                    faceType = faceType,
                                    targetLayout = targetLayout,
                                ),
                            )
                        },
                        onDragEnd = {
                            val placement = dragPreview
                            dragPreview = null
                            onLensSnapshotChanged(null)
                            if (placement == null) return@detectDragGestures
                            val result = scorePlot(
                                placement = placement,
                                faceSizePx = minOf(size.width, size.height).toFloat(),
                                arrowDiameterMm = arrowDiameterMm,
                                geometry = geometry,
                                multiSpot = multiSpot,
                            ) ?: return@detectDragGestures
                            onArrowPlotted(result.plotX, result.plotY, result.ring, result.zone)
                        },
                        onDragCancel = {
                            dragPreview = null
                            onLensSnapshotChanged(null)
                        },
                    )
                },
        ) {
            if (multiSpot != null) {
                drawMultiSpotCard(multiSpot, size)
            } else {
                drawTargetFace(faceType)
            }
            drawExistingArrows(
                arrows = arrows,
                arrowDiameterMm = arrowDiameterMm,
                geometry = geometry,
                multiSpot = multiSpot,
            )
        }
    }
}

/** Result of scoring a committed plot. */
internal data class PlotResult(
    val plotX: Double,
    val plotY: Double,
    val ring: Int,
    val zone: Zone,
)

/**
 * Score a committed touch. For a single face this is the WA edge-rule via
 * [TargetGeometry]; for a multi-spot card it routes through [MultiSpotGeometry]
 * (nearest-spot, spot-local ring banding) — mirrors iOS `handleTap`
 * (TargetPlotView.swift:488-517). Returns null for a miss.
 */
internal fun scorePlot(
    placement: Offset,
    faceSizePx: Float,
    arrowDiameterMm: Double,
    geometry: TargetGeometry,
    multiSpot: MultiSpotGeometry?,
): PlotResult? {
    val radiusPx = (faceSizePx / 2f).toDouble()
    if (radiusPx <= 0.0) return null
    // plotX/plotY are stored normalised to the face square (-1..1), east- and
    // south-positive — the same convention on single and multi-spot faces.
    val plotX = (placement.x - radiusPx) / radiusPx
    val plotY = (placement.y - radiusPx) / radiusPx
    if (multiSpot != null) {
        // Multi-spot card: scoring is local to the nearest spot. Arrow radius
        // fraction is shaftMm / spotMm, not shaftMm / faceMm.
        val pointNorm = MultiSpotGeometry.NormPoint(x = 0.5 + plotX / 2.0, y = 0.5 + plotY / 2.0)
        val near = multiSpot.nearestSpotLocalRadius(pointNorm)
        val arrowFrac = arrowDiameterMm / multiSpot.spotDiameterMm
        val ring = multiSpot.ring(near.local, arrowFrac) ?: return null
        // Zone relative to the *spot* centre — same compass convention as the
        // single-face path, just with a different origin.
        val spotCenter = multiSpot.centers[near.spot]
        val spotLocalX = pointNorm.x - spotCenter.x
        val spotLocalY = pointNorm.y - spotCenter.y
        val zone = geometry.zone(
            normalizedDist = sqrt(spotLocalX * spotLocalX + spotLocalY * spotLocalY),
            angleDegrees = Math.toDegrees(atan2(-spotLocalY, spotLocalX)),
        )
        return PlotResult(plotX, plotY, ring, zone)
    }
    val dotNormRadius = (arrowDiameterMm / 2.0) / geometry.mmPerNormUnit
    val result = geometry.classifyWithDotRadius(plotX, plotY, dotNormRadius)
    val ring = result.ring ?: return null
    return PlotResult(plotX, plotY, ring, result.zone)
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

    val rings: List<Pair<Float, Color>> = listOf(
        1.0f to WHITE,
        g.R7_RADIUS.toFloat() to BLUE,
        g.R8_RADIUS.toFloat() to RED,
        g.R10_RADIUS.toFloat() to YELLOW,
    )
    for ((normRadius, color) in rings) {
        drawCircle(color = color, radius = normRadius * radiusPx, center = center)
    }

    val thresholds = listOf(
        g.X_RADIUS, g.R10_RADIUS, g.R9_RADIUS, g.R8_RADIUS, g.R7_RADIUS, g.R6_RADIUS,
    )
    drawRingDividers(thresholds, radiusPx, center)
    drawOuterEdge(radiusPx, center)
    drawXTick(g.X_RADIUS, radiusPx, center)
}

/**
 * Full WA 10-ring face — ten concentric fills outer→inner alternating the
 * canonical WA colours, plus an X tick at the centre.
 */
private fun DrawScope.drawTenRingFace() {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val g = TargetGeometry.TenRing

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

    val thresholds = listOf(
        g.X_RADIUS, g.R10_RADIUS, g.R9_RADIUS, g.R8_RADIUS, g.R7_RADIUS,
        g.R6_RADIUS, g.R5_RADIUS, g.R4_RADIUS, g.R3_RADIUS, g.R2_RADIUS, g.R1_RADIUS,
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

/**
 * Draw a 40cm Vegas 3-spot card — paper background + three concentric 6-ring
 * spots. Mirrors iOS `MultiSpotFaceCanvas`. Shared with the Pen lens so a
 * multi-spot session shows the actual card shape under the magnifier too.
 */
internal fun DrawScope.drawMultiSpotCard(geometry: MultiSpotGeometry, canvas: Size) {
    val minEdge = minOf(canvas.width, canvas.height)
    for (center in geometry.centers) {
        val cx = center.x.toFloat() * canvas.width
        val cy = center.y.toFloat() * canvas.height
        val r = geometry.radiusNorm.toFloat() * minEdge
        // Concentric rings — blue (6) → red (7/8) → yellow (9/10) → X.
        val bands = listOf(1.00f to BLUE, 0.80f to RED, 0.40f to YELLOW)
        for ((frac, color) in bands) {
            drawCircle(color = color, radius = r * frac, center = Offset(cx, cy))
        }
        // Ink hairlines — 0.075 is the X-ring boundary, the rest the 10/9/8/7/6
        // band boundaries.
        val dividers = listOf(0.075f, 0.20f, 0.40f, 0.60f, 0.80f, 1.00f)
        for (frac in dividers) {
            drawCircle(
                color = Color(0xFF333333),
                radius = r * frac,
                center = Offset(cx, cy),
                style = Stroke(width = 0.9f),
            )
        }
        // X-ring tick — thin cross at dead centre.
        val tick = r * 0.03f
        drawLine(Color(0x80333333), Offset(cx - tick, cy), Offset(cx + tick, cy), 0.8f)
        drawLine(Color(0x80333333), Offset(cx, cy - tick), Offset(cx, cy + tick), 0.8f)
    }
}

/**
 * Draw the already-placed arrows. On a single face the stored plotX/plotY map
 * straight to the face square; on a multi-spot card each arrow is bucketed to
 * its nearest spot and drawn local to that spot's centre. Mirrors iOS, which
 * renders arrows through the same per-spot mapping.
 */
private fun DrawScope.drawExistingArrows(
    arrows: List<ArrowPlot>,
    arrowDiameterMm: Double,
    geometry: TargetGeometry,
    multiSpot: MultiSpotGeometry?,
) {
    val radiusPx = minOf(size.width, size.height) / 2f
    val dotRadiusPx = (arrowDiameterMm / geometry.mmPerNormUnit * radiusPx)
        .toFloat()
        .coerceAtLeast(8f) / 2f

    if (multiSpot != null) {
        multiSpot.assignArrows(arrows).forEach { perSpot ->
            val center = multiSpot.centers[perSpot.spotIndex]
            val spotR = multiSpot.radiusNorm.toFloat() * minOf(size.width, size.height)
            val cx = (center.x.toFloat() * size.width + perSpot.localX.toFloat() * spotR)
            val cy = (center.y.toFloat() * size.height + perSpot.localY.toFloat() * spotR)
            val number = arrows.indexOf(perSpot.arrow) + 1
            drawArrowDot(Offset(cx, cy), dotRadiusPx, perSpot.arrow.ring, number)
        }
    } else {
        val center = Offset(size.width / 2f, size.height / 2f)
        arrows.forEachIndexed { index, arrow ->
            val px = arrow.plotX ?: return@forEachIndexed
            val py = arrow.plotY ?: return@forEachIndexed
            val cx = (center.x + px * radiusPx).toFloat()
            val cy = (center.y + py * radiusPx).toFloat()
            drawArrowDot(Offset(cx, cy), dotRadiusPx, arrow.ring, index + 1)
        }
    }
}

/**
 * Draw one plotted arrow — mirrors iOS `ArrowDot` (TargetPlotView.swift:732):
 * a [ringDotColor] fill with a subtle drop shadow, an outline only on the
 * white/black rings 1–4, and the arrow's index number inside the dot when it
 * is large enough to fit.
 */
internal fun DrawScope.drawArrowDot(
    center: Offset,
    radius: Float,
    ring: Int,
    number: Int,
) {
    // Drop shadow — black 35%, radius ~2, y +1 (iOS ArrowDot .shadow).
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius,
        center = Offset(center.x, center.y + 1f),
    )
    // Fill.
    drawCircle(color = ringDotColor(ring), radius = radius, center = center)
    // Outline — only rings 1–4 (dark on white 1–2, light on black 3–4); iOS
    // leaves 5–X with no outline.
    ringDotOutline(ring)?.let { outline ->
        drawCircle(
            color = outline,
            radius = radius,
            center = center,
            style = Stroke(width = 1f),
        )
    }
    // Index number — bold rounded font, only when the dot is large enough.
    if (radius * 2f >= 12f) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = ringDotTextColor(ring).toArgb()
                textSize = (radius * 0.9f).coerceAtLeast(7f)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                isAntiAlias = true
            }
            // Vertically centre the baseline.
            val baseline = center.y - (paint.descent() + paint.ascent()) / 2f
            canvas.nativeCanvas.drawText(number.toString(), center.x, baseline, paint)
        }
    }
}

/** Arrow-dot fill colour — matches iOS `ArrowDot.dotColor` (appTarget* tokens). */
private fun ringDotColor(ring: Int): Color = when (ring) {
    11 -> AppTgtYellow   // X — appTargetGold == appTgtYellow on iOS
    10, 9 -> AppTgtYellow
    8, 7 -> AppTgtRed
    6, 5 -> AppTgtBlue
    4, 3 -> AppTgtBlack
    2, 1 -> AppTgtWhite
    else -> Color.Gray
}

/** Dot outline — only the white/black rings 1–4; null = no outline (iOS .clear). */
private fun ringDotOutline(ring: Int): Color? = when (ring) {
    1, 2 -> AppTgtBlack.copy(alpha = 0.85f)
    3, 4 -> AppTgtWhite.copy(alpha = 0.9f)
    else -> null
}

/** Index-number colour — dark ink on light fills, white on dark fills (iOS). */
private fun ringDotTextColor(ring: Int): Color = when (ring) {
    9, 10, 11 -> AppTgtBlack    // dark ink on yellow / gold
    1, 2 -> AppTgtBlack         // dark ink on white
    else -> Color.White         // white on black / blue / red
}

private val WHITE = Color(0xFFFFFDF7)
private val BLACK = Color(0xFF1B1B1B)
private val YELLOW = Color(0xFFFFEE33)
private val RED = Color(0xFFE04738)
private val BLUE = Color(0xFF00BAE3)

/** Accent color the task brief calls out. Exposed so screens can re-use it if needed. */
@Suppress("unused")
internal val SESSION_ACCENT = Color(0xFFD14B2E)
