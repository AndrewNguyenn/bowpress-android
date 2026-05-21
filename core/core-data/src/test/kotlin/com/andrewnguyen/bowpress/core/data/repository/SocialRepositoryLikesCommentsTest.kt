package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.social.SessionPhotoCache
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.model.ActivityActor
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.CommentSort
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
    fun `getActivityComments forwards the sort and keeps the server's top-level order`() = runTest {
        // The §6.3 API returns top-level comments already in the requested
        // order — the repository must NOT re-sort them.
        val first = comment("c-1").copy(createdAt = Instant.parse("2026-05-01T12:00:00Z"))
        val second = comment("c-2").copy(createdAt = Instant.parse("2026-05-01T10:00:00Z"))
        coEvery { api.getActivityComments("ss-1", "top") } returns listOf(first, second)

        val thread = repo.getActivityComments("ss-1", CommentSort.top)

        assertThat(thread.map { it.id }).containsExactly("c-1", "c-2").inOrder()
        coVerify { api.getActivityComments("ss-1", "top") }
    }

    @Test
    fun `getActivityComments normalises each reply chain to oldest-to-newest`() = runTest {
        val newerReply = comment("r-new").copy(createdAt = Instant.parse("2026-05-01T12:00:00Z"))
        val olderReply = comment("r-old").copy(createdAt = Instant.parse("2026-05-01T10:00:00Z"))
        val top = comment("c-1").copy(replies = listOf(newerReply, olderReply))
        coEvery { api.getActivityComments("ss-1", "recent") } returns listOf(top)

        val thread = repo.getActivityComments("ss-1", CommentSort.recent)

        assertThat(thread.single().replies.map { it.id })
            .containsExactly("r-old", "r-new").inOrder()
    }

    @Test
    fun `postComment trims the body, posts it, and atomically bumps the cached count`() = runTest {
        val bodySlot = slot<PostCommentBody>()
        coEvery { api.postActivityComment("ss-1", capture(bodySlot)) } returns comment("c-1")

        val created = repo.postComment("ss-1", "  Solid session  ")

        assertThat(created.id).isEqualTo("c-1")
        // The body is trimmed before it reaches the API.
        assertThat(bodySlot.captured.body).isEqualTo("Solid session")
        // A top-level comment carries no parent.
        assertThat(bodySlot.captured.parentCommentId).isNull()
        // The cached comment count is bumped atomically (+1) — a single
        // delta UPDATE, no read-modify-write that two posts could race.
        coVerify { feedDao.adjustCommentCount("ss-1", +1) }
    }

    @Test
    fun `postComment forwards the parentCommentId when the comment is a reply`() = runTest {
        val bodySlot = slot<PostCommentBody>()
        coEvery { api.postActivityComment("ss-1", capture(bodySlot)) } returns comment("r-1")

        repo.postComment("ss-1", "good point", parentCommentId = "c-7")

        assertThat(bodySlot.captured.parentCommentId).isEqualTo("c-7")
        // A reply still counts toward the subject's total thread size.
        coVerify { feedDao.adjustCommentCount("ss-1", +1) }
    }

    // ── §6.2 comment likes ───────────────────────────────────────────────────

    @Test
    fun `toggleCommentLike likes a comment via POST without patching the feed cache`() = runTest {
        coEvery { api.likeActivity("c-1") } returns ToggleLikeResponse(likeCount = 4, likedByMe = true)

        val result = repo.toggleCommentLike(commentId = "c-1", currentlyLiked = false)

        assertThat(result.likeCount).isEqualTo(4)
        assertThat(result.likedByMe).isTrue()
        coVerify { api.likeActivity("c-1") }
        // A comment has no activity_feed row — no cache patch happens.
        coVerify(exactly = 0) { feedDao.updateLikeState(any(), any(), any()) }
    }

    @Test
    fun `toggleCommentLike un-likes a comment via DELETE`() = runTest {
        coEvery { api.unlikeActivity("c-1") } returns ToggleLikeResponse(likeCount = 3, likedByMe = false)

        val result = repo.toggleCommentLike(commentId = "c-1", currentlyLiked = true)

        assertThat(result.likedByMe).isFalse()
        coVerify { api.unlikeActivity("c-1") }
        coVerify(exactly = 0) { api.likeActivity(any()) }
    }

    // ── §6.4 likers ──────────────────────────────────────────────────────────

    @Test
    fun `getActivityLikers returns the full liker list from the API`() = runTest {
        val likers = listOf(
            ActivityActor(userId = "u-1", handle = "marcus.t", displayName = "Marcus T."),
            ActivityActor(userId = "u-2", handle = "jamie.a", displayName = "Jamie A."),
        )
        coEvery { api.getActivityLikers("ss-1") } returns likers

        val result = repo.getActivityLikers("ss-1")

        assertThat(result.map { it.handle }).containsExactly("marcus.t", "jamie.a").inOrder()
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
