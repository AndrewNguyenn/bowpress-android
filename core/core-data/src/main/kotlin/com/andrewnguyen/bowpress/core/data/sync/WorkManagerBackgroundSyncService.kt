package com.andrewnguyen.bowpress.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real [BackgroundSyncService] backed by WorkManager. Replaces the
 * `NoopBackgroundSyncService` that we ran with during bring-up.
 *
 * Mirrors iOS `BackgroundSyncService` — the iOS implementation uses
 * `NWPathMonitor` to kick the drain on connectivity restore; on Android we
 * defer that to WorkManager's [NetworkType.CONNECTED] constraint, which
 * handles the same "run only when we have a network" guarantee without
 * us managing the listener ourselves.
 *
 * Policy:
 *  - [enqueueSync] uses [ExistingWorkPolicy.KEEP] so repeated saves don't
 *    stack up duplicate runs; the next scheduled run already covers them.
 *  - [syncNow] uses [ExistingWorkPolicy.REPLACE] so explicit pull-to-refresh
 *    pre-empts any scheduled run with a fresh one.
 *  - [cancelAll] cancels by unique name; called on sign-out.
 */
@Singleton
class WorkManagerBackgroundSyncService @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : BackgroundSyncService {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(appContext)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun enqueueSync() {
        val request = OneTimeWorkRequestBuilder<BowPressSyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            BowPressSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun syncNow() {
        val request = OneTimeWorkRequestBuilder<BowPressSyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            BowPressSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(BowPressSyncWorker.UNIQUE_NAME)
    }
}
