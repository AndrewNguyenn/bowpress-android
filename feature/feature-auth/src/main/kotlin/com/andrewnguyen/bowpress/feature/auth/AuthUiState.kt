package com.andrewnguyen.bowpress.feature.auth

import com.andrewnguyen.bowpress.core.model.User

/**
 * Single UI state for the whole auth flow. Each screen reads the slices it cares about.
 * Mirrors the state iOS juggles across `AuthView`, `EmailAuthView`, `VerifyEmailView`
 * — but collapsed into one object so a single [AuthViewModel] can drive all three
 * screens without propagating state through nav args.
 */
data class AuthUiState(
    val mode: Mode = Mode.SIGN_IN,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    /** Email the user typed most recently. Remembered so Verify screen can read it. */
    val email: String = "",
    /** Set when the server returns 403 `email_not_verified`; triggers navigation to verify. */
    val pendingVerificationEmail: String? = null,
    /** Set when a sign-up / sign-in / verify succeeds with a full user. */
    val signedInUser: User? = null,
    /** Attempts remaining after a bad verification code (if the server returned one). */
    val verificationAttemptsRemaining: Int? = null,
    /** Seconds left on the "Resend code" cooldown; 0 = can resend. */
    val resendCooldownSeconds: Int = 0,
) {
    enum class Mode { SIGN_IN, CREATE_ACCOUNT }
}

/**
 * Structured auth errors. Everything that comes through the UI is one of these —
 * `UiMessage` is the fallback for "something we didn't specifically model". Screens
 * decide presentation (banner text, whether to clear digits, whether to navigate).
 */
sealed class AuthError {
    object InvalidCredentials : AuthError()
    object RateLimited : AuthError()
    data class InvalidVerificationCode(val attemptsRemaining: Int) : AuthError()
    object VerificationCodeExpired : AuthError()
    data class UiMessage(val message: String) : AuthError()
}

/** One-shot events the ViewModel emits for navigation. */
sealed class AuthUiEvent {
    /** Sign-up succeeded — navigate to Verify with this email. */
    data class NavigateToVerify(val email: String) : AuthUiEvent()

    /** Full auth succeeded (sign-in / verify / Google) — app should route to main flow. */
    data class SignedIn(val user: User) : AuthUiEvent()
}
