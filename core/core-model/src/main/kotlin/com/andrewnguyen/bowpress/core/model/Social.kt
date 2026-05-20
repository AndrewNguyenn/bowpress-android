package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.math.roundToInt

// ── §1 Social Profile ────────────────────────────────────────────────────────

/** Who can read a user's sessions / friend profile. */
@Serializable
enum class SocialVisibility {
    nobody, friends, club, public;
}

/** Division derived from the active bow type. */
@Serializable
enum class Division { CMP, REC, BAR }

/**
 * Mirrors API §1 `SocialProfile`.
 */
@Serializable
data class SocialProfile(
    val userId: String,
    val handle: String,
    val displayName: String,
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant,
    val visibility: SocialVisibility = SocialVisibility.friends,
    val bowSummary: String? = null,
    val sessionCount: Int = 0,
    val arrowCount: Int = 0,
    val division: Division? = null,
)

@Serializable
data class UpdateSocialProfileRequest(
    val handle: String? = null,
    val displayName: String? = null,
    val visibility: SocialVisibility? = null,
)

// ── §2 Friendships ───────────────────────────────────────────────────────────

@Serializable
enum class FriendshipStatus { pending, accepted }

@Serializable
enum class FriendshipSource { handle, link }

@Serializable
enum class FriendshipDirection { incoming, outgoing }

/**
 * Mirrors API §2 `Friendship`. The `direction` field is only relevant for `pending` status.
 */
@Serializable
data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: FriendshipStatus,
    val source: FriendshipSource = FriendshipSource.handle,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val respondedAt: Instant? = null,
    val otherUserId: String,
    val otherHandle: String,
    val otherDisplayName: String,
    val direction: FriendshipDirection? = null,
)

@Serializable
data class SendFriendRequestBody(
    val handle: String,
    val source: FriendshipSource? = null,
)

/** Minimal session summary attached to a friend profile. Mirrors API §2 `SessionSummary`. */
@Serializable
data class SessionSummary(
    val id: String,
    val title: String? = null,
    val distance: String? = null,
    val targetFaceType: String? = null,
    val score: Int = 0,
    val xCount: Int = 0,
    val arrowCount: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val endedAt: Instant? = null,
)

/** 30-day stat block on a friend profile. */
@Serializable
data class Stat30d(
    val avgArrowScore: Double = 0.0,
    val xRate: Double = 0.0,
    val groupSigmaMm: Double? = null,
    val sessionCount: Int = 0,
    val arrowCount: Int = 0,
)

/** Full friend profile page. Mirrors API §2 `FriendProfile`. */
@Serializable
data class FriendProfile(
    val profile: SocialProfile,
    val recentSessions: List<SessionSummary> = emptyList(),
    val stat30d: Stat30d = Stat30d(),
)

/** Head-to-head compare view. Mirrors API §2 `CompareView`. */
@Serializable
data class CompareStatBlock(
    val avgScore: Double = 0.0,
    val xRate: Double = 0.0,
    val groupSigmaMm: Double? = null,
    val sessionCount: Int = 0,
    val arrowCount: Int = 0,
    val centroidX: Double? = null,
    val centroidY: Double? = null,
)

@Serializable
data class CompareView(
    val me: CompareStatBlock,
    val them: CompareStatBlock,
    val distance: String? = null,
    val face: String? = null,
)

// ── §3 Clubs ─────────────────────────────────────────────────────────────────

@Serializable
enum class ClubRole { host, member }

/**
 * Mirrors API §3 `Club`.
 */
@Serializable
data class Club(
    val id: String,
    val name: String,
    val description: String? = null,
    val notes: String? = null,
    val inviteCode: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val createdBy: String,
    val memberCount: Int = 0,
    val myRole: ClubRole = ClubRole.member,
)

@Serializable
data class ClubMember(
    val userId: String,
    val handle: String,
    val displayName: String,
    val role: ClubRole,
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant,
    val division: Division? = null,
)

@Serializable
data class ClubFeedItem(
    val id: String,
    val kind: String,
    val actorHandle: String,
    val actorDisplayName: String,
    val title: String,
    val meta: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

@Serializable
data class LeaderboardRow(
    val rank: Int,
    val userId: String,
    val handle: String,
    val displayName: String,
    val score: Int,
    val xCount: Int,
    val sessionCount: Int,
    val isYou: Boolean = false,
)

@Serializable
data class CreateClubBody(val name: String, val description: String? = null)

@Serializable
data class UpdateClubBody(
    val name: String? = null,
    val description: String? = null,
    val notes: String? = null,
)

@Serializable
data class JoinClubBody(val inviteCode: String)

// ── §4 Leagues ───────────────────────────────────────────────────────────────

@Serializable
data class RoundDef(val endCount: Int, val arrowsPerEnd: Int)

@Serializable
enum class LeagueScheduleKind { single, weekly }

@Serializable
data class LeagueSchedule(
    val kind: LeagueScheduleKind,
    @Serializable(with = InstantSerializer::class)
    val startsAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val endsAt: Instant,
    val totalWeeks: Int? = null,
    val weekDeadlineDow: Int? = null,
)

@Serializable
enum class HandicapEquation { none, allowance, bracket, rolling }

@Serializable
data class HandicapConfig(
    val equation: HandicapEquation = HandicapEquation.none,
    val allowancePct: Double? = null,
    val setupWeeks: Int = 0,
)

@Serializable
enum class LeagueType { individual, team }

@Serializable
enum class TeamScoring { total, `set-points` }

@Serializable
data class TeamConfig(val size: Int, val scoring: TeamScoring)

@Serializable
enum class LeagueEntryRule { open, `club-only`, `friends-of-members`, `invite-only` }

@Serializable
enum class LeagueStatus { upcoming, active, ended }

@Serializable
data class LeagueEntry(
    val userId: String,
    val handle: String,
    val displayName: String,
    val division: Division,
    val teamId: String? = null,
    val bestScore: Int = 0,
    val bestX: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant,
)

/**
 * Mirrors API §4 `League`.
 */
@Serializable
data class League(
    val id: String,
    val name: String,
    val hostClubId: String? = null,
    val hostUserId: String,
    val type: LeagueType,
    val divisions: List<Division>,
    val team: TeamConfig? = null,
    val round: RoundDef,
    val schedule: LeagueSchedule,
    val handicap: HandicapConfig,
    val entryRule: LeagueEntryRule,
    val inviteCode: String,
    val status: LeagueStatus,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val myEntry: LeagueEntry? = null,
    val entryCount: Int = 0,
)

@Serializable
data class LeagueSubmission(
    val id: String,
    val leagueId: String,
    val userId: String,
    val week: Int,
    val sessionId: String? = null,
    val rawScore: Int,
    val xCount: Int,
    val adjustedScore: Int,
    @Serializable(with = InstantSerializer::class)
    val submittedAt: Instant,
)

@Serializable
data class LeagueStandingRow(
    val rank: Int,
    val userId: String,
    val handle: String,
    val displayName: String,
    val division: Division,
    val total: Int,
    val adjustedTotal: Int,
    val weeksSubmitted: Int,
    val isYou: Boolean = false,
)

@Serializable
data class AdminMatrix(
    val entries: List<LeagueEntry> = emptyList(),
    val submissions: List<LeagueSubmission> = emptyList(),
    val totalWeeks: Int = 0,
)

@Serializable
data class CreateLeagueBody(
    val name: String,
    val hostClubId: String? = null,
    val type: LeagueType,
    val divisions: List<Division>,
    val team: TeamConfig? = null,
    val round: RoundDef,
    val schedule: LeagueSchedule,
    val handicap: HandicapConfig,
    val entryRule: LeagueEntryRule,
)

/**
 * Partial-update body for `PATCH /social/leagues/:id`. All fields are
 * nullable — only set ones are applied. Mirrors [UpdateClubBody].
 */
@Serializable
data class UpdateLeagueBody(
    val name: String? = null,
    val divisions: List<Division>? = null,
    val team: TeamConfig? = null,
    val round: RoundDef? = null,
    val schedule: LeagueSchedule? = null,
    val handicap: HandicapConfig? = null,
    val entryRule: LeagueEntryRule? = null,
)

@Serializable
data class JoinLeagueBody(val inviteCode: String? = null, val division: Division)

@Serializable
data class SubmitScoreBody(
    val week: Int,
    val sessionId: String? = null,
    val rawScore: Int,
    val xCount: Int,
)

// ── §5 Activity Feed ─────────────────────────────────────────────────────────

@Serializable
enum class ActivityKind {
    friend_pr, club_session, league_event, friend_setup, club_member_joined,
    // §15: a friend shared a (non-PR) session.
    friend_session,
}

@Serializable
enum class ActivitySourceKind { friend, club, league }

/**
 * Mirrors API §5 `ActivityItem`, extended in §15 with the shared-session
 * payload, achievement badges, and the `highlighted` flag that drives the
 * Strava-style highlighted-row treatment in the feed.
 *
 * All §15 fields have defaults so a feed row from an older API still decodes.
 */
@Serializable
data class ActivityItem(
    val id: String,
    val kind: ActivityKind,
    val sourceKind: ActivitySourceKind,
    val actorHandle: String,
    val actorDisplayName: String,
    val title: String,
    val meta: String? = null,
    val stamp: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    // ── §15 shared-session fields ──
    val session: ActivitySession? = null,
    val achievements: List<AchievementBadge> = emptyList(),
    val highlighted: Boolean = false,
)

// ── §6 Handicap Math ─────────────────────────────────────────────────────────

/**
 * Pure Kotlin implementation of the handicap math from API contract §6.
 * Must produce identical results on all platforms.
 */
object HandicapCalculator {

    /**
     * Bracket allowance table per §6.
     * avg >= 590 -> 5; >= 580 -> 15; >= 560 -> 35; >= 540 -> 60; >= 500 -> 95; else -> 120
     */
    fun bracketAllowance(avg: Double): Int = when {
        avg >= 590.0 -> 5
        avg >= 580.0 -> 15
        avg >= 560.0 -> 35
        avg >= 540.0 -> 60
        avg >= 500.0 -> 95
        else -> 120
    }

    /**
     * Raw per-week allowance as a `Double` — kept un-rounded so the
     * `rawScore + allowance` sum can be rounded once (see [adjustedScore]).
     * The cross-platform rule is sum-then-round; rounding the allowance on
     * its own would drift from the design JS / iOS / API.
     */
    private fun rawAllowance(
        equation: HandicapEquation,
        baseline: Double,
        allowancePct: Double?,
    ): Double = when (equation) {
        HandicapEquation.none -> 0.0
        HandicapEquation.allowance -> (600.0 - baseline) * (allowancePct ?: 0.8)
        HandicapEquation.bracket -> bracketAllowance(baseline).toDouble()
        HandicapEquation.rolling -> (600.0 - baseline) * 0.85
    }

    /**
     * Per-week allowance points (rounded) for display. The authoritative
     * score math is [adjustedScore], which rounds the *sum* — call this only
     * when an allowance figure must be shown on its own.
     */
    fun perWeekAllowance(
        equation: HandicapEquation,
        baseline: Double,
        allowancePct: Double?,
    ): Int = rawAllowance(equation, baseline, allowancePct).roundToInt()

    /**
     * Adjusted score = round(rawScore + allowance). Sum-then-round matches
     * the design JS (`Math.round`) and the iOS/API implementations.
     */
    fun adjustedScore(
        rawScore: Int,
        equation: HandicapEquation,
        baseline: Double,
        allowancePct: Double?,
    ): Int = (rawScore + rawAllowance(equation, baseline, allowancePct)).roundToInt()
}

// ── §11 Invitations (club + league) ──────────────────────────────────────────

/** Whether an invitation targets a club or a league. */
@Serializable
enum class InvitationKind { club, league }

/** Lifecycle status of a [SocialInvitation]. */
@Serializable
enum class InvitationStatus { pending, accepted, declined }

/**
 * Mirrors API §11 `SocialInvitation`. The club/league analogue of a friend
 * request — a host directly invites a specific archer by handle.
 */
@Serializable
data class SocialInvitation(
    val id: String,
    val kind: InvitationKind,
    val targetId: String,
    val targetName: String,
    val inviterUserId: String,
    val inviterHandle: String,
    val inviteeUserId: String,
    val status: InvitationStatus,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val respondedAt: Instant? = null,
)

/** Request body for `POST /social/{clubs|leagues}/:id/invites`. */
@Serializable
data class SendInvitationBody(val handle: String)

/** Optional request body for `POST /social/invitations/:id/accept`. */
@Serializable
data class AcceptInvitationBody(val division: Division? = null)

// ── §12 Pending count (Social tab badge) ─────────────────────────────────────

/**
 * Mirrors API §12 `SocialPendingCount`. Drives the Social tab badge — the
 * badge shows when [total] > 0.
 */
@Serializable
data class SocialPendingCount(
    val friendRequests: Int = 0,
    val invitations: Int = 0,
    val total: Int = 0,
)

// ── §14 Mute / block ─────────────────────────────────────────────────────────

/** What a [SocialBlock] targets. */
@Serializable
enum class BlockKind { archer, club, league }

/**
 * Severity of a [SocialBlock].
 * - [mute] — soft: the target's activity leaves your feed; no pushes. You stay
 *   friends / a member; fully reversible.
 * - [block] — hard: everything mute does, plus for an `archer` the friendship
 *   is severed and neither side may send a friend request.
 */
@Serializable
enum class BlockMode { mute, block }

/**
 * Mirrors API §14 `SocialBlock` — a mute or block the signed-in user has
 * placed on an archer, club, or league.
 */
@Serializable
data class SocialBlock(
    val id: String,
    val userId: String,
    val kind: BlockKind,
    val targetId: String,
    val targetName: String,
    val mode: BlockMode,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/** Request body for `POST /social/blocks`. Re-posting the same target updates [mode]. */
@Serializable
data class CreateBlockBody(
    val kind: BlockKind,
    val targetId: String,
    val mode: BlockMode,
)

// ── §15 Shared sessions & achievements ───────────────────────────────────────

/**
 * The kind of standout an [Achievement] / [AchievementBadge] represents.
 * Detection is server-only — clients only render these.
 */
@Serializable
enum class AchievementKind {
    score_pr, x_pr, arrows_milestone, sessions_milestone, streak, first_distance
}

/**
 * Mirrors API §15 `SharedSession` — a session a user has published to the
 * friend feed. `sessionId` is the client's `ShootingSession` id.
 */
@Serializable
data class SharedSession(
    val id: String,
    val userId: String,
    val sessionId: String,
    val score: Int,
    val xCount: Int,
    val arrowCount: Int,
    val distance: String? = null,
    val face: String? = null,
    val title: String? = null,
    @Serializable(with = InstantSerializer::class)
    val shotAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/**
 * Mirrors API §15 `Achievement` — a server-detected standout, the unit of the
 * profile trophy case.
 */
@Serializable
data class Achievement(
    val id: String,
    val userId: String,
    val sharedSessionId: String,
    val kind: AchievementKind,
    val label: String,
    val value: Int,
    val sublabel: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/**
 * Compact achievement descriptor embedded in an [ActivityItem] — the badge
 * shown on a highlighted feed row.
 */
@Serializable
data class AchievementBadge(
    val kind: AchievementKind,
    val label: String,
    val value: Int,
    val sublabel: String? = null,
)

/**
 * Shared-session payload embedded in an [ActivityItem] — the stat line shown
 * on a session feed row.
 */
@Serializable
data class ActivitySession(
    val sharedSessionId: String,
    val sessionId: String,
    val score: Int,
    val xCount: Int,
    val arrowCount: Int,
    val distance: String? = null,
    val face: String? = null,
)

/** Request body for `POST /social/sessions/share`. */
@Serializable
data class ShareSessionBody(
    val sessionId: String,
    val score: Int,
    val xCount: Int,
    val arrowCount: Int,
    val distance: String? = null,
    val face: String? = null,
    val title: String? = null,
    @Serializable(with = InstantSerializer::class)
    val shotAt: Instant? = null,
)

/** Response from `POST /social/sessions/share`. */
@Serializable
data class ShareSessionResult(
    val sharedSession: SharedSession,
    val achievements: List<Achievement> = emptyList(),
    val activityId: String? = null,
    val headline: String? = null,
)

// ── §16 Friend session detail ────────────────────────────────────────────────

/**
 * Mirrors API §16 `GET /social/sessions/:sharedSessionId` — a friend's full
 * shared-session detail: the scorecard ends + plotted arrows for the target
 * face. [session] / [ends] / [arrows] are null/empty when the owner has
 * deleted the underlying session (the [sharedSession] stat summary survives).
 */
@Serializable
data class SharedSessionDetail(
    val sharedSession: SharedSession,
    val ownerHandle: String,
    val ownerDisplayName: String,
    val session: ShootingSession? = null,
    val ends: List<SessionEnd> = emptyList(),
    val arrows: List<ArrowPlot> = emptyList(),
)
