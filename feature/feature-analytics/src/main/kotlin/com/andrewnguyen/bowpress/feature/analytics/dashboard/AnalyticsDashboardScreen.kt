package com.andrewnguyen.bowpress.feature.analytics.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.AnalyticsOverview
import com.andrewnguyen.bowpress.core.model.AnalyticsPeriod
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.PeriodComparison
import com.andrewnguyen.bowpress.core.model.PeriodSlice
import com.andrewnguyen.bowpress.feature.analytics.ui.AnalyticsCard
import com.andrewnguyen.bowpress.feature.analytics.ui.ConfidenceBadge
import com.andrewnguyen.bowpress.feature.analytics.ui.DeltaRow
import com.andrewnguyen.bowpress.feature.analytics.ui.SectionHeader
import com.andrewnguyen.bowpress.feature.analytics.ui.StatPill
import com.andrewnguyen.bowpress.feature.analytics.ui.formatPercent
import com.andrewnguyen.bowpress.feature.analytics.ui.formatScore
import java.time.Instant

/** Stable test tags the Compose UI test depends on. */
object AnalyticsDashboardTestTags {
    const val DashboardRoot: String = "analytics_dashboard_root"
    const val SuggestionsSection: String = "analytics_dashboard_suggestions"
    const val SuggestionCard: String = "analytics_dashboard_suggestion_card"
}

/**
 * Dashboard screen entry point. Resolves the Hilt view-model and drives the state-first
 * composable. Callers pass navigation lambdas so this screen never touches NavController.
 */
@Composable
fun AnalyticsDashboardScreen(
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTimeline: (bowId: String) -> Unit,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsDashboardContent(
        state = state,
        onPeriodChange = viewModel::selectPeriod,
        onRetry = viewModel::refresh,
        onOpenSuggestion = onOpenSuggestion,
        onOpenHistory = onOpenHistory,
        onOpenTimeline = onOpenTimeline,
    )
}

@Composable
internal fun AnalyticsDashboardContent(
    state: DashboardUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onRetry: () -> Unit,
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTimeline: (bowId: String) -> Unit,
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(AnalyticsDashboardTestTags.DashboardRoot),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PeriodPickerRow(selected = state.period, onSelect = onPeriodChange) }

            if (state.isLoading && state.overview == null) {
                item { LoadingBlock() }
            }

            state.error?.let { message ->
                item { ErrorBanner(message = message, onRetry = onRetry) }
            }

            state.overview?.let { overview ->
                item { OverviewCard(overview = overview) }
            }

            state.comparison?.let { comparison ->
                item { ComparisonCard(comparison = comparison) }
            }

            item { HistoryEntryCard(onOpenHistory = onOpenHistory) }

            item {
                SectionHeader(
                    title = "Suggestions",
                    modifier = Modifier.testTag(AnalyticsDashboardTestTags.SuggestionsSection),
                )
            }

            if (state.topSuggestions.isEmpty()) {
                item {
                    AnalyticsCard {
                        Text(
                            text = "No suggestions yet. Log more sessions to unlock tuning tips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                items(
                    items = state.topSuggestions,
                    key = { it.id },
                ) { suggestion ->
                    SuggestionRowCard(
                        suggestion = suggestion,
                        onClick = { onOpenSuggestion(suggestion.bowId, suggestion.id) },
                        onOpenTimeline = { onOpenTimeline(suggestion.bowId) },
                    )
                }
            }
        }
    }
}

// ---------- Sub-components ----------

@Composable
private fun PeriodPickerRow(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
) {
    // 3-day is a preview-only option on iOS; spec lists 7d/14d/30d/90d/180d/365d.
    val options = listOf(
        AnalyticsPeriod.WEEK,
        AnalyticsPeriod.TWO_WEEKS,
        AnalyticsPeriod.MONTH,
        AnalyticsPeriod.THREE_MONTHS,
        AnalyticsPeriod.SIX_MONTHS,
        AnalyticsPeriod.YEAR,
    )
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun OverviewCard(overview: AnalyticsOverview) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Overview — ${overview.period.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    label = "Sessions",
                    value = overview.sessionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Avg score",
                    value = formatScore(overview.avgArrowScore),
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "X%",
                    value = formatPercent(overview.xPercentage),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(comparison: PeriodComparison) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${comparison.current.label} vs ${comparison.previous.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            DeltaRow(
                label = "Avg score",
                current = comparison.current.avgArrowScore,
                previous = comparison.previous.avgArrowScore,
            )
            DeltaRow(
                label = "X%",
                current = comparison.current.xPercentage,
                previous = comparison.previous.xPercentage,
            )
            DeltaRow(
                label = "Sessions",
                current = comparison.current.sessionCount.toDouble(),
                previous = comparison.previous.sessionCount.toDouble(),
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(onOpenHistory: () -> Unit) {
    AnalyticsCard(
        modifier = Modifier.clickable { onOpenHistory() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Session log", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Chronological history grouped by month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SuggestionRowCard(
    suggestion: AnalyticsSuggestion,
    onClick: () -> Unit,
    onOpenTimeline: () -> Unit,
) {
    AnalyticsCard(
        modifier = Modifier
            .clickable { onClick() }
            .testTag(AnalyticsDashboardTestTags.SuggestionCard),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = suggestion.parameter,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ConfidenceBadge(confidence = suggestion.confidence)
            }
            Text(
                text = "${suggestion.currentValue}  →  ${suggestion.suggestedValue}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = suggestion.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 3,
            )
            Row {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "View timeline",
                    modifier = Modifier
                        .clickable { onOpenTimeline() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    val container = MaterialTheme.colorScheme.errorContainer
    val onContainer = MaterialTheme.colorScheme.onErrorContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Failed to load analytics",
            style = MaterialTheme.typography.titleSmall,
            color = onContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = onContainer.copy(alpha = 0.9f),
        )
        Text(
            text = "Retry",
            modifier = Modifier
                .clickable { onRetry() }
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, name = "Dashboard — loaded")
@Composable
private fun DashboardPreview_Loaded() {
    BowPressTheme {
        AnalyticsDashboardContent(
            state = previewLoadedState,
            onPeriodChange = {},
            onRetry = {},
            onOpenSuggestion = { _, _ -> },
            onOpenHistory = {},
            onOpenTimeline = {},
        )
    }
}

@Preview(showBackground = true, name = "Dashboard — empty")
@Composable
private fun DashboardPreview_Empty() {
    BowPressTheme {
        AnalyticsDashboardContent(
            state = DashboardUiState(isLoading = false),
            onPeriodChange = {},
            onRetry = {},
            onOpenSuggestion = { _, _ -> },
            onOpenHistory = {},
            onOpenTimeline = {},
        )
    }
}

internal val previewOverview: AnalyticsOverview = AnalyticsOverview(
    period = AnalyticsPeriod.WEEK,
    sessionCount = 5,
    avgArrowScore = 9.6,
    xPercentage = 28.0,
)

internal val previewComparison: PeriodComparison = PeriodComparison(
    period = AnalyticsPeriod.WEEK,
    current = PeriodSlice(
        label = "Last 1 Week",
        avgArrowScore = 9.6,
        xPercentage = 28.0,
        sessionCount = 5,
    ),
    previous = PeriodSlice(
        label = "Previous 1 Week",
        avgArrowScore = 9.2,
        xPercentage = 22.0,
        sessionCount = 4,
    ),
)

internal val previewSuggestions: List<AnalyticsSuggestion> = listOf(
    AnalyticsSuggestion(
        id = "s1",
        bowId = "b1",
        createdAt = Instant.now(),
        parameter = "restVertical",
        suggestedValue = "+3/16\"",
        currentValue = "+2/16\"",
        reasoning = "Vertical impact bias detected across last 3 sessions.",
        confidence = 0.82,
        qualifier = null,
        wasRead = false,
        wasDismissed = false,
        deliveryType = DeliveryType.PUSH,
        evidence = null,
    ),
    AnalyticsSuggestion(
        id = "s2",
        bowId = "b1",
        createdAt = Instant.now(),
        parameter = "peepHeight",
        suggestedValue = "9.5\"",
        currentValue = "9.25\"",
        reasoning = "Anchor inconsistency mitigated at new height.",
        confidence = 0.71,
        qualifier = null,
        wasRead = false,
        wasDismissed = false,
        deliveryType = DeliveryType.IN_APP,
        evidence = null,
    ),
    AnalyticsSuggestion(
        id = "s3",
        bowId = "b1",
        createdAt = Instant.now(),
        parameter = "tillerTop",
        suggestedValue = "-0.25",
        currentValue = "0.0",
        reasoning = "Limb balance drifting.",
        confidence = 0.58,
        qualifier = null,
        wasRead = true,
        wasDismissed = false,
        deliveryType = DeliveryType.IN_APP,
        evidence = null,
    ),
)

private val previewLoadedState = DashboardUiState(
    period = AnalyticsPeriod.WEEK,
    isLoading = false,
    overview = previewOverview,
    comparison = previewComparison,
    topSuggestions = previewSuggestions,
)
