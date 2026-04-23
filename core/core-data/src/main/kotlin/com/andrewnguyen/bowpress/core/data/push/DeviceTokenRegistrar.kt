package com.andrewnguyen.bowpress.core.data.push

import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.RegisterDeviceTokenRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent registration of the FCM device token with the BowPress API.
 *
 * Mirrors iOS `PushRegistrar.onTokenReceived(hex:)` — the app invokes [register]
 * (a) every time `FirebaseMessagingService.onNewToken` fires, and (b) once
 * post-auth on app launch (using `FirebaseMessaging.getInstance().token`).
 *
 * We dedupe in-memory on the [token] string so the "on launch + immediate refresh"
 * race doesn't produce two identical POSTs — the server is idempotent on
 * `(userId, token)` so double-posts are harmless, but there's no point in the
 * network round-trip. A failed registration is NOT cached — the next call will
 * retry.
 */
@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val api: BowPressApi,
) {
    private val mutex = Mutex()
    private var lastRegisteredToken: String? = null

    /**
     * Register [token] with the backend. Safe to call from any thread. Swallows
     * network errors (logged by the `ErrorInterceptor`) — push registration is
     * best-effort and must not block app launch.
     */
    suspend fun register(token: String) {
        if (token.isBlank()) return
        mutex.withLock {
            if (token == lastRegisteredToken) return
            runCatching {
                api.registerDeviceToken(
                    RegisterDeviceTokenRequest(token = token, environment = environment()),
                )
            }.onSuccess {
                lastRegisteredToken = token
            }
        }
    }

    /** Clear the in-memory dedupe cache — call on sign-out. */
    fun reset() {
        lastRegisteredToken = null
    }

    /**
     * Matches the iOS convention — `"development"` for debug builds,
     * `"production"` for release. We can't read `BuildConfig.DEBUG` from a
     * library module without wiring a BuildConfig dependency, so the app layer
     * injects this via [environmentOverride]. Default matches iOS dev default.
     */
    private fun environment(): String = environmentOverride ?: "development"

    /** App-layer sets this at startup; keeps the library free of BuildConfig coupling. */
    var environmentOverride: String? = null
}
