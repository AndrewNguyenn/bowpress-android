package com.andrewnguyen.bowpress.feature.equipment.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors

/**
 * Uniform "section header" row — bold, tinted title followed by a rule. Section
 * content is laid out underneath in the calling Column.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = BowPressColors.Accent,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * Label / value row — two-column layout, title on the left, value on the right.
 */
@Composable
fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Numeric stepper with a `−`/`+` pair around a value label. Clamps to
 * `[min, max]` and nudges by `step` on each tap.
 */
@Composable
fun IntStepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    valueLabel: String = value.toString(),
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    step: Int = 1,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label", tint = BowPressColors.Accent)
            }
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(96.dp),
            )
            IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label", tint = BowPressColors.Accent)
            }
        }
    }
}

@Composable
fun DoubleStepperRow(
    label: String,
    value: Double,
    onChange: (Double) -> Unit,
    valueLabel: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    step: Double = 0.5,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label", tint = BowPressColors.Accent)
            }
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(96.dp),
            )
            IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label", tint = BowPressColors.Accent)
            }
        }
    }
}

/** Simple outlined text field wrapper used by Add/Edit forms. */
@Composable
fun LabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Small vertical spacer used between sections. */
@Composable
fun VSpace(height: Int = 8) {
    Spacer(modifier = Modifier.height(height.dp))
}

/**
 * Score badge shown on history entries — a small pill coloured by the accent when
 * the row is the reference, tinted secondary otherwise.
 */
@Composable
fun ScoreBadge(score: Double?, isReference: Boolean) {
    val label = score?.let { "${it.toInt()}/100" } ?: "—"
    val bg = if (isReference) BowPressColors.Accent else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isReference) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
