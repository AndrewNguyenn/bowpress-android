package com.andrewnguyen.bowpress.feature.analytics.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk2
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.designsystem.bp.BPEyebrow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPLedgerRow
import com.andrewnguyen.bowpress.core.designsystem.bp.BPNavHeader
import com.andrewnguyen.bowpress.core.designsystem.bp.BPSectionTitle
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStamp
import com.andrewnguyen.bowpress.core.designsystem.bp.BPStampTone
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.jetbrainsMono
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.SuggestionStatusStamp
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

/** Stable test tags exposed for Compose UI tests. */
object SuggestionsDashboardTestTags {
    const val Root: String = "suggestions_dashboard_root"
    const val UnreadBadge: String = "suggestions_dashboard_unread_badge"
    const val SuggestionCard: String = "suggestions_dashboard_suggestion_card"
    const val EmptyState: String = "suggestions_dashboard_empty_state"
}

/**
 * Dashboard tab (home). Groups all undismissed suggestions by bow, surfaces
 * unread-first with an unread-count stamp in the header, and supports
 * pull-to-refresh that hits `SuggestionRepository.refreshForBow` for each bow.
 *
 * Kenrokuen port — BPNavHeader + BPLedgerRow ledger layout, no Material3
 * surfaces or chips.
 */
@Composable
fun SuggestionsDashboardScreen(
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
    viewModel: SuggestionsDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SuggestionsDashboardContent(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenSuggestion = { bowId, suggestionId ->
            viewModel.markRead(suggestionId)
            onOpenSuggestion(bowId, suggestionId)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SuggestionsDashboardContent(
    state: SuggestionsDashboardUiState,
    onRefresh: () -> Unit,
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPaper)
            .testTag(SuggestionsDashboardTestTags.Root),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    BPNavHeader(
                        title = "Insights",
                        meta = {
                            HeaderMeta(
                                unreadCount = state.unreadCount,
                                totalSuggestions = state.groups.sumOf { it.suggestions.size },
                                bowCount = state.groups.size,
                            )
                        },
                    )
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        SummaryBanner(unreadCount = state.unreadCount)
                    }
                }

                when {
                    state.isLoading && state.groups.isEmpty() -> item {
                        Box(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Loading insights…",
                                style = interUI(11.sp).copy(color = AppInk3),
                            )
                        }
                    }
                    state.groups.isEmpty() -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 32.dp)
                                .testTag(SuggestionsDashboardTestTags.EmptyState),
                        ) {
                            EmptyState()
                        }
                    }
                    else -> state.groups.forEach { group ->
                        item(key = "group-header-${group.bowId}") {
                            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                                BPSectionTitle(
                                    title = group.bowName,
                                    aside = if (group.unreadCount > 0) "${group.unreadCount} unread" else null,
                                )
                            }
                        }
                        val ordered = group.suggestions
                        ordered.forEachIndexed { idx, suggestion ->
                            item(key = "sug-${suggestion.id}") {
                                Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                    SuggestionLedgerRow(
                                        suggestion = suggestion,
                                        index = idx + 1,
                                        onClick = { onOpenSuggestion(suggestion.bowId, suggestion.id) },
                                    )
                                }
                                if (idx < ordered.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp)
                                            .height(1.dp)
                                            .background(AppLine2),
                                    )
                                }
                            }
                        }
                        item(key = "group-spacer-${group.bowId}") {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(AppLine),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Header meta ----------

@Composable
private fun HeaderMeta(unreadCount: Int, totalSuggestions: Int, bowCount: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = monoLine("$unreadCount", " unread"),
            style = jetbrainsMono(10.sp),
            modifier = Modifier.testTag(SuggestionsDashboardTestTags.UnreadBadge),
        )
        Text(
            text = monoLine("$totalSuggestions", " ranked"),
            style = jetbrainsMono(10.sp),
        )
        Text(
            text = "across $bowCount bow${if (bowCount == 1) "" else "s"}",
            style = jetbrainsMono(10.sp).copy(color = AppInk3),
        )
    }
}

private fun monoLine(value: String, unit: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = AppInk, fontWeight = FontWeight.Medium)) { append(value) }
    withStyle(SpanStyle(color = AppInk3)) { append(unit) }
}

// ---------- Summary banner ----------

@Composable
private fun SummaryBanner(unreadCount: Int) {
    val title: String
    val subtitle: String
    if (unreadCount == 0) {
        title = "All caught up"
        subtitle = "no new insights at the moment"
    } else {
        title = if (unreadCount == 1) "1 new insight" else "$unreadCount new insights"
        subtitle = "tap a row to review and apply"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.andrewnguyen.bowpress.core.designsystem.AppPaper2)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(38.dp).background(AppPondDk))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = frauncesDisplay(18.sp, italic = true).copy(color = AppInk),
            )
            Text(
                text = subtitle,
                style = interUI(10.5.sp).copy(color = AppInk3),
            )
        }
    }
}

// ---------- Ledger row ----------

@Composable
private fun SuggestionLedgerRow(
    suggestion: AnalyticsSuggestion,
    index: Int,
    onClick: () -> Unit,
) {
    val confPct = (suggestion.confidence * 100).roundToInt()
    val stampText = resolvedStatusStamp(suggestion)
    val stampTone = stampTone(stampText)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(SuggestionsDashboardTestTags.SuggestionCard),
    ) {
        BPLedgerRow(
            index = index,
            title = bowParameterDisplayName(suggestion.parameter),
            detail = resolvedInlineSummary(suggestion),
            monoLine = "$confPct% confidence · " + relativeTime(suggestion.createdAt) +
                if (!suggestion.wasRead) " · new" else "",
            stamp = { BPStamp(text = stampText.uppercase(), tone = stampTone) },
            accessory = { ConfidenceBar(confidence = suggestion.confidence) },
        )
    }
}

@Composable
private fun ConfidenceBar(confidence: Double) {
    val pct = confidence.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(AppLine),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .height(2.dp)
                .background(AppPond),
        )
    }
}

// ---------- Empty ----------

@Composable
private fun EmptyState() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BPEyebrow(text = "No suggestions yet")
        Text(
            text = "Keep logging sessions and we'll surface insights here.",
            style = frauncesDisplay(18.sp, italic = true).copy(color = AppInk),
        )
        Text(
            text = "Typically six sessions form the first picture — tune one parameter, " +
                "retest, and the engine adapts.",
            style = interUI(11.5.sp).copy(color = AppInk2),
        )
    }
}

// ---------- Helpers (local mirrors of AnalyticsDashboardScreen's) ----------

private fun resolvedInlineSummary(suggestion: AnalyticsSuggestion): String {
    val override = suggestion.inlineSummary?.trim().orEmpty()
    if (override.isNotEmpty()) return override
    val cur = suggestion.currentValue.trim()
    val next = suggestion.suggestedValue.trim()
    return when {
        cur.isEmpty() -> next
        next.isEmpty() -> cur
        else -> "$cur → $next"
    }
}

private fun resolvedStatusStamp(suggestion: AnalyticsSuggestion): String {
    suggestion.statusStamp?.let {
        return when (it) {
            SuggestionStatusStamp.NEW -> "New"
            SuggestionStatusStamp.PROPOSED -> "Proposed"
            SuggestionStatusStamp.GOOD -> "Good"
            SuggestionStatusStamp.REVIEW -> "Review"
        }
    }
    return when {
        suggestion.wasApplied -> "Applied"
        suggestion.confidence >= 0.85 -> "Good"
        suggestion.confidence < 0.6 -> "Review"
        else -> "Proposed"
    }
}

private fun stampTone(stamp: String): BPStampTone = when (stamp.lowercase(Locale.US)) {
    "new", "proposed", "applied" -> BPStampTone.Pond
    "good" -> BPStampTone.Pine
    "review", "dismissed" -> BPStampTone.Maple
    else -> BPStampTone.Pond
}

private fun relativeTime(t: Instant): String {
    val now = Instant.now()
    val secs = (now.epochSecond - t.epochSecond).coerceAtLeast(0L)
    return when {
        secs < 60 -> "just now"
        secs < 3600 -> "${secs / 60} min ago"
        secs < 86_400 -> "${secs / 3600} hr ago"
        secs < 86_400 * 30 -> "${secs / 86_400} d ago"
        else -> "${secs / (86_400 * 30)} mo ago"
    }
}

private fun bowParameterDisplayName(raw: String): String {
    val map = mapOf(
        "drawLength" to "Draw Length",
        "letOffPct" to "Let-Off %",
        "peepHeight" to "Peep Height",
        "dLoopLength" to "D-Loop Length",
        "topCableTwists" to "Top Cable Twists",
        "bottomCableTwists" to "Bottom Cable Twists",
        "mainStringTopTwists" to "Main String Top Twists",
        "mainStringBottomTwists" to "Main String Bottom Twists",
        "topLimbTurns" to "Top Limb Turns",
        "bottomLimbTurns" to "Bottom Limb Turns",
        "restVertical" to "Rest Vertical",
        "restHorizontal" to "Rest Horizontal",
        "restDepth" to "Rest Depth",
        "sightPosition" to "Sight Position",
        "gripAngle" to "Grip Angle",
        "nockingHeight" to "Nocking Height",
    )
    map[raw]?.let { return it }
    val sb = StringBuilder()
    raw.forEachIndexed { idx, c ->
        if (idx > 0 && c.isUpperCase()) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().replaceFirstChar { it.uppercase() }
}


// ---------- Previews ----------

@Preview(showBackground = true, name = "Dashboard — with suggestions")
@Composable
private fun DashboardPreview_WithSuggestions() {
    val s1 = AnalyticsSuggestion(
        id = "s1",
        bowId = "b1",
        createdAt = Instant.parse("2026-04-20T10:00:00Z"),
        parameter = "restVertical",
        suggestedValue = "+3/16\"",
        currentValue = "+2/16\"",
        reasoning = "Vertical bias detected.",
        confidence = 0.82,
        qualifier = null,
        wasRead = false,
        wasDismissed = false,
        deliveryType = DeliveryType.PUSH,
        evidence = null,
    )
    val s2 = s1.copy(id = "s2", parameter = "peepHeight", wasRead = true, confidence = 0.65)
    BowPressTheme {
        SuggestionsDashboardContent(
            state = SuggestionsDashboardUiState(
                isLoading = false,
                groups = listOf(
                    SuggestionGroup(
                        bowId = "b1",
                        bowName = "Hoyt RX7",
                        suggestions = listOf(s1, s2),
                    ),
                ),
                unreadCount = 1,
            ),
            onRefresh = {},
            onOpenSuggestion = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Dashboard — empty")
@Composable
private fun DashboardPreview_Empty() {
    BowPressTheme {
        SuggestionsDashboardContent(
            state = SuggestionsDashboardUiState(isLoading = false),
            onRefresh = {},
            onOpenSuggestion = { _, _ -> },
        )
    }
}
