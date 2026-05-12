package com.andrewnguyen.bowpress.feature.equipment.arrow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.UnitToggle
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.ShaftDiameter
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitRange
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.feature.equipment.components.ArrowMassStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.DoubleStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.LengthStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddArrowScreen(
    userId: String,
    onCreated: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddArrowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedArrowId) {
        state.savedArrowId?.let(onCreated)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add Arrow") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = BowPressColors.Accent,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                        )
                    } else {
                        TextButton(onClick = { viewModel.save(userId) }, enabled = state.canSave) {
                            Text("Save")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val unitSystem = LocalUnitSystem.current
        val setUnitSystem = LocalUnitSystemSetter.current
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            UnitToggle(
                system = unitSystem,
                onSystemChange = setUnitSystem,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ArrowFormBody(
                label = state.label, onLabel = viewModel::updateLabel,
                brand = state.brand, onBrand = viewModel::updateBrand,
                model = state.model, onModel = viewModel::updateModel,
                length = state.length, onLength = viewModel::updateLength,
                pointWeight = state.pointWeight, onPointWeight = viewModel::updatePointWeight,
                fletchingType = state.fletchingType, onFletchingType = viewModel::updateFletchingType,
                fletchingLength = state.fletchingLength, onFletchingLength = viewModel::updateFletchingLength,
                fletchingOffset = state.fletchingOffset, onFletchingOffset = viewModel::updateFletchingOffset,
                nockType = state.nockType, onNockType = viewModel::updateNockType,
                shaftDiameter = state.shaftDiameter, onShaftDiameter = viewModel::updateShaftDiameter,
                unitSystem = unitSystem,
            )
            state.errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArrowFormBody(
    label: String, onLabel: (String) -> Unit,
    brand: String, onBrand: (String) -> Unit,
    model: String, onModel: (String) -> Unit,
    length: Double, onLength: (Double) -> Unit,
    pointWeight: Int, onPointWeight: (Int) -> Unit,
    fletchingType: FletchingType, onFletchingType: (FletchingType) -> Unit,
    fletchingLength: Double, onFletchingLength: (Double) -> Unit,
    fletchingOffset: Double, onFletchingOffset: (Double) -> Unit,
    nockType: String, onNockType: (String) -> Unit,
    shaftDiameter: ShaftDiameter?, onShaftDiameter: (ShaftDiameter?) -> Unit,
    unitSystem: UnitSystem,
) {
    // Mirrors iOS AddArrowView section structure (AddArrowView.swift:130-198):
    // Identity → Specs → Fletching → Shaft Diameter → Nock. iOS does not surface
    // a Total Weight field (the model has it but the form doesn't), and there's
    // no Notes section on AddArrow.
    SectionHeader("Identity")
    OutlinedTextField(
        value = label, onValueChange = onLabel,
        label = { Text("Label (required)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("arrow_label_field"),
    )
    OutlinedTextField(
        value = brand, onValueChange = onBrand,
        label = { Text("Brand (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    OutlinedTextField(
        value = model, onValueChange = onModel,
        label = { Text("Model (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )

    SectionHeader("Specs")
    LengthStepperRow(
        label = "Length",
        inches = length, onInchesChange = onLength,
        range = UnitRange.ARROW_LENGTH, unitSystem = unitSystem,
    )
    ArrowMassStepperRow(
        label = "Point Weight",
        grains = pointWeight, onGrainsChange = onPointWeight,
        range = UnitRange.POINT_WEIGHT, unitSystem = unitSystem,
    )

    SectionHeader("Fletching")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        FletchingType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = FletchingType.entries.size),
                selected = fletchingType == type,
                onClick = { onFletchingType(type) },
            ) { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
        }
    }
    LengthStepperRow(
        label = "Fletching Length",
        inches = fletchingLength, onInchesChange = onFletchingLength,
        range = UnitRange.FLETCHING_LENGTH, unitSystem = unitSystem,
    )
    DoubleStepperRow(
        "Offset", fletchingOffset, onFletchingOffset,
        UnitFormatting.degrees(fletchingOffset),
        min = 0.0, max = 10.0, step = 0.5,
    )

    SectionHeader("Shaft Diameter")
    ShaftDiameterPicker(selected = shaftDiameter, onSelect = onShaftDiameter, unitSystem = unitSystem)

    SectionHeader("Nock")
    OutlinedTextField(
        value = nockType, onValueChange = onNockType,
        label = { Text("Nock Type (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShaftDiameterPicker(
    selected: ShaftDiameter?,
    onSelect: (ShaftDiameter?) -> Unit,
    unitSystem: UnitSystem,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName(unitSystem) ?: "Not set",
            onValueChange = {},
            readOnly = true,
            label = { Text("Diameter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().padding(vertical = 4.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Not set") }, onClick = { onSelect(null); expanded = false })
            ShaftDiameter.entries.forEach { d ->
                DropdownMenuItem(text = { Text(d.displayName(unitSystem)) }, onClick = { onSelect(d); expanded = false })
            }
        }
    }
}
