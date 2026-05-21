package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Multi-spot scoring parity tests — guardrails that keep the Android
 * [MultiSpotGeometry] in lockstep with iOS
 * `bowpress-ios/.../Session/MultiSpotGeometry.swift`.
 */
class MultiSpotGeometryTest {

    // ---- Presets ----

    @Test fun `single layout has no multi-spot geometry`() {
        assertThat(MultiSpotGeometry.preset(TargetLayout.SINGLE)).isNull()
    }

    @Test fun `triangle preset has three spots and the design radius`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        assertThat(g.centers).hasSize(3)
        assertThat(g.radiusNorm).isEqualTo(40.0 / 200.0)
        assertThat(g.spotDiameterMm).isEqualTo(180.0)
        // BL = (55,145), TOP = (100,58), BR = (145,145) on the 200 viewBox.
        assertThat(g.centers[0]).isEqualTo(MultiSpotGeometry.NormPoint(55.0 / 200.0, 145.0 / 200.0))
        assertThat(g.centers[1]).isEqualTo(MultiSpotGeometry.NormPoint(0.5, 58.0 / 200.0))
        assertThat(g.centers[2]).isEqualTo(MultiSpotGeometry.NormPoint(145.0 / 200.0, 145.0 / 200.0))
    }

    @Test fun `vertical preset stacks three spots`() {
        val g = MultiSpotGeometry.preset(TargetLayout.VERTICAL)!!
        assertThat(g.centers.map { it.y }).containsExactly(0.183, 0.500, 0.817).inOrder()
        assertThat(g.centers.all { it.x == 0.5 }).isTrue()
    }

    // ---- Nearest-spot resolution ----

    @Test fun `nearestSpotIndex picks the closest spot`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        // A point right on the TOP spot's centre resolves to spot 1.
        assertThat(g.nearestSpotIndex(g.centers[1])).isEqualTo(1)
        // A point near the bottom-right resolves to spot 2.
        assertThat(g.nearestSpotIndex(MultiSpotGeometry.NormPoint(0.70, 0.70))).isEqualTo(2)
    }

    @Test fun `nearestSpotLocalRadius is zero at a spot centre and one on the ring-6 line`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        val centreHit = g.nearestSpotLocalRadius(g.centers[0])
        assertThat(centreHit.spot).isEqualTo(0)
        assertThat(centreHit.local).isWithin(1e-9).of(0.0)
        // One radiusNorm east of spot 0's centre → local radius 1.0.
        val edge = MultiSpotGeometry.NormPoint(g.centers[0].x + g.radiusNorm, g.centers[0].y)
        assertThat(g.nearestSpotLocalRadius(edge).local).isWithin(1e-9).of(1.0)
    }

    // ---- Ring banding (spot-local WA edge rule) ----

    @Test fun `ring banding maps spot-local distance to a ring`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        // No arrow-radius offset — pick a representative point in each band.
        assertThat(g.ring(local = 0.00, arrowRadiusFrac = 0.0)).isEqualTo(11) // X
        assertThat(g.ring(local = 0.15, arrowRadiusFrac = 0.0)).isEqualTo(10)
        assertThat(g.ring(local = 0.30, arrowRadiusFrac = 0.0)).isEqualTo(9)
        assertThat(g.ring(local = 0.50, arrowRadiusFrac = 0.0)).isEqualTo(8)
        assertThat(g.ring(local = 0.70, arrowRadiusFrac = 0.0)).isEqualTo(7)
        assertThat(g.ring(local = 0.90, arrowRadiusFrac = 0.0)).isEqualTo(6)
        assertThat(g.ring(local = 1.20, arrowRadiusFrac = 0.0)).isNull()  // miss
    }

    @Test fun `arrow radius fraction pulls a touch toward the higher ring`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        // local 0.10 alone scores 10; a 0.05 shaft fraction drops the
        // effective distance below the 0.075 X boundary → X.
        assertThat(g.ring(local = 0.10, arrowRadiusFrac = 0.0)).isEqualTo(10)
        assertThat(g.ring(local = 0.10, arrowRadiusFrac = 0.05)).isEqualTo(11)
    }

    // ---- Arrow assignment ----

    @Test fun `assignArrows buckets arrows onto their nearest spot`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        // plotX/plotY are face-square coords (-1..1); convert the triangle
        // spot centres back: faceX = 2*(centerX) - 1.
        val blArrow = plot(plotX = -0.45, plotY = 0.45)   // near BL spot
        val topArrow = plot(plotX = 0.00, plotY = -0.42)  // near TOP spot
        val brArrow = plot(plotX = 0.45, plotY = 0.45)    // near BR spot
        val assigned = g.assignArrows(listOf(blArrow, topArrow, brArrow))
        assertThat(assigned.map { it.spotIndex }).containsExactly(0, 1, 2).inOrder()
        // An arrow on a spot centre recenters to local (0, 0).
        val onCentre = g.assignArrows(listOf(plot(plotX = -0.45, plotY = 0.45)))[0]
        assertThat(onCentre.localX).isWithin(0.05).of(0.0)
        assertThat(onCentre.localY).isWithin(0.05).of(0.0)
    }

    @Test fun `assignArrows skips plots without coordinates`() {
        val g = MultiSpotGeometry.preset(TargetLayout.TRIANGLE)!!
        assertThat(g.assignArrows(listOf(plot(plotX = null, plotY = null)))).isEmpty()
    }

    private fun plot(plotX: Double?, plotY: Double?): ArrowPlot = ArrowPlot(
        id = "p",
        sessionId = "s",
        bowConfigId = "bc",
        arrowConfigId = "ac",
        ring = 10,
        zone = Zone.CENTER,
        plotX = plotX,
        plotY = plotY,
        shotAt = Instant.parse("2026-05-21T10:00:00Z"),
    )
}
