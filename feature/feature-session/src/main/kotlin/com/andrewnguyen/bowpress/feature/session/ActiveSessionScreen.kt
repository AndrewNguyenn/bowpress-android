package com.andrewnguyen.bowpress.feature.session

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPBigScore
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEditLink
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetFace
import com.andrewnguyen.bowpress.core.designsystem.bp.BPTargetStyle
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Kenrokuen active-session screen — live header with pulsing "IN SESSION" label,
 * config banner, target face with tap-to-plot, recent-arrows strip, running
 * totals, and actions / finish bar. Mirrors iOS `activeSessionContent` in
 * `bowpress-ios/Sources/BowPress/Session/SessionView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionId: String,
    onSessionEnded: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showConfigSheet by remember { mutableStateOf(false) }
    var showEndSheet by remember { mutableStateOf(false) }

    // Track whether we've ever seen the session as active so a transient null during
    // initial hydration doesn't bounce the user back to the home screen.
    var sawActive by remember { mutableStateOf(false) }
    if (state.activeSession != null) sawActive = true
    val justEnded = sawActive && state.activeSession == null
    LaunchedEffect(justEnded) {
        if (justEnded) onSessionEnded()
    }
    val active = state.activeSession ?: return

    Box(modifier = Modifier.fillMaxSize().background(AppPaper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            LiveHeader(
                arrowNumber = state.currentArrows.size + 1,
                sessionStart = active.startedAt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 10.dp)
                    .drawBottomHairline(),
            )

            ConfigBanner(
                state = state,
                onChangeClick = { showConfigSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            TargetSection(
                state = state,
                onPlot = { plotX, plotY, ring, zone ->
                    scope.launch { viewModel.plotArrow(plotX, plotY, ring, zone) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            // iOS SessionView doesn't surface a Recent Arrows strip at all
            // (no equivalent in SessionView.swift). Render it only once
            // arrows have been plotted so the empty-state layout matches iOS;
            // future iters can decide whether to drop the strip entirely.
            if (state.currentArrows.isNotEmpty()) {
                RecentArrowsStrip(
                    arrows = state.currentArrows,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            RunningTotals(
                arrows = state.currentArrows,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .drawBottomHairline(),
            )

            ActionsRow(
                canUndo = state.currentArrows.isNotEmpty(),
                arrowCount = state.currentArrows.size,
                onUndo = { scope.launch { viewModel.removeLastArrow() } },
                onAddNote = { showEndSheet = true },
                onFinish = { showEndSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 10.dp),
            )

            FinishBar(
                onDiscard = { showEndSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .drawTopHairline(),
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showConfigSheet) {
        SessionConfigSheet(
            state = state,
            onDismiss = { showConfigSheet = false },
            onConfirm = { bowConfigId, arrowConfigId ->
                viewModel.changeConfig(bowConfigId, arrowConfigId)
                showConfigSheet = false
            },
        )
    }

    if (showEndSheet) {
        EndSessionSheet(
            onDismiss = { showEndSheet = false },
            onFinish = { notes, feelTags ->
                scope.launch {
                    viewModel.endSession(notes, feelTags)
                    showEndSheet = false
                    onSessionEnded()
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Live header — "IN SESSION" pulse + "Arrow N" title + elapsed timer
// ---------------------------------------------------------------------------

@Composable
private fun LiveHeader(
    arrowNumber: Int,
    sessionStart: Instant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LivePulseLabel()
            val title = buildAnnotatedString {
                withStyle(SpanStyle(color = AppInk)) { append("Arrow ") }
                withStyle(SpanStyle(color = AppPondDk)) { append(arrowNumber.toString()) }
            }
            Text(
                text = title,
                style = frauncesDisplay(32.sp, italic = true, weight = FontWeight.Medium),
            )
        }
        Spacer(Modifier.width(8.dp))
        TimerBlock(sessionStart = sessionStart)
    }
}

@Composable
private fun LivePulseLabel() {
    val transition = rememberInfiniteTransition(label = "in_session_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "in_session_alpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = AppMaple.copy(alpha = alpha))
        }
        Text(
            text = "IN SESSION",
            style = interUI(9.5.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.28.em,
                color = AppPondDk,
            ),
        )
    }
}

@Composable
private fun TimerBlock(sessionStart: Instant) {
    val elapsedSeconds by produceState(initialValue = secondsSince(sessionStart), sessionStart) {
        while (true) {
            value = secondsSince(sessionStart)
            delay(1000L)
        }
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = formatElapsed(elapsedSeconds),
            style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
        )
        Text(
            text = "ELAPSED",
            style = jetbrainsMono(10.5.sp).copy(letterSpacing = 0.06.em, color = AppInk3),
        )
    }
}

private fun secondsSince(start: Instant): Long {
    val diff = Instant.now().epochSecond - start.epochSecond
    return diff.coerceAtLeast(0)
}

private fun formatElapsed(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------------------
// Config banner — "<distance> · <bow> · <arrow>" + CHANGE link
// ---------------------------------------------------------------------------

@Composable
private fun ConfigBanner(
    state: SessionUiState,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.hasPendingConfigChange) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPaper2)
                    .border(1.dp, AppLine)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "■",
                    style = frauncesDisplay(10.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppMaple),
                )
                Text(
                    text = "Config changed — plot an arrow to confirm new config",
                    style = frauncesDisplay(12.sp, italic = true).copy(color = AppMaple),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBottomHairline()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = bannerPrimary(state),
                    style = frauncesDisplay(13.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppInk),
                    maxLines = 1,
                )
                bannerSub(state)?.let { sub ->
                    Text(
                        text = sub,
                        style = jetbrainsMono(9.5.sp).copy(letterSpacing = 0.04.em, color = AppInk3),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            BPEditLink(onClick = onChangeClick, label = "CHANGE")
        }
    }
}

private fun bannerPrimary(state: SessionUiState): String {
    val distance = state.activeSession?.distance?.label ?: "—"
    val bow = state.selectedBow?.name ?: "—"
    val arrow = (state.pendingArrowConfig ?: state.activeArrowConfig)?.label ?: "—"
    return "$distance · $bow · $arrow"
}

private fun bannerSub(state: SessionUiState): String? {
    val c = state.activeSession?.conditions ?: return null
    val parts = mutableListOf<String>()
    c.windSpeed?.let { parts += "WIND %.0fKT".format(it) }
    c.tempF?.let {
        val tC = (it - 32.0) * 5.0 / 9.0
        parts += "%.0f°C".format(tC)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

// ---------------------------------------------------------------------------
// Target section — BPTargetFace + overlay dots + tap-to-plot gesture
// ---------------------------------------------------------------------------

private val TARGET_FACE_SIZE = 300.dp

@Composable
private fun TargetSection(
    state: SessionUiState,
    onPlot: (plotX: Double, plotY: Double, ring: Int, zone: Zone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val faceType = state.targetFaceType
    val arrowDiameterMm = state.activeArrowConfig?.shaftDiameterEnum?.rawValue ?: 5.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            CrumbOverlay(faceType = faceType)
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TargetInteractiveFace(
                arrows = state.currentArrows,
                faceType = faceType,
                arrowDiameterMm = arrowDiameterMm,
                isEnabled = !state.isLoading,
                onArrowPlotted = onPlot,
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "tap where the arrow landed. long-press to fine-adjust.",
                style = frauncesDisplay(11.sp, italic = true).copy(color = AppInk3),
            )
        }
    }
}

@Composable
private fun CrumbOverlay(faceType: TargetFaceType) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BPEyebrow("FACE")
        Text(
            text = "${faceName(faceType)} · ${faceSize(faceType)}",
            style = frauncesDisplay(10.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = AppPondDk),
        )
    }
}

private fun faceName(face: TargetFaceType): String = when (face) {
    TargetFaceType.TEN_RING -> "10-ring"
    TargetFaceType.SIX_RING -> "6-ring"
}

private fun faceSize(face: TargetFaceType): String = when (face) {
    TargetFaceType.TEN_RING -> "122cm"
    TargetFaceType.SIX_RING -> "80cm"
}

/**
 * BPTargetFace (visual) + Canvas overlay (arrow dots) + drag-to-plot gesture
 * with the Pen-magnifier UX (iOS f2be7ea). The thumb-offset trick is gone:
 * the dot now commits AT the finger position, and a floating
 * [PenLensOverlay] provides 2.5× magnification + a live score stamp during
 * drag so the archer can see where the dot will land. Snapshot updates
 * fire on every drag tick.
 */
@Composable
private fun TargetInteractiveFace(
    arrows: List<ArrowPlot>,
    faceType: TargetFaceType,
    arrowDiameterMm: Double,
    isEnabled: Boolean,
    onArrowPlotted: (plotX: Double, plotY: Double, ring: Int, zone: Zone) -> Unit,
) {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { 11.dp.toPx() }
    val dotStrokePx = with(density) { 1.2.dp.toPx() }
    val geometry = TargetGeometry.forFace(faceType)
    // dragPreview is the live touch position in the inner Canvas's local
    // pixel space, used to drive the lens snapshot.
    var dragPreview by remember { mutableStateOf<Offset?>(null) }
    var lensSnapshot by remember { mutableStateOf<PenLensSnapshot?>(null) }

    Box(modifier = Modifier.size(TARGET_FACE_SIZE)) {
        BPTargetFace(size = TARGET_FACE_SIZE, style = BPTargetStyle.WA)

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(isEnabled, faceType, arrowDiameterMm) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            dragPreview = start
                            lensSnapshot = buildPenLensSnapshot(
                                start, size.width.toFloat(), arrows,
                                arrowDiameterMm, geometry, faceType,
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPreview = change.position
                            lensSnapshot = buildPenLensSnapshot(
                                change.position, size.width.toFloat(), arrows,
                                arrowDiameterMm, geometry, faceType,
                            )
                        },
                        onDragEnd = {
                            val placement = dragPreview
                            dragPreview = null
                            lensSnapshot = null
                            if (placement == null) return@detectDragGestures
                            val radiusPx = (minOf(size.width, size.height) / 2f).toDouble()
                            if (radiusPx <= 0.0) return@detectDragGestures
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            // Commit AT the finger position — iOS f2be7ea.
                            val plotX = (placement.x - centerX).toDouble() / radiusPx
                            val plotY = (placement.y - centerY).toDouble() / radiusPx
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
                        onDragCancel = {
                            dragPreview = null
                            lensSnapshot = null
                        },
                    )
                },
        ) {
            val canvasRadiusPx = minOf(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Opacity fade over the last 6 arrows (newest → oldest).
            val fadeScale = listOf(1.0f, 0.78f, 0.62f, 0.50f, 0.38f, 0.28f)
            val total = arrows.size
            arrows.forEachIndexed { idx, arrow ->
                val px = arrow.plotX ?: return@forEachIndexed
                val py = arrow.plotY ?: return@forEachIndexed
                val fromEnd = total - 1 - idx
                val alpha = when {
                    fromEnd >= fadeScale.size -> fadeScale.last()
                    else -> fadeScale[fromEnd]
                }
                val cx = (center.x + px * canvasRadiusPx).toFloat()
                val cy = (center.y + py * canvasRadiusPx).toFloat()
                drawCircle(
                    color = AppInk.copy(alpha = alpha),
                    radius = dotRadiusPx,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = AppCream.copy(alpha = alpha),
                    radius = dotRadiusPx,
                    center = Offset(cx, cy),
                    style = Stroke(width = dotStrokePx),
                )
            }
            // No more drag-preview dot — the lens is the visual aid now.
        }

        // Pen magnifier — same z-level as the target so it can extend past
        // the target's frame. Renders nothing while not dragging.
        PenLensOverlay(snapshot = lensSnapshot, modifier = Modifier.matchParentSize())
    }
}

// Snapshot building lives in PenLensOverlay.kt — shared with TargetPlot.

// ---------------------------------------------------------------------------
// Recent arrows strip — 6-cell grid with avg of last N
// ---------------------------------------------------------------------------

@Composable
private fun RecentArrowsStrip(
    arrows: List<ArrowPlot>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .drawTopHairline()
            .drawBottomHairline()
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BPEyebrow("RECENT ARROWS")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recentAvgString(arrows),
                    style = frauncesDisplay(16.sp, italic = true, weight = FontWeight.Medium)
                        .copy(color = AppPondDk),
                )
                Text(
                    text = " AVG OF LAST ${minOf(6, arrows.size)}",
                    style = interUI(8.5.sp, weight = FontWeight.SemiBold).copy(
                        letterSpacing = 0.12.em,
                        color = AppInk3,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val startIdx = maxOf(0, arrows.size - 6)
            for (col in 0 until 6) {
                val cellIdx = startIdx + col
                val arrow = arrows.getOrNull(cellIdx)
                RecentCell(
                    arrow = arrow,
                    arrowNumber = if (arrow != null) cellIdx + 1 else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecentCell(
    arrow: ArrowPlot?,
    arrowNumber: Int?,
    modifier: Modifier = Modifier,
) {
    val isX = arrow?.ring == 11
    val bg = if (arrow == null) AppPaper2 else AppPaper
    val border = if (isX) AppPondDk else AppLine
    val valueColor = when {
        arrow == null -> AppInk3
        isX -> AppPondDk
        else -> AppInk
    }
    Column(
        modifier = modifier
            .background(bg)
            .border(1.dp, border)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = arrow?.let { ringLabel(it.ring) } ?: "—",
            style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = valueColor),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = arrowNumber?.let { "#$it" } ?: "—",
            style = interUI(8.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.16.em,
                color = AppInk3,
            ),
        )
    }
}

private fun ringLabel(ring: Int): String = if (ring == 11) "X" else ring.toString()

private fun recentAvgString(arrows: List<ArrowPlot>): String {
    if (arrows.isEmpty()) return "0.0"
    val last = arrows.takeLast(6)
    val total = last.sumOf { minOf(it.ring, 10) }
    return "%.1f".format(total.toDouble() / last.size)
}

// ---------------------------------------------------------------------------
// Running totals — AVG / XS / SESSION BEST
// ---------------------------------------------------------------------------

@Composable
private fun RunningTotals(arrows: List<ArrowPlot>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TotalsPrimary(arrows = arrows, modifier = Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(60.dp).background(AppLine2))
        TotalsXs(arrows = arrows, modifier = Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(60.dp).background(AppLine2))
        TotalsBest(arrows = arrows, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TotalsPrimary(arrows: List<ArrowPlot>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BPEyebrow("AVG SO FAR")
        BPBigScore(value = avgSoFarString(arrows), size = 28.sp)
        Text(
            text = "across ${arrows.size} arrow${if (arrows.size == 1) "" else "s"}",
            style = interUI(9.sp).copy(color = AppInk3),
        )
    }
}

@Composable
private fun TotalsXs(arrows: List<ArrowPlot>, modifier: Modifier = Modifier) {
    val xs = arrows.count { it.ring == 11 }
    val total = arrows.size
    val rate = if (total > 0) (xs.toDouble() / total.toDouble()) * 100.0 else 0.0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BPEyebrow("XS")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = xs.toString(),
                style = frauncesDisplay(24.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "/$total",
                style = jetbrainsMono(11.sp).copy(color = AppInk3),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = "%.0f%% rate".format(rate),
            style = interUI(9.sp).copy(color = AppInk3),
        )
    }
}

@Composable
private fun TotalsBest(arrows: List<ArrowPlot>, modifier: Modifier = Modifier) {
    val best = bestStreak(arrows)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BPEyebrow("SESSION BEST")
        Text(
            text = best.label,
            style = frauncesDisplay(18.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = AppInk),
        )
        Text(
            text = best.sub,
            style = interUI(9.sp).copy(color = AppInk3),
        )
    }
}

private fun avgSoFarString(arrows: List<ArrowPlot>): String {
    if (arrows.isEmpty()) return "0.0"
    val total = arrows.sumOf { minOf(it.ring, 10) }
    return "%.1f".format(total.toDouble() / arrows.size)
}

private data class BestStreak(val label: String, val sub: String)

private fun bestStreak(arrows: List<ArrowPlot>): BestStreak {
    if (arrows.isEmpty()) return BestStreak("—", "no arrows yet")
    var bestX = 0
    var curX = 0
    var bestStart = 0
    var curStart = 0
    for ((i, a) in arrows.withIndex()) {
        if (a.ring == 11) {
            if (curX == 0) curStart = i
            curX += 1
            if (curX > bestX) {
                bestX = curX
                bestStart = curStart
            }
        } else {
            curX = 0
        }
    }
    if (bestX >= 2) {
        val end = bestStart + bestX - 1
        return BestStreak("X", "$bestX in a row · #${bestStart + 1}–${end + 1}")
    }
    val bestRing = arrows.maxOf { it.ring }
    return BestStreak(ringLabel(bestRing), "best shot so far")
}

// ---------------------------------------------------------------------------
// Actions row — Undo / Note / Finish
// ---------------------------------------------------------------------------

@Composable
private fun ActionsRow(
    canUndo: Boolean,
    arrowCount: Int,
    onUndo: () -> Unit,
    onAddNote: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            glyph = "↶",
            label = "UNDO LAST",
            enabled = canUndo,
            onClick = onUndo,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            glyph = "✎",
            label = "ADD NOTE",
            enabled = true,
            onClick = onAddNote,
            modifier = Modifier.weight(1f),
        )
        FinishTile(
            arrowCount = arrowCount,
            onClick = onFinish,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionTile(
    glyph: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelTone = if (enabled) AppInk else AppInk3
    Column(
        modifier = modifier
            .background(AppPaper2)
            .border(1.dp, AppLine)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = glyph,
            style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = labelTone),
        )
        Text(
            text = label,
            style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = labelTone,
            ),
        )
    }
}

@Composable
private fun FinishTile(
    arrowCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // iOS finishEndBar disables when currentEndArrows is empty and swaps
    // the subtitle to "PLOT ARROWS TO CONTINUE" (SessionView.swift:1097-1126).
    // Android keeps a single-end model so the title stays "✓ Finish" rather
    // than iOS's per-end "Finish End N", but the enabled-state + subtitle
    // mirror iOS's hint exactly.
    val active = arrowCount > 0
    val bg = if (active) AppPondDk else AppPaper2
    val border = if (active) AppPondDk else AppLine
    val title = if (active) AppPaper else AppInk3
    val subtitle = if (active) AppPaper.copy(alpha = 0.72f) else AppInk3
    val subtitleText = if (arrowCount == 0) {
        "PLOT ARROWS TO CONTINUE"
    } else {
        "$arrowCount ARROW${if (arrowCount == 1) "" else "S"} LOGGED"
    }
    Column(
        modifier = modifier
            .background(bg)
            .border(1.dp, border)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "✓ Finish",
            style = frauncesDisplay(13.5.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = title),
        )
        Text(
            text = subtitleText,
            style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                letterSpacing = 0.18.em,
                color = subtitle,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// Finish bar — Discard / autosaved stamp
// ---------------------------------------------------------------------------

@Composable
private fun FinishBar(onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onDiscard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "DISCARD",
                style = interUI(9.5.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.20.em,
                    color = AppInk3,
                ),
            )
            Text(
                text = "›",
                style = frauncesDisplay(11.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk3),
            )
        }

        val timestampLabel by produceState(initialValue = autosaveStamp()) {
            while (true) {
                value = autosaveStamp()
                delay(30_000L)
            }
        }
        Text(
            text = timestampLabel,
            style = frauncesDisplay(11.sp, italic = true).copy(color = AppInk3),
        )

        // Right placeholder to keep the autosave line visually centered against
        // the discard link on the left.
        Spacer(Modifier.width(72.dp))
    }
}

private fun autosaveStamp(): String {
    val now = java.time.LocalTime.now()
    return "autosaved · %02d:%02d · cloud ✓".format(now.hour, now.minute)
}

// ---------------------------------------------------------------------------
// Hairline helpers
// ---------------------------------------------------------------------------

private fun Modifier.drawBottomHairline(): Modifier = this.drawBehind {
    val y = size.height
    drawLine(
        color = AppLine,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1f,
    )
}

private fun Modifier.drawTopHairline(): Modifier = this.drawBehind {
    drawLine(
        color = AppLine,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1f,
    )
}
