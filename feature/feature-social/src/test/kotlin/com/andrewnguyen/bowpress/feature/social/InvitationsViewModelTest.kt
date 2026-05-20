package com.andrewnguyen.bowpress.feature.social

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.sync.SocialBadgeRefreshBus
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.feature.social.ui.invitations.InvitationsViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class InvitationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository
    private lateinit var badgeBus: SocialBadgeRefreshBus

    private fun invite(
        id: String,
        kind: InvitationKind,
        status: InvitationStatus = InvitationStatus.pending,
    ) = SocialInvitation(
        id = id,
        kind = kind,
        targetId = "target-$id",
        targetName = "Target $id",
        inviterUserId = "u-host",
        inviterHandle = "host.h",
        inviteeUserId = "me",
        status = status,
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        badgeBus = SocialBadgeRefreshBus()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadInvitations keeps only pending and splits by kind`() = runTest {
        coEvery { repository.getInvitations() } returns listOf(
            invite("c1", InvitationKind.club),
            invite("c2", InvitationKind.club),
            invite("l1", InvitationKind.league),
            // an already-accepted invite must be filtered out
            invite("l2", InvitationKind.league, status = InvitationStatus.accepted),
        )

        val vm = InvitationsViewModel(repository, badgeBus)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.invitations).hasSize(3)
        assertThat(state.clubInvites.map { it.id }).containsExactly("c1", "c2")
        assertThat(state.leagueInvites.map { it.id }).containsExactly("l1")
    }

    @Test
    fun `acceptInvitation removes the row, bumps the badge, and runs onAccepted`() = runTest {
        coEvery { repository.getInvitations() } returns listOf(
            invite("c1", InvitationKind.club),
            invite("l1", InvitationKind.league),
        )
        coEvery { repository.acceptInvitation("c1", null) } returns
            invite("c1", InvitationKind.club, status = InvitationStatus.accepted)

        val vm = InvitationsViewModel(repository, badgeBus)
        testDispatcher.scheduler.advanceUntilIdle()

        var onAcceptedRan = false

        badgeBus.events.test {
            vm.acceptInvitation("c1") { onAcceptedRan = true }
            testDispatcher.scheduler.advanceUntilIdle()

            // Badge-refresh ping fired.
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repository.acceptInvitation("c1", null) }
        assertThat(onAcceptedRan).isTrue()
        // c1 dropped optimistically; l1 remains.
        assertThat(vm.uiState.value.invitations.map { it.id }).containsExactly("l1")
    }

    @Test
    fun `declineInvitation removes the row and bumps the badge`() = runTest {
        coEvery { repository.getInvitations() } returns listOf(
            invite("c1", InvitationKind.club),
            invite("l1", InvitationKind.league),
        )
        coEvery { repository.declineInvitation("l1") } returns Unit

        val vm = InvitationsViewModel(repository, badgeBus)
        testDispatcher.scheduler.advanceUntilIdle()

        badgeBus.events.test {
            vm.declineInvitation("l1")
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repository.declineInvitation("l1") }
        assertThat(vm.uiState.value.invitations.map { it.id }).containsExactly("c1")
    }

    @Test
    fun `loadInvitations surfaces the error and stops loading on failure`() = runTest {
        coEvery { repository.getInvitations() } throws RuntimeException("network down")

        val vm = InvitationsViewModel(repository, badgeBus)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo("network down")
        assertThat(state.invitations).isEmpty()
    }

    @Test
    fun `accept failure surfaces error and keeps the row`() = runTest {
        coEvery { repository.getInvitations() } returns listOf(invite("c1", InvitationKind.club))
        coEvery { repository.acceptInvitation("c1", null) } throws RuntimeException("409 already member")

        val vm = InvitationsViewModel(repository, badgeBus)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.acceptInvitation("c1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.error).isEqualTo("409 already member")
        // Row stays — accept did not succeed.
        assertThat(vm.uiState.value.invitations.map { it.id }).containsExactly("c1")
    }
}
