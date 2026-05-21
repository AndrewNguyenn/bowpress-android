package com.andrewnguyen.bowpress.core.data.social

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Social Feed V2 §4 — the process-scoped photo-byte cache: a hit short-circuits
 * the network, and the access-ordered LRU eviction keeps it bounded.
 */
class SessionPhotoCacheTest {

    @Test
    fun `a stored photo is returned on get`() {
        val cache = SessionPhotoCache()
        val bytes = byteArrayOf(1, 2, 3)
        cache.put("ph-1", bytes)
        assertThat(cache.get("ph-1")).isEqualTo(bytes)
    }

    @Test
    fun `a miss returns null`() {
        assertThat(SessionPhotoCache().get("nope")).isNull()
    }

    @Test
    fun `re-putting a key replaces its bytes`() {
        val cache = SessionPhotoCache()
        cache.put("ph-1", byteArrayOf(1))
        cache.put("ph-1", byteArrayOf(9, 9))
        assertThat(cache.get("ph-1")).isEqualTo(byteArrayOf(9, 9))
    }

    @Test
    fun `the least-recently-used entry is evicted past the byte budget`() {
        val cache = SessionPhotoCache()
        // One photo just over half the 16 MB budget — two cannot coexist.
        val big = ByteArray(9 * 1024 * 1024)

        cache.put("old", big)
        cache.put("new", big)

        // "old" was pushed out; "new" survives.
        assertThat(cache.get("old")).isNull()
        assertThat(cache.get("new")).isNotNull()
    }

    @Test
    fun `a recent get spares an entry from eviction`() {
        val cache = SessionPhotoCache()
        val big = ByteArray(7 * 1024 * 1024)

        cache.put("a", big)
        cache.put("b", big)
        // Touch "a" so it becomes the most-recently-used.
        cache.get("a")
        // Adding "c" must now evict "b", not the freshly-touched "a".
        cache.put("c", big)

        assertThat(cache.get("a")).isNotNull()
        assertThat(cache.get("b")).isNull()
        assertThat(cache.get("c")).isNotNull()
    }
}
