package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetFaceType

/**
 * Snapshot of a live touch on the target, in pixel coordinates relative to
 * the [TargetPlot]'s top-left. Emitted by [TargetPlot] on every drag tick
 * and consumed by [PenLensOverlay] which renders the magnifier on a sibling
 * layer (so the lens can extend past the target's frame).
 *
 * Mirrors iOS `PenLensSnapshot` (TargetPlotView.swift:780). The iOS
 * implementation stores screen-global coords; on Android we work in the
 * parent Box's local coord space, which has the same effect because the
 * lens overlay sits at the same z-level as the target.
 */
data class PenLensSnapshot(
    /** Touch position in the parent Box's coord space, px. */
    val touchPx: Offset,
    /** Target face top-left in the parent Box's coord space, px. */
    val faceOriginPx: Offset,
    /** Face square edge length, px (face is a circle inscribed in this). */
    val faceSizePx: Float,
    /** Arrow shaft footprint diameter at 1× scale, px. */
    val arrowDotSizePx: Float,
    val faceType: TargetFaceType,
    val arrows: List<ArrowPlot>,
    /** Ring at the live touch point (1–11 or null for miss). */
    val previewRing: Int?,
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
 * Renders nothing when [snapshot] is null. Place this at the same z-level
 * as [TargetPlot] inside a Box so the lens can extend outside the target's
 * bounds.
 */
@Composable
fun PenLensOverlay(snapshot: PenLensSnapshot?, modifier: Modifier = Modifier) {
    if (snapshot == null) return
    val density = LocalDensity.current
    val lensSizePx = maxOf(with(density) { 120.dp.toPx() }, snapshot.faceSizePx * LENS_SIZE_RATIO)
    val lensRadius = lensSizePx / 2f
    val zoomedFaceSize = snapshot.faceSizePx * LENS_ZOOM
    val thumbHalf = with(density) { THUMB_HALF_DP.toPx() }
    val centerBuffer = with(density) { LENS_CENTER_BUFFER_DP.toPx() }
    val edgeBuffer = with(density) { EDGE_BUFFER_DP.toPx() }
    val stampOffset = with(density) { STAMP_OFFSET_DP.toPx() }

    // Vertical placement — prefer above the finger; flip below if the top
    // edge of the lens would clip the parent container's top.
    val touchClearance = thumbHalf + centerBuffer
    val preferredAboveY = snapshot.touchPx.y - touchClearance
    val lensTopIfAbove = preferredAboveY - lensRadius
    val placeBelow = lensTopIfAbove < edgeBuffer
    val lensCenterY = if (placeBelow) snapshot.touchPx.y + touchClearance else preferredAboveY

    // Horizontal placement is the touch x clamped so the lens stays inside
    // the parent Box. Caller passes a Modifier that fills the available
    // space; the inner offset is in pixels.
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Compute the lens top-left so the centre lands at (touch.x, lensCenterY).
        // Resolve at render time so the parent Box's max width is known.
        val lensTopLeft: (Float) -> Offset = { boxWidth ->
            val clampedX = snapshot.touchPx.x.coerceIn(
                lensRadius + edgeBuffer,
                (boxWidth - lensRadius - edgeBuffer).coerceAtLeast(lensRadius + edgeBuffer),
            )
            Offset(clampedX - lensRadius, lensCenterY - lensRadius)
        }

        BoxWithMeasuredWidth { measuredWidth ->
            val origin = lensTopLeft(measuredWidth)
            val lensSizeDp = with(density) { lensSizePx.toDp() }

            // Score stamp — floats above the lens center, shows preview ring.
            val stampTopY = origin.y - stampOffset
            ScoreStamp(
                ring = snapshot.previewRing,
                modifier = Modifier
                    .offset(
                        x = with(density) { (origin.x + lensRadius - stampHalfWidthPx() / 2f).toDp() },
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
}

/** Measures the parent box once and feeds the width back into [content]. */
@Composable
private fun BoxWithMeasuredWidth(content: @Composable (widthPx: Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        content(widthPx)
    }
}

@Composable
private fun LensContent(
    snapshot: PenLensSnapshot,
    lensRadius: Float,
    zoomedFaceSize: Float,
    density: Density,
) {
    // Translate the zoomed face so the touch (in face-local coords) lands
    // at the lens center. faceOriginPx is the parent-box-local origin of
    // the target face square; the lens is also positioned in that space.
    val touchFaceX = snapshot.touchPx.x - snapshot.faceOriginPx.x
    val touchFaceY = snapshot.touchPx.y - snapshot.faceOriginPx.y
    val contentOffsetX = lensRadius - touchFaceX * LENS_ZOOM
    val contentOffsetY = lensRadius - touchFaceY * LENS_ZOOM

    // Footprint dot size with display floor (matches iOS — scoring math
    // still uses the unclamped value, but the rendered footprint floors
    // at 8dp so thin shafts don't disappear in the lens).
    val footprintFloorPx = with(density) { 8.dp.toPx() }
    val footprintSize = maxOf(snapshot.arrowDotSizePx * LENS_ZOOM, footprintFloorPx)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Re-draw the target face at lensZoom×, offset to keep touch at center.
        translate(left = contentOffsetX, top = contentOffsetY) {
            drawSize { _ ->
                drawTargetFaceAtSize(
                    faceType = snapshot.faceType,
                    sizePx = zoomedFaceSize,
                )
            }
        }

        // Existing arrows — same coord system as the face, scaled by lensZoom.
        translate(left = contentOffsetX, top = contentOffsetY) {
            drawArrowsAtSize(
                arrows = snapshot.arrows,
                facePx = zoomedFaceSize,
                arrowDotPx = snapshot.arrowDotSizePx * LENS_ZOOM,
            )
        }

        // Footprint ring at the lens center marks where the commit will land.
        drawCircle(
            color = AppPondDk.copy(alpha = 0.7f),
            radius = footprintSize / 2f,
            center = Offset(lensRadius, lensRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density.density),
        )
    }
}

@Composable
private fun ScoreStamp(ring: Int?, modifier: Modifier = Modifier) {
    val (label, tint) = when (ring) {
        null -> "M" to AppLine
        11 -> "X" to AppPondDk
        else -> ring.toString() to AppInk
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

private fun stampHalfWidthPx(): Float = 0f // unused — kept for layout symmetry

private const val LENS_SIZE_RATIO = 0.75f
private const val LENS_ZOOM = 2.5f
private val STAMP_OFFSET_DP = 46.dp
private val THUMB_HALF_DP = 30.dp
private val LENS_CENTER_BUFFER_DP = 25.dp
private val EDGE_BUFFER_DP = 4.dp

// --- Drawing helpers used by the lens ---

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSize(
    block: androidx.compose.ui.graphics.drawscope.DrawScope.(Size) -> Unit,
) {
    block(size)
}

/**
 * Draw the target face at an explicit size, ignoring the canvas's natural
 * size. Used inside the lens where we paint a zoomed copy at faceSize × 2.5.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTargetFaceAtSize(
    faceType: TargetFaceType,
    sizePx: Float,
) {
    // Delegate to TargetPlot's existing drawTargetFace by clipping a scaled
    // sub-region. The simplest path is to inline the ring fills here; we
    // share the geometry tables from TargetGeometry.
    val center = Offset(sizePx / 2f, sizePx / 2f)
    val radius = sizePx / 2f
    val geom = TargetGeometry.forFace(faceType)
    drawCircle(color = androidx.compose.ui.graphics.Color(0xFFF6F8F3), radius = radius, center = center)
    // Re-paint as concentric rings outermost → innermost. We don't bother
    // with dividers + X tick inside the lens — magnified rings are already
    // visually clear without the hairlines.
    when (faceType) {
        TargetFaceType.SIX_RING -> {
            val g = TargetGeometry.SixRing
            drawCircle(color = Color(0xFF4EA8C9), radius = (g.R7_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFFD94B3B), radius = (g.R8_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFFF0D04A), radius = (g.R10_RADIUS * radius).toFloat(), center = center)
        }
        TargetFaceType.TEN_RING -> {
            val g = TargetGeometry.TenRing
            drawCircle(color = Color(0xFFF6F8F3), radius = (g.R2_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFF1F2A26), radius = (g.R4_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFF4EA8C9), radius = (g.R6_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFFD94B3B), radius = (g.R8_RADIUS * radius).toFloat(), center = center)
            drawCircle(color = Color(0xFFF0D04A), radius = (g.R10_RADIUS * radius).toFloat(), center = center)
        }
    }
}

/**
 * Render previously-placed arrows inside the lens. Positions are normalized
 * (plotX/plotY in -1..1) and scaled to the lens's [facePx] coord space.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowsAtSize(
    arrows: List<ArrowPlot>,
    facePx: Float,
    arrowDotPx: Float,
) {
    val center = Offset(facePx / 2f, facePx / 2f)
    val radius = facePx / 2f
    for (a in arrows) {
        val x = a.plotX ?: continue
        val y = a.plotY ?: continue
        val px = (center.x + x * radius).toFloat()
        val py = (center.y + y * radius).toFloat()
        drawCircle(
            color = AppInk,
            radius = (arrowDotPx / 2f).coerceAtLeast(2f),
            center = Offset(px, py),
        )
    }
}
