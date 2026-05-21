package com.andrewnguyen.bowpress.core.data.social

/**
 * Session ids known to carry a target-paper photo — drives the §18 feed
 * photo-preview variant (a shared session shows its photo over the
 * discipline preview).
 *
 * Android has no photo-capture feature yet (iOS issue #23). Today this is
 * only the DEBUG mock photo: `DevMockData` tags one mock shared session so
 * the activity feed can demonstrate the photo preview in the emulator,
 * matching the iOS DEBUG build (where `seedMockTargetPhotos()` does the same).
 * Real shared sessions use UUID session ids, so this never matches outside
 * the fixture data. When photo capture is ported this becomes a real lookup.
 */
object TargetPhotoCatalog {
    /** Mock shared-session ids that have a photo — kept in sync with `DevMockData`. */
    private val mockPhotoSessionIds = setOf("sess_devon_1")

    fun hasPhoto(sessionId: String): Boolean = sessionId in mockPhotoSessionIds
}
