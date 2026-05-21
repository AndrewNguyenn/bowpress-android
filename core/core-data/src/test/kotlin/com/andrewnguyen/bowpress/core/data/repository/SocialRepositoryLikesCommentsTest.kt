package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.social.SessionPhotoCache
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.PostCommentBody
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Social Feed V2 Part 2 (contract §5) — covers the like-toggle and comment
 * methods on [SocialRepository].
 */
class SocialRepositoryLikesCommentsTest {

    private lateinit var api: BowPressApi
    private lateinit var feedDao: ActivityFeedDao
    private lateinit var repo: SocialRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        feedDao = mockk(relaxed = true)
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
            SessionPhotoCache(),
            mockk(relaxed = true), // context
        )
    }

    private fun comment(id: String, body: String = "Nice", userId: String = "u-2") =
        ActivityComment(
            id = id,
            subjectId = "ss-1",
            userId = userId,
            authorHandle = "devon.c",
            authorDisplayName = "Devon Chen",
            body = body,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    // ── like-toggle ──────────────────────────────────────────────────────────

    @Test
    fun `toggleLike likes an unliked subject via POST and patches the cache`() = runTest {
        coEvery { api.likeActivity("ss-1") } returns ToggleLikeResponse(likeCount = 3, likedByMe = true)

        val result = repo.toggleLike(subjectId = "ss-1", currentlyLiked = false)

        assertThat(result.likeCount).isEqualTo(3)
        assertThat(result.likedByMe).isTrue()
        coVerify { api.likeActivity("ss-1") }
        coVerify(exactly = 0) { api.unlikeActivity(any()) }
        // Every cached row for the subject is patched with the server counts.
        coVerify { feedDao.updateLikeState("ss-1", 3, true) }
    }

    @Test
    fun `toggleLike un-likes a liked subject via DELETE`() = runTest {
        coEvery { api.unlikeActivity("ss-1") } returns ToggleLikeResponse(likeCount = 2, likedByMe = false)

        val result = repo.toggleLike(subjectId = "ss-1", currentlyLiked = true)

        assertThat(result.likeCount).isEqualTo(2)
        assertThat(result.likedByMe).isFalse()
        coVerify { api.unlikeActivity("ss-1") }
        coVerify(exactly = 0) { api.likeActivity(any()) }
        coVerify { feedDao.updateLikeState("ss-1", 2, false) }
    }

    // ── comments ─────────────────────────────────────────────────────────────

    @Test
    fun `getActivityComments returns the thread sorted oldest-to-newest`() = runTest {
        val older = comment("c-old").copy(createdAt = Instant.parse("2026-05-01T10:00:00Z"))
        val newer = comment("c-new").copy(createdAt = Instant.parse("2026-05-01T12:00:00Z"))
        coEvery { api.getActivityComments("ss-1") } returns listOf(newer, older)

        val thread = repo.getActivityComments("ss-1")

        assertThat(thread.map { it.id }).containsExactly("c-old", "c-new").inOrder()
    }

    @Test
    fun `postComment trims the body, posts it, and atomically bumps the cached count`() = runTest {
        val bodySlot = slot<PostCommentBody>()
        coEvery { api.postActivityComment("ss-1", capture(bodySlot)) } returns comment("c-1")

        val created = repo.postComment("ss-1", "  Solid session  ")

        assertThat(created.id).isEqualTo("c-1")
        // The body is trimmed before it reaches the API.
        assertThat(bodySlot.captured.body).isEqualTo("Solid session")
        // The cached comment count is bumped atomically (+1) — a single
        // delta UPDATE, no read-modify-write that two posts could race.
        coVerify { feedDao.adjustCommentCount("ss-1", +1) }
    }

    @Test
    fun `deleteComment removes a comment and atomically decrements the cached count`() = runTest {
        coEvery { api.deleteActivityComment("ss-1", "c-1") } returns Unit

        repo.deleteComment("ss-1", "c-1", canDelete = true)

        coVerify { api.deleteActivityComment("ss-1", "c-1") }
        // The cached comment count is decremented atomically (-1); the clamp
        // at 0 lives in the SQL (MAX(0, …)) — see ActivityFeedDao.
        coVerify { feedDao.adjustCommentCount("ss-1", -1) }
    }

    @Test
    fun `deleteComment never reaches the network when the caller may not delete`() = runTest {
        val error = runCatching { repo.deleteComment("ss-1", "c-1", canDelete = false) }
        assertThat(error.isFailure).isTrue()
        assertThat(error.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        coVerify(exactly = 0) { api.deleteActivityComment(any(), any()) }
        // No cache write either when the delete is refused up front.
        coVerify(exactly = 0) { feedDao.adjustCommentCount(any(), any()) }
    }
}
