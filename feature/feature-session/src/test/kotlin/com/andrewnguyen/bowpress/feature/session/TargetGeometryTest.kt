package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ring/zone parity tests. These are the guardrails that keep the Android scorer in sync
 * with the iOS `TargetGeometry` at bowpress-ios/.../TargetPlotView.swift:3–56. Every input
 * here is written in the persisted (plotX, plotY) convention (plotY is south-positive).
 */
class TargetGeometryTest {

    // ---- Ring boundaries (4 required, plus X/miss for completeness) ----

    @Test fun `ring is X when inside xRadius`() {
        // Anywhere well inside the inner-yellow disc is X (11).
        val classification = TargetGeometry.classify(plotX = 0.00, plotY = 0.00)
        assertThat(classification.ring).isEqualTo(11)
        assertThat(classification.zone).isEqualTo(Zone.CENTER)
    }

    @Test fun `ring 10 just outside xRadius`() {
        // Just past 60/735 ≈ 0.0816 → ring 10.
        val r = TargetGeometry.X_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(10)
    }

    @Test fun `ring 9 just outside r10Radius`() {
        val r = TargetGeometry.R10_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(9)
    }

    @Test fun `ring 8 just outside r9Radius`() {
        val r = TargetGeometry.R9_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(8)
    }

    @Test fun `ring 7 just outside r8Radius`() {
        val r = TargetGeometry.R8_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(7)
    }

    @Test fun `ring 6 just outside r7Radius`() {
        val r = TargetGeometry.R7_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(6)
    }

    @Test fun `null ring outside r6Radius`() {
        val r = TargetGeometry.R6_RADIUS + 0.001
        val c = TargetGeometry.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isNull()
    }

    // ---- Eight zones (one point per octant, placed in ring 8 so radius > CENTER) ----
    // Zone is computed via compass bearing (N=0°, CW). To synthesize a plot at a given
    // compass bearing, we convert to the persisted screen-y-down convention:
    //   bearing -> (plotX = r * sin(bearing), plotY = -r * cos(bearing))
    // Derivation: math angle θ = 90° - bearing, math-x = r*cos θ, math-y = r*sin θ,
    // and plotY = -math-y (iOS line 165).

    private fun atBearing(bearingDeg: Double, radius: Double): Pair<Double, Double> {
        val rad = Math.toRadians(bearingDeg)
        val plotX = radius * sin(rad)
        val plotY = -radius * cos(rad)
        return plotX to plotY
    }

    @Test fun `zone N at compass bearing 0`() {
        val (x, y) = atBearing(0.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `zone NE at compass bearing 45`() {
        val (x, y) = atBearing(45.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.NE)
    }

    @Test fun `zone E at compass bearing 90`() {
        val (x, y) = atBearing(90.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.E)
    }

    @Test fun `zone SE at compass bearing 135`() {
        val (x, y) = atBearing(135.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.SE)
    }

    @Test fun `zone S at compass bearing 180`() {
        val (x, y) = atBearing(180.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.S)
    }

    @Test fun `zone SW at compass bearing 225`() {
        val (x, y) = atBearing(225.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.SW)
    }

    @Test fun `zone W at compass bearing 270`() {
        val (x, y) = atBearing(270.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.W)
    }

    @Test fun `zone NW at compass bearing 315`() {
        val (x, y) = atBearing(315.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.NW)
    }

    // ---- CENTER ----

    @Test fun `center zone inside centerZoneRadius regardless of angle`() {
        val r = TargetGeometry.CENTER_ZONE_RADIUS - 0.001
        // Arbitrary non-axis-aligned direction
        val c = TargetGeometry.classify(plotX = r * 0.7, plotY = r * 0.3)
        assertThat(c.zone).isEqualTo(Zone.CENTER)
    }

    // ---- Dot-radius scoring (parity with iOS lines 155–158) ----

    @Test fun `arrow dot touching higher ring scores the higher ring`() {
        // Place plot just inside ring 9; a dot radius that reaches back into ring 10
        // should upgrade the score.
        val radius = TargetGeometry.R10_RADIUS + 0.01
        val c = TargetGeometry.classifyWithDotRadius(
            plotX = radius,
            plotY = 0.0,
            dotNormRadius = 0.02,
        )
        assertThat(c.ring).isEqualTo(10)
    }

    // ---- Octant boundary: half-open intervals match iOS exactly ----
    //
    // Note: we test just past each boundary (not *exactly* on it) because the chain
    //   toRadians → sin/cos → atan2 → toDegrees
    // introduces a few ULPs of drift that could push an on-boundary input to the
    // wrong side of a `<` comparison. The semantic we actually care about is that
    // the interval is half-open at 22.5/337.5/etc, and these tests verify that.

    @Test fun `just past 22_5 boundary belongs to NE`() {
        // iOS switch: `case 22.5..<67.5: .ne`.
        val (x, y) = atBearing(23.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.NE)
    }

    @Test fun `just before 22_5 boundary belongs to N`() {
        val (x, y) = atBearing(22.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `just past 337_5 boundary belongs to N`() {
        // iOS: `case 337.5..<360, 0..<22.5: .n`.
        val (x, y) = atBearing(338.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `just before 337_5 boundary belongs to NW`() {
        val (x, y) = atBearing(337.0, 0.4)
        assertThat(TargetGeometry.classify(x, y).zone).isEqualTo(Zone.NW)
    }

    // ---- iOS fixture parity ----
    //
    // (plotX, plotY) sampled from bowpress-ios DevMockData.swift:484–485 — the "low-grouping"
    // archer. These are the coords the iOS app writes to the DB, so classifying them
    // here proves round-trip parity with iOS scoring.

    @Test fun `iOS fixture 0_015 0_095 classifies as ring 10 zone S`() {
        // Low and just right of centre → outer 10, shots low on the face → south.
        val c = TargetGeometry.classify(plotX = 0.015, plotY = 0.095)
        assertThat(c.ring).isEqualTo(10)
        assertThat(c.zone).isEqualTo(Zone.S)
    }

    @Test fun `iOS fixture negative plotY classifies in northern zones`() {
        // Flip the above — a shot high on the face → north. iOS sets plotY negative
        // for shots above centre (TargetPlotView.swift:165).
        val c = TargetGeometry.classify(plotX = 0.015, plotY = -0.095)
        assertThat(c.ring).isEqualTo(10)
        assertThat(c.zone).isEqualTo(Zone.N)
    }
}
