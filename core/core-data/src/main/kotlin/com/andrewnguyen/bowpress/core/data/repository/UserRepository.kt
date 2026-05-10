package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.model.AuthProvider
import com.andrewnguyen.bowpress.core.model.User
import com.andrewnguyen.bowpress.core.network.AuthResponse
import com.andrewnguyen.bowpress.core.network.AppleSignInRequest
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.ChangePasswordRequest
import com.andrewnguyen.bowpress.core.network.DeleteAccountRequest
import com.andrewnguyen.bowpress.core.network.GoogleSignInRequest
import com.andrewnguyen.bowpress.core.network.ResendVerificationRequest
import com.andrewnguyen.bowpress.core.network.SignInRequest
import com.andrewnguyen.bowpress.core.network.SignUpRequest
import com.andrewnguyen.bowpress.core.network.SignUpResponse
import com.andrewnguyen.bowpress.core.network.TokenStore
import com.andrewnguyen.bowpress.core.network.UpdateProfileRequest
import com.andrewnguyen.bowpress.core.network.VerifyEmailRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin repository over the `/auth` + `/me` endpoints. Mirrors iOS `AuthService`
 * responsibilities. The in-memory `currentUser` [StateFlow] is the authoritative
 * signed-in-user signal; feature modules observe it to gate navigation.
 *
 * We intentionally don't cache the User in Room — sessions are small and we'd
 * rather refresh on app start than serve stale name/email.
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: BowPressApi,
    private val tokenStore: TokenStore,
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isSignedIn: Boolean get() = tokenStore.getToken() != null

    suspend fun signUp(name: String, email: String, password: String): SignUpResponse {
        val response = api.signUp(SignUpRequest(name, email, password))
        return response.body() ?: error("Sign-up returned an empty body")
    }

    suspend fun signIn(email: String, password: String): User =
        applyAuth(api.signIn(SignInRequest(email, password)))

    suspend fun signInWithApple(identityToken: String): User =
        applyAuth(api.signInWithApple(AppleSignInRequest(identityToken)))

    suspend fun signInWithGoogle(idToken: String): User =
        applyAuth(api.signInWithGoogle(GoogleSignInRequest(idToken)))

    suspend fun verifyEmail(email: String, code: String): User =
        applyAuth(api.verifyEmail(VerifyEmailRequest(email, code)))

    suspend fun resendVerification(email: String) {
        api.resendVerification(ResendVerificationRequest(email))
    }

    suspend fun changePassword(current: String, new: String) {
        api.changePassword(ChangePasswordRequest(current, new))
    }

    suspend fun refreshProfile(): User {
        val user = api.getProfile()
        _currentUser.value = user
        return user
    }

    suspend fun updateProfile(name: String): User {
        val user = api.updateProfile(UpdateProfileRequest(name))
        _currentUser.value = user
        return user
    }

    suspend fun deleteAccount(password: String? = null) {
        if (password != null) {
            api.deleteAccountWithPassword(DeleteAccountRequest(password))
        } else {
            api.deleteAccount()
        }
        signOut()
    }

    /** Clears the persisted token + in-memory user. Safe to call even if not signed in. */
    fun signOut() {
        tokenStore.clear()
        _currentUser.value = null
    }

    /**
     * DEBUG-only short-circuit that pre-flips the app to the signed-in state
     * with a dev fixture user, matching iOS `AppState` DEBUG behavior
     * (`isAuthenticated = true`, pre-filled dev user). No-op if a token
     * already exists.
     *
     * Gated by `BuildConfig.DEBUG` in [AppStateViewModel]; production paths
     * never call this. Backend calls under this token will 401 — that's
     * fine, `hydrate()` swallows the failure via runCatching.
     */
    fun seedDevAuth() {
        if (tokenStore.getToken() != null) return
        tokenStore.setToken(DEV_DEBUG_TOKEN)
        _currentUser.value = User(
            id = "dev-user",
            email = "dev@bowpress.local",
            name = "Dev User",
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            emailVerified = true,
            authProvider = AuthProvider.EMAIL,
        )
    }

    private fun applyAuth(response: AuthResponse): User {
        tokenStore.setToken(response.token)
        _currentUser.value = response.user
        return response.user
    }

    private companion object {
        const val DEV_DEBUG_TOKEN = "dev-debug-token"
    }
}
