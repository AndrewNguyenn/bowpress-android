package com.andrewnguyen.bowpress.core.database

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit-level sanity checks for the migration registry. The end-to-end migration is
 * exercised by `BowPressDatabaseMigrationTest` under `androidTest/`, which requires
 * SQLite and `MigrationTestHelper` to run.
 */
class MigrationsTest {

    @Test
    fun migration_1_to_2_has_correct_bounds() {
        val m = Migrations.MIGRATION_1_2
        assertThat(m.startVersion).isEqualTo(1)
        assertThat(m.endVersion).isEqualTo(2)
    }

    @Test
    fun all_migrations_chain_without_gaps() {
        val all = Migrations.ALL
        assertThat(all).isNotEmpty()

        val starts = all.map { it.startVersion }.toSet()
        val ends = all.map { it.endVersion }.toSet()
        assertThat(starts).contains(1)
        for (migration in all) {
            assertThat(migration.endVersion).isGreaterThan(migration.startVersion)
            // Every non-root migration must chain from a previous migration's end.
            if (migration.startVersion != 1) {
                assertThat(ends).contains(migration.startVersion)
            }
        }
    }
}
