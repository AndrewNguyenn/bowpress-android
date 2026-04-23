package com.andrewnguyen.bowpress.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Credential Manager + Google Identity that returns an ID
 * token string. Mirrors iOS `GIDSignIn.sharedInstance.signIn(...)` but exposes a
 * suspending Kotlin API.
 *
 * Interface so tests can inject a fake without touching a real `Activity`
 * context (Credential Manager needs an Activity to present the picker).
 */
interface GoogleSignInClient {
    /**
     * Launch the Google credential picker bound to [activityContext] and return a
     * Google ID token JWT, or `null` if the user cancelled.
     *
     * @throws GoogleSignInException on any other failure (no Google account,
     * corrupt token, Play Services missing, etc.).
     */
    suspend fun fetchIdToken(activityContext: Context): String?
}

/** Thrown when Credential Manager returns a non-cancellation failure. */
class GoogleSignInException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class CredentialManagerGoogleSignInClient @Inject constructor() : GoogleSignInClient {

    override suspend fun fetchIdToken(activityContext: Context): String? {
        val credentialManager = CredentialManager.create(activityContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(GoogleAuthConfig.SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = try {
            credentialManager.getCredential(activityContext, request)
        } catch (_: GetCredentialCancellationException) {
            // User dismissed the picker — treat as graceful no-op (matches iOS -5 cancel).
            return null
        } catch (e: GetCredentialException) {
            throw GoogleSignInException(e.message ?: "Credential Manager failed", e)
        }

        val credential = response.credential
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleSignInException("Unexpected credential type: ${credential.type}")
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw GoogleSignInException("Could not parse Google ID token", e)
        }
    }
}
