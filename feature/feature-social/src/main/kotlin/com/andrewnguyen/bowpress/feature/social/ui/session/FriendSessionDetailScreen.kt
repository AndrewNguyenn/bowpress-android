package com.andrewnguyen.bowpress.feature.social.ui.session

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppMaple
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPaper2
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.bp.BPPlottedTarget
import com.andrewnguyen.bowpress.core.designsystem.bp.ScorecardTable
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.Scorecard
import com.andrewnguyen.bowpress.core.model.SharedSession

/**
 * Friend session detail (§16) — drilled into from a tapped feed session row.
 *
 * Renders the friend's session: the target face with their arrows plotted
 * (shared [BPPlottedTarget]), a read-only scorecard (ends × arrows with each
 * end's running total), and a header (handle / name / score / distance /
 * face). When the owner has deleted the underlying session the detail comes
 * back with `session == null` and only the stat summary is shown.
 */
@Composable
fun FriendSessionDetailScreen(
    sharedSessionId: String,
    onBack: () -> Unit,
    viewModel: FriendSessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sharedSessionId) { viewModel.load(sharedSessionId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(TestTags.SocialSessionDetailRoot),
    ) {
        // Top nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "‹  Feed",
                    style = interUI(10.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.32.em),
                    color = AppPondDk,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("Session", style = frauncesDisplay(28.sp), color = AppInk)
                state.detail?.let { d ->
                    Text(
                        "@${d.ownerHandle} · ${d.ownerDisplayName}",
                        style = jetbrainsMono(10.sp),
                        color = AppInk3,
                    )
                }
            }
        }
        HorizontalDivider(color = AppLine, thickness = 1.dp)

        when {
            state.isLoading -> {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Loading…",
                    style = frauncesDisplay(14.sp),
                    color = AppInk3,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            state.error != null -> {
                Spacer(Modifier.height(20.dp))
                Text(
                    state.error.orEmpty(),
                    style = jetbrainsMono(10.sp),
                    color = AppMaple,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

            state.detail != null -> {
                val detail = state.detail!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                    // Header — stat summary always shown (survives a deleted session).
                    item {
                        Spacer(Modifier.height(14.dp))
                        SessionStatHeader(shared = detail.sharedSession)
                    }

                    val shotSession = detail.session
                    if (shotSession == null) {
                        // Owner deleted the underlying session — stat summary only.
                        item { DeletedSessionNotice() }
                    } else {
                        // Target face with the friend's arrows plotted — renders
                        // the real face type + 3-spot layout the friend shot.
                        item {
                            Spacer(Modifier.height(18.dp))
                            SectionEyebrow("SHOT DISTRIBUTION · ${shotSession.targetLayout.label}")
                            Spacer(Modifier.height(10.dp))
                            BPPlottedTarget(
                                arrows = detail.arrows,
                                faceType = shotSession.targetFaceType,
                                layout = shotSession.targetLayout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.SocialSessionTarget),
                            )
                        }

                        // Scorecard — the canonical ruled table, identical to
                        // the session-detail screen (read-only here).
                        item {
                            Spacer(Modifier.height(18.dp))
                            SectionEyebrow("SCORECARD")
                            Spacer(Modifier.height(10.dp))
                            val scorecard = Scorecard.build(
                                detail.arrows, detail.ends, shotSession.id,
                            )
                            if (scorecard.lines.isEmpty()) {
                                Text(
                                    "No arrows recorded for this session.",
                                    style = frauncesDisplay(13.sp, italic = true),
                                    color = AppInk3,
                                )
                            } else {
                                ScorecardTable(scorecard = scorecard)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/** Score / X / arrows / distance / face summary off the shared session. */
@Composable
private fun SessionStatHeader(shared: SharedSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(14.dp),
    ) {
        shared.title?.takeIf { it.isNotBlank() }?.let { title ->
            Text(title, style = frauncesDisplay(16.sp), color = AppInk)
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${shared.score}", style = frauncesDisplay(34.sp), color = AppPondDk)
            Spacer(Modifier.width(8.dp))
            Text(
                "${shared.xCount}X · ${shared.arrowCount} arrows",
                style = jetbrainsMono(10.sp),
                color = AppInk3,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        val context = listOfNotNull(shared.distance, shared.face)
        if (context.isNotEmpty()) {
            Text(
                context.joinToString(" · "),
                style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppMaple,
            )
        }
    }
}

@Composable
private fun DeletedSessionNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .border(1.dp, AppLine)
            .background(AppPaper2)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Session no longer available.", style = frauncesDisplay(15.sp), color = AppInk)
        Spacer(Modifier.height(6.dp))
        Text(
            "The owner deleted this session — only the summary above remains.",
            style = frauncesDisplay(12.sp, italic = true),
            color = AppInk3,
        )
    }
}

@Composable
private fun SectionEyebrow(label: String) {
    Text(
        label,
        style = interUI(9.sp, FontWeight.SemiBold).copy(letterSpacing = 0.24.em),
        color = AppInk3,
    )
}
