package com.andrewnguyen.bowpress.core.data.export

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules [ExportJobWorker] runs — the Android equivalent of iOS kicking
 * `ExportJobDriver` (and `ExportJobStore.resumePendingJobs`).
 *
 * One unique work chain per job id (`export-job-{id}`) with
 * [ExistingWorkPolicy.KEEP] so a resume sweep racing the original enqueue
 * can't double-start a job. Exponential backoff on the worker's
 * `Result.retry()` rides WorkManager's scheduler, surviving process death;
 * the worker's own attempt ceiling is the hard stop.
 */
@Singleton
class ExportJobScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: ExportJobRepository,
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(appContext)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Enqueue (or no-op if already queued) the worker for one job. */
    fun schedule(jobId: String) {
        val request = OneTimeWorkRequestBuilder<ExportJobWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ExportJobWorker.KEY_JOB_ID to jobId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(10))
            .build()
        workManager.enqueueUniqueWork(uniqueName(jobId), ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Launch-time sweep: reclaim terminal rows + their staged blobs, then
     * re-schedule every still-active job.
     *
     * The terminal reclaim closes a leak — the only other removal path is the
     * worker's `finish()` (`Ready` → flash → `remove()`), which never runs for
     * a `Failed` job and is skipped if the process dies during the ✓-flash
     * window; without this, those rows + their cache blobs accumulate forever.
     *
     * Re-scheduling an already-queued job is a no-op under `KEEP`; this mainly
     * covers a job persisted to Room before WorkManager recorded its request
     * (process killed in the enqueue→schedule gap).
     */
    suspend fun resumePendingJobs() {
        val jobs = repository.getAll()
        jobs.filter { it.state.isTerminal }.forEach { repository.remove(it.id) }
        jobs.filter { it.state.isActive }.forEach { schedule(it.id) }
    }

    private fun uniqueName(jobId: String) = "export-job-$jobId"
}
