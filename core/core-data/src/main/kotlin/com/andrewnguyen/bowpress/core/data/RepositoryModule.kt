package com.andrewnguyen.bowpress.core.data

import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.data.sync.WorkManagerBackgroundSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring for repositories. Each `*Repository` already uses `@Inject constructor`,
 * so we only need to bind the one interface — [BackgroundSyncService] — here.
 *
 * The production binding is [WorkManagerBackgroundSyncService], which drains
 * every repository's `pendingSync` queue via a Hilt-assisted `CoroutineWorker`.
 * Tests that want a stub can swap this out with Hilt's `@TestInstallIn`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBackgroundSyncService(
        impl: WorkManagerBackgroundSyncService,
    ): BackgroundSyncService
}
