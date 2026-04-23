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
import com.andrewnguyen.bowpress.core.model.Zone

/**
 * Canvas that draws the target face and captures drag-to-plot gestures. Mirrors
 * bowpress-ios/Sources/BowPress/Session/TargetPlotView.swift — the exact line refs for
 * the scoring logic are on [TargetGeometry]. UI-side references:
 *
 *   - Ring rendering: iOS lines 83–86 (target face is a pre-rendered image on iOS;
 *     Android draws vector circles because we don't ship `target_face.png` yet).
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
) {
    val density = LocalDensity.current
    val touchOffsetPx = with(density) { TOUCH_OFFSET_DP.toPx() }
    var dragPreview by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(TARGET_PLOT_TEST_TAG)
            .pointerInput(isEnabled, touchOffsetPx) {
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
                            (arrowDiameterMm / 2.0) / TargetGeometry.MM_PER_NORM_UNIT
                        val result = TargetGeometry.classifyWithDotRadius(
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
        drawTargetFace()
        drawExistingArrows(arrows = arrows, arrowDiameterMm = arrowDiameterMm)
        drawDragPreview(preview = dragPreview, arrowDiameterMm = arrowDiameterMm)
    }
}

// ---- Drawing helpers ----

private fun DrawScope.drawTargetFace() {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Target face colors approximating the WA 10-ring face. Draw outermost → innermost
    // so inner rings paint on top.
    val rings: List<Pair<Float, Color>> = listOf(
        1.0f to WHITE,
        TargetGeometry.R7_RADIUS.toFloat() to BLUE,
        TargetGeometry.R8_RADIUS.toFloat() to RED,
        TargetGeometry.R10_RADIUS.toFloat() to YELLOW,
    )
    for ((normRadius, color) in rings) {
        drawCircle(color = color, radius = normRadius * radiusPx, center = center)
    }

    // Ring lines at every threshold so the archer can see the scoring boundaries.
    val ringLineColor = Color(0xFF333333)
    val thresholds = listOf(
        TargetGeometry.X_RADIUS,
        TargetGeometry.R10_RADIUS,
        TargetGeometry.R9_RADIUS,
        TargetGeometry.R8_RADIUS,
        TargetGeometry.R7_RADIUS,
        TargetGeometry.R6_RADIUS,
    )
    for (t in thresholds) {
        drawCircle(
            color = ringLineColor,
            radius = (t * radiusPx).toFloat(),
            center = center,
            style = Stroke(width = 1.5f),
        )
    }
    drawCircle(
        color = ringLineColor,
        radius = radiusPx,
        center = center,
        style = Stroke(width = 2f),
    )
}

private fun DrawScope.drawExistingArrows(
    arrows: List<ArrowPlot>,
    arrowDiameterMm: Double,
) {
    val radiusPx = minOf(size.width, size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val dotDiameterPx = (arrowDiameterMm / TargetGeometry.MM_PER_NORM_UNIT * radiusPx)
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

private fun DrawScope.drawDragPreview(preview: Offset?, arrowDiameterMm: Double) {
    if (preview == null) return
    val radiusPx = minOf(size.width, size.height) / 2f
    val dotDiameterPx = (arrowDiameterMm / TargetGeometry.MM_PER_NORM_UNIT * radiusPx)
        .toFloat()
        .coerceAtLeast(8f)
    drawCircle(
        color = Color.White,
        radius = dotDiameterPx / 2f,
        center = preview,
        style = Stroke(width = 1.5f),
    )
}

private fun dotColor(ring: Int): Color = when (ring) {
    11 -> GOLD
    10, 9 -> YELLOW
    8, 7 -> RED
    6 -> BLUE
    else -> Color.Gray
}

private val WHITE = Color(0xFFFFFDF7)
private val GOLD = Color(0xFFFFD900)
private val YELLOW = Color(0xFFFFEE33)
private val RED = Color(0xFFE04738)
private val BLUE = Color(0xFF00BAE3)

/** Accent color the task brief calls out. Exposed so screens can re-use it if needed. */
@Suppress("unused")
internal val SESSION_ACCENT = Color(0xFFD14B2E)
