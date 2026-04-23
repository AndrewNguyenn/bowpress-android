package com.andrewnguyen.bowpress.core.network

import com.andrewnguyen.bowpress.core.model.Entitlement
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts non-2xx responses into [ApiException] subclasses. Mirrors the discrimination
 * iOS does in `APIClient.ensureSuccess` / `verifyEmailError`:
 *
 *   401  → [ApiException.Unauthorized]  (or [ApiException.InvalidVerificationCode])
 *   402  → [ApiException.SubscriptionRequired], parsed body attached
 *   403  → [ApiException.EmailNotVerified] when body.error == "email_not_verified"
 *   410  → [ApiException.VerificationCodeExpired] when body.error == "verification_expired"
 *   429  → [ApiException.RateLimited]
 *   else → [ApiException.Generic]
 *
 * The body is buffered so downstream Retrofit converters still see it unchanged.
 */
@Singleton
class ErrorInterceptor @Inject constructor(
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val peek = response.peekBody(MAX_PEEK_BYTES).string()
        val parsed = runCatching { json.decodeFromString(ErrorBody.serializer(), peek) }.getOrNull()

        val rebuilt = response.newBuilder()
            .body(peek.toResponseBody(response.body?.contentType()))
            .build()

        val err: ApiException = when (response.code) {
            401 -> when (parsed?.error) {
                "invalid_code" ->
                    ApiException.InvalidVerificationCode(parsed.attemptsRemaining ?: 0)
                else -> ApiException.Unauthorized(parsed?.error)
            }
            402 -> {
                val entitlement = runCatching {
                    json.decodeFromString(Entitlement.serializer(), peek)
                }.getOrNull()
                ApiException.SubscriptionRequired(entitlement, parsed?.error)
            }
            403 -> if (parsed?.error == "email_not_verified") {
                ApiException.EmailNotVerified(email = parsed.email, message = parsed.error)
            } else {
                ApiException.Generic(403, parsed?.error, peek)
            }
            410 -> if (parsed?.error == "verification_expired") {
                ApiException.VerificationCodeExpired()
            } else {
                ApiException.Generic(410, parsed?.error, peek)
            }
            429 -> ApiException.RateLimited(parsed?.error)
            else -> ApiException.Generic(response.code, parsed?.error, peek)
        }

        // Closing `response` and returning `rebuilt` preserves the body for Retrofit
        // when it later tries to surface the HttpException. OkHttp requires that we
        // close exactly one of the two (rebuilt is what travels downstream).
        response.close()
        // We throw rather than return so the Retrofit call-adapter surfaces the typed
        // exception directly to `suspend fun` callers. OkHttp interceptors are allowed
        // to throw IOException; ApiException extends RuntimeException, which propagates
        // through the call stack on a coroutine dispatcher as expected.
        throw err
    }

    private companion object {
        const val MAX_PEEK_BYTES = 1L * 1024 * 1024 // 1 MiB
    }
}
