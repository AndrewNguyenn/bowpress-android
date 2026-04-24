package com.andrewnguyen.bowpress.feature.analytics.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressTheme
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.SuggestionEvidence
import com.andrewnguyen.bowpress.feature.analytics.ui.AnalyticsCard
import com.andrewnguyen.bowpress.feature.analytics.ui.ConfidenceBadge
import com.andrewnguyen.bowpress.feature.analytics.ui.StatPill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// TODO(units): suggestion.currentValue / suggestedValue arrive from the server as
// free-form imperial strings ("+3/16\"", "+0.5 turns"). They do not honor the
// client unit toggle yet; revisit once the API emits structured {value, unit} payloads.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionDetailScreen(
    onBack: () -> Unit,
    onAppliedConfig: (bowId: String) -> Unit,
    viewModel: SuggestionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // Flip `wasRead` the first time the user opens the screen.
        viewModel.markRead()
        viewModel.events.collect { evt ->
            when (evt) {
                is SuggestionDetailEvent.Applied -> onAppliedConfig(evt.bowId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.suggestion?.parameter ?: "Suggestion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
                state.suggestion == null -> Text(
                    text = state.error ?: "Suggestion unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> SuggestionDetailBody(
                    suggestion = state.suggestion!!,
                    isApplying = state.isApplying,
                    error = state.error,
                    onApply = viewModel::apply,
                    onDismiss = viewModel::dismiss,
                    onMarkRead = viewModel::markRead,
                )
            }
        }
    }
}

@Composable
private fun SuggestionDetailBody(
    suggestion: AnalyticsSuggestion,
    isApplying: Boolean,
    error: String?,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderCard(suggestion = suggestion)

        ReasoningCard(suggestion = suggestion)

        suggestion.evidence?.let { evidence ->
            EvidenceCard(evidence = evidence)
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        ActionRow(
            suggestion = suggestion,
            isApplying = isApplying,
            onApply = onApply,
            onDismiss = onDismiss,
            onMarkRead = onMarkRead,
        )
    }
}

// ---------- Sub-components ----------

@Composable
private fun HeaderCard(suggestion: AnalyticsSuggestion) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = suggestion.parameter,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                ConfidenceBadge(confidence = suggestion.confidence)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatPill(
                    label = "Current",
                    value = suggestion.currentValue,
                    accent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                StatPill(
                    label = "Suggested",
                    value = suggestion.suggestedValue,
                    accent = MaterialTheme.colorScheme.primary,
                )
            }
            if (suggestion.wasApplied) {
                AppliedBadge(appliedAt = suggestion.appliedAt)
            }
        }
    }
}

@Composable
private fun AppliedBadge(appliedAt: Instant?) {
    val label = appliedAt
        ?.let { instant ->
            val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                .withZone(ZoneId.systemDefault())
            "Applied ${fmt.format(instant)}"
        } ?: "Applied"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFF2E7D32).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReasoningCard(suggestion: AnalyticsSuggestion) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Why",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = suggestion.reasoning,
                style = MaterialTheme.typography.bodyMedium,
            )
            suggestion.qualifier?.takeIf { it.isNotBlank() }?.let { qualifier ->
                Text(
                    text = qualifier,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun EvidenceCard(evidence: SuggestionEvidence) {
    AnalyticsCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Evidence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneId.systemDefault())
            Text(
                text = "Based on ${evidence.sampleSize} arrows across ${evidence.sessionIds.size} " +
                    "sessions, ${dateFmt.format(evidence.windowStart)} – ${dateFmt.format(evidence.windowEnd)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            evidence.metrics.forEach { metric ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = metric.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    metric.deltaFromBaseline?.takeIf { it.isNotBlank() }?.let { delta ->
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = delta,
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor(delta),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private fun deltaColor(delta: String): Color = when {
    delta.startsWith("+") -> Color(0xFF2E7D32)
    delta.startsWith("-") -> Color(0xFFC62828)
    else -> Color.Gray
}

@Composable
private fun ActionRow(
    suggestion: AnalyticsSuggestion,
    isApplying: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onApply,
            enabled = !isApplying && !suggestion.wasApplied,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.width(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Applying…")
            } else {
                Text(if (suggestion.wasApplied) "Already applied" else "Apply")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !suggestion.wasDismissed,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (suggestion.wasDismissed) "Dismissed" else "Dismiss")
            }
            OutlinedButton(
                onClick = onMarkRead,
                enabled = !suggestion.wasRead,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (suggestion.wasRead) "Read" else "Mark as read")
            }
        }
    }
}

// ---------- Preview ----------

@Preview(showBackground = true)
@Composable
private fun SuggestionDetailPreview() {
    BowPressTheme {
        Column(Modifier.padding(16.dp)) {
            SuggestionDetailBody(
                suggestion = previewSuggestion,
                isApplying = false,
                error = null,
                onApply = {},
                onDismiss = {},
                onMarkRead = {},
            )
        }
    }
}

private val previewSuggestion = AnalyticsSuggestion(
    id = "preview",
    bowId = "bow1",
    createdAt = Instant.now(),
    parameter = "restVertical",
    suggestedValue = "+3/16\"",
    currentValue = "+2/16\"",
    reasoning = "Vertical impact bias detected across last 3 sessions.",
    confidence = 0.82,
    qualifier = "Re-verify after 2 sessions.",
    wasRead = false,
    wasDismissed = false,
    deliveryType = DeliveryType.PUSH,
    evidence = SuggestionEvidence(
        sampleSize = 47,
        sessionIds = listOf("s1", "s2", "s3"),
        windowStart = LocalDate.now().minusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        windowEnd = Instant.now(),
        metrics = listOf(
            SuggestionEvidence.Metric("Average score", "10.5", "+0.4"),
            SuggestionEvidence.Metric("Vertical drift", "0.09 in", "+0.06 in"),
        ),
        patternType = "directional_drift",
    ),
)
