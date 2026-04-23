package com.andrewnguyen.bowpress.feature.analytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Accent color referenced by the team-lead contract (0xFFD14B2E). */
internal val AccentColor: Color = Color(0xFFD14B2E)

/** Formats `value` to one decimal, using `en-US` locale so dots don't flip to commas on other locales. */
internal fun formatScore(value: Double): String = String.format(Locale.US, "%.1f", value)

/** Formats a 0–1 rate as an integer percentage, e.g. `0.235 → "24%"`. */
internal fun formatPercent(value: Double): String =
    String.format(Locale.US, "%.0f%%", value)

/**
 * Compact "pill" — reused across overview cards and comparison deltas.
 * Fills its parent width, aligns label + value vertically.
 */
@Composable
internal fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

/**
 * Circular confidence badge (0–1 input → percentage label). Green at ≥0.8,
 * amber at ≥0.6, grey otherwise — matches the iOS `ConfidenceBadge`.
 */
@Composable
internal fun ConfidenceBadge(confidence: Double, modifier: Modifier = Modifier) {
    val tint = when {
        confidence >= 0.80 -> Color(0xFF2E7D32) // green
        confidence >= 0.60 -> Color(0xFFEF6C00) // amber
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val label = String.format(Locale.US, "%.0f%%", confidence * 100.0)
    Box(
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

/** Standard card used for analytics sections — matches Material 3 surface tint. */
@Composable
internal fun AnalyticsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

/** Arrow glyph used to communicate delta direction on the comparison card. */
@Composable
internal fun DeltaRow(label: String, current: Double, previous: Double) {
    val delta = current - previous
    val symbol = when {
        delta > 0 -> "▲"
        delta < 0 -> "▼"
        else -> "—"
    }
    val tint = when {
        delta > 0 -> Color(0xFF2E7D32)
        delta < 0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatScore(current),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$symbol ${String.format(Locale.US, "%+.1f", delta)}",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
