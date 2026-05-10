package com.andrewnguyen.bowpress.core.data.seed

import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.Zone
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Android port of the iOS `DevMockData.swift` shape — used by DEBUG
 * builds to seed Room with fixture archery data so the Analytics dashboard
 * doesn't show the "Not enough data" empty state on a fresh emulator.
 *
 * Counterpart: iOS `Sources/BowPress/State/DevMockData.swift` (kept around as
 * the reference fixture shape). Field values here are simplified for tractable
 * coverage — fewer sessions than iOS, but enough to populate
 * `LocalAnalyticsEngine` outputs (overview, comparison, trends).
 *
 * The seeder writes via DAO converters with `pendingSync = false` so these
 * fixtures don't fight the background sync worker (the dev token is fake; any
 * upload would 401 in a loop).
 *
 * Gated by `BuildConfig.DEBUG` at the call site — see
 * `AppStateViewModel.init`. Idempotent: returns early if any bow already
 * exists in Room.
 */
@Singleton
class DevMockDataSeeder @Inject constructor(
    private val bowDao: BowDao,
    private val bowConfigDao: BowConfigDao,
    private val arrowConfigDao: ArrowConfigDao,
    private val sessionDao: SessionDao,
    private val plotDao: ArrowPlotDao,
    private val suggestionDao: SuggestionDao,
) {

    suspend fun seedIfEmpty() {
        if (bowDao.getAll().isNotEmpty()) return
        bowDao.upsertAll(DevMockData.bows.map { it.toEntity(pendingSync = false) })
        bowConfigDao.upsertAll(DevMockData.bowConfigs.map { it.toEntity(pendingSync = false) })
        arrowConfigDao.upsertAll(DevMockData.arrowConfigs.map { it.toEntity(pendingSync = false) })
        sessionDao.upsertAll(DevMockData.sessions.map { it.toEntity(pendingSync = false) })
        plotDao.upsertAll(DevMockData.arrowPlots.map { it.toEntity(pendingSync = false) })
        suggestionDao.upsertAll(DevMockData.suggestions.map { it.toEntity() })
    }
}

private object DevMockData {

    private val userId = "dev-user"
    private val now: Instant = Instant.now()
    private fun daysAgo(n: Long): Instant = now.minus(n, ChronoUnit.DAYS)
    private fun minutesAfter(at: Instant, n: Long): Instant = at.plus(n, ChronoUnit.MINUTES)

    // --- Bows --------------------------------------------------------------

    val bow1 = Bow(
        id = "dev_bow1",
        userId = userId,
        name = "Mathews TITLE 36",
        bowType = BowType.COMPOUND,
        brand = "Mathews",
        model = "TITLE 36",
        createdAt = daysAgo(45),
    )
    val bow2 = Bow(
        id = "dev_bow2",
        userId = userId,
        name = "Hoyt Satori",
        bowType = BowType.RECURVE,
        brand = "Hoyt",
        model = "Satori",
        createdAt = daysAgo(60),
    )
    // Third bow exists so the Equipment list shows all three bow types,
    // matching iOS DevMockData. No sessions attached — barebow chip in
    // Analytics will surface empty state (correct behavior).
    val bow3 = Bow(
        id = "dev_bow3",
        userId = userId,
        name = "Border HEX7 Barebow",
        bowType = BowType.BAREBOW,
        brand = "Border",
        model = "HEX7",
        createdAt = daysAgo(20),
    )
    val bows: List<Bow> = listOf(bow1, bow2, bow3)

    // --- Arrow configs -----------------------------------------------------

    val arrow1 = ArrowConfiguration(
        id = "dev_arrow1",
        userId = userId,
        label = "Competition X10s",
        brand = "Easton",
        model = "X10",
        length = 28.5,
        pointWeight = 110,
        fletchingType = FletchingType.VANE,
        fletchingLength = 2.0,
        fletchingOffset = 1.5,
        nockType = "pin",
        totalWeight = 420,
    )
    val arrow2 = ArrowConfiguration(
        id = "dev_arrow2",
        userId = userId,
        label = "Practice Platinums",
        brand = "Gold Tip",
        model = "Platinum",
        length = 29.0,
        pointWeight = 100,
        fletchingType = FletchingType.VANE,
        fletchingLength = 2.25,
        fletchingOffset = 2.0,
    )
    val arrowConfigs: List<ArrowConfiguration> = listOf(arrow1, arrow2)

    // --- Bow configurations -----------------------------------------------

    val bc1a = BowConfiguration(
        id = "dev_bc1a",
        bowId = "dev_bow1",
        createdAt = daysAgo(40),
        label = "Out of the Box",
        drawLength = 28.5,
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
        gripAngle = 45.0,
        nockingHeight = 0,
        frontStabWeight = 12.0,
        frontStabAngle = 0.0,
        rearStabSide = RearStabSide.NONE,
        rearStabWeight = 0.0,
        rearStabVertAngle = 0.0,
        rearStabHorizAngle = 0.0,
    )
    val bc1b = bc1a.copy(
        id = "dev_bc1b",
        createdAt = daysAgo(25),
        label = "Rest & Nocking Tune",
        peepHeight = 9.25,
        topCableTwists = 2,
        bottomCableTwists = 2,
        restVertical = 1,
        restHorizontal = -1,
        restDepth = 0.25,
        sightPosition = 1,
        nockingHeight = 2,
        frontStabAngle = 5.0,
        rearStabSide = RearStabSide.LEFT,
        rearStabWeight = 8.0,
        rearStabVertAngle = -45.0,
        rearStabHorizAngle = 45.0,
    )
    val bc1c = bc1b.copy(
        id = "dev_bc1c",
        createdAt = daysAgo(10),
        label = "Competition Setup",
        dLoopLength = 2.125,
        topCableTwists = 3,
        bottomCableTwists = 3,
        mainStringTopTwists = 2,
        mainStringBottomTwists = 1,
        topLimbTurns = -0.5,
        bottomLimbTurns = -0.5,
        restVertical = 2,
        sightPosition = 2,
        nockingHeight = 3,
        frontStabWeight = 14.0,
        rearStabWeight = 10.0,
    )
    val bc2a = BowConfiguration(
        id = "dev_bc2a",
        bowId = "dev_bow2",
        createdAt = daysAgo(55),
        label = "Initial Draw Set",
        drawLength = 29.0,
        restVertical = 0,
        restHorizontal = 0,
        restDepth = 0.0,
        gripAngle = 40.0,
        nockingHeight = 0,
        braceHeight = 8.75,
        tillerTop = 1.0,
        tillerBottom = 0.0,
        plungerTension = 8,
    )
    val bowConfigs: List<BowConfiguration> = listOf(bc1a, bc1b, bc1c, bc2a)

    // --- Sessions ---------------------------------------------------------
    //
    // Distribution: most-recent 3 sessions inside the 3-day analytics window
    // (so the default-period filter has data), older sessions span back 35
    // days for trend lines.

    val sessions: List<ShootingSession> = listOf(
        // bow1 (compound) — recent, configured bc1c
        session(
            id = "dev_s1_8", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(0),
            durationMin = 90, arrowCount = 18, distance = ShootingDistance.METERS_50,
            title = "Pre-comp tune check",
            notes = "Pre-comp tune check at 50m. Groups holding well.",
            feelTags = listOf("consistent", "clean_release"),
        ),
        session(
            id = "dev_s1_7", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(1),
            durationMin = 95, arrowCount = 18, distance = ShootingDistance.METERS_70,
            title = "Long-distance work",
            notes = "Long-distance work at 70m. Back tension fully engaged.",
            feelTags = listOf("back_tension", "clean_release", "consistent"),
        ),
        session(
            id = "dev_s1_6", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(2),
            durationMin = 90, arrowCount = 18, distance = ShootingDistance.YARDS_20,
            title = "Indoor practice",
            notes = "Indoor 20yd practice — clean groups.",
            feelTags = listOf("consistent", "clean_release"),
        ),
        session(
            id = "dev_s1_5", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(18),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.METERS_50,
            title = "Short session",
            notes = "Short session, fatigue late.",
            feelTags = listOf("fatigue", "consistent"),
        ),
        session(
            id = "dev_s1_4", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(23),
            durationMin = 80, arrowCount = 16, distance = ShootingDistance.YARDS_20,
            title = "Rest tune check",
            notes = "New rest position making a clear difference.",
            feelTags = listOf("clean_release", "consistent"),
        ),
        // bow2 (recurve) — for All-bows chip variety
        session(
            id = "dev_s2_5", bowId = "dev_bow2", bowConfigId = "dev_bc2a",
            arrowConfigId = "dev_arrow2", startedAt = daysAgo(12),
            durationMin = 95, arrowCount = 15, distance = ShootingDistance.METERS_70,
            title = "Strong session",
            notes = "Strong session. Release timing syncing.",
            feelTags = listOf("back_tension", "clean_release"),
        ),
    )

    private fun session(
        id: String,
        bowId: String,
        bowConfigId: String,
        arrowConfigId: String,
        startedAt: Instant,
        durationMin: Long,
        arrowCount: Int,
        distance: ShootingDistance,
        title: String,
        notes: String,
        feelTags: List<String>,
    ): ShootingSession = ShootingSession(
        id = id,
        bowId = bowId,
        bowConfigId = bowConfigId,
        arrowConfigId = arrowConfigId,
        startedAt = startedAt,
        endedAt = minutesAfter(startedAt, durationMin),
        notes = notes,
        feelTags = feelTags,
        arrowCount = arrowCount,
        targetFaceType = TargetFaceType.TEN_RING,
        distance = distance,
        title = title,
    )

    // --- Arrow plots ------------------------------------------------------
    //
    // Distribution shifts over time: oldest sessions (bc1b) at ring 9-10
    // with cardinal-zone bias; recent sessions (bc1c) at ring 10-11 mostly
    // CENTER. Mirrors iOS DevMockData's "tightening over time" pattern so
    // the score-timeline + comparison views show genuine improvement.

    val arrowPlots: List<ArrowPlot> = buildList {
        addAll(makePlots("dev_s1_8", "dev_bc1c", "dev_arrow1", daysAgo(0), 18,
            rings = listOf(10, 11, 11, 10, 11, 10, 11, 11, 10, 11, 10, 11, 11, 10, 11, 10, 11, 11),
            zones = listOf(Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.CENTER,
                Zone.N, Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER),
        ))
        addAll(makePlots("dev_s1_7", "dev_bc1c", "dev_arrow1", daysAgo(1), 18,
            rings = listOf(11, 11, 10, 11, 11, 10, 11, 11, 11, 10, 11, 11, 10, 11, 11, 11, 10, 11),
            zones = listOf(Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.CENTER,
                Zone.N, Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER),
        ))
        addAll(makePlots("dev_s1_6", "dev_bc1c", "dev_arrow1", daysAgo(2), 18,
            rings = listOf(10, 11, 10, 11, 10, 11, 10, 10, 11, 10, 11, 10, 11, 10, 10, 11, 10, 11),
            zones = listOf(Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER,
                Zone.NE, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER,
                Zone.N, Zone.CENTER, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER),
        ))
        addAll(makePlots("dev_s1_5", "dev_bc1b", "dev_arrow1", daysAgo(18), 12,
            rings = listOf(9, 10, 10, 9, 10, 9, 10, 10, 9, 10, 9, 10),
            zones = listOf(Zone.NE, Zone.N, Zone.CENTER, Zone.NE, Zone.CENTER, Zone.N,
                Zone.NE, Zone.CENTER, Zone.N, Zone.CENTER, Zone.NE, Zone.N),
        ))
        addAll(makePlots("dev_s1_4", "dev_bc1b", "dev_arrow1", daysAgo(23), 16,
            rings = listOf(9, 10, 9, 10, 10, 9, 10, 9, 10, 10, 9, 10, 9, 10, 9, 10),
            zones = listOf(Zone.N, Zone.NE, Zone.N, Zone.CENTER, Zone.N, Zone.NE,
                Zone.N, Zone.CENTER, Zone.NE, Zone.N, Zone.NE, Zone.CENTER,
                Zone.N, Zone.NE, Zone.N, Zone.CENTER),
        ))
        addAll(makePlots("dev_s2_5", "dev_bc2a", "dev_arrow2", daysAgo(12), 15,
            rings = listOf(10, 11, 10, 11, 10, 11, 10, 11, 10, 11, 10, 11, 10, 11, 10),
            zones = listOf(Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER,
                Zone.NE, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.N),
        ))
    }

    private fun makePlots(
        sessionId: String,
        bowConfigId: String,
        arrowConfigId: String,
        startedAt: Instant,
        count: Int,
        rings: List<Int>,
        zones: List<Zone>,
    ): List<ArrowPlot> = (0 until count).map { i ->
        ArrowPlot(
            id = "${sessionId}_p${i + 1}",
            sessionId = sessionId,
            bowConfigId = bowConfigId,
            arrowConfigId = arrowConfigId,
            ring = rings[i % rings.size],
            zone = zones[i % zones.size],
            shotAt = startedAt.plus((i * 4L), ChronoUnit.MINUTES),
            excluded = false,
        )
    }

    // --- Suggestions ------------------------------------------------------

    val suggestions: List<AnalyticsSuggestion> = listOf(
        AnalyticsSuggestion(
            id = "dev_sug1a",
            bowId = "dev_bow1",
            createdAt = daysAgo(2),
            parameter = "restVertical",
            suggestedValue = "+3/16\"",
            currentValue = "+2/16\"",
            reasoning = "Arrow flight shows mild porpoising pattern across last 3 sessions. Raising rest 1/16\" may tighten vertical grouping.",
            confidence = 0.82,
            wasRead = false,
            deliveryType = DeliveryType.PUSH,
        ),
        AnalyticsSuggestion(
            id = "dev_sug1b",
            bowId = "dev_bow1",
            createdAt = daysAgo(3),
            parameter = "peepHeight",
            suggestedValue = "9.5\"",
            currentValue = "9.25\"",
            reasoning = "Anchor inconsistency detected in 4 of last 12 arrows.",
            confidence = 0.71,
            wasRead = false,
            deliveryType = DeliveryType.IN_APP,
        ),
        AnalyticsSuggestion(
            id = "dev_sug2a",
            bowId = "dev_bow2",
            createdAt = daysAgo(4),
            parameter = "mainStringTopTwists",
            suggestedValue = "4 twists",
            currentValue = "3 twists",
            reasoning = "String walking detected — top string twists slightly looser than bottom.",
            confidence = 0.74,
            wasRead = false,
            deliveryType = DeliveryType.PUSH,
        ),
    )
}
