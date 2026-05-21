package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andrewnguyen.bowpress.core.designsystem.AppInk
import com.andrewnguyen.bowpress.core.designsystem.AppInk3
import com.andrewnguyen.bowpress.core.designsystem.AppPond
import com.andrewnguyen.bowpress.core.designsystem.AppPondDk
import com.andrewnguyen.bowpress.core.designsystem.frauncesDisplay
import com.andrewnguyen.bowpress.core.designsystem.interUI
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.feature.social.ui.location.LocationTagPicker

/**
 * Final end-session sheet — collects closing notes, optional feel tags, and a
 * §18 Instagram-style location tag before handing off to
 * [SessionViewModel.endSession]. Mirrors iOS end-of-session flow + the
 * session-notes-sheet Location row.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EndSessionSheet(
    onDismiss: () -> Unit,
    onFinish: (notes: String, feelTags: List<String>, location: SessionLocation?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var notes by remember { mutableStateOf("") }
    val availableTags = remember {
        listOf("Strong", "Tired", "Focused", "Shaky", "Groovy", "Tense")
    }
    val selectedTags = remember { mutableStateListOf<String>() }
    // §18 — the location tag for the session, captured via LocationTagPicker.
    var location by remember { mutableStateOf<SessionLocation?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(20.dp)) {
            Text("End Session", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Session notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(16.dp))
            Text("Feel", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTags.forEach { tag ->
                    val selected = tag in selectedTags
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedTags.remove(tag)
                            else selectedTags.add(tag)
                        },
                        label = { Text(tag) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            LocationRow(
                location = location,
                onClick = { showLocationPicker = true },
            )
            HorizontalDivider()

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { onFinish(notes, selectedTags.toList(), location) },
                ) { Text("Finish") }
            }
        }
    }

    if (showLocationPicker) {
        LocationTagPicker(
            initial = location,
            onSave = { location = it },
            onRemove = { location = null },
            onDismiss = { showLocationPicker = false },
        )
    }
}

/**
 * The §18 Location row — opens the map-pin tag picker. Shows the tagged place
 * once set. Mirrors iOS `SessionNotesSheet.locationRow`.
 */
@Composable
private fun LocationRow(
    location: SessionLocation?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag(TestTags.SessionNotesLocationRow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (location == null) {
                Icons.Default.LocationOff
            } else {
                Icons.Default.Place
            },
            contentDescription = null,
            tint = AppPondDk,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "LOCATION",
                style = interUI(9.5.sp, FontWeight.SemiBold).copy(letterSpacing = 0.22.em),
                color = AppInk3,
            )
            Text(
                text = location?.name ?: "Tag where you shot",
                style = frauncesDisplay(15.sp),
                color = if (location == null) AppInk3 else AppInk,
            )
        }
        Text(
            text = "›",
            style = frauncesDisplay(18.sp),
            color = AppPond,
        )
    }
}
