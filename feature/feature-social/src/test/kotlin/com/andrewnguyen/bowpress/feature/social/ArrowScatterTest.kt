package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.feed.ringRadiusBand
import com.andrewnguyen.bowpress.feature.social.ui.feed.scatterArrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The Social Activity Card · 50/50 plots a friend's arrows from `endRings`
 * alone — the feed payload carries no x/y. `scatterArrows` must place every
 * arrow inside its scoring ring's radius band, deterministically.
 */
class ArrowScatterTest {

    @Test
    fun `flattens every arrow across every end`() {
        val ends = listOf(listOf(11, 10, 9), listOf(8, 0), listOf(10))
        val scattered = scatterArrows(ends)
        // 3 + 2 + 1 arrows, in shot order.
        assertThat(scattered).hasSize(6)
        assertThat(scattered.map { it.ring })
            .containsExactly(11, 10, 9, 8, 0, 10)
            .inOrder()
    }

    @Test
    fun `an empty scorecard plots nothing`() {
        assertThat(scatterArrows(emptyList())).isEmpty()
        assertThat(scatterArrows(listOf(emptyList()))).isEmpty()
    }

    @Test
    fun `the scatter is deterministic — same rings, same plot`() {
        val ends = listOf(listOf(10, 9, 8), listOf(7, 6, 11))
        val first = scatterArrows(ends)
        val second = scatterArrows(ends)
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `each arrow lands inside its scoring ring's radius band`() {
        // One arrow per ring 1..10, plus an X and a miss.
        val ends = listOf((1..10).toList(), listOf(11, 0))
        scatterArrows(ends).forEach { arrow ->
            val (inner, outer) = ringRadiusBand(arrow.ring)
            // The dot sits strictly within the band (kept off the edges).
            assertThat(arrow.radiusNorm).isAtLeast(inner)
            assertThat(arrow.radiusNorm).isAtMost(outer)
        }
    }

    @Test
    fun `the X has its own tight centre band inside the ring-10 bullseye`() {
        val (xInner, xOuter) = ringRadiusBand(11)
        assertThat(xInner).isWithin(1e-5f).of(0f)
        assertThat(xOuter).isWithin(1e-5f).of(0.05f)
        // Ring 10's band is the wider bullseye — the X band sits strictly
        // within it, so an X always plots inside the X ring.
        val (tenInner, tenOuter) = ringRadiusBand(10)
        assertThat(tenInner).isWithin(1e-5f).of(0f)
        assertThat(tenOuter).isWithin(1e-5f).of(0.10f)
        assertThat(xOuter).isLessThan(tenOuter)
    }

    @Test
    fun `an X plots inside the X ring`() {
        scatterArrows(listOf(listOf(11, 11, 11))).forEach { arrow ->
            // Within the X band's outer edge (0.05) — the maple standout dot.
            assertThat(arrow.radiusNorm).isAtMost(0.05f)
        }
    }

    @Test
    fun `ring 1 occupies the outermost scoring band`() {
        val (inner, outer) = ringRadiusBand(1)
        assertThat(inner).isWithin(1e-5f).of(0.90f)
        assertThat(outer).isWithin(1e-5f).of(1.0f)
    }

    @Test
    fun `a miss lands just past the face edge`() {
        val (inner, outer) = ringRadiusBand(0)
        // Beyond the 1.0 face radius — the dot reads as off the face.
        assertThat(inner).isAtLeast(1.0f)
        assertThat(outer).isGreaterThan(inner)
    }
}
