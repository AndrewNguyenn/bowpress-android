package com.andrewnguyen.bowpress.core.model

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// ─── Raw conversion constants ──────────────────────────────────────────────

object UnitConversion {
    const val INCH_TO_CM: Double = 2.54
    const val INCH_TO_MM: Double = 25.4
    const val OUNCE_TO_GRAM: Double = 28.349523125
    const val GRAIN_TO_GRAM: Double = 0.06479891
}

// ─── Display formatting + parsing ──────────────────────────────────────────

/** Outcome of validating the editable shaft-diameter field. */
sealed interface ShaftDiameterValidation {
    /** Field blank — diameter unset. */
    data object Empty : ShaftDiameterValidation
    /** In-range value, millimetres. */
    data class Valid(val mm: Double) : ShaftDiameterValidation
    /** User-facing error message. */
    data class Invalid(val message: String) : ShaftDiameterValidation
}

object UnitFormatting {

    // ── Length (storage: inches, Double) ───────────────────────────────────

    /** Renders a length stored in inches in the user's current system. */
    fun length(inches: Double, system: UnitSystem, digits: Int = 2): String = when (system) {
        UnitSystem.IMPERIAL -> "${trimTrailingZeros(inches, digits)}\""
        UnitSystem.METRIC -> "${trimTrailingZeros(inches * UnitConversion.INCH_TO_CM, 1)} cm"
    }

    /** Length display without the unit suffix — used where the suffix is drawn separately. */
    fun lengthValue(inches: Double, system: UnitSystem, digits: Int = 2): String = when (system) {
        UnitSystem.IMPERIAL -> trimTrailingZeros(inches, digits)
        UnitSystem.METRIC -> trimTrailingZeros(inches * UnitConversion.INCH_TO_CM, 1)
    }

    fun lengthSuffix(system: UnitSystem): String = if (system == UnitSystem.IMPERIAL) "\"" else "cm"

    /** Parses a user-entered length in the active system and returns canonical inches. */
    fun parseLength(text: String, system: UnitSystem): Double? {
        val v = text.trim().toDoubleOrNull() ?: return null
        return when (system) {
            UnitSystem.IMPERIAL -> v
            UnitSystem.METRIC -> v / UnitConversion.INCH_TO_CM
        }
    }

    // ── Sixteenths (storage: Int 1/16") ────────────────────────────────────

    fun sixteenths(n: Int, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> when {
            n == 0 -> "0/16\""
            n > 0 -> "+${n}/16\""
            else -> "-${-n}/16\""
        }
        UnitSystem.METRIC -> {
            if (n == 0) {
                "0 mm"
            } else {
                val mm = n * UnitConversion.INCH_TO_MM / 16.0
                val sign = if (mm > 0) "+" else ""
                "$sign${trimTrailingZeros(mm, 1)} mm"
            }
        }
    }

    // ── MM length (storage: mm, Double — tiller / clicker) ─────────────────

    fun mmLength(mm: Double, system: UnitSystem, digits: Int = 1): String = when (system) {
        UnitSystem.IMPERIAL -> {
            val inches = mm / UnitConversion.INCH_TO_MM
            val sign = if (inches > 0) "+" else ""
            "$sign${trimTrailingZeros(inches, 2)}\""
        }
        UnitSystem.METRIC -> {
            val sign = if (mm > 0) "+" else ""
            "$sign${trimTrailingZeros(mm, digits)} mm"
        }
    }

    // ── Shaft diameter (storage: mm, Double — free input) ──────────────────

    /** Allowed arrow shaft (outside) diameter range, in millimetres: 1 mm … 30/64". */
    val SHAFT_DIAMETER_RANGE_MM: ClosedFloatingPointRange<Double> =
        1.0..(30.0 / 64.0 * UnitConversion.INCH_TO_MM)

    /**
     * Editable text for a diameter stored in millimetres — rendered in the
     * active system's unit, without a suffix (drawn separately).
     */
    fun shaftDiameterValue(mm: Double, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> trimTrailingZeros(mm / UnitConversion.INCH_TO_MM, 3)
        UnitSystem.METRIC -> trimTrailingZeros(mm, 2)
    }

    /** Unit suffix shown beside the diameter field. */
    fun shaftDiameterSuffix(system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "\"" else "mm"

    private enum class ShaftUnit { MM, CM, INCH }

    /**
     * Parses a user-entered shaft diameter and returns the value in millimetres,
     * or null if the text is empty or cannot be parsed.
     *
     * Accepts fractions (`30/64`), decimals, and explicit unit suffixes
     * (`mm`, `cm`, `in`, `"`). A bare number is read in the active system's
     * unit; a bare fraction is always inches (archery convention). The result
     * is not range-checked — callers compare against [SHAFT_DIAMETER_RANGE_MM].
     */
    fun parseShaftDiameter(text: String, system: UnitSystem): Double? {
        var s = text.trim().lowercase()
        if (s.isEmpty()) return null

        // An explicit unit suffix overrides the active system.
        var unit: ShaftUnit? = null
        when {
            s.endsWith("mm") -> { unit = ShaftUnit.MM; s = s.dropLast(2) }
            s.endsWith("cm") -> { unit = ShaftUnit.CM; s = s.dropLast(2) }
            s.endsWith("in") -> { unit = ShaftUnit.INCH; s = s.dropLast(2) }
            s.endsWith("\"") || s.endsWith("”") || s.endsWith("″") -> {
                unit = ShaftUnit.INCH; s = s.dropLast(1)
            }
        }
        s = s.trim()
        if (s.isEmpty()) return null

        val value: Double
        if (s.contains("/")) {
            val parts = s.split("/", limit = 2)
            val num = parts[0].trim().toDoubleOrNull() ?: return null
            val den = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
            if (den == 0.0) return null
            value = num / den
            // A bare fraction with no explicit unit is inches.
            if (unit == null) unit = ShaftUnit.INCH
        } else {
            value = s.toDoubleOrNull() ?: return null
        }

        return when (unit ?: if (system == UnitSystem.IMPERIAL) ShaftUnit.INCH else ShaftUnit.MM) {
            ShaftUnit.MM -> value
            ShaftUnit.CM -> value * 10.0
            ShaftUnit.INCH -> value * UnitConversion.INCH_TO_MM
        }
    }

    // Display rounding (2-dp mm / 3-dp inch) can nudge a boundary value a few
    // thousandths of a millimetre past the range; accept within this tolerance
    // and clamp back, so re-saving a max/min value is never spuriously rejected.
    private const val SHAFT_DIAMETER_TOLERANCE_MM = 0.05

    /**
     * Validates the editable shaft-diameter field for save. A valid result is
     * clamped into [SHAFT_DIAMETER_RANGE_MM] so stored values stay within bounds.
     */
    fun validateShaftDiameter(text: String, system: UnitSystem): ShaftDiameterValidation {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ShaftDiameterValidation.Empty
        val mm = parseShaftDiameter(trimmed, system)
            ?: return ShaftDiameterValidation.Invalid(
                "Enter a valid arrow diameter — e.g. 30/64, 0.46\", or 9 mm.",
            )
        val lo = SHAFT_DIAMETER_RANGE_MM.start
        val hi = SHAFT_DIAMETER_RANGE_MM.endInclusive
        if (mm < lo - SHAFT_DIAMETER_TOLERANCE_MM || mm > hi + SHAFT_DIAMETER_TOLERANCE_MM) {
            return ShaftDiameterValidation.Invalid(
                "Arrow diameter must be between 1 mm and 30/64\" (11.9 mm).",
            )
        }
        return ShaftDiameterValidation.Valid(mm.coerceIn(lo, hi))
    }

    // ── Arrow mass (storage: grains, Int) ──────────────────────────────────

    fun arrowMass(grains: Int, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> "$grains gr"
        UnitSystem.METRIC -> "${trimTrailingZeros(grains * UnitConversion.GRAIN_TO_GRAM, 1)} g"
    }

    fun arrowMassValue(grains: Int, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> grains.toString()
        UnitSystem.METRIC -> trimTrailingZeros(grains * UnitConversion.GRAIN_TO_GRAM, 1)
    }

    fun massSuffix(system: UnitSystem): String = if (system == UnitSystem.IMPERIAL) "gr" else "g"

    fun parseArrowMass(text: String, system: UnitSystem): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return when (system) {
            UnitSystem.IMPERIAL -> trimmed.toIntOrNull()
            UnitSystem.METRIC -> trimmed.toDoubleOrNull()?.let {
                (it / UnitConversion.GRAIN_TO_GRAM).roundToInt()
            }
        }
    }

    // ── Stabilizer weight (storage: ounces, Double) ────────────────────────

    fun stabWeight(ounces: Double, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> "${trimTrailingZeros(ounces, 1)} oz"
        // 10-gram increments are the metric step; drop decimals entirely.
        UnitSystem.METRIC -> "${(ounces * UnitConversion.OUNCE_TO_GRAM).roundToLong()} g"
    }

    // ── Unit-less (degrees / percent) ──────────────────────────────────────

    fun degrees(deg: Double, digits: Int = 1): String =
        "${trimTrailingZeros(deg, digits)}°"

    fun percent(pct: Double): String = "${pct.roundToInt()}%"

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Round to `digits` decimal places, then strip trailing zeros.
     * 28.50 → "28.5", 28.00 → "28", 2.125 → "2.125".
     */
    private fun trimTrailingZeros(value: Double, digits: Int): String {
        val p = 10.0.pow(digits)
        val rounded = Math.round(value * p) / p
        if (rounded == rounded.toLong().toDouble()) return rounded.toLong().toString()
        // Format with up to `digits` decimals, then trim trailing zeros and a
        // dangling decimal point (matches Swift's `%g` output for our range).
        var s = "%.${digits}f".format(rounded)
        if (s.contains('.')) {
            s = s.trimEnd('0').trimEnd('.')
        }
        return s
    }
}

// ─── Unit scales (storage ↔ display conversions for binding wrappers) ──────

/**
 * Describes how a canonical storage unit maps to a display unit when the user
 * flips between imperial and metric. Used by the Compose wrapper to expose a
 * Double state as a display-space value for `SliderLike` stepper bindings.
 */
enum class UnitScale {
    /** Storage is inches; display is inches or cm. */
    INCH_TO_CM,
    /** Storage is grains (Int); display is grains or grams. */
    GRAIN_TO_GRAM,
    /** Storage is ounces; display is ounces or grams. */
    OUNCE_TO_GRAM,
    /** Storage is millimetres; display is millimetres or inches. */
    MM_TO_INCH,
    /** Unit-less — no conversion. */
    IDENTITY;

    fun toDisplay(canonical: Double, system: UnitSystem): Double {
        if (system == UnitSystem.IMPERIAL) {
            // Imperial display == canonical for most; mmToInch flips only in imperial.
            return if (this == MM_TO_INCH) canonical / UnitConversion.INCH_TO_MM else canonical
        }
        return when (this) {
            INCH_TO_CM -> canonical * UnitConversion.INCH_TO_CM
            GRAIN_TO_GRAM -> canonical * UnitConversion.GRAIN_TO_GRAM
            OUNCE_TO_GRAM -> canonical * UnitConversion.OUNCE_TO_GRAM
            MM_TO_INCH -> canonical
            IDENTITY -> canonical
        }
    }

    fun toCanonical(display: Double, system: UnitSystem): Double {
        if (system == UnitSystem.IMPERIAL) {
            return if (this == MM_TO_INCH) display * UnitConversion.INCH_TO_MM else display
        }
        return when (this) {
            INCH_TO_CM -> display / UnitConversion.INCH_TO_CM
            GRAIN_TO_GRAM -> display / UnitConversion.GRAIN_TO_GRAM
            OUNCE_TO_GRAM -> display / UnitConversion.OUNCE_TO_GRAM
            MM_TO_INCH -> display
            IDENTITY -> display
        }
    }
}

// ─── Per-field range + step table ──────────────────────────────────────────

/**
 * Imperial / metric range + step for each stepper field. Step values are chosen
 * per system (not literal conversions) so metric users aren't nudging by awkward
 * `0.635 cm` increments.
 */
enum class UnitRange {
    DRAW_LENGTH, PEEP_HEIGHT, D_LOOP_LENGTH, BRACE_HEIGHT,
    ARROW_LENGTH, FLETCHING_LENGTH, REST_DEPTH,
    POINT_WEIGHT, TOTAL_WEIGHT,
    FLETCHING_OFFSET, GRIP_ANGLE, STAB_ANGLE_SMALL, STAB_ANGLE_LARGE,
    FRONT_STAB_WEIGHT, REAR_STAB_WEIGHT, VBAR_WEIGHT,
    TILLER, CLICKER,
    LET_OFF;

    fun displayRange(system: UnitSystem): ClosedFloatingPointRange<Double> {
        val (imp, met) = ranges
        return if (system == UnitSystem.IMPERIAL) imp else met
    }

    fun displayStep(system: UnitSystem): Double {
        val (imp, met) = steps
        return if (system == UnitSystem.IMPERIAL) imp else met
    }

    private val ranges: Pair<ClosedFloatingPointRange<Double>, ClosedFloatingPointRange<Double>>
        get() = when (this) {
            DRAW_LENGTH      -> 17.0..37.0 to 43.2..94.0
            PEEP_HEIGHT      -> 3.0..17.0 to 7.6..43.2
            D_LOOP_LENGTH    -> 0.1..5.0 to 0.3..12.7
            BRACE_HEIGHT     -> 5.0..12.0 to 12.7..30.5
            ARROW_LENGTH     -> 18.0..36.0 to 45.7..91.4
            FLETCHING_LENGTH -> 1.0..5.0 to 2.5..12.7
            REST_DEPTH       -> (-5.0)..5.0 to (-12.7)..12.7
            POINT_WEIGHT     -> 50.0..300.0 to 3.2..19.4
            TOTAL_WEIGHT     -> 100.0..800.0 to 6.5..51.8
            FLETCHING_OFFSET -> 0.0..10.0 to 0.0..10.0
            GRIP_ANGLE       -> 0.0..90.0 to 0.0..90.0
            STAB_ANGLE_SMALL -> 0.0..10.0 to 0.0..10.0
            STAB_ANGLE_LARGE -> (-90.0)..90.0 to (-90.0)..90.0
            FRONT_STAB_WEIGHT -> 0.0..60.0 to 0.0..1700.0
            REAR_STAB_WEIGHT -> 0.0..60.0 to 0.0..1700.0
            VBAR_WEIGHT      -> 0.0..30.0 to 0.0..850.0
            TILLER           -> (-0.4)..0.4 to (-10.0)..10.0
            CLICKER          -> (-2.0)..2.0 to (-50.0)..50.0
            LET_OFF          -> 40.0..99.0 to 40.0..99.0
        }

    private val steps: Pair<Double, Double>
        get() = when (this) {
            DRAW_LENGTH      -> 0.25 to 0.5
            PEEP_HEIGHT      -> 0.1 to 0.2
            D_LOOP_LENGTH    -> 1.0 / 16 to 0.1
            BRACE_HEIGHT     -> 1.0 / 16 to 0.1
            ARROW_LENGTH     -> 0.25 to 0.5
            FLETCHING_LENGTH -> 0.25 to 0.5
            REST_DEPTH       -> 0.25 to 0.5
            POINT_WEIGHT     -> 5.0 to 0.5
            TOTAL_WEIGHT     -> 1.0 to 0.1
            FLETCHING_OFFSET -> 0.5 to 0.5
            GRIP_ANGLE       -> 0.5 to 0.5
            STAB_ANGLE_SMALL -> 1.0 to 1.0
            STAB_ANGLE_LARGE -> 5.0 to 5.0
            FRONT_STAB_WEIGHT -> 0.5 to 10.0
            REAR_STAB_WEIGHT -> 0.5 to 10.0
            VBAR_WEIGHT      -> 0.5 to 10.0
            TILLER           -> 0.03125 to 0.5
            CLICKER          -> 0.03125 to 1.0
            LET_OFF          -> 1.0 to 1.0
        }
}
