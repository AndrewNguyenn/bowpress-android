package com.andrewnguyen.bowpress.feature.session

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.andrewnguyen.bowpress.core.model.Zone
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Smoke Compose UI test — drags upward through the target, and asserts the emitted
 * plot is in the northern hemisphere (plotY < 0) with a valid ring. `TargetPlot`
 * applies an ~80dp touch-offset (matching iOS `touchOffset` in TargetPlotView.swift:74)
 * so the effective plot lands above the actual finger position.
 */
class TargetPlotComposeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun drag_upward_emits_plot_in_northern_hemisphere() {
        var capturedRing: Int? = null
        var capturedZone: Zone? = null
        var capturedX: Double? = null
        var capturedY: Double? = null

        composeRule.setContent {
            TargetPlot(
                arrows = emptyList(),
                onArrowPlotted = { x, y, ring, zone ->
                    capturedX = x
                    capturedY = y
                    capturedRing = ring
                    capturedZone = zone
                },
                modifier = Modifier.size(400.dp),
                arrowDiameterMm = 5.0,
            )
        }

        composeRule.onNodeWithTag(TARGET_PLOT_TEST_TAG).performTouchInput {
            val cx = width / 2f
            val cy = height / 2f
            // Big upward drag so we easily clear touch slop. End above the centre so
            // that, after the 80dp lift, the plot is even further north.
            swipe(
                start = Offset(cx, cy + height * 0.3f),
                end = Offset(cx, cy - height * 0.1f),
                durationMillis = 200,
            )
        }

        composeRule.waitForIdle()

        val ring = capturedRing
        val zone = capturedZone
        val py = capturedY
        val px = capturedX
        assert(ring != null) { "Expected onArrowPlotted to fire" }
        // Plot should land in the northern hemisphere — zone N (or CENTER if the lift
        // happened to put us inside centerZoneRadius).
        assert(zone == Zone.N || zone == Zone.CENTER) {
            "Expected N/CENTER zone, got $zone"
        }
        // plotY < 0 means north in the iOS storage convention.
        assert(py != null && py < 0.05) {
            "Expected plotY in northern hemisphere (≤ ~0), got $py"
        }
        // Horizontal drag was zero, so plotX should be close to 0.
        assert(px != null && abs(px) < 0.1) {
            "Expected plotX near 0, got $px"
        }
    }
}
