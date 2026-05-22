package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.social.SessionPhotoCache
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Mentions contract §3.1 / §3.2 — covers [SocialRepository.searchHandles] and
 * [SocialRepository.resolveHandle], the handle-search and handle-resolution
 * methods backing the `@`-mention autocomplete and tap-to-profile.
 */
class SocialRepositoryHandlesTest {

    private lateinit var api: BowPressApi
    private lateinit var repo: SocialRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        repo = SocialRepository(
            api,
            mockk(relaxed = true), // profileDao
            mockk(relaxed = true), // friendshipDao
            mockk(relaxed = true), // clubDao
            mockk(relaxed = true), // feedDao
            mockk(relaxed = true), // leagueDao
            mockk(relaxed = true), // invitationDao
            mockk(relaxed = true), // blockDao
            mockk(relaxed = true), // achievementDao
            mockk(relaxed = true), // sessionDao
            mockk(relaxed = true), // sessionEndDao
            mockk(relaxed = true), // plotDao
            SessionPhotoCache(),
            mockk(relaxed = true), // context
        )
    }

    private fun suggestion(handle: String) =
        HandleSuggestion(userId = "u-$handle", handle = handle, displayName = handle)

    private fun profile(handle: String) = SocialProfile(
        userId = "u-$handle",
        handle = handle,
        displayName = handle,
        joinedAt = Instant.now(),
        visibility = SocialVisibility.friends,
    )

    // ── searchHandles ────────────────────────────────────────────────────────

    @Test
    fun `searchHandles passes the prefix through to the API`() = runTest {
        coEvery { api.searchHandles("sar") } returns listOf(suggestion("sara.lin"))

        val result = repo.searchHandles("sar")

        assertThat(result.map { it.handle }).containsExactly("sara.lin")
        coVerify { api.searchHandles("sar") }
    }

    @Test
    fun `searchHandles trims a leading @ from the query`() = runTest {
        coEvery { api.searchHandles("lina") } returns listOf(suggestion("lina.h"))

        repo.searchHandles("@lina")

        // The server expects `q` without the `@`.
        coVerify { api.searchHandles("lina") }
    }

    @Test
    fun `searchHandles short-circuits a blank query without hitting the API`() = runTest {
        val result = repo.searchHandles("   ")

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { api.searchHandles(any()) }
    }

    @Test
    fun `searchHandles short-circuits a bare @ without hitting the API`() = runTest {
        val result = repo.searchHandles("@")

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { api.searchHandles(any()) }
    }

    // ── resolveHandle ────────────────────────────────────────────────────────

    @Test
    fun `resolveHandle returns the archer profile on success`() = runTest {
        coEvery { api.getArcherByHandle("sara.lin") } returns profile("sara.lin")

        val result = repo.resolveHandle("sara.lin")

        assertThat(result?.userId).isEqualTo("u-sara.lin")
    }

    @Test
    fun `resolveHandle tolerates a leading @ on the handle`() = runTest {
        coEvery { api.getArcherByHandle("lina.h") } returns profile("lina.h")

        repo.resolveHandle("@lina.h")

        coVerify { api.getArcherByHandle("lina.h") }
    }

    @Test
    fun `resolveHandle returns null when the handle resolves to no archer`() = runTest {
        coEvery { api.getArcherByHandle("ghost") } throws RuntimeException("404")

        val result = repo.resolveHandle("ghost")

        assertThat(result).isNull()
    }
}
