package com.andrewnguyen.bowpress.feature.equipment.bow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystemSetter
import com.andrewnguyen.bowpress.core.designsystem.UnitToggle
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Field
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Section
import com.andrewnguyen.bowpress.feature.equipment.components.LabeledValueRow
import com.andrewnguyen.bowpress.feature.equipment.components.SectionCard
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import com.andrewnguyen.bowpress.feature.equipment.components.halfTwistLabel
import com.andrewnguyen.bowpress.feature.equipment.components.limbTurnsLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sightPositionLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Read-only rendering of a historic [BowConfiguration]. The section visibility
 * rules come from the same [EquipmentFieldRules] predicate used by the editor,
 * so what's displayed here is by construction the set of fields that were
 * editable at the time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowConfigDetailScreen(
    onBack: () -> Unit,
    onLogNewTuning: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BowConfigDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.config?.label ?: "Tuning Record") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isReference = state.config?.isReference == true
                    IconButton(
                        onClick = { viewModel.toggleReference() },
                        enabled = state.config != null,
                    ) {
                        Icon(
                            imageVector = if (isReference) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isReference) "Unpin reference" else "Pin as reference",
                            tint = if (isReference) BowPressColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onLogNewTuning, enabled = state.config != null) {
                        Text("Log from this")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BowPressColors.Accent)
            }
            state.bow == null || state.config == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Not found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> ReadOnlyBody(
                bow = state.bow!!,
                config = state.config!!,
                unitSystem = LocalUnitSystem.current,
                onUnitSystemChange = LocalUnitSystemSetter.current,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ReadOnlyBody(
    bow: com.andrewnguyen.bowpress.core.model.Bow,
    config: BowConfiguration,
    unitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bowType = bow.bowType
    val isSetup = config.label == "Initial Setup"
    val rearStabSide = config.rearStabSide ?: RearStabSide.NONE
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        UnitToggle(
            system = unitSystem,
            onSystemChange = onUnitSystemChange,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        SectionHeader("Snapshot")
        SectionCard {
            LabeledValueRow("Label", config.label ?: if (isSetup) "Initial Setup" else "—")
            LabeledValueRow("Recorded", formatter.format(config.createdAt))
            LabeledValueRow("Type", bowType.label)
        }

        // Bow Setup / Base Setup
        SectionHeader("Bow Setup")
        SectionCard {
            if (EquipmentFieldRules.isVisible(Field.DRAW_LENGTH, bowType, isSetup, rearStabSide)) {
                LabeledValueRow("Draw Length", UnitFormatting.length(config.drawLength, unitSystem, digits = 1))
            }
            if (bowType == BowType.COMPOUND) {
                config.letOffPct?.let { LabeledValueRow("Let-off", UnitFormatting.percent(it)) }
                config.peepHeight?.let { LabeledValueRow("Peep Height", UnitFormatting.length(it, unitSystem)) }
                config.dLoopLength?.let { LabeledValueRow("D-Loop", UnitFormatting.length(it, unitSystem, digits = 3)) }
            }
            if (EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, bowType, isSetup, rearStabSide)) {
                config.braceHeight?.let { LabeledValueRow("Brace Height", UnitFormatting.length(it, unitSystem, digits = 3)) }
            }
        }

        if (EquipmentFieldRules.sectionVisible(Section.STRING_AND_CABLE, bowType, isSetup)) {
            SectionHeader("String & Cable")
            SectionCard {
                LabeledValueRow("Top Cable", halfTwistLabel(config.topCableTwists ?: 0))
                LabeledValueRow("Bottom Cable", halfTwistLabel(config.bottomCableTwists ?: 0))
                LabeledValueRow("Main String Top", halfTwistLabel(config.mainStringTopTwists ?: 0))
                LabeledValueRow("Main String Bottom", halfTwistLabel(config.mainStringBottomTwists ?: 0))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.LIMBS, bowType, isSetup)) {
            SectionHeader("Limbs")
            SectionCard {
                LabeledValueRow("Top Limb", limbTurnsLabel(config.topLimbTurns ?: 0.0))
                LabeledValueRow("Bottom Limb", limbTurnsLabel(config.bottomLimbTurns ?: 0.0))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.REST, bowType, isSetup)) {
            SectionHeader("Rest")
            SectionCard {
                LabeledValueRow("Vertical", UnitFormatting.sixteenths(config.restVertical, unitSystem))
                LabeledValueRow("Horizontal", UnitFormatting.sixteenths(config.restHorizontal, unitSystem))
                LabeledValueRow("Depth", UnitFormatting.length(config.restDepth, unitSystem))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.SIGHT_GRIP_NOCK, bowType, isSetup)) {
            SectionHeader("Sight, Grip & Nock")
            SectionCard {
                LabeledValueRow("Sight Position", sightPositionLabel(config.sightPosition ?: 0))
                // Pin distance is optional — only render the row when it's set.
                config.sightPinDistance?.let {
                    LabeledValueRow("Pin Distance", UnitFormatting.length(it, unitSystem))
                }
                LabeledValueRow("Grip Angle", UnitFormatting.degrees(config.gripAngle))
                LabeledValueRow("Nocking Height", UnitFormatting.sixteenths(config.nockingHeight, unitSystem))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.TILLER, bowType, isSetup)) {
            SectionHeader("Tiller")
            SectionCard {
                LabeledValueRow("Top", UnitFormatting.mmLength(config.tillerTop ?: 0.0, unitSystem))
                LabeledValueRow("Bottom", UnitFormatting.mmLength(config.tillerBottom ?: 0.0, unitSystem))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.PLUNGER, bowType, isSetup)) {
            SectionHeader("Plunger")
            SectionCard {
                LabeledValueRow("Tension", "${config.plungerTension ?: 0} clicks")
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.CLICKER, bowType, isSetup)) {
            SectionHeader("Clicker")
            SectionCard {
                LabeledValueRow("Position", UnitFormatting.mmLength(config.clickerPosition ?: 0.0, unitSystem, digits = 0))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.GRIP_AND_NOCK, bowType, isSetup)) {
            SectionHeader("Grip & Nock")
            SectionCard {
                LabeledValueRow("Grip Angle", UnitFormatting.degrees(config.gripAngle))
                LabeledValueRow("Nocking Height", UnitFormatting.sixteenths(config.nockingHeight, unitSystem))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, bowType, isSetup)) {
            SectionHeader("Front Stabilizer")
            SectionCard {
                LabeledValueRow("Weight", UnitFormatting.stabWeight(config.frontStabWeight ?: 0.0, unitSystem))
                LabeledValueRow("Angle", UnitFormatting.degrees(config.frontStabAngle ?: 0.0, digits = 0))
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.REAR_STAB, bowType, isSetup)) {
            SectionHeader("Rear Stabilizer")
            SectionCard {
                LabeledValueRow("Side", rearStabSide.label)
                if (rearStabSide != RearStabSide.NONE) {
                    if (rearStabSide == RearStabSide.BOTH) {
                        LabeledValueRow("Left Weight", UnitFormatting.stabWeight(config.rearStabLeftWeight ?: 0.0, unitSystem))
                        LabeledValueRow("Right Weight", UnitFormatting.stabWeight(config.rearStabRightWeight ?: 0.0, unitSystem))
                    } else {
                        LabeledValueRow("Weight", UnitFormatting.stabWeight(config.rearStabWeight ?: 0.0, unitSystem))
                    }
                    LabeledValueRow("Vertical Angle", UnitFormatting.degrees(config.rearStabVertAngle ?: 0.0, digits = 0))
                    LabeledValueRow("Horizontal Angle", UnitFormatting.degrees(config.rearStabHorizAngle ?: 0.0, digits = 0))
                }
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.V_BAR, bowType, isSetup)) {
            SectionHeader("V-Bar (Rear Stabilizer)")
            SectionCard {
                LabeledValueRow("Left Weight", UnitFormatting.stabWeight(config.rearStabLeftWeight ?: 0.0, unitSystem))
                LabeledValueRow("Right Weight", UnitFormatting.stabWeight(config.rearStabRightWeight ?: 0.0, unitSystem))
                LabeledValueRow("Vertical Angle", UnitFormatting.degrees(config.rearStabVertAngle ?: 0.0, digits = 0))
                LabeledValueRow("Horizontal Angle", UnitFormatting.degrees(config.rearStabHorizAngle ?: 0.0, digits = 0))
            }
        }
    }
}
