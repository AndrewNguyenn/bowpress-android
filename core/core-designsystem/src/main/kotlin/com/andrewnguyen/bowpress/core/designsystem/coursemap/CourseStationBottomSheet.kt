@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.andrewnguyen.bowpress.core.designsystem.coursemap

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.ThreeDScoringSystem
import com.andrewnguyen.bowpress.core.model.UnitSystem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// CourseStationBottomSheet — a draggable 3-snap-point detail sheet drawn over
// a 3D-course map. Recreated from the Claude Design "3D Course - Bottom Sheet"
// handoff; the prototype's HTML/JSX structure is not copied — this is an
// idiomatic Compose port built on Material3 `AnchoredDraggableState`.
//
// The grabber handle + sheet header own the vertical drag; the map below stays
// independently pannable. Content is additive across the three snaps:
//   Peek — header only (station label, close, score, dist/angle/bearing line).
//   Mid  — + circular target plot of the one shot + a 2×2 stat grid.
//   Full — + the station detail (notes, photos), Edit/Discard, a total footer.
// The body scrolls only at Full. Opening a station rises the sheet to Mid;
// closing is the explicit CLOSE ✕ (the sheet cannot be dragged to hidden).
// ---------------------------------------------------------------------------

/** The three open snap points of the station sheet, low → high. */
enum class SheetSnap { PEEK, MID, FULL }

/** Resolved pixel heights of the sheet at each snap, for a given container. */
data class SheetSnapHeights(
    val peek: Float,
    val mid: Float,
    val full: Float,
)

/**
 * Resolve the sheet's pixel height at each snap.
 *
 * @param containerHeightPx the height the sheet draws within (the map area).
 * @param peekHeightPx fixed peek height (≈168dp in px).
 * @param midFraction Mid as a fraction of the container (~0.55).
 * @param fullFraction Full as a fraction of the container (~0.88).
 */
fun resolveSnapHeights(
    containerHeightPx: Float,
    peekHeightPx: Float,
    midFraction: Float = 0.55f,
    fullFraction: Float = 0.88f,
): SheetSnapHeights {
    val full = containerHeightPx * fullFraction
    val mid = (containerHeightPx * midFraction).coerceAtMost(full)
    val peek = peekHeightPx.coerceAtMost(mid)
    return SheetSnapHeights(peek = peek, mid = mid, full = full)
}

/** Horizontal cut distance — the ground range an inclined shot covers. */
internal fun cutDistance(distance: Double, angleDegrees: Double): Double =
    distance * cos(abs(angleDegrees) * PI / 180.0)

/**
 * The draggable station-detail bottom sheet, drawn over a 3D-course map.
 *
 * Pass [station] non-null to open the sheet — it rises to Mid. A null
 * [station] dismisses it. Tapping CLOSE invokes [onClose]; the host clears its
 * selection in response.
 *
 * @param station the focused station, or null when no station is selected.
 * @param containerHeight the height of the area the sheet draws within — the
 *   map area. The sheet's snap heights are derived from it.
 * @param stationCount total stations on the course (drives "OF M").
 * @param runningTotal points scored up to and including this station — shown
 *   in the Full-snap footer.
 * @param editable when true the Full snap shows the Edit / Discard actions;
 *   read-only screens pass false.
 * @param onEdit invoked from the Full-snap Edit action (editable only).
 * @param onDiscard invoked from the Full-snap Discard action (editable only).
 */
@Composable
fun CourseStationBottomSheet(
    station: CourseStation?,
    containerHeight: Dp,
    stationCount: Int,
    system: ThreeDScoringSystem,
    unitSystem: UnitSystem,
    runningTotal: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onEdit: (CourseStation) -> Unit = {},
    onDiscard: (CourseStation) -> Unit = {},
    peekHeight: Dp = 168.dp,
    midFraction: Float = 0.55f,
    fullFraction: Float = 0.88f,
) {
    val density = LocalDensity.current
    val containerPx = with(density) { containerHeight.toPx() }
    val peekPx = with(density) { peekHeight.toPx() }
    val heights = remember(containerPx, peekPx, midFraction, fullFraction) {
        resolveSnapHeights(containerPx, peekPx, midFraction, fullFraction)
    }

    // AnchoredDraggableState positions the sheet's TOP edge: 0 at Full (tallest
    // sheet), larger offsets push the top down toward Peek. Anchor value = the
    // top-edge offset in px from the container top.
    val anchors = remember(heights) {
        DraggableAnchors {
            SheetSnap.PEEK at (containerPx - heights.peek)
            SheetSnap.MID at (containerPx - heights.mid)
            SheetSnap.FULL at (containerPx - heights.full)
        }
    }
    val decaySpec = androidx.compose.animation.rememberSplineBasedDecay<Float>()
    val velocityThresholdPx = with(density) { 80.dp.toPx() }
    // C3: the drag state must survive a re-measure of the map area (system
    // bars, rotation, relayout) — keying it on the container size resets the
    // sheet to PEEK. Key only on the station id so each station gets a fresh
    // state; anchors are kept current separately via `updateAnchors` below.
    val dragState = remember(station?.id) {
        AnchoredDraggableState(
            initialValue = SheetSnap.MID,
            anchors = anchors,
            positionalThreshold = { d -> d * 0.4f },
            velocityThreshold = { velocityThresholdPx },
            snapAnimationSpec = tween(durationMillis = 320),
            decayAnimationSpec = decaySpec,
        )
    }
    // Keep anchors fresh if the container is re-measured.
    LaunchedEffect(anchors) { dragState.updateAnchors(anchors) }

    // Open to Mid on selection; the host nulling `station` tears the sheet down.
    val currentStation by rememberUpdatedState(station)
    LaunchedEffect(station?.id) {
        if (currentStation != null) {
            dragState.animateTo(SheetSnap.MID)
        }
    }

    val current = station ?: return

    // The sheet height is the container minus the live top-edge offset, so it
    // grows / shrinks smoothly as the drag offset interpolates between anchors.
    val topOffsetPx = if (dragState.offset.isNaN()) {
        containerPx - heights.mid
    } else {
        dragState.offset
    }
    val sheetHeightPx = (containerPx - topOffsetPx).coerceIn(0f, containerPx)
    // C2: additive content follows the SETTLED snap, not the in-flight target,
    // so tiers don't pop in mid-drag / mid-animation.
    val settled = dragState.settledValue
    val atFull = settled == SheetSnap.FULL

    // C1: at Full the body scrolls; a body drag must scroll the content first
    // and only move the sheet at the scroll extreme. This nested-scroll
    // connection feeds leftover scroll / fling to the drag state so a drag
    // begun anywhere in the sheet body never reaches the map underneath.
    val scroll = rememberScrollState()
    val nestedScroll = remember(dragState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Dragging the sheet down (positive delta) is consumed by the
                // sheet before the body scrolls — the sheet collapses first.
                return if (delta > 0f) {
                    Offset(0f, dragState.dispatchRawDelta(delta))
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Scroll the body hit its top extreme — leftover upward drag
                // raises the sheet.
                val delta = available.y
                return Offset(0f, dragState.dispatchRawDelta(delta))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (available.y > 0f) {
                    Velocity(0f, dragState.settle(available.y))
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                return Velocity(0f, dragState.settle(available.y))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { sheetHeightPx.toDp() })
                .background(AppPaper)
                .border(width = 1.dp, color = AppLine)
                // C1: the WHOLE opaque sheet is the drag surface — a drag
                // begun anywhere on it moves the sheet, so nothing leaks
                // through to the map's pan detector below. At Full the
                // nested-scroll connection lets the body scroll first.
                .nestedScroll(nestedScroll)
                .anchoredDraggable(dragState, Orientation.Vertical),
        ) {
            Grabber()
            SheetHeader(
                station = current,
                stationCount = stationCount,
                unitSystem = unitSystem,
                snap = settled,
                onClose = onClose,
            )

            // BODY — additive content. Scrollable only at Full.
            val bodyModifier = if (atFull) {
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
            } else {
                Modifier.fillMaxWidth()
            }
            Column(
                modifier = bodyModifier.padding(horizontal = 18.dp),
            ) {
                if (settled == SheetSnap.MID || settled == SheetSnap.FULL) {
                    SheetMidBlock(current, system, unitSystem)
                }
                if (settled == SheetSnap.FULL) {
                    SheetFullBlock(
                        station = current,
                        runningTotal = runningTotal,
                        editable = editable,
                        onEdit = { onEdit(current) },
                        onDiscard = { onDiscard(current) },
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header — always visible at every snap.
// ---------------------------------------------------------------------------

@Composable
private fun Grabber() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppInk3.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun SheetHeader(
    station: CourseStation,
    stationCount: Int,
    unitSystem: UnitSystem,
    snap: SheetSnap,
    onClose: () -> Unit,
) {
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "● STATION ${"%02d".format(station.stationNumber)} OF $stationCount",
                style = jetbrainsMono(10.5.sp, FontWeight.SemiBold).copy(color = AppMaple),
            )
            // Close hit-target is deliberately above the drag layer (it's a
            // child of the draggable Column but consumes its own clicks).
            Text(
                "CLOSE ✕",
                style = interUI(10.5.sp, FontWeight.Bold).copy(color = AppInk2),
                modifier = Modifier.clickable(onClick = onClose),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Station ${station.stationNumber}",
                    style = frauncesDisplay(30.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        DistanceFormatting.short(
                            station.estimatedDistance, station.distanceUnit, unitSystem,
                        ),
                        style = frauncesDisplay(15.sp, italic = true, weight = FontWeight.Medium)
                            .copy(color = AppInk),
                    )
                    Text(
                        "${AngleFormatting.signed(station.angleDegrees)} cut",
                        style = jetbrainsMono(11.sp).copy(color = AppInk3),
                    )
                    station.bearingDegrees?.let { bearing ->
                        Text(
                            BearingFormatting.compass(bearing),
                            style = jetbrainsMono(11.sp).copy(color = AppInk3),
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (station.ring == 0) "M" else "${station.ring}",
                    style = frauncesDisplay(36.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = if (station.ring == 0) AppMaple else AppPondDk),
                )
                Text(
                    "SCORE",
                    style = interUI(8.5.sp, FontWeight.Bold).copy(color = AppInk3),
                )
            }
        }
        if (snap == SheetSnap.PEEK) {
            Spacer(Modifier.height(10.dp))
            Text(
                "SWIPE UP FOR PLOT & STATS",
                style = interUI(9.5.sp, FontWeight.SemiBold).copy(color = AppInk3),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mid block — circular target plot + 2×2 stat grid.
// ---------------------------------------------------------------------------

@Composable
private fun SheetMidBlock(
    station: CourseStation,
    system: ThreeDScoringSystem,
    unitSystem: UnitSystem,
) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppLine2),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                CircleTargetView(
                    system = system,
                    arrows = arrowsFor(station),
                    showLabels = false,
                    modifier = Modifier.size(128.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "SHOT PLOT",
                    style = interUI(8.5.sp, FontWeight.Bold).copy(color = AppInk3),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val dist = station.estimatedDistance
            val cut = if (dist != null) cutDistance(dist, station.angleDegrees) else null
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth()) {
                    StatGridCell(
                        "DIST",
                        DistanceFormatting.short(dist, station.distanceUnit, unitSystem),
                        Modifier.weight(1f),
                    )
                    StatGridCell(
                        "ANGLE",
                        AngleFormatting.signed(station.angleDegrees),
                        Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    StatGridCell(
                        "CUT",
                        DistanceFormatting.short(cut, station.distanceUnit, unitSystem),
                        Modifier.weight(1f),
                    )
                    StatGridCell(
                        "BEARING",
                        station.bearingDegrees?.let { "${it.roundToInt()}°" } ?: "—",
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatGridCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, AppLine2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = interUI(8.5.sp, FontWeight.Bold).copy(color = AppInk3))
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = AppInk),
        )
    }
}

// ---------------------------------------------------------------------------
// Full block — station detail (notes / photos), actions, running total.
//
// The design's Full tier showed an arrow-by-arrow list, but a BowPress
// `CourseStation` is genuinely one shot per station (a single ring + plot),
// so the single-shot detail — photos + note — is the correct adaptation, not
// a one-row "list".
// ---------------------------------------------------------------------------

@Composable
private fun SheetFullBlock(
    station: CourseStation,
    runningTotal: Int,
    editable: Boolean,
    onEdit: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(Modifier.padding(top = 18.dp)) {
        Text(
            "STATION DETAIL",
            style = interUI(9.sp, FontWeight.Bold).copy(color = AppInk3),
        )
        Spacer(Modifier.height(8.dp))

        if (station.hasScenePhoto || station.hasArrowPhoto) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (station.hasScenePhoto) {
                    StationPhotoTile(
                        station.id, CourseStationPhotoStore.Slot.SCENE,
                        present = true, side = 96.dp,
                    )
                }
                if (station.hasArrowPhoto) {
                    StationPhotoTile(
                        station.id, CourseStationPhotoStore.Slot.ARROW,
                        present = true, side = 96.dp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val note = station.notes?.takeIf { it.isNotBlank() }
        Column(
            Modifier
                .fillMaxWidth()
                .background(AppPaper2)
                .border(1.dp, AppLine)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                "NOTE",
                style = interUI(9.sp, FontWeight.Bold).copy(color = AppPondDk),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                note ?: "No note for this station.",
                style = frauncesDisplay(14.sp, italic = true, weight = FontWeight.Normal)
                    .copy(color = AppInk2),
            )
        }

        if (editable) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionCell("EDIT STATION", AppPondDk, Modifier.weight(1f), onEdit)
                ActionCell("DISCARD", AppMaple, Modifier.weight(1f), onDiscard)
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppLine2),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "RUNNING TOTAL · $runningTotal pts after ${station.stationNumber} stations",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
    }
}

@Composable
private fun ActionCell(
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(AppPaper)
            .border(1.dp, AppLine)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = interUI(10.sp, FontWeight.Bold).copy(color = tone),
            )
            Text(
                " ›",
                style = frauncesDisplay(12.sp, italic = true).copy(color = tone),
            )
        }
    }
}
