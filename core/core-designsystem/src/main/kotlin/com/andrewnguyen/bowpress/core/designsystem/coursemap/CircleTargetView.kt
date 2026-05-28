package com.andrewnguyen.bowpress.core.designsystem.coursemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondLt
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import kotlin.math.min
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// CircleTargetView — Android port of the iOS 3D circular scoring target.
//
// A plotted arrow's position is normalized -1…1 from centre (the same space
// `CourseStation.plotX/plotY` store). The target draws concentric kill-zone
// ring bands per `ThreeDScoringSystem` (no animal silhouette). When `onPlot`
// is supplied the target is interactive: touch-drag-release drops the arrow
// and reports its normalized position, and while the finger is down it
// publishes `CircleLensSnapshot`s so the host can float a precision
// magnifier.
// ---------------------------------------------------------------------------

/** A plotted arrow on the 3D circular target — position normalized -1…1. */
data class CircleArrow(
    val id: String,
    val x: Double,
    val y: Double,
)

/** Outer scoring radius as a fraction of the view's side (`baseR = side * 0.46`). */
private const val RADIUS_FRACTION = 0.46f

/**
 * The 3D-archery scoring target: concentric kill-zone rings.
 *
 * @param onPlot nil = display-only. Non-nil = interactive; reports normalized x,y.
 * @param onLensSnapshotChanged fired on every drag tick with a snapshot for the
 *   magnifier (nil when the finger lifts). The host renders `CircleLensOverlay`
 *   at its root.
 */
@Composable
fun CircleTargetView(
    system: ThreeDScoringSystem,
    arrows: List<CircleArrow> = emptyList(),
    showLabels: Boolean = true,
    onPlot: ((Double, Double) -> Unit)? = null,
    onLensSnapshotChanged: ((CircleLensSnapshot?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = jetbrainsMono(9.sp)
    // The target square's window origin — captured so lens snapshots can be
    // expressed in root coordinates.
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coords ->
                rootOrigin = coords.positionInWindow()
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onPlot != null) {
                        Modifier.pointerInput(system, onPlot, onLensSnapshotChanged) {
                            // `detectDragGestures` fires onDragStart on the first
                            // touch and onDragEnd on release even when the finger
                            // never moves — so a plain tap plots too.
                            var current = Offset.Zero
                            detectDragGestures(
                                onDragStart = { start ->
                                    current = start
                                    publishSnapshot(
                                        size, start, rootOrigin, system,
                                        onLensSnapshotChanged,
                                    )
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    current += drag
                                    publishSnapshot(
                                        size, current, rootOrigin, system,
                                        onLensSnapshotChanged,
                                    )
                                },
                                onDragCancel = {
                                    onLensSnapshotChanged?.invoke(null)
                                },
                                onDragEnd = {
                                    onLensSnapshotChanged?.invoke(null)
                                    val side = min(size.width, size.height).toFloat()
                                    val baseR = side * RADIUS_FRACTION
                                    if (baseR > 0f) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val dx = (current.x - cx) / baseR
                                        val dy = (current.y - cy) / baseR
                                        onPlot.invoke(dx.toDouble(), dy.toDouble())
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val side = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = side * RADIUS_FRACTION

            // --- Ring bands -------------------------------------------------
            // Outermost → innermost so inner bands paint on top. Each band is
            // an opaque cream base plus a pond wash that deepens toward the
            // centre.
            val bands = system.bands
            val count = bands.size
            for (index in bands.indices.reversed()) {
                val band = bands[index]
                val r = baseR * band.outerFraction.toFloat()
                drawCircle(color = AppCream, radius = r, center = center)
                val depth = if (count > 1) {
                    (count - 1 - index).toFloat() / (count - 1).toFloat()
                } else {
                    0f
                }
                if (depth > 0f) {
                    drawCircle(
                        color = AppPondLt.copy(alpha = (depth * 0.5f).coerceIn(0f, 1f)),
                        radius = r,
                        center = center,
                    )
                }
                drawCircle(
                    color = AppLine,
                    radius = r,
                    center = center,
                    style = Stroke(width = if (index == 0) 0.9f else 0.5f),
                )
            }
            // Centre mark.
            drawCircle(color = AppInk, radius = 1.2f, center = center)

            // --- Ring value labels -----------------------------------------
            if (showLabels) {
                for (band in bands) {
                    val r = baseR * band.outerFraction.toFloat()
                    val measured = textMeasurer.measure(band.value.toString(), labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        color = AppInk3,
                        topLeft = Offset(
                            x = center.x - measured.size.width / 2f,
                            // iOS positions the text's centre at `center.y - r + 9`.
                            y = center.y - r + 9f - measured.size.height / 2f,
                        ),
                    )
                }
            }

            // --- Arrow dots -------------------------------------------------
            arrows.forEachIndexed { idx, arrow ->
                val opacity = (0.4f + 0.6f *
                    ((idx + 1).toFloat() / arrows.size.coerceAtLeast(1).toFloat()))
                    .coerceIn(0f, 1f)
                val px = center.x + arrow.x.toFloat() * baseR
                val py = center.y + arrow.y.toFloat() * baseR
                // Cream halo then ink dot (7.2pt dot, 0.9pt cream border).
                drawCircle(
                    color = AppCream.copy(alpha = opacity),
                    radius = 3.6f + 0.9f,
                    center = Offset(px, py),
                )
                drawCircle(
                    color = AppInk.copy(alpha = opacity),
                    radius = 3.6f,
                    center = Offset(px, py),
                )
            }
        }
    }
}

/** Builds and publishes a lens snapshot for the given local touch point. */
private fun publishSnapshot(
    canvasSize: IntSize,
    localTouch: Offset,
    rootOrigin: Offset,
    system: ThreeDScoringSystem,
    onLensSnapshotChanged: ((CircleLensSnapshot?) -> Unit)?,
) {
    val cb = onLensSnapshotChanged ?: return
    val side = min(canvasSize.width, canvasSize.height).toFloat()
    val baseR = side * RADIUS_FRACTION
    if (baseR <= 0f) return
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val dx = (localTouch.x - cx) / baseR
    val dy = (localTouch.y - cy) / baseR
    val d = min(sqrt((dx * dx + dy * dy).toDouble()), 1.0)
    val ring = system.ringForNormalizedRadius(d)
    // The target square's window origin — the Box is aspect-fit 1:1, but
    // inset defensively in case width != height.
    val squareOrigin = Offset(
        x = rootOrigin.x + (canvasSize.width - side) / 2f,
        y = rootOrigin.y + (canvasSize.height - side) / 2f,
    )
    cb(
        CircleLensSnapshot(
            touchRoot = Offset(rootOrigin.x + localTouch.x, rootOrigin.y + localTouch.y),
            targetOriginRoot = squareOrigin,
            targetSize = side,
            system = system,
            previewRing = ring,
        ),
    )
}

// ---------------------------------------------------------------------------
// Precision magnifier (Circle lens)
//
// `CircleTargetView` publishes a snapshot on every drag tick; the host stores
// it in a `CircleLensController` and renders `CircleLensOverlay` at its root
// z-level so the loupe can float above the target and the chrome below it.
// ---------------------------------------------------------------------------

/**
 * A live magnifier snapshot — everything `CircleLensView` needs to draw the
 * loupe, all in window/root coordinates (pixels).
 */
data class CircleLensSnapshot(
    /** The finger position, window/root coords (px). */
    val touchRoot: Offset,
    /** Top-left of the target square, window/root coords (px). */
    val targetOriginRoot: Offset,
    /** Side length of the target square (px). */
    val targetSize: Float,
    val system: ThreeDScoringSystem,
    /** Ring value at the live touch point — drives the score stamp. */
    val previewRing: Int,
)

/**
 * Holds the live lens snapshot. Backed by a mutable state so only
 * `CircleLensOverlay` re-renders on each drag tick — the host's body doesn't.
 */
class CircleLensController {
    var snapshot: CircleLensSnapshot? by mutableStateOf(null)
}

/** Renders the live lens — the only composable that observes the controller. */
@Composable
fun CircleLensOverlay(controller: CircleLensController) {
    val snap = controller.snapshot
    if (snap != null) {
        // `publishSnapshot` writes touchRoot / targetOriginRoot in window
        // coordinates, but `Modifier.offset` on the lens disc and stamp
        // positions them relative to the overlay's parent (which sits below
        // the status bar / inside scaffold padding). Capture the overlay's
        // window origin and rebase the snapshot into overlay-local coords so
        // the lens lands directly under the finger instead of being shifted
        // down by the status-bar height.
        var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { overlayOrigin = it.positionInWindow() },
        ) {
            CircleLensView(snapshot = snap.rebasedTo(overlayOrigin))
        }
    }
}

private fun CircleLensSnapshot.rebasedTo(origin: Offset): CircleLensSnapshot =
    copy(
        touchRoot = touchRoot - origin,
        targetOriginRoot = targetOriginRoot - origin,
    )

private const val LENS_SIZE_RATIO = 0.75f
private const val LENS_ZOOM = 2.5f

/**
 * The floating loupe — a magnified slice of the circular target centred on
 * the finger, with a maple footprint ring and a score stamp above.
 */
@Composable
fun CircleLensView(snapshot: CircleLensSnapshot) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // All snapshot geometry is in pixels; lay the loupe out in px too,
        // then convert to dp where Compose modifiers need it.
        val screenWidthPx = with(density) { maxWidth.toPx() }

        val touch = snapshot.touchRoot
        val lensSizePx = maxOf(with(density) { 120.dp.toPx() },
            snapshot.targetSize * LENS_SIZE_RATIO)
        val lensRadiusPx = lensSizePx / 2f

        // Prefer the lens above the finger; flip below only if it would clip
        // the screen top.
        val thumbHalfPx = with(density) { 30.dp.toPx() }
        val centerBufferPx = with(density) { 25.dp.toPx() }
        val edgeBufferPx = with(density) { 4.dp.toPx() }
        val stampOffsetPx = with(density) { 46.dp.toPx() }
        val touchClearance = thumbHalfPx + centerBufferPx
        val preferredAboveY = touch.y - touchClearance
        val placeBelow = preferredAboveY - lensRadiusPx < edgeBufferPx
        val lensCenterY = if (placeBelow) touch.y + touchClearance else preferredAboveY
        val lensCenterX = touch.x.coerceIn(
            lensRadiusPx + edgeBufferPx,
            (screenWidthPx - lensRadiusPx - edgeBufferPx)
                .coerceAtLeast(lensRadiusPx + edgeBufferPx),
        )

        // Touch position relative to the target square's top-left, in pixels.
        // Used to place the zoomed ring centre so the touched canvas point
        // lands at the lens centre.
        val touchInTargetX = touch.x - snapshot.targetOriginRoot.x
        val touchInTargetY = touch.y - snapshot.targetOriginRoot.y

        val lensSizeDp = with(density) { lensSizePx.toDp() }
        val footprintDp = 22.dp

        // --- The loupe disc -------------------------------------------------
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (lensCenterX - lensRadiusPx).toInt(),
                        y = (lensCenterY - lensRadiusPx).toInt(),
                    )
                }
                .size(lensSizeDp)
                .shadow(elevation = 16.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(AppPaper)
                .border(width = 1.dp, color = AppInk, shape = CircleShape),
        ) {
            // Magnified slice of the target — drawn DIRECTLY into a Canvas
            // sized to the lens disc, with the rings positioned so the touched
            // canvas point lands at the lens centre. Avoids the
            // `.offset(...).requiredSize(zoomedTargetDp)` nesting that earlier
            // fought Compose's layout/clip semantics and left the lens content
            // visibly misaligned from the touch point.
            val baseRZoomed = snapshot.targetSize * RADIUS_FRACTION * LENS_ZOOM
            val canvasCenterInLens = Offset(
                x = lensRadiusPx + (snapshot.targetSize / 2f - touchInTargetX) * LENS_ZOOM,
                y = lensRadiusPx + (snapshot.targetSize / 2f - touchInTargetY) * LENS_ZOOM,
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bands = snapshot.system.bands
                val count = bands.size
                for (index in bands.indices.reversed()) {
                    val band = bands[index]
                    val r = baseRZoomed * band.outerFraction.toFloat()
                    drawCircle(color = AppCream, radius = r, center = canvasCenterInLens)
                    val depth = if (count > 1) {
                        (count - 1 - index).toFloat() / (count - 1).toFloat()
                    } else {
                        0f
                    }
                    if (depth > 0f) {
                        drawCircle(
                            color = AppPondLt.copy(alpha = (depth * 0.5f).coerceIn(0f, 1f)),
                            radius = r,
                            center = canvasCenterInLens,
                        )
                    }
                    drawCircle(
                        color = AppLine,
                        radius = r,
                        center = canvasCenterInLens,
                        style = Stroke(width = if (index == 0) 0.9f else 0.5f),
                    )
                }
                drawCircle(color = AppInk, radius = 1.2f, center = canvasCenterInLens)
            }
            // Maple footprint ring + pin at the lens centre.
            //
            // Positioned explicitly with `offset` rather than `align(Center)`
            // because the sibling Box above uses `requiredSize(zoomedTargetDp)`
            // (~3120 px, much larger than the lens disc). Compose's Box
            // measurePolicy reports the LARGEST child's size up the chain,
            // and `.size(lensSizeDp)` doesn't actually clamp it — so
            // `.align(Center)` was centering within the zoomed size, not the
            // lens size, dropping the maple footprint hundreds of pixels
            // away from the true lens center.
            val footprintRadiusPx = with(density) { (footprintDp / 2).toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (lensRadiusPx - footprintRadiusPx).toInt(),
                            y = (lensRadiusPx - footprintRadiusPx).toInt(),
                        )
                    }
                    .size(footprintDp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val ringR = this.size.minDimension / 2f
                    drawCircle(
                        color = AppMaple.copy(alpha = 0.18f),
                        radius = ringR,
                        center = c,
                    )
                    drawCircle(
                        color = AppMaple,
                        radius = ringR - 1f,
                        center = c,
                        style = Stroke(width = 2f),
                    )
                    drawCircle(color = AppMaple, radius = 1.5f, center = c)
                }
            }
        }

        // --- Score stamp above the loupe ------------------------------------
        val ring = snapshot.previewRing
        val stampLabel = if (ring == 0) "M" else ring.toString()
        val stampBg = if (ring == 0) AppMaple else AppInk
        val stampStyle = frauncesDisplay(28.sp, italic = true)
        val measured = textMeasurer.measure(stampLabel, stampStyle)
        val hPadPx = with(density) { 14.dp.toPx() }
        val vPadPx = with(density) { 6.dp.toPx() }
        val stampWidthPx = measured.size.width + hPadPx * 2f
        val stampHeightPx = measured.size.height + vPadPx * 2f
        val stampCenterY = maxOf(
            stampOffsetPx / 2f,
            lensCenterY - lensRadiusPx - stampOffsetPx,
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (lensCenterX - stampWidthPx / 2f).toInt(),
                        y = (stampCenterY - stampHeightPx / 2f).toInt(),
                    )
                }
                .background(stampBg)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = stampLabel,
                style = stampStyle,
                color = AppPaper,
            )
        }
    }
}
