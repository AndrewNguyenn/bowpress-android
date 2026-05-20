package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

// ── Social Profile ─────────────────────────────────────────────────────────

@Entity(tableName = "social_profiles")
data class SocialProfileEntity(
    @PrimaryKey val userId: String,
    val handle: String,
    val displayName: String,
    val joinedAt: Instant,
    val visibility: String,          // SocialVisibility.name
    val bowSummary: String? = null,
    val sessionCount: Int = 0,
    val arrowCount: Int = 0,
    val division: String? = null,    // Division.name or null
    // reserved; social writes are online-first in v1
    val pendingSync: Boolean = false,
)

// ── Friendship ─────────────────────────────────────────────────────────────

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String,              // FriendshipStatus.name
    val source: String,              // FriendshipSource.name
    val createdAt: Instant,
    val respondedAt: Instant? = null,
    val otherUserId: String,
    val otherHandle: String,
    val otherDisplayName: String,
    val direction: String? = null,   // FriendshipDirection.name or null
    // reserved; social writes are online-first in v1
    val pendingSync: Boolean = false,
)

// ── Club ───────────────────────────────────────────────────────────────────

@Entity(tableName = "clubs")
data class ClubEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val notes: String? = null,
    val inviteCode: String,
    val createdAt: Instant,
    val createdBy: String,
    val memberCount: Int = 0,
    val myRole: String,              // ClubRole.name
    // reserved; social writes are online-first in v1
    val pendingSync: Boolean = false,
)

// ── Activity Feed ──────────────────────────────────────────────────────────

@Entity(tableName = "activity_feed")
data class ActivityItemEntity(
    @PrimaryKey val id: String,
    val kind: String,                // ActivityKind.name
    val sourceKind: String,          // ActivitySourceKind.name
    val actorHandle: String,
    val actorDisplayName: String,
    val title: String,
    val meta: String? = null,
    val stamp: String? = null,
    val createdAt: Instant,
    // §15 shared-session fields — JSON-encoded; nullable / defaulted so the
    // v9→v10 AutoMigration is purely additive. `highlighted` needs an explicit
    // SQL default for the NOT NULL column the migration adds.
    val sessionJson: String? = null,        // JSON ActivitySession or null
    val achievementsJson: String? = null,   // JSON List<AchievementBadge> or null
    @ColumnInfo(defaultValue = "0")
    val highlighted: Boolean = false,
    // Routing-target columns — added v12. `actorUserId` is NOT NULL so it
    // needs an explicit SQL default for the AutoMigration; club/league are
    // nullable. All purely additive.
    @ColumnInfo(defaultValue = "")
    val actorUserId: String = "",
    val clubId: String? = null,
    val leagueId: String? = null,
)

// ── League ─────────────────────────────────────────────────────────────────

@Entity(tableName = "leagues")
data class LeagueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val hostClubId: String? = null,
    val hostUserId: String,
    val leagueType: String,          // LeagueType.name
    val divisions: String,           // JSON list of Division.name
    val teamJson: String? = null,    // JSON TeamConfig or null
    val roundJson: String,           // JSON RoundDef
    val scheduleJson: String,        // JSON LeagueSchedule
    val handicapJson: String,        // JSON HandicapConfig
    val entryRule: String,           // LeagueEntryRule.name
    val inviteCode: String,
    val status: String,              // LeagueStatus.name
    val createdAt: Instant,
    val myEntryJson: String? = null, // JSON LeagueEntry or null
    val entryCount: Int = 0,
    // reserved; social writes are online-first in v1
    val pendingSync: Boolean = false,
)

// ── Invitation (§11) ─────────────────────────────────────────────────────────

@Entity(tableName = "invitations")
data class InvitationEntity(
    @PrimaryKey val id: String,
    val kind: String,                // InvitationKind.name
    val targetId: String,
    val targetName: String,
    val inviterUserId: String,
    val inviterHandle: String,
    val inviteeUserId: String,
    val status: String,              // InvitationStatus.name
    val createdAt: Instant,
    val respondedAt: Instant? = null,
)

// ── Block (§14) ──────────────────────────────────────────────────────────────

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val kind: String,                // BlockKind.name
    val targetId: String,
    val targetName: String,
    val mode: String,                // BlockMode.name
    val createdAt: Instant,
)

// ── Achievement (§15) ────────────────────────────────────────────────────────

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sharedSessionId: String?,    // null for league / club trophies
    val kind: String,                // AchievementKind.name
    val label: String,
    val value: Int,
    val sublabel: String? = null,
    val createdAt: Instant,
)
