package com.andrewnguyen.bowpress.core.model

/**
 * Pure, view-agnostic per-end scoring model for a session. Mirrors iOS
 * `SessionScorecard` (Models/SessionScorecard.swift) — minus the iOS
 * `Line.isSynthetic` flag, which nothing on the Android screen renders.
 *
 * The invariant that matters — and the one a whole class of "the numbers
 * don't add up" bug reports comes down to — is: every arrow lands in
 * exactly one line, so the sum of every line equals the session total.
 */
data class Scorecard(val lines: List<Line>) {

    /**
     * One row of the scorecard — a recorded (or synthesized) end and the
     * arrows that belong to it. [arrows] is in `shotAt` order — [build]
     * sorts before grouping and `groupBy` preserves input order.
     */
    data class Line(
        val end: SessionEnd,
        val arrows: List<ArrowPlot>,
    ) {
        /** End total — X (ring 11) and a 10 both score 10; a miss scores 0. */
        val sum: Int get() = arrows.sumOf { score(it.ring) }
        val xCount: Int get() = arrows.count { it.ring == 11 }
    }

    /** Number of ends (rows) in the scorecard. */
    val endCount: Int get() = lines.size

    /** Sum of every line — i.e. every arrow in the session. */
    val totalScore: Int get() = lines.sumOf { it.sum }
    val totalArrows: Int get() = lines.sumOf { it.arrows.size }
    val totalXCount: Int get() = lines.sumOf { it.xCount }

    /** Best score the session could have posted — `totalArrows × 10`. */
    val maxPossibleScore: Int get() = totalArrows * 10

    /**
     * Widest end in the session — drives the table's shot-column count so a
     * 3-arrow 3-spot end and a 6-arrow recurve end both render without empty
     * padding columns. Falls back to 3 for an empty card.
     */
    val maxShotsPerEnd: Int get() = lines.maxOfOrNull { it.arrows.size } ?: 3

    companion object {
        /** Ring value clamped to scoring range — matches iOS `Line.score(of:)`. */
        fun score(ring: Int): Int = ring.coerceIn(0, 10)

        /**
         * Build the per-end breakdown for a session. Mirrors iOS
         * `SessionScorecard.make`:
         *  - With recorded [ends], arrows are matched by `endId`; any arrow
         *    that matches no recorded end (nil `endId`, or a stale id) is
         *    collected into one trailing "in progress" line so it still
         *    counts toward the total.
         *  - With no recorded ends, arrows are chunked [synthChunkSize] at a
         *    time so the breakdown still renders for legacy / mock sessions.
         */
        fun build(
            arrows: List<ArrowPlot>,
            ends: List<SessionEnd>,
            sessionId: String,
            synthChunkSize: Int = 3,
        ): Scorecard {
            val sorted = arrows.sortedBy { it.shotAt }

            if (ends.isNotEmpty()) {
                val byEndId = sorted.filter { it.endId != null }.groupBy { it.endId!! }
                // Drop recorded ends with zero arrows. They surface when an
                // end is created but every arrow under it is deleted / never
                // assigned (e.g. a sync race orphans the arrows). Rendering
                // them as a "0" row looks like a scoring bug to the archer.
                // Orphans below still cover the unattributed arrows so the
                // partition invariant holds.
                val lines = ends.mapNotNull { end ->
                    val endArrows = byEndId[end.id].orEmpty()
                    if (endArrows.isEmpty()) null else Line(end = end, arrows = endArrows)
                }.toMutableList()

                val recordedIds = ends.map { it.id }.toSet()
                val orphans = sorted.filter { it.endId == null || it.endId !in recordedIds }
                if (orphans.isNotEmpty()) {
                    lines += Line(
                        end = SessionEnd(
                            id = "inprogress-$sessionId",
                            sessionId = sessionId,
                            endNumber = lines.size + 1,
                            notes = null,
                            completedAt = orphans.last().shotAt,
                        ),
                        arrows = orphans,
                    )
                }
                // Renumber the surviving lines top-down so dropped empty
                // ends don't leave a visible gap in the END column (1, 2,
                // 3, 5 would look like a render bug). `endNumber` on this
                // model is only ever a sort + display value, never an FK,
                // so re-sequencing it is safe.
                val renumbered = lines.mapIndexed { idx, line ->
                    line.copy(end = line.end.copy(endNumber = idx + 1))
                }
                return Scorecard(renumbered)
            }

            // No recorded ends — chunk the arrows so the breakdown still renders.
            if (sorted.isEmpty()) return Scorecard(emptyList())
            val lines = sorted.chunked(synthChunkSize).mapIndexed { idx, chunk ->
                Line(
                    end = SessionEnd(
                        id = "synth-$sessionId-${idx + 1}",
                        sessionId = sessionId,
                        endNumber = idx + 1,
                        notes = null,
                        completedAt = chunk.last().shotAt,
                    ),
                    arrows = chunk,
                )
            }
            return Scorecard(lines)
        }
    }
}
