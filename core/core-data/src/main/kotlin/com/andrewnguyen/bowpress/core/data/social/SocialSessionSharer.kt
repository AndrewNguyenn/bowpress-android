package com.andrewnguyen.bowpress.core.data.social

import android.util.Log
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

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

    private companion object {
        const val TAG = "SocialSessionSharer"
    }
}
