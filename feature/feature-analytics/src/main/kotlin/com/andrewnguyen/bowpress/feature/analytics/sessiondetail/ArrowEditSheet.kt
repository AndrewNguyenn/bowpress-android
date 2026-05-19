package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPine
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import kotlinx.coroutines.launch

/**
 * Per-arrow edit sheet. Mirrors iOS `ArrowEditSheet` (SessionView.swift:1480):
 *
 *   ARROW #N
 *   Currently scored — N (or X)
 *   [quick-score keypad: 11/10/9/8/7/.../M]
 *   [delete arrow]
 *
 * Differences from iOS:
 *   - Tap-to-replot via target plot is deferred to task #28 (Pen magnifier).
 *     Quick-score keypad is sufficient for re-scoring an arrow; positional
 *     replot lands with the pen magnifier port.
 *   - Re-score keeps the existing plotX/plotY; only the ring/zone change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrowEditSheet(
    arrow: ArrowPlot,
    arrowNumber: Int,
    faceType: TargetFaceType,
    onReplotRing: (ring: Int, zone: Zone) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var pendingDeleteConfirm by remember { mutableStateOf(false) }

    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "ARROW #$arrowNumber",
                style = interUI(11.sp, weight = FontWeight.SemiBold).copy(
                    letterSpacing = 0.22.em,
                    color = AppInk3,
                ),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Currently scored",
                    style = frauncesDisplay(21.sp, italic = true).copy(color = AppInk2),
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = ringLabel(arrow.ring),
                    style = frauncesDisplay(32.sp, italic = true, weight = FontWeight.Medium).copy(
                        color = if (arrow.ring == 11) AppPine else AppInk,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap a score to change it. Positional re-plot lands with the pen magnifier.",
                style = frauncesDisplay(14.sp, italic = true).copy(color = AppInk3),
            )

            Spacer(Modifier.height(16.dp))

            QuickScoreKeypad(
                currentRing = arrow.ring,
                faceType = faceType,
                onPick = { newRing ->
                    val newZone = zoneForRing(newRing, arrow.zone)
                    onReplotRing(newRing, newZone)
                    close()
                },
            )

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = { pendingDeleteConfirm = true },
                    modifier = Modifier.testTag("arrow_edit_delete"),
                ) {
                    Text(
                        text = "Delete arrow",
                        color = MaterialTheme.colorScheme.error,
                        style = frauncesDisplay(15.sp, italic = true),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (pendingDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { pendingDeleteConfirm = false },
            title = { Text("Delete this arrow?") },
            text = { Text("Arrow #$arrowNumber will be removed from this session.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    pendingDeleteConfirm = false
                    close()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

/** Score ladder mirrors iOS `quickScoreOptions`: 11 (X), 10..lowest, then M. */
@Composable
private fun QuickScoreKeypad(
    currentRing: Int,
    faceType: TargetFaceType,
    onPick: (Int) -> Unit,
) {
    val lowest = when (faceType) {
        TargetFaceType.SIX_RING -> 6
        TargetFaceType.TEN_RING -> 1
    }
    val ladder = buildList {
        add(11)
        for (r in 10 downTo lowest) add(r)
        add(0) // miss
    }
    val cellPad = 14.dp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ladder.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { ring ->
                    val isCurrent = ring == currentRing
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (isCurrent) AppPondDk else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) AppPondDk else AppLine,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { onPick(ring) }
                            .padding(cellPad),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ringLabel(ring),
                            style = frauncesDisplay(
                                16.sp,
                                italic = true,
                                weight = FontWeight.Medium,
                            ).copy(
                                color = if (isCurrent) AppInk2.copy(alpha = 0f).let { _ -> AppLine } else AppInk,
                            ),
                        )
                    }
                }
                // Fill empty slots in the last row so the grid stays even.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun ringLabel(ring: Int): String = when (ring) {
    11 -> "X"
    0 -> "M"
    else -> ring.toString()
}

/**
 * Zone classification: iOS replotArrow keeps the existing zone when only the
 * ring changes via the keypad (the zone tracks positional cardinal/middle/
 * miss-pattern, not the score), so we mirror that here.
 */
private fun zoneForRing(ring: Int, currentZone: Zone): Zone = currentZone

@Composable
internal fun pondColor(): androidx.compose.ui.graphics.Color = AppPond
