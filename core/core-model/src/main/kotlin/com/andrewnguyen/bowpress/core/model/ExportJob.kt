package com.andrewnguyen.bowpress.core.model

import java.time.Instant

/**
 * Lifecycle of a finish-time export + share job. Drives both the
 * `ExportJobWorker` state machine and the optimistic feed-card chip.
 *
 * Mirrors iOS `ExportJobState`. `Pending` and `Uploading` both read as
 * "UPLOADING" on the chip; the split lets the worker distinguish a
 * never-started job from one it's actively pushing. `Transcoding` is the gap
 * between "bytes sent" and "Cloudflare Stream's webhook flipped the video to
 * ready" (reserved for the deferred Android video pipeline — see the
 * `videoBlobPath` note on [ExportJob]). `Ready` / `Failed` are terminal.
 */
enum class ExportJobState {
    Pending,
    Uploading,
    Transcoding,
    Ready,
    Failed,
    ;

    val isTerminal: Boolean get() = this == Ready || this == Failed
    val isActive: Boolean get() = !isTerminal
}

/**
 * Domain view of a persisted export job — the durable record of a session's
 * finish-time share fan-out (share POST + description PATCH + photo uploads),
 * made resumable across process death by Room + WorkManager.
 *
 * Mirrors iOS `ExportJob`. The Android job owns only the **share** fan-out:
 * the `endSession` PUT is already durable via `SessionRepository`'s
 * `pendingSync` + `BowPressSyncWorker` drain, so the job doesn't duplicate it.
 *
 * `sharedSessionId` is the idempotency hinge — recorded once the share POST
 * lands so a resumed run skips straight to the photo uploads instead of
 * creating a second SharedSession (the server share is idempotent per
 * `sessionId`, so even a racing re-POST returns the same row).
 */
data class ExportJob(
    val id: String,
    val sessionId: String,
    val sharedSessionId: String?,
    val shouldShare: Boolean,
    val description: String,
    val title: String?,
    val location: SessionLocation?,
    /** Absolute cache-dir paths to the queued JPEG blobs, in pick order. */
    val photoBlobPaths: List<String>,
    /**
     * Reserved for the deferred Android video pipeline (task #23). Always null
     * today — the Android finish sheet has no video slot yet, and the worker
     * skips video entirely. The column exists so the future uploader hangs off
     * the same durable state as iOS rather than bolting on a parallel store.
     */
    val videoBlobPath: String?,
    val state: ExportJobState,
    val progress: Double,
    val lastError: String?,
    val attemptCount: Int,
    val photosUploaded: Int,
    // Pre-computed share inputs.
    val score: Int,
    val xCount: Int,
    val arrowCount: Int,
    val distance: String?,
    val face: String?,
    val shotAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
