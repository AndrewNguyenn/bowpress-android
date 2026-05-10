package com.andrewnguyen.bowpress.feature.auth

/**
 * Google Sign-In server client ID, passed to
 * `GetGoogleIdOption.Builder().setServerClientId(...)`.
 *
 * This is the **Web Application** OAuth 2.0 client minted in the
 * `bowpress-ios` Google Cloud project. Credential Manager mints ID
 * tokens whose audience equals this client ID; the bowpress-api Worker's
 * `GOOGLE_CLIENT_ID` secret must include this string in its
 * comma-separated allowlist alongside the iOS client ID so
 * `verifyIdToken` (authController.ts:443-450) accepts Android-minted
 * tokens.
 *
 * Note: the Android-typed OAuth client is auto-created by Firebase when
 * the debug + release SHA-1 fingerprints are registered — it gates
 * client integrity but its client ID is never sent in the sign-in
 * request, so it doesn't appear here.
 */
object GoogleAuthConfig {
    const val SERVER_CLIENT_ID: String =
        "516990179779-ktgfk5rv1taubsht419mlvv2clbh6qhc.apps.googleusercontent.com"
}
