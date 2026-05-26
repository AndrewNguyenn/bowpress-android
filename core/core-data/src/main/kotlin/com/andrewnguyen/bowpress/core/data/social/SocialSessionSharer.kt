package com.andrewnguyen.bowpress.core.data.social

import android.util.Log
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of an extras-bearing share call. The initial POST is required to
 * land a sharedSessionId; everything past that point (description PATCH,
 * photo uploads) is best-effort and surfaced through the success counters.
 * Mirrors iOS `SessionViewModel.ShareOutcome`. The Kotlin layer keeps this
 * in core-data to avoid feature-session having to crack the [SocialRepository]
 * itself.
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
 * Fire-and-forget publisher for the §15 Strava-style feed.
 *
 * When a shooting session is finalized, [shareCompletedSession] posts its
 * stats to `POST /social/sessions/share` so the session lands in friends'
 * feeds and earns any server-detected achievements.
 *
 * Two hard rules baked in here so call sites stay trivial:
 *  - **Visibility gate** — a `nobody`-visibility archer never shares (and the
 *    API would 403 anyway). Visibility is read best-effort from the cached
 *    profile.
 *  - **Never fails the save** — every failure is swallowed and logged. The
 *    session is already persisted locally by the time this runs; a share
 *    failure must not surface to the user.
 *
 * Call site: `SessionViewModel.endSession` (feature-session).
 */
@Singleton
class SocialSessionSharer @Inject constructor(
    private val socialRepository: SocialRepository,
    private val snackbarBus: com.andrewnguyen.bowpress.core.data.sync.AppSnackbarBus,
) {

    /**
     * Publish a just-saved session. Safe to call unconditionally — it
     * self-gates on visibility and never throws.
     *
     * @param sessionId the client `ShootingSession` id (share is idempotent per id)
     * @param score sum of scored rings
     * @param xCount number of X (inner-10) hits
     * @param arrowCount arrows shot
     * @param distance human label, e.g. "50m" / "20yd" — null if unset
     * @param face target-face label — null if unset
     * @param title optional user-supplied session name
     * @param shotAt when the session was shot
     * @param location §18 Instagram-style location tag — null if shared untagged
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
        runCatching {
            val visibility = socialRepository.getMyProfile().visibility
            if (visibility == SocialVisibility.nobody) {
                Log.d(TAG, "Skipping share — visibility is nobody")
                return
            }
            socialRepository.shareSession(
                ShareSessionBody(
                    sessionId = sessionId,
                    score = score,
                    xCount = xCount,
                    arrowCount = arrowCount,
                    distance = distance,
                    face = face,
                    title = title,
                    shotAt = shotAt,
                    location = location,
                ),
            )
            // A new feed row exists server-side now — pull it into the cache so
            // the Feed tab shows the just-shared session without a manual refresh.
            socialRepository.refreshFeed()
        }.onFailure { e ->
            // Never fails the save — the session is already persisted.
            Log.w(TAG, "Session share failed (non-fatal)", e)
        }
    }

    /**
     * Extras-bearing variant for the C1 finish sheet. Posts the share, then
     * (best-effort) PATCHes a non-empty description and uploads photos one
     * at a time so server-side gallery order matches the archer's pick
     * order. Returns null when the share itself fails / is skipped for
     * visibility — there is no SharedSession to attach extras to either way.
     *
     * 3D-course callers should provide pre-computed [score] / [xCount] /
     * [arrowCount] derived from the scoring system — the legacy
     * [shareCompletedSession] path is range-only.
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
    ): ShareWithExtrasOutcome? {
        if (arrowCount <= 0) return null

        // Respect the archer's privacy setting — `nobody` archers never
        // share. Best-effort; on transport failure we fall through to the
        // server-side check (which 403s `nobody` regardless).
        val visibilityCheck = runCatching {
            socialRepository.getMyProfile().visibility
        }.getOrNull()
        if (visibilityCheck == SocialVisibility.nobody) {
            Log.d(TAG, "Skipping share — visibility is nobody")
            return null
        }

        val shareResult = runCatching {
            socialRepository.shareSession(
                ShareSessionBody(
                    sessionId = sessionId,
                    score = score,
                    xCount = xCount,
                    arrowCount = arrowCount,
                    distance = distance,
                    face = face,
                    title = title,
                    shotAt = shotAt,
                    location = location,
                ),
            )
        }.onFailure { Log.w(TAG, "Initial share POST failed", it) }
            .getOrNull() ?: return null

        val sharedId = shareResult.sharedSession.id
        val originalTitle = shareResult.sharedSession.title
        val originalDescription = shareResult.sharedSession.description
        val originalLocation = shareResult.sharedSession.location

        // Description PATCH — only when the archer typed something. An empty
        // description counts as success (no-op step) so the partial-failure
        // hint only fires when a step actually ran and failed.
        val trimmedDescription = description.trim()
        val descriptionSucceeded = if (trimmedDescription.isEmpty()) {
            true
        } else {
            runCatching {
                socialRepository.editSharedSession(
                    sharedSessionId = sharedId,
                    newTitle = title,
                    newDescription = trimmedDescription,
                    newLocation = originalLocation,
                    originalTitle = originalTitle,
                    originalDescription = originalDescription,
                    originalLocation = originalLocation,
                )
            }.onFailure { Log.w(TAG, "Description PATCH failed", it) }.isSuccess
        }

        // Photo uploads — sequential so the server-side display order matches
        // the archer's pick order. A missed photo can't keep the rest from
        // landing; we count successes and report the gap.
        var photosUploaded = 0
        for (bytes in photoData) {
            val ok = runCatching {
                socialRepository.uploadSharedSessionPhoto(sharedId, bytes)
            }.onFailure { Log.w(TAG, "Photo upload failed", it) }.isSuccess
            if (ok) photosUploaded += 1
        }

        // A new feed row exists server-side now — pull it into the cache so
        // the Feed tab shows the just-shared session without a manual refresh.
        runCatching { socialRepository.refreshFeed() }

        val outcome = ShareWithExtrasOutcome(
            sharedSessionId = sharedId,
            descriptionSucceeded = descriptionSucceeded,
            photosUploaded = photosUploaded,
            photosAttempted = photoData.size,
        )
        // App-wide hint surface — the SessionViewModel that fired this share
        // may be torn down by the time the user lands on Log / Feed, so the
        // partial-failure message rides through the AppSnackbarBus to the
        // MainScaffold-level surface.
        partialFailureHint(outcome)?.let { snackbarBus.emit(it) }
        return outcome
    }

    /**
     * Render the partial-failure hint string for [outcome]. Mirrors iOS
     * `SessionViewModel.ShareOutcome.partialFailureMessage` — null when the
     * post landed cleanly. Lives here so the SessionViewModel layer can
     * compute the same string from its own outcome wrapper.
     */
    private fun partialFailureHint(outcome: ShareWithExtrasOutcome): String? {
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

    private companion object {
        const val TAG = "SocialSessionSharer"
    }
}
