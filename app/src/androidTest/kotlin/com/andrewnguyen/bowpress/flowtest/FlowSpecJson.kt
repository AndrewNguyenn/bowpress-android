package com.andrewnguyen.bowpress.flowtest

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.IOException

/**
 * JSON decoder for [FlowSpec] documents. Uses kotlinx.serialization with the
 * `type` field as the discriminator (matching `flows/schema.json`).
 */
object FlowSpecJson {

    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = false
        serializersModule = SerializersModule {
            polymorphic(Step::class) {
                subclass(Step.Launch::class)
                subclass(Step.Tap::class)
                subclass(Step.TapTab::class)
                subclass(Step.TypeText::class)
                subclass(Step.Swipe::class)
                subclass(Step.Wait::class)
                subclass(Step.WaitGone::class)
                subclass(Step.AssertText::class)
                subclass(Step.AssertScreen::class)
                subclass(Step.AssertExists::class)
                subclass(Step.AssertAbsent::class)
                subclass(Step.ApiExpect::class)
                subclass(Step.ApiRespond::class)
                subclass(Step.ServerPatch::class)
            }
        }
    }

    fun parse(jsonString: String): FlowSpec = json.decodeFromString(FlowSpec.serializer(), jsonString)

    /**
     * Read every `*.flow.json` file packaged under the test APK's
     * `assets/flows/` directory and return the parsed [FlowSpec]s.
     *
     * Flow files are copied into the test APK assets at build time by the
     * `flowAssets` gradle source-set hook (see `app/build.gradle.kts`).
     */
    fun loadAllFromAssets(): List<FlowSpec> {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val assets = ctx.assets
        val files = assets.list("flows")
            ?.filter { it.endsWith(".flow.json") }
            ?.sorted()
            ?: emptyList()
        return files.map { name ->
            val text = try {
                assets.open("flows/$name").bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                throw IllegalStateException("Could not read flow asset flows/$name", e)
            }
            try {
                parse(text)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse flow flows/$name: ${e.message}", e)
            }
        }
    }

    /**
     * Read a single named flow (e.g. `paywall_purchase_monthly`) from
     * `assets/flows/<name>.flow.json`.
     */
    fun loadFromAssets(name: String): FlowSpec {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val text = ctx.assets.open("flows/$name.flow.json").bufferedReader().use { it.readText() }
        return parse(text)
    }
}
