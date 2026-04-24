package com.andrewnguyen.bowpress.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration history for [BowPressDatabase].
 *
 * Each migration is a one-way forward step and is registered via [BowPressDatabase.ALL_MIGRATIONS]
 * on the database builder. Schema JSON snapshots live in `core-database/schemas/` and are
 * the source of truth when writing the SQL here.
 */
internal object Migrations {

    /**
     * v1 -> v2: add `targetFaceType` to `sessions`. Legacy rows default to `SIX_RING`,
     * which matches the renderer's historical behaviour so stored ring values 6..X keep
     * their meaning.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE sessions ADD COLUMN targetFaceType TEXT NOT NULL DEFAULT 'SIX_RING'"
            )
        }
    }

    /**
     * v2 -> v3: add `distance` to `sessions`. Nullable — existing rows get NULL,
     * which the analytics filter naturally drops out of any specific-distance view.
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN distance TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
