package com.andrewnguyen.bowpress.feature.equipment.sightmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppCream
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.model.DistanceUnit
import com.andrewnguyen.bowpress.core.model.SightMark
import kotlinx.coroutines.launch

/**
 * Add/edit sheet for a sight mark. iOS `SightMarkEditSheet` form:
 *   Distance + unit pill (Yards / Meters)
 *   Sight mark numeric
 *   Note (optional)
 *   Save / Delete (edit mode)
 *
 * Returns inputs via [onSave]; the caller writes to the repository.
 * Mirrors iOS modes: [initial] == null is add; non-null is edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightMarkEditSheet(
    initial: SightMark?,
    onSave: (distance: Double, unit: DistanceUnit, mark: Double, note: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var distance by remember { mutableStateOf(initial?.distance?.toString() ?: "") }
    var mark by remember { mutableStateOf(initial?.mark?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var unit by remember { mutableStateOf(initial?.distanceUnit ?: DistanceUnit.YARDS) }
    var pendingDelete by remember { mutableStateOf(false) }

    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val canSave = distance.toDoubleOrNull() != null && mark.toDoubleOrNull() != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initial == null) "Add sight mark" else "Edit sight mark",
                style = frauncesDisplay(22.sp, italic = true, weight = FontWeight.Medium)
                    .copy(color = AppInk),
            )

            // Distance row — text field + unit toggle pill.
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Distance") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    modifier = Modifier.weight(1f).testTag("sight_mark_distance_field"),
                )
                Spacer(Modifier.padding(start = 8.dp))
                UnitToggle(value = unit, onChange = { unit = it })
            }

            OutlinedTextField(
                value = mark,
                onValueChange = { mark = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Sight mark") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth().testTag("sight_mark_value_field"),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onDelete != null) {
                    TextButton(
                        onClick = { pendingDelete = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = frauncesDisplay(15.sp, italic = true),
                        )
                    }
                }
                Button(
                    onClick = {
                        val d = distance.toDoubleOrNull() ?: return@Button
                        val m = mark.toDoubleOrNull() ?: return@Button
                        onSave(d, unit, m, note)
                        close()
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPondDk,
                        contentColor = AppCream,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sight_mark_save_button"),
                ) {
                    Text("Save", style = frauncesDisplay(15.sp, italic = true))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (pendingDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("Delete this mark?") },
            text = {
                Text(
                    "Removes this mark from the calibration set. Past sessions aren't affected.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    pendingDelete = false
                    close()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun UnitToggle(value: DistanceUnit, onChange: (DistanceUnit) -> Unit) {
    val options = listOf(DistanceUnit.YARDS to "yd", DistanceUnit.METERS to "m")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppInk3.copy(alpha = 0.18f))
            .padding(2.dp),
    ) {
        options.forEach { (u, label) ->
            val selected = u == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) AppCream else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(u) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    style = frauncesDisplay(14.sp, italic = true).copy(
                        color = if (selected) AppInk else AppInk3,
                    ),
                )
            }
        }
    }
}
