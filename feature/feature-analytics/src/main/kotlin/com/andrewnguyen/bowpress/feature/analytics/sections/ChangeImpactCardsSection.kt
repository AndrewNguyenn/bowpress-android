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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.ChangeClassification
import com.andrewnguyen.bowpress.core.model.ChangeImpactCard
import com.andrewnguyen.bowpress.core.model.ConfigurationChange
import kotlin.math.roundToInt

/**
 * Spec §Analysis Outputs #3 — Change Impact Cards. Mirrors
 * iOS `ChangeImpactCardsSection.swift`; one card per [ConfigurationChange]
 * with score delta, classification badge, and aggregated feel tags.
 */
@Composable
fun ChangeImpactCardsSection(
    changes: List<ConfigurationChange>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Change Impact",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        when {
            isLoading && changes.isEmpty() -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
            changes.isEmpty() -> Text(
                text = "No config changes yet. Tuning adjustments will appear here with before/after scores.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                changes.forEach { ChangeImpactCard(change = it) }
            }
        }
    }
}

@Composable
private fun ChangeImpactCard(change: ConfigurationChange) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = summaryTitle(change),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            change.impact?.let { ClassificationBadge(it.classification) }
        }
        if (change.impact != null) {
            DeltaRow(change.impact!!)
            if (change.impact!!.feelTagsBefore.isNotEmpty() || change.impact!!.feelTagsAfter.isNotEmpty()) {
                FeelTagsRow(change.impact!!)
            }
        } else {
            Text(
                text = "Pending analytics — needs ≥6 arrows under this config for impact scoring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        change.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

private fun summaryTitle(c: ConfigurationChange): String {
    if (c.changedFields.isEmpty()) return "Configuration change"
    val shown = c.changedFields.take(2)
        .joinToString("  ·  ") { "${it.field}: ${it.fromValue} → ${it.toValue}" }
    return if (c.changedFields.size > 2) "$shown  +${c.changedFields.size - 2} more" else shown
}

@Composable
private fun ClassificationBadge(classification: ChangeClassification) {
    val tint = if (classification == ChangeClassification.CLEAN) {
        Color(0xFF2E7D32) // green
    } else {
        Color(0xFFEF6C00) // orange
    }
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = classification.name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DeltaRow(impact: ChangeImpactCard) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        impact.scoreBefore?.let { before ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "BEFORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = before.roundToInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        impact.scoreAfter?.let { after ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "AFTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = after.roundToInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        impact.scoreDelta?.let { delta ->
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
private fun FeelTagsRow(impact: ChangeImpactCard) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (impact.feelTagsBefore.isNotEmpty()) {
            Text(
                text = "Before: ${impact.feelTagsBefore.take(3).joinToString(" · ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        if (impact.feelTagsAfter.isNotEmpty()) {
            Text(
                text = "After: ${impact.feelTagsAfter.take(3).joinToString(" · ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}
