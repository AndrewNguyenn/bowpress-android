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
    fun `BowConfiguration round-trips compound BOTH with left and right rear-stab weights`() {
        val cfg = BowConfiguration(
            id = "c2",
            bowId = "b1",
            createdAt = Instant.parse("2026-04-22T12:34:56Z"),
            label = "Both rear stabs",
            drawLength = 28.5,
            restVertical = 0,
            restHorizontal = 0,
            restDepth = 0.0,
            gripAngle = 0.0,
            nockingHeight = 0,
            rearStabSide = RearStabSide.BOTH,
            rearStabWeight = null,
            rearStabLeftWeight = 4.5,
            rearStabRightWeight = 6.0,
            rearStabVertAngle = -10.0,
            rearStabHorizAngle = 20.0,
        )
        val encoded = json.encodeToString(BowConfiguration.serializer(), cfg)
        val decoded = json.decodeFromString(BowConfiguration.serializer(), encoded)
        assertThat(decoded).isEqualTo(cfg)
        assertThat(decoded.rearStabWeight).isNull()
        assertThat(decoded.rearStabLeftWeight).isEqualTo(4.5)
        assertThat(decoded.rearStabRightWeight).isEqualTo(6.0)
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

    @Test
    fun `SharedSessionDetail round-trips with nested session, ends, and arrows`() {
        val detail = SharedSessionDetail(
            sharedSession = SharedSession(
                id = "ss-1",
                userId = "u-1",
                sessionId = "sess-1",
                score = 196,
                xCount = 9,
                arrowCount = 18,
                distance = "50m",
                face = "10-Ring",
                title = "Pre-comp check",
                shotAt = Instant.parse("2026-05-19T08:00:00Z"),
                createdAt = Instant.parse("2026-05-19T09:00:00Z"),
            ),
            ownerHandle = "sara.l",
            ownerDisplayName = "Sara Lin",
            session = ShootingSession(
                id = "sess-1",
                bowId = "bow-1",
                bowConfigId = "cfg-1",
                arrowConfigId = "arrow-1",
                startedAt = Instant.parse("2026-05-19T08:00:00Z"),
            ),
            ends = listOf(
                SessionEnd(
                    id = "end-1",
                    sessionId = "sess-1",
                    endNumber = 1,
                    completedAt = Instant.parse("2026-05-19T08:05:00Z"),
                ),
            ),
            arrows = listOf(
                ArrowPlot(
                    id = "p1",
                    sessionId = "sess-1",
                    bowConfigId = "cfg-1",
                    arrowConfigId = "arrow-1",
                    ring = 11,
                    zone = Zone.CENTER,
                    plotX = 0.01,
                    plotY = -0.02,
                    endId = "end-1",
                    shotAt = Instant.parse("2026-05-19T08:01:00Z"),
                ),
            ),
        )
        val encoded = json.encodeToString(SharedSessionDetail.serializer(), detail)
        val decoded = json.decodeFromString(SharedSessionDetail.serializer(), encoded)
        assertThat(decoded).isEqualTo(detail)
    }

    @Test
    fun `SharedSessionDetail tolerates a deleted session with empty defaults`() {
        // The owner deleted the underlying session — session/ends/arrows absent.
        val raw = """
            {
              "sharedSession": {
                "id": "ss-2", "userId": "u-1", "sessionId": "sess-2",
                "score": 540, "xCount": 12, "arrowCount": 60,
                "shotAt": "2026-05-18T10:00:00Z", "createdAt": "2026-05-18T11:00:00Z"
              },
              "ownerHandle": "marcus.t",
              "ownerDisplayName": "Marcus T"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(SharedSessionDetail.serializer(), raw)
        assertThat(decoded.session).isNull()
        assertThat(decoded.ends).isEmpty()
        assertThat(decoded.arrows).isEmpty()
        assertThat(decoded.sharedSession.score).isEqualTo(540)
    }

    @Test
    fun `ShootingSession round-trips the targetLayout, and legacy rows default to SINGLE`() {
        val triangle = ShootingSession(
            id = "sess-1",
            bowId = "bow-1",
            bowConfigId = "cfg-1",
            arrowConfigId = "arrow-1",
            startedAt = Instant.parse("2026-05-19T08:00:00Z"),
            targetFaceType = TargetFaceType.TEN_RING,
            targetLayout = TargetLayout.TRIANGLE,
        )
        val encoded = json.encodeToString(ShootingSession.serializer(), triangle)
        val decoded = json.decodeFromString(ShootingSession.serializer(), encoded)
        assertThat(decoded.targetLayout).isEqualTo(TargetLayout.TRIANGLE)

        // A pre-field row (no targetLayout key) decodes as SINGLE.
        val legacy = """
            {
              "id": "sess-2", "bowId": "b1", "bowConfigId": "c1", "arrowConfigId": "a1",
              "startedAt": "2026-05-19T08:00:00Z"
            }
        """.trimIndent()
        val legacyDecoded = json.decodeFromString(ShootingSession.serializer(), legacy)
        assertThat(legacyDecoded.targetLayout).isEqualTo(TargetLayout.SINGLE)
    }

    // ── §18 trophy catalogue ──────────────────────────────────────────────────

    @Test
    fun `TrophyDef round-trips for all 12 known kinds`() {
        val allKinds = listOf(
            "score_pr", "x_pr", "flawless", "sharpshooter",
            "arrows_milestone", "sessions_milestone", "marathon",
            "streak", "weeks_active", "comeback",
            "first_distance", "distance_explorer",
        )
        allKinds.forEach { kind ->
            val def = TrophyDef(
                kind = kind,
                name = "Trophy $kind",
                description = "Earn this by doing $kind things.",
                tiers = listOf(1, 5, 10),
                category = TrophyCategory.skill,
            )
            val encoded = json.encodeToString(TrophyDef.serializer(), def)
            val decoded = json.decodeFromString(TrophyDef.serializer(), encoded)
            assertThat(decoded).isEqualTo(def)
        }
    }

    @Test
    fun `TrophyDef tolerates an unknown category and unknown kind`() {
        // A future server might add new categories or kinds — must not crash.
        val raw = """
            {
              "kind": "future_kind",
              "name": "Future Trophy",
              "description": "Not yet invented.",
              "tiers": [1, 3],
              "category": "skill"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(TrophyDef.serializer(), raw)
        assertThat(decoded.kind).isEqualTo("future_kind")
        assertThat(decoded.name).isEqualTo("Future Trophy")
        assertThat(decoded.category).isEqualTo(TrophyCategory.skill)
    }

    @Test
    fun `TrophyDef defaults tiers to empty and category to skill when absent`() {
        val raw = """{"kind": "score_pr", "name": "Score PR", "description": "Beat your score."}"""
        val decoded = json.decodeFromString(TrophyDef.serializer(), raw)
        assertThat(decoded.tiers).isEmpty()
        assertThat(decoded.category).isEqualTo(TrophyCategory.skill)
    }

    @Test
    fun `AchievementKind deserializes all 12 wire values`() {
        val wireValues = listOf(
            "score_pr" to AchievementKind.score_pr,
            "x_pr" to AchievementKind.x_pr,
            "flawless" to AchievementKind.flawless,
            "sharpshooter" to AchievementKind.sharpshooter,
            "arrows_milestone" to AchievementKind.arrows_milestone,
            "sessions_milestone" to AchievementKind.sessions_milestone,
            "marathon" to AchievementKind.marathon,
            "streak" to AchievementKind.streak,
            "weeks_active" to AchievementKind.weeks_active,
            "comeback" to AchievementKind.comeback,
            "first_distance" to AchievementKind.first_distance,
            "distance_explorer" to AchievementKind.distance_explorer,
        )
        wireValues.forEach { (wire, expected) ->
            val decoded = json.decodeFromString(AchievementKind.serializer(), "\"$wire\"")
            assertThat(decoded).isEqualTo(expected)
        }
    }

    @Test
    fun `Achievement round-trips for all 12 kinds`() {
        val kinds = AchievementKind.entries
        val now = Instant.parse("2026-05-20T10:00:00Z")
        kinds.forEach { kind ->
            val achievement = Achievement(
                id = "a-$kind",
                userId = "u-1",
                sharedSessionId = "ss-$kind",
                kind = kind,
                label = "Label $kind",
                value = 42,
                sublabel = "sublabel",
                createdAt = now,
            )
            val encoded = json.encodeToString(Achievement.serializer(), achievement)
            val decoded = json.decodeFromString(Achievement.serializer(), encoded)
            assertThat(decoded).isEqualTo(achievement)
            assertThat(decoded.kind).isEqualTo(kind)
        }
    }

    @Test
    fun `ActivityItem round-trips routing fields and tolerates an older payload`() {
        val routed = ActivityItem(
            id = "act-1",
            kind = ActivityKind.club_session,
            sourceKind = ActivitySourceKind.club,
            actorHandle = "marcus.t",
            actorDisplayName = "Marcus T",
            title = "Logged a club session",
            createdAt = Instant.parse("2026-05-19T08:00:00Z"),
            actorUserId = "u-9",
            clubId = "club-7",
        )
        val encoded = json.encodeToString(ActivityItem.serializer(), routed)
        val decoded = json.decodeFromString(ActivityItem.serializer(), encoded)
        assertThat(decoded.actorUserId).isEqualTo("u-9")
        assertThat(decoded.clubId).isEqualTo("club-7")
        assertThat(decoded.leagueId).isNull()

        // A pre-routing-fields payload still decodes — fields take their defaults.
        val legacy = """
            {
              "id": "act-2", "kind": "friend_setup", "sourceKind": "friend",
              "actorHandle": "jake.t", "actorDisplayName": "Jake T",
              "title": "Updated bow setup", "createdAt": "2026-05-19T08:00:00Z"
            }
        """.trimIndent()
        val legacyDecoded = json.decodeFromString(ActivityItem.serializer(), legacy)
        assertThat(legacyDecoded.actorUserId).isEqualTo("")
        assertThat(legacyDecoded.clubId).isNull()
        assertThat(legacyDecoded.leagueId).isNull()
    }
}
