package com.andrewnguyen.bowpress.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches `Authorization: Bearer $token` when the [TokenStore] holds a non-empty value.
 * Requests that already set the header (e.g. login flows) are left untouched.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(HEADER_AUTH) != null) return chain.proceed(original)

        val token = tokenStore.getToken() ?: return chain.proceed(original)
        val req = original.newBuilder()
            .header(HEADER_AUTH, "Bearer $token")
            .build()
        return chain.proceed(req)
    }

    private companion object {
        const val HEADER_AUTH = "Authorization"
    }
}
