package com.andrewnguyen.bowpress.feature.equipment.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitRange
import com.andrewnguyen.bowpress.core.model.UnitScale
import com.andrewnguyen.bowpress.core.model.UnitSystem
// InterleavedStepperRow lives in this same package (components/EquipmentComponents.kt)

/**
 * Stepper rows that bind to canonical-unit state but present and nudge in the
 * active display unit. Mirrors the iOS `Binding.displayed(in:scale:)` pattern.
 *
 * The underlying storage value (inches / ounces / grains / mm) never changes
 * when the user flips the toggle — only the label and the nudge step do.
 */

/** Length stored in inches; displayed in inches or cm. */
@Composable
fun LengthStepperRow(
    label: String,
    inches: Double,
    onInchesChange: (Double) -> Unit,
    range: UnitRange,
    unitSystem: UnitSystem,
    digits: Int = 2,
    modifier: Modifier = Modifier,
) {
    // Step + bounds in canonical (inches) space so repeated nudges in metric
    // don't accumulate the 1/2.54 round-trip error from re-deriving the
    // display value on every tap.
    val displayBounds = range.displayRange(unitSystem)
    val minInches = UnitScale.INCH_TO_CM.toCanonical(displayBounds.start, unitSystem)
    val maxInches = UnitScale.INCH_TO_CM.toCanonical(displayBounds.endInclusive, unitSystem)
    val stepInches = UnitScale.INCH_TO_CM.toCanonical(range.displayStep(unitSystem), unitSystem) -
        UnitScale.INCH_TO_CM.toCanonical(0.0, unitSystem)
    InterleavedStepperRow(
        label = label,
        onDecrease = { onInchesChange((inches - stepInches).coerceAtLeast(minInches)) },
        onIncrease = { onInchesChange((inches + stepInches).coerceAtMost(maxInches)) },
        valueText = UnitFormatting.lengthValue(inches, unitSystem, digits),
        unitSuffix = UnitFormatting.lengthSuffix(unitSystem),
        modifier = modifier,
    )
}

/** Arrow mass stored in grains; displayed in grains or grams. */
@Composable
fun ArrowMassStepperRow(
    label: String,
    grains: Int,
    onGrainsChange: (Int) -> Unit,
    range: UnitRange,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val bounds = range.displayRange(unitSystem)
    DoubleStepperRow(
        label = label,
        value = UnitScale.GRAIN_TO_GRAM.toDisplay(grains.toDouble(), unitSystem),
        onChange = {
            onGrainsChange(UnitScale.GRAIN_TO_GRAM.toCanonical(it, unitSystem).toInt())
        },
        valueLabel = UnitFormatting.arrowMass(grains, unitSystem),
        min = bounds.start,
        max = bounds.endInclusive,
        step = range.displayStep(unitSystem),
        modifier = modifier,
    )
}

/** Stabilizer weight stored in ounces; displayed in ounces or grams. */
@Composable
fun StabWeightStepperRow(
    label: String,
    ounces: Double,
    onOuncesChange: (Double) -> Unit,
    range: UnitRange,
    unitSystem: UnitSystem,
    valueLabelOverride: String? = null,
    modifier: Modifier = Modifier,
) {
    val bounds = range.displayRange(unitSystem)
    DoubleStepperRow(
        label = label,
        value = UnitScale.OUNCE_TO_GRAM.toDisplay(ounces, unitSystem),
        onChange = { onOuncesChange(UnitScale.OUNCE_TO_GRAM.toCanonical(it, unitSystem)) },
        valueLabel = valueLabelOverride ?: UnitFormatting.stabWeight(ounces, unitSystem),
        min = bounds.start,
        max = bounds.endInclusive,
        step = range.displayStep(unitSystem),
        modifier = modifier,
    )
}

/** Value stored in mm (tiller, clicker); displayed in mm or inches. */
@Composable
fun MmLengthStepperRow(
    label: String,
    mm: Double,
    onMmChange: (Double) -> Unit,
    range: UnitRange,
    unitSystem: UnitSystem,
    digits: Int = 1,
    modifier: Modifier = Modifier,
) {
    val bounds = range.displayRange(unitSystem)
    DoubleStepperRow(
        label = label,
        value = UnitScale.MM_TO_INCH.toDisplay(mm, unitSystem),
        onChange = { onMmChange(UnitScale.MM_TO_INCH.toCanonical(it, unitSystem)) },
        valueLabel = UnitFormatting.mmLength(mm, unitSystem, digits),
        min = bounds.start,
        max = bounds.endInclusive,
        step = range.displayStep(unitSystem),
        modifier = modifier,
    )
}
