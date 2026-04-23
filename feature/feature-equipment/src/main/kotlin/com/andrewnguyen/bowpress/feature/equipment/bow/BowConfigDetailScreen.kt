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
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Field
import com.andrewnguyen.bowpress.feature.equipment.EquipmentFieldRules.Section
import com.andrewnguyen.bowpress.feature.equipment.components.LabeledValueRow
import com.andrewnguyen.bowpress.feature.equipment.components.SectionHeader
import com.andrewnguyen.bowpress.feature.equipment.components.clickerLabel
import com.andrewnguyen.bowpress.feature.equipment.components.formatG
import com.andrewnguyen.bowpress.feature.equipment.components.halfTwistLabel
import com.andrewnguyen.bowpress.feature.equipment.components.limbTurnsLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sightPositionLabel
import com.andrewnguyen.bowpress.feature.equipment.components.sixteenthLabel
import com.andrewnguyen.bowpress.feature.equipment.components.tillerLabel
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
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ReadOnlyBody(
    bow: com.andrewnguyen.bowpress.core.model.Bow,
    config: BowConfiguration,
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
        SectionHeader("Snapshot")
        LabeledValueRow("Label", config.label ?: if (isSetup) "Initial Setup" else "—")
        LabeledValueRow("Recorded", formatter.format(config.createdAt))
        LabeledValueRow("Type", bowType.label)

        // Bow Setup / Base Setup
        SectionHeader("Bow Setup")
        if (EquipmentFieldRules.isVisible(Field.DRAW_LENGTH, bowType, isSetup, rearStabSide)) {
            LabeledValueRow("Draw Length", "${"%.1f".format(config.drawLength)}\"")
        }
        if (EquipmentFieldRules.isVisible(Field.LET_OFF_PCT, bowType, isSetup = true, rearStabSide)) {
            // On detail we render let-off unconditionally on compound — the value is persisted either way.
            config.letOffPct?.let { LabeledValueRow("Let-off", "${it.toInt()}%") }
        } else if (bowType == BowType.COMPOUND) {
            config.letOffPct?.let { LabeledValueRow("Let-off", "${it.toInt()}%") }
        }
        if (bowType == BowType.COMPOUND) {
            config.peepHeight?.let { LabeledValueRow("Peep Height", "${"%.2f".format(it)}\"") }
            config.dLoopLength?.let { LabeledValueRow("D-Loop Length", "${"%.3f".format(it)}\"") }
        }
        if (EquipmentFieldRules.isVisible(Field.BRACE_HEIGHT, bowType, isSetup, rearStabSide)) {
            config.braceHeight?.let { LabeledValueRow("Brace Height", "${"%.3f".format(it)}\"") }
        }

        if (EquipmentFieldRules.sectionVisible(Section.STRING_AND_CABLE, bowType, isSetup)) {
            SectionHeader("String & Cable")
            LabeledValueRow("Top Cable", halfTwistLabel(config.topCableTwists ?: 0))
            LabeledValueRow("Bottom Cable", halfTwistLabel(config.bottomCableTwists ?: 0))
            LabeledValueRow("Main String Top", halfTwistLabel(config.mainStringTopTwists ?: 0))
            LabeledValueRow("Main String Bottom", halfTwistLabel(config.mainStringBottomTwists ?: 0))
        }
        if (EquipmentFieldRules.sectionVisible(Section.LIMBS, bowType, isSetup)) {
            SectionHeader("Limbs")
            LabeledValueRow("Top Limb", limbTurnsLabel(config.topLimbTurns ?: 0.0))
            LabeledValueRow("Bottom Limb", limbTurnsLabel(config.bottomLimbTurns ?: 0.0))
        }
        if (EquipmentFieldRules.sectionVisible(Section.REST, bowType, isSetup)) {
            SectionHeader("Rest")
            LabeledValueRow("Vertical", sixteenthLabel(config.restVertical))
            LabeledValueRow("Horizontal", sixteenthLabel(config.restHorizontal))
            LabeledValueRow("Depth", "${"%.2f".format(config.restDepth)}\"")
        }
        if (EquipmentFieldRules.sectionVisible(Section.SIGHT_GRIP_NOCK, bowType, isSetup)) {
            SectionHeader("Sight, Grip & Nock")
            LabeledValueRow("Sight Position", sightPositionLabel(config.sightPosition ?: 0))
            LabeledValueRow("Grip Angle", "${"%.1f".format(config.gripAngle)}°")
            LabeledValueRow("Nocking Height", sixteenthLabel(config.nockingHeight))
        }
        if (EquipmentFieldRules.sectionVisible(Section.TILLER, bowType, isSetup)) {
            SectionHeader("Tiller")
            LabeledValueRow("Top", tillerLabel(config.tillerTop ?: 0.0))
            LabeledValueRow("Bottom", tillerLabel(config.tillerBottom ?: 0.0))
        }
        if (EquipmentFieldRules.sectionVisible(Section.PLUNGER, bowType, isSetup)) {
            SectionHeader("Plunger")
            LabeledValueRow("Tension", "${config.plungerTension ?: 0} clicks")
        }
        if (EquipmentFieldRules.sectionVisible(Section.CLICKER, bowType, isSetup)) {
            SectionHeader("Clicker")
            LabeledValueRow("Position", clickerLabel(config.clickerPosition ?: 0.0))
        }
        if (EquipmentFieldRules.sectionVisible(Section.GRIP_AND_NOCK, bowType, isSetup)) {
            SectionHeader("Grip & Nock")
            LabeledValueRow("Grip Angle", "${"%.1f".format(config.gripAngle)}°")
            LabeledValueRow("Nocking Height", sixteenthLabel(config.nockingHeight))
        }
        if (EquipmentFieldRules.sectionVisible(Section.FRONT_STAB, bowType, isSetup)) {
            SectionHeader("Front Stabilizer")
            LabeledValueRow("Weight", "${formatG(config.frontStabWeight ?: 0.0)} oz")
            LabeledValueRow("Angle", "${(config.frontStabAngle ?: 0.0).toInt()}°")
        }
        if (EquipmentFieldRules.sectionVisible(Section.REAR_STAB, bowType, isSetup)) {
            SectionHeader("Rear Stabilizer")
            LabeledValueRow("Side", rearStabSide.label)
            if (rearStabSide != RearStabSide.NONE) {
                LabeledValueRow("Weight", "${formatG(config.rearStabWeight ?: 0.0)} oz")
                LabeledValueRow("Vertical Angle", "${(config.rearStabVertAngle ?: 0.0).toInt()}°")
                LabeledValueRow("Horizontal Angle", "${(config.rearStabHorizAngle ?: 0.0).toInt()}°")
            }
        }
        if (EquipmentFieldRules.sectionVisible(Section.V_BAR, bowType, isSetup)) {
            SectionHeader("V-Bar (Rear Stabilizer)")
            LabeledValueRow("Left Weight", "${formatG(config.rearStabLeftWeight ?: 0.0)} oz")
            LabeledValueRow("Right Weight", "${formatG(config.rearStabRightWeight ?: 0.0)} oz")
            LabeledValueRow("Vertical Angle", "${(config.rearStabVertAngle ?: 0.0).toInt()}°")
            LabeledValueRow("Horizontal Angle", "${(config.rearStabHorizAngle ?: 0.0).toInt()}°")
        }
    }
}
