package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * Decodes ISO-8601 timestamps emitted by the BowPress backend. Mirrors the Swift
 * custom JSONDecoder date strategy in `APIClient.swift`:
 *
 *   - `2026-04-22T12:34:56Z`           (plain)
 *   - `2026-04-22T12:34:56.789Z`       (with fractional seconds)
 *
 * Encoding always emits milliseconds — matches the iOS encoder.
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    // Writer — always fractional ms + `Z`. The `.appendFraction(... 3, 3, ...)` call forces exactly
    // three decimals so encoded values match what iOS produces.
    private val encoderFormatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .appendLiteral('Z')
            .toFormatter()

    override fun serialize(encoder: Encoder, value: Instant) {
        val formatted = encoderFormatter.format(value.atOffset(java.time.ZoneOffset.UTC))
        encoder.encodeString(formatted)
    }

    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString()
        // `OffsetDateTime.parse` accepts both the plain and fractional-second variants
        // when given `ISO_OFFSET_DATE_TIME`. Fall back to `Instant.parse` for safety.
        return try {
            OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        } catch (_: Exception) {
            Instant.parse(raw)
        }
    }
}
