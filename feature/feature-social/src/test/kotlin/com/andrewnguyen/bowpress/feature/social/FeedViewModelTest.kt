package com.andrewnguyen.bowpress.feature.social

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.Club
import com.andrewnguyen.bowpress.core.model.ClubRole
import com.andrewnguyen.bowpress.core.model.Division
import com.andrewnguyen.bowpress.core.model.Friendship
import com.andrewnguyen.bowpress.core.model.FriendshipSource
import com.andrewnguyen.bowpress.core.model.FriendshipStatus
import com.andrewnguyen.bowpress.core.model.HandicapConfig
import com.andrewnguyen.bowpress.core.model.League
import com.andrewnguyen.bowpress.core.model.LeagueEntryRule
import com.andrewnguyen.bowpress.core.model.LeagueSchedule
import com.andrewnguyen.bowpress.core.model.LeagueScheduleKind
import com.andrewnguyen.bowpress.core.model.LeagueStatus
import com.andrewnguyen.bowpress.core.model.LeagueType
import com.andrewnguyen.bowpress.core.model.RoundDef
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    private val stubProfile = SocialProfile(
        userId = "me",
        handle = "andrew.n",
        displayName = "Andrew Nguyen",
        joinedAt = Instant.now(),
        visibility = SocialVisibility.friends,
    )

    private val stubFriendship = Friendship(
        id = "fr-1",
        requesterId = "me",
        addresseeId = "u-2",
        status = FriendshipStatus.accepted,
        source = FriendshipSource.handle,
        createdAt = Instant.now(),
        otherUserId = "u-2",
        otherHandle = "sara.l",
        otherDisplayName = "Sara Lin",
    )

    private val stubClub = Club(
        id = "club-1",
        name = "Riverside Archers",
        inviteCode = "RVSIDE01",
        createdAt = Instant.now(),
        createdBy = "me",
        memberCount = 5,
        myRole = ClubRole.host,
    )

    private val stubLeague = League(
        id = "lg-1",
        name = "Spring CMP Weekly",
        hostUserId = "me",
        type = LeagueType.individual,
        divisions = listOf(Division.CMP),
        round = RoundDef(endCount = 10, arrowsPerEnd = 6),
        schedule = LeagueSchedule(
            kind = LeagueScheduleKind.weekly,
            startsAt = Instant.now(),
            endsAt = Instant.now().plusSeconds(7 * 24 * 3600),
            totalWeeks = 8,
        ),
        handicap = HandicapConfig(),
        entryRule = LeagueEntryRule.open,
        inviteCode = "SPRWK001",
        status = LeagueStatus.active,
        createdAt = Instant.now(),
        entryCount = 4,
    )

    private val stubActivityItem = ActivityItem(
        id = "act-1",
        kind = ActivityKind.friend_pr,
        sourceKind = ActivitySourceKind.friend,
        actorHandle = "sara.l",
        actorDisplayName = "Sara Lin",
        title = "Hit a new personal best",
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        every { repository.observeFeed() } returns flowOf(listOf(stubActivityItem))
        every { repository.observeFriends() } returns flowOf(listOf(stubFriendship))
        every { repository.observePendingRequests() } returns flowOf(emptyList())
        every { repository.observeClubs() } returns flowOf(listOf(stubClub))
        every { repository.observeLeagues() } returns flowOf(listOf(stubLeague))

        coEvery { repository.refreshFeed() } returns Unit
        coEvery { repository.refreshFriends() } returns Unit
        coEvery { repository.refreshClubs() } returns Unit
        coEvery { repository.refreshLeagues() } returns Unit
        coEvery { repository.getMyProfile() } returns stubProfile
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true`() = runTest {
        val vm = FeedViewModel(repository)
        // Initial emission before any collection
        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `uiState emits combined data from all repository flows`() = runTest {
        val vm = FeedViewModel(repository)

        vm.uiState.test {
            // Skip initial loading state
            val firstState = awaitItem()
            assertThat(firstState.isLoading).isTrue()

            // Advance so refresh() coroutine runs
            testDispatcher.scheduler.advanceUntilIdle()

            // Collect the state emitted after flows combine
            val loaded = awaitItem()
            assertThat(loaded.friends).hasSize(1)
            assertThat(loaded.friends.first().otherHandle).isEqualTo("sara.l")
            assertThat(loaded.clubs).hasSize(1)
            assertThat(loaded.clubs.first().name).isEqualTo("Riverside Archers")
            assertThat(loaded.leagues).hasSize(1)
            assertThat(loaded.leagues.first().name).isEqualTo("Spring CMP Weekly")
            assertThat(loaded.feed).hasSize(1)
            assertThat(loaded.feed.first().kind).isEqualTo(ActivityKind.friend_pr)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pending requests are separated from accepted friends`() = runTest {
        val pendingFriendship = stubFriendship.copy(
            id = "fr-pending",
            status = FriendshipStatus.pending,
        )
        every { repository.observePendingRequests() } returns flowOf(listOf(pendingFriendship))

        val vm = FeedViewModel(repository)

        vm.uiState.test {
            skipItems(1) // initial
            testDispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()

            assertThat(state.friends).hasSize(1)
            assertThat(state.friends.first().status).isEqualTo(FriendshipStatus.accepted)
            assertThat(state.pendingRequests).hasSize(1)
            assertThat(state.pendingRequests.first().status).isEqualTo(FriendshipStatus.pending)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
