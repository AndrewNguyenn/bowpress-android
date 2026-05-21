package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.CommentSort
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.core.model.ToggleLikeResponse
import com.andrewnguyen.bowpress.feature.social.ui.comments.CommentsViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        // The VM also fetches the session-recap context strip — make that a
        // benign failure by default so tests that don't care about it can
        // ignore it (a failed detail fetch simply leaves the strip absent).
        coEvery { repository.getSharedSessionDetail(any()) } throws RuntimeException("no detail")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CommentsViewModel(repository)

    private fun profile(userId: String) = SocialProfile(
        userId = userId,
        handle = "me.handle",
        displayName = "Me",
        joinedAt = Instant.now(),
        visibility = SocialVisibility.friends,
    )

    private fun comment(
        id: String,
        userId: String,
        body: String = "Nice",
        createdAt: Instant = Instant.now(),
        parentCommentId: String? = null,
        likeCount: Int = 0,
        likedByMe: Boolean = false,
        replies: List<ActivityComment> = emptyList(),
    ) = ActivityComment(
        id = id,
        subjectId = "ss-1",
        userId = userId,
        authorHandle = "$userId.h",
        authorDisplayName = "Author $userId",
        body = body,
        createdAt = createdAt,
        updatedAt = createdAt,
        parentCommentId = parentCommentId,
        likeCount = likeCount,
        likedByMe = likedByMe,
        replies = replies,
        replyCount = replies.size,
    )

    @Test
    fun `load resolves the thread and the caller id`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.comments).hasSize(1)
        assertThat(state.callerUserId).isEqualTo("me-id")
        assertThat(state.subjectOwnerUserId).isEqualTo("owner-id")
        assertThat(state.sort).isEqualTo(CommentSort.recent)
    }

    @Test
    fun `totalCount counts top-level comments plus their replies`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment(
                "c-1", userId = "u-2",
                replies = listOf(comment("r-1", userId = "u-3"), comment("r-2", userId = "u-4")),
            ),
            comment("c-2", userId = "u-5"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        // 2 top-level + 2 replies = 4.
        assertThat(vm.uiState.value.totalCount).isEqualTo(4)
    }

    @Test
    fun `setSort re-fetches the thread in the new order`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", CommentSort.recent) } returns
            listOf(comment("c-recent", userId = "u-2"))
        coEvery { repository.getActivityComments("ss-1", CommentSort.top) } returns
            listOf(comment("c-top", userId = "u-9"))

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-recent")

        vm.setSort(CommentSort.top)
        testDispatcher.scheduler.advanceUntilIdle()
        // The sort change replaces the thread wholesale — the new ordering wins.
        assertThat(vm.uiState.value.sort).isEqualTo(CommentSort.top)
        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-top")
        coVerify { repository.getActivityComments("ss-1", CommentSort.top) }
    }

    @Test
    fun `post appends a top-level comment and clears the draft`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns emptyList()
        coEvery { repository.postComment("ss-1", "Solid grouping", parentCommentId = null) } returns
            comment("c-new", userId = "me-id", body = "Solid grouping")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateDraft("  Solid grouping  ")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-new")
        // The draft is cleared on a successful post.
        assertThat(vm.uiState.value.draft).isEmpty()
        coVerify { repository.postComment("ss-1", "Solid grouping", parentCommentId = null) }
    }

    @Test
    fun `a blank comment is never posted`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns emptyList()

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.updateDraft("   ")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.postComment(any(), any(), any()) }
    }

    // ── M2 — a failed post keeps the draft + reply context ───────────────────

    @Test
    fun `a failed post keeps the draft and the reply context`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )
        coEvery { repository.postComment(any(), any(), any()) } throws RuntimeException("offline")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startReply(vm.uiState.value.comments.first())
        vm.updateDraft("@u-2.h still typing this")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()

        // M2 — the draft survives the failure so the user can retry, and the
        // "Replying to @x" context is still active.
        assertThat(vm.uiState.value.draft).isEqualTo("@u-2.h still typing this")
        assertThat(vm.uiState.value.replyTarget).isNotNull()
        assertThat(vm.uiState.value.error).isNotNull()
    }

    // ── §6.3 — replies ───────────────────────────────────────────────────────

    @Test
    fun `startReply on a top-level comment targets that comment and prefills the mention`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startReply(vm.uiState.value.comments.first())
        val target = vm.uiState.value.replyTarget
        assertThat(target).isNotNull()
        assertThat(target!!.parentCommentId).isEqualTo("c-1")
        assertThat(target.addresseeHandle).isEqualTo("u-2.h")
        // M1 — the draft opens with the addressee's @mention.
        assertThat(vm.uiState.value.draft).isEqualTo("@u-2.h ")
    }

    // ── M1 — the mention prefill is deterministic ────────────────────────────

    @Test
    fun `startReply with text already typed keeps the text and prepends the mention`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        // The user typed something before tapping Reply.
        vm.updateDraft("nice shooting")
        vm.startReply(vm.uiState.value.comments.first())

        // M1 — the typed text is kept; the mention is prepended (NOT lost
        // because the draft was non-blank).
        assertThat(vm.uiState.value.draft).isEqualTo("@u-2.h nice shooting")
    }

    @Test
    fun `switching reply targets replaces the stale mention`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
            comment("c-2", userId = "u-9"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startReply(vm.uiState.value.comments.first { it.id == "c-1" })
        vm.updateDraft("@u-2.h here is my reply")
        // Switch the reply target without sending.
        vm.startReply(vm.uiState.value.comments.first { it.id == "c-2" })

        // M1 — the stale @u-2.h is replaced by @u-9.h, the body kept.
        assertThat(vm.uiState.value.draft).isEqualTo("@u-9.h here is my reply")
        assertThat(vm.uiState.value.replyTarget!!.addresseeHandle).isEqualTo("u-9.h")
    }

    @Test
    fun `cancelReply strips the leading mention from the draft`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startReply(vm.uiState.value.comments.first())
        vm.updateDraft("@u-2.h actually a general comment")
        vm.cancelReply()

        // The stale mention is dropped; the body becomes a plain comment.
        assertThat(vm.uiState.value.draft).isEqualTo("actually a general comment")
        assertThat(vm.uiState.value.replyTarget).isNull()
    }

    @Test
    fun `startReply on a reply normalises the parent up to the top-level comment`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        val reply = comment("r-1", userId = "u-3", parentCommentId = "c-1")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", replies = listOf(reply)),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Replying to the reply targets its top-level parent, not the reply.
        vm.startReply(vm.uiState.value.comments.first().replies.first())
        val target = vm.uiState.value.replyTarget
        assertThat(target!!.parentCommentId).isEqualTo("c-1")
        assertThat(target.addresseeHandle).isEqualTo("u-3.h")
    }

    @Test
    fun `posting a reply nests it under its top-level parent and clears the target`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )
        val createdReply = comment(
            "r-new", userId = "me-id", body = "@u-2.h good point", parentCommentId = "c-1",
        )
        coEvery {
            repository.postComment("ss-1", "@u-2.h good point", parentCommentId = "c-1")
        } returns createdReply

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startReply(vm.uiState.value.comments.first())
        // startReply prefilled "@u-2.h "; the user finishes the body.
        vm.updateDraft("@u-2.h good point")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()

        val top = vm.uiState.value.comments.single { it.id == "c-1" }
        assertThat(top.replies.map { it.id }).containsExactly("r-new")
        assertThat(top.replyCount).isEqualTo(1)
        // The reply target + draft are cleared after a successful post.
        assertThat(vm.uiState.value.replyTarget).isNull()
        assertThat(vm.uiState.value.draft).isEmpty()
    }

    // ── C2 — a reply into a collapsed thread auto-expands it ─────────────────

    @Test
    fun `posting a reply into a collapsed thread auto-expands that thread`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        // c-1 already has 3 replies — over the collapse threshold of 2.
        val existing = listOf(
            comment("r-1", userId = "u-3", parentCommentId = "c-1"),
            comment("r-2", userId = "u-4", parentCommentId = "c-1"),
            comment("r-3", userId = "u-5", parentCommentId = "c-1"),
        )
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", replies = existing),
        )
        val createdReply = comment(
            "r-new", userId = "me-id", body = "@u-2.h mine", parentCommentId = "c-1",
        )
        coEvery {
            repository.postComment("ss-1", "@u-2.h mine", parentCommentId = "c-1")
        } returns createdReply

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()
        // The thread starts collapsed.
        assertThat(vm.uiState.value.isThreadExpanded("c-1")).isFalse()

        vm.startReply(vm.uiState.value.comments.first())
        vm.updateDraft("@u-2.h mine")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()

        // C2 — the parent thread is auto-expanded so the new reply is not
        // hidden behind the "view N more replies" stub.
        assertThat(vm.uiState.value.isThreadExpanded("c-1")).isTrue()
        val top = vm.uiState.value.comments.single { it.id == "c-1" }
        assertThat(top.replies.map { it.id }).contains("r-new")
    }

    @Test
    fun `expandThread marks a thread expanded`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.isThreadExpanded("c-1")).isFalse()
        vm.expandThread("c-1")
        assertThat(vm.uiState.value.isThreadExpanded("c-1")).isTrue()
    }

    // ── §6 — author stamp ────────────────────────────────────────────────────

    @Test
    fun `isAuthorComment is true only for the session owner's comments`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-owner", userId = "owner-id"),
            comment("c-other", userId = "stranger-id"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isAuthorComment(state.comments.first { it.id == "c-owner" })).isTrue()
        assertThat(state.isAuthorComment(state.comments.first { it.id == "c-other" })).isFalse()
    }

    // ── §6.2 — comment likes ─────────────────────────────────────────────────

    @Test
    fun `toggleCommentLike optimistically flips then reconciles with the server`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", likeCount = 2, likedByMe = false),
        )
        coEvery { repository.toggleCommentLike("c-1", false) } returns
            ToggleLikeResponse(likeCount = 3, likedByMe = true)

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleCommentLike(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        val liked = vm.uiState.value.comments.single()
        assertThat(liked.likedByMe).isTrue()
        assertThat(liked.likeCount).isEqualTo(3)
        coVerify { repository.toggleCommentLike("c-1", false) }
    }

    // ── C1 — comment-like in-flight guard ────────────────────────────────────

    @Test
    fun `a double-tap on a comment heart fires exactly one toggle`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", likeCount = 2, likedByMe = false),
        )
        // The toggle is held open so a second tap lands while the first is
        // still in flight.
        val toggleGate = CompletableDeferred<ToggleLikeResponse>()
        coEvery { repository.toggleCommentLike("c-1", false) } coAnswers { toggleGate.await() }

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val target = vm.uiState.value.comments.first()
        // First tap — starts the toggle, marks the comment in flight.
        vm.toggleCommentLike(target)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.likingCommentIds).contains("c-1")

        // C1 — second tap while in flight is dropped: no racing unlike.
        vm.toggleCommentLike(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        // The first (and only) toggle resolves.
        toggleGate.complete(ToggleLikeResponse(likeCount = 3, likedByMe = true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Exactly one server call — the double-tap did not drift the count.
        coVerify(exactly = 1) { repository.toggleCommentLike("c-1", false) }
        coVerify(exactly = 0) { repository.toggleCommentLike("c-1", true) }
        val liked = vm.uiState.value.comments.single()
        assertThat(liked.likedByMe).isTrue()
        assertThat(liked.likeCount).isEqualTo(3)
        // The in-flight guard is cleared once the toggle settles.
        assertThat(vm.uiState.value.likingCommentIds).doesNotContain("c-1")
    }

    @Test
    fun `a failed comment-like toggle reverts the optimistic flip`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", likeCount = 5, likedByMe = true),
        )
        coEvery { repository.toggleCommentLike("c-1", true) } throws RuntimeException("offline")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleCommentLike(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        // Reverted to the pre-toggle state.
        val reverted = vm.uiState.value.comments.single()
        assertThat(reverted.likedByMe).isTrue()
        assertThat(reverted.likeCount).isEqualTo(5)
    }

    @Test
    fun `toggleCommentLike works on a nested reply`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        val reply = comment("r-1", userId = "u-3", parentCommentId = "c-1", likeCount = 0)
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", replies = listOf(reply)),
        )
        coEvery { repository.toggleCommentLike("r-1", false) } returns
            ToggleLikeResponse(likeCount = 1, likedByMe = true)

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleCommentLike(vm.uiState.value.comments.first().replies.first())
        testDispatcher.scheduler.advanceUntilIdle()

        val likedReply = vm.uiState.value.comments.first().replies.single()
        assertThat(likedReply.likedByMe).isTrue()
        assertThat(likedReply.likeCount).isEqualTo(1)
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    fun `canDelete is true for the author and the post owner only`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-mine", userId = "me-id"),
            comment("c-theirs", userId = "stranger-id"),
        )

        val ownerVm = viewModel()
        ownerVm.load("ss-1", subjectOwnerUserId = "me-id")
        testDispatcher.scheduler.advanceUntilIdle()
        val ownerState = ownerVm.uiState.value
        assertThat(ownerState.canDelete(ownerState.comments.first { it.id == "c-theirs" })).isTrue()

        val viewerVm = viewModel()
        viewerVm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()
        val viewerState = viewerVm.uiState.value
        assertThat(viewerState.canDelete(viewerState.comments.first { it.id == "c-mine" })).isTrue()
        assertThat(viewerState.canDelete(viewerState.comments.first { it.id == "c-theirs" })).isFalse()
    }

    @Test
    fun `delete drops a top-level comment from the thread on success`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-mine", userId = "me-id"),
        )
        coEvery { repository.deleteComment("ss-1", "c-mine", canDelete = true) } returns Unit

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.delete(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.comments).isEmpty()
        coVerify { repository.deleteComment("ss-1", "c-mine", canDelete = true) }
    }

    @Test
    fun `delete drops a single reply without touching its siblings`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        val r1 = comment("r-1", userId = "me-id", parentCommentId = "c-1")
        val r2 = comment("r-2", userId = "u-3", parentCommentId = "c-1")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-1", userId = "u-2", replies = listOf(r1, r2)),
        )
        coEvery { repository.deleteComment("ss-1", "r-1", canDelete = true) } returns Unit

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.delete(vm.uiState.value.comments.first().replies.first { it.id == "r-1" })
        testDispatcher.scheduler.advanceUntilIdle()

        val top = vm.uiState.value.comments.single()
        assertThat(top.replies.map { it.id }).containsExactly("r-2")
        assertThat(top.replyCount).isEqualTo(1)
    }

    @Test
    fun `delete is refused without reaching the repository when the caller may not delete`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns listOf(
            comment("c-theirs", userId = "stranger-id"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.delete(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.comments).hasSize(1)
        assertThat(vm.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { repository.deleteComment(any(), any(), any()) }
    }

    // ── C1 — post-during-load race ───────────────────────────────────────────

    @Test
    fun `a comment posted while a slow load is in flight survives the load merge`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")

        val loadGate = CompletableDeferred<List<ActivityComment>>()
        coEvery { repository.getActivityComments("ss-1", any()) } coAnswers { loadGate.await() }

        val posted = comment(
            "c-posted", userId = "me-id", body = "My fresh comment",
            createdAt = Instant.parse("2026-05-01T12:00:00Z"),
        )
        coEvery { repository.postComment("ss-1", "My fresh comment", parentCommentId = null) } returns posted

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.isLoading).isTrue()

        vm.updateDraft("My fresh comment")
        vm.post()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-posted")

        val stale = comment(
            "c-stale", userId = "stranger-id",
            createdAt = Instant.parse("2026-05-01T10:00:00Z"),
        )
        loadGate.complete(listOf(stale))
        testDispatcher.scheduler.advanceUntilIdle()

        // C1 — the same-sort reload merges: the posted comment survives, the
        // server's comment is added; server order first, local-only appended.
        assertThat(vm.uiState.value.comments.map { it.id })
            .containsExactly("c-stale", "c-posted").inOrder()
    }

    // ── C2 — initial-load failure ────────────────────────────────────────────

    @Test
    fun `an initial load failure raises loadFailed and retry recovers the thread`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } throws RuntimeException("offline") andThen
            listOf(comment("c-1", userId = "u-2"))

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val failed = vm.uiState.value
        assertThat(failed.loadFailed).isTrue()
        assertThat(failed.isLoading).isFalse()
        assertThat(failed.comments).isEmpty()
        assertThat(failed.error).isNotNull()

        vm.retry()
        testDispatcher.scheduler.advanceUntilIdle()
        val recovered = vm.uiState.value
        assertThat(recovered.loadFailed).isFalse()
        assertThat(recovered.comments.map { it.id }).containsExactly("c-1")
    }

    @Test
    fun `a refresh failure with comments already shown keeps the thread visible`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1", any()) } returns
            listOf(comment("c-1", userId = "u-2")) andThenThrows RuntimeException("offline")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.comments).hasSize(1)

        vm.retry()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.loadFailed).isFalse()
        assertThat(state.comments).hasSize(1)
        assertThat(state.error).isNotNull()
    }
}
