package com.andrewnguyen.bowpress.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

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
    /**
     * Profile-picture cache-buster — 0 / null when no avatar is set,
     * otherwise the version to ask `/social/avatars/:userId?v=...` for.
     * Drives the URL-vs-monogram fork on every avatar render surface.
     * Defaulted so a pre-avatarVersion payload still decodes.
     */
    val avatarVersion: Int? = null,
    /**
     * Pre-resolved avatar URL — most endpoints don't emit this (the API
     * just bumps [avatarVersion] and clients build the URL themselves),
     * but the field is here so any endpoint that does emit one — or a
     * future contract change — wires through without another model bump.
     */
    val avatarUrl: String? = null,
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

/**
 * Minimal session summary attached to a friend profile. Mirrors API §2
 * `SessionSummary`.
 *
 * The API emits null on every scored field below for friend-profile rows
 * because per-session score/title/face aren't computed at that endpoint —
 * the profile screen uses them as row headers, not as scoring data.
 * Marking them nullable matches the iOS oracle (commit 82b38fd) and fixes a
 * latent decode crash on any friend with sessions.
 */
@Serializable
data class SessionSummary(
    val id: String,
    val title: String? = null,
    val distance: String? = null,
    val targetFaceType: String? = null,
    val score: Int? = null,
    val xCount: Int? = null,
    val arrowCount: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val endedAt: Instant? = null,
)

/**
 * 30-day stat block on a friend profile.
 *
 * `avgArrowScore` and `xRate` are nullable because the API emits null when
 * the friend has zero arrows in the window — matches iOS (commit 82b38fd).
 */
@Serializable
data class Stat30d(
    val avgArrowScore: Double? = null,
    val xRate: Double? = null,
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

/**
 * Paginated feed response envelope — wraps one page of [ActivityItem]s and an
 * opaque [nextCursor] the caller passes back to load the following page.
 * [nextCursor] is null when there are no more pages.
 */
@Serializable
data class FeedPage(
    val items: List<ActivityItem>,
    val nextCursor: String?,
)

@Serializable
enum class ActivityKind {
    friend_pr, club_session, league_event, friend_setup, club_member_joined,
    // §15: a friend shared a (non-PR) session.
    friend_session,
    // Real-event rows: a club/league was created, or a league concluded
    // with a podium finish.
    club_created, league_created, league_podium,
    // Forward-compat: any feed kind a newer server emits that this build
    // doesn't recognise. `ActivityItem.kind` defaults to this and the JSON
    // reader's `coerceInputValues` maps unknown values here, so one
    // unfamiliar row can never fail the whole feed decode.
    unknown,
}

@Serializable
enum class ActivitySourceKind { friend, club, league }

/**
 * An Instagram-style location tag on a shared session (§18) — the named place
 * an archer shot at, plus the coordinate the map popup centres on.
 */
@Serializable
data class SessionLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

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
    // Defaulted so `coerceInputValues` can coerce an unrecognised kind to
    // `unknown` instead of failing the decode (see ActivityKind.unknown).
    val kind: ActivityKind = ActivityKind.unknown,
    val sourceKind: ActivitySourceKind,
    val actorHandle: String,
    val actorDisplayName: String,
    /**
     * Actor's profile-picture cache-buster — 0 / null when no avatar is set,
     * otherwise the version for `/social/avatars/:userId?v=...`. Defaulted so
     * a pre-avatarVersion feed payload still decodes (monogram fallback).
     * Mirrors iOS `ActivityItem.actorAvatarVersion` (commit ebd0258).
     */
    val actorAvatarVersion: Int? = null,
    /** Pre-resolved actor avatar URL when the endpoint emits one. */
    val actorAvatarUrl: String? = null,
    val title: String,
    val meta: String? = null,
    val stamp: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    // ── §15 shared-session fields ──
    val session: ActivitySession? = null,
    val achievements: List<AchievementBadge> = emptyList(),
    val highlighted: Boolean = false,
    // ── Routing targets — every feed row drills somewhere. Defaulted so a
    // feed row from an older API still decodes.
    val actorUserId: String = "",
    val clubId: String? = null,
    val leagueId: String? = null,
    // ── Social Feed V2 (contract §1, §2) ──
    // `titleIsCustom` is true when [title] is the archer's own session name
    // rather than a generic verb phrase — clients may render it as a quoted
    // caption instead of gluing it to the actor verb.
    // `isOwn` is true when the actor is the signed-in caller — the API now
    // interleaves the caller's own activity into the feed, and only `isOwn`
    // rows expose the edit affordance.
    // Both defaulted so a feed row from an older API still decodes.
    val titleIsCustom: Boolean = false,
    val isOwn: Boolean = false,
    // ── Social Feed V2 Part 2 — Likes & Comments (contract §5) ──
    // `subjectId` is the stable id likes/comments attach to: the
    // `sharedSessionId` for a session post, else the activity row's own id
    // (§5.1). `likeCount`/`likedByMe` drive the like button; `commentCount`
    // the comment button. All defaulted so a feed row from a pre-§5 API still
    // decodes — `subjectId` then falls back to `id` via [resolvedSubjectId].
    val subjectId: String = "",
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val commentCount: Int = 0,
    // ── Social Feed V2 Part 3 — Comment threads & kudos (contract §6.4) ──
    // `likers` is up to 3 most-recent likers, hydrated for the Strava-style
    // kudos avatar stack on the feed card; `likeCount` carries the true total
    // (the stack shows `likers` + a `+{likeCount - likers.size}` chip).
    // Defaulted so a pre-§6 feed payload still decodes.
    val likers: List<ActivityActor> = emptyList(),
) {
    /**
     * The id likes/comments attach to. The §5 API always sends `subjectId`,
     * but a pre-§5 feed payload omits it — fall back to the activity row id
     * (which equals the subject id for non-session events) so the like/comment
     * buttons still address *something* coherent.
     */
    val resolvedSubjectId: String get() = subjectId.ifBlank { id }
}

/**
 * Mirrors API contract §6.5 `ActivityActor` — a minimal actor descriptor used
 * for the kudos avatar stack (the `likers` list) and the full liker list.
 */
@Serializable
data class ActivityActor(
    val userId: String,
    val handle: String,
    val displayName: String,
    /**
     * Profile-picture cache-buster — 0 / null when no avatar is set,
     * otherwise the version for `/social/avatars/:userId?v=...`. Drives
     * the kudos-stack / liker-list avatar render. Defaulted so a
     * pre-avatarVersion payload still decodes (monogram fallback).
     * Mirrors iOS `ActivityActor.avatarVersion` (commit ebd0258).
     */
    val avatarVersion: Int? = null,
    /** Pre-resolved actor avatar URL when the endpoint emits one. */
    val avatarUrl: String? = null,
)

// ── Mentions — handle search (mentions contract §2.1 / §3.1) ─────────────────

/**
 * Mirrors API `GET /social/handles?q=<prefix>` — one row of the @-mention
 * autocomplete. The endpoint returns up to 8 of these whose [handle] starts
 * with the typed prefix, the caller's accepted friends ranked first, the
 * caller and anyone block-related excluded.
 */
@Serializable
data class HandleSuggestion(
    val userId: String,
    val handle: String,
    val displayName: String,
)

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
 * Mirrors API §12 `SocialPendingCount`. Drives the Social tab + bell badge —
 * the badge shows when [total] > 0. [total] now folds in unread
 * notifications, so likes and comments count toward it; [notifications] is
 * the unread-informational sub-count.
 */
@Serializable
data class SocialPendingCount(
    val friendRequests: Int = 0,
    val invitations: Int = 0,
    val notifications: Int = 0,
    val total: Int = 0,
)

// ── §13 Notification center ──────────────────────────────────────────────────

/**
 * One row in the notification center — the persistent in-app record behind
 * the bell. [type] is the event kind (like | comment | comment_reply |
 * mention_post | mention_comment | friend_request | friend_accepted |
 * club_invite | league_invite | club_announcement | friend_pr | …).
 */
@Serializable
data class SocialNotification(
    val id: String,
    val type: String = "",
    val actorUserId: String? = null,
    val actorHandle: String? = null,
    val actorDisplayName: String? = null,
    val subjectId: String? = null,
    val title: String,
    val body: String? = null,
    val read: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
) {
    /** Coarse category for the notification-center filter pills. */
    val category: NotificationCategory
        get() = when (type) {
            "like" -> NotificationCategory.Kudos
            "comment", "comment_reply" -> NotificationCategory.Comments
            "mention_post", "mention_comment" -> NotificationCategory.Mentions
            "club_invite", "league_invite", "club_announcement",
            "league_deadline", "league_event" -> NotificationCategory.League
            else -> NotificationCategory.Other
        }

    /** True for the maple-tinted alert rows (a friend's PR). */
    val isAlert: Boolean get() = type == "friend_pr"
}

/** Filter-pill buckets for the notification center. */
enum class NotificationCategory { All, Kudos, Comments, Mentions, League, Other }

/** `GET /social/notifications` envelope — the list + the header unread count. */
@Serializable
data class NotificationList(
    val items: List<SocialNotification> = emptyList(),
    val unread: Int = 0,
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
 *
 * 12 kinds total as of API §18 trophy catalogue expansion. The serializer uses
 * [kotlinx.serialization.json.JsonNames] (via [SerialName]) so an unknown kind
 * from a future API version decodes as null and the caller can fall back to a
 * safe default — see [AchievementKind.orUnknown].
 *
 * Wire values are the exact lowercase snake_case strings the API sends.
 */
@Serializable
enum class AchievementKind {
    // Skill
    score_pr,
    x_pr,
    @SerialName("flawless") flawless,
    @SerialName("sharpshooter") sharpshooter,
    @SerialName("xs_milestone") xs_milestone,
    // Milestone
    arrows_milestone,
    sessions_milestone,
    @SerialName("marathon") marathon,
    // Streak
    streak,
    @SerialName("weeks_active") weeks_active,
    @SerialName("comeback") comeback,
    // Exploration
    first_distance,
    @SerialName("distance_explorer") distance_explorer,
    // Course (3D)
    @SerialName("course_first") course_first,
    @SerialName("course_milestone") course_milestone,
    @SerialName("course_explorer") course_explorer,
    @SerialName("course_marathon") course_marathon,
    @SerialName("course_pr") course_pr,
    // Competition (leagues)
    @SerialName("league_first_finish") league_first_finish,
    @SerialName("league_champion") league_champion,
    @SerialName("league_podium") league_podium,
    // Community (clubs)
    @SerialName("club_founder") club_founder,
    @SerialName("club_host_growth") club_host_growth,
    @SerialName("club_member") club_member,
    // Forward-compat fallback: a kind a newer API emits that this build does
    // not recognize. The network Json uses coerceInputValues, so an unknown
    // wire value lands here whenever the decoded property defaults to it.
    unknown,
}

/**
 * Trophy catalogue — the full set of kinds a user can earn, keyed by category.
 * Mirrors the API §18 `GET /social/trophies` `TrophyDef` shape.
 */
@Serializable
enum class TrophyCategory {
    @SerialName("skill") skill,
    @SerialName("milestone") milestone,
    @SerialName("streak") streak,
    @SerialName("exploration") exploration,
    @SerialName("course") course,
    @SerialName("competition") competition,
    @SerialName("community") community,
    // Forward-compat fallback for a category a newer API adds — see [unknown]
    // on [AchievementKind].
    unknown,
}

/**
 * Mirrors API §18 `TrophyDef` — the server's canonical definition of a trophy
 * kind: its human-readable name, description (the "how to earn" hint), and the
 * tier breakpoints (e.g. `[7, 14, 30]` for streak days).
 *
 * The client renders every [TrophyDef] in the catalogue as either earned
 * (full-colour, showing the earned tier/value) or locked (dimmed, showing the
 * description as a hint). Unknown [kind] values from a future API version do
 * **not** crash — the row simply renders as locked with no matching earned
 * achievement.
 */
@Serializable
data class TrophyDef(
    // A raw String, not the AchievementKind enum, so the catalogue decode is
    // immune to a future kind — the client matches it against earned
    // achievements by string.
    val kind: String,
    val name: String,
    val description: String,
    val tiers: List<Int> = emptyList(),
    val category: TrophyCategory = TrophyCategory.skill,
)

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
    // Migration 0039 — a free-text caption, distinct from the short `title`.
    // May contain @handle mentions; null when the post has no description.
    val description: String? = null,
    @Serializable(with = InstantSerializer::class)
    val shotAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    // §18 — the tagged location, or null when shared untagged. New, so
    // decoding tolerates pre-v1.6 payloads that omit it.
    val location: SessionLocation? = null,
)

/**
 * Mirrors API §15 `Achievement` — a server-detected standout, the unit of the
 * profile trophy case. `sharedSessionId` is null for league and club
 * trophies, which are not earned from a shared session.
 */
@Serializable
data class Achievement(
    val id: String,
    val userId: String,
    val sharedSessionId: String? = null,
    // Defaults to `unknown` so the network Json's coerceInputValues maps an
    // unrecognized kind from a newer API here instead of failing the decode.
    val kind: AchievementKind = AchievementKind.unknown,
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
    val kind: AchievementKind = AchievementKind.unknown,
    val label: String,
    val value: Int,
    val sublabel: String? = null,
)

/**
 * Status of a shared-session photo's display-JPEG transcode.
 *
 * Defaults to [unknown] so the network Json's `coerceInputValues` maps an
 * unrecognised status from a newer API here instead of failing the decode.
 */
@Serializable
enum class PhotoStatus {
    pending, ready, failed,
    // Forward-compat fallback.
    unknown,
}

/**
 * Mirrors API contract §4 `ActivityPhoto` — the wire subset of a
 * shared-session photo carried on feed rows and the shared-session detail
 * payload. Ordered by [position]. Clients fetch each `ready` photo's bytes
 * from the GET-by-id endpoint (Bearer auth).
 */
@Serializable
data class ActivityPhoto(
    val id: String,
    val status: PhotoStatus = PhotoStatus.unknown,
    val position: Int = 0,
)

/**
 * Mirrors API contract §4 `SharedSessionPhoto` — the full photo record
 * returned by the photo upload / list endpoints.
 */
@Serializable
data class SharedSessionPhoto(
    val id: String,
    val sharedSessionId: String,
    val userId: String,
    val position: Int = 0,
    val status: PhotoStatus = PhotoStatus.unknown,
    val contentType: String? = null,
    val byteSize: Long? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

// NOTE — `PATCH /social/sessions/:sharedSessionId` (contract §3) has no typed
// request DTO on purpose. The contract distinguishes an *omitted* field
// (leave unchanged) from an explicit JSON `null` (clear it); a plain
// `@Serializable data class` cannot express that distinction (the shared
// network `Json` has `explicitNulls = false`, which drops a `null` entirely).
// `SocialRepository.editSharedSession` builds the patch JSON object by hand,
// putting only the keys that actually changed and `JsonNull` for a clear.

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
    // §18 — discipline picks the feed-row preview (a target face for a range
    // session, a course block for a 3D course); location backs the "in {place}"
    // tag + map popup. Both are new, so decoding is backward-tolerant:
    // pre-v1.6 feed payloads omit them → null.
    val discipline: String? = null,
    val location: SessionLocation? = null,
    // First 10 ends of the scorecard, each a list of arrow ring values
    // (11 = X … 0 = miss) in shot order — drives the feed-row scorecard
    // preview beside the target face. null for a 3D course, a session with
    // no recorded arrows, or a pre-scorecard feed payload.
    val endRings: List<List<Int>>? = null,
    // Plotted arrow positions for the feed-row target face — each [x, y]
    // normalised to −1…1 (east-positive, south-positive) in shot order.
    // null for a 3D course or arrows with no recorded coordinates.
    val plotPoints: List<List<Double>>? = null,
    // For a 3D course — the ordered stations, so the feed preview can draw
    // the real course map. The bowpress-api `/social/feed` returns them for
    // `3d_course` items; null for a range session or a pre-v1.7 payload.
    val stations: List<CourseStation>? = null,
    // Social Feed V2 (contract §4) — the multi-photo gallery, ordered by
    // position. Empty on a session with no photos or a pre-V2 feed payload.
    val photos: List<ActivityPhoto> = emptyList(),
    // Migration 0039 — the post's free-text caption (may contain @mentions),
    // or null. Drives the truncated description line on the feed card.
    val description: String? = null,
) {
    /**
     * True when the shared session is a walked 3D course rather than a
     * fixed-distance range session — drives the feed-row preview choice.
     */
    val isCourse: Boolean get() = discipline == "3d_course"
}

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
    // §18 — the Instagram-style location tag, omitted when shared untagged.
    val location: SessionLocation? = null,
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
    /**
     * Owner's profile-picture cache-buster — 0 / null when no avatar is set,
     * otherwise the version for `/social/avatars/:userId?v=...`. Drives the
     * detail screen's owner-avatar render. Defaulted so a pre-avatarVersion
     * detail payload still decodes. Mirrors iOS
     * `SharedSessionDetail.ownerAvatarVersion` (commit ebd0258).
     */
    val ownerAvatarVersion: Int? = null,
    /** Pre-resolved owner avatar URL when the endpoint emits one. */
    val ownerAvatarUrl: String? = null,
    val session: ShootingSession? = null,
    val ends: List<SessionEnd> = emptyList(),
    val arrows: List<ArrowPlot> = emptyList(),
    // Course stations — populated only when the shared session is a walked
    // 3D course; empty for a range session (which carries ends/arrows).
    val stations: List<CourseStation> = emptyList(),
    // Social Feed V2 (contract §4) — the multi-photo gallery, ordered by
    // position. Empty on a session with no photos or a pre-V2 payload.
    val photos: List<ActivityPhoto> = emptyList(),
    // Social Feed V2 Part 2 (contract §5.4) — like/comment counts so the
    // detail screen shows the same affordances as the feed row. `subjectId`
    // is the like/comment subject. All defaulted so a pre-§5 detail payload
    // still decodes.
    val subjectId: String = "",
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val commentCount: Int = 0,
    // Social Feed V2 Part 3 (contract §6.4) — up to 3 most-recent likers for
    // the kudos avatar stack. Empty on a pre-§6 detail payload.
    val likers: List<ActivityActor> = emptyList(),
)

// ── §5 Likes & Comments (Social Feed V2 Part 2) ──────────────────────────────

/**
 * Mirrors API contract §5.5 / §6.5 `ActivityComment` — one comment on a feed
 * subject, hydrated with its author's handle + display name.
 *
 * Part 3 (§6) adds one level of threading: a top-level comment carries its
 * nested [replies] (oldest→newest) and [replyCount]; a reply carries the
 * top-level [parentCommentId] it hangs under (the API normalises a
 * reply-to-a-reply up to its top-level parent, the addressee then living in an
 * `@mention` in the body). It is also a likeable subject — [likeCount] /
 * [likedByMe] reuse the generic like infra keyed on the comment's own id.
 *
 * `createdAt`/`updatedAt` are ISO-8601 strings on the wire; decoded to
 * [Instant] with the shared [InstantSerializer]. All §6 fields are defaulted
 * so a pre-§6 comment payload still decodes.
 */
@Serializable
data class ActivityComment(
    val id: String,
    val subjectId: String,
    val userId: String,
    val authorHandle: String,
    val authorDisplayName: String,
    /**
     * Author's profile-picture cache-buster — 0 / null when no avatar is set,
     * otherwise the version for `/social/avatars/:userId?v=...`. Drives the
     * per-comment avatar render. Defaulted so a pre-avatarVersion comment
     * payload still decodes (monogram fallback). Mirrors iOS
     * `ActivityComment.authorAvatarVersion` (commit ebd0258).
     */
    val authorAvatarVersion: Int? = null,
    /** Pre-resolved author avatar URL when the endpoint emits one. */
    val authorAvatarUrl: String? = null,
    val body: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    // ── §6 comment threads + comment likes ──
    val parentCommentId: String? = null,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val replies: List<ActivityComment> = emptyList(),
    val replyCount: Int = 0,
)

/** Sort order for the comment thread — `GET .../comments?sort=` (§6.3). */
@Serializable
enum class CommentSort {
    /** Newest top-level comments first — the default. */
    recent,

    /** Most-liked top-level comments first, then newest. */
    top;

    /** The lowercase wire value the API expects on the `sort` query param. */
    val wire: String get() = name
}

/**
 * Response from `POST`/`DELETE social/activity/:subjectId/like` (§5.3) — the
 * fresh like count plus whether the caller now likes the subject. Part 3
 * (§6.2) reuses the same endpoint with a comment id as the subject.
 */
@Serializable
data class ToggleLikeResponse(
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
)

/**
 * Request body for `POST social/activity/:subjectId/comments` (§5.3, §6.3).
 * [parentCommentId] is set when the comment is a reply — the API normalises it
 * to the top-level comment id.
 */
@Serializable
data class PostCommentBody(
    val body: String,
    val parentCommentId: String? = null,
)

/**
 * Whether [callerUserId] may delete [comment] — true for the comment's own
 * author **or** the subject owner (moderation), per contract §5.3. The server
 * is authoritative (a forbidden delete is a 403); this mirror lets the client
 * gate the affordance and short-circuit a doomed request.
 *
 * [subjectOwnerUserId] is the §5.1 subject owner: the shared session's owner
 * for a session subject, else the activity's actor. Null when the client
 * cannot determine it — only the author check then applies.
 */
fun canDeleteComment(
    comment: ActivityComment,
    callerUserId: String,
    subjectOwnerUserId: String?,
): Boolean =
    callerUserId.isNotBlank() &&
        (comment.userId == callerUserId || subjectOwnerUserId == callerUserId)

// ── §17 Club announcement board + league attachments ────────────────────────

/**
 * Mirrors API §17 `ClubAnnouncement` — a post on a club's announcement board.
 * Members read; the host posts. Pinned posts sort first, then newest.
 */
@Serializable
data class ClubAnnouncement(
    val id: String,
    val clubId: String,
    val authorUserId: String,
    val authorHandle: String,
    val authorDisplayName: String,
    val body: String,
    val pinned: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/** Request body for `POST /social/clubs/:id/announcements`. */
@Serializable
data class CreateAnnouncementBody(
    val body: String,
    val pinned: Boolean = false,
)

/** Request body for `PATCH /social/clubs/:id/announcements/:annId`. */
@Serializable
data class UpdateAnnouncementBody(
    val pinned: Boolean,
)

/** What a [LeagueAttachment] is — a shared link, an uploaded file, or a plain note. */
@Serializable
enum class AttachmentKind {
    @SerialName("link") LINK,
    @SerialName("file") FILE,
    @SerialName("note") NOTE,
}

/**
 * Mirrors API §17 `LeagueAttachment` — a resource the host pinned to a league
 * (a rules link, a results file, a note). `url` is required for link/file.
 */
@Serializable
data class LeagueAttachment(
    val id: String,
    val leagueId: String,
    val addedByUserId: String,
    val addedByHandle: String,
    val kind: AttachmentKind,
    val title: String,
    val url: String? = null,
    val note: String? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/** Request body for `POST /social/leagues/:id/attachments`. */
@Serializable
data class CreateAttachmentBody(
    val kind: AttachmentKind,
    val title: String,
    val url: String? = null,
    val note: String? = null,
)
