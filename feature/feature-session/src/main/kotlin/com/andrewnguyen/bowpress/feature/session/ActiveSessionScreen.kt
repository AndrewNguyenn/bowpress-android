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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.andrewnguyen.bowpress.core.model.TargetLayout
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
    onSessionEnded: (wasShared: Boolean) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showConfigSheet by remember { mutableStateOf(false) }
    var showEndSheet by remember { mutableStateOf(false) }
    // The completed end whose actions dialog is open (add a missed arrow /
    // delete the end), keyed by end id. Mirrors iOS endActionsTarget.
    var endActionsTarget by remember { mutableStateOf<String?>(null) }
    // The id of the arrow whose ArrowEditSheet is open. Tapping a shot cell
    // in the live scorecard previously deleted the arrow with no
    // confirmation — this opens the per-arrow re-score / delete sheet
    // instead, matching iOS `editingArrowId` (SessionView.swift).
    var editingArrowId by remember { mutableStateOf<String?>(null) }

    // Track whether we've ever seen the session as active so a transient null during
    // initial hydration doesn't bounce the user back to the home screen.
    var sawActive by remember { mutableStateOf(false) }
    if (state.activeSession != null) sawActive = true
    val justEnded = sawActive && state.activeSession == null
    // Single nav-out path. Reads the audience pick the view model wrote on
    // finish (`SessionUiState.lastFinishWasShared`) so the host can route
    // Public finishes to the Social feed. Discards / legacy nil-extras
    // finishes / external null transitions all leave the field at its
    // default false and fall through to the standard Log path.
    LaunchedEffect(justEnded) {
        if (justEnded) onSessionEnded(state.lastFinishWasShared)
    }
    val active = state.activeSession ?: return

    // The Pen-magnifier lens snapshot is hoisted to the screen root so the
    // lens overlay can extend up into the header space above the target —
    // hosting it inside the target-sized box would force every above-the-
    // finger lens to clip + flip below. Mirrors iOS, where the lens lives at
    // the screen root in global coords.
    var lensSnapshot by remember { mutableStateOf<PenLensSnapshot?>(null) }

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
                onLensSnapshotChanged = { lensSnapshot = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            val breakdown = state.endsBreakdown

            // Undo + Finish-End bar directly under the target — closes the
            // in-progress end and starts the next. Mirrors iOS undoEndButton
            // + finishEndBar (SessionView.swift).
            EndControlsRow(
                currentEndNumber = breakdown.currentEndNumber,
                inProgressCount = breakdown.inProgressArrows.size,
                isLoading = state.isLoading,
                onUndo = { scope.launch { viewModel.removeLastArrow() } },
                onFinishEnd = { scope.launch { viewModel.completeEnd() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            // In-progress end arrows — quick live feedback for the end being
            // plotted right now (not the whole session). Strip is read-only;
            // a mis-tapped arrow in the current end is fixed via UNDO LAST,
            // not the per-arrow edit sheet (which is wired through the
            // completed-ends scorecard below). Mirrors iOS.
            if (breakdown.inProgressArrows.isNotEmpty()) {
                RecentArrowsStrip(
                    arrows = breakdown.inProgressArrows,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            // Running ends-history scorecard — every completed end. Mirrors
            // iOS endsHistory. Tapping an end opens its actions; tapping a
            // shot cell opens the per-arrow edit sheet (re-score / delete).
            if (breakdown.hasCompletedEnds) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BPEyebrow("ENDS")
                    Spacer(Modifier.height(10.dp))
                    EndsScorecard(
                        breakdown = breakdown,
                        onTapEnd = { endId -> endActionsTarget = endId },
                        onTapArrow = { arrow -> editingArrowId = arrow.id },
                    )
                }
            }

            RunningTotals(
                arrows = state.currentArrows,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .drawBottomHairline(),
            )

            ActionsRow(
                canUndo = breakdown.inProgressArrows.isNotEmpty(),
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

        // Pen magnifier — hosted at the screen root so it can float up into
        // the header space above the target. Renders nothing while not
        // dragging. Snapshot coords are root-relative (see PenLensSnapshot).
        PenLensOverlay(snapshot = lensSnapshot, modifier = Modifier.fillMaxSize())
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
        val arrows = state.currentArrows.filterNot { it.excluded }
        val score = arrows.sumOf { it.ring.coerceAtMost(10) }
        val xCount = arrows.count { it.ring == 11 }
        val mode = FinishMode.Range(
            score = score,
            xCount = xCount,
            arrowCount = arrows.size,
            endCount = state.completedEnds.size + if (state.currentEndArrows.isNotEmpty()) 1 else 0,
        )
        FinishSheet(
            mode = mode,
            bowName = state.selectedBow?.name ?: "—",
            arrowSummary = state.activeArrowConfig?.label,
            isPosting = state.isLoading,
            initialTitle = active.title.orEmpty(),
            initialDescription = active.notes,
            initialLocation = null,
            onFinish = { extras ->
                scope.launch {
                    viewModel.endSession(extras = extras)
                    showEndSheet = false
                    // Navigation is handled by the `justEnded` LaunchedEffect
                    // above — single observer pattern. SessionViewModel
                    // latches `lastFinishWasShared` before its first suspend
                    // so the LaunchedEffect sees the right audience by the
                    // time `activeSession` becomes null.
                }
            },
            onDiscard = {
                scope.launch {
                    viewModel.discardActiveSession()
                    showEndSheet = false
                    // Same single-path note as onFinish.
                }
            },
            onClose = { showEndSheet = false },
        )
    }

    // End-actions dialog — delete a completed end. Mirrors iOS's
    // confirmationDialog for an end (SessionView.swift). iOS additionally
    // offers "Add an arrow" via AddArrowSheet, which Android doesn't have
    // yet — on Android, a mis-tapped arrow in a completed end is fixed by
    // tapping the shot cell to open the ArrowEditSheet (re-score / delete).
    endActionsTarget?.let { endId ->
        val end = state.completedEnds.firstOrNull { it.id == endId }
        if (end == null) {
            endActionsTarget = null
        } else {
            EndActionsDialog(
                endNumber = end.endNumber,
                onDelete = {
                    viewModel.deleteEnd(endId)
                    endActionsTarget = null
                },
                onDismiss = { endActionsTarget = null },
            )
        }
    }

    // Per-arrow edit sheet — re-score via keypad or delete with
    // confirmation. Mirrors iOS ArrowEditSheet on the live session
    // (SessionView.swift). Arrow number is the global 1-based
    // chronological index across the whole session so the caption stays
    // stable regardless of how ends group.
    // currentArrows is already ORDER BY shotAt ASC (ArrowPlotDao) so the
    // mapIndexed index is the global 1-based chronological number.
    val arrowNumbers = remember(state.currentArrows) {
        state.currentArrows.mapIndexed { i, p -> p.id to (i + 1) }.toMap()
    }
    editingArrowId?.let { id ->
        val arrow = state.currentArrows.firstOrNull { it.id == id }
        val number = arrowNumbers[id]
        if (arrow == null || number == null) {
            editingArrowId = null
        } else {
            ArrowEditSheet(
                arrow = arrow,
                arrowNumber = number,
                faceType = state.targetFaceType,
                distance = active.distance,
                onReplotRing = { ring ->
                    // Keypad re-score: VM decides whether to keep the
                    // existing position (in-band) or snap to the new
                    // ring's midline (out-of-band), and recomputes the
                    // zone from the final coordinates either way.
                    scope.launch { viewModel.replotArrow(id, ring, null, null) }
                },
                onDelete = { scope.launch { viewModel.deleteArrow(id) } },
                onDismiss = { editingArrowId = null },
            )
        }
    }
}

/**
 * Confirmation dialog for a completed end — currently just "delete end".
 * Mirrors the destructive branch of iOS's end confirmationDialog.
 */
@Composable
private fun EndActionsDialog(
    endNumber: Int,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End $endNumber") },
        text = { Text("Delete this end and its arrows? The arrows won't appear in the session log.") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete end", color = AppMaple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Undo + Finish-End controls under the target. The Finish-End button closes
 * the in-progress end (variable length — the archer decides) and starts the
 * next; disabled until the current end has at least one arrow. Mirrors iOS
 * undoEndButton + finishEndBar.
 */
@Composable
private fun EndControlsRow(
    currentEndNumber: Int,
    inProgressCount: Int,
    isLoading: Boolean,
    onUndo: () -> Unit,
    onFinishEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canAct = inProgressCount > 0 && !isLoading
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Undo — drops the last arrow of the in-progress end.
        Column(
            modifier = Modifier
                .width(76.dp)
                .background(AppPaper2)
                .border(1.dp, AppLine)
                .clickable(enabled = canAct, onClick = onUndo)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "↶",
                style = frauncesDisplay(21.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = if (canAct) AppInk else AppInk3),
            )
            Text(
                text = "UNDO",
                style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.18.em,
                    color = if (canAct) AppInk else AppInk3,
                ),
            )
        }
        // Finish End N — closes the end, clears the target for the next.
        Column(
            modifier = Modifier
                .weight(1f)
                .background(if (canAct) AppPondDk else AppPaper2)
                .border(1.dp, if (canAct) AppPondDk else AppLine)
                .clickable(enabled = canAct, onClick = onFinishEnd)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Finish End $currentEndNumber",
                style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = if (canAct) AppPaper else AppInk3),
            )
            Text(
                text = if (inProgressCount == 0) {
                    "PLOT ARROWS TO CONTINUE"
                } else {
                    "$inProgressCount ARROW${if (inProgressCount == 1) "" else "S"} · NEXT END"
                },
                style = interUI(10.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.18.em,
                    color = if (canAct) AppPaper.copy(alpha = 0.72f) else AppInk3,
                ),
            )
        }
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
    onLensSnapshotChanged: (PenLensSnapshot?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val faceType = state.targetFaceType
    val targetLayout = state.targetLayout
    val arrowDiameterMm = state.activeArrowConfig?.shaftDiameter ?: 5.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            CrumbOverlay(faceType = faceType, layout = targetLayout)
        }

        Spacer(Modifier.height(4.dp))

        // Sight mark chip — surfaces the relevant mark for the active bow at
        // the session's target distance. Renders nothing if there's no
        // useful reading. Mirrors iOS SightMarkChip.
        SightMarkChip(
            bowId = state.selectedBow?.id,
            distance = state.activeSession?.distance,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        // The shared TargetPlot composable draws the WA face (single) or the
        // 3-spot Vegas card (multi-spot), captures the drag-to-plot gesture,
        // and emits the root-coord Pen-lens snapshot. The lens itself is
        // hosted at the screen root by ActiveSessionScreen.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TargetPlot(
                // Only the in-progress end's arrows — the live target clears
                // when an end finishes so it doesn't get busy over many ends.
                // Mirrors iOS `TargetPlotView(arrows: currentEndArrows)`.
                arrows = state.currentEndArrows,
                onArrowPlotted = onPlot,
                modifier = Modifier.size(TARGET_FACE_SIZE),
                isEnabled = !state.isLoading,
                arrowDiameterMm = arrowDiameterMm,
                faceType = faceType,
                targetLayout = targetLayout,
                // §B3 — session distance drives the sixRing variant
                // (Vegas at 20yd, Outdoor80 at 50/70m). tenRing is
                // distance-invariant; the geometry layer no-ops it.
                distance = state.activeSession?.distance,
                onLensSnapshotChanged = onLensSnapshotChanged,
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
private fun CrumbOverlay(faceType: TargetFaceType, layout: TargetLayout) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        BPEyebrow("FACE")
        val crumb = if (layout.isMultiSpot) {
            "${layout.label} · Vegas"
        } else {
            "${faceName(faceType)} · ${faceSize(faceType)}"
        }
        Text(
            text = crumb,
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

// Snapshot building lives in PenLensOverlay.kt — shared with TargetPlot.

// ---------------------------------------------------------------------------
// Recent arrows strip — one cell per arrow plotted in the current end
// (dynamic; no placeholder slots), with the avg of the last few
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

        // One fixed-width cell per arrow actually plotted in this end — no
        // empty placeholder slots, so a 3-arrow end shows 3 cells. The last
        // 6 are shown so a long end can't overflow the row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shown = arrows.takeLast(6)
            val baseNumber = arrows.size - shown.size
            shown.forEachIndexed { i, arrow ->
                RecentCell(
                    arrow = arrow,
                    arrowNumber = baseNumber + i + 1,
                    modifier = Modifier.width(52.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentCell(
    arrow: ArrowPlot,
    arrowNumber: Int,
    modifier: Modifier = Modifier,
) {
    val isX = arrow.ring == 11
    val border = if (isX) AppPondDk else AppLine
    val valueColor = if (isX) AppPondDk else AppInk
    Column(
        modifier = modifier
            .background(AppPaper)
            .border(1.dp, border)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ringLabel(arrow.ring),
            style = frauncesDisplay(20.sp, italic = true, weight = FontWeight.Medium)
                .copy(color = valueColor),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "#$arrowNumber",
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
