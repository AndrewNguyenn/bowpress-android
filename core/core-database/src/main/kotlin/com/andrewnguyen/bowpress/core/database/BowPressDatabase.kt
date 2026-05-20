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
 * activity-feed routing-target columns in version 12, and the
 * `sightPinDistance` column on `bow_configurations` in version 13.
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
    ],
    version = 13,
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
