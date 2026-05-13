package com.andrewnguyen.bowpress.feature.equipment.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppLine
import com.andrewnguyen.bowpress.core.designsystem.AppLine2
import com.andrewnguyen.bowpress.core.designsystem.AppPaper
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = AppInk3,
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp, start = 4.dp),
    )
}

/**
 * Rounded surface that groups consecutive form rows under a SectionHeader.
 * Mirrors iOS GroupedListStyle — cream background with a hairline outline and
 * 0.5dp separators between rows. Dividers are auto-inserted via SubcomposeLayout
 * so call sites just stack rows naturally.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppCream,
    ) {
        SubcomposeLayout(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) { constraints ->
            val rowPlaceables = subcompose("rows", content)
                .map { it.measure(constraints) }
            val dividerCount = (rowPlaceables.size - 1).coerceAtLeast(0)
            val dividerPlaceables = subcompose("dividers") {
                repeat(dividerCount) {
                    HorizontalDivider(color = AppLine2, thickness = 0.5.dp)
                }
            }.map { it.measure(constraints) }
            val totalHeight = rowPlaceables.sumOf { it.height } +
                dividerPlaceables.sumOf { it.height }
            val totalWidth = rowPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
            layout(totalWidth, totalHeight) {
                var y = 0
                rowPlaceables.forEachIndexed { i, row ->
                    row.placeRelative(0, y)
                    y += row.height
                    if (i < dividerPlaceables.size) {
                        dividerPlaceables[i].placeRelative(0, y)
                        y += dividerPlaceables[i].height
                    }
                }
            }
        }
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
 * Joined stepper chip — two icon buttons sharing a rounded outline with a
 * vertical hairline between them. Mirrors the SwiftUI Stepper assembly used
 * on iOS BowDetail rows.
 */
@Composable
fun StepperChip(
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = AppPaper,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(width = 44.dp, height = 32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = decreaseDescription, tint = BowPressColors.Accent)
            }
            VerticalDivider(modifier = Modifier.height(20.dp), thickness = 1.dp, color = AppLine)
            IconButton(onClick = onIncrease, modifier = Modifier.size(width = 44.dp, height = 32.dp)) {
                Icon(Icons.Default.Add, contentDescription = increaseDescription, tint = BowPressColors.Accent)
            }
        }
    }
}

/**
 * Numeric stepper with a `−`/`+` pair around a value label. Clamps to
 * `[min, max]` and nudges by `step` on each tap. Value sits to the left of
 * the joined StepperChip — iOS layout.
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
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 8.dp),
            )
            StepperChip(
                onDecrease = { onChange((value - step).coerceAtLeast(min)) },
                onIncrease = { onChange((value + step).coerceAtMost(max)) },
                decreaseDescription = "Decrease $label",
                increaseDescription = "Increase $label",
            )
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
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 8.dp),
            )
            StepperChip(
                onDecrease = { onChange((value - step).coerceAtLeast(min)) },
                onIncrease = { onChange((value + step).coerceAtMost(max)) },
                decreaseDescription = "Decrease $label",
                increaseDescription = "Increase $label",
            )
        }
    }
}

/**
 * Interleaved stepper row — mirrors iOS BowConfigEditView.lengthRow:
 * `Label … [⊖] value unit [⊕]` with circle-outline icons. The value and
 * unit suffix render as separate text columns so the unit aligns rather
 * than being concatenated into one token. Used by LengthStepperRow for
 * imperial / metric length scalars (Draw Length, Brace Height, etc).
 */
@Composable
fun InterleavedStepperRow(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    valueText: String,
    unitSuffix: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.RemoveCircleOutline,
                    contentDescription = "Decrease $label",
                    tint = BowPressColors.Accent,
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp).padding(end = 2.dp),
            )
            Text(
                text = unitSuffix,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(16.dp),
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.AddCircleOutline,
                    contentDescription = "Increase $label",
                    tint = BowPressColors.Accent,
                )
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
