package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.AppTgtBlack
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.MultiSpotGeometry
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout

/**
 * Build a [PenLensSnapshot] for a touch on the target Canvas.
 *
 * [touch] is in the target Canvas's local pixel space; [faceOriginInWindow] is
 * the Canvas's top-left in the screen-root coordinate space (obtained via
 * `Modifier.onGloballyPositioned { positionInWindow() }`). Both the touch and
 * the face origin are translated into root coords so the [PenLensOverlay] —
 * hosted at the screen root — can place the lens up into the header space
 * above the target. Mirrors iOS, where the lens lives at the screen root in
 * global screen coords (TargetPlotView.swift:800-960).
 *
 * Shared between [TargetPlot] and [ActiveSessionScreen.TargetInteractiveFace]
 * so the two plotting surfaces stay in lockstep.
 */
internal fun buildPenLensSnapshot(
    touch: Offset,
    faceOriginInWindow: Offset,
    faceSizePx: Float,
    arrows: List<ArrowPlot>,
    arrowDiameterMm: Double,
    geometry: TargetGeometry,
    faceType: TargetFaceType,
    targetLayout: TargetLayout = TargetLayout.SINGLE,
    // §B3 — distance threads into the lens so the magnified face matches
    // the underlying plotter's geometry (a 50/70m sixRing session sees the
    // 7-zone face zoomed, not the Vegas 6-zone). Null defaults to Vegas.
    distance: ShootingDistance? = null,
): PenLensSnapshot {
    val radiusPx = faceSizePx / 2f
    val multiSpot = MultiSpotGeometry.preset(targetLayout)
    val previewRing: Int? = if (multiSpot != null) {
        // Multi-spot: ring is local to the nearest spot.
        val normX = (touch.x - radiusPx) / radiusPx
        val normY = (touch.y - radiusPx) / radiusPx
        val pointNorm = MultiSpotGeometry.NormPoint(
            x = 0.5 + normX / 2.0,
            y = 0.5 + normY / 2.0,
        )
        val near = multiSpot.nearestSpotLocalRadius(pointNorm)
        val arrowFrac = arrowDiameterMm / multiSpot.spotDiameterMm
        multiSpot.ring(near.local, arrowFrac)
    } else {
        val plotX = (touch.x - radiusPx).toDouble() / radiusPx
        val plotY = (touch.y - radiusPx).toDouble() / radiusPx
        val dotNormRadius = (arrowDiameterMm / 2.0) / geometry.mmPerNormUnit
        geometry.classifyWithDotRadius(plotX, plotY, dotNormRadius).ring
    }
    val arrowDotPx = ((arrowDiameterMm / geometry.mmPerNormUnit) * radiusPx).toFloat()
    return PenLensSnapshot(
        touchPx = Offset(faceOriginInWindow.x + touch.x, faceOriginInWindow.y + touch.y),
        faceOriginPx = faceOriginInWindow,
        faceSizePx = faceSizePx,
        arrowDotSizePx = arrowDotPx,
        faceType = faceType,
        targetLayout = targetLayout,
        arrows = arrows,
        previewRing = previewRing,
        distance = distance,
    )
}

/**
 * Snapshot of a live touch on the target, in **screen-root pixel
 * coordinates**. Emitted by the plotting surface on every drag tick and
 * consumed by [PenLensOverlay], which is hosted at the screen root so the
 * lens can extend up into the header space above the target.
 *
 * Mirrors iOS `PenLensSnapshot` (TargetPlotView.swift:780) — iOS stores
 * global screen coords; Android stores root coords (positionInWindow), which
 * is the equivalent: the lens overlay fills the same root and has real room
 * above the target.
 */
data class PenLensSnapshot(
    /** Touch position in the screen-root coord space, px. */
    val touchPx: Offset,
    /** Target face top-left in the screen-root coord space, px. */
    val faceOriginPx: Offset,
    /** Face square edge length, px (face is a circle inscribed in this). */
    val faceSizePx: Float,
    /** Arrow shaft footprint diameter at 1× scale, px. */
    val arrowDotSizePx: Float,
    val faceType: TargetFaceType,
    /** Multi-spot layout — drives the magnified face render. */
    val targetLayout: TargetLayout = TargetLayout.SINGLE,
    val arrows: List<ArrowPlot>,
    /** Ring at the live touch point (1–11 or null for miss). */
    val previewRing: Int?,
    /**
     * §B3 — session distance forwards through to the magnified face renderer
     * so a 50/70m sixRing session sees the 7-zone face zoomed rather than
     * the Vegas 6-zone. Null keeps the Vegas default.
     */
    val distance: ShootingDistance? = null,
)

/**
 * Floating Pen-magnifier lens. Mirrors iOS `PenLensView` (TargetPlotView.swift:800):
 *
 *   - Lens diameter = 0.75 × face diameter
 *   - 2.5× zoom inside the lens
 *   - Lens centre sits ~55pt above the touch (above), or below if it would
 *     clip the top edge
 *   - Live ring stamp above the lens shows the current preview ring
 *   - Footprint ring at the lens centre marks where the arrow will commit
 *
 * Renders nothing when [snapshot] is null. **Host this at the screen root**
 * (a container that has space above the target) so the prefer-above
 * placement has real room — hosting it inside the target-sized box would
 * force every above-the-finger lens to clip + flip below.
 */
@Composable
fun PenLensOverlay(snapshot: PenLensSnapshot?, modifier: Modifier = Modifier) {
    // The plotting surface records touch/face coords in window space; the
    // overlay positions its children relative to its own top-left. Capture
    // this overlay's window origin so window-space coords can be rebased into
    // overlay-local space — otherwise a status-bar / header inset offsets the
    // lens from the finger.
    var overlayOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val positioned = modifier.then(
        Modifier.onGloballyPositioned { overlayOriginInWindow = it.positionInWindow() },
    )
    if (snapshot == null) {
        // Still occupy the slot so onGloballyPositioned keeps the origin
        // fresh for the next drag.
        androidx.compose.foundation.layout.Box(positioned.fillMaxSize())
        return
    }
    val local = snapshot.rebased(overlayOriginInWindow)
    PenLensOverlayContent(snapshot = local, modifier = positioned)
}

/** Snapshot rebased into the overlay's local coord space. */
private fun PenLensSnapshot.rebased(overlayOrigin: Offset): PenLensSnapshot = copy(
    touchPx = touchPx - overlayOrigin,
    faceOriginPx = faceOriginPx - overlayOrigin,
)

@Composable
private fun PenLensOverlayContent(snapshot: PenLensSnapshot, modifier: Modifier) {
    val density = LocalDensity.current
    val lensSizePx = maxOf(with(density) { 120.dp.toPx() }, snapshot.faceSizePx * LENS_SIZE_RATIO)
    val lensRadius = lensSizePx / 2f
    val zoomedFaceSize = snapshot.faceSizePx * LENS_ZOOM
    val thumbHalf = with(density) { THUMB_HALF_DP.toPx() }
    val centerBuffer = with(density) { LENS_CENTER_BUFFER_DP.toPx() }
    val edgeBuffer = with(density) { EDGE_BUFFER_DP.toPx() }
    val stampOffset = with(density) { STAMP_OFFSET_DP.toPx() }

    // Vertical placement — prefer above the finger; flip below only if the
    // top edge of the lens would clip the root container's top. Hosted at
    // the screen root, "above" has real room (the header), so most plots
    // keep the lens above the thumb — matching iOS.
    val touchClearance = thumbHalf + centerBuffer
    val preferredAboveY = snapshot.touchPx.y - touchClearance
    val lensTopIfAbove = preferredAboveY - lensRadius
    val placeBelow = lensTopIfAbove < edgeBuffer
    val lensCenterY = if (placeBelow) snapshot.touchPx.y + touchClearance else preferredAboveY

    // Horizontal placement clamps inside the root container's bounds.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val clampedX = snapshot.touchPx.x.coerceIn(
            lensRadius + edgeBuffer,
            (boxWidthPx - lensRadius - edgeBuffer).coerceAtLeast(lensRadius + edgeBuffer),
        )
        val origin = Offset(clampedX - lensRadius, lensCenterY - lensRadius)
        val lensSizeDp = with(density) { lensSizePx.toDp() }

        // Score stamp — floats above the lens centre. Always rendered: the Y
        // is clamped (not the stamp hidden) so the live ring readout stays
        // visible even when above-lens placement would clip the root top.
        // Mirrors iOS TargetPlotView.swift:950-952.
        val stampWidthPx = with(density) { 40.dp.toPx() }
        val stampTopY = (origin.y - stampOffset).coerceAtLeast(stampOffset / 2f)
        ScoreStamp(
            ring = snapshot.previewRing,
            modifier = Modifier.offset(
                x = with(density) { (origin.x + lensRadius - stampWidthPx / 2f).toDp() },
                y = with(density) { stampTopY.toDp() },
            ),
        )

        // Lens body — circular clip wraps the magnified face + arrows.
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { origin.x.toDp() },
                    y = with(density) { origin.y.toDp() },
                )
                .size(lensSizeDp)
                .clip(CircleShape)
                .background(AppPaper)
                .border(1.dp, AppLine, CircleShape),
        ) {
            LensContent(
                snapshot = snapshot,
                lensRadius = lensRadius,
                zoomedFaceSize = zoomedFaceSize,
                density = density,
            )
        }
    }
}

@Composable
private fun LensContent(
    snapshot: PenLensSnapshot,
    lensRadius: Float,
    zoomedFaceSize: Float,
    density: Density,
) {
    // Translate the zoomed face so the touch (in face-local coords) lands at
    // the lens centre. Both touchPx and faceOriginPx are in root coords.
    val touchFaceX = snapshot.touchPx.x - snapshot.faceOriginPx.x
    val touchFaceY = snapshot.touchPx.y - snapshot.faceOriginPx.y
    val contentOffsetX = lensRadius - touchFaceX * LENS_ZOOM
    val contentOffsetY = lensRadius - touchFaceY * LENS_ZOOM

    val footprintFloorPx = with(density) { 8.dp.toPx() }
    val footprintSize = maxOf(snapshot.arrowDotSizePx * LENS_ZOOM, footprintFloorPx)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Re-draw the target face at lensZoom×, offset to keep touch at centre.
        translate(left = contentOffsetX, top = contentOffsetY) {
            drawLensFace(snapshot = snapshot, sizePx = zoomedFaceSize)
        }

        // Existing arrows — same coord system as the face, scaled by lensZoom.
        translate(left = contentOffsetX, top = contentOffsetY) {
            drawArrowsAtSize(
                snapshot = snapshot,
                facePx = zoomedFaceSize,
                arrowDotPx = snapshot.arrowDotSizePx * LENS_ZOOM,
            )
        }

        // Footprint ring at the lens centre marks where the commit will land.
        drawCircle(
            color = AppPondDk.copy(alpha = 0.7f),
            radius = footprintSize / 2f,
            center = Offset(lensRadius, lensRadius),
            style = Stroke(width = 1.5f * density.density),
        )
    }
}

@Composable
private fun ScoreStamp(ring: Int?, modifier: Modifier = Modifier) {
    val (label, tint) = when (ring) {
        null -> "M" to AppLine
        11 -> "X" to AppPondDk
        else -> ring.toString() to AppTgtBlack
    }
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 30.dp)
            .background(AppPaper.copy(alpha = 0.92f))
            .border(1.dp, AppLine),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium).copy(color = tint),
        )
    }
}

private const val LENS_SIZE_RATIO = 0.75f
private const val LENS_ZOOM = 2.5f
private val STAMP_OFFSET_DP = 46.dp
private val THUMB_HALF_DP = 30.dp
private val LENS_CENTER_BUFFER_DP = 25.dp
private val EDGE_BUFFER_DP = 4.dp

// --- Drawing helpers used by the lens ---

/**
 * Draw the magnified target face inside the lens — a single WA face, or the
 * 3-spot Vegas card when the session uses a multi-spot layout.
 */
private fun DrawScope.drawLensFace(snapshot: PenLensSnapshot, sizePx: Float) {
    val multiSpot = MultiSpotGeometry.preset(snapshot.targetLayout)
    if (multiSpot != null) {
        drawMultiSpotCard(multiSpot, Size(sizePx, sizePx))
    } else {
        // §B3 — route the lens face through the same distance-aware
        // geometry the underlying plotter uses, so a 50/70m sixRing
        // session sees a 7-zone face zoomed (split-blue band + outer ring 5).
        drawSingleLensFace(TargetGeometry.forFace(snapshot.faceType, snapshot.distance), sizePx)
    }
}

/**
 * Draw one WA face filling [sizePx]. Mirrors [drawTargetFace] (TargetPlot.kt):
 * colored bands painted outer→inner, then ring dividers / outer edge / X tick
 * over the top. iOS gets these for free because its lens reuses
 * `TargetFaceCanvas`; Android renders the lens face manually, so without the
 * explicit divider calls the lens reads as a solid color disc under the
 * touch on wider faces (e.g. 50m/70m 80cm).
 */
private fun DrawScope.drawSingleLensFace(geometry: TargetGeometry, sizePx: Float) {
    val center = Offset(sizePx / 2f, sizePx / 2f)
    val radius = sizePx / 2f
    // Paper background fills the outer-most ring boundary.
    drawCircle(color = Color(0xFFF6F8F3), radius = radius, center = center)
    // Paint outer → inner; skip thresholds[0] (X centre cross, no fill).
    for (i in geometry.thresholds.size - 1 downTo 1) {
        val ringEdge = geometry.thresholds[i]
        val ringNumber = geometry.outerRingValue + (geometry.thresholds.size - 1 - i)
        drawCircle(
            color = TargetGeometry.ringColor(ringNumber),
            radius = (ringEdge * radius).toFloat(),
            center = center,
        )
    }
    drawRingDividers(geometry.thresholds.toList(), radius, center)
    drawOuterEdge(radius, center)
    drawXTick(geometry.thresholds[0], radius, center)
}

/**
 * Render previously-placed arrows inside the lens. Mirrors the on-target
 * [drawArrowDot] look (shadow, ring-1–4 outline, index number) so the
 * magnified arrows match the arrows under the finger.
 */
private fun DrawScope.drawArrowsAtSize(
    snapshot: PenLensSnapshot,
    facePx: Float,
    arrowDotPx: Float,
) {
    val radius = facePx / 2f
    // 4 dp radius floor (mirrors the 8 dp diameter floor on the underlying
    // face renderer). The previous 4 px floor collapsed to ~1.3 dp on a 3x
    // device, making dots inside the lens nearly invisible on wider faces.
    val dotRadius = (arrowDotPx / 2f).coerceAtLeast(4.dp.toPx())
    val multiSpot = MultiSpotGeometry.preset(snapshot.targetLayout)
    if (multiSpot != null) {
        // Multi-spot: place each arrow on its bucketed spot.
        multiSpot.assignArrows(snapshot.arrows).forEach { perSpot ->
            val center = multiSpot.centers[perSpot.spotIndex]
            val spotR = multiSpot.radiusNorm * facePx
            val cx = (center.x * facePx + perSpot.localX * spotR).toFloat()
            val cy = (center.y * facePx + perSpot.localY * spotR).toFloat()
            val number = snapshot.arrows.indexOf(perSpot.arrow) + 1
            drawArrowDot(Offset(cx, cy), dotRadius, perSpot.arrow.ring, number)
        }
    } else {
        val center = Offset(facePx / 2f, facePx / 2f)
        snapshot.arrows.forEachIndexed { idx, a ->
            val x = a.plotX ?: return@forEachIndexed
            val y = a.plotY ?: return@forEachIndexed
            val cx = (center.x + x * radius).toFloat()
            val cy = (center.y + y * radius).toFloat()
            drawArrowDot(Offset(cx, cy), dotRadius, a.ring, idx + 1)
        }
    }
}
