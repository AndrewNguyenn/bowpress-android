package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.model.SessionLocation

/**
 * Audience pick from the finish sheet's segmented chip — mirrors iOS
 * `FinishAudience`.
 *
 * `Public` posts the session to the friend feed (the existing §15 share path).
 * `Private` keeps the session in the archer's log + analytics only; the
 * share path is short-circuited.
 */
enum class FinishAudience {
    Public,
    Private,
    ;

    val label: String get() = when (this) {
        Public -> "Public"
        Private -> "Private"
    }

    /** Sub-line under the chip label. */
    val detail: String get() = when (this) {
        Public -> "Shared to feed"
        Private -> "Only in your log"
    }

    /** Primary-action title that follows the audience pick. */
    val primaryTitle: String get() = when (this) {
        Public -> "Post to feed"
        Private -> "Finish"
    }

    /** Primary-action subtitle. */
    val primarySubtitle: String get() = when (this) {
        Public -> "save · sync · share publicly"
        Private -> "save · sync · keep private"
    }

    /**
     * True when the finish path should post to the social feed. `Private`
     * short-circuits — the session still lands in the archer's log and
     * analytics, nobody else sees it.
     */
    val shouldShare: Boolean get() = this == Public
}

/**
 * Caller-driven payload the [SessionViewModel] applies after the archer
 * commits the finish sheet. Mirrors iOS `FinishExtras` in
 * `Sources/BowPress/Session/FinishSheet.swift`.
 *
 *  - [title] replaces `session.title` verbatim (after trim).
 *  - [description] replaces `session.notes` and becomes the SharedSession
 *    caption (empty string clears the field).
 *  - [audience] gates the share: `Private` skips the share entirely.
 *  - [location] overrides the in-session location tag when non-null;
 *    null falls back to the NearestRangeFinder auto-tag.
 *  - [photoData] is the per-photo JPEG bytes (downscaled by
 *    `TargetPhotoStore.downscaledForUpload`). Skipped when audience ==
 *    Private (no SharedSession to attach them to). Empty array == no
 *    photos.
 */
data class FinishExtras(
    val title: String,
    val description: String,
    val audience: FinishAudience,
    val location: SessionLocation?,
    val photoData: List<ByteArray>,
) {
    /**
     * `equals` / `hashCode` for `data class` would compare ByteArray by
     * reference; override so tests can compare `FinishExtras` instances by
     * value (matches iOS's `Equatable` synthesis).
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FinishExtras) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (audience != other.audience) return false
        if (location != other.location) return false
        if (photoData.size != other.photoData.size) return false
        for (i in photoData.indices) {
            if (!photoData[i].contentEquals(other.photoData[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + audience.hashCode()
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + photoData.sumOf { it.contentHashCode() }
        return result
    }
}

// Note: a [ShareOutcome] data class used to live here as a parallel surface
// for the partial-failure message. It has been collapsed — [SocialSessionSharer]
// fans the same hint string straight to [AppSnackbarBus], which the
// MainScaffold's SnackbarHost consumes. One source of truth for the message
// shape, one delivery path. [ShareWithExtrasOutcome] in core-data carries the
// raw counters when callers need to assert (tests).
