package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.feed.multiSpotArrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The Social Activity Card's multi-spot body hands `BPPlottedTarget` real
 * `ArrowPlot`s built from the feed payload's flat `plotPoints` + `endRings`.
 * `multiSpotArrows` is the adapter — its three branches are pinned here so
 * a future signature change can't silently strip an X colour or place a
 * dot on the wrong spot.
 */
class MultiSpotArrowsTest {

    @Test
    fun `an empty plotPoints plots nothing`() {
        // A multi-spot session whose feed payload carries no x/y — the card
        // still renders the empty 3-spot face, but `multiSpotArrows` is
        // what's responsible for producing zero arrows in that case (not the
        // renderer).
        assertThat(multiSpotArrows(endRings = emptyList(), plotPoints = emptyList())).isEmpty()
        assertThat(
            multiSpotArrows(
                endRings = listOf(listOf(10, 9, 8)),
                plotPoints = emptyList(),
            ),
        ).isEmpty()
    }

    @Test
    fun `each plot is paired with its ring in shot order`() {
        // Two ends, three arrows each — the flat ring order is 11,10,9, 8,7,11.
        val ends = listOf(listOf(11, 10, 9), listOf(8, 7, 11))
        val plots = listOf(
            listOf(0.1, 0.2), listOf(-0.3, 0.4), listOf(0.5, -0.6),
            listOf(0.7, 0.8), listOf(-0.9, 0.0), listOf(0.0, 0.1),
        )
        val out = multiSpotArrows(ends, plots)
        assertThat(out.map { it.ring })
            .containsExactly(11, 10, 9, 8, 7, 11)
            .inOrder()
        assertThat(out.map { it.plotX to it.plotY })
            .containsExactly(
                0.1 to 0.2, -0.3 to 0.4, 0.5 to -0.6,
                0.7 to 0.8, -0.9 to 0.0, 0.0 to 0.1,
            )
            .inOrder()
    }

    @Test
    fun `a plot point with fewer than two coords is skipped`() {
        // Malformed entries (missing coords) shouldn't crash or smear a dot
        // onto the centre — drop them so the renderer just plots the rest.
        val ends = listOf(listOf(10, 10, 10))
        val plots = listOf(listOf(0.1, 0.2), listOf(0.3), listOf(-0.4, 0.5))
        val out = multiSpotArrows(ends, plots)
        assertThat(out).hasSize(2)
        assertThat(out.map { it.plotX to it.plotY })
            .containsExactly(0.1 to 0.2, -0.4 to 0.5)
            .inOrder()
    }

    @Test
    fun `extra plot points beyond the scorecard fall back to ring 10`() {
        // If the feed ever ships more `plotPoints` than `endRings` accounts
        // for, the helper defaults the unpaired ring to 10 rather than
        // throwing — keeps the dot on screen.
        val ends = listOf(listOf(11))
        val plots = listOf(listOf(0.0, 0.0), listOf(0.1, 0.1))
        val out = multiSpotArrows(ends, plots)
        assertThat(out.map { it.ring }).containsExactly(11, 10).inOrder()
    }
}
