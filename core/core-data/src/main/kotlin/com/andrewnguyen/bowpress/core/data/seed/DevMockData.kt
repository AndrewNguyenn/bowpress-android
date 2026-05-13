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
import com.andrewnguyen.bowpress.core.model.SessionEnd
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
    private val sessionEndDao: com.andrewnguyen.bowpress.core.database.dao.SessionEndDao,
    private val suggestionDao: SuggestionDao,
) {

    suspend fun seedIfEmpty() {
        if (bowDao.getAll().isNotEmpty()) return
        bowDao.upsertAll(DevMockData.bows.map { it.toEntity(pendingSync = false) })
        bowConfigDao.upsertAll(DevMockData.bowConfigs.map { it.toEntity(pendingSync = false) })
        arrowConfigDao.upsertAll(DevMockData.arrowConfigs.map { it.toEntity(pendingSync = false) })
        sessionDao.upsertAll(DevMockData.sessions.map { it.toEntity(pendingSync = false) })
        sessionEndDao.upsertAll(DevMockData.sessionEnds.map { it.toEntity(pendingSync = false) })
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
        // Fill out the historical trend to match iOS DevMockData
        // (14 sessions / 206 arrows). Earlier bc1b sessions sit further
        // from the centre so the "tightening over time" pattern reads
        // clearly across the Score timeline + ImpactMap views.
        // LAST WEEK bucket — one session at day 6 to match iOS's
        // "may 6 · 1 session" layout. iOS shows this row as "Evening
        // range" on Hoyt Satori (recurve), 70m / 14 arrows; mirror the
        // same title + bow context so the row reads identically.
        session(
            id = "dev_s1_3a", bowId = "dev_bow2", bowConfigId = "dev_bc2a",
            arrowConfigId = "dev_arrow2", startedAt = daysAgo(6),
            durationMin = 60, arrowCount = 14, distance = ShootingDistance.METERS_70,
            title = "Evening range",
            notes = "Late evening session, slightly fatigued but managed consistently.",
            feelTags = listOf("fatigue", "consistent"),
        ),
        session(
            id = "dev_s1_3b", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(16),
            durationMin = 85, arrowCount = 18, distance = ShootingDistance.METERS_70,
            title = "Long-range groups",
            notes = "70m groups holding through finals prep.",
            feelTags = listOf("back_tension", "consistent"),
        ),
        session(
            id = "dev_s1_3c", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(25),
            durationMin = 90, arrowCount = 18, distance = ShootingDistance.METERS_50,
            title = "Cadence work",
            notes = "Worked on shot cadence under timer.",
            feelTags = listOf("back_tension", "clean_release"),
        ),
        session(
            id = "dev_s1_3d", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(14),
            durationMin = 70, arrowCount = 15, distance = ShootingDistance.METERS_50,
            title = "Indoor finals tune-up",
            notes = "Indoor tune-up — mostly center misses.",
            feelTags = listOf("consistent"),
        ),
        session(
            id = "dev_s1_3e", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(20),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.YARDS_20,
            title = "Light practice",
            notes = "Light practice between competitions.",
            feelTags = listOf("fatigue"),
        ),
        session(
            id = "dev_s1_3f", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(26),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.YARDS_20,
            title = "Rest baseline",
            notes = "Pre-tune baseline at 20yd.",
            feelTags = listOf("consistent"),
        ),
        session(
            id = "dev_s1_3g", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(30),
            durationMin = 75, arrowCount = 12, distance = ShootingDistance.METERS_50,
            title = "Form review",
            notes = "Coach review session — focus on anchor.",
            feelTags = listOf("back_tension"),
        ),
        // Oldest April session — drop 2 arrows + push 1 day back so
        // total arrow count stays at 206 after s1_3a went 12→14, and
        // the APRIL range header reads "apr 7 - apr 30" like iOS.
        session(
            id = "dev_s1_3h", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(35),
            durationMin = 50, arrowCount = 8, distance = ShootingDistance.YARDS_20,
            title = "Short tune block",
            notes = "Short blocks while diagnosing nocking-point drift.",
            feelTags = listOf("fatigue"),
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
        // Backfill plots for the 8 historical sessions added above. Older
        // bc1b sessions favour cardinal-zone bias at rings 8-10; the bc1c
        // mid-range sessions stay tight at 10-11.
        addAll(makePlots("dev_s1_3a", "dev_bc2a", "dev_arrow2", daysAgo(6), 14,
            rings = listOf(10, 11, 10, 10, 11, 10, 11, 10, 10, 11, 10, 11, 10, 10),
            zones = listOf(Zone.CENTER, Zone.N, Zone.CENTER, Zone.NE, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER, Zone.NE, Zone.CENTER,
                Zone.N, Zone.CENTER),
        ))
        addAll(makePlots("dev_s1_3b", "dev_bc1c", "dev_arrow1", daysAgo(16), 18,
            rings = listOf(10, 11, 10, 11, 10, 10, 11, 11, 10, 11, 10, 10, 11, 10, 11, 10, 11, 10),
            zones = listOf(Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER,
                Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER, Zone.N, Zone.CENTER,
                Zone.CENTER, Zone.N, Zone.CENTER, Zone.N, Zone.CENTER, Zone.CENTER),
        ))
        addAll(makePlots("dev_s1_3c", "dev_bc1c", "dev_arrow1", daysAgo(25), 18,
            rings = listOf(10, 10, 11, 10, 11, 10, 10, 11, 10, 10, 11, 10, 10, 11, 10, 10, 11, 10),
            zones = listOf(Zone.CENTER, Zone.N, Zone.CENTER, Zone.NE, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.N, Zone.NE, Zone.CENTER, Zone.N,
                Zone.CENTER, Zone.CENTER, Zone.N, Zone.NE, Zone.CENTER, Zone.N),
        ))
        addAll(makePlots("dev_s1_3d", "dev_bc1b", "dev_arrow1", daysAgo(14), 15,
            rings = listOf(9, 10, 10, 9, 10, 9, 10, 10, 9, 10, 9, 10, 9, 10, 10),
            zones = listOf(Zone.N, Zone.CENTER, Zone.N, Zone.NE, Zone.N, Zone.CENTER,
                Zone.NE, Zone.N, Zone.CENTER, Zone.N, Zone.NE, Zone.CENTER,
                Zone.N, Zone.CENTER, Zone.N),
        ))
        addAll(makePlots("dev_s1_3e", "dev_bc1b", "dev_arrow1", daysAgo(20), 12,
            rings = listOf(8, 9, 9, 10, 9, 8, 9, 10, 9, 8, 9, 9),
            zones = listOf(Zone.NE, Zone.N, Zone.NE, Zone.CENTER, Zone.N, Zone.NE,
                Zone.N, Zone.CENTER, Zone.NE, Zone.N, Zone.NE, Zone.N),
        ))
        addAll(makePlots("dev_s1_3f", "dev_bc1b", "dev_arrow1", daysAgo(26), 12,
            rings = listOf(9, 8, 9, 9, 8, 9, 10, 8, 9, 9, 8, 9),
            zones = listOf(Zone.N, Zone.NE, Zone.N, Zone.NW, Zone.NE, Zone.N,
                Zone.CENTER, Zone.NE, Zone.N, Zone.NW, Zone.NE, Zone.N),
        ))
        addAll(makePlots("dev_s1_3g", "dev_bc1b", "dev_arrow1", daysAgo(30), 12,
            rings = listOf(8, 9, 8, 9, 8, 9, 9, 8, 9, 8, 9, 8),
            zones = listOf(Zone.NE, Zone.N, Zone.NE, Zone.N, Zone.NE, Zone.N,
                Zone.NE, Zone.N, Zone.NE, Zone.N, Zone.NE, Zone.N),
        ))
        addAll(makePlots("dev_s1_3h", "dev_bc1b", "dev_arrow1", daysAgo(35), 8,
            rings = listOf(8, 8, 9, 8, 9, 8, 8, 9),
            zones = listOf(Zone.NE, Zone.N, Zone.NE, Zone.N, Zone.NE, Zone.NW, Zone.N, Zone.NE),
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
        // Olympic-style: every 3 arrows = one end. Link via the seeded
        // SessionEnd IDs below so SessionDetail can show real end counts
        // and so the X·10 ring detail can group by end (future iter).
        val endIndex = i / 3
        val ring = rings[i % rings.size]
        val zone = zones[i % zones.size]
        val coords = syntheticPlotCoords(ring, zone, seed = i)
        ArrowPlot(
            id = "${sessionId}_p${i + 1}",
            sessionId = sessionId,
            bowConfigId = bowConfigId,
            arrowConfigId = arrowConfigId,
            ring = ring,
            zone = zone,
            plotX = coords.first,
            plotY = coords.second,
            shotAt = startedAt.plus((i * 4L), ChronoUnit.MINUTES),
            endId = "${sessionId}_e${endIndex + 1}",
            excluded = false,
        )
    }

    /**
     * Synthesise a plausible (plotX, plotY) for a seeded plot given its
     * scored ring + zone. The real client writes coords directly when the
     * user taps; the dev seed only carries ring+zone, so SessionDetail's
     * target would be empty without this. Coordinates follow the iOS
     * convention: east-positive X, south-positive Y, normalised to [-1, 1].
     *
     * The radius for each ring is chosen mid-band so the dot sits visibly
     * inside the scored ring. The angle is the centre bearing of the
     * compass zone (CENTER → near origin, with a tiny offset varied by the
     * arrow index so dots don't pile up perfectly).
     */
    private fun syntheticPlotCoords(ring: Int, zone: Zone, seed: Int): Pair<Double, Double> {
        // Ring radii — mid-band of each scoring ring's outer edge.
        // X is the centre dot; 10..1 step outward in 0.1 increments.
        val radius = when (ring) {
            11 -> 0.02   // X — extremely close to centre
            10 -> 0.08
            9 -> 0.15
            8 -> 0.25
            7 -> 0.35
            6 -> 0.45
            5 -> 0.55
            4 -> 0.65
            3 -> 0.75
            2 -> 0.85
            1 -> 0.92
            else -> 0.50
        }
        // Compass bearings → math angles (0° east, CCW). Plot uses
        // east-positive X and south-positive Y, so we flip the y-component.
        val angleDeg = when (zone) {
            Zone.E -> 0.0
            Zone.NE -> 45.0
            Zone.N -> 90.0
            Zone.NW -> 135.0
            Zone.W -> 180.0
            Zone.SW -> 225.0
            Zone.S -> 270.0
            Zone.SE -> 315.0
            Zone.CENTER -> (seed * 47.0) % 360.0
        }
        // Small jitter so consecutive dots don't overlap exactly.
        val jitter = 0.02 * ((seed % 5) - 2)
        val finalRadius = (radius + jitter).coerceIn(0.0, 0.97)
        val rad = Math.toRadians(angleDeg)
        val px = finalRadius * kotlin.math.cos(rad)
        val py = -finalRadius * kotlin.math.sin(rad) // flip y so south-positive matches storage
        return px to py
    }

    // --- Session ends -----------------------------------------------------
    // One end per 3 arrows for each seeded session — matches the Olympic
    // round structure iOS seeds. Without these, SessionDetail falls back
    // to "1 end" and analytics that group by end can't render.
    val sessionEnds: List<SessionEnd> = buildList {
        addAll(makeEnds("dev_s1_8", daysAgo(0), arrowCount = 18))
        addAll(makeEnds("dev_s1_7", daysAgo(1), arrowCount = 18))
        addAll(makeEnds("dev_s1_6", daysAgo(2), arrowCount = 18))
        addAll(makeEnds("dev_s1_5", daysAgo(18), arrowCount = 12))
        addAll(makeEnds("dev_s1_4", daysAgo(23), arrowCount = 16))
        addAll(makeEnds("dev_s2_5", daysAgo(12), arrowCount = 15))
        addAll(makeEnds("dev_s1_3a", daysAgo(6), arrowCount = 14))
        addAll(makeEnds("dev_s1_3b", daysAgo(16), arrowCount = 18))
        addAll(makeEnds("dev_s1_3c", daysAgo(25), arrowCount = 18))
        addAll(makeEnds("dev_s1_3d", daysAgo(14), arrowCount = 15))
        addAll(makeEnds("dev_s1_3e", daysAgo(20), arrowCount = 12))
        addAll(makeEnds("dev_s1_3f", daysAgo(26), arrowCount = 12))
        addAll(makeEnds("dev_s1_3g", daysAgo(30), arrowCount = 12))
        addAll(makeEnds("dev_s1_3h", daysAgo(35), arrowCount = 8))
    }

    private fun makeEnds(
        sessionId: String,
        startedAt: Instant,
        arrowCount: Int,
        arrowsPerEnd: Int = 3,
    ): List<SessionEnd> {
        val endCount = (arrowCount + arrowsPerEnd - 1) / arrowsPerEnd
        return (0 until endCount).map { idx ->
            // Each end completes after its three arrows + a brief pause.
            val completedAt = startedAt.plus(
                (idx * arrowsPerEnd * 4L + arrowsPerEnd * 4L).toLong(),
                ChronoUnit.MINUTES,
            )
            SessionEnd(
                id = "${sessionId}_e${idx + 1}",
                sessionId = sessionId,
                endNumber = idx + 1,
                completedAt = completedAt,
            )
        }
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
