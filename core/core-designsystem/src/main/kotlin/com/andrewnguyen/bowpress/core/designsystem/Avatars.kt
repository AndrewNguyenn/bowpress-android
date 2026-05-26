package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Resolves `{userId, avatarVersion}` to an HTTPS URL Coil can fetch — the iOS
 * `APIClient.fetchAvatar` shape (`{baseUrl}/social/avatars/{userId}?v={n}`).
 *
 * The API never returns an absolute URL; both platforms reconstruct it from
 * `avatarVersion` so a fresh upload busts the image cache. The app root
 * wires this to `AvatarUrlResolver` (core-network); previews fall back to the
 * no-op default so they don't need a Hilt entry point.
 */
val LocalAvatarUrl: ProvidableCompositionLocal<(String?, Int?) -> String?> =
    staticCompositionLocalOf { { _, _ -> null } }
