package com.andrewnguyen.bowpress.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Mirrors the iOS UnitFormattingTests suite — round-trips, parser symmetry,
 * sixteenths exactness, shaft-diameter parsing, UnitRange sanity.
 */
class UnitFormattingTest {

    // ── Length (inches ↔ cm) ──────────────────────────────────────────

    @Test
    fun `length imperial render matches quoted inches`() {
        assertThat(UnitFormatting.length(28.5, UnitSystem.IMPERIAL)).isEqualTo("28.5\"")
        assertThat(UnitFormatting.length(28.0, UnitSystem.IMPERIAL)).isEqualTo("28\"")
        assertThat(UnitFormatting.length(2.125, UnitSystem.IMPERIAL, digits = 3)).isEqualTo("2.125\"")
    }

    @Test
    fun `length metric render uses cm`() {
        assertThat(UnitFormatting.length(28.5, UnitSystem.METRIC)).isEqualTo("72.4 cm")
        assertThat(UnitFormatting.length(10.0, UnitSystem.METRIC)).isEqualTo("25.4 cm")
    }

    @Test
    fun `parse imperial returns inches unchanged`() {
        assertThat(UnitFormatting.parseLength("28.5", UnitSystem.IMPERIAL)).isEqualTo(28.5)
        assertThat(UnitFormatting.parseLength("nonsense", UnitSystem.IMPERIAL)).isNull()
    }

    @Test
    fun `parse metric converts back to inches`() {
        val inches = UnitFormatting.parseLength("72.39", UnitSystem.METRIC) ?: -1.0
        assertThat(abs(inches - 28.5)).isLessThan(0.01)
    }

    @Test
    fun `length round-trips within tolerance`() {
        // Imperial: exact round-trip at 3 decimals.
        // Metric:  1-decimal cm display loses ~1 mm (0.02") — allow for it.
        var canonical = 5.0
        while (canonical <= 35.0) {
            val impRendered = UnitFormatting.lengthValue(canonical, UnitSystem.IMPERIAL, digits = 3)
            val impBack = UnitFormatting.parseLength(impRendered, UnitSystem.IMPERIAL)!!
            assertThat(abs(impBack - canonical)).isLessThan(0.001)

            val metRendered = UnitFormatting.lengthValue(canonical, UnitSystem.METRIC)
            val metBack = UnitFormatting.parseLength(metRendered, UnitSystem.METRIC)!!
            assertThat(abs(metBack - canonical)).isLessThan(0.02)

            canonical += 0.5
        }
    }

    // ── Sixteenths (stored as Int 1/16") ──────────────────────────────

    @Test
    fun `sixteenths imperial render exact`() {
        assertThat(UnitFormatting.sixteenths(3, UnitSystem.IMPERIAL)).isEqualTo("+3/16\"")
        assertThat(UnitFormatting.sixteenths(-7, UnitSystem.IMPERIAL)).isEqualTo("-7/16\"")
        assertThat(UnitFormatting.sixteenths(0, UnitSystem.IMPERIAL)).isEqualTo("0/16\"")
    }

    @Test
    fun `sixteenths metric rounds to tenth of mm`() {
        assertThat(UnitFormatting.sixteenths(3, UnitSystem.METRIC)).isEqualTo("+4.8 mm")
        assertThat(UnitFormatting.sixteenths(16, UnitSystem.METRIC)).isEqualTo("+25.4 mm")
        assertThat(UnitFormatting.sixteenths(-16, UnitSystem.METRIC)).isEqualTo("-25.4 mm")
    }

    // ── MM length (tiller, clicker — stored in mm) ────────────────────

    @Test
    fun `mmLength metric preserves mm`() {
        assertThat(UnitFormatting.mmLength(2.0, UnitSystem.METRIC)).isEqualTo("+2 mm")
        assertThat(UnitFormatting.mmLength(-2.5, UnitSystem.METRIC)).isEqualTo("-2.5 mm")
        assertThat(UnitFormatting.mmLength(0.0, UnitSystem.METRIC)).isEqualTo("0 mm")
    }

    @Test
    fun `mmLength imperial converts to inches`() {
        val rendered = UnitFormatting.mmLength(25.4, UnitSystem.IMPERIAL)
        assertThat(rendered).endsWith("\"")
        assertThat(rendered).contains("1")
    }

    // ── Arrow mass (grains ↔ grams) ───────────────────────────────────

    @Test
    fun `arrowMass imperial renders grains`() {
        assertThat(UnitFormatting.arrowMass(110, UnitSystem.IMPERIAL)).isEqualTo("110 gr")
    }

    @Test
    fun `arrowMass metric renders grams`() {
        assertThat(UnitFormatting.arrowMass(110, UnitSystem.METRIC)).isEqualTo("7.1 g")
        assertThat(UnitFormatting.arrowMass(0, UnitSystem.METRIC)).isEqualTo("0 g")
    }

    @Test
    fun `arrowMass metric round-trip stays within one grain`() {
        for (grains in listOf(50, 100, 110, 150, 220, 300)) {
            val text = UnitFormatting.arrowMassValue(grains, UnitSystem.METRIC)
            val back = UnitFormatting.parseArrowMass(text, UnitSystem.METRIC)!!
            assertThat(abs(back - grains)).isAtMost(1)
        }
    }

    // ── Stabilizer weight (oz ↔ g) ────────────────────────────────────

    @Test
    fun `stabWeight imperial shows oz`() {
        assertThat(UnitFormatting.stabWeight(6.0, UnitSystem.IMPERIAL)).isEqualTo("6 oz")
        assertThat(UnitFormatting.stabWeight(0.5, UnitSystem.IMPERIAL)).isEqualTo("0.5 oz")
    }

    @Test
    fun `stabWeight metric shows grams as integers`() {
        assertThat(UnitFormatting.stabWeight(6.0, UnitSystem.METRIC)).isEqualTo("170 g")
        assertThat(UnitFormatting.stabWeight(12.0, UnitSystem.METRIC)).isEqualTo("340 g")
    }

    // ── Unit-less ─────────────────────────────────────────────────────

    @Test
    fun `degrees and percent render consistently`() {
        assertThat(UnitFormatting.degrees(5.0, digits = 0)).isEqualTo("5°")
        assertThat(UnitFormatting.degrees(5.5)).isEqualTo("5.5°")
        assertThat(UnitFormatting.percent(80.0)).isEqualTo("80%")
        assertThat(UnitFormatting.percent(79.7)).isEqualTo("80%")
    }

    // ── UnitScale round-trip ──────────────────────────────────────────

    @Test
    fun `unitScale inchToCm round-trips exact`() {
        var inches = 1.0
        while (inches <= 40.0) {
            val cm = UnitScale.INCH_TO_CM.toDisplay(inches, UnitSystem.METRIC)
            val back = UnitScale.INCH_TO_CM.toCanonical(cm, UnitSystem.METRIC)
            assertThat(abs(back - inches)).isLessThan(0.0001)
            inches += 0.25
        }
    }

    @Test
    fun `unitScale identity on imperial`() {
        assertThat(UnitScale.INCH_TO_CM.toDisplay(28.5, UnitSystem.IMPERIAL)).isEqualTo(28.5)
        assertThat(UnitScale.OUNCE_TO_GRAM.toDisplay(6.0, UnitSystem.IMPERIAL)).isEqualTo(6.0)
    }

    // ── Shaft diameter (free input) ───────────────────────────────────

    @Test
    fun `shaftDiameter parses fraction as inches`() {
        // 30/64" = 11.90625 mm — also the upper bound.
        assertThat(UnitFormatting.parseShaftDiameter("30/64", UnitSystem.METRIC)!!)
            .isWithin(1e-9).of(11.90625)
        assertThat(UnitFormatting.parseShaftDiameter("19/64", UnitSystem.IMPERIAL)!!)
            .isWithin(1e-9).of(7.540625)
    }

    @Test
    fun `shaftDiameter bare number uses active system`() {
        assertThat(UnitFormatting.parseShaftDiameter("9", UnitSystem.METRIC)!!)
            .isWithin(1e-9).of(9.0)
        assertThat(UnitFormatting.parseShaftDiameter("0.3", UnitSystem.IMPERIAL)!!)
            .isWithin(1e-9).of(0.3 * 25.4)
    }

    @Test
    fun `shaftDiameter explicit suffix overrides system`() {
        assertThat(UnitFormatting.parseShaftDiameter("9mm", UnitSystem.IMPERIAL)!!)
            .isWithin(1e-9).of(9.0)
        assertThat(UnitFormatting.parseShaftDiameter("0.3in", UnitSystem.METRIC)!!)
            .isWithin(1e-9).of(0.3 * 25.4)
        assertThat(UnitFormatting.parseShaftDiameter("0.3\"", UnitSystem.METRIC)!!)
            .isWithin(1e-9).of(0.3 * 25.4)
        assertThat(UnitFormatting.parseShaftDiameter("1cm", UnitSystem.IMPERIAL)!!)
            .isWithin(1e-9).of(10.0)
    }

    @Test
    fun `shaftDiameter rejects garbage`() {
        assertThat(UnitFormatting.parseShaftDiameter("", UnitSystem.METRIC)).isNull()
        assertThat(UnitFormatting.parseShaftDiameter("abc", UnitSystem.METRIC)).isNull()
        assertThat(UnitFormatting.parseShaftDiameter("5/0", UnitSystem.METRIC)).isNull()
        assertThat(UnitFormatting.parseShaftDiameter("5/", UnitSystem.METRIC)).isNull()
        assertThat(UnitFormatting.parseShaftDiameter("/64", UnitSystem.IMPERIAL)).isNull()
    }

    @Test
    fun `shaftDiameter boundary round-trips stay valid`() {
        // The 30/64" max and 1 mm min lose a few thousandths to display
        // rounding; re-validating the displayed text must still pass.
        for (system in listOf(UnitSystem.IMPERIAL, UnitSystem.METRIC)) {
            for (bound in listOf(
                UnitFormatting.SHAFT_DIAMETER_RANGE_MM.start,
                UnitFormatting.SHAFT_DIAMETER_RANGE_MM.endInclusive,
            )) {
                val text = UnitFormatting.shaftDiameterValue(bound, system)
                assertThat(UnitFormatting.validateShaftDiameter(text, system))
                    .isInstanceOf(ShaftDiameterValidation.Valid::class.java)
            }
        }
    }

    @Test
    fun `shaftDiameter validation accepts in-range values`() {
        assertThat(UnitFormatting.validateShaftDiameter("", UnitSystem.METRIC))
            .isEqualTo(ShaftDiameterValidation.Empty)
        assertThat(UnitFormatting.validateShaftDiameter("9", UnitSystem.METRIC))
            .isEqualTo(ShaftDiameterValidation.Valid(9.0))
        assertThat(UnitFormatting.validateShaftDiameter("30/64", UnitSystem.IMPERIAL))
            .isEqualTo(ShaftDiameterValidation.Valid(11.90625))
    }

    @Test
    fun `shaftDiameter validation rejects out-of-range values`() {
        assertThat(UnitFormatting.validateShaftDiameter("31/64", UnitSystem.IMPERIAL))
            .isInstanceOf(ShaftDiameterValidation.Invalid::class.java)
        assertThat(UnitFormatting.validateShaftDiameter("0.5", UnitSystem.METRIC))
            .isInstanceOf(ShaftDiameterValidation.Invalid::class.java)
        assertThat(UnitFormatting.validateShaftDiameter("nonsense", UnitSystem.METRIC))
            .isInstanceOf(ShaftDiameterValidation.Invalid::class.java)
    }

    @Test
    fun `shaftDiameter value round-trips through text`() {
        for (system in listOf(UnitSystem.IMPERIAL, UnitSystem.METRIC)) {
            val text = UnitFormatting.shaftDiameterValue(9.5, system)
            assertThat(UnitFormatting.parseShaftDiameter(text, system)!!)
                .isWithin(0.01).of(9.5)
        }
    }

    // ── UnitRange ─────────────────────────────────────────────────────

    @Test
    fun `unitRange drawLength imperial and metric agree`() {
        val imp = UnitRange.DRAW_LENGTH.displayRange(UnitSystem.IMPERIAL)
        val met = UnitRange.DRAW_LENGTH.displayRange(UnitSystem.METRIC)
        assertThat(abs(imp.start * UnitConversion.INCH_TO_CM - met.start)).isLessThan(0.5)
        assertThat(abs(imp.endInclusive * UnitConversion.INCH_TO_CM - met.endInclusive)).isLessThan(0.5)
    }

    @Test
    fun `unitRange steps differ per system`() {
        assertThat(UnitRange.DRAW_LENGTH.displayStep(UnitSystem.IMPERIAL))
            .isNotEqualTo(UnitRange.DRAW_LENGTH.displayStep(UnitSystem.METRIC))
        assertThat(UnitRange.FRONT_STAB_WEIGHT.displayStep(UnitSystem.IMPERIAL))
            .isNotEqualTo(UnitRange.FRONT_STAB_WEIGHT.displayStep(UnitSystem.METRIC))
    }
}
