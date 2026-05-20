package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.feature.social.ui.clubs.ClubViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Covers the §17 club announcement board on [ClubViewModel] — load, post,
 * pin, and delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClubBoardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    private fun club() = Club(
        id = "club-1",
        name = "Riverside Archers",
        description = null,
        notes = null,
        inviteCode = "RVS01",
        createdAt = Instant.now(),
        createdBy = "me",
        memberCount = 9,
        myRole = ClubRole.host,
    )

    private fun announcement(id: String, pinned: Boolean) = ClubAnnouncement(
        id = id,
        clubId = "club-1",
        authorUserId = "me",
        authorHandle = "andrew.n",
        authorDisplayName = "Andrew Nguyen",
        body = "Body $id",
        pinned = pinned,
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.observeClubs() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadClubHome populates the announcement board`() = runTest {
        coEvery { repository.getClub("club-1") } returns club()
        coEvery { repository.getClubMembers("club-1") } returns emptyList()
        coEvery { repository.getClubFeed("club-1") } returns emptyList()
        coEvery { repository.getClubLeaderboard("club-1", any()) } returns emptyList()
        coEvery { repository.getClubAnnouncements("club-1") } returns listOf(
            announcement("a-pin", pinned = true),
            announcement("a-1", pinned = false),
        )

        val vm = ClubViewModel(repository)
        vm.loadClubHome("club-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.clubHomeState.value
        assertThat(state.announcements.map { it.id }).containsExactly("a-pin", "a-1")
    }

    @Test
    fun `postAnnouncement posts, reloads the board, and runs onPosted`() = runTest {
        coEvery { repository.getClub("club-1") } returns club()
        coEvery { repository.getClubMembers("club-1") } returns emptyList()
        coEvery { repository.getClubFeed("club-1") } returns emptyList()
        coEvery { repository.getClubLeaderboard("club-1", any()) } returns emptyList()
        coEvery { repository.getClubAnnouncements("club-1") } returns emptyList()
        coEvery { repository.postClubAnnouncement("club-1", "Range closed", true) } returns
            announcement("a-new", pinned = true)

        val vm = ClubViewModel(repository)
        vm.loadClubHome("club-1")
        testDispatcher.scheduler.advanceUntilIdle()

        var onPostedRan = false
        // After posting, the board reload returns the new post.
        coEvery { repository.getClubAnnouncements("club-1") } returns listOf(
            announcement("a-new", pinned = true),
        )
        vm.postAnnouncement("club-1", "Range closed", pinned = true) { onPostedRan = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.postClubAnnouncement("club-1", "Range closed", true) }
        assertThat(onPostedRan).isTrue()
        assertThat(vm.clubHomeState.value.announcements.map { it.id }).containsExactly("a-new")
    }

    @Test
    fun `a post failure surfaces the announcement error`() = runTest {
        coEvery { repository.getClub("club-1") } returns club()
        coEvery { repository.getClubAnnouncements("club-1") } returns emptyList()
        coEvery { repository.postClubAnnouncement(any(), any(), any()) } throws
            RuntimeException("403 host only")

        val vm = ClubViewModel(repository)
        vm.loadClubHome("club-1")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.postAnnouncement("club-1", "Body", pinned = false) { }
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.clubHomeState.value.announcementError).isEqualTo("403 host only")
    }

    @Test
    fun `setAnnouncementPinned reloads the board`() = runTest {
        coEvery { repository.getClub("club-1") } returns club()
        coEvery { repository.getClubAnnouncements("club-1") } returns listOf(
            announcement("a-1", pinned = false),
        )
        coEvery { repository.setClubAnnouncementPinned("club-1", "a-1", true) } returns
            announcement("a-1", pinned = true)

        val vm = ClubViewModel(repository)
        vm.loadClubHome("club-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getClubAnnouncements("club-1") } returns listOf(
            announcement("a-1", pinned = true),
        )
        vm.setAnnouncementPinned("club-1", "a-1", pinned = true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.setClubAnnouncementPinned("club-1", "a-1", true) }
        assertThat(vm.clubHomeState.value.announcements.first().pinned).isTrue()
    }

    @Test
    fun `deleteAnnouncement drops the row optimistically`() = runTest {
        coEvery { repository.getClub("club-1") } returns club()
        coEvery { repository.getClubAnnouncements("club-1") } returns listOf(
            announcement("a-1", pinned = false),
            announcement("a-2", pinned = false),
        )
        coEvery { repository.deleteClubAnnouncement("club-1", "a-1") } returns Unit

        val vm = ClubViewModel(repository)
        vm.loadClubHome("club-1")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteAnnouncement("club-1", "a-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteClubAnnouncement("club-1", "a-1") }
        assertThat(vm.clubHomeState.value.announcements.map { it.id }).containsExactly("a-2")
    }
}
