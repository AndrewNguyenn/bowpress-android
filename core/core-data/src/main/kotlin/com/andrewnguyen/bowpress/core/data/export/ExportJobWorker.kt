package com.andrewnguyen.bowpress.core.data.export

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.social.SocialSessionSharer
import com.andrewnguyen.bowpress.core.data.social.ShareWithExtrasOutcome
import com.andrewnguyen.bowpress.core.data.sync.AppSnackbarBus
import com.andrewnguyen.bowpress.core.model.ExportJob
import com.andrewnguyen.bowpress.core.model.ExportJobState
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * WorkManager-backed driver for one finish-time export job — the Android
 * counterpart to iOS `ExportJobDriver`. Runs the resumable, idempotent share
 * fan-out (share POST → description PATCH → photo uploads) for the job whose
 * id arrives in [KEY_JOB_ID].
 *
 * WorkManager gives the persistence iOS gets from SwiftData rehydration:
 * `Result.retry()` survives process death and re-runs `doWork` with backoff.
 * Idempotency mirrors iOS — `sharedSessionId` is recorded the moment the share
 * lands so a re-run skips the POST (and the server share is idempotent per
 * `sessionId` anyway), and `photosUploaded` is the resume watermark.
 *
 * Scope note: unlike iOS, the job does NOT run the `endSession` PUT — that's
 * already durable on Android via `SessionRepository`'s `pendingSync` +
 * `BowPressSyncWorker`. The job owns the *share* only. Video is out of scope
 * until the Android upload pipeline lands (task #23).
 */
@HiltWorker
class ExportJobWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ExportJobRepository,
    private val blobStore: ExportJobBlobStore,
    private val socialRepository: SocialRepository,
    private val snackbarBus: AppSnackbarBus,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        var job = repository.getById(jobId) ?: return Result.success() // gone — nothing to do
        if (job.state.isTerminal) return Result.success()

        // Stale-attempt guard — give up after the ceiling rather than looping a
        // permanently-rejecting payload. Blobs are left on disk for a possible
        // future manual retry; only success purges.
        if (job.attemptCount >= MAX_ATTEMPTS) {
            repository.save(job.copy(state = ExportJobState.Failed, lastError = job.lastError ?: "Gave up after $MAX_ATTEMPTS attempts."))
            Log.e(TAG, "job $jobId failed — exhausted $MAX_ATTEMPTS attempts")
            return Result.failure()
        }

        job = job.copy(attemptCount = job.attemptCount + 1, state = ExportJobState.Uploading)
        repository.save(job)

        // A private finish shouldn't have enqueued at all, but guard anyway.
        if (!job.shouldShare) {
            finish(job, partial = null)
            return Result.success()
        }

        // Respect the archer's privacy setting — the server is the authority
        // (403s `nobody`), so ask rather than trust a stale cached profile.
        val visibility = runCatching { socialRepository.getMyProfile().visibility }.getOrNull()
        if (visibility == SocialVisibility.nobody) {
            finish(job, partial = null)
            return Result.success()
        }

        // Content guard — parity with the old SocialSessionSharer: a 0-arrow,
        // no-photo public finish has nothing to share.
        if (job.arrowCount <= 0 && job.photoBlobPaths.isEmpty()) {
            finish(job, partial = null)
            return Result.success()
        }

        // 1) Share POST — only when we don't already have a SharedSession
        //    (resume skips it). A failure retries via WorkManager backoff; the
        //    attempt ceiling above bounds it.
        if (job.sharedSessionId == null) {
            val result = runCatching {
                socialRepository.shareSession(
                    ShareSessionBody(
                        sessionId = job.sessionId,
                        score = job.score,
                        xCount = job.xCount,
                        arrowCount = job.arrowCount,
                        distance = job.distance,
                        face = job.face,
                        title = job.title,
                        shotAt = job.shotAt,
                        location = job.location,
                    ),
                )
            }.getOrElse { e ->
                Log.w(TAG, "share POST failed for $jobId (attempt ${job.attemptCount})", e)
                repository.save(job.copy(lastError = e.message))
                return Result.retry()
            }
            job = job.copy(sharedSessionId = result.sharedSession.id, progress = 0.3)
            repository.save(job)
        }

        val sharedId = job.sharedSessionId ?: return Result.retry()

        // 2) Description PATCH — best-effort, skipped on blank. Passing null
        //    originals forces a description-only change (the share POST already
        //    carried title + location), so the PATCH is idempotent on resume.
        var descriptionFailed = false
        if (job.description.isNotBlank()) {
            descriptionFailed = runCatching {
                socialRepository.editSharedSession(
                    sharedSessionId = sharedId,
                    newTitle = null,
                    newDescription = job.description.trim(),
                    newLocation = null,
                    originalTitle = null,
                    originalDescription = null,
                    originalLocation = null,
                )
            }.onFailure { Log.w(TAG, "description PATCH failed for $jobId", it) }.isFailure
        }

        // 3) Photos — resume from the watermark, sequential so server-side
        //    order matches pick order. Best-effort single attempt per photo
        //    (matching the prior Android contract); a missed photo is a partial
        //    failure surfaced in the hint, not a job failure — the post is up.
        var photoGap = 0
        if (job.photosUploaded < job.photoBlobPaths.size) {
            for (index in job.photosUploaded until job.photoBlobPaths.size) {
                val bytes = blobStore.readPhoto(job.photoBlobPaths[index])
                if (bytes == null) {
                    photoGap += 1
                    continue
                }
                val ok = runCatching {
                    socialRepository.uploadSharedSessionPhoto(sharedId, bytes)
                }.onFailure { Log.w(TAG, "photo upload failed for $jobId", it) }.isSuccess
                if (ok) {
                    val next = job.copy(photosUploaded = job.photosUploaded + 1)
                    job = next.copy(progress = progressFor(next))
                    repository.save(job)
                } else {
                    photoGap += 1
                }
            }
        }

        // A new feed row exists server-side — pull it into the cache so the
        // Feed tab shows the just-shared session without a manual refresh.
        runCatching { socialRepository.refreshFeed() }

        val partial = SocialSessionSharer.partialFailureHint(
            ShareWithExtrasOutcome(
                sharedSessionId = sharedId,
                descriptionSucceeded = !descriptionFailed,
                photosUploaded = job.photoBlobPaths.size - photoGap,
                photosAttempted = job.photoBlobPaths.size,
            ),
        )
        finish(job, partial)
        return Result.success()
    }

    /**
     * Mark the job done, flash the ✓ chip briefly, then drop the row + blobs.
     * Emits the partial-failure hint to the app-wide snackbar bus (the
     * SessionViewModel that fired this is long gone), matching the old
     * SocialSessionSharer path.
     */
    private suspend fun finish(job: ExportJob, partial: String?) {
        repository.save(job.copy(state = ExportJobState.Ready, progress = 1.0))
        partial?.let { snackbarBus.emit(it) }
        // Let the feed chip show its ✓ for a beat before the row disappears.
        delay(READY_FLASH_MS)
        repository.remove(job.id)
    }

    private fun progressFor(job: ExportJob): Double {
        val total = job.photoBlobPaths.size
        if (total == 0) return if (job.sharedSessionId == null) 0.0 else 1.0
        return (0.3 + 0.7 * (job.photosUploaded.toDouble() / total)).coerceAtMost(1.0)
    }

    companion object {
        const val KEY_JOB_ID = "exportJobId"
        const val MAX_ATTEMPTS = 5
        private const val READY_FLASH_MS = 2_500L
        private const val TAG = "ExportJobWorker"
    }
}
