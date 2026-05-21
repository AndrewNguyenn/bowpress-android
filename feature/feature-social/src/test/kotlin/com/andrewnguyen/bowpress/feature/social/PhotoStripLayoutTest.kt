package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.feature.social.ui.feed.PhotoStripLayout
import com.andrewnguyen.bowpress.feature.social.ui.feed.photoStripLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Section 4 — the activity card's photo strip flexes its layout with the
 * count of `ready` photos. `photoStripLayout` is the pure count→layout
 * mapping; this pins each of the design's four layouts and the "+N more"
 * overflow math.
 */
class PhotoStripLayoutTest {

    @Test
    fun `zero photos draws no strip`() {
        // No photos → null → the caller renders no strip and no hairline.
        assertThat(photoStripLayout(0)).isNull()
    }

    @Test
    fun `a negative count is treated as no strip`() {
        // Defensive — a count can never go below 0, but the mapping must not
        // throw or invent a layout if it somehow does.
        assertThat(photoStripLayout(-1)).isNull()
    }

    @Test
    fun `one photo fills the width at 4 by 3`() {
        assertThat(photoStripLayout(1)).isEqualTo(PhotoStripLayout.Single)
        assertThat(photoStripLayout(1)!!.visibleCells).isEqualTo(1)
    }

    @Test
    fun `two photos split 50 50`() {
        assertThat(photoStripLayout(2)).isEqualTo(PhotoStripLayout.Pair)
        assertThat(photoStripLayout(2)!!.visibleCells).isEqualTo(2)
    }

    @Test
    fun `three photos use the big-left composition`() {
        assertThat(photoStripLayout(3)).isEqualTo(PhotoStripLayout.Trio)
        assertThat(photoStripLayout(3)!!.visibleCells).isEqualTo(3)
    }

    @Test
    fun `exactly four photos is a 2x2 grid with no overflow`() {
        // 4 photos fill the grid exactly — the 4th cell shows the photo, not
        // a "+N" overlay.
        assertThat(photoStripLayout(4)).isEqualTo(PhotoStripLayout.Grid(overflow = 0))
        assertThat(photoStripLayout(4)!!.visibleCells).isEqualTo(4)
    }

    @Test
    fun `five photos show plus one more`() {
        // 5 ready → 4 cells, 4th carries "+N" where N = total − 3 = 2.
        assertThat(photoStripLayout(5)).isEqualTo(PhotoStripLayout.Grid(overflow = 2))
    }

    @Test
    fun `seven photos show plus four more`() {
        // The design's State C: 7 photos → 2x2 grid, 4th cell "+4 more".
        // overflow = 7 − 3 = 4 (three photos sit in the other cells).
        assertThat(photoStripLayout(7)).isEqualTo(PhotoStripLayout.Grid(overflow = 4))
    }

    @Test
    fun `the overflow count is total minus three for more than 4 photos`() {
        // The grid never shows more than 4 cells; past 4 photos the overflow
        // on the last cell is (total − 3) — 3 photos sit uncovered, the 4th
        // cell's photo is counted into the "+N". Exactly 4 has no overflow.
        for (total in 5..40) {
            val layout = photoStripLayout(total)
            assertThat(layout).isInstanceOf(PhotoStripLayout.Grid::class.java)
            assertThat((layout as PhotoStripLayout.Grid).overflow).isEqualTo(total - 3)
            assertThat(layout.visibleCells).isEqualTo(4)
        }
    }
}
