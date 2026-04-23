package com.andrewnguyen.bowpress.core.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Instant

/**
 * Round-trip coverage — serialise each DTO, deserialise, assert equality. Also
 * verifies the custom [InstantSerializer] accepts both plain and fractional-second
 * ISO-8601 inputs.
 */
class DtoRoundTripTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `User round-trips`() {
        val user = User(
            id = "u1",
            email = "a@b.com",
            name = "Archer",
            createdAt = Instant.parse("2026-04-22T12:34:56Z"),
            emailVerified = true,
            authProvider = AuthProvider.EMAIL,
        )
        val encoded = json.encodeToString(User.serializer(), user)
        val decoded = json.decodeFromString(User.serializer(), encoded)
        assertThat(decoded).isEqualTo(user)
    }

    @Test
    fun `Bow round-trips with all fields`() {
        val bow = Bow(
            id = "b1",
            userId = "u1",
            name = "Hoyt",
            bowType = BowType.COMPOUND,
            brand = "Hoyt",
            model = "RX-7",
            createdAt = Instant.parse("2026-04-22T12:34:56.789Z"),
        )
        val encoded = json.encodeToString(Bow.serializer(), bow)
        val decoded = json.decodeFromString(Bow.serializer(), encoded)
        assertThat(decoded).isEqualTo(bow)
    }

    @Test
    fun `BowConfiguration round-trips with nullable fields`() {
        val cfg = BowConfiguration(
            id = "c1",
            bowId = "b1",
            createdAt = Instant.parse("2026-04-22T12:34:56Z"),
            label = "Initial",
            drawLength = 28.0,
            letOffPct = 80.0,
            peepHeight = 9.0,
            dLoopLength = 2.0,
            topCableTwists = 0,
            bottomCableTwists = 0,
            mainStringTopTwists = 0,
            mainStringBottomTwists = 0,
            topLimbTurns = 0.0,
            bottomLimbTurns = 0.0,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            sightPosition = 0,
            gripAngle = 0.0,
            nockingHeight = 0,
            frontStabWeight = 0.0,
            frontStabAngle = 0.0,
            rearStabSide = RearStabSide.NONE,
            rearStabWeight = 0.0,
            rearStabVertAngle = 0.0,
            rearStabHorizAngle = 0.0,
        )
        val encoded = json.encodeToString(BowConfiguration.serializer(), cfg)
        val decoded = json.decodeFromString(BowConfiguration.serializer(), encoded)
        assertThat(decoded).isEqualTo(cfg)
    }

    @Test
    fun `ArrowPlot round-trips with zone enum`() {
        val plot = ArrowPlot(
            id = "p1",
            sessionId = "s1",
            bowConfigId = "bc1",
            arrowConfigId = "ac1",
            ring = 11,
            zone = Zone.CENTER,
            plotX = 0.01,
            plotY = -0.02,
            endId = "e1",
            shotAt = Instant.parse("2026-04-22T12:34:56Z"),
            excluded = false,
            notes = null,
        )
        val encoded = json.encodeToString(ArrowPlot.serializer(), plot)
        val decoded = json.decodeFromString(ArrowPlot.serializer(), encoded)
        assertThat(decoded).isEqualTo(plot)
    }

    @Test
    fun `AnalyticsSuggestion round-trips and tolerates missing evidence`() {
        val json = Json { ignoreUnknownKeys = true }
        val withoutEvidence = """
            {
              "id": "s1",
              "bowId": "b1",
              "createdAt": "2026-04-22T12:34:56.000Z",
              "parameter": "peepHeight",
              "suggestedValue": "9.5",
              "currentValue": "9.0",
              "reasoning": "rise",
              "confidence": 0.8,
              "wasRead": false,
              "deliveryType": "inApp"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(AnalyticsSuggestion.serializer(), withoutEvidence)
        assertThat(decoded.evidence).isNull()
        assertThat(decoded.wasApplied).isFalse()
        assertThat(decoded.deliveryType).isEqualTo(DeliveryType.IN_APP)
    }

    @Test
    fun `InstantSerializer accepts both plain and fractional-second forms`() {
        val fractional = """"2026-04-22T12:34:56.789Z""""
        val plain = """"2026-04-22T12:34:56Z""""
        val fracInstant = json.decodeFromString(InstantSerializer, fractional)
        val plainInstant = json.decodeFromString(InstantSerializer, plain)
        assertThat(fracInstant).isEqualTo(Instant.parse("2026-04-22T12:34:56.789Z"))
        assertThat(plainInstant).isEqualTo(Instant.parse("2026-04-22T12:34:56Z"))
    }

    @Test
    fun `ConfigurationChange coerces string-or-number field values`() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """
            {
              "id": "cc1",
              "bowId": "b1",
              "fromConfigId": "c1",
              "toConfigId": "c2",
              "createdAt": "2026-04-22T12:34:56Z",
              "changedFields": [
                {"field": "peepHeight", "fromValue": 9, "toValue": 9.5},
                {"field": "rearStabSide", "fromValue": "left", "toValue": "right"}
              ],
              "changeCount": 2
            }
        """.trimIndent()
        val decoded = json.decodeFromString(ConfigurationChange.serializer(), raw)
        assertThat(decoded.changedFields[0].fromValue).isEqualTo("9")
        assertThat(decoded.changedFields[0].toValue).isEqualTo("9.5")
        assertThat(decoded.changedFields[1].fromValue).isEqualTo("left")
    }

    @Test
    fun `TagCorrelation decodes snake_case wire keys`() {
        val raw = """
            {
              "bow_id": "b1",
              "user_id": "u1",
              "tag": "wind",
              "tagged_session_count": 5,
              "untagged_session_count": 10,
              "avg_score_tagged": 9.1,
              "avg_score_untagged": 9.6,
              "score_delta": -0.5,
              "strength": "moderate",
              "updated_at": "2026-04-22T12:34:56Z"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(TagCorrelation.serializer(), raw)
        assertThat(decoded.bowId).isEqualTo("b1")
        assertThat(decoded.strength).isEqualTo(TagStrength.MODERATE)
    }
}
