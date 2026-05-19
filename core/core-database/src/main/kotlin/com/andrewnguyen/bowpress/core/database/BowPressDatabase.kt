package com.andrewnguyen.bowpress.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andrewnguyen.bowpress.core.database.dao.ArrowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BowConfigDao
import com.andrewnguyen.bowpress.core.database.dao.BowDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.database.dao.SightMarkDao
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import com.andrewnguyen.bowpress.core.database.entities.ArrowConfigEntity
import com.andrewnguyen.bowpress.core.database.entities.ArrowPlotEntity
import com.andrewnguyen.bowpress.core.database.entities.BowConfigEntity
import com.andrewnguyen.bowpress.core.database.entities.BowEntity
import com.andrewnguyen.bowpress.core.database.entities.SessionEndEntity
import com.andrewnguyen.bowpress.core.database.entities.SessionEntity
import com.andrewnguyen.bowpress.core.database.entities.SightMarkEntity
import com.andrewnguyen.bowpress.core.database.entities.SuggestionEntity

/**
 * Root Room database — 7 entities matching iOS `PersistentModels.swift`.
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
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        // 5→6: added sight_marks table. Pure additive — Room derives the
        // CREATE TABLE from SightMarkEntity, no column changes elsewhere.
        AutoMigration(from = 5, to = 6),
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

    companion object {
        const val NAME = "bowpress.db"
    }
}
