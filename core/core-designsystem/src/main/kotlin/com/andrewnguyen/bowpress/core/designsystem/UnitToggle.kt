package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.UnitSystem

/**
 * The currently active unit system, observed by every screen. The app root
 * collects the preference Flow once and publishes it through this local so
 * every composable can read it without threading the value through props.
 *
 * `IMPERIAL` is the fallback so UI that runs before the preferences Flow
 * emits (e.g. early previews) matches the stored default.
 */
val LocalUnitSystem: ProvidableCompositionLocal<UnitSystem> =
    staticCompositionLocalOf { UnitSystem.DEFAULT }

/**
 * Side-effecting setter for the unit system. Backed by
 * `UnitPreferencesRepository.setUnitSystem` at the app root.
 */
val LocalUnitSystemSetter: ProvidableCompositionLocal<(UnitSystem) -> Unit> =
    staticCompositionLocalOf { { /* no-op in previews */ } }

/**
 * Segmented control for swapping imperial ↔ metric. Sits at the top of every
 * configuration surface; mirrors iOS `Picker(.segmented)`: rounded tray with
 * a lighter pill behind the selected option, no check icon, dark ink for
 * both states.
 */
@Composable
fun UnitToggle(
    system: UnitSystem,
    onSystemChange: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("unit_system_toggle")
            .semantics { contentDescription = "Unit system toggle" },
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(AppLine2)
                .padding(2.dp),
        ) {
            UnitSystem.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = system == option,
                    onClick = { onSystemChange(option) },
                    shape = RoundedCornerShape(50),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = AppCream,
                        activeContentColor = AppInk,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = AppInk,
                    ),
                    border = BorderStroke(0.dp, Color.Transparent),
                    icon = {},
                ) { Text(option.label) }
            }
        }
    }
}
