package com.andrewnguyen.bowpress.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises database migrations using [MigrationTestHelper].
 *
 * - [migrate_1_to_2_adds_targetFaceType_column_with_default]: Verifies that the
 *   v1→v2 migration adds `targetFaceType` with the `SIX_RING` default.
 * - [migrate_6_to_7_adds_social_tables]: Verifies that the v6→v7 AutoMigration
 *   adds all 5 social tables (`social_profiles`, `friendships`, `clubs`, `leagues`,
 *   `activity_feed`) without data loss to existing rows.
 * - [migrate_7_to_8_adds_invitations_table]: Verifies that the v7→v8 AutoMigration
 *   adds the `invitations` table without data loss.
 * - [migrate_8_to_9_adds_blocks_table]: Verifies that the v8→v9 AutoMigration
 *   adds the `blocks` table without data loss.
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
    fun migrate_6_to_7_adds_social_tables() {
        val dbName = "migration-6-7-test.db"

        // Create a v6 DB and insert a bow row to confirm data survives.
        helper.createDatabase(dbName, 6).apply {
            execSQL(
                """
                INSERT INTO bows (id, userId, name, bowType, brand, model, createdAt, pendingSync)
                VALUES ('bow-1', 'user-1', 'Test Bow', 'COMPOUND', 'Hoyt', 'Satori', 1700000000000, 0)
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 6→7 adds the 5 social tables — no manual Migration needed.
        val db = helper.runMigrationsAndValidate(dbName, 7, /* validateDroppedTables = */ true)

        // Existing data survives.
        db.query("SELECT id FROM bows WHERE id = 'bow-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
        }

        // All 5 social tables exist (INSERT without error confirms they're present).
        db.execSQL(
            """
            INSERT INTO social_profiles (userId, handle, displayName, joinedAt, visibility, pendingSync)
            VALUES ('u-1', 'test.handle', 'Test User', 1700000000000, 'friends', 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO clubs (id, name, inviteCode, createdAt, createdBy, memberCount, myRole, pendingSync)
            VALUES ('club-1', 'Test Club', 'CODE0001', 1700000000000, 'u-1', 1, 'host', 0)
            """.trimIndent(),
        )
        db.query("SELECT id FROM clubs WHERE id = 'club-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
        }

        db.close()
    }

    @Test
    fun migrate_7_to_8_adds_invitations_table() {
        val dbName = "migration-7-8-test.db"

        // Create a v7 DB and insert a friendship row to confirm data survives.
        helper.createDatabase(dbName, 7).apply {
            execSQL(
                """
                INSERT INTO friendships (
                    id, requesterId, addresseeId, status, source, createdAt,
                    otherUserId, otherHandle, otherDisplayName, pendingSync
                ) VALUES (
                    'fr-1', 'u-1', 'u-2', 'accepted', 'handle', 1700000000000,
                    'u-2', 'other.h', 'Other Archer', 0
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 7→8 adds the invitations table — pure additive.
        val db = helper.runMigrationsAndValidate(dbName, 8, /* validateDroppedTables = */ true)

        // Existing data survives.
        db.query("SELECT id FROM friendships WHERE id = 'fr-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
        }

        // The invitations table exists (INSERT without error confirms it).
        db.execSQL(
            """
            INSERT INTO invitations (
                id, kind, targetId, targetName, inviterUserId, inviterHandle,
                inviteeUserId, status, createdAt
            ) VALUES (
                'inv-1', 'club', 'club-1', 'Test Club', 'u-9', 'host.h',
                'u-1', 'pending', 1700000000000
            )
            """.trimIndent(),
        )
        db.query("SELECT COUNT(*) FROM invitations WHERE status = 'pending'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(1)
        }

        db.close()
    }

    @Test
    fun migrate_8_to_9_adds_blocks_table() {
        val dbName = "migration-8-9-test.db"

        // Create a v8 DB and insert an invitation row to confirm data survives.
        helper.createDatabase(dbName, 8).apply {
            execSQL(
                """
                INSERT INTO invitations (
                    id, kind, targetId, targetName, inviterUserId, inviterHandle,
                    inviteeUserId, status, createdAt
                ) VALUES (
                    'inv-1', 'club', 'club-1', 'Test Club', 'u-9', 'host.h',
                    'u-1', 'pending', 1700000000000
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 8→9 adds the blocks table — pure additive.
        val db = helper.runMigrationsAndValidate(dbName, 9, /* validateDroppedTables = */ true)

        // Existing data survives.
        db.query("SELECT id FROM invitations WHERE id = 'inv-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
        }

        // The blocks table exists (INSERT without error confirms it).
        db.execSQL(
            """
            INSERT INTO blocks (
                id, userId, kind, targetId, targetName, mode, createdAt
            ) VALUES (
                'blk-1', 'u-1', 'archer', 'u-2', 'rival.h', 'mute', 1700000000000
            )
            """.trimIndent(),
        )
        db.query("SELECT mode FROM blocks WHERE id = 'blk-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("mute")
        }

        db.close()
    }

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
