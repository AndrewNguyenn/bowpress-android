package com.andrewnguyen.bowpress.core.data.social

import com.andrewnguyen.bowpress.core.data.export.ExportJobRepository
import com.andrewnguyen.bowpress.core.data.export.ExportJobScheduler
import com.andrewnguyen.bowpress.core.model.SessionLocation
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of an extras-bearing share. Retained as the carrier for the
 * partial-failure counters: [ExportJobWorker][com.andrewnguyen.bowpress.core.data.export.ExportJobWorker]
 * builds one to render the hint via [SocialSessionSharer.partialFailureHint],
 * and the core-data unit tests pin the message shape against it.
 *
 * Mirrors iOS `SessionViewModel.ShareOutcome`.
 */
data class ShareWithExtrasOutcome(
    val sharedSessionId: String,
    val descriptionSucceeded: Boolean,
    val photosUploaded: Int,
    val photosAttempted: Int,
) {
    val hasPartialFailure: Boolean
        get() = !descriptionSucceeded || photosUploaded < photosAttempted
}

/**
 * Enqueues the §15 Strava-style feed share as a **durable export job**.
 *
 * Phase B refactor: where this class used to run the share fan-out inline in a
 * fire-and-forget `viewModelScope` coroutine (lost on process death), it now
 * stages the payload + persists an [com.andrewnguyen.bowpress.core.model.ExportJob]
 * and schedules [ExportJobWorker][com.andrewnguyen.bowpress.core.data.export.ExportJobWorker],
 * which owns the visibility gate, the idempotent share POST, the description
 * PATCH, the photo uploads, and the partial-failure snackbar. WorkManager
 * carries the retry across app kills — the same durability iOS gets from
 * SwiftData rehydration.
 *
 * Call sites: `SessionViewModel.endSession` / `ThreeDCourseViewModel`
 * (feature-session).
 */
@Singleton
class SocialSessionSharer @Inject constructor(
    private val exportJobRepository: ExportJobRepository,
    private val exportJobScheduler: ExportJobScheduler,
) {

    /**
     * Publish a just-saved session (legacy no-extras path — no description or
     * photos). Self-gates on visibility inside the worker and never throws.
     */
    suspend fun shareCompletedSession(
        sessionId: String,
        score: Int,
        xCount: Int,
        arrowCount: Int,
        distance: String?,
        face: String?,
        title: String?,
        shotAt: Instant,
        location: SessionLocation? = null,
    ) {
        enqueue(
            sessionId = sessionId,
            score = score,
            xCount = xCount,
            arrowCount = arrowCount,
            distance = distance,
            face = face,
            title = title,
            shotAt = shotAt,
            location = location,
            description = "",
            photoData = emptyList(),
        )
    }

    /**
     * Extras-bearing variant for the finish sheet — carries the caption +
     * photo gallery. 3D-course callers pass pre-computed [score] / [xCount] /
     * [arrowCount] derived from the scoring system.
     */
    suspend fun shareWithExtras(
        sessionId: String,
        score: Int,
        xCount: Int,
        arrowCount: Int,
        distance: String?,
        face: String?,
        title: String?,
        shotAt: Instant,
        location: SessionLocation?,
        description: String,
        photoData: List<ByteArray>,
    ) {
        enqueue(
            sessionId = sessionId,
            score = score,
            xCount = xCount,
            arrowCount = arrowCount,
            distance = distance,
            face = face,
            title = title,
            shotAt = shotAt,
            location = location,
            description = description,
            photoData = photoData,
        )
    }

    private suspend fun enqueue(
        sessionId: String,
        score: Int,
        xCount: Int,
        arrowCount: Int,
        distance: String?,
        face: String?,
        title: String?,
        shotAt: Instant,
        location: SessionLocation?,
        description: String,
        photoData: List<ByteArray>,
    ) {
        val jobId = exportJobRepository.enqueue(
            sessionId = sessionId,
            shouldShare = true,
            description = description,
            title = title,
            location = location,
            photoData = photoData,
            score = score,
            xCount = xCount,
            arrowCount = arrowCount,
            distance = distance,
            face = face,
            shotAt = shotAt,
        )
        exportJobScheduler.schedule(jobId)
    }

    /**
     * Render the partial-failure hint string for [outcome] — null when the
     * post landed cleanly. Mirrors iOS `ShareOutcome.partialFailureMessage`.
     * Exposed @JvmStatic so the worker and the core-data unit tests share one
     * message-shape source.
     */
    companion object {
        @JvmStatic
        fun partialFailureHint(outcome: ShareWithExtrasOutcome): String? {
            if (!outcome.hasPartialFailure) return null
            val missingPhotos = outcome.photosAttempted - outcome.photosUploaded
            return when {
                !outcome.descriptionSucceeded && missingPhotos == 0 ->
                    "Posted, but your description didn't attach. Tap the post to add it."
                outcome.descriptionSucceeded && missingPhotos > 0 -> {
                    val label = if (missingPhotos == 1) "1 photo" else "$missingPhotos photos"
                    "Posted, but $label didn't upload. Tap the post to retry."
                }
                else -> {
                    val label = if (missingPhotos == 1) "1 photo" else "$missingPhotos photos"
                    "Posted, but your description and $label didn't attach. Tap the post to retry."
                }
            }
        }
    }
}
