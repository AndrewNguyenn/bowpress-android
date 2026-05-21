package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Pure-model tests for the live-session end breakdown — the endId slicing,
 * end numbering, the in-progress end, and what a completeEnd / deleteEnd
 * leaves behind. These are the guardrails for the iOS-parity ends mechanism.
 */
class SessionEndsBreakdownTest {

    private val t0: Instant = Instant.parse("2026-05-21T10:00:00Z")

    private fun arrow(id: String, ring: Int, endId: String?, offsetMin: Long): ArrowPlot =
        ArrowPlot(
            id = id,
            sessionId = "s1",
            bowConfigId = "bc1",
            arrowConfigId = "ac1",
            ring = ring,
            zone = Zone.CENTER,
            plotX = 0.0,
            plotY = 0.0,
            endId = endId,
            shotAt = t0.plusSeconds(offsetMin * 60),
        )

    private fun end(id: String, number: Int): SessionEnd =
        SessionEnd(id = id, sessionId = "s1", endNumber = number, completedAt = t0)

    // ---- In-progress end + numbering ----

    @Test fun `no ends — every arrow is the in-progress end, number is 1`() {
        val arrows = listOf(
            arrow("a1", 10, endId = null, offsetMin = 0),
            arrow("a2", 11, endId = null, offsetMin = 1),
        )
        val b = SessionEndsBreakdown.from(arrows, ends = emptyList())
        assertThat(b.completedEndLines).isEmpty()
        assertThat(b.inProgressArrows.map { it.id }).containsExactly("a1", "a2").inOrder()
        assertThat(b.currentEndNumber).isEqualTo(1)
        assertThat(b.hasCompletedEnds).isFalse()
    }

    @Test fun `currentEndNumber is max completed endNumber plus one`() {
        // Two completed ends numbered 1 and 2 → the next end is 3.
        val ends = listOf(end("e1", 1), end("e2", 2))
        val arrows = listOf(
            arrow("a1", 10, endId = "e1", offsetMin = 0),
            arrow("a2", 9, endId = "e2", offsetMin = 1),
        )
        val b = SessionEndsBreakdown.from(arrows, ends)
        assertThat(b.currentEndNumber).isEqualTo(3)
    }

    // ---- endId slicing — completeEnd's effect ----

    @Test fun `arrows slice into ends by endId, orphans are the in-progress end`() {
        // e1 has a1+a2; e2 has a3; a4+a5 have no endId (the live end).
        val ends = listOf(end("e1", 1), end("e2", 2))
        val arrows = listOf(
            arrow("a1", 10, endId = "e1", offsetMin = 0),
            arrow("a2", 11, endId = "e1", offsetMin = 1),
            arrow("a3", 9, endId = "e2", offsetMin = 2),
            arrow("a4", 8, endId = null, offsetMin = 3),
            arrow("a5", 10, endId = null, offsetMin = 4),
        )
        val b = SessionEndsBreakdown.from(arrows, ends)

        assertThat(b.completedEndLines).hasSize(2)
        assertThat(b.completedEndLines[0].arrows.map { it.id }).containsExactly("a1", "a2").inOrder()
        assertThat(b.completedEndLines[1].arrows.map { it.id }).containsExactly("a3")
        assertThat(b.inProgressArrows.map { it.id }).containsExactly("a4", "a5").inOrder()
        // The slices + the in-progress end partition every arrow exactly.
        val sliced = b.completedEndLines.sumOf { it.arrows.size } + b.inProgressArrows.size
        assertThat(sliced).isEqualTo(arrows.size)
    }

    @Test fun `completing an end moves its arrows out of the in-progress end`() {
        // Before: a1+a2 are in-progress (no endId), no recorded ends.
        val before = SessionEndsBreakdown.from(
            arrows = listOf(
                arrow("a1", 10, endId = null, offsetMin = 0),
                arrow("a2", 11, endId = null, offsetMin = 1),
            ),
            ends = emptyList(),
        )
        assertThat(before.inProgressArrows).hasSize(2)
        assertThat(before.currentEndNumber).isEqualTo(1)

        // After completeEnd: a SessionEnd("e1", 1) is recorded and both
        // arrows are stamped with its id (what SessionViewModel.completeEnd does).
        val after = SessionEndsBreakdown.from(
            arrows = listOf(
                arrow("a1", 10, endId = "e1", offsetMin = 0),
                arrow("a2", 11, endId = "e1", offsetMin = 1),
            ),
            ends = listOf(end("e1", 1)),
        )
        assertThat(after.inProgressArrows).isEmpty()
        assertThat(after.completedEndLines).hasSize(1)
        assertThat(after.completedEndLines[0].arrows.map { it.id })
            .containsExactly("a1", "a2").inOrder()
        // End 1 is done → the next end the archer plots is end 2.
        assertThat(after.currentEndNumber).isEqualTo(2)
    }

    // ---- deleteEnd — leaves a numbering gap ----

    @Test fun `deleting a middle end leaves a numbering gap so the next end is unique`() {
        // Started with ends 1, 2, 3 — end 2 (and its arrows) deleted.
        val ends = listOf(end("e1", 1), end("e3", 3))
        val arrows = listOf(
            arrow("a1", 10, endId = "e1", offsetMin = 0),
            arrow("a3", 9, endId = "e3", offsetMin = 2),
        )
        val b = SessionEndsBreakdown.from(arrows, ends)
        assertThat(b.completedEndLines.map { it.end.endNumber }).containsExactly(1, 3).inOrder()
        // max(endNumber) is 3 → next end is 4, not a duplicate 3.
        assertThat(b.currentEndNumber).isEqualTo(4)
    }

    @Test fun `an arrow whose endId points at a deleted end falls into the in-progress end`() {
        // a2's end was deleted but the arrow somehow survived with a stale id.
        val ends = listOf(end("e1", 1))
        val arrows = listOf(
            arrow("a1", 10, endId = "e1", offsetMin = 0),
            arrow("a2", 9, endId = "deleted-end", offsetMin = 1),
        )
        val b = SessionEndsBreakdown.from(arrows, ends)
        assertThat(b.completedEndLines[0].arrows.map { it.id }).containsExactly("a1")
        assertThat(b.inProgressArrows.map { it.id }).containsExactly("a2")
    }

    // ---- Scoring ----

    @Test fun `end sum clamps X and a 10 to 10, a miss to 0`() {
        val ends = listOf(end("e1", 1))
        val arrows = listOf(
            arrow("a1", 11, endId = "e1", offsetMin = 0),  // X → 10
            arrow("a2", 10, endId = "e1", offsetMin = 1),  // 10
            arrow("a3", 0, endId = "e1", offsetMin = 2),   // miss → 0
        )
        val line = SessionEndsBreakdown.from(arrows, ends).completedEndLines[0]
        assertThat(line.sum).isEqualTo(20)
        assertThat(line.xCount).isEqualTo(1)
    }

    @Test fun `maxShotsPerEnd is the widest completed end`() {
        val ends = listOf(end("e1", 1), end("e2", 2))
        val arrows = listOf(
            arrow("a1", 10, endId = "e1", offsetMin = 0),
            arrow("a2", 10, endId = "e2", offsetMin = 1),
            arrow("a3", 10, endId = "e2", offsetMin = 2),
            arrow("a4", 10, endId = "e2", offsetMin = 3),
        )
        val b = SessionEndsBreakdown.from(arrows, ends)
        assertThat(b.maxShotsPerEnd).isEqualTo(3)
    }
}
