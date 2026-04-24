package com.andrewnguyen.bowpress.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [Migrations.MIGRATION_1_2]: creates a v1 DB populated with a legacy `sessions`
 * row, runs the migration, and asserts that the new `targetFaceType` column exists on the
 * row with the default `SIX_RING` value (so pre-migration rows keep their historical
 * scoring meaning).
 *
 * Schema JSON snapshots under `core-database/schemas/` are exposed to this test as
 * assets via `sourceSets["androidTest"].assets.srcDirs` in `build.gradle.kts`.
 */
@RunWith(AndroidJUnit4::class)
class BowPressDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BowPressDatabase::class.java,
    )

    @Test
    fun migrate_1_to_2_adds_targetFaceType_column_with_default() {
        val dbName = "migration-test.db"

        // Create the DB at v1 and insert a legacy session row (no targetFaceType column).
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO sessions (
                    id, bowId, bowConfigId, arrowConfigId,
                    startedAt, endedAt,
                    notes, feelTags, arrowCount,
                    windSpeed, tempF, lighting,
                    pendingSync
                ) VALUES (
                    'sess-1', 'bow-1', 'cfg-1', 'arrow-1',
                    1700000000000, NULL,
                    '', '[]', 0,
                    NULL, NULL, NULL,
                    0
                )
                """.trimIndent()
            )
            close()
        }

        // Run the migration and validate the schema against the v2 snapshot.
        val db = helper.runMigrationsAndValidate(
            dbName,
            2,
            /* validateDroppedTables = */ true,
            Migrations.MIGRATION_1_2,
        )

        // The legacy row should now have the default SIX_RING face.
        db.query("SELECT targetFaceType FROM sessions WHERE id = 'sess-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("SIX_RING")
        }
        db.close()
    }
}
