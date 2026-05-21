package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityComment
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
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
    ) = ActivityComment(
        id = id,
        subjectId = "ss-1",
        userId = userId,
        authorHandle = "$userId.h",
        authorDisplayName = "Author $userId",
        body = body,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun `load resolves the thread and the caller id`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns listOf(
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
    }

    @Test
    fun `post appends the created comment to the thread`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns emptyList()
        coEvery { repository.postComment("ss-1", "Solid grouping") } returns
            comment("c-new", userId = "me-id", body = "Solid grouping")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.post("  Solid grouping  ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-new")
        coVerify { repository.postComment("ss-1", "Solid grouping") }
    }

    @Test
    fun `a blank comment is never posted`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns emptyList()

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.post("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.postComment(any(), any()) }
    }

    @Test
    fun `canDelete is true for the author and the post owner only`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns listOf(
            comment("c-mine", userId = "me-id"),
            comment("c-theirs", userId = "stranger-id"),
        )

        // Caller is the post owner.
        val ownerVm = viewModel()
        ownerVm.load("ss-1", subjectOwnerUserId = "me-id")
        testDispatcher.scheduler.advanceUntilIdle()
        val ownerState = ownerVm.uiState.value
        // Owner can delete any comment (moderation).
        assertThat(ownerState.canDelete(ownerState.comments.first { it.id == "c-theirs" })).isTrue()

        // Caller is a plain viewer (not the owner).
        val viewerVm = viewModel()
        viewerVm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()
        val viewerState = viewerVm.uiState.value
        // The viewer may delete only their own comment.
        assertThat(viewerState.canDelete(viewerState.comments.first { it.id == "c-mine" })).isTrue()
        assertThat(viewerState.canDelete(viewerState.comments.first { it.id == "c-theirs" })).isFalse()
    }

    @Test
    fun `delete drops the comment from the thread on success`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns listOf(
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
    fun `delete is refused without reaching the repository when the caller may not delete`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        coEvery { repository.getActivityComments("ss-1") } returns listOf(
            comment("c-theirs", userId = "stranger-id"),
        )

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.delete(vm.uiState.value.comments.first())
        testDispatcher.scheduler.advanceUntilIdle()

        // The comment stays; no repository call was made.
        assertThat(vm.uiState.value.comments).hasSize(1)
        assertThat(vm.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { repository.deleteComment(any(), any(), any()) }
    }

    // ── C1 — post-during-load race ───────────────────────────────────────────

    @Test
    fun `a comment posted while a slow load is in flight survives the load merge`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")

        // A load whose server fetch is held open — its snapshot predates the
        // post and carries an older, unrelated comment.
        val loadGate = CompletableDeferred<List<ActivityComment>>()
        coEvery { repository.getActivityComments("ss-1") } coAnswers { loadGate.await() }

        val posted = comment(
            "c-posted",
            userId = "me-id",
            body = "My fresh comment",
            createdAt = Instant.parse("2026-05-01T12:00:00Z"),
        )
        coEvery { repository.postComment("ss-1", "My fresh comment") } returns posted

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = null)
        // The load coroutine is suspended on loadGate — the thread is not
        // resolved yet.
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.isLoading).isTrue()

        // The user posts a comment while the load is still in flight.
        vm.post("My fresh comment")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.comments.map { it.id }).containsExactly("c-posted")

        // Now the slow load completes with a stale snapshot that does NOT
        // include the just-posted comment.
        val stale = comment(
            "c-stale",
            userId = "stranger-id",
            createdAt = Instant.parse("2026-05-01T10:00:00Z"),
        )
        loadGate.complete(listOf(stale))
        testDispatcher.scheduler.advanceUntilIdle()

        // C1 — the load merges rather than overwriting: the posted comment is
        // still there, alongside the server's comment, sorted oldest→newest.
        assertThat(vm.uiState.value.comments.map { it.id })
            .containsExactly("c-stale", "c-posted").inOrder()
    }

    // ── C2 — initial-load failure ────────────────────────────────────────────

    @Test
    fun `an initial load failure raises loadFailed and retry recovers the thread`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        // The first fetch fails; the retry succeeds.
        coEvery { repository.getActivityComments("ss-1") } throws RuntimeException("offline") andThen
            listOf(comment("c-1", userId = "u-2"))

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()

        // C2 — a failed initial load with nothing to show flags loadFailed so
        // the screen renders a retry state, NOT the empty "no comments" state.
        val failed = vm.uiState.value
        assertThat(failed.loadFailed).isTrue()
        assertThat(failed.isLoading).isFalse()
        assertThat(failed.comments).isEmpty()
        assertThat(failed.error).isNotNull()

        // retry() re-runs the fetch — it now succeeds and clears the flag.
        vm.retry()
        testDispatcher.scheduler.advanceUntilIdle()
        val recovered = vm.uiState.value
        assertThat(recovered.loadFailed).isFalse()
        assertThat(recovered.comments.map { it.id }).containsExactly("c-1")
    }

    @Test
    fun `a refresh failure with comments already shown keeps the thread visible`() = runTest {
        coEvery { repository.getMyProfile() } returns profile("me-id")
        // First load succeeds, a later reload fails.
        coEvery { repository.getActivityComments("ss-1") } returns
            listOf(comment("c-1", userId = "u-2")) andThenThrows RuntimeException("offline")

        val vm = viewModel()
        vm.load("ss-1", subjectOwnerUserId = "owner-id")
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.comments).hasSize(1)

        // A reload fails — but the thread already has content, so it is NOT a
        // hard failure: loadFailed stays false, the comments stay on screen.
        vm.retry()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.loadFailed).isFalse()
        assertThat(state.comments).hasSize(1)
        assertThat(state.error).isNotNull()
    }
}
