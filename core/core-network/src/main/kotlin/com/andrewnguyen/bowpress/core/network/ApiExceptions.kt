package com.andrewnguyen.bowpress.core.network

import com.andrewnguyen.bowpress.core.model.Entitlement
import kotlinx.serialization.Serializable

/**
 * Parsed body for a backend error response. Every controller returns `{ "error": "..." }`
 * on failure; auth flows additionally echo back `email` / `attemptsRemaining`.
 */
@Serializable
data class ErrorBody(
    val error: String? = null,
    val email: String? = null,
    val attemptsRemaining: Int? = null,
)

/**
 * Typed exceptions surfaced by [ErrorInterceptor]. Network code throws these; feature
 * view-models catch and map to UI state. Mirrors the discrimination iOS does inline
 * in `APIClient.ensureSuccess` + `AuthError`.
 */
sealed class ApiException(
    val status: Int,
    message: String?,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /** 401 — token is missing, expired, or rejected. Caller should clear session. */
    class Unauthorized(message: String? = null) : ApiException(401, message ?: "Unauthorized")

    /**
     * 402 — active subscription required. [entitlement] is parsed from the body if present,
     * so paywall UIs can pre-fill the inactive/trial copy.
     */
    class SubscriptionRequired(
        val entitlement: Entitlement? = null,
        message: String? = null,
    ) : ApiException(402, message ?: "Subscription required")

    /** 403 with body `{ "error": "email_not_verified", "email": "..." }`. */
    class EmailNotVerified(
        val email: String? = null,
        message: String? = null,
    ) : ApiException(403, message ?: "Email not verified")

    /** 429 — rate-limited. */
    class RateLimited(message: String? = null) : ApiException(429, message ?: "Too many requests")

    /**
     * Verify-email dedicated errors. These mirror iOS `AuthError.invalidCode`/`codeExpired`/
     * `tooManyAttempts`. We expose the structured data rather than the English message.
     */
    class InvalidVerificationCode(val attemptsRemaining: Int) :
        ApiException(401, "Invalid code; $attemptsRemaining attempts remaining")

    class VerificationCodeExpired : ApiException(410, "Verification code expired")

    /** Generic fallback — unknown status or a non-JSON body. */
    class Generic(
        status: Int,
        val errorCode: String? = null,
        val body: String? = null,
        message: String? = null,
    ) : ApiException(status, message ?: "HTTP $status${errorCode?.let { ": $it" } ?: ""}")
}
