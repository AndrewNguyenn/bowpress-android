package com.andrewnguyen.bowpress.flowtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.andrewnguyen.bowpress.core.designsystem.testing.TestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sanity-check that every flow JSON file under `flows/` parses cleanly
 * against [FlowSpec], and that every tag referenced by a `live`-status
 * flow exists in the canonical [TestTags] registry. Runs in milliseconds
 * — no Compose rule, no app launch.
 *
 * If you add a step type to the schema that isn't registered in
 * [FlowSpecJson.json]'s polymorphic module, this test fails first.
 *
 * If you add a tag string to a flow file that isn't in [TestTags] (or
 * its `*Prefix` constants for dynamic ids), this test fails too —
 * preventing flows from referencing non-existent Composable tags.
 *
 * Flows with `status: "draft"` are exempt from tag validation. They
 * exist as parity placeholders that capture iOS behavior we haven't
 * yet wired into Android Composables. Graduate to `status: "live"`
 * by adding the missing testTag(s) to the relevant Composable.
 */
@RunWith(AndroidJUnit4::class)
class FlowSpecParseTest {

    @Test
    fun all_flow_files_parse() {
        val flows = FlowSpecJson.loadAllFromAssets()
        assertFalse(
            "No flow files found in test assets — " +
                "is the assets/flows copy step wired in app/build.gradle.kts?",
            flows.isEmpty(),
        )
        flows.forEach { flow ->
            assertNotNull("Flow ${flow.name} has null name", flow.name)
            assertFalse(
                "Flow ${flow.name} has empty steps list",
                flow.steps.isEmpty(),
            )
        }
    }

    @Test
    fun live_flows_only_reference_known_tags() {
        val knownTags = collectKnownTags()
        val knownPrefixes = collectKnownPrefixes()
        val violations = mutableListOf<String>()

        FlowSpecJson.loadAllFromAssets()
            .filter { it.status == FlowStatus.LIVE }
            .forEach { flow ->
                flow.steps.forEach { step ->
                    val tag = step.referencedTag() ?: return@forEach
                    val ok = tag in knownTags ||
                        knownPrefixes.any { tag.startsWith(it) }
                    if (!ok) {
                        violations.add(
                            "Flow '${flow.name}' references unknown tag '$tag' (step ${step::class.simpleName}). " +
                                "Either add it to TestTags + apply at the Composable, or mark the flow status: \"draft\"."
                        )
                    }
                }
            }

        if (violations.isNotEmpty()) {
            throw AssertionError(violations.joinToString("\n"))
        }
    }

    @Test
    fun paywall_purchase_monthly_round_trips() {
        // Anchor on the canonical iOS-mirroring flow specifically.
        val flow = FlowSpecJson.loadFromAssets("paywall_purchase_monthly")
        assert(flow.runModes.contains(RunMode.LIVE)) {
            "paywall_purchase_monthly must include 'live' run mode"
        }
        // Step sequence anchor: must wait for splash, tap Equipment, wait
        // for upgrade banner, tap, wait for monthly button, tap, then wait
        // for banner gone. iOS PaywallUITests.testPaywallPurchase enforces
        // the same order.
        val tags = flow.steps.mapNotNull {
            when (it) {
                is Step.Wait -> it.tag
                is Step.WaitGone -> "!${it.tag}"
                is Step.Tap -> "tap:${it.tag}"
                is Step.TapTab -> "tab:${it.label.name}"
                else -> null
            }
        }
        check(tags.indexOf("!hydration_splash") < tags.indexOf("tab:EQUIPMENT")) {
            "Flow must wait for hydration splash to clear before tapping Equipment tab"
        }
        check(tags.indexOf("upgrade_banner") < tags.indexOf("tap:upgrade_banner")) {
            "Flow must wait for upgrade banner before tapping it"
        }
        check(tags.indexOf("paywall_monthly_button") < tags.indexOf("tap:paywall_monthly_button")) {
            "Flow must wait for monthly button before tapping it"
        }
        // The "banner gone" assertion must come AFTER the monthly tap. Use
        // drop+contains so a future flow that does waitGone twice (pre-tap
        // and post-tap) still has its post-tap assertion validated.
        val tapIdx = tags.indexOf("tap:paywall_monthly_button")
        check(tags.drop(tapIdx + 1).contains("!upgrade_banner")) {
            "Flow must wait for upgrade banner gone AFTER tapping monthly button"
        }
    }

    // -- helpers -----------------------------------------------------------

    /**
     * Pull every non-prefix `String` constant out of [TestTags] via reflection
     * so the test can't drift from the registry. Excludes constants whose name
     * ends with `Prefix` — those are handled separately.
     */
    private fun collectKnownTags(): Set<String> =
        TestTags::class.java.declaredFields
            .filter { it.type == String::class.java }
            .filterNot { it.name.endsWith("Prefix") }
            .mapNotNull { it.get(null) as? String }
            .toSet()

    private fun collectKnownPrefixes(): Set<String> =
        TestTags::class.java.declaredFields
            .filter { it.type == String::class.java }
            .filter { it.name.endsWith("Prefix") }
            .mapNotNull { it.get(null) as? String }
            .toSet()

    /** The single tag a step references, or null for steps that don't carry one. */
    private fun Step.referencedTag(): String? = when (this) {
        is Step.Tap -> tag
        is Step.TypeText -> tag
        is Step.Swipe -> tag
        is Step.Wait -> tag
        is Step.WaitGone -> tag
        is Step.AssertText -> tag
        is Step.AssertExists -> tag
        is Step.AssertAbsent -> tag
        // Tab-targeting + screen route + api + launch don't reference a tag.
        is Step.TapTab, is Step.AssertScreen, is Step.ApiExpect,
        is Step.ApiRespond, is Step.ServerPatch, is Step.Launch -> null
    }
}
