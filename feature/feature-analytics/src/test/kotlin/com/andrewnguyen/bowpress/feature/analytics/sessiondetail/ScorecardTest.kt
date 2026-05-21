package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * Locks the [Scorecard] invariant ported from iOS `SessionScorecard`: every
 * arrow lands in exactly one line, so the TOTAL row always equals the
 * session total. Covers the real-ends, orphan, stale-endId, chunked, and
 * empty cases.
 */
class ScorecardTest {

    private val t0 = Instant.parse("2026-05-01T10:00:00Z")

    private fun arrow(ring: Int, endId: String?, seq: Int) = ArrowPlot(
        id = "a$seq",
        sessionId = "s1",
        bowConfigId = "bc1",
        arrowConfigId = "ac1",
        ring = ring,
        zone = Zone.CENTER,
        endId = endId,
        shotAt = t0.plusSeconds(seq.toLong()),
    )

    private fun end(number: Int) = SessionEnd(
        id = "e$number",
        sessionId = "s1",
        endNumber = number,
        completedAt = t0.plusSeconds(number * 100L),
    )

    /** The contract: arrows are partitioned — no arrow is lost or double-counted. */
    private fun assertPartitions(card: Scorecard, arrows: List<ArrowPlot>) {
        assertThat(card.totalArrows).isEqualTo(arrows.size)
        assertThat(card.totalScore).isEqualTo(arrows.sumOf { Scorecard.score(it.ring) })
        assertThat(card.totalXCount).isEqualTo(arrows.count { it.ring == 11 })
    }

    @Test
    fun `real ends — every arrow matched by endId`() {
        val arrows = listOf(
            arrow(10, "e1", 1), arrow(9, "e1", 2),
            arrow(11, "e2", 3), arrow(8, "e2", 4),
        )
        val card = Scorecard.build(arrows, listOf(end(1), end(2)), "s1")

        assertThat(card.endCount).isEqualTo(2)
        assertThat(card.lines[0].sum).isEqualTo(19)
        assertThat(card.lines[1].sum).isEqualTo(18) // X scores 10
        assertThat(card.lines[1].xCount).isEqualTo(1)
        assertPartitions(card, arrows)
    }

    @Test
    fun `orphan arrows with null endId fold into a trailing line`() {
        val arrows = listOf(
            arrow(10, "e1", 1),
            arrow(9, null, 2), // never assigned to a completed end
        )
        val card = Scorecard.build(arrows, listOf(end(1)), "s1")

        assertThat(card.endCount).isEqualTo(2)
        assertThat(card.lines.last().end.endNumber).isEqualTo(2)
        assertThat(card.lines.last().arrows.single().ring).isEqualTo(9)
        assertPartitions(card, arrows)
    }

    @Test
    fun `arrows with a stale endId still count toward the total`() {
        val arrows = listOf(
            arrow(10, "e1", 1),
            arrow(7, "deleted-end", 2), // points at an end that no longer exists
        )
        val card = Scorecard.build(arrows, listOf(end(1)), "s1")

        assertThat(card.lines.last().arrows.single().ring).isEqualTo(7)
        assertPartitions(card, arrows)
    }

    @Test
    fun `no recorded ends — arrows chunk into synthetic lines`() {
        val arrows = (1..7).map { arrow(ring = 10, endId = null, seq = it) }
        val card = Scorecard.build(arrows, emptyList(), "s1", synthChunkSize = 3)

        assertThat(card.endCount).isEqualTo(3) // 3 + 3 + 1
        assertThat(card.lines.map { it.arrows.size }).containsExactly(3, 3, 1).inOrder()
        assertPartitions(card, arrows)
    }

    @Test
    fun `empty session yields an empty scorecard`() {
        val card = Scorecard.build(emptyList(), emptyList(), "s1")

        assertThat(card.lines).isEmpty()
        assertThat(card.totalScore).isEqualTo(0)
        assertThat(card.maxPossibleScore).isEqualTo(0)
    }

    @Test
    fun `maxPossibleScore and maxShotsPerEnd track the widest end`() {
        val arrows = listOf(
            arrow(10, "e1", 1), arrow(10, "e1", 2), arrow(10, "e1", 3),
            arrow(9, "e2", 4),
        )
        val card = Scorecard.build(arrows, listOf(end(1), end(2)), "s1")

        assertThat(card.maxShotsPerEnd).isEqualTo(3)
        assertThat(card.maxPossibleScore).isEqualTo(40) // 4 arrows × 10
    }

    @Test
    fun `score clamps X and misses into scoring range`() {
        assertThat(Scorecard.score(11)).isEqualTo(10) // X
        assertThat(Scorecard.score(10)).isEqualTo(10)
        assertThat(Scorecard.score(0)).isEqualTo(0) // miss
        assertThat(Scorecard.score(-1)).isEqualTo(0)
    }
}
