package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.database.entities.AchievementEntity
import com.andrewnguyen.bowpress.core.database.entities.ActivityItemEntity
import com.andrewnguyen.bowpress.core.database.entities.BlockEntity
import com.andrewnguyen.bowpress.core.database.entities.ClubEntity
import com.andrewnguyen.bowpress.core.database.entities.FriendshipEntity
import com.andrewnguyen.bowpress.core.database.entities.InvitationEntity
import com.andrewnguyen.bowpress.core.database.entities.LeagueEntity
import com.andrewnguyen.bowpress.core.database.entities.SocialProfileEntity
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementBadge
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.FriendshipDirection
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.HandicapConfig
import com.andrewnguyen.bowpress.core.model.HandicapEquation
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueEntry
import com.andrewnguyen.bowpress.core.model.LeagueEntryRule
import com.andrewnguyen.bowpress.core.model.LeagueSchedule
import com.andrewnguyen.bowpress.core.model.LeagueScheduleKind
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.LeagueType
import com.andrewnguyen.bowpress.core.model.RoundDef
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.TeamConfig
import com.andrewnguyen.bowpress.core.model.TeamScoring
import com.andrewnguyen.bowpress.core.model.Friendship
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ── SocialProfile ──────────────────────────────────────────────────────────

fun SocialProfileEntity.toDto(): SocialProfile = SocialProfile(
    userId = userId,
    handle = handle,
    displayName = displayName,
    joinedAt = joinedAt,
    visibility = runCatching { SocialVisibility.valueOf(visibility) }.getOrDefault(SocialVisibility.friends),
    bowSummary = bowSummary,
    sessionCount = sessionCount,
    arrowCount = arrowCount,
    division = division?.let { runCatching { Division.valueOf(it) }.getOrNull() },
)

// Social writes are online-first in v1 — entities use the default
// `pendingSync = false`; no caller flips it.
fun SocialProfile.toEntity(): SocialProfileEntity = SocialProfileEntity(
    userId = userId,
    handle = handle,
    displayName = displayName,
    joinedAt = joinedAt,
    visibility = visibility.name,
    bowSummary = bowSummary,
    sessionCount = sessionCount,
    arrowCount = arrowCount,
    division = division?.name,
)

// ── Friendship ─────────────────────────────────────────────────────────────

fun FriendshipEntity.toDto(): Friendship = Friendship(
    id = id,
    requesterId = requesterId,
    addresseeId = addresseeId,
    status = runCatching { FriendshipStatus.valueOf(status) }.getOrDefault(FriendshipStatus.pending),
    source = runCatching { FriendshipSource.valueOf(source) }.getOrDefault(FriendshipSource.handle),
    createdAt = createdAt,
    respondedAt = respondedAt,
    otherUserId = otherUserId,
    otherHandle = otherHandle,
    otherDisplayName = otherDisplayName,
    direction = direction?.let { runCatching { FriendshipDirection.valueOf(it) }.getOrNull() },
)

fun Friendship.toEntity(): FriendshipEntity = FriendshipEntity(
    id = id,
    requesterId = requesterId,
    addresseeId = addresseeId,
    status = status.name,
    source = source.name,
    createdAt = createdAt,
    respondedAt = respondedAt,
    otherUserId = otherUserId,
    otherHandle = otherHandle,
    otherDisplayName = otherDisplayName,
    direction = direction?.name,
)

// ── Club ───────────────────────────────────────────────────────────────────

fun ClubEntity.toDto(): Club = Club(
    id = id,
    name = name,
    description = description,
    notes = notes,
    inviteCode = inviteCode,
    createdAt = createdAt,
    createdBy = createdBy,
    memberCount = memberCount,
    myRole = runCatching { ClubRole.valueOf(myRole) }.getOrDefault(ClubRole.member),
)

fun Club.toEntity(): ClubEntity = ClubEntity(
    id = id,
    name = name,
    description = description,
    notes = notes,
    inviteCode = inviteCode,
    createdAt = createdAt,
    createdBy = createdBy,
    memberCount = memberCount,
    myRole = myRole.name,
)

// ── ActivityItem ───────────────────────────────────────────────────────────

fun ActivityItemEntity.toDto(): ActivityItem = ActivityItem(
    id = id,
    kind = runCatching { ActivityKind.valueOf(kind) }.getOrDefault(ActivityKind.unknown),
    sourceKind = runCatching { ActivitySourceKind.valueOf(sourceKind) }.getOrDefault(ActivitySourceKind.club),
    actorHandle = actorHandle,
    actorDisplayName = actorDisplayName,
    title = title,
    meta = meta,
    stamp = stamp,
    createdAt = createdAt,
    session = sessionJson?.let { runCatching { json.decodeFromString<ActivitySession>(it) }.getOrNull() },
    achievements = achievementsJson
        ?.let { runCatching { json.decodeFromString<List<AchievementBadge>>(it) }.getOrNull() }
        ?: emptyList(),
    highlighted = highlighted,
    actorUserId = actorUserId,
    clubId = clubId,
    leagueId = leagueId,
)

fun ActivityItem.toEntity(): ActivityItemEntity = ActivityItemEntity(
    id = id,
    kind = kind.name,
    sourceKind = sourceKind.name,
    actorHandle = actorHandle,
    actorDisplayName = actorDisplayName,
    title = title,
    meta = meta,
    stamp = stamp,
    createdAt = createdAt,
    sessionJson = session?.let { json.encodeToString(it) },
    achievementsJson = if (achievements.isEmpty()) null else json.encodeToString(achievements),
    highlighted = highlighted,
    actorUserId = actorUserId,
    clubId = clubId,
    leagueId = leagueId,
)

// ── League ─────────────────────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
fun LeagueEntity.toDto(): League {
    val divisions = json.decodeFromString<List<String>>(divisions)
        .mapNotNull { runCatching { Division.valueOf(it) }.getOrNull() }
    val round = runCatching { json.decodeFromString<RoundDef>(roundJson) }
        .getOrDefault(RoundDef(endCount = 0, arrowsPerEnd = 0))
    val schedule = runCatching { json.decodeFromString<LeagueSchedule>(scheduleJson) }
        .getOrDefault(LeagueSchedule(kind = LeagueScheduleKind.single, startsAt = Instant.EPOCH, endsAt = Instant.EPOCH))
    val handicap = runCatching { json.decodeFromString<HandicapConfig>(handicapJson) }
        .getOrDefault(HandicapConfig())
    val team = teamJson?.let { runCatching { json.decodeFromString<TeamConfig>(it) }.getOrNull() }
    val myEntry = myEntryJson?.let { runCatching { json.decodeFromString<LeagueEntry>(it) }.getOrNull() }
    return League(
        id = id,
        name = name,
        hostClubId = hostClubId,
        hostUserId = hostUserId,
        type = runCatching { LeagueType.valueOf(leagueType) }.getOrDefault(LeagueType.individual),
        divisions = divisions,
        team = team,
        round = round,
        schedule = schedule,
        handicap = handicap,
        entryRule = runCatching { LeagueEntryRule.valueOf(entryRule) }.getOrDefault(LeagueEntryRule.open),
        inviteCode = inviteCode,
        status = runCatching { LeagueStatus.valueOf(status) }.getOrDefault(LeagueStatus.upcoming),
        createdAt = createdAt,
        myEntry = myEntry,
        entryCount = entryCount,
    )
}

fun League.toEntity(): LeagueEntity = LeagueEntity(
    id = id,
    name = name,
    hostClubId = hostClubId,
    hostUserId = hostUserId,
    leagueType = type.name,
    divisions = json.encodeToString(divisions.map { it.name }),
    teamJson = team?.let { json.encodeToString(it) },
    roundJson = json.encodeToString(round),
    scheduleJson = json.encodeToString(schedule),
    handicapJson = json.encodeToString(handicap),
    entryRule = entryRule.name,
    inviteCode = inviteCode,
    status = status.name,
    createdAt = createdAt,
    myEntryJson = myEntry?.let { json.encodeToString(it) },
    entryCount = entryCount,
)

// ── Invitation (§11) ─────────────────────────────────────────────────────────

fun InvitationEntity.toDto(): SocialInvitation = SocialInvitation(
    id = id,
    kind = runCatching { InvitationKind.valueOf(kind) }.getOrDefault(InvitationKind.club),
    targetId = targetId,
    targetName = targetName,
    inviterUserId = inviterUserId,
    inviterHandle = inviterHandle,
    inviteeUserId = inviteeUserId,
    status = runCatching { InvitationStatus.valueOf(status) }.getOrDefault(InvitationStatus.pending),
    createdAt = createdAt,
    respondedAt = respondedAt,
)

fun SocialInvitation.toEntity(): InvitationEntity = InvitationEntity(
    id = id,
    kind = kind.name,
    targetId = targetId,
    targetName = targetName,
    inviterUserId = inviterUserId,
    inviterHandle = inviterHandle,
    inviteeUserId = inviteeUserId,
    status = status.name,
    createdAt = createdAt,
    respondedAt = respondedAt,
)

// ── Block (§14) ──────────────────────────────────────────────────────────────

fun BlockEntity.toDto(): SocialBlock = SocialBlock(
    id = id,
    userId = userId,
    kind = runCatching { BlockKind.valueOf(kind) }.getOrDefault(BlockKind.archer),
    targetId = targetId,
    targetName = targetName,
    mode = runCatching { BlockMode.valueOf(mode) }.getOrDefault(BlockMode.mute),
    createdAt = createdAt,
)

fun SocialBlock.toEntity(): BlockEntity = BlockEntity(
    id = id,
    userId = userId,
    kind = kind.name,
    targetId = targetId,
    targetName = targetName,
    mode = mode.name,
    createdAt = createdAt,
)

// ── Achievement (§15) ────────────────────────────────────────────────────────

fun AchievementEntity.toDto(): Achievement = Achievement(
    id = id,
    userId = userId,
    sharedSessionId = sharedSessionId,
    // An unparseable cached kind degrades to `unknown` — it must never
    // impersonate a real trophy (a wrong `score_pr` would show a fake PR).
    kind = runCatching { AchievementKind.valueOf(kind) }.getOrDefault(AchievementKind.unknown),
    label = label,
    value = value,
    sublabel = sublabel,
    createdAt = createdAt,
)

fun Achievement.toEntity(): AchievementEntity = AchievementEntity(
    id = id,
    userId = userId,
    sharedSessionId = sharedSessionId,
    kind = kind.name,
    label = label,
    value = value,
    sublabel = sublabel,
    createdAt = createdAt,
)
