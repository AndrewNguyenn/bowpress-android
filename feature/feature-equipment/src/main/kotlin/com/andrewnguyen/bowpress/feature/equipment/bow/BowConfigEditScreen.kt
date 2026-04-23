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
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Field
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Section
import com.andrewnguyen.bowpress.feature.equipment.components.DoubleStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.IntStepperRow
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import com.andrewnguyen.bowpress.feature.equipment.components.clickerLabel
import com.andrewnguyen.bowpress.feature.equipment.components.formatG
import com.andrewnguyen.bowpress.feature.equipment.components.halfTwistLabel
import com.andrewnguyen.bowpress.feature.equipment.components.limbTurnsLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sightPositionLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sixteenthLabel
import com.andrewnguyen.bowpress.feature.equipment.components.tillerLabel

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
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * Stateless form body extracted for Compose UI testing. The screen-level
 * composable above gathers `state` via Hilt; this version takes it directly so
 * tests can drive `bowType` / `rearStabSide` through synthetic state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BowConfigEditFormBody(
    state: BowConfigEditViewModel.UiState,
    bowType: BowType,
    isSetup: Boolean,
    callbacks: BowConfigEditCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("bow_config_edit_form_${bowType.name.lowercase()}"),
    ) {
        if (EquipmentFieldRules.sectionVisible(Section.LABEL, bowType, isSetup)) {
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
        SectionHeader(if (EquipmentFieldRules.sectionVisible(Section.BASE_SETUP, bowType, isSetup)) "Base Setup" else "Bow Setup")
        when {
            EquipmentFieldRules.sectionVisible(Section.BASE_SETUP, bowType, isSetup) -> {
                // Compound non-setup: read-only summary row (matches iOS text block).
                val base = state.baseConfig
                val summary = base?.let {
                    "Draw ${"%.1f".format(it.drawLength)}\" · " +
                        "Let-off ${(it.letOffPct ?: 0.0).toInt()}% · " +
                        "Peep ${"%.2f".format(it.peepHeight ?: 0.0)}\" · " +
                        "D-loop ${"%.3f".format(it.dLoopLength ?: 0.0)}\""
                }.orEmpty()
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                if (EquipmentFieldRules.isVisible(Field.DRAW_LENGTH, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "Draw Length", state.drawLength, callbacks::updateDrawLength,
                        "${formatG(state.drawLength)}\"",
                        min = 17.0, max = 37.0, step = 0.25,
                    )
                }
                if (EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "Let-off", state.letOffPct, callbacks::updateLetOff,
                        "${state.letOffPct.toInt()}%",
                        min = 40.0, max = 99.0, step = 1.0,
                    )
                }
                if (EquipmentFieldRules.isVisible(Field.PEEP_HEIGHT, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "Peep Height", state.peepHeight, callbacks::updatePeepHeight,
                        "${formatG(state.peepHeight)}\"",
                        min = 3.0, max = 17.0, step = 0.1,
                    )
                }
                if (EquipmentFieldRules.isVisible(Field.D_LOOP_LENGTH, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "D-Loop Length", state.dLoopLength, callbacks::updateDLoop,
                        "${formatG(state.dLoopLength)}\"",
                        min = 0.1, max = 5.0, step = 1.0 / 16.0,
                    )
                }
                if (EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, bowType, isSetup, state.rearStabSide)) {
                    DoubleStepperRow(
                        "Brace Height", state.braceHeight, callbacks::updateBraceHeight,
                        "${formatG(state.braceHeight)}\"",
                        min = 5.0, max = 12.0, step = 1.0 / 16.0,
                    )
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
            DoubleStepperRow("Top Limb", state.topLimbTurns, callbacks::updateTopLimb, limbTurnsLabel(state.topLimbTurns), -10.0, 10.0, 0.5)
            DoubleStepperRow("Bottom Limb", state.bottomLimbTurns, callbacks::updateBottomLimb, limbTurnsLabel(state.bottomLimbTurns), -10.0, 10.0, 0.5)
        }
        if (EquipmentFieldRules.sectionVisible(Section.REST, bowType, isSetup)) {
            SectionHeader("Rest")
            IntStepperRow("Vertical", state.restVertical, callbacks::updateRestVertical, sixteenthLabel(state.restVertical), -16, 16)
            IntStepperRow("Horizontal", state.restHorizontal, callbacks::updateRestHorizontal, sixteenthLabel(state.restHorizontal), -16, 16)
            DoubleStepperRow("Depth", state.restDepth, callbacks::updateRestDepth, "${"%.2f".format(state.restDepth)}\"", -5.0, 5.0, 0.25)
        }
        if (EquipmentFieldRules.sectionVisible(Section.SIGHT_GRIP_NOCK, bowType, isSetup)) {
            SectionHeader("Sight, Grip & Nock")
            IntStepperRow("Sight Position", state.sightPosition, callbacks::updateSightPosition, sightPositionLabel(state.sightPosition), -15, 15)
            DoubleStepperRow("Grip Angle", state.gripAngle, callbacks::updateGripAngle, "${"%.1f".format(state.gripAngle)}°", 0.0, 90.0, 0.5)
            IntStepperRow("Nocking Height", state.nockingHeight, callbacks::updateNockingHeight, sixteenthLabel(state.nockingHeight), -80, 80)
        }

        if (EquipmentFieldRules.sectionVisible(Section.TILLER, bowType, isSetup)) {
            SectionHeader("Tiller")
            DoubleStepperRow("Top Tiller", state.tillerTop, callbacks::updateTillerTop, tillerLabel(state.tillerTop), -10.0, 10.0, 0.5)
            DoubleStepperRow("Bottom Tiller", state.tillerBottom, callbacks::updateTillerBottom, tillerLabel(state.tillerBottom), -10.0, 10.0, 0.5)
        }
        if (EquipmentFieldRules.sectionVisible(Section.PLUNGER, bowType, isSetup)) {
            SectionHeader("Plunger")
            IntStepperRow("Tension", state.plungerTension, callbacks::updatePlungerTension, "${state.plungerTension} clicks", 0, 30)
        }
        if (EquipmentFieldRules.sectionVisible(Section.CLICKER, bowType, isSetup)) {
            SectionHeader("Clicker")
            DoubleStepperRow("Position", state.clickerPosition, callbacks::updateClickerPosition, clickerLabel(state.clickerPosition), -50.0, 50.0, 1.0)
        }
        if (EquipmentFieldRules.sectionVisible(Section.GRIP_AND_NOCK, bowType, isSetup)) {
            SectionHeader("Grip & Nock")
            DoubleStepperRow("Grip Angle", state.gripAngle, callbacks::updateGripAngle, "${"%.1f".format(state.gripAngle)}°", 0.0, 90.0, 0.5)
            IntStepperRow("Nocking Height", state.nockingHeight, callbacks::updateNockingHeight, sixteenthLabel(state.nockingHeight), -80, 80)
        }

        if (EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, bowType, isSetup)) {
            SectionHeader("Front Stabilizer")
            val maxWeight = if (bowType == BowType.COMPOUND) 60.0 else 30.0
            val weightLabel = if (state.frontStabWeight == 0.0 && bowType == BowType.COMPOUND) {
                "None"
            } else {
                "${formatG(state.frontStabWeight)} oz"
            }
            DoubleStepperRow("Weight", state.frontStabWeight, callbacks::updateFrontStabWeight, weightLabel, 0.0, maxWeight, 0.5)
            DoubleStepperRow("Angle", state.frontStabAngle, callbacks::updateFrontStabAngle, "${state.frontStabAngle.toInt()}°", 0.0, 10.0, 1.0)
        }

        if (EquipmentFieldRules.sectionVisible(Section.REAR_STAB, bowType, isSetup)) {
            SectionHeader("Rear Stabilizer")
            RearStabSidePicker(selected = state.rearStabSide, onSelect = callbacks::updateRearStabSide)
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_WEIGHT, bowType, isSetup, state.rearStabSide)) {
                DoubleStepperRow(
                    "Weight", state.rearStabWeight, callbacks::updateRearStabWeight,
                    "${formatG(state.rearStabWeight)} oz", 0.0, 60.0, 0.5,
                )
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_VERT_ANGLE, bowType, isSetup, state.rearStabSide)) {
                DoubleStepperRow(
                    "Vertical Angle", state.rearStabVertAngle, callbacks::updateRearStabVertAngle,
                    "${state.rearStabVertAngle.toInt()}°", -90.0, 90.0, 5.0,
                )
            }
            if (EquipmentFieldRules.isVisible(Field.REAR_STAB_HORIZ_ANGLE, bowType, isSetup, state.rearStabSide)) {
                DoubleStepperRow(
                    "Horizontal Angle", state.rearStabHorizAngle, callbacks::updateRearStabHorizAngle,
                    "${state.rearStabHorizAngle.toInt()}°", 0.0, 90.0, 5.0,
                )
            }
        }

        if (EquipmentFieldRules.sectionVisible(Section.V_BAR, bowType, isSetup)) {
            SectionHeader("V-Bar (Rear Stabilizer)")
            DoubleStepperRow("Left Weight", state.rearStabLeftWeight, callbacks::updateRearStabLeftWeight, "${formatG(state.rearStabLeftWeight)} oz", 0.0, 30.0, 0.5)
            DoubleStepperRow("Right Weight", state.rearStabRightWeight, callbacks::updateRearStabRightWeight, "${formatG(state.rearStabRightWeight)} oz", 0.0, 30.0, 0.5)
            DoubleStepperRow("Vertical Angle", state.rearStabVertAngle, callbacks::updateRearStabVertAngle, "${state.rearStabVertAngle.toInt()}°", -90.0, 90.0, 5.0)
            DoubleStepperRow("Horizontal Angle", state.rearStabHorizAngle, callbacks::updateRearStabHorizAngle, "${state.rearStabHorizAngle.toInt()}°", 0.0, 90.0, 5.0)
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
}
