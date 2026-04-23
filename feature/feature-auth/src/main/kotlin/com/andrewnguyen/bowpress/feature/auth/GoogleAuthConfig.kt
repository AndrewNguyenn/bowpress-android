package com.andrewnguyen.bowpress.feature.auth

/**
 * Google Sign-In server client ID.
 *
 * **HEADS UP:** the value below is the iOS OAuth client ID. Android apps
 * authenticated via Credential Manager need their _own_ Android OAuth 2.0 client
 * registered in the Google Cloud Console (keyed by the app's package + SHA-1 of
 * the signing keypair).
 *
 * Once the Android client is created, two follow-ups are needed before the flow
 * works end-to-end:
 *   1. Replace [SERVER_CLIENT_ID] with the Android (or shared Web) OAuth client ID.
 *   2. Add that client ID to the `audience` allowlist in the bowpress-api Worker
 *      (see `verifyGoogleIdToken` in `bowpress-api/src/auth.ts`), so the server
 *      accepts ID tokens minted for Android.
 *
 * For now we pass the iOS client ID as a placeholder so the code compiles and
 * the UI renders — flagged in README.
 */
object GoogleAuthConfig {
    // TODO(auth): replace with Android OAuth client ID once registered.
    const val SERVER_CLIENT_ID: String =
        "516990179779-05k066j5guhgc0021jsbl9pvj8bb285m.apps.googleusercontent.com"
}
