package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPrimaryButton
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.feature.session.FinishMode
import com.andrewnguyen.bowpress.feature.session.FinishSheet

/**
 * Course sign-off review — the walked map as the hero, the round's totals,
 * and a clean-round stamp, shown before the course is committed to the Log.
 * Mirrors iOS `ThreeDFinishView`.
 */
@Composable
fun ThreeDFinishReview(
    state: ThreeDCourseUiState,
    breadcrumb: List<GeoPoint>,
    onSign: () -> Unit,
    onBack: () -> Unit,
    /**
     * Optional extras-driven sign-off — when non-null, tapping "Sign &
     * return" opens the C1 finish sheet (title + description + range +
     * photos + audience) instead of the legacy [onSign] fire-and-forget.
     * Both callbacks are wired so a caller can pass null to keep the
     * pre-C1 surface (tests, previews).
     */
    onSignWithExtras: ((com.andrewnguyen.bowpress.feature.session.FinishExtras) -> Unit)? = null,
) {
    // C1 — when the caller wires `onSignWithExtras`, route the Sign tap
    // through the FinishSheet so the archer can caption + audience-toggle
    // the course before it lands in Log + Feed.
    var showFinishSheet by remember { mutableStateOf(false) }
    if (showFinishSheet && onSignWithExtras != null) {
        val mode = FinishMode.Course(
            totalScore = state.totalScore,
            killCount = state.killCount,
            stationCount = state.stations.size,
            averagePerTarget = state.averageScore,
        )
        FinishSheet(
            mode = mode,
            bowName = state.session?.let { "Bow" } ?: "—",
            arrowSummary = null,
            isPosting = false,
            initialTitle = state.session?.title.orEmpty(),
            initialDescription = state.session?.notes.orEmpty(),
            initialLocation = null,
            onFinish = { extras ->
                showFinishSheet = false
                onSignWithExtras(extras)
            },
            onDiscard = {
                showFinishSheet = false
                onBack()
            },
            onClose = { showFinishSheet = false },
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppPaper)
            .verticalScroll(rememberScrollState()),
    ) {
        BPNavHeader(
            eyebrow = "BOWPRESS · SIGN OFF",
            title = "Course complete",
            meta = {
                Text(
                    "${state.stations.size} STATIONS",
                    style = jetbrainsMono(10.sp, FontWeight.Medium).copy(color = AppInk),
                )
            },
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CourseInkMapView(
                stations = state.stations,
                breadcrumb = breadcrumb,
                elevationGrid = state.elevationGrid,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinishStat("TOTAL", "${state.totalScore}", Modifier.weight(1f))
                FinishStat("AVERAGE", "%.1f".format(state.averageScore), Modifier.weight(1f))
                FinishStat("KILLS", "${state.killCount}", Modifier.weight(1f))
            }
            if (state.isCleanRound) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(AppPondDk)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "● CLEAN ROUND — NEW BEST FORM",
                        style = jetbrainsMono(10.sp, FontWeight.Medium).copy(color = AppPaper),
                    )
                }
            }
            BPPrimaryButton(
                title = "Sign & return to Log",
                subtitle = "COURSE FINISHED",
                onClick = {
                    if (onSignWithExtras != null) {
                        showFinishSheet = true
                    } else {
                        onSign()
                    }
                },
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppLine)
                    .clickable(onClick = onBack)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Keep walking",
                    style = frauncesDisplay(13.sp, italic = true).copy(color = AppInk3),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinishStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.border(1.dp, AppLine).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = jetbrainsMono(8.5.sp).copy(color = AppInk3))
        Text(
            value,
            style = frauncesDisplay(26.sp, italic = true, weight = FontWeight.Medium).copy(color = AppInk),
        )
    }
}
