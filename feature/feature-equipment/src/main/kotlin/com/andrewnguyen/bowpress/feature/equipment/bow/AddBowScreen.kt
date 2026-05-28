package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader

/**
 * Form for creating a new bow. Two sections: Bow Type segmented control +
 * Name Your Bow text field. Mirrors iOS `AddBowView` which saves brand/model
 * as empty strings; advanced configuration happens later on BowDetail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBowScreen(
    userId: String,
    onBowCreated: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddBowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedBow) {
        state.savedBow?.let { bow ->
            onBowCreated(bow.id)
            viewModel.consumeSaved()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add Bow") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = BowPressColors.Accent, modifier = Modifier.size(24.dp).padding(end = 12.dp))
                    } else {
                        TextButton(
                            onClick = { viewModel.save(userId) },
                            enabled = state.canSave,
                        ) { Text("Save") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Bow Type")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                BowType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = BowType.entries.size),
                        selected = state.bowType == type,
                        onClick = { viewModel.updateBowType(type) },
                        modifier = Modifier.testTag("bow_type_${type.name.lowercase()}"),
                    ) { Text(type.label) }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Name Your Bow")
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("e.g. My Hoyt, Competition Rig") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("bow_name_field"),
            )

            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
