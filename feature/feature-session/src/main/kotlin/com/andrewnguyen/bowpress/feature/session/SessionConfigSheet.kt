package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.designsystem.BowPressColors
import com.andrewnguyen.bowpress.core.designsystem.LocalUnitSystem
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.UnitFormatting

/**
 * Modal bottom sheet for mid-session bow/arrow config changes. Mirrors iOS
 * `SessionConfigSheet.swift` — the archer picks a different bow config / arrow config
 * and taps Confirm; the view model stages it as pending until the next plot commits
 * the switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionConfigSheet(
    state: SessionUiState,
    onDismiss: () -> Unit,
    onConfirm: (bowConfigId: String?, arrowConfigId: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeBowId = state.activeSession?.bowId
    val bowConfigs = activeBowId?.let { state.bowConfigsByBow[it] } ?: emptyList()

    var selectedBowConfigId by remember { mutableStateOf<String?>(state.activeBowConfig?.id) }
    var selectedArrowConfigId by remember { mutableStateOf<String?>(state.activeArrowConfig?.id) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(20.dp)) {
            Text("Change Config", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            val unitSystem = LocalUnitSystem.current
            Text("Bow configuration", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            bowConfigs.forEach { cfg ->
                ConfigRow(
                    title = cfg.label ?: "Config · ${UnitFormatting.length(cfg.drawLength, unitSystem, digits = 2)}",
                    isSelected = cfg.id == selectedBowConfigId,
                    onClick = { selectedBowConfigId = cfg.id },
                )
            }
            if (bowConfigs.isEmpty()) {
                Text(
                    "No saved configs for this bow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Arrow", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            state.arrowConfigs.forEach { arrow ->
                ConfigRow(
                    title = arrow.label,
                    isSelected = arrow.id == selectedArrowConfigId,
                    onClick = { selectedArrowConfigId = arrow.id },
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        val bowId = selectedBowConfigId?.takeIf { it != state.activeBowConfig?.id }
                        val arrowId = selectedArrowConfigId?.takeIf { it != state.activeArrowConfig?.id }
                        onConfirm(bowId, arrowId)
                    },
                ) { Text("Confirm") }
            }
        }
    }
}

@Composable
private fun ConfigRow(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) BowPressColors.Accent else Color.Transparent
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Text(title, modifier = Modifier.padding(12.dp), color = fg)
    }
}
