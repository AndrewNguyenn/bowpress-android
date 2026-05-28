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
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlue
import com.andrewnguyen.bowpress.core.designsystem.AppTgtRed
import com.andrewnguyen.bowpress.core.designsystem.AppTgtWhite
import com.andrewnguyen.bowpress.core.designsystem.AppTgtYellow
import com.andrewnguyen.bowpress.core.designsystem.bp.drawVegasSixRingSpot
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.MultiSpotGeometry
import com.andrewnguyen.bowpress.core.model.ShootingDistance
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
    // §B3 — session distance picks the sixRing visual + scoring variant.
    // 50m / 70m → Outdoor80 (7-zone, scores into ring 5); else Vegas
    // (6-zone, capped at ring 6). null → Vegas default.
    distance: ShootingDistance? = null,
    onLensSnapshotChanged: (PenLensSnapshot?) -> Unit = {},
) {
    val geometry = TargetGeometry.forFace(faceType, distance)
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
                .pointerInput(isEnabled, faceType, targetLayout, arrowDiameterMm, distance) {
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
                                    distance = distance,
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
                                    distance = distance,
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
                drawTargetFace(geometry)
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

/**
 * Geometry-driven face renderer. Paints the outer-most band first, then each
 * inner band on top, so only the annular slice of each colour is visible.
 * Colours come from [TargetGeometry.ringColor], keyed on the ring number
 * derived from the geometry's [outerRingValue]. The same code path correctly
 * paints the Vegas 6-zone (rings 6..X), the 80cm 7-zone (rings 5..X), and
 * the WA 10-zone full face. Mirrors iOS `TargetFaceCanvas.body` after
 * commit e162ac5.
 */
private fun DrawScope.drawTargetFace(geometry: TargetGeometry) {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Paint a paper-coloured disc at the FULL canvas radius first so any
    // gap between the outer-most scoring band and the canvas edge — most
    // notably Vegas, whose ring-6 outer edge is at ~0.808 rather than 1.0
    // — reads as a true white margin instead of leaking the parent
    // background through. Mirrors PenLensOverlay's lens-face renderer.
    drawCircle(color = WHITE, radius = radiusPx, center = center)
    // Walk outer → inner skipping the X ring at index 0. `thresholds` is
    // innermost-first (X .. outer), so walking in reverse paints outer
    // first — exactly what the over-paint stack wants.
    for (i in geometry.thresholds.size - 1 downTo 1) {
        val ringEdge = geometry.thresholds[i]
        // ringEdge corresponds to the outer edge of the (innermostNumericRing - (size-1-i))'th ring.
        // Equivalently: the band at thresholds[i] paints the colour of ring (outerRingValue + (size-1-i)).
        // (innermost numeric ring lives at thresholds[1]; outermost ring lives at thresholds[size-1].)
        val ringNumber = geometry.outerRingValue + (geometry.thresholds.size - 1 - i)
        drawCircle(
            color = TargetGeometry.ringColor(ringNumber),
            radius = (ringEdge * radiusPx).toFloat(),
            center = center,
        )
    }

    drawRingDividers(geometry.thresholds.toList(), radiusPx, center)
    drawOuterEdge(radiusPx, center)
    drawXTick(geometry.thresholds[0], radiusPx, center)
}

internal fun DrawScope.drawRingDividers(
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

internal fun DrawScope.drawOuterEdge(radiusPx: Float, center: Offset) {
    drawCircle(
        color = Color(0xFF333333),
        radius = radiusPx,
        center = center,
        style = Stroke(width = 2f),
    )
}

internal fun DrawScope.drawXTick(xRadius: Double, radiusPx: Float, center: Offset) {
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
 * Draw a 40cm Vegas 3-spot card — three concentric 6-ring spots via the
 * canonical [drawVegasSixRingSpot] helper. Mirrors iOS `MultiSpotFaceCanvas`.
 * Shared with the Pen lens so a multi-spot session shows the actual card
 * shape under the magnifier too.
 */
internal fun DrawScope.drawMultiSpotCard(geometry: MultiSpotGeometry, canvas: Size) {
    val minEdge = minOf(canvas.width, canvas.height)
    val r = geometry.radiusNorm.toFloat() * minEdge
    for (center in geometry.centers) {
        val cx = center.x.toFloat() * canvas.width
        val cy = center.y.toFloat() * canvas.height
        drawVegasSixRingSpot(Offset(cx, cy), r)
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
    // 8 dp diameter floor mirrors iOS `displayedDotSize = max(arrowDotSize, 8)`
    // (pt). On the SixRingOutdoor 80cm face (mmPerNormUnit=400) the raw
    // geometric diameter is ~5.6 px on a 3x device — well under the floor —
    // so a px-based floor of 8 collapses to ~2.7 dp and the dot disappears.
    val dotRadiusPx = (arrowDiameterMm / geometry.mmPerNormUnit * radiusPx)
        .toFloat()
        .coerceAtLeast(8.dp.toPx()) / 2f

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
    // Gate + text-size floor are in dp, not raw px (iOS uses pt; same idea).
    if (radius * 2f >= 12.dp.toPx()) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = ringDotTextColor(ring).toArgb()
                textSize = (radius * 0.9f).coerceAtLeast(7.dp.toPx())
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

/** Arrow-dot fill — solid ink on every ring. Diverges from iOS (which colour-
 *  matches the dot to its ring) because on Android the matched-colour dot was
 *  routinely invisible against the band it landed on, especially on wider
 *  faces. The analytics/history view (`BPPlottedTarget.drawArrowDot`) already
 *  uses this same black fill. */
@Suppress("UNUSED_PARAMETER")
private fun ringDotColor(ring: Int): Color = AppTgtBlack

/** Light outline only on the black scoring bands (rings 3, 4) so the black
 *  dot doesn't disappear there. Everywhere else the black-on-coloured contrast
 *  is enough; no outline. */
private fun ringDotOutline(ring: Int): Color? = when (ring) {
    3, 4 -> AppTgtWhite.copy(alpha = 0.9f)
    else -> null
}

/** Index-number colour — white on the black dot. */
@Suppress("UNUSED_PARAMETER")
private fun ringDotTextColor(ring: Int): Color = Color.White

private val WHITE = Color(0xFFFFFDF7)

/** Accent color the task brief calls out. Exposed so screens can re-use it if needed. */
@Suppress("unused")
internal val SESSION_ACCENT = Color(0xFFD14B2E)
