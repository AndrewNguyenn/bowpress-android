package com.andrewnguyen.bowpress.feature.equipment.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import kotlinx.coroutines.launch

/**
 * Sheet-backed picker row for grip / limb identifiers on bow configs.
 * Mirrors iOS `BowConfigSuggestRow`: tapping the row opens a bottom sheet
 * listing every value the archer has previously entered across their bow
 * configs, with options to pick, add a new one, or clear the selection.
 *
 * Row label shows the current value or "None" when empty, with a trailing
 * chevron. Catalog is sourced from a caller-supplied `suggestions` list
 * (typically a flow of distinct values across BowConfigRepository).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowConfigSuggestRow(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    accessibilityKey: String,
    modifier: Modifier = Modifier,
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val trimmed = value.trim()
    val displayValue = if (trimmed.isEmpty()) "None" else trimmed
    val isPlaceholder = trimmed.isEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isPickerOpen = true }
            .padding(vertical = 12.dp)
            .testTag("${accessibilityKey}_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaceholder) AppInk3 else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 4.dp),
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppInk3,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (isPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { isPickerOpen = false },
            sheetState = sheetState,
        ) {
            SuggestPickerSheetBody(
                title = label,
                placeholder = placeholder,
                value = value,
                onValueChange = onValueChange,
                suggestions = suggestions,
                accessibilityKey = accessibilityKey,
                onDismiss = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { isPickerOpen = false }
                },
            )
        }
    }
}

@Composable
private fun SuggestPickerSheetBody(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    accessibilityKey: String,
    onDismiss: () -> Unit,
) {
    var newEntry by remember { mutableStateOf("") }
    val canAddNew = newEntry.trim().isNotEmpty() &&
        suggestions.none { it.equals(newEntry.trim(), ignoreCase = true) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (suggestions.isNotEmpty()) {
            Text(
                text = "Saved ${title.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = AppInk3,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppCream,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(suggestions) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(name)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("${accessibilityKey}_picker_row_${suggestions.indexOf(name)}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            if (name.equals(value.trim(), ignoreCase = true)) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = BowPressColors.Accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (name != suggestions.last()) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "Add new",
            style = MaterialTheme.typography.labelSmall,
            color = AppInk3,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = newEntry,
                onValueChange = { newEntry = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${accessibilityKey}_picker_new_field"),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = {
                    onValueChange(newEntry.trim())
                    onDismiss()
                },
                enabled = canAddNew,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppCream,
                    contentColor = AppPondDk,
                ),
                shape = RoundedCornerShape(50),
            ) { Text("Add") }
        }

        if (value.trim().isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = {
                    onValueChange("")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Clear selection",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        Spacer(Modifier.height(16.dp))
    }
}
