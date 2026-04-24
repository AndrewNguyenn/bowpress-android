package com.andrewnguyen.bowpress.core.data.sync

/**
 * Contract for the background sync worker. Repositories hand off `pendingSync` writes
 * by calling `enqueueSync()`; the production implementation is
 * [WorkManagerBackgroundSyncService].
 *
 * Mirrors iOS `BackgroundSyncService`.
 */
interface BackgroundSyncService {
    /** Kick off (or coalesce) a sync run. Safe to call from any thread. */
    fun enqueueSync()

    /** Force immediate sync for a specific aggregate — used by explicit "pull to refresh". */
    suspend fun syncNow()

    /** Cancel any scheduled work. Called on logout. */
    fun cancelAll()
}

/**
 * No-op implementation retained for unit tests that don't want a real WorkManager.
 * Production wiring uses [WorkManagerBackgroundSyncService].
 */
class NoopBackgroundSyncService : BackgroundSyncService {
    override fun enqueueSync() = Unit
    override suspend fun syncNow() = Unit
    override fun cancelAll() = Unit
}
