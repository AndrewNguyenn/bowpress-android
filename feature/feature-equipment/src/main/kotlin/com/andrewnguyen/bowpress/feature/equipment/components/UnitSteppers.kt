package com.andrewnguyen.bowpress.feature.equipment.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrewnguyen.bowpress.core.model.UnitFormatting
import com.andrewnguyen.bowpress.core.model.UnitRange
import com.andrewnguyen.bowpress.core.model.UnitScale
import com.andrewnguyen.bowpress.core.model.UnitSystem

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
    val bounds = range.displayRange(unitSystem)
    DoubleStepperRow(
        label = label,
        value = UnitScale.INCH_TO_CM.toDisplay(inches, unitSystem),
        onChange = { onInchesChange(UnitScale.INCH_TO_CM.toCanonical(it, unitSystem)) },
        valueLabel = UnitFormatting.length(inches, unitSystem, digits),
        min = bounds.start,
        max = bounds.endInclusive,
        step = range.displayStep(unitSystem),
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
