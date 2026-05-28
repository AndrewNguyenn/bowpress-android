package com.andrewnguyen.bowpress.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.CourseStationDao
import com.andrewnguyen.bowpress.core.database.dao.ExportJobDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.database.dao.SightMarkDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import com.andrewnguyen.bowpress.core.database.entities.AchievementEntity
import com.andrewnguyen.bowpress.core.database.entities.ActivityItemEntity
import com.andrewnguyen.bowpress.core.database.entities.ArrowConfigEntity
import com.andrewnguyen.bowpress.core.database.entities.ArrowPlotEntity
import com.andrewnguyen.bowpress.core.database.entities.BlockEntity
import com.andrewnguyen.bowpress.core.database.entities.BowConfigEntity
import com.andrewnguyen.bowpress.core.database.entities.BowEntity
import com.andrewnguyen.bowpress.core.database.entities.ClubEntity
import com.andrewnguyen.bowpress.core.database.entities.CourseStationEntity
import com.andrewnguyen.bowpress.core.database.entities.ExportJobEntity
import com.andrewnguyen.bowpress.core.database.entities.FriendshipEntity
import com.andrewnguyen.bowpress.core.database.entities.InvitationEntity
import com.andrewnguyen.bowpress.core.database.entities.LeagueEntity
import com.andrewnguyen.bowpress.core.database.entities.SessionEndEntity
import com.andrewnguyen.bowpress.core.database.entities.SessionEntity
import com.andrewnguyen.bowpress.core.database.entities.SightMarkEntity
import com.andrewnguyen.bowpress.core.database.entities.SocialProfileEntity
import com.andrewnguyen.bowpress.core.database.entities.SuggestionEntity

/**
 * Root Room database — entities matching iOS `PersistentModels.swift` plus
 * the Social Layer tables added in version 7, the invitations table in
 * version 8, the blocks table in version 9, the §15 shared-session columns on
 * `activity_feed` in version 10, the achievements table in version 11, the
 * activity-feed routing-target columns in version 12, the
 * `sightPinDistance` column on `bow_configurations` in version 13, the
 * nullable `sharedSessionId` on `achievements` in version 14 (league and
 * club trophies are not earned from a shared session), the `course_stations`
 * table in version 15, the Social Feed V2 columns on `activity_feed`
 * (`titleIsCustom`, `isOwn`) in version 16, the Likes & Comments
 * columns on `activity_feed` (`subjectId`, `likeCount`, `likedByMe`,
 * `commentCount`) in version 17, and the Comment-threads kudos column on
 * `activity_feed` (`likersJson`) in version 18, and the parity wave
 * additions in version 19: avatar cache-buster columns on
 * `activity_feed` (`actorAvatarVersion`, `actorAvatarUrl`, parity E5) +
 * visibility / join-policy columns on `clubs` and `leagues`
 * (parity E8 — iOS #33/#34).
 *
 * `exportSchema = true` writes generated schema JSON to `core-database/schemas/`
 * so we have a migration history from day 1 (configured via `room.schemaLocation`
 * in the module's `build.gradle.kts`).
 */
@Database(
    entities = [
        BowEntity::class,
        BowConfigEntity::class,
        ArrowConfigEntity::class,
        SessionEntity::class,
        ArrowPlotEntity::class,
        SessionEndEntity::class,
        SuggestionEntity::class,
        SightMarkEntity::class,
        // Social Layer — added v7
        SocialProfileEntity::class,
        FriendshipEntity::class,
        ClubEntity::class,
        ActivityItemEntity::class,
        LeagueEntity::class,
        // Social invitations — added v8
        InvitationEntity::class,
        // Social mutes/blocks — added v9
        BlockEntity::class,
        // Social achievements — added v11
        AchievementEntity::class,
        // 3D Course stations — added v15
        CourseStationEntity::class,
        // Phase B — durable finish-time export + share jobs — added v20
        ExportJobEntity::class,
    ],
    version = 20,
    exportSchema = true,
    autoMigrations = [
        // 5→6: added sight_marks table. Pure additive.
        AutoMigration(from = 5, to = 6),
        // 6→7: added social layer tables (social_profiles, friendships, clubs,
        // activity_feed, leagues). Pure additive — Room derives all CREATE TABLE
        // statements from the new entity classes; no column changes elsewhere.
        AutoMigration(from = 6, to = 7),
        // 7→8: added the invitations table. Pure additive.
        AutoMigration(from = 7, to = 8),
        // 8→9: added the blocks table. Pure additive.
        AutoMigration(from = 8, to = 9),
        // 9→10: added §15 shared-session columns to activity_feed
        // (sessionJson, achievementsJson, highlighted). Pure additive —
        // all three have defaults.
        AutoMigration(from = 9, to = 10),
        // 10→11: added the achievements table. Pure additive.
        AutoMigration(from = 10, to = 11),
        // 11→12: added activity_feed routing-target columns (actorUserId,
        // clubId, leagueId). Pure additive — all defaulted.
        AutoMigration(from = 11, to = 12),
        // 12→13: added the sightPinDistance column to bow_configurations.
        // Pure additive — nullable, defaults to NULL.
        AutoMigration(from = 12, to = 13),
        // 13→14: achievements.sharedSessionId becomes nullable. Room recreates
        // the table; the achievements table is a server cache, so any rows
        // carried across are refreshed on the next fetch regardless.
        AutoMigration(from = 13, to = 14),
        // 14→15: added the course_stations table and the sessionType /
        // scoringSystem columns on sessions. Pure additive — the new column
        // is NOT NULL with a SQL DEFAULT of 'RANGE', scoringSystem is nullable.
        AutoMigration(from = 14, to = 15),
        // 15→16: added Social Feed V2 columns to activity_feed
        // (titleIsCustom, isOwn). Pure additive — both NOT NULL with a SQL
        // DEFAULT of 0.
        AutoMigration(from = 15, to = 16),
        // 16→17: added Likes & Comments columns to activity_feed
        // (subjectId, likeCount, likedByMe, commentCount). Pure additive —
        // subjectId is NOT NULL with a SQL DEFAULT of '', the three counters
        // are NOT NULL with a SQL DEFAULT of 0.
        AutoMigration(from = 16, to = 17),
        // 17→18: added the Comment-threads kudos column to activity_feed
        // (likersJson). Pure additive — nullable, defaults to NULL.
        AutoMigration(from = 17, to = 18),
        // 18→19: parity wave additions, all pure-additive so a single
        // AutoMigration carries them. Avatar cache-buster columns on
        // activity_feed (actorAvatarVersion, actorAvatarUrl) — both nullable
        // with defaults of NULL; an older cached row resolves to the
        // monogram until a fresh feed fetch backfills the version (parity E5).
        // visibility + joinPolicy columns on clubs + leagues — NOT NULL with
        // SQL DEFAULT 'PUBLIC' / 'OPEN' so legacy rows decode as the iOS
        // backward-compat default (parity E8 / iOS #33/#34).
        AutoMigration(from = 18, to = 19),
        // 19→20: added the export_jobs table (Phase B). Pure additive — Room
        // derives the CREATE TABLE from the new entity; no column changes
        // elsewhere.
        AutoMigration(from = 19, to = 20),
    ],
)
@TypeConverters(Converters::class)
abstract class BowPressDatabase : RoomDatabase() {
    abstract fun bowDao(): BowDao
    abstract fun bowConfigDao(): BowConfigDao
    abstract fun arrowConfigDao(): ArrowConfigDao
    abstract fun sessionDao(): SessionDao
    abstract fun arrowPlotDao(): ArrowPlotDao
    abstract fun sessionEndDao(): SessionEndDao
    abstract fun courseStationDao(): CourseStationDao
    abstract fun exportJobDao(): ExportJobDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun sightMarkDao(): SightMarkDao

    // Social Layer DAOs
    abstract fun socialProfileDao(): SocialProfileDao
    abstract fun friendshipDao(): FriendshipDao
    abstract fun clubDao(): ClubDao
    abstract fun activityFeedDao(): ActivityFeedDao
    abstract fun leagueDao(): LeagueDao
    abstract fun invitationDao(): InvitationDao
    abstract fun blockDao(): BlockDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val NAME = "bowpress.db"
    }
}
