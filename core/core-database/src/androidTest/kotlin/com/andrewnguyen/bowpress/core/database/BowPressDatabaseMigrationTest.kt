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
 * - [migrate_9_to_11_adds_shared_session_columns_and_achievements]: Verifies the
 *   v9→v10 (§15 activity_feed columns) and v10→v11 (achievements table)
 *   AutoMigrations chain without data loss.
 * - [migrate_11_to_12_adds_routing_and_layout_columns]: Verifies the v11→v12
 *   AutoMigration adds activity_feed routing columns + sessions.targetLayout.
 * - [migrate_12_to_13_adds_sight_pin_distance_column]: Verifies the v12→v13
 *   AutoMigration adds the nullable `sightPinDistance` column to
 *   `bow_configurations` without data loss.
 * - [migrate_16_to_17_adds_likes_and_comments_columns]: Verifies the v16→v17
 *   AutoMigration adds the Likes & Comments columns (`subjectId`,
 *   `likeCount`, `likedByMe`, `commentCount`) to `activity_feed` without
 *   data loss.
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
    fun migrate_9_to_11_adds_shared_session_columns_and_achievements() {
        val dbName = "migration-9-11-test.db"

        // Create a v9 DB and insert an activity_feed row to confirm data survives
        // the §15 column additions.
        helper.createDatabase(dbName, 9).apply {
            execSQL(
                """
                INSERT INTO activity_feed (
                    id, kind, sourceKind, actorHandle, actorDisplayName,
                    title, createdAt
                ) VALUES (
                    'act-1', 'friend_pr', 'friend', 'sara.l', 'Sara Lin',
                    'Hit a new PR', 1700000000000
                )
                """.trimIndent(),
            )
            close()
        }

        // Chains 9→10 (activity_feed columns) and 10→11 (achievements table).
        val db = helper.runMigrationsAndValidate(dbName, 11, /* validateDroppedTables = */ true)

        // Existing row survives, with the §15 column defaults applied.
        db.query("SELECT highlighted, sessionJson FROM activity_feed WHERE id = 'act-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(0)   // highlighted defaults to 0
            assertThat(cursor.isNull(1)).isTrue()       // sessionJson defaults to null
        }

        // A §15 highlighted row can be written.
        db.execSQL(
            """
            INSERT INTO activity_feed (
                id, kind, sourceKind, actorHandle, actorDisplayName,
                title, createdAt, sessionJson, achievementsJson, highlighted
            ) VALUES (
                'act-2', 'friend_pr', 'friend', 'devon.c', 'Devon Chen',
                'Shared a session', 1700000000000, '{}', '[]', 1
            )
            """.trimIndent(),
        )

        // The achievements table exists (INSERT without error confirms it).
        db.execSQL(
            """
            INSERT INTO achievements (
                id, userId, sharedSessionId, kind, label, value, createdAt
            ) VALUES (
                'ach-1', 'u-1', 'ss-1', 'score_pr', 'Score PR', 558, 1700000000000
            )
            """.trimIndent(),
        )
        db.query("SELECT value FROM achievements WHERE id = 'ach-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(558)
        }

        db.close()
    }

    @Test
    fun migrate_11_to_12_adds_routing_and_layout_columns() {
        val dbName = "migration-11-12-test.db"

        // Create a v11 DB with an activity_feed row and a sessions row, to
        // confirm both survive the v12 column additions.
        helper.createDatabase(dbName, 11).apply {
            execSQL(
                """
                INSERT INTO activity_feed (
                    id, kind, sourceKind, actorHandle, actorDisplayName,
                    title, createdAt, highlighted
                ) VALUES (
                    'act-1', 'friend_pr', 'friend', 'sara.l', 'Sara Lin',
                    'Hit a new PR', 1700000000000, 0
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, bowId, bowConfigId, arrowConfigId, startedAt,
                    notes, feelTags, arrowCount, targetFaceType, pendingSync
                ) VALUES (
                    'sess-1', 'bow-1', 'cfg-1', 'arrow-1', 1700000000000,
                    '', '[]', 0, 'TEN_RING', 0
                )
                """.trimIndent(),
            )
            close()
        }

        // 11→12 adds activity_feed routing columns + sessions.targetLayout.
        val db = helper.runMigrationsAndValidate(dbName, 12, /* validateDroppedTables = */ true)

        // The legacy activity_feed row gets the routing-column defaults.
        db.query("SELECT actorUserId, clubId, leagueId FROM activity_feed WHERE id = 'act-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("")   // actorUserId defaults to ""
            assertThat(cursor.isNull(1)).isTrue()           // clubId defaults to null
            assertThat(cursor.isNull(2)).isTrue()           // leagueId defaults to null
        }
        // The legacy sessions row gets the SINGLE layout default.
        db.query("SELECT targetLayout FROM sessions WHERE id = 'sess-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("SINGLE")
        }

        // A routed feed row + a 3-spot session can be written.
        db.execSQL(
            """
            INSERT INTO activity_feed (
                id, kind, sourceKind, actorHandle, actorDisplayName,
                title, createdAt, highlighted, actorUserId, clubId, leagueId
            ) VALUES (
                'act-2', 'club_session', 'club', 'marcus.t', 'Marcus T',
                'Club session', 1700000000000, 0, 'u-9', 'club-7', NULL
            )
            """.trimIndent(),
        )
        db.query("SELECT clubId FROM activity_feed WHERE id = 'act-2'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("club-7")
        }

        db.close()
    }

    @Test
    fun migrate_12_to_13_adds_sight_pin_distance_column() {
        val dbName = "migration-12-13-test.db"

        // Create a v12 DB with a bow_configurations row to confirm it survives
        // the v13 column addition.
        helper.createDatabase(dbName, 12).apply {
            execSQL(
                """
                INSERT INTO bow_configurations (
                    id, bowId, createdAt, label, drawLength,
                    restVertical, restHorizontal, restDepth,
                    sightPosition, gripAngle, nockingHeight,
                    isReference, referenceManuallyPinned, scoreable, pendingSync
                ) VALUES (
                    'cfg-1', 'bow-1', 1700000000000, 'Initial', 28.5,
                    0, 0, 0.0,
                    2, 0.0, 0,
                    0, 0, 0, 0
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 12→13 adds the nullable sightPinDistance column.
        val db = helper.runMigrationsAndValidate(dbName, 13, /* validateDroppedTables = */ true)

        // The legacy row survives, with sightPinDistance defaulting to null.
        db.query("SELECT sightPinDistance FROM bow_configurations WHERE id = 'cfg-1'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.isNull(0)).isTrue()
        }

        // A row carrying a pin distance can be written.
        db.execSQL(
            """
            INSERT INTO bow_configurations (
                id, bowId, createdAt, label, drawLength,
                restVertical, restHorizontal, restDepth,
                sightPosition, sightPinDistance, gripAngle, nockingHeight,
                isReference, referenceManuallyPinned, scoreable, pendingSync
            ) VALUES (
                'cfg-2', 'bow-1', 1700000000000, 'With pin distance', 28.5,
                0, 0, 0.0,
                2, 6.5, 0.0, 0,
                0, 0, 0, 0
            )
            """.trimIndent(),
        )
        db.query("SELECT sightPinDistance FROM bow_configurations WHERE id = 'cfg-2'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getDouble(0)).isEqualTo(6.5)
        }

        db.close()
    }

    @Test
    fun migrate_15_to_16_adds_social_feed_v2_columns() {
        val dbName = "migration-15-16-test.db"

        // Create a v15 DB with a pre-V2 activity_feed row to confirm it
        // survives the Social Feed V2 column additions.
        helper.createDatabase(dbName, 15).apply {
            execSQL(
                """
                INSERT INTO activity_feed (
                    id, kind, sourceKind, actorHandle, actorDisplayName,
                    title, createdAt
                ) VALUES (
                    'act-1', 'friend_session', 'friend', 'sara.l', 'Sara Lin',
                    'logged a session', 1700000000000
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 15→16 adds titleIsCustom / isOwn — pure additive, both
        // NOT NULL with a SQL DEFAULT of 0. (The photo gallery is not a
        // separate column — it lives inside the serialized sessionJson blob.)
        val db = helper.runMigrationsAndValidate(dbName, 16, /* validateDroppedTables = */ true)

        // The pre-V2 row survives and the new NOT NULL columns take their
        // SQL defaults.
        db.query(
            "SELECT titleIsCustom, isOwn FROM activity_feed WHERE id = 'act-1'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(0)
            assertThat(cursor.getInt(1)).isEqualTo(0)
        }

        // A V2 row with the new columns set inserts cleanly.
        db.execSQL(
            """
            INSERT INTO activity_feed (
                id, kind, sourceKind, actorHandle, actorDisplayName,
                title, createdAt, titleIsCustom, isOwn
            ) VALUES (
                'act-2', 'friend_session', 'friend', 'andrew.n', 'Andrew N',
                'Saturday 70m practice', 1700000000001, 1, 1
            )
            """.trimIndent(),
        )
        db.query(
            "SELECT titleIsCustom, isOwn FROM activity_feed WHERE id = 'act-2'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(1)
            assertThat(cursor.getInt(1)).isEqualTo(1)
        }

        db.close()
    }

    @Test
    fun migrate_16_to_17_adds_likes_and_comments_columns() {
        val dbName = "migration-16-17-test.db"

        // Create a v16 DB with a pre-§5 activity_feed row to confirm it
        // survives the Likes & Comments column additions.
        helper.createDatabase(dbName, 16).apply {
            execSQL(
                """
                INSERT INTO activity_feed (
                    id, kind, sourceKind, actorHandle, actorDisplayName,
                    title, createdAt, titleIsCustom, isOwn
                ) VALUES (
                    'act-1', 'friend_session', 'friend', 'sara.l', 'Sara Lin',
                    'logged a session', 1700000000000, 0, 0
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 16→17 adds subjectId / likeCount / likedByMe /
        // commentCount — pure additive. subjectId is NOT NULL DEFAULT '',
        // the three counters NOT NULL DEFAULT 0.
        val db = helper.runMigrationsAndValidate(dbName, 17, /* validateDroppedTables = */ true)

        // The pre-§5 row survives and the new NOT NULL columns take their
        // SQL defaults.
        db.query(
            "SELECT subjectId, likeCount, likedByMe, commentCount FROM activity_feed WHERE id = 'act-1'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("")
            assertThat(cursor.getInt(1)).isEqualTo(0)
            assertThat(cursor.getInt(2)).isEqualTo(0)
            assertThat(cursor.getInt(3)).isEqualTo(0)
        }

        // A §5 row with the new columns set inserts cleanly.
        db.execSQL(
            """
            INSERT INTO activity_feed (
                id, kind, sourceKind, actorHandle, actorDisplayName,
                title, createdAt, titleIsCustom, isOwn,
                subjectId, likeCount, likedByMe, commentCount
            ) VALUES (
                'act-2', 'friend_session', 'friend', 'andrew.n', 'Andrew N',
                'Saturday 70m practice', 1700000000001, 1, 1,
                'ss-9', 4, 1, 2
            )
            """.trimIndent(),
        )
        db.query(
            "SELECT subjectId, likeCount, likedByMe, commentCount FROM activity_feed WHERE id = 'act-2'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("ss-9")
            assertThat(cursor.getInt(1)).isEqualTo(4)
            assertThat(cursor.getInt(2)).isEqualTo(1)
            assertThat(cursor.getInt(3)).isEqualTo(2)
        }

        db.close()
    }

    @Test
    fun migrate_17_to_18_adds_likers_column() {
        val dbName = "migration-17-18-test.db"

        // Create a v17 DB with a pre-§6 activity_feed row to confirm it
        // survives the Comment-threads kudos column addition.
        helper.createDatabase(dbName, 17).apply {
            execSQL(
                """
                INSERT INTO activity_feed (
                    id, kind, sourceKind, actorHandle, actorDisplayName,
                    title, createdAt, titleIsCustom, isOwn,
                    subjectId, likeCount, likedByMe, commentCount
                ) VALUES (
                    'act-1', 'friend_session', 'friend', 'sara.l', 'Sara Lin',
                    'logged a session', 1700000000000, 0, 0,
                    'ss-1', 0, 0, 0
                )
                """.trimIndent(),
            )
            close()
        }

        // AutoMigration 17→18 adds likersJson — pure additive, nullable.
        val db = helper.runMigrationsAndValidate(dbName, 18, /* validateDroppedTables = */ true)

        // The pre-§6 row survives and the new nullable column defaults to NULL.
        db.query(
            "SELECT likersJson FROM activity_feed WHERE id = 'act-1'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.isNull(0)).isTrue()
        }

        // A §6 row carrying a kudos stack inserts cleanly.
        db.execSQL(
            """
            INSERT INTO activity_feed (
                id, kind, sourceKind, actorHandle, actorDisplayName,
                title, createdAt, titleIsCustom, isOwn,
                subjectId, likeCount, likedByMe, commentCount, likersJson
            ) VALUES (
                'act-2', 'friend_session', 'friend', 'andrew.n', 'Andrew N',
                'Saturday 70m practice', 1700000000001, 1, 1,
                'ss-9', 4, 1, 2,
                '[{"userId":"u-1","handle":"marcus.t","displayName":"Marcus T."}]'
            )
            """.trimIndent(),
        )
        db.query(
            "SELECT likersJson FROM activity_feed WHERE id = 'act-2'",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).contains("marcus.t")
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
