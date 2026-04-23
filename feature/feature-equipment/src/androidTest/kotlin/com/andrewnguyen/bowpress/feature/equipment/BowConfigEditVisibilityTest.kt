package com.andrewnguyen.bowpress.feature.equipment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigEditCallbacks
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigEditFormBody
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigEditViewModel
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test acceptance criterion from task #4:
 *
 *   (a) selecting `compound` bow type shows the let-off field and hides brace height
 *   (b) selecting `recurve` inverts this
 *
 * We render the stateless form body directly so the test stays hermetic — no
 * Hilt, no repositories, no navigation. If the visibility predicate ever drifts
 * from the iOS source, both this test and the pure predicate unit tests will fire.
 */
class BowConfigEditVisibilityTest {

    @get:Rule val composeRule = createComposeRule()

    private val callbacks = object : BowConfigEditCallbacks {
        override fun updateLabel(v: String) = Unit
        override fun updateDrawLength(v: Double) = Unit
        override fun updateRestVertical(v: Int) = Unit
        override fun updateRestHorizontal(v: Int) = Unit
        override fun updateRestDepth(v: Double) = Unit
        override fun updateSightPosition(v: Int) = Unit
        override fun updateGripAngle(v: Double) = Unit
        override fun updateNockingHeight(v: Int) = Unit
        override fun updateLetOff(v: Double) = Unit
        override fun updatePeepHeight(v: Double) = Unit
        override fun updateDLoop(v: Double) = Unit
        override fun updateTopCable(v: Int) = Unit
        override fun updateBottomCable(v: Int) = Unit
        override fun updateMainStringTop(v: Int) = Unit
        override fun updateMainStringBottom(v: Int) = Unit
        override fun updateTopLimb(v: Double) = Unit
        override fun updateBottomLimb(v: Double) = Unit
        override fun updateFrontStabWeight(v: Double) = Unit
        override fun updateFrontStabAngle(v: Double) = Unit
        override fun updateRearStabSide(v: RearStabSide) = Unit
        override fun updateRearStabWeight(v: Double) = Unit
        override fun updateRearStabVertAngle(v: Double) = Unit
        override fun updateRearStabHorizAngle(v: Double) = Unit
        override fun updateBraceHeight(v: Double) = Unit
        override fun updateTillerTop(v: Double) = Unit
        override fun updateTillerBottom(v: Double) = Unit
        override fun updatePlungerTension(v: Int) = Unit
        override fun updateClickerPosition(v: Double) = Unit
        override fun updateRearStabLeftWeight(v: Double) = Unit
        override fun updateRearStabRightWeight(v: Double) = Unit
    }

    private fun baseState() = BowConfigEditViewModel.UiState(isLoading = false)

    @Test
    fun compound_shows_let_off_and_hides_brace_height() {
        composeRule.setContent {
            BowConfigEditFormBody(
                state = baseState(),
                bowType = BowType.COMPOUND,
                isSetup = true,
                callbacks = callbacks,
            )
        }
        composeRule.onNodeWithText("Let-off").assertIsDisplayed()
        composeRule.onNodeWithText("Brace Height").assertDoesNotExist()
    }

    @Test
    fun recurve_shows_brace_height_and_hides_let_off() {
        composeRule.setContent {
            BowConfigEditFormBody(
                state = baseState(),
                bowType = BowType.RECURVE,
                isSetup = true,
                callbacks = callbacks,
            )
        }
        composeRule.onNodeWithText("Brace Height").assertIsDisplayed()
        composeRule.onNodeWithText("Let-off").assertDoesNotExist()
    }

    @Test
    fun barebow_hides_all_stabilisers() {
        composeRule.setContent {
            BowConfigEditFormBody(
                state = baseState(),
                bowType = BowType.BAREBOW,
                isSetup = true,
                callbacks = callbacks,
            )
        }
        composeRule.onNodeWithText("Brace Height").assertIsDisplayed()
        composeRule.onNodeWithText("Front Stabilizer").assertDoesNotExist()
        composeRule.onNodeWithText("V-Bar (Rear Stabilizer)").assertDoesNotExist()
        composeRule.onNodeWithText("Rear Stabilizer").assertDoesNotExist()
        composeRule.onNodeWithText("Clicker").assertDoesNotExist()
    }
}
