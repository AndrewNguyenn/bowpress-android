package com.andrewnguyen.bowpress.core.data

import com.andrewnguyen.bowpress.core.data.sync.BackgroundSyncService
import com.andrewnguyen.bowpress.core.data.sync.NoopBackgroundSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring for repositories. Each `*Repository` already uses `@Inject constructor`,
 * so we only need to bind the one interface — [BackgroundSyncService] — here.
 *
 * Feature modules that want a real WorkManager-backed implementation should replace
 * this binding in their own module (Hilt's `@TestInstallIn` / `@UninstallModules`).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBackgroundSyncService(): BackgroundSyncService = NoopBackgroundSyncService()
}
