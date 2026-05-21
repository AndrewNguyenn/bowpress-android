package com.andrewnguyen.bowpress.core.data.social

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped in-memory cache of shared-session photo bytes (Social Feed V2
 * §4).
 *
 * Photo display JPEGs are fetched through the authenticated Retrofit stack —
 * a network round-trip. The feed is the app's main screen and recycles rows
 * aggressively as the `LazyColumn` scrolls, so without a cache every recycle
 * re-downloads the same photo. This caches a photo's bytes after the first
 * fetch, keyed by `photoId` (globally unique), bounded by total byte size so
 * a large gallery cannot grow the cache without limit. Least-recently-used
 * entries are evicted once the byte budget is exceeded.
 *
 * A `ready` photo's bytes are immutable (the server transcodes once), so a
 * cache hit never goes stale. Deleting a photo simply leaves a dead entry that
 * the LRU evicts in time — harmless, since a deleted photo is never requested
 * again.
 *
 * Backed by an access-ordered [LinkedHashMap] rather than `android.util.LruCache`
 * so it is plain-JVM testable without Robolectric. All access is `synchronized`
 * — the cache is touched from multiple photo-loading coroutines at once.
 */
@Singleton
class SessionPhotoCache @Inject constructor() {

    private var currentBytes = 0L

    // accessOrder = true → get() moves an entry to the most-recent end.
    private val entries = object : LinkedHashMap<String, ByteArray>(
        /* initialCapacity = */ 16,
        /* loadFactor = */ 0.75f,
        /* accessOrder = */ true,
    ) {}

    /** The cached bytes for [photoId], or null on a miss. */
    @Synchronized
    fun get(photoId: String): ByteArray? = entries[photoId]

    /** Store [bytes] for [photoId], evicting LRU entries past the byte budget. */
    @Synchronized
    fun put(photoId: String, bytes: ByteArray) {
        // A re-put of the same key replaces its byte contribution.
        entries.put(photoId, bytes)?.let { currentBytes -= it.size }
        currentBytes += bytes.size
        trimToBudget()
    }

    /** Evict from the least-recently-used end until within [MAX_BYTES]. */
    private fun trimToBudget() {
        val iterator = entries.entries.iterator()
        while (currentBytes > MAX_BYTES && iterator.hasNext()) {
            val evicted = iterator.next()
            currentBytes -= evicted.value.size
            iterator.remove()
        }
    }

    private companion object {
        /** Byte budget — ~16 MB, comfortably more than a feed-screen of photos. */
        const val MAX_BYTES = 16L * 1024 * 1024
    }
}
