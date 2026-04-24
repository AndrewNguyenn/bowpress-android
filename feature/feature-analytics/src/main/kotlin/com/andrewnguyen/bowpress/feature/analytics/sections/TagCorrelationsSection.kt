package com.andrewnguyen.bowpress.feature.analytics.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.TagCorrelation
import com.andrewnguyen.bowpress.core.model.TagStrength
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Spec §Analysis Outputs #4 — Subjective-Objective Correlation. Mirrors iOS
 * `TagCorrelationsSection.swift`; one row per feel-tag with strength badge,
 * tagged vs untagged counts, and score delta.
 */
@Composable
fun TagCorrelationsSection(
    correlations: List<TagCorrelation>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Feel-to-Performance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        when {
            isLoading && correlations.isEmpty() -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
            correlations.isEmpty() -> Text(
                text = "No correlations yet. Keep logging session feel tags — correlations appear once a tag has ≥3 tagged sessions and ≥15% score difference vs untagged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                correlations.forEach { TagCorrelationRow(c = it) }
            }
        }
    }
}

@Composable
private fun TagCorrelationRow(c: TagCorrelation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StrengthBadge(c.strength)
                Text(
                    text = displayName(c.tag),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${c.taggedSessionCount} tagged · ${c.untaggedSessionCount} untagged",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.weight(0f))
        c.scoreDelta?.let { delta ->
            val sign = if (delta >= 0) "+" else ""
            val tint = if (delta >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            Text(
                text = "$sign${delta.roundToInt()} pts",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
        }
    }
}

@Composable
private fun StrengthBadge(strength: TagStrength) {
    val tint = when (strength) {
        TagStrength.WEAK -> Color(0xFFEF6C00)
        TagStrength.MODERATE -> Color(0xFFF9A825)
        TagStrength.STRONG -> Color(0xFF2E7D32)
    }
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = strength.name.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Humanise `sentiment:positive` → `Positive sentiment`, `grip_torque` → `Grip torque`. */
private fun displayName(tag: String): String {
    if (tag.startsWith("sentiment:")) {
        val value = tag.removePrefix("sentiment:")
        return value.replaceFirstChar { it.uppercase() } + " sentiment"
    }
    return tag.replace('_', ' ').split(' ').joinToString(" ") {
        it.replaceFirstChar { c -> c.uppercase() }
    }
}
