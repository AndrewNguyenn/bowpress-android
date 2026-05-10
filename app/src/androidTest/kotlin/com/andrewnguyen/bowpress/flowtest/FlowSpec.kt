package com.andrewnguyen.bowpress.flowtest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Typed model for a `flows/<name>.flow.json` document.
 *
 * Mirrors `flows/schema.json`. Both iOS and Android consume this contract;
 * any change here is a contract change — update `schema.json` + the iOS
 * decoder in lockstep.
 */
@Serializable
data class FlowSpec(
    val name: String,
    val description: String,
    val runModes: List<RunMode>,
    /**
     * Lifecycle stamp. `live` flows are runnable today and the parse test
     * cross-validates every tag they reference against `TestTags`. `draft`
     * flows are spec-only — the harness recognizes them but does not (yet)
     * execute or tag-validate them. New flows usually start as `draft`,
     * graduate to `live` when their tags + runner wire-up land.
     */
    val status: FlowStatus = FlowStatus.LIVE,
    val fixtures: Map<String, String> = emptyMap(),
    val launch: LaunchSpec? = null,
    val steps: List<Step>,
)

@Serializable
enum class FlowStatus {
    @SerialName("live") LIVE,
    @SerialName("draft") DRAFT,
}

@Serializable
enum class RunMode {
    @SerialName("mock") MOCK,
    @SerialName("live") LIVE,
}

@Serializable
data class LaunchSpec(
    val autoSignInEmail: String? = null,
    val autoSignInPassword: String? = null,
    val startTab: Int? = null,
    val env: Map<String, String> = emptyMap(),
)

/**
 * Discriminated union of every step type the runner understands. The
 * [type] string drives polymorphic deserialization; see [FlowSpecJson].
 */
@Serializable
sealed class Step {
    @Serializable
    @SerialName("launch")
    data class Launch(val env: Map<String, String> = emptyMap()) : Step()

    @Serializable
    @SerialName("tap")
    data class Tap(val tag: String) : Step()

    @Serializable
    @SerialName("tapTab")
    data class TapTab(val label: TabLabel) : Step()

    @Serializable
    @SerialName("type")
    data class TypeText(
        val tag: String,
        val text: String,
        val clearFirst: Boolean = false,
    ) : Step()

    @Serializable
    @SerialName("swipe")
    data class Swipe(
        val tag: String,
        val direction: SwipeDirection,
        val distance: Float = 0.5f,
    ) : Step()

    @Serializable
    @SerialName("wait")
    data class Wait(val tag: String, val timeout: Double = 5.0) : Step()

    @Serializable
    @SerialName("waitGone")
    data class WaitGone(val tag: String, val timeout: Double = 5.0) : Step()

    @Serializable
    @SerialName("assertText")
    data class AssertText(
        val tag: String,
        val text: String,
        val match: TextMatch = TextMatch.EQUALS,
    ) : Step()

    @Serializable
    @SerialName("assertScreen")
    data class AssertScreen(val route: String) : Step()

    @Serializable
    @SerialName("assertExists")
    data class AssertExists(val tag: String) : Step()

    @Serializable
    @SerialName("assertAbsent")
    data class AssertAbsent(val tag: String) : Step()

    @Serializable
    @SerialName("apiExpect")
    data class ApiExpect(
        val method: HttpMethod,
        val path: String,
        val bodyContains: JsonElement? = null,
    ) : Step()

    @Serializable
    @SerialName("apiRespond")
    data class ApiRespond(
        val method: HttpMethod,
        val path: String,
        val status: Int,
        val body: JsonElement? = null,
        val bodyFile: String? = null,
    ) : Step()

    @Serializable
    @SerialName("serverPatch")
    data class ServerPatch(
        val method: HttpMethod,
        val path: String,
        val body: JsonElement,
    ) : Step()
}

@Serializable
enum class TabLabel {
    /**
     * Android's tab 0 is currently labeled "Home" (Suggestions Dashboard);
     * iOS's tab 0 is "Analytics" (a different surface). Tracked as a
     * parity divergence in BLOCKERS.md item 4 — flow files should avoid
     * `Home`/`Log` until that's resolved.
     */
    @SerialName("Home") HOME,
    @SerialName("Analytics") ANALYTICS,
    @SerialName("Log") LOG,
    @SerialName("Session") SESSION,
    @SerialName("Equipment") EQUIPMENT,
    @SerialName("Settings") SETTINGS,
}

@Serializable
enum class SwipeDirection {
    @SerialName("up") UP,
    @SerialName("down") DOWN,
    @SerialName("left") LEFT,
    @SerialName("right") RIGHT,
}

@Serializable
enum class TextMatch {
    @SerialName("equals") EQUALS,
    @SerialName("contains") CONTAINS,
}

@Serializable
enum class HttpMethod {
    @SerialName("GET") GET,
    @SerialName("POST") POST,
    @SerialName("PUT") PUT,
    @SerialName("PATCH") PATCH,
    @SerialName("DELETE") DELETE,
}
