package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.UnitToggle
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitRange
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Field
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Section
import com.andrewnguyen.bowpress.feature.equipment.components.DoubleStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.IntStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.LengthStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.MmLengthStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import com.andrewnguyen.bowpress.feature.equipment.components.StabWeightStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.halfTwistLabel
import com.andrewnguyen.bowpress.feature.equipment.components.limbTurnsLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sightPositionLabel

/**
 * Immutable bundle of update callbacks for [BowConfigEditFormBody]. Backing
 * instance on ViewModel; fake in tests. Keeping all mutators on one interface
 * keeps the composable signature from spiralling into 30+ lambda parameters.
 */
interface BowConfigEditCallbacks {
    fun updateLabel(v: String)
    fun updateDrawLength(v: Double)
    fun updateRestVertical(v: Int)
    fun updateRestHorizontal(v: Int)
    fun updateRestDepth(v: Double)
    fun updateSightPosition(v: Int)
    fun updateGripAngle(v: Double)
    fun updateNockingHeight(v: Int)
    fun updateLetOff(v: Double)
    fun updatePeepHeight(v: Double)
    fun updateDLoop(v: Double)
    fun updateTopCable(v: Int)
    fun updateBottomCable(v: Int)
    fun updateMainStringTop(v: Int)
    fun updateMainStringBottom(v: Int)
    fun updateTopLimb(v: Double)
    fun updateBottomLimb(v: Double)
    fun updateFrontStabWeight(v: Double)
    fun updateFrontStabAngle(v: Double)
    fun updateRearStabSide(v: RearStabSide)
    fun updateRearStabWeight(v: Double)
    fun updateRearStabVertAngle(v: Double)
    fun updateRearStabHorizAngle(v: Double)
    fun updateBraceHeight(v: Double)
    fun updateTillerTop(v: Double)
    fun updateTillerBottom(v: Double)
    fun updatePlungerTension(v: Int)
    fun updateClickerPosition(v: Double)
    fun updateRearStabLeftWeight(v: Double)
    fun updateRearStabRightWeight(v: Double)
    fun updateSpecificGrip(v: String)
    fun updateSpecificLimbs(v: String)
}

/**
 * Bow-config edit form — the complex one. Every section's visibility is routed
 * through [EquipmentFieldRules] so the same predicate the unit tests use also
 * governs what renders here.
 *
 * `isSetup = true` when seeded from the initial v1 configuration (compound Bow
 * Setup is the editable let-off/peep/d-loop variant). For subsequent tunes,
 * `isSetup = false` produces the read-only Base Setup summary — matches iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowConfigEditScreen(
    onSaved: (configId: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BowConfigEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedConfigId) {
        state.savedConfigId?.let(onSaved)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = if (state.baseConfig?.label == "Initial Setup") "Set Up Bow" else "Log Tuning"
                    Text(title)
                },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = BowPressColors.Accent,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                        )
                    } else {
                        TextButton(
                            onClick = viewModel::save,
                            enabled = !state.isLoading && state.bow != null,
                            modifier = Modifier.testTag("save_config_button"),
                        ) { Text("Save") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BowPressColors.Accent) }

            state.bow == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Bow not found", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> BowConfigEditFormBody(
                state = state,
                bowType = state.bow!!.bowType,
                isSetup = state.baseConfig?.label == "Initial Setup",
                callbacks = viewModel.asCallbacks(),
                unitSystem = LocalUnitSystem.current,
                onUnitSystemChange = LocalUnitSystemSetter.current,
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

/**
 * Stateless form body extracted for Compose UI testing. The screen-level
 * composable above gathers `state` via Hilt; this version takes it directly so
 * tests can drive `bowType` / `rearStabSide` through synthetic state.
 *
 * The caller owns the scroll modifier — standalone usage applies
 * `Modifier.verticalScroll(...)`; when embedded (BowDetailScreen) the parent's
 * scroll container already paginates everything, and a nested vertical scroll
 * would crash with `Vertically scrollable component was measured with infinity`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BowConfigEditFormBody(
    state: BowConfigEditViewModel.UiState,
    bowType: BowType,
    isSetup: Boolean,
    callbacks: BowConfigEditCallbacks,
    unitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
    // BowDetailScreen renders its own UnitToggle + Bow Info ahead of this body
    // (matches iOS BowDetailView ordering) and iOS doesn't expose a Label there,
    // so let the caller suppress both.
    showUnitToggle: Boolean = true,
    showLabel: Boolean = true,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .testTag("bow_config_edit_form_${bowType.name.lowercase()}"),
    ) {
        if (showUnitToggle) {
            UnitToggle(
                system = unitSystem,
                onSystemChange = onUnitSystemChange,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        if (showLabel && EquipmentFieldRules.sectionVisible(Section.LABEL, bowType, isSetup)) {
            SectionHeader("Label")
            OutlinedTextField(
                value = state.label,
                onValueChange = callbacks::updateLabel,
                label = { Text("Optional label") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("bow_config_label_field"),
            )
        }

        // Bow setup — type-specific.
        // iOS BowDetailView uses "Setup" for the editable case; "Base Setup" is
        // Android-only — the read-only summary shown when logging a new tuning
        // entry against an existing snapshot.
        SectionHeader(if (EquipmentFieldRules.sectionVisible(Section.BASE_SETUP, bowType, isSetup)) "Base Setup" else "Setup")
        when {
            EquipmentFieldRules.sectionVisible(Section.BASE_SETUP, bowType, isSetup) -> {
                // Compound non-setup: read-only summary row (matches iOS text block).
                val summary = state.baseConfig?.compactSetupLine(unitSystem).orEmpty()
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                if (EquipmentFieldRules.isVisible(Field.DRAW_LENGTH, bowType, isSetup, state.rearStabSide)) {
                    LengthStepperRow("Draw Length", state.drawLength, callbacks::updateDrawLength,
                        UnitRange.DRAW_LENGTH, unitSystem, digits = 1)
                }
                if (EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "Let-off", state.letOffPct, callbacks::updateLetOff,
                        UnitFormatting.percent(state.letOffPct),
                        min = 40.0, max = 99.0, step = 1.0,
                    )
                }
                if (EquipmentFieldRules.isVisible(Field.PEEP_HEIGHT, bowType, isSetup, state.rearStabSide)) {
                    LengthStepperRow("Peep Height", state.peepHeight, callbacks::updatePeepHeight,
                        UnitRange.PEEP_HEIGHT, unitSystem)
                }
                if (EquipmentFieldRules.isVisible(Field.D_LOOP_LENGTH, bowType, isSetup, state.rearStabSide)) {
                    LengthStepperRow("D-Loop Length", state.dLoopLength, callbacks::updateDLoop,
                        UnitRange.D_LOOP_LENGTH, unitSystem, digits = 3)
                }
                if (EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, bowType, isSetup, state.rearStabSide)) {
                    LengthStepperRow("Brace Height", state.braceHeight, callbacks::updateBraceHeight,
                        UnitRange.BRACE_HEIGHT, unitSystem, digits = 3)
                }
            }
        }

        if (EquipmentFieldRules.sectionVisible(Section.STRING_AND_CABLE, bowType, isSetup)) {
            SectionHeader("String & Cable")
            IntStepperRow("Top Cable", state.topCableTwists, callbacks::updateTopCable, halfTwistLabel(state.topCableTwists), -10, 10)
            IntStepperRow("Bottom Cable", state.bottomCableTwists, callbacks::updateBottomCable, halfTwistLabel(state.bottomCableTwists), -10, 10)
            IntStepperRow("Main String Top", state.mainStringTopTwists, callbacks::updateMainStringTop, halfTwistLabel(state.mainStringTopTwists), -10, 10)
            IntStepperRow("Main String Bottom", state.mainStringBottomTwists, callbacks::updateMainStringBottom, halfTwistLabel(state.mainStringBottomTwists), -10, 10)
        }
        if (EquipmentFieldRules.sectionVisible(Section.LIMBS, bowType, isSetup)) {
            SectionHeader("Limbs")
            when (bowType) {
                BowType.COMPOUND -> {
                    DoubleStepperRow("Top Limb", state.topLimbTurns, callbacks::updateTopLimb, limbTurnsLabel(state.topLimbTurns), -10.0, 10.0, 0.5)
                    DoubleStepperRow("Bottom Limb", state.bottomLimbTurns, callbacks::updateBottomLimb, limbTurnsLabel(state.bottomLimbTurns), -10.0, 10.0, 0.5)
                }
                BowType.RECURVE, BowType.BAREBOW -> {
                    OutlinedTextField(
                        value = state.specificLimbs,
                        onValueChange = callbacks::updateSpecificLimbs,
                        label = { Text("Specific Limbs") },
                        placeholder = { Text("e.g. Hoyt 970 Velos 36# medium") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("specific_limbs_field"),
                    )
                }
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.REST, bowType, isSetup)) {
            SectionHeader("Rest")
            IntStepperRow("Vertical", state.restVertical, callbacks::updateRestVertical,
                UnitFormatting.sixteenths(state.restVertical, unitSystem), -16, 16)
            IntStepperRow("Horizontal", state.restHorizontal, callbacks::updateRestHorizontal,
                UnitFormatting.sixteenths(state.restHorizontal, unitSystem), -16, 16)
            LengthStepperRow("Depth", state.restDepth, callbacks::updateRestDepth,
                UnitRange.REST_DEPTH, unitSystem)
        }
        if (EquipmentFieldRules.sectionVisible(Section.SIGHT_GRIP_NOCK, bowType, isSetup)) {
            SectionHeader("Sight, Grip & Nock")
            IntStepperRow("Sight Position", state.sightPosition, callbacks::updateSightPosition, sightPositionLabel(state.sightPosition), -15, 15)
            DoubleStepperRow("Grip Angle", state.gripAngle, callbacks::updateGripAngle,
                UnitFormatting.degrees(state.gripAngle), 0.0, 90.0, 0.5)
            IntStepperRow("Nocking Height", state.nockingHeight, callbacks::updateNockingHeight,
                UnitFormatting.sixteenths(state.nockingHeight, unitSystem), -80, 80)
        }

        if (EquipmentFieldRules.sectionVisible(Section.TILLER, bowType, isSetup)) {
            SectionHeader("Tiller")
            MmLengthStepperRow("Top Tiller", state.tillerTop, callbacks::updateTillerTop,
                UnitRange.TILLER, unitSystem)
            MmLengthStepperRow("Bottom Tiller", state.tillerBottom, callbacks::updateTillerBottom,
                UnitRange.TILLER, unitSystem)
        }
        if (EquipmentFieldRules.sectionVisible(Section.PLUNGER, bowType, isSetup)) {
            SectionHeader("Plunger")
            IntStepperRow("Tension", state.plungerTension, callbacks::updatePlungerTension, "${state.plungerTension} clicks", 0, 30)
        }
        if (EquipmentFieldRules.sectionVisible(Section.CLICKER, bowType, isSetup)) {
            SectionHeader("Clicker")
            MmLengthStepperRow("Position", state.clickerPosition, callbacks::updateClickerPosition,
                UnitRange.CLICKER, unitSystem, digits = 0)
        }
        if (EquipmentFieldRules.sectionVisible(Section.GRIP_AND_NOCK, bowType, isSetup)) {
            SectionHeader("Grip & Nock")
            DoubleStepperRow("Grip Angle", state.gripAngle, callbacks::updateGripAngle,
                UnitFormatting.degrees(state.gripAngle), 0.0, 90.0, 0.5)
            // iOS renders the Specific Grip text field between Grip Angle and
            // Nocking Height (BowDetailView.swift recurveSections / barebowSections).
            OutlinedTextField(
                value = state.specificGrip,
                onValueChange = callbacks::updateSpecificGrip,
                label = { Text("Specific Grip") },
                placeholder = { Text("e.g. Jager Hunter") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("specific_grip_field"),
            )
            IntStepperRow("Nocking Height", state.nockingHeight, callbacks::updateNockingHeight,
                UnitFormatting.sixteenths(state.nockingHeight, unitSystem), -80, 80)
        }

        if (EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, bowType, isSetup)) {
            SectionHeader("Front Stabilizer")
            val weightRange = if (bowType == BowType.COMPOUND) UnitRange.FRONT_STAB_WEIGHT else UnitRange.VBAR_WEIGHT
            val weightOverride = if (state.frontStabWeight == 0.0 && bowType == BowType.COMPOUND) "None" else null
            StabWeightStepperRow("Weight", state.frontStabWeight, callbacks::updateFrontStabWeight,
                weightRange, unitSystem, valueLabelOverride = weightOverride)
            DoubleStepperRow("Angle", state.frontStabAngle, callbacks::updateFrontStabAngle,
                UnitFormatting.degrees(state.frontStabAngle, digits = 0), 0.0, 10.0, 1.0)
        }

        if (EquipmentFieldRules.sectionVisible(Section.REAR_STAB, bowType, isSetup)) {
            SectionHeader("Rear Stabilizer")
            RearStabSidePicker(selected = state.rearStabSide, onSelect = callbacks::updateRearStabSide)
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_WEIGHT, bowType, isSetup, state.rearStabSide)) {
                StabWeightStepperRow("Weight", state.rearStabWeight, callbacks::updateRearStabWeight,
                    UnitRange.REAR_STAB_WEIGHT, unitSystem)
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_LEFT_WEIGHT, bowType, isSetup, state.rearStabSide)) {
                StabWeightStepperRow("Left Weight", state.rearStabLeftWeight, callbacks::updateRearStabLeftWeight,
                    UnitRange.REAR_STAB_WEIGHT, unitSystem)
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_RIGHT_WEIGHT, bowType, isSetup, state.rearStabSide)) {
                StabWeightStepperRow("Right Weight", state.rearStabRightWeight, callbacks::updateRearStabRightWeight,
                    UnitRange.REAR_STAB_WEIGHT, unitSystem)
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_VERT_ANGLE, bowType, isSetup, state.rearStabSide)) {
                DoubleStepperRow(
                    "Vertical Angle", state.rearStabVertAngle, callbacks::updateRearStabVertAngle,
                    UnitFormatting.degrees(state.rearStabVertAngle, digits = 0), -90.0, 90.0, 5.0,
                )
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_HORIZ_ANGLE, bowType, isSetup, state.rearStabSide)) {
                DoubleStepperRow(
                    "Horizontal Angle", state.rearStabHorizAngle, callbacks::updateRearStabHorizAngle,
                    UnitFormatting.degrees(state.rearStabHorizAngle, digits = 0), 0.0, 90.0, 5.0,
                )
            }
        }

        if (EquipmentFieldRules.sectionVisible(Section.V_BAR, bowType, isSetup)) {
            SectionHeader("V-Bar (Rear Stabilizer)")
            StabWeightStepperRow("Left Weight", state.rearStabLeftWeight, callbacks::updateRearStabLeftWeight,
                UnitRange.VBAR_WEIGHT, unitSystem)
            StabWeightStepperRow("Right Weight", state.rearStabRightWeight, callbacks::updateRearStabRightWeight,
                UnitRange.VBAR_WEIGHT, unitSystem)
            DoubleStepperRow("Vertical Angle", state.rearStabVertAngle, callbacks::updateRearStabVertAngle,
                UnitFormatting.degrees(state.rearStabVertAngle, digits = 0), -90.0, 90.0, 5.0)
            DoubleStepperRow("Horizontal Angle", state.rearStabHorizAngle, callbacks::updateRearStabHorizAngle,
                UnitFormatting.degrees(state.rearStabHorizAngle, digits = 0), 0.0, 90.0, 5.0)
        }

        Spacer(Modifier.height(24.dp))
        state.errorMessage?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RearStabSidePicker(selected: RearStabSide, onSelect: (RearStabSide) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Side") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("rear_stab_side_picker"),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RearStabSide.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

/** Exposes the ViewModel's mutators as a [BowConfigEditCallbacks] implementation. */
internal fun BowConfigEditViewModel.asCallbacks(): BowConfigEditCallbacks = object : BowConfigEditCallbacks {
    override fun updateLabel(v: String) = this@asCallbacks.updateLabel(v)
    override fun updateDrawLength(v: Double) = this@asCallbacks.updateDrawLength(v)
    override fun updateRestVertical(v: Int) = this@asCallbacks.updateRestVertical(v)
    override fun updateRestHorizontal(v: Int) = this@asCallbacks.updateRestHorizontal(v)
    override fun updateRestDepth(v: Double) = this@asCallbacks.updateRestDepth(v)
    override fun updateSightPosition(v: Int) = this@asCallbacks.updateSightPosition(v)
    override fun updateGripAngle(v: Double) = this@asCallbacks.updateGripAngle(v)
    override fun updateNockingHeight(v: Int) = this@asCallbacks.updateNockingHeight(v)
    override fun updateLetOff(v: Double) = this@asCallbacks.updateLetOff(v)
    override fun updatePeepHeight(v: Double) = this@asCallbacks.updatePeepHeight(v)
    override fun updateDLoop(v: Double) = this@asCallbacks.updateDLoop(v)
    override fun updateTopCable(v: Int) = this@asCallbacks.updateTopCable(v)
    override fun updateBottomCable(v: Int) = this@asCallbacks.updateBottomCable(v)
    override fun updateMainStringTop(v: Int) = this@asCallbacks.updateMainStringTop(v)
    override fun updateMainStringBottom(v: Int) = this@asCallbacks.updateMainStringBottom(v)
    override fun updateTopLimb(v: Double) = this@asCallbacks.updateTopLimb(v)
    override fun updateBottomLimb(v: Double) = this@asCallbacks.updateBottomLimb(v)
    override fun updateFrontStabWeight(v: Double) = this@asCallbacks.updateFrontStabWeight(v)
    override fun updateFrontStabAngle(v: Double) = this@asCallbacks.updateFrontStabAngle(v)
    override fun updateRearStabSide(v: RearStabSide) = this@asCallbacks.updateRearStabSide(v)
    override fun updateRearStabWeight(v: Double) = this@asCallbacks.updateRearStabWeight(v)
    override fun updateRearStabVertAngle(v: Double) = this@asCallbacks.updateRearStabVertAngle(v)
    override fun updateRearStabHorizAngle(v: Double) = this@asCallbacks.updateRearStabHorizAngle(v)
    override fun updateBraceHeight(v: Double) = this@asCallbacks.updateBraceHeight(v)
    override fun updateTillerTop(v: Double) = this@asCallbacks.updateTillerTop(v)
    override fun updateTillerBottom(v: Double) = this@asCallbacks.updateTillerBottom(v)
    override fun updatePlungerTension(v: Int) = this@asCallbacks.updatePlungerTension(v)
    override fun updateClickerPosition(v: Double) = this@asCallbacks.updateClickerPosition(v)
    override fun updateRearStabLeftWeight(v: Double) = this@asCallbacks.updateRearStabLeftWeight(v)
    override fun updateRearStabRightWeight(v: Double) = this@asCallbacks.updateRearStabRightWeight(v)
    override fun updateSpecificGrip(v: String) = this@asCallbacks.updateSpecificGrip(v)
    override fun updateSpecificLimbs(v: String) = this@asCallbacks.updateSpecificLimbs(v)
}
