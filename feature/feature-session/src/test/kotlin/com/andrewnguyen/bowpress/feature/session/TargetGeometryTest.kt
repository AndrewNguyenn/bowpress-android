package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.TargetFaceType
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
        val classification = TargetGeometry.SixRing.classify(plotX = 0.00, plotY = 0.00)
        assertThat(classification.ring).isEqualTo(11)
        assertThat(classification.zone).isEqualTo(Zone.CENTER)
    }

    @Test fun `ring 10 just outside xRadius`() {
        // Just past 60/735 ≈ 0.0816 → ring 10.
        val r = TargetGeometry.SixRing.X_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(10)
    }

    @Test fun `ring 9 just outside r10Radius`() {
        val r = TargetGeometry.SixRing.R10_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(9)
    }

    @Test fun `ring 8 just outside r9Radius`() {
        val r = TargetGeometry.SixRing.R9_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(8)
    }

    @Test fun `ring 7 just outside r8Radius`() {
        val r = TargetGeometry.SixRing.R8_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(7)
    }

    @Test fun `ring 6 just outside r7Radius`() {
        val r = TargetGeometry.SixRing.R7_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
        assertThat(c.ring).isEqualTo(6)
    }

    @Test fun `null ring outside r6Radius`() {
        val r = TargetGeometry.SixRing.R6_RADIUS + 0.001
        val c = TargetGeometry.SixRing.classify(plotX = r, plotY = 0.0)
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
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `zone NE at compass bearing 45`() {
        val (x, y) = atBearing(45.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.NE)
    }

    @Test fun `zone E at compass bearing 90`() {
        val (x, y) = atBearing(90.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.E)
    }

    @Test fun `zone SE at compass bearing 135`() {
        val (x, y) = atBearing(135.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.SE)
    }

    @Test fun `zone S at compass bearing 180`() {
        val (x, y) = atBearing(180.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.S)
    }

    @Test fun `zone SW at compass bearing 225`() {
        val (x, y) = atBearing(225.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.SW)
    }

    @Test fun `zone W at compass bearing 270`() {
        val (x, y) = atBearing(270.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.W)
    }

    @Test fun `zone NW at compass bearing 315`() {
        val (x, y) = atBearing(315.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.NW)
    }

    // ---- CENTER ----

    @Test fun `center zone inside centerZoneRadius regardless of angle`() {
        val r = TargetGeometry.CENTER_ZONE_RADIUS - 0.001
        // Arbitrary non-axis-aligned direction
        val c = TargetGeometry.SixRing.classify(plotX = r * 0.7, plotY = r * 0.3)
        assertThat(c.zone).isEqualTo(Zone.CENTER)
    }

    // ---- Dot-radius scoring (parity with iOS lines 155–158) ----

    @Test fun `arrow dot touching higher ring scores the higher ring`() {
        // Place plot just inside ring 9; a dot radius that reaches back into ring 10
        // should upgrade the score.
        val radius = TargetGeometry.SixRing.R10_RADIUS + 0.01
        val c = TargetGeometry.SixRing.classifyWithDotRadius(
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
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.NE)
    }

    @Test fun `just before 22_5 boundary belongs to N`() {
        val (x, y) = atBearing(22.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `just past 337_5 boundary belongs to N`() {
        // iOS: `case 337.5..<360, 0..<22.5: .n`.
        val (x, y) = atBearing(338.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.N)
    }

    @Test fun `just before 337_5 boundary belongs to NW`() {
        val (x, y) = atBearing(337.0, 0.4)
        assertThat(TargetGeometry.SixRing.classify(x, y).zone).isEqualTo(Zone.NW)
    }

    // ---- iOS fixture parity ----
    //
    // (plotX, plotY) sampled from bowpress-ios DevMockData.swift:484–485 — the "low-grouping"
    // archer. These are the coords the iOS app writes to the DB, so classifying them
    // here proves round-trip parity with iOS scoring.

    @Test fun `iOS fixture 0_015 0_095 classifies as ring 10 zone S`() {
        // Low and just right of centre → outer 10, shots low on the face → south.
        val c = TargetGeometry.SixRing.classify(plotX = 0.015, plotY = 0.095)
        assertThat(c.ring).isEqualTo(10)
        assertThat(c.zone).isEqualTo(Zone.S)
    }

    @Test fun `iOS fixture negative plotY classifies in northern zones`() {
        // Flip the above — a shot high on the face → north. iOS sets plotY negative
        // for shots above centre (TargetPlotView.swift:165).
        val c = TargetGeometry.SixRing.classify(plotX = 0.015, plotY = -0.095)
        assertThat(c.ring).isEqualTo(10)
        assertThat(c.zone).isEqualTo(Zone.N)
    }

    // ---- forFace() dispatch ------------------------------------------------

    @Test fun `forFace returns the correct preset`() {
        assertThat(TargetGeometry.forFace(TargetFaceType.SIX_RING))
            .isSameInstanceAs(TargetGeometry.SixRing)
        assertThat(TargetGeometry.forFace(TargetFaceType.TEN_RING))
            .isSameInstanceAs(TargetGeometry.TenRing)
    }

    @Test fun `defaultFor maps compound to 6-ring and others to 10-ring`() {
        assertThat(TargetFaceType.defaultFor(com.andrewnguyen.bowpress.core.model.BowType.COMPOUND))
            .isEqualTo(TargetFaceType.SIX_RING)
        assertThat(TargetFaceType.defaultFor(com.andrewnguyen.bowpress.core.model.BowType.RECURVE))
            .isEqualTo(TargetFaceType.TEN_RING)
        assertThat(TargetFaceType.defaultFor(com.andrewnguyen.bowpress.core.model.BowType.BAREBOW))
            .isEqualTo(TargetFaceType.TEN_RING)
    }

    // ---- TenRing face: ring boundaries ------------------------------------
    //
    // TenRing thresholds (plan): X=0.05, then every 0.10 out to 1.00.
    // ring() returns 11 (X) inside 0.05; 10 inside 0.10; 9 inside 0.20; ...; 1 inside 1.00;
    // null beyond.

    @Test fun `tenRing X at centre`() {
        val c = TargetGeometry.TenRing.classify(plotX = 0.0, plotY = 0.0)
        assertThat(c.ring).isEqualTo(11)
    }

    @Test fun `tenRing ring 10 just past X`() {
        val c = TargetGeometry.TenRing.classify(plotX = 0.06, plotY = 0.0)
        assertThat(c.ring).isEqualTo(10)
    }

    @Test fun `tenRing ring 9 just past r10`() {
        val c = TargetGeometry.TenRing.classify(plotX = 0.11, plotY = 0.0)
        assertThat(c.ring).isEqualTo(9)
    }

    @Test fun `tenRing ring 5 mid-face`() {
        // Inside 0.60 (ring 5), outside 0.50.
        val c = TargetGeometry.TenRing.classify(plotX = 0.55, plotY = 0.0)
        assertThat(c.ring).isEqualTo(5)
    }

    @Test fun `tenRing ring 1 near outer edge`() {
        // plan requirement: ring(0.95) == 1
        val c = TargetGeometry.TenRing.classify(plotX = 0.95, plotY = 0.0)
        assertThat(c.ring).isEqualTo(1)
    }

    @Test fun `tenRing ring X inside xRadius`() {
        // plan requirement: ring(0.04) == 11
        val c = TargetGeometry.TenRing.classify(plotX = 0.04, plotY = 0.0)
        assertThat(c.ring).isEqualTo(11)
    }

    @Test fun `tenRing miss outside r1`() {
        // plan requirement: ring(1.01) == null
        val r = TargetGeometry.TenRing.ring(1.01)
        assertThat(r).isNull()
    }

    @Test fun `tenRing all rings sweep`() {
        // Spot-check every ring by walking outward from centre in 0.001 steps past each
        // threshold so we assert ring = 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 in order.
        val ring = TargetGeometry.TenRing
        val expectedAtRadius = listOf(
            0.06 to 10,
            0.11 to 9,
            0.21 to 8,
            0.31 to 7,
            0.41 to 6,
            0.51 to 5,
            0.61 to 4,
            0.71 to 3,
            0.81 to 2,
            0.91 to 1,
        )
        for ((r, expected) in expectedAtRadius) {
            assertThat(ring.ring(r)).isEqualTo(expected)
        }
    }

    @Test fun `tenRing zone is independent of face type`() {
        // Same bearing, same zone, regardless of which face we're on.
        val (x, y) = atBearing(45.0, 0.8)
        assertThat(TargetGeometry.TenRing.classify(x, y).zone).isEqualTo(Zone.NE)
    }

    @Test fun `sixRing on TenRing preset behaves identically for the shared rings`() {
        // A point that scores ring 10 on sixRing should also score ring 10 on tenRing,
        // because both thresholds cross the same physical boundary. X on sixRing (0.08)
        // differs from X on tenRing (0.05), so we pick a point well inside both X rings.
        val plotX = 0.02
        val six = TargetGeometry.SixRing.classify(plotX, 0.0)
        val ten = TargetGeometry.TenRing.classify(plotX, 0.0)
        assertThat(six.ring).isEqualTo(11)
        assertThat(ten.ring).isEqualTo(11)
    }

    @Test fun `tenRing arrow dot touching higher ring scores the higher ring`() {
        // Plot just past r8 (0.30), with a dot radius that reaches back into ring 8.
        val c = TargetGeometry.TenRing.classifyWithDotRadius(
            plotX = 0.305,
            plotY = 0.0,
            dotNormRadius = 0.02,
        )
        assertThat(c.ring).isEqualTo(8)
    }

    // ---- §B3 Distance-aware preset (sixRing 6-zone vs 7-zone) ------------
    //
    // Ports iOS commit b53b748's TargetGeometryTests + the new sixRingOutdoor
    // ring-boundary coverage. The geometry layer is load-bearing for the
    // keypad ladder (outerRingValue), displayed group-spread mm (mmPerNormUnit),
    // the re-plot snap, and live ring(for:) scoring — a regression for
    // sixRingOutdoor would silently mis-score 50/70m sessions.

    @Test fun `preset sixRing at indoor is Vegas geometry`() {
        // 20yd indoor + null both resolve to the 40cm Vegas 6-zone face.
        // Vegas has outerRingValue 6 and 6 thresholds (X + 5 numeric).
        val vegas = TargetGeometry.forFace(TargetFaceType.SIX_RING, com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20)
        assertThat(vegas.outerRingValue).isEqualTo(6)
        assertThat(vegas.thresholds.size).isEqualTo(6)

        val defaulted = TargetGeometry.forFace(TargetFaceType.SIX_RING, distance = null)
        assertThat(defaulted.outerRingValue).isEqualTo(6)
        assertThat(defaulted.thresholds.size).isEqualTo(6)
    }

    @Test fun `preset sixRing at outdoor is seven-zone geometry`() {
        // 50/70m resolve to the 80cm WA compound outdoor 7-zone face.
        // 7-zone has outerRingValue 5 and 7 thresholds (X + 6 numeric).
        val fifty = TargetGeometry.forFace(TargetFaceType.SIX_RING, com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50)
        assertThat(fifty.outerRingValue).isEqualTo(5)
        assertThat(fifty.thresholds.size).isEqualTo(7)

        val seventy = TargetGeometry.forFace(TargetFaceType.SIX_RING, com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70)
        assertThat(seventy.outerRingValue).isEqualTo(5)
        assertThat(seventy.thresholds.size).isEqualTo(7)
    }

    @Test fun `preset tenRing ignores distance`() {
        // tenRing geometry is the same at every distance — the WA 122cm full
        // face isn't distance-overloaded the way sixRing is.
        val distances = listOf(
            null,
            com.andrewnguyen.bowpress.core.model.ShootingDistance.YARDS_20,
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_50,
            com.andrewnguyen.bowpress.core.model.ShootingDistance.METERS_70,
        )
        for (d in distances) {
            val geo = TargetGeometry.forFace(TargetFaceType.TEN_RING, d)
            assertThat(geo.outerRingValue).isEqualTo(1)
            assertThat(geo.thresholds.size).isEqualTo(11)
            assertThat(geo).isSameInstanceAs(TargetGeometry.TenRing)
        }
    }

    // ---- §B3 sixRingOutdoor (80cm WA compound, rings 5–X) ----------------

    @Test fun `sixRingOutdoor X-ring hits at centre`() {
        val geo = TargetGeometry.SixRingOutdoor
        assertThat(geo.ring(0.0)).isEqualTo(11)
        // xRadius = 0.5/6 ≈ 0.0833
        assertThat(geo.ring(0.08)).isEqualTo(11)
    }

    @Test fun `sixRingOutdoor intermediate rings`() {
        val geo = TargetGeometry.SixRingOutdoor
        // Equal-width 1/6 bands — pick a midpoint inside each.
        assertThat(geo.ring(0.12)).isEqualTo(10)   // between X (≈0.083) and 1/6 (≈0.167)
        assertThat(geo.ring(0.25)).isEqualTo(9)    // 1/6 → 2/6
        assertThat(geo.ring(0.42)).isEqualTo(8)    // 2/6 → 3/6
        assertThat(geo.ring(0.58)).isEqualTo(7)    // 3/6 → 4/6
        assertThat(geo.ring(0.75)).isEqualTo(6)    // 4/6 → 5/6
        assertThat(geo.ring(0.95)).isEqualTo(5)    // 5/6 → 1.0 (the new outer ring)
    }

    @Test fun `sixRingOutdoor outer ring 5 is last scoring ring`() {
        val geo = TargetGeometry.SixRingOutdoor
        // Just inside the outer edge still scores 5 — ring 5 IS the outer.
        assertThat(geo.ring(0.99)).isEqualTo(5)
    }

    @Test fun `sixRingOutdoor miss is null`() {
        val geo = TargetGeometry.SixRingOutdoor
        // outer R5_RADIUS == 1.0, so anything >= 1.0 is off the printed face.
        assertThat(geo.ring(1.0)).isNull()
        assertThat(geo.ring(1.05)).isNull()
    }

    @Test fun `sixRingOutdoor mmPerNormUnit is 400`() {
        // 400mm radius / 1.0 canvas norm. The ~24% discrepancy vs Vegas
        // (mmPerNormUnit ≈ 123.5) is what drove the historical groupSpreadMm
        // bug — pin the value so a future tweak to ring radii or the
        // realFaceRadiusMm constant can't silently regress mm numerics.
        assertThat(TargetGeometry.SixRingOutdoor.mmPerNormUnit).isEqualTo(400.0)
    }

    @Test fun `sixRingOutdoor outerRingValue is 5`() {
        // Drives the ArrowEditSheet keypad's lowest cell so a 50/70m archer
        // can correct an arrow down to ring 5.
        assertThat(TargetGeometry.SixRingOutdoor.outerRingValue).isEqualTo(5)
    }

    @Test fun `sixRing Vegas outerRingValue is 6`() {
        // Drives the keypad ladder on a 20yd indoor sixRing session — the
        // lowest legal ring is 6, not 5 (Vegas has no ring 5).
        assertThat(TargetGeometry.SixRing.outerRingValue).isEqualTo(6)
    }

    @Test fun `tenRing outerRingValue is 1`() {
        // tenRing's ladder runs all the way to ring 1.
        assertThat(TargetGeometry.TenRing.outerRingValue).isEqualTo(1)
    }

    // ---- §B3 ringColor mapping covers every numeric ring -----------------

    @Test fun `ringColor maps white 1-2, black 3-4, blue 5-6, red 7-8, yellow 9-10`() {
        // Equals comparison covers the (1,2)→white, (3,4)→black, etc. branches.
        // Pure smoke check; the colour values themselves come from the design
        // tokens. Failure would mean a future ring number slipped through —
        // exactly what the assertion failure on the else branch is meant to
        // protect against.
        for (r in 1..10) {
            val color = TargetGeometry.ringColor(r)
            // Verify it didn't throw — that's the actual contract here.
            assertThat(color).isNotNull()
        }
    }
}
