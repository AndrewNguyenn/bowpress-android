package com.andrewnguyen.bowpress.flowtest

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTouchInput

/**
 * Executes a [FlowSpec] against a [ComposeContentTestRule] driving a
 * Composable hierarchy. Steps that touch the network ([Step.ApiExpect],
 * [Step.ApiRespond], [Step.ServerPatch]) require a [NetworkProbe] to be
 * attached; without one, those steps are no-ops with a console warning.
 *
 * The runner is intentionally stateless — one instance per run — so flow
 * test classes can compose multiple flows without leaking state.
 *
 * Usage:
 * ```
 * @get:Rule val composeRule = createComposeRule()
 *
 * @Test fun signOutFlow() {
 *     composeRule.setContent { App(...) }
 *     val flow = FlowSpecJson.loadFromAssets("settings_sign_out")
 *     FlowRunner(composeRule, networkProbe = null).run(flow)
 * }
 * ```
 */
class FlowRunner(
    private val composeRule: ComposeContentTestRule,
    private val networkProbe: NetworkProbe? = null,
) {

    fun run(flow: FlowSpec) {
        flow.steps.forEachIndexed { index, step ->
            try {
                execute(step)
            } catch (e: Throwable) {
                throw FlowExecutionError(
                    flowName = flow.name,
                    stepIndex = index,
                    step = step,
                    cause = e,
                )
            }
        }
    }

    private fun execute(step: Step) {
        when (step) {
            is Step.Launch -> {
                // Launch is wired by the test class itself before run() is
                // called — env vars must be applied to the host process
                // before the Composable is composed. Treat as a no-op here.
            }

            is Step.Tap -> tagNode(step.tag).performClick()

            is Step.TapTab -> {
                // Tab labels are exposed via Icon.contentDescription in
                // MainScaffold (see TopTab definitions). Match by content
                // description for deterministic targeting.
                composeRule.onNodeWithContentDescription(step.label.displayName())
                    .performClick()
            }

            is Step.TypeText -> {
                val node = tagNode(step.tag)
                if (step.clearFirst) node.performTextClearance()
                node.performTextInput(step.text)
            }

            is Step.Swipe -> {
                tagNode(step.tag).performTouchInput {
                    when (step.direction) {
                        SwipeDirection.UP -> swipeUp()
                        SwipeDirection.DOWN -> swipeDown()
                        SwipeDirection.LEFT -> swipeLeft()
                        SwipeDirection.RIGHT -> swipeRight()
                    }
                }
            }

            is Step.Wait -> waitForTag(step.tag, step.timeout, presence = true)
            is Step.WaitGone -> waitForTag(step.tag, step.timeout, presence = false)

            is Step.AssertText -> {
                val node = tagNode(step.tag)
                when (step.match) {
                    TextMatch.EQUALS -> node.assertTextEquals(step.text)
                    TextMatch.CONTAINS -> node.assertTextContains(step.text)
                }
            }

            is Step.AssertScreen -> {
                // Compose nav routes are addressable via the back stack
                // entry but not directly through ComposeTestRule. Until we
                // wire a NavController probe, this step is a structured TODO
                // — it must be implemented by a test class that owns the
                // NavController. For now, throw clearly so the gap is
                // visible.
                throw NotImplementedError(
                    "assertScreen requires a NavController probe; wire one " +
                        "via FlowRunner constructor (route='${step.route}')."
                )
            }

            is Step.AssertExists -> tagNode(step.tag).assertExists()

            is Step.AssertAbsent -> {
                composeRule.onAllNodesWithTag(step.tag).assertCountEquals(0)
            }

            is Step.ApiExpect -> {
                val probe = networkProbe ?: error(
                    "Flow uses apiExpect (${step.method} ${step.path}) but no " +
                        "NetworkProbe is attached. Either pass a probe to FlowRunner " +
                        "or remove network steps from this flow."
                )
                probe.expect(step.method, step.path, step.bodyContains)
            }

            is Step.ApiRespond -> {
                val probe = networkProbe ?: error(
                    "Flow uses apiRespond (${step.method} ${step.path}) but no " +
                        "NetworkProbe is attached. Either pass a probe to FlowRunner " +
                        "or remove network steps from this flow."
                )
                probe.respond(step)
            }

            is Step.ServerPatch -> {
                throw NotImplementedError(
                    "serverPatch is for live-mode flows — wire an HTTP client to the " +
                        "test backend via FlowRunner constructor."
                )
            }
        }
    }

    private fun tagNode(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun waitForTag(tag: String, timeoutSeconds: Double, presence: Boolean) {
        val timeoutMillis = (timeoutSeconds * 1_000L).toLong()
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            val matches = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes(
                atLeastOneRootRequired = false,
            )
            if (presence) matches.isNotEmpty() else matches.isEmpty()
        }
    }
}

private fun TabLabel.displayName(): String = when (this) {
    TabLabel.HOME -> "Home"
    TabLabel.ANALYTICS -> "Analytics"
    TabLabel.LOG -> "Log"
    TabLabel.SESSION -> "Session"
    TabLabel.EQUIPMENT -> "Equipment"
    TabLabel.SETTINGS -> "Settings"
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(
    expected: Int,
) {
    val actual = fetchSemanticsNodes(atLeastOneRootRequired = false).size
    check(actual == expected) {
        "Expected $expected nodes, found $actual"
    }
}

class FlowExecutionError(
    val flowName: String,
    val stepIndex: Int,
    val step: Step,
    cause: Throwable,
) : AssertionError(
    "Flow '$flowName' failed at step $stepIndex (${step::class.simpleName}): " +
        "${cause.message}",
    cause,
)
