package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.feed.ledgerGlyphSizeSp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The 50/50 card's per-end ledger lives in a narrow column. A wide end (a
 * 5- or 6-arrow indoor end) would crush its equal-weight arrow cells, so the
 * glyph scales down for wide ends — `ledgerGlyphSizeSp` decides by how much.
 */
class LedgerGlyphSizeTest {

    @Test
    fun `a 3-arrow outdoor end uses the full glyph size`() {
        assertThat(ledgerGlyphSizeSp(3)).isEqualTo(13.5f)
    }

    @Test
    fun `a 5-arrow end shrinks the glyph one step`() {
        assertThat(ledgerGlyphSizeSp(5)).isEqualTo(12f)
    }

    @Test
    fun `a 6-arrow indoor end shrinks the glyph furthest`() {
        assertThat(ledgerGlyphSizeSp(6)).isEqualTo(10.5f)
    }

    @Test
    fun `the glyph never grows back for ends wider than 6`() {
        // Defensive — an 8-arrow end (no WA face has one, but the data could)
        // stays at the smallest size rather than overflowing.
        assertThat(ledgerGlyphSizeSp(8)).isEqualTo(10.5f)
    }

    @Test
    fun `the glyph shrinks monotonically as the end widens`() {
        // More arrows in the row must never mean a larger glyph.
        val sizes = (1..8).map { ledgerGlyphSizeSp(it) }
        sizes.zipWithNext().forEach { (narrower, wider) ->
            assertThat(wider).isAtMost(narrower)
        }
    }
}
