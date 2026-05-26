package com.andrewnguyen.bowpress.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds avatar URLs the way iOS `APIClient.fetchAvatar` does:
 *   {baseUrl}/social/avatars/{userId}?v={version}
 *
 * The API never sends an absolute `avatarUrl` for profiles, actors, comment
 * authors, etc. — it only sends `avatarVersion`. Both platforms construct
 * the URL client-side so a fresh upload bumps the version and busts any
 * downstream caches. Coil's ImageLoader reuses the Hilt OkHttp stack, so
 * the request rides through `AuthInterceptor` and lands with the Bearer
 * header attached.
 */
@Singleton
class AvatarUrlResolver @Inject constructor(
    private val config: NetworkConfig,
) {
    fun urlFor(userId: String?, version: Int?): String? {
        if (userId.isNullOrBlank()) return null
        if (version == null || version <= 0) return null
        val enc = java.net.URLEncoder.encode(userId, Charsets.UTF_8)
        val base = config.baseUrl.trimEnd('/')
        return "$base/social/avatars/$enc?v=$version"
    }
}
