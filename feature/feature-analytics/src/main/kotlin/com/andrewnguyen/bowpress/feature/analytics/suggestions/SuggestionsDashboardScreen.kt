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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.feature.analytics.ui.AnalyticsCard
import com.andrewnguyen.bowpress.feature.analytics.ui.ConfidenceBadge
import java.time.Instant

/** Stable test tags exposed for Compose UI tests. */
object SuggestionsDashboardTestTags {
    const val Root: String = "suggestions_dashboard_root"
    const val UnreadBadge: String = "suggestions_dashboard_unread_badge"
    const val SuggestionCard: String = "suggestions_dashboard_suggestion_card"
    const val EmptyState: String = "suggestions_dashboard_empty_state"
}

/**
 * Dashboard tab (home). Groups all undismissed suggestions by bow, surfaces
 * unread-first with an unread-count badge in the header, and supports
 * pull-to-refresh that hits `SuggestionRepository.refreshForBow` for each bow.
 *
 * Android port of iOS `DashboardView` / `DashboardViewModel`.
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("BowPress", fontWeight = FontWeight.SemiBold)
                        if (state.unreadCount > 0) {
                            Spacer(Modifier.width(10.dp))
                            BadgedBox(
                                modifier = Modifier.testTag(SuggestionsDashboardTestTags.UnreadBadge),
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Text(state.unreadCount.toString())
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Unread suggestions",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(SuggestionsDashboardTestTags.Root),
        ) {
            when {
                state.isLoading && state.groups.isEmpty() -> LoadingState()
                state.groups.isEmpty() -> EmptyState()
                else -> SuggestionList(
                    state = state,
                    onOpenSuggestion = onOpenSuggestion,
                )
            }
        }
    }
}

// ---------- Sub-components ----------

@Composable
private fun SuggestionList(
    state: SuggestionsDashboardUiState,
    onOpenSuggestion: (bowId: String, suggestionId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SummaryBanner(unreadCount = state.unreadCount) }

        state.groups.forEach { group ->
            item(key = "hdr-${group.bowId}") {
                Text(
                    text = group.bowName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(
                items = group.suggestions,
                key = { it.id },
            ) { suggestion ->
                SuggestionRowCard(
                    suggestion = suggestion,
                    onClick = { onOpenSuggestion(suggestion.bowId, suggestion.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryBanner(unreadCount: Int) {
    if (unreadCount == 0) {
        BannerRow(
            icon = Icons.Filled.CheckCircle,
            title = "All caught up",
            subtitle = "No new insights at the moment.",
            tint = Color(0xFF2E7D32),
        )
    } else {
        val title = if (unreadCount == 1) "1 new insight" else "$unreadCount new insights"
        BannerRow(
            icon = Icons.Filled.AutoAwesome,
            title = title,
            subtitle = "Tap a card to review and apply.",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BannerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SuggestionRowCard(
    suggestion: AnalyticsSuggestion,
    onClick: () -> Unit,
) {
    AnalyticsCard(
        modifier = Modifier
            .clickable { onClick() }
            .testTag(SuggestionsDashboardTestTags.SuggestionCard),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = suggestion.parameter,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (suggestion.wasRead) FontWeight.Medium else FontWeight.SemiBold,
                    color = if (suggestion.wasRead) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
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
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SuggestionsDashboardTestTags.EmptyState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier
                .width(72.dp)
                .height(72.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No suggestions yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Keep logging sessions and we'll surface insights here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
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
