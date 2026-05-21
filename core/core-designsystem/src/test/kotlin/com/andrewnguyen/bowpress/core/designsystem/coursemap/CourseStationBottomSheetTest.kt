package com.andrewnguyen.bowpress.core.designsystem.coursemap

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-logic tests for the two helpers that drive the live
 * [CourseStationBottomSheet]: snap-height resolution and the cut-distance
 * computation. The snap *transitions* themselves are owned by Material3
 * `AnchoredDraggableState` (positional + velocity thresholds), not by any
 * hand-rolled logic, so there is nothing else here to unit-test.
 */
class CourseStationBottomSheetTest {

    @Test
    fun `snap heights derive from container and fractions`() {
        val h = resolveSnapHeights(
            containerHeightPx = 1000f,
            peekHeightPx = 168f,
            midFraction = 0.55f,
            fullFraction = 0.88f,
        )
        assertThat(h.peek).isEqualTo(168f)
        assertThat(h.mid).isEqualTo(550f)
        assertThat(h.full).isEqualTo(880f)
    }

    @Test
    fun `peek is clamped so it never exceeds mid on a short container`() {
        val h = resolveSnapHeights(
            containerHeightPx = 240f,
            peekHeightPx = 168f,
            midFraction = 0.55f,
            fullFraction = 0.88f,
        )
        // Mid = 132 < peek 168 — peek clamps to mid.
        assertThat(h.mid).isEqualTo(132f)
        assertThat(h.peek).isEqualTo(132f)
    }

    @Test
    fun `mid is clamped so it never exceeds full`() {
        // A pathological midFraction above fullFraction — mid clamps to full.
        val h = resolveSnapHeights(
            containerHeightPx = 1000f,
            peekHeightPx = 168f,
            midFraction = 0.95f,
            fullFraction = 0.88f,
        )
        assertThat(h.full).isEqualTo(880f)
        assertThat(h.mid).isEqualTo(880f)
    }

    @Test
    fun `cut distance shrinks with shot angle and ignores sign`() {
        // Level shot — cut equals the ranged distance.
        assertThat(cutDistance(40.0, 0.0)).isWithin(1e-6).of(40.0)
        // 60-degree shot — cut is half the distance.
        assertThat(cutDistance(40.0, 60.0)).isWithin(1e-6).of(20.0)
        // Downhill is the same cut as uphill.
        assertThat(cutDistance(40.0, -60.0)).isWithin(1e-6).of(20.0)
    }
}
