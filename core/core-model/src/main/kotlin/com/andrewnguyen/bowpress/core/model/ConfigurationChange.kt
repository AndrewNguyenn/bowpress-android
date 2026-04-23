package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant

/**
 * Mirrors iOS `ChangeImpactCard`. Server-populated (spec §Stage 4); nil until
 * the pipeline has seen both sides of a change.
 */
@Serializable
data class ChangeImpactCard(
    val scoreBefore: Double? = null,
    val scoreAfter: Double? = null,
    val scoreDelta: Double? = null,
    val classification: ChangeClassification,
    val feelTagsBefore: List<String> = emptyList(),
    val feelTagsAfter: List<String> = emptyList(),
)

/**
 * Mirrors iOS `ConfigurationChange`. `changedFields[i].fromValue`/`toValue` can come
 * back as either a string or a number on the wire — we coerce both to String for display,
 * matching iOS's `decodeStringOrNumber`.
 */
@Serializable
data class ConfigurationChange(
    val id: String,
    val bowId: String,
    val fromConfigId: String,
    val toConfigId: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val changedFields: List<FieldChange> = emptyList(),
    val changeCount: Int,
    val notes: String? = null,
    val impact: ChangeImpactCard? = null,
) {
    @Serializable
    data class FieldChange(
        val field: String,
        @Serializable(with = StringOrNumberAsStringSerializer::class)
        val fromValue: String,
        @Serializable(with = StringOrNumberAsStringSerializer::class)
        val toValue: String,
    )
}

/**
 * Coerce a wire string-or-number into a display String. Integer-valued doubles render
 * without trailing zeros (e.g. `9.0` → "9"), matching iOS's `String(format: "%g", d)`.
 */
internal object StringOrNumberAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrNumberAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        if (decoder !is JsonDecoder) return decoder.decodeString()
        val el = decoder.decodeJsonElement()
        if (el !is JsonPrimitive) return el.toString()
        if (el.isString) return el.content
        // Numeric: prefer long when round, otherwise %g-style formatting.
        el.longOrNull?.let { return it.toString() }
        el.doubleOrNull?.let { d ->
            return if (d == d.toLong().toDouble()) d.toLong().toString()
            else d.toString().trimEnd('0').trimEnd('.')
        }
        return el.content
    }
}
