package com.andrewnguyen.bowpress.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * WorkManager-backed drain of every repository's `pendingSync` queue.
 *
 * Order matches iOS `BackgroundSyncService.drain()`:
 *  1. bows
 *  2. bow-configs (depend on bows)
 *  3. arrow-configs (independent, but slotted here per iOS)
 *  4. sessions (depend on bows + bow-configs + arrow-configs)
 *  5. plots (depend on sessions — must carry full ArrowPlot shape)
 *  6. session-ends (end state per session; post after the session itself)
 *
 * Any individual repo's `flushPendingSync()` is internally fault-tolerant —
 * it wraps each POST in `runCatching` — so an IOException here only surfaces
 * when something catastrophic happens (e.g. DAO exception). We still return
 * [Result.retry] on `IOException` for parity with the iOS NWPathMonitor
 * retry-on-connectivity pattern.
 */
@HiltWorker
class BowPressSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
    private val arrowConfigRepository: ArrowConfigRepository,
    private val sessionRepository: SessionRepository,
    private val plotRepository: PlotRepository,
    private val sessionEndRepository: SessionEndRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        bowRepository.flushPendingSync()
        bowConfigRepository.flushPendingSync()
        arrowConfigRepository.flushPendingSync()
        sessionRepository.flushPendingSync()
        plotRepository.flushPendingSync()
        sessionEndRepository.flushPendingSync()
        Result.success()
    } catch (io: IOException) {
        Result.retry()
    } catch (t: Throwable) {
        // Don't hammer WorkManager on a programming bug — surface as failure.
        Result.failure()
    }

    companion object {
        const val UNIQUE_NAME = "bowpress-sync"
    }
}
