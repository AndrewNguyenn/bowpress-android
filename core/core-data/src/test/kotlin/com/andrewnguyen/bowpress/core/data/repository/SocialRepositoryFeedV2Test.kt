package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.social.SessionPhotoCache
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.model.PhotoStatus
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.model.SharedSessionPhoto
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.andrewnguyen.bowpress.core.network.PatchSharedSessionResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Instant

/**
 * Social Feed V2 (contract §3, §4) — covers the shared-session edit and the
 * multi-photo gallery methods on [SocialRepository].
 */
class SocialRepositoryFeedV2Test {

    private lateinit var api: BowPressApi
    private lateinit var feedDao: ActivityFeedDao
    private lateinit var photoCache: SessionPhotoCache
    private lateinit var repo: SocialRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        feedDao = mockk(relaxed = true)
        // A real cache so the M2 caching behaviour can be asserted directly.
        photoCache = SessionPhotoCache()
        repo = SocialRepository(
            api,
            mockk(relaxed = true), // profileDao
            mockk(relaxed = true), // friendshipDao
            mockk(relaxed = true), // clubDao
            feedDao,
            mockk(relaxed = true), // leagueDao
            mockk(relaxed = true), // invitationDao
            mockk(relaxed = true), // blockDao
            mockk(relaxed = true), // achievementDao
            mockk(relaxed = true), // sessionDao
            mockk(relaxed = true), // sessionEndDao
            mockk(relaxed = true), // plotDao
            photoCache,
            mockk(relaxed = true), // context
        )
    }

    private fun sharedSession(
        title: String? = "Comp prep",
        location: SessionLocation? = null,
    ) = SharedSession(
        id = "ss-1",
        userId = "u-1",
        sessionId = "sess-1",
        score = 548,
        xCount = 12,
        arrowCount = 60,
        title = title,
        location = location,
        shotAt = Instant.now(),
        createdAt = Instant.now(),
    )

    private fun detailWith(title: String?, location: SessionLocation?) = SharedSessionDetail(
        sharedSession = sharedSession(title = title, location = location),
        ownerHandle = "u-1.h",
        ownerDisplayName = "Owner",
    )

    private fun photo(id: String, position: Int) = SharedSessionPhoto(
        id = id,
        sharedSessionId = "ss-1",
        userId = "u-1",
        position = position,
        status = PhotoStatus.ready,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    /** Read the JSON string out of a captured Retrofit @Body RequestBody. */
    private fun RequestBody.asString(): String =
        Buffer().also { writeTo(it) }.readUtf8()

    // -------------------------------------------------------------------------
    // §3 — editSharedSession (partial PATCH, explicit-null clears)
    // -------------------------------------------------------------------------

    @Test
    fun `editSharedSession sends a changed title and location and refreshes the feed`() = runTest {
        val bodySlot = slot<RequestBody>()
        coEvery {
            api.patchSharedSession("ss-1", capture(bodySlot))
        } returns PatchSharedSessionResponse(sharedSession())
        coEvery { api.getActivityFeed() } returns emptyList()

        val place = SessionLocation(name = "Riverside", latitude = 1.0, longitude = 2.0)
        val result = repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "New name",
            newDescription = null,
            newLocation = place,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )

        assertThat(result.id).isEqualTo("ss-1")
        val body = bodySlot.captured.asString()
        assertThat(body).contains("\"title\":\"New name\"")
        assertThat(body).contains("\"name\":\"Riverside\"")
        // The server rewrote every feed row — the cache is refreshed.
        coVerify { api.getActivityFeed() }
    }

    @Test
    fun `editSharedSession clears the location as an explicit JSON null`() = runTest {
        // C1 regression — a cleared location must reach the server as
        // "location":null, NOT be silently dropped from the body.
        val bodySlot = slot<RequestBody>()
        coEvery {
            api.patchSharedSession("ss-1", capture(bodySlot))
        } returns PatchSharedSessionResponse(sharedSession())
        coEvery { api.getActivityFeed() } returns emptyList()

        val original = SessionLocation(name = "Riverside", latitude = 1.0, longitude = 2.0)
        repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Comp prep",
            newDescription = null,
            newLocation = null, // cleared
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = original,
        )

        val body = bodySlot.captured.asString()
        assertThat(body).contains("\"location\":null")
        // Title unchanged → its key must be omitted (no spurious rename).
        assertThat(body).doesNotContain("\"title\"")
    }

    @Test
    fun `editSharedSession clears the title as an explicit JSON null`() = runTest {
        val bodySlot = slot<RequestBody>()
        coEvery {
            api.patchSharedSession("ss-1", capture(bodySlot))
        } returns PatchSharedSessionResponse(sharedSession(title = null))
        coEvery { api.getActivityFeed() } returns emptyList()

        repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "   ", // blank → clear
            newDescription = null,
            newLocation = null,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )

        val body = bodySlot.captured.asString()
        assertThat(body).contains("\"title\":null")
    }

    @Test
    fun `editSharedSession sends a changed description and clears it with JSON null`() = runTest {
        // Migration 0039 — a changed description reaches the body; a cleared
        // one is an explicit "description":null, never a silent drop.
        val bodySlot = slot<RequestBody>()
        coEvery {
            api.patchSharedSession("ss-1", capture(bodySlot))
        } returns PatchSharedSessionResponse(sharedSession())
        coEvery { api.getActivityFeed() } returns emptyList()

        repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Comp prep",
            newDescription = "Best group of the month @sarah.n",
            newLocation = null,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )
        var body = bodySlot.captured.asString()
        assertThat(body).contains("\"description\":\"Best group of the month @sarah.n\"")
        // Title unchanged → its key is omitted.
        assertThat(body).doesNotContain("\"title\"")

        repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Comp prep",
            newDescription = "  ", // blank → clear
            newLocation = null,
            originalTitle = "Comp prep",
            originalDescription = "Best group of the month",
            originalLocation = null,
        )
        body = bodySlot.captured.asString()
        assertThat(body).contains("\"description\":null")
    }

    @Test
    fun `a location-only edit omits the title so the session is not renamed`() = runTest {
        // M1 — only the location changed; the PATCH body must not carry a
        // title key (sending it would trigger contract §3 effect 3, a rename).
        val bodySlot = slot<RequestBody>()
        coEvery {
            api.patchSharedSession("ss-1", capture(bodySlot))
        } returns PatchSharedSessionResponse(sharedSession())
        coEvery { api.getActivityFeed() } returns emptyList()

        val place = SessionLocation(name = "New Range", latitude = 3.0, longitude = 4.0)
        repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Comp prep", // unchanged
            newDescription = null,
            newLocation = place,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )

        val body = bodySlot.captured.asString()
        assertThat(body).doesNotContain("\"title\"")
        assertThat(body).contains("\"location\"")
    }

    @Test
    fun `editSharedSession with no changes skips the PATCH entirely`() = runTest {
        // Neither field changed — no PATCH, the loaded summary is returned.
        coEvery {
            api.getSharedSessionDetail("ss-1")
        } returns detailWith(title = "Comp prep", location = null)

        val result = repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Comp prep",
            newDescription = null,
            newLocation = null,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )

        assertThat(result.id).isEqualTo("ss-1")
        coVerify(exactly = 0) { api.patchSharedSession(any(), any()) }
    }

    @Test
    fun `editSharedSession survives a feed-refresh failure`() = runTest {
        coEvery {
            api.patchSharedSession(any(), any())
        } returns PatchSharedSessionResponse(sharedSession())
        coEvery { api.getActivityFeed() } throws RuntimeException("offline")

        // The edit itself succeeded; a best-effort refresh failure is swallowed.
        val result = repo.editSharedSession(
            sharedSessionId = "ss-1",
            newTitle = "Renamed",
            newDescription = null,
            newLocation = null,
            originalTitle = "Comp prep",
            originalDescription = null,
            originalLocation = null,
        )
        assertThat(result.id).isEqualTo("ss-1")
    }

    // -------------------------------------------------------------------------
    // §4 — photo gallery
    // -------------------------------------------------------------------------

    @Test
    fun `uploadSharedSessionPhoto posts the jpeg bytes`() = runTest {
        coEvery { api.uploadSharedSessionPhoto(any(), any()) } returns photo("ph-1", 0)

        val result = repo.uploadSharedSessionPhoto("ss-1", byteArrayOf(1, 2, 3, 4))

        assertThat(result.id).isEqualTo("ph-1")
        coVerify { api.uploadSharedSessionPhoto("ss-1", any()) }
    }

    @Test
    fun `listSharedSessionPhotos returns photos sorted by position`() = runTest {
        coEvery { api.listSharedSessionPhotos("ss-1") } returns listOf(
            photo("ph-c", 2),
            photo("ph-a", 0),
            photo("ph-b", 1),
        )

        val photos = repo.listSharedSessionPhotos("ss-1")

        assertThat(photos.map { it.id }).containsExactly("ph-a", "ph-b", "ph-c").inOrder()
    }

    @Test
    fun `fetchSharedSessionPhotoBytes returns the body on a 200`() = runTest {
        val bytes = byteArrayOf(9, 8, 7)
        coEvery {
            api.getSharedSessionPhoto("ss-1", "ph-1")
        } returns Response.success(bytes.toResponseBody())

        val result = repo.fetchSharedSessionPhotoBytes("ss-1", "ph-1")

        assertThat(result).isEqualTo(bytes)
    }

    @Test
    fun `fetchSharedSessionPhotoBytes caches a ready photo so a second read skips the network`() =
        runTest {
            // M2 — a successful fetch is cached; the feed must not re-download.
            val bytes = byteArrayOf(5, 5, 5)
            coEvery {
                api.getSharedSessionPhoto("ss-1", "ph-cached")
            } returns Response.success(bytes.toResponseBody())

            val first = repo.fetchSharedSessionPhotoBytes("ss-1", "ph-cached")
            val second = repo.fetchSharedSessionPhotoBytes("ss-1", "ph-cached")

            assertThat(first).isEqualTo(bytes)
            assertThat(second).isEqualTo(bytes)
            // Only one network call despite two reads.
            coVerify(exactly = 1) { api.getSharedSessionPhoto("ss-1", "ph-cached") }
        }

    @Test
    fun `fetchSharedSessionPhotoBytes returns null while the photo is still transcoding`() =
        runTest {
            // 202 — the display JPEG is not ready yet; must not be cached.
            coEvery {
                api.getSharedSessionPhoto("ss-1", "ph-pending")
            } returns Response.success(202, ByteArray(0).toResponseBody())

            val result = repo.fetchSharedSessionPhotoBytes("ss-1", "ph-pending")

            assertThat(result).isNull()
            // A later retry still hits the network (nothing was cached).
            repo.fetchSharedSessionPhotoBytes("ss-1", "ph-pending")
            coVerify(exactly = 2) { api.getSharedSessionPhoto("ss-1", "ph-pending") }
        }

    @Test
    fun `fetchSharedSessionPhotoBytes returns null on a network failure`() = runTest {
        coEvery {
            api.getSharedSessionPhoto(any(), any())
        } throws RuntimeException("boom")

        val result = repo.fetchSharedSessionPhotoBytes("ss-1", "ph-x")

        assertThat(result).isNull()
    }

    @Test
    fun `deleteSharedSessionPhoto calls the delete endpoint`() = runTest {
        coEvery { api.deleteSharedSessionPhoto(any(), any()) } returns Unit

        repo.deleteSharedSessionPhoto("ss-1", "ph-1")

        coVerify { api.deleteSharedSessionPhoto("ss-1", "ph-1") }
    }
}
