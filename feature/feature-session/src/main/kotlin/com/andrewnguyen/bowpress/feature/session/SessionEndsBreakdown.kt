package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd

/**
 * Live-session end breakdown — the Android analogue of the iOS
 * `SessionViewModel.completedEndsBreakdown` + `currentEndArrows`
 * (bowpress-ios/Sources/BowPress/Session/SessionViewModel.swift).
 *
 * iOS keeps the live breakdown positional (`endArrowCounts` slicing the flat
 * `allArrows`) because, on iOS, arrows aren't stamped with an `endId` until
 * their end is completed. The Android live session DOES stamp `endId` at
 * [SessionViewModel.completeEnd] time (see that method), so this breakdown is
 * derived purely by grouping the reactive plot stream by `endId`:
 *
 *  - a [completedEndLine] per recorded [SessionEnd], holding the arrows whose
 *    `endId` matches it (in `shotAt` order — `currentArrows` is already sorted);
 *  - the [inProgressArrows] — every arrow with no `endId` (the end the archer
 *    is plotting right now), which becomes the next end on `completeEnd`.
 *
 * The slices plus [inProgressArrows] partition `currentArrows` exactly, so the
 * scorecard TOTAL always equals the sum over every plotted arrow — the same
 * invariant the iOS comment calls out.
 */
data class SessionEndsBreakdown(
    /** Completed ends with their arrows, in `endNumber` order. */
    val completedEndLines: List<EndLine>,
    /** Arrows in the end currently being plotted (no `endId` yet). */
    val inProgressArrows: List<ArrowPlot>,
) {
    /** One completed end paired with the arrows that belong to it. */
    data class EndLine(
        val end: SessionEnd,
        val arrows: List<ArrowPlot>,
    ) {
        /** End total — X (ring 11) and a 10 both score 10; a miss scores 0. */
        val sum: Int get() = arrows.sumOf { score(it.ring) }
        val xCount: Int get() = arrows.count { it.ring == 11 }
    }

    /** True when there is at least one completed end to show in the history. */
    val hasCompletedEnds: Boolean get() = completedEndLines.isNotEmpty()

    /**
     * The display number of the end currently being plotted. Uses
     * `max(endNumber) + 1` rather than `count + 1` so deleting a completed end
     * leaves a gap — the next end won't reuse a deleted end's number. Mirrors
     * iOS `currentEndNumber`.
     */
    val currentEndNumber: Int
        get() = (completedEndLines.maxOfOrNull { it.end.endNumber } ?: 0) + 1

    /** Widest end so far — drives the scorecard's shot-column count. */
    val maxShotsPerEnd: Int
        get() = completedEndLines.maxOfOrNull { it.arrows.size } ?: 3

    companion object {
        /** Ring value clamped to scoring range — matches iOS `Line.score(of:)`. */
        fun score(ring: Int): Int = ring.coerceIn(0, 10)

        /**
         * Derive the breakdown from the reactive session state.
         *
         * @param arrows every plot for the active session, in `shotAt` order
         *   (as delivered by `PlotRepository.observeBySession`).
         * @param ends the recorded [SessionEnd]s, in `endNumber` order
         *   (as delivered by `SessionEndRepository.observeBySession`).
         */
        fun from(arrows: List<ArrowPlot>, ends: List<SessionEnd>): SessionEndsBreakdown {
            val byEndId = arrows.filter { it.endId != null }.groupBy { it.endId }
            val recordedIds = ends.mapTo(HashSet()) { it.id }
            val completedLines = ends.map { end ->
                EndLine(end = end, arrows = byEndId[end.id].orEmpty())
            }
            // Arrows with no endId — or a stale endId pointing at a deleted
            // end — are the in-progress end (iOS `currentEndArrows`).
            val inProgress = arrows.filter { it.endId == null || it.endId !in recordedIds }
            return SessionEndsBreakdown(
                completedEndLines = completedLines,
                inProgressArrows = inProgress,
            )
        }
    }
}
