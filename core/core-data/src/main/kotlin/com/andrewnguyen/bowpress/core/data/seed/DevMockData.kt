package com.andrewnguyen.bowpress.core.data.seed

import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementBadge
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.ArrowConfiguration
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.FletchingType
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.HandicapConfig
import com.andrewnguyen.bowpress.core.model.HandicapEquation
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.core.model.LeagueEntry
import com.andrewnguyen.bowpress.core.model.LeagueEntryRule
import com.andrewnguyen.bowpress.core.model.LeagueSchedule
import com.andrewnguyen.bowpress.core.model.LeagueScheduleKind
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.LeagueType
import com.andrewnguyen.bowpress.core.model.RearStabSide
import com.andrewnguyen.bowpress.core.model.RoundDef
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.TrophyCategory
import com.andrewnguyen.bowpress.core.model.TrophyDef
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
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
    // Social DAOs
    private val socialProfileDao: SocialProfileDao,
    private val friendshipDao: FriendshipDao,
    private val clubDao: ClubDao,
    private val leagueDao: LeagueDao,
    private val activityFeedDao: ActivityFeedDao,
    private val invitationDao: InvitationDao,
    private val blockDao: BlockDao,
    private val achievementDao: AchievementDao,
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

        // Social graph (§10 contract fixture).
        // When the launch property `socialEmpty` is set the social collections
        // are seeded as empty so the new-user / quiet-week empty states are
        // previewable on a DEBUG build without clearing Room manually:
        //
        //   adb shell am start -n com.andrewnguyen.bowpress.debug/... \
        //       --es socialEmpty true
        //
        // or via Gradle instrumented test:
        //
        //   System.setProperty("socialEmpty", "1")
        //
        // The toggle is DEBUG-only and has no effect on release builds.
        val socialEmpty = DevMockData.isSocialEmptyMode()
        socialProfileDao.upsertAll(DevMockData.socialProfiles.map { it.toEntity() })
        friendshipDao.upsertAll(
            if (socialEmpty) emptyList() else DevMockData.friendships.map { it.toEntity() },
        )
        clubDao.upsertAll(
            if (socialEmpty) emptyList() else DevMockData.clubs.map { it.toEntity() },
        )
        leagueDao.upsertAll(
            if (socialEmpty) emptyList() else DevMockData.leagues.map { it.toEntity() },
        )
        activityFeedDao.upsertAll(
            if (socialEmpty) emptyList() else DevMockData.activityFeed.map { it.toEntity() },
        )
        // Pending club + league invitations (§11) — drives the Social tab badge
        invitationDao.upsertAll(
            if (socialEmpty) emptyList() else DevMockData.invitations.map { it.toEntity() },
        )
        // Mutes / blocks (§14) — one muted archer + one muted club
        blockDao.upsertAll(DevMockData.blocks.map { it.toEntity() })
        // Achievements (§15) — the dev user's trophy case + a friend's
        achievementDao.upsertAll(DevMockData.achievements.map { it.toEntity() })
    }
}

/**
 * The DEBUG fixture object. `internal` (not `private`) so DEBUG-only
 * repository fallbacks in the same module can serve the §17 club-board /
 * league-attachment fixtures that have no Room cache.
 */
internal object DevMockData {

    /**
     * Returns `true` when the `socialEmpty` JVM system property is set,
     * making [DevMockDataSeeder] seed an empty social graph so the new-user
     * and quiet-week empty states are previewable on DEBUG builds.
     *
     * Set via ADB intent extra before launch:
     * ```
     * adb shell am start -n com.andrewnguyen.bowpress.debug/<activity> \
     *     --es socialEmpty true
     * ```
     * Or in a unit/instrumented test:
     * ```
     * System.setProperty("socialEmpty", "1")
     * ```
     *
     * Always returns `false` on release builds (the seeder is never called
     * outside `BuildConfig.DEBUG` at the [AppStateViewModel] call site).
     */
    fun isSocialEmptyMode(): Boolean =
        System.getProperty("socialEmpty") != null


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
        sightPinDistance = 6.5,
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
        sightPinDistance = 6.75,
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
            notes = "Pre-comp tune check at 50m. Groups holding well. Minor sight drift to correct.",
            feelTags = listOf("consistent", "clean_release"),
            // §16 — this session backs the mocked friend session detail; a
            // Vegas triangle layout makes the 3-spot render visible in DEBUG.
            targetLayout = TargetLayout.TRIANGLE,
        ),
        session(
            id = "dev_s1_7", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(1),
            durationMin = 95, arrowCount = 18, distance = ShootingDistance.METERS_70,
            title = "Long-distance work",
            notes = "Long-distance work at 70m. Back tension fully engaged, impact pattern very tight.",
            feelTags = listOf("back_tension", "clean_release", "consistent"),
        ),
        session(
            id = "dev_s1_6", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(2),
            durationMin = 90, arrowCount = 18, distance = ShootingDistance.YARDS_20,
            title = "Indoor practice",
            notes = "Indoor 20yd practice — clean groups, tested new D-loop length.",
            feelTags = listOf("consistent", "clean_release"),
        ),
        session(
            id = "dev_s1_5", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(18),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.METERS_50,
            title = "Short session",
            notes = "Short session due to fatigue. Still, groups are noticeably tighter than first week.",
            feelTags = listOf("fatigue", "consistent"),
        ),
        session(
            id = "dev_s1_4", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(23),
            durationMin = 80, arrowCount = 16, distance = ShootingDistance.YARDS_20,
            title = "Rest tune check",
            notes = "New rest position making a clear difference. Cleaner breaks off the wall.",
            feelTags = listOf("clean_release", "consistent"),
        ),
        // bow2 (recurve) — for All-bows chip variety
        session(
            id = "dev_s2_5", bowId = "dev_bow2", bowConfigId = "dev_bc2a",
            arrowConfigId = "dev_arrow2", startedAt = daysAgo(12),
            durationMin = 95, arrowCount = 15, distance = ShootingDistance.METERS_70,
            title = "Strong session",
            notes = "Strong session. Back tension and release timing syncing up well.",
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
            notes = "Late evening session, slightly fatigued but managed consistent groups.",
            feelTags = listOf("fatigue", "consistent"),
        ),
        session(
            id = "dev_s1_3b", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(16),
            durationMin = 85, arrowCount = 18, distance = ShootingDistance.METERS_70,
            title = "Tune applied",
            notes = "Full tune applied. Huge difference in arrow flight stability off the rest.",
            feelTags = listOf("back_tension", "consistent"),
        ),
        session(
            id = "dev_s1_3c", bowId = "dev_bow1", bowConfigId = "dev_bc1c",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(25),
            durationMin = 90, arrowCount = 18, distance = ShootingDistance.METERS_50,
            title = "Hoyt opener",
            notes = "Initial setup session. Getting used to the longer draw on the Hoyt.",
            feelTags = listOf("back_tension", "clean_release"),
        ),
        session(
            id = "dev_s1_3d", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(14),
            durationMin = 70, arrowCount = 15, distance = ShootingDistance.METERS_50,
            title = "Windage chase",
            notes = "Focused on windage. Groups printing slightly left — adjustments logged.",
            feelTags = listOf("consistent"),
        ),
        session(
            id = "dev_s1_3e", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(20),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.YARDS_20,
            title = "Finding the draw",
            notes = "More comfortable with the draw cycle. Groups still wide but more intentional.",
            feelTags = listOf("fatigue"),
        ),
        session(
            id = "dev_s1_3f", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(26),
            durationMin = 60, arrowCount = 12, distance = ShootingDistance.YARDS_20,
            title = "Windy afternoon",
            notes = "Groups tightening slightly. Wind made it hard to judge impact consistently.",
            feelTags = listOf("consistent"),
        ),
        session(
            id = "dev_s1_3g", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(30),
            durationMin = 75, arrowCount = 12, distance = ShootingDistance.METERS_50,
            title = "Back tension drill",
            notes = "Worked on back tension. Some improvement but still inconsistent peep alignment.",
            feelTags = listOf("back_tension"),
        ),
        // Oldest April session — drop 2 arrows + push 1 day back so
        // total arrow count stays at 206 after s1_3a went 12→14, and
        // the APRIL range header reads "apr 7 - apr 30" like iOS.
        session(
            id = "dev_s1_3h", bowId = "dev_bow1", bowConfigId = "dev_bc1b",
            arrowConfigId = "dev_arrow1", startedAt = daysAgo(35),
            durationMin = 50, arrowCount = 8, distance = ShootingDistance.YARDS_20,
            title = "First range",
            notes = "First range session with the new bow. Still finding anchor, groups were wide.",
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
        targetLayout: TargetLayout = TargetLayout.SINGLE,
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
        targetLayout = targetLayout,
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

    // ── Social graph (contract §10) ────────────────────────────────────────
    //
    // Dev fixture: handle "andrew.n", 14 mutual friends (12 accepted + 2
    // pending incoming), 2 clubs, 1 active weekly league, activity feed.

    private val devUserId = "dev-user"
    private val devHandle = "andrew.n"

    val socialProfiles: List<SocialProfile> = buildList {
        // Own profile
        add(SocialProfile(
            userId = devUserId,
            handle = devHandle,
            displayName = "Andrew Nguyen",
            joinedAt = daysAgo(45),
            visibility = SocialVisibility.friends,
            bowSummary = "Mathews TITLE 36 · CMP",
            sessionCount = 14,
            arrowCount = 206,
            division = Division.CMP,
        ))
        // Friend profiles (accepted)
        listOf(
            Triple("u_001", "sara.l", "Sara Lin"),
            Triple("u_002", "m.okonkwo", "Marcus Okonkwo"),
            Triple("u_003", "priya.v", "Priya Varma"),
            Triple("u_004", "jake.t", "Jake Torres"),
            Triple("u_005", "emi.f", "Emi Fujita"),
            Triple("u_006", "dev.ch", "Devon Chen"),
            Triple("u_007", "nadia.b", "Nadia Bouchard"),
            Triple("u_008", "luca.m", "Luca Moretti"),
            Triple("u_009", "kim.y", "Kim Yoon"),
            Triple("u_010", "alex.r", "Alex Rivera"),
            Triple("u_011", "zoe.h", "Zoe Hartley"),
            Triple("u_012", "ryan.k", "Ryan Kim"),
            // Pending inbound
            Triple("u_013", "theo.w", "Theo Walsh"),
            Triple("u_014", "bianca.c", "Bianca Caruso"),
        ).forEach { (id, handle, name) ->
            add(SocialProfile(
                userId = id,
                handle = handle,
                displayName = name,
                joinedAt = daysAgo((20..90).random().toLong()),
                visibility = SocialVisibility.friends,
                division = Division.entries.random(),
            ))
        }
    }

    val friendships: List<Friendship> = buildList {
        // 12 accepted mutual friendships
        listOf("u_001", "u_002", "u_003", "u_004", "u_005", "u_006",
               "u_007", "u_008", "u_009", "u_010", "u_011", "u_012").forEachIndexed { i, friendId ->
            add(Friendship(
                id = "fr_$friendId",
                requesterId = devUserId,
                addresseeId = friendId,
                status = FriendshipStatus.accepted,
                source = FriendshipSource.handle,
                createdAt = daysAgo((10 + i * 3).toLong()),
                respondedAt = daysAgo((9 + i * 3).toLong()),
                otherUserId = friendId,
                otherHandle = socialProfiles.first { it.userId == friendId }.handle,
                otherDisplayName = socialProfiles.first { it.userId == friendId }.displayName,
                direction = null,
            ))
        }
        // 2 pending incoming (they sent to us)
        listOf("u_013", "u_014").forEach { friendId ->
            val profile = socialProfiles.first { it.userId == friendId }
            add(Friendship(
                id = "fr_$friendId",
                requesterId = friendId,
                addresseeId = devUserId,
                status = FriendshipStatus.pending,
                source = FriendshipSource.handle,
                createdAt = daysAgo(1),
                otherUserId = friendId,
                otherHandle = profile.handle,
                otherDisplayName = profile.displayName,
                direction = FriendshipDirection.incoming,
            ))
        }
    }

    val clubs: List<Club> = listOf(
        Club(
            id = "club_001",
            name = "Riverside Archers",
            description = "Weekend range crew at Riverside",
            notes = "Gate code 4491 · Shooting hours 8am–5pm Sat/Sun",
            inviteCode = "RVSIDE01",
            createdAt = daysAgo(60),
            createdBy = devUserId,
            memberCount = 9,
            myRole = ClubRole.host,
        ),
        Club(
            id = "club_002",
            name = "Metro Indoor League",
            description = "City indoor range regular shooters",
            inviteCode = "METRO001",
            createdAt = daysAgo(30),
            createdBy = "u_001",
            memberCount = 14,
            myRole = ClubRole.member,
        ),
    )

    val leagues: List<League> = listOf(
        League(
            id = "lg_001",
            name = "Spring Compound Weekly",
            hostClubId = "club_001",
            hostUserId = devUserId,
            type = LeagueType.individual,
            divisions = listOf(Division.CMP, Division.REC),
            round = RoundDef(endCount = 10, arrowsPerEnd = 6),
            schedule = LeagueSchedule(
                kind = LeagueScheduleKind.weekly,
                startsAt = daysAgo(28),
                endsAt = daysAgo(-28),
                totalWeeks = 8,
                weekDeadlineDow = 0, // Sunday
            ),
            handicap = HandicapConfig(
                equation = HandicapEquation.bracket,
                allowancePct = null,
                setupWeeks = 2,
            ),
            entryRule = LeagueEntryRule.open,
            inviteCode = "SPRWK001",
            status = LeagueStatus.active,
            createdAt = daysAgo(30),
            entryCount = 8,
            myEntry = LeagueEntry(
                userId = devUserId,
                handle = devHandle,
                displayName = "Andrew Nguyen",
                division = Division.CMP,
                bestScore = 548,
                bestX = 12,
                joinedAt = daysAgo(28),
            ),
        ),
    )

    val activityFeed: List<ActivityItem> = listOf(
        // §15 — a highlighted shared-session row with achievement badges.
        ActivityItem(
            id = "act_001",
            kind = ActivityKind.friend_pr,
            sourceKind = ActivitySourceKind.friend,
            actorHandle = "sara.l",
            actorDisplayName = "Sara Lin",
            title = "Shared a session — new personal best",
            meta = "50m · CMP",
            stamp = "PR",
            createdAt = daysAgo(0).minus(2, ChronoUnit.HOURS),
            session = ActivitySession(
                sharedSessionId = "ss_sara_1",
                // §16 — points at a seeded session (dev_s1_8: 18 plots + ends)
                // so the friend session detail renders a real scorecard +
                // plotted target in DEBUG.
                sessionId = "dev_s1_8",
                score = 196,
                xCount = 9,
                arrowCount = 18,
                distance = "50m",
                face = "10-Ring",
            ),
            achievements = listOf(
                AchievementBadge(
                    kind = AchievementKind.score_pr,
                    label = "Score PR · 196",
                    value = 196,
                    sublabel = "50m · 10-Ring",
                ),
            ),
            highlighted = true,
        ),
        ActivityItem(
            id = "act_002",
            kind = ActivityKind.club_session,
            sourceKind = ActivitySourceKind.club,
            actorHandle = "m.okonkwo",
            actorDisplayName = "Marcus Okonkwo",
            title = "Logged a club session at Riverside Archers",
            meta = "70m · 18 arrows",
            stamp = "Yesterday",
            createdAt = daysAgo(1),
        ),
        ActivityItem(
            id = "act_003",
            kind = ActivityKind.league_event,
            sourceKind = ActivitySourceKind.league,
            actorHandle = "priya.v",
            actorDisplayName = "Priya Varma",
            title = "Submitted W4 score · 531",
            meta = "Spring Compound Weekly",
            stamp = "2d ago",
            createdAt = daysAgo(2),
        ),
        ActivityItem(
            id = "act_004",
            kind = ActivityKind.friend_setup,
            sourceKind = ActivitySourceKind.friend,
            actorHandle = "jake.t",
            actorDisplayName = "Jake Torres",
            title = "Updated bow setup — Hoyt Satori",
            stamp = "3d ago",
            createdAt = daysAgo(3),
        ),
        ActivityItem(
            id = "act_005",
            kind = ActivityKind.club_member_joined,
            sourceKind = ActivitySourceKind.club,
            actorHandle = "emi.f",
            actorDisplayName = "Emi Fujita",
            title = "Joined Metro Indoor League",
            stamp = "5d ago",
            createdAt = daysAgo(5),
        ),
        // §15 — a highlighted multi-achievement shared session.
        ActivityItem(
            id = "act_006",
            kind = ActivityKind.friend_pr,
            sourceKind = ActivitySourceKind.friend,
            actorHandle = "dev.ch",
            actorDisplayName = "Devon Chen",
            title = "Shared a session — milestone reached",
            meta = "20yd · Indoor",
            stamp = "PR",
            createdAt = daysAgo(7),
            session = ActivitySession(
                sharedSessionId = "ss_devon_1",
                sessionId = "sess_devon_1",
                score = 561,
                xCount = 19,
                arrowCount = 60,
                distance = "20yd",
                face = "6-Ring",
            ),
            achievements = listOf(
                AchievementBadge(
                    kind = AchievementKind.score_pr,
                    label = "Score PR · 561",
                    value = 561,
                    sublabel = "20yd · 6-Ring",
                ),
                AchievementBadge(
                    kind = AchievementKind.arrows_milestone,
                    label = "5,000 arrows",
                    value = 5000,
                    sublabel = "cumulative",
                ),
            ),
            highlighted = true,
        ),
        // §15 — a non-PR shared session (plain stat line, not highlighted).
        ActivityItem(
            id = "act_007",
            kind = ActivityKind.friend_session,
            sourceKind = ActivitySourceKind.friend,
            actorHandle = "luca.m",
            actorDisplayName = "Luca Moretti",
            title = "Shared a session",
            meta = "70m · REC",
            stamp = "4d ago",
            createdAt = daysAgo(4),
            session = ActivitySession(
                sharedSessionId = "ss_luca_1",
                sessionId = "sess_luca_1",
                score = 512,
                xCount = 8,
                arrowCount = 36,
                distance = "70m",
                face = "10-Ring",
            ),
        ),
    )

    // --- Invitations (§11) ------------------------------------------------
    //
    // Two pending invitations addressed to the dev user — one club, one
    // league. Combined with the 2 incoming friend requests above, the Social
    // tab badge shows a non-zero count in DEBUG.

    val invitations: List<SocialInvitation> = listOf(
        SocialInvitation(
            id = "inv_001",
            kind = InvitationKind.club,
            targetId = "club_invited_001",
            targetName = "Saturday Morning Recurve",
            inviterUserId = "u_003",
            inviterHandle = "priya.v",
            inviteeUserId = devUserId,
            status = InvitationStatus.pending,
            createdAt = daysAgo(1),
        ),
        SocialInvitation(
            id = "inv_002",
            kind = InvitationKind.league,
            targetId = "lg_invited_001",
            targetName = "Weekly Indoor 600",
            inviterUserId = "u_002",
            inviterHandle = "m.okonkwo",
            inviteeUserId = devUserId,
            status = InvitationStatus.pending,
            createdAt = daysAgo(2),
        ),
    )

    // --- Mutes / blocks (§14) ---------------------------------------------
    //
    // One muted archer (still a friend — u_012 Ryan Kim) and one muted club
    // (Metro Indoor League). Both are soft mutes, so they stay in the friends
    // list / club list but their activity is dropped from the feed.

    val blocks: List<SocialBlock> = listOf(
        SocialBlock(
            id = "blk_001",
            userId = devUserId,
            kind = BlockKind.archer,
            targetId = "u_012",
            targetName = "ryan.k",
            mode = BlockMode.mute,
            createdAt = daysAgo(4),
        ),
        SocialBlock(
            id = "blk_002",
            userId = devUserId,
            kind = BlockKind.club,
            targetId = "club_002",
            targetName = "Metro Indoor League",
            mode = BlockMode.mute,
            createdAt = daysAgo(6),
        ),
    )

    // --- Achievements (§15 / §18) ----------------------------------------
    //
    // The dev user has a believable earned spread across several categories
    // (skill, milestone, streak, 3D course, competition, community) so the
    // earned-only trophy case showcases each group.
    // Sara Lin (u_001) has 2 earned so the friend-profile case is non-empty.

    val achievements: List<Achievement> = listOf(
        // ── Skill ──
        Achievement(
            id = "ach_001",
            userId = devUserId,
            sharedSessionId = "ss_dev_1",
            kind = AchievementKind.score_pr,
            label = "Score PR · 558",
            value = 558,
            sublabel = "50m · 10-Ring",
            createdAt = daysAgo(2),
        ),
        Achievement(
            id = "ach_005",
            userId = devUserId,
            sharedSessionId = "ss_dev_2",
            kind = AchievementKind.x_pr,
            label = "X PR · 14",
            value = 14,
            sublabel = "50m · 10-Ring",
            createdAt = daysAgo(7),
        ),
        Achievement(
            id = "ach_006",
            userId = devUserId,
            sharedSessionId = "ss_dev_3",
            kind = AchievementKind.flawless,
            label = "Flawless 300",
            value = 300,
            sublabel = "20yd · 6-Ring — every arrow scored",
            createdAt = daysAgo(14),
        ),
        // ── Milestone ──
        Achievement(
            id = "ach_003",
            userId = devUserId,
            sharedSessionId = "ss_dev_old",
            kind = AchievementKind.arrows_milestone,
            label = "2,500 arrows",
            value = 2500,
            sublabel = "cumulative across shared sessions",
            createdAt = daysAgo(20),
        ),
        Achievement(
            id = "ach_007",
            userId = devUserId,
            sharedSessionId = "ss_dev_old2",
            kind = AchievementKind.sessions_milestone,
            label = "10 sessions",
            value = 10,
            sublabel = "milestone",
            createdAt = daysAgo(25),
        ),
        // ── Streak ──
        Achievement(
            id = "ach_002",
            userId = devUserId,
            sharedSessionId = "ss_dev_1",
            kind = AchievementKind.streak,
            label = "7-day streak",
            value = 7,
            sublabel = "a session every day this week",
            createdAt = daysAgo(2),
        ),
        // ── Exploration ──
        Achievement(
            id = "ach_008",
            userId = devUserId,
            sharedSessionId = "ss_dev_4",
            kind = AchievementKind.first_distance,
            label = "First at 70m",
            value = 70,
            sublabel = "first session at this distance",
            createdAt = daysAgo(30),
        ),
        // ── Friend: Sara Lin (u_001) — 2 earned ──
        Achievement(
            id = "ach_004",
            userId = "u_001",
            sharedSessionId = "ss_sara_1",
            kind = AchievementKind.score_pr,
            label = "Score PR · 574",
            value = 574,
            sublabel = "50m · 10-Ring",
            createdAt = daysAgo(0).minus(2, ChronoUnit.HOURS),
        ),
        Achievement(
            id = "ach_009",
            userId = "u_001",
            sharedSessionId = "ss_sara_2",
            kind = AchievementKind.streak,
            label = "14-day streak",
            value = 14,
            sublabel = "two weeks without a day off",
            createdAt = daysAgo(5),
        ),
        // ── Course (3D) ──
        Achievement(
            id = "ach_010",
            userId = devUserId,
            sharedSessionId = "ss_dev_4",
            kind = AchievementKind.course_first,
            label = "Into the Woods",
            value = 0,
            sublabel = "first 3D course",
            createdAt = daysAgo(12),
        ),
        // ── Competition / Community — not earned from a shared session ──
        Achievement(
            id = "ach_011",
            userId = devUserId,
            sharedSessionId = null,
            kind = AchievementKind.league_champion,
            label = "League Champion",
            value = 1,
            sublabel = "Won a league outright",
            createdAt = daysAgo(3),
        ),
        Achievement(
            id = "ach_012",
            userId = devUserId,
            sharedSessionId = null,
            kind = AchievementKind.club_founder,
            label = "Club Founder",
            value = 0,
            sublabel = "Founded a club",
            createdAt = daysAgo(48),
        ),
    )

    // --- Trophy catalogue (§18) ------------------------------------------
    //
    // Full 24-entry catalogue mirroring `GET /social/trophies`. Used as the
    // DEBUG fallback when the API is unreachable so TrophyCaseSection can map
    // an earned achievement's kind to its category on a fresh emulator.

    val trophyCatalog: List<TrophyDef> = listOf(
        // ── Skill ──
        TrophyDef(
            kind = "score_pr",
            name = "Score PR",
            description = "Set a new personal-best score at any distance.",
            tiers = listOf(1, 5, 10),
            category = TrophyCategory.skill,
        ),
        TrophyDef(
            kind = "x_pr",
            name = "X PR",
            description = "Set a new personal-best X count in a single session.",
            tiers = listOf(1, 5, 10),
            category = TrophyCategory.skill,
        ),
        TrophyDef(
            kind = "flawless",
            name = "Flawless",
            description = "Complete a full session without a miss — every arrow scores.",
            tiers = listOf(1, 3, 5),
            category = TrophyCategory.skill,
        ),
        TrophyDef(
            kind = "sharpshooter",
            name = "Sharpshooter",
            description = "Hit the X-ring on 80 % or more of your arrows in a session of 18+.",
            tiers = listOf(1, 3, 10),
            category = TrophyCategory.skill,
        ),
        // ── Milestone ──
        TrophyDef(
            kind = "arrows_milestone",
            name = "Arrow Milestone",
            description = "Reach a cumulative arrow count across all shared sessions.",
            tiers = listOf(500, 1000, 2500, 5000, 10000),
            category = TrophyCategory.milestone,
        ),
        TrophyDef(
            kind = "sessions_milestone",
            name = "Session Milestone",
            description = "Log a total number of sessions.",
            tiers = listOf(10, 25, 50, 100),
            category = TrophyCategory.milestone,
        ),
        TrophyDef(
            kind = "marathon",
            name = "Marathon",
            description = "Shoot 100 or more arrows in a single session.",
            tiers = listOf(1, 5, 20),
            category = TrophyCategory.milestone,
        ),
        // ── Streak ──
        TrophyDef(
            kind = "streak",
            name = "Streak",
            description = "Log a session every day for a consecutive number of days.",
            tiers = listOf(3, 7, 14, 30),
            category = TrophyCategory.streak,
        ),
        TrophyDef(
            kind = "weeks_active",
            name = "Weeks Active",
            description = "Log at least one session per week for consecutive weeks.",
            tiers = listOf(4, 8, 12, 26),
            category = TrophyCategory.streak,
        ),
        TrophyDef(
            kind = "comeback",
            name = "Comeback",
            description = "Return to the range after a break of 14 or more days.",
            tiers = listOf(1, 3, 5),
            category = TrophyCategory.streak,
        ),
        // ── Exploration ──
        TrophyDef(
            kind = "first_distance",
            name = "First Distance",
            description = "Log your first session at a distance you've never shot before.",
            tiers = listOf(1, 3, 6),
            category = TrophyCategory.exploration,
        ),
        TrophyDef(
            kind = "distance_explorer",
            name = "Distance Explorer",
            description = "Shoot at 4 or more distinct distances across your sessions.",
            tiers = listOf(4, 6, 8),
            category = TrophyCategory.exploration,
        ),
        // ── Course (3D) ──
        TrophyDef(
            kind = "course_first",
            name = "Into the Woods",
            description = "Walk and shoot your first 3D course.",
            tiers = emptyList(),
            category = TrophyCategory.course,
        ),
        TrophyDef(
            kind = "course_milestone",
            name = "Course Regular",
            description = "Total 3D courses completed — 5 to 100.",
            tiers = listOf(5, 10, 25, 50, 100),
            category = TrophyCategory.course,
        ),
        TrophyDef(
            kind = "course_explorer",
            name = "Rule Set",
            description = "Shoot a course under 2, then all 3 scoring systems (ASA, IBO, WA3D).",
            tiers = listOf(2, 3),
            category = TrophyCategory.course,
        ),
        TrophyDef(
            kind = "course_marathon",
            name = "Long Walk",
            description = "Shoot a single 3D course of 20, 30, then 40 stations.",
            tiers = listOf(20, 30, 40),
            category = TrophyCategory.course,
        ),
        TrophyDef(
            kind = "course_pr",
            name = "Course Record",
            description = "Beat your best 3D course score under a scoring system.",
            tiers = emptyList(),
            category = TrophyCategory.course,
        ),
        // ── Competition (leagues) ──
        TrophyDef(
            kind = "league_first_finish",
            name = "In the Running",
            description = "Finish your first league.",
            tiers = emptyList(),
            category = TrophyCategory.competition,
        ),
        TrophyDef(
            kind = "league_champion",
            name = "League Champion",
            description = "Win 1, 3, 5, then 10 leagues outright.",
            tiers = listOf(1, 3, 5, 10),
            category = TrophyCategory.competition,
        ),
        TrophyDef(
            kind = "league_podium",
            name = "Podium Finish",
            description = "Finish a league in the top 3 — 3, 5, 10, then 25 times.",
            tiers = listOf(3, 5, 10, 25),
            category = TrophyCategory.competition,
        ),
        // ── Community (clubs) ──
        TrophyDef(
            kind = "club_founder",
            name = "Club Founder",
            description = "Found a club of your own.",
            tiers = emptyList(),
            category = TrophyCategory.community,
        ),
        TrophyDef(
            kind = "club_host_growth",
            name = "Club Builder",
            description = "Grow a club you host to 5, 10, 25, 50, then 100 members.",
            tiers = listOf(5, 10, 25, 50, 100),
            category = TrophyCategory.community,
        ),
        TrophyDef(
            kind = "club_member",
            name = "Joiner",
            description = "Be a member of 3, 5, then 10 clubs.",
            tiers = listOf(3, 5, 10),
            category = TrophyCategory.community,
        ),
    )

    // --- §17 Club announcement board ----------------------------------------
    // Keyed by club id; the dev club (club_001) is the one the dev hosts.

    val clubAnnouncements: Map<String, List<ClubAnnouncement>> = mapOf(
        "club_001" to listOf(
            ClubAnnouncement(
                id = "ann_001",
                clubId = "club_001",
                authorUserId = devUserId,
                authorHandle = devHandle,
                authorDisplayName = "Andrew Nguyen",
                body = "Range closed this Sunday for the regional shoot — " +
                    "we're back to normal hours Monday.",
                pinned = true,
                createdAt = daysAgo(1),
            ),
            ClubAnnouncement(
                id = "ann_002",
                clubId = "club_001",
                authorUserId = devUserId,
                authorHandle = devHandle,
                authorDisplayName = "Andrew Nguyen",
                body = "New target butts arrived. Thanks to everyone who " +
                    "chipped in for the order.",
                pinned = false,
                createdAt = daysAgo(5),
            ),
        ),
    )

    // --- §17 League attachments ---------------------------------------------
    // Keyed by league id; the dev league (lg_001) is the one the dev hosts.

    val leagueAttachments: Map<String, List<LeagueAttachment>> = mapOf(
        "lg_001" to listOf(
            LeagueAttachment(
                id = "att_001",
                leagueId = "lg_001",
                addedByUserId = devUserId,
                addedByHandle = devHandle,
                kind = AttachmentKind.LINK,
                title = "Official round rules (WA Indoor)",
                url = "https://worldarchery.sport/rulebook",
                note = null,
                createdAt = daysAgo(20),
            ),
            LeagueAttachment(
                id = "att_002",
                leagueId = "lg_001",
                addedByUserId = devUserId,
                addedByHandle = devHandle,
                kind = AttachmentKind.NOTE,
                title = "Scoring reminder",
                url = null,
                note = "Submit by Sunday 23:59 — late scores roll to next week.",
                createdAt = daysAgo(12),
            ),
        ),
    )

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
