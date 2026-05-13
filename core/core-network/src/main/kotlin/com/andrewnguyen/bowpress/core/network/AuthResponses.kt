package com.andrewnguyen.bowpress.core.network

import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.core.model.User
import kotlinx.serialization.Serializable

/**
 * `{user, token}` — response body for every successful auth endpoint that returns an
 * authenticated session. Mirrors iOS `AuthSuccessBody`.
 */
@Serializable
data class AuthResponse(
    val user: User,
    val token: String,
)

/**
 * Response to `POST /auth/signup` — 202 Accepted. Signals the client to navigate to the
 * email-verification screen.
 */
@Serializable
data class SignUpResponse(
    val status: String,
    val email: String,
)

/** Request body helpers used by feature-auth. */
@Serializable
data class SignUpRequest(val name: String, val email: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class AppleSignInRequest(val identityToken: String)

@Serializable
data class GoogleSignInRequest(val idToken: String)

@Serializable
data class VerifyEmailRequest(val email: String, val code: String)

@Serializable
data class ResendVerificationRequest(val email: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class UpdateProfileRequest(val name: String)

@Serializable
data class DeleteAccountRequest(val password: String? = null)

/** Body for `PUT /sessions/:id` used to close a session. */
@Serializable
data class EndSessionRequest(
    @kotlinx.serialization.Serializable(with = com.andrewnguyen.bowpress.core.model.InstantSerializer::class)
    val endedAt: java.time.Instant,
    val notes: String,
)

/**
 * Partial `PUT /sessions/:id` body used to edit an already-ended session.
 * Only includes editable fields; endedAt is untouched so the server's
 * analytics-queue guard (triggered only on null → set) won't re-fire.
 */
@Serializable
data class UpdateSessionRequest(
    val notes: String,
    val feelTags: List<String>,
)

/** Body for `POST /subscription/verify` (Apple). */
@Serializable
data class VerifySubscriptionRequest(val jws: String)

/**
 * Body for `POST /subscription/verify-google`. The backend resolves
 * `purchaseToken` against the Play Developer API using the stored service
 * account, then maps the response into an [Entitlement]. While the backend
 * endpoint returns 501 (no Play Console account yet — see BLOCKERS.md #3),
 * the client still POSTs so the call lights up automatically once the
 * server side ships.
 */
@Serializable
data class VerifyGoogleSubscriptionRequest(
    val purchaseToken: String,
    val productId: String,
    val packageName: String,
)

/** Body for `POST /device-tokens`. */
@Serializable
data class RegisterDeviceTokenRequest(val token: String, val environment: String)

/**
 * Mirrors iOS `ApplyResult` — returned by `POST /bows/:bowId/suggestions/:id/apply`. The
 * caller persists `newConfig` locally and overwrites the old suggestion with `suggestion`.
 */
@Serializable
data class ApplyResult(
    val suggestion: AnalyticsSuggestion,
    val newConfig: BowConfiguration,
)

/** Body for `PATCH /bow-configurations/:id` (pin/unpin + score overrides). */
@Serializable
data class UpdateBowConfigRequest(
    val isReference: Boolean? = null,
    val referenceManuallyPinned: Boolean? = null,
    val avgArrowScore: Double? = null,
    val xPercentage: Double? = null,
)

/** Body for `PATCH /suggestions/:id/read` / `/dismiss` — no payload needed but present for parity. */
@Serializable
class EmptyBody
