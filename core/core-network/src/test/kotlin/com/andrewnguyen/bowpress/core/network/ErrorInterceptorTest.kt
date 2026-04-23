package com.andrewnguyen.bowpress.core.network

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Verifies the non-2xx → typed-ApiException mapping. No real network here — we feed
 * synthetic [Response] objects through a fake [Interceptor.Chain].
 */
class ErrorInterceptorTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val interceptor = ErrorInterceptor(json)

    @Test
    fun `401 with invalid_code body maps to InvalidVerificationCode`() {
        val ex = runAndCatch(
            code = 401,
            body = """{"error":"invalid_code","attemptsRemaining":2}""",
        )
        assertThat(ex).isInstanceOf(ApiException.InvalidVerificationCode::class.java)
        assertThat((ex as ApiException.InvalidVerificationCode).attemptsRemaining).isEqualTo(2)
    }

    @Test
    fun `401 plain maps to Unauthorized`() {
        val ex = runAndCatch(401, """{"error":"token_expired"}""")
        assertThat(ex).isInstanceOf(ApiException.Unauthorized::class.java)
    }

    @Test
    fun `402 with entitlement body carries parsed Entitlement`() {
        val ex = runAndCatch(
            code = 402,
            body = """{"isActive":false,"inTrial":false,"autoRenew":false}""",
        )
        assertThat(ex).isInstanceOf(ApiException.SubscriptionRequired::class.java)
        assertThat((ex as ApiException.SubscriptionRequired).entitlement?.isActive).isFalse()
    }

    @Test
    fun `403 with email_not_verified echoes email`() {
        val ex = runAndCatch(
            code = 403,
            body = """{"error":"email_not_verified","email":"a@b.com"}""",
        )
        assertThat(ex).isInstanceOf(ApiException.EmailNotVerified::class.java)
        assertThat((ex as ApiException.EmailNotVerified).email).isEqualTo("a@b.com")
    }

    @Test
    fun `410 with verification_expired maps cleanly`() {
        val ex = runAndCatch(410, """{"error":"verification_expired"}""")
        assertThat(ex).isInstanceOf(ApiException.VerificationCodeExpired::class.java)
    }

    @Test
    fun `429 maps to RateLimited`() {
        val ex = runAndCatch(429, """{"error":"Too many requests"}""")
        assertThat(ex).isInstanceOf(ApiException.RateLimited::class.java)
    }

    @Test
    fun `500 maps to Generic and preserves status`() {
        val ex = runAndCatch(500, """{"error":"boom"}""")
        assertThat(ex).isInstanceOf(ApiException.Generic::class.java)
        assertThat((ex as ApiException.Generic).status).isEqualTo(500)
        assertThat(ex.errorCode).isEqualTo("boom")
    }

    @Test
    fun `200 passes through untouched`() {
        val chain = FakeChain(buildResponse(200, """{"ok":true}"""))
        val resp = interceptor.intercept(chain)
        assertThat(resp.isSuccessful).isTrue()
    }

    // ---- helpers --------------------------------------------------------------

    private fun runAndCatch(code: Int, body: String): Throwable {
        val chain = FakeChain(buildResponse(code, body))
        return assertThrows(ApiException::class.java) {
            interceptor.intercept(chain)
        }
    }

    private fun buildResponse(code: Int, bodyJson: String): Response {
        val req = Request.Builder().url("https://example.test/x").build()
        return Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("http")
            .body(bodyJson.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private class FakeChain(private val response: Response) : Interceptor.Chain {
        override fun call() = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun connection() = null
        override fun proceed(request: Request): Response = response
        override fun readTimeoutMillis() = 0
        override fun request(): Request = response.request
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun writeTimeoutMillis() = 0
    }
}
