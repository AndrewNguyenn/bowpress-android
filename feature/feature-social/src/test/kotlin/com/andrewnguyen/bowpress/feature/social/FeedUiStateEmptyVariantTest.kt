package com.andrewnguyen.bowpress.feature.social

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
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedEmptyVariant
import com.andrewnguyen.bowpress.feature.social.ui.feed.FeedUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Pure unit tests for [FeedUiState.emptyVariant] — no coroutines needed; the
 * property is a synchronous derived value from the state's list fields.
 */
class FeedUiStateEmptyVariantTest {

    private val oneItem = ActivityItem(
        id = "act-1",
        kind = ActivityKind.friend_pr,
        sourceKind = ActivitySourceKind.friend,
        actorHandle = "sara.l",
        actorDisplayName = "Sara Lin",
        title = "Hit a new personal best",
        createdAt = Instant.now(),
    )

    private val oneFriend = Friendship(
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

    private val oneClub = Club(
        id = "club-1",
        name = "Riverside Archers",
        inviteCode = "RVSIDE01",
        createdAt = Instant.now(),
        createdBy = "me",
        memberCount = 5,
        myRole = ClubRole.host,
    )

    private val oneLeague = League(
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

    // ---- null when loading or feed non-empty --------------------------------

    @Test
    fun `emptyVariant is null while isLoading regardless of list contents`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = emptyList(),
            clubs = emptyList(),
            leagues = emptyList(),
            isLoading = true,
        )
        assertThat(state.emptyVariant).isNull()
    }

    @Test
    fun `emptyVariant is null when feed has items even if no social connections`() {
        val state = FeedUiState(
            feed = listOf(oneItem),
            friends = emptyList(),
            clubs = emptyList(),
            leagues = emptyList(),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isNull()
    }

    // ---- NewUser branch -----------------------------------------------------

    @Test
    fun `emptyVariant is NewUser when empty feed and no friends, clubs, or leagues`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = emptyList(),
            clubs = emptyList(),
            leagues = emptyList(),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isEqualTo(FeedEmptyVariant.NewUser)
    }

    // ---- QuietWeek branch — any connection flips it -------------------------

    @Test
    fun `emptyVariant is QuietWeek when empty feed but has a friend`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = listOf(oneFriend),
            clubs = emptyList(),
            leagues = emptyList(),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isEqualTo(FeedEmptyVariant.QuietWeek)
    }

    @Test
    fun `emptyVariant is QuietWeek when empty feed but belongs to a club`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = emptyList(),
            clubs = listOf(oneClub),
            leagues = emptyList(),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isEqualTo(FeedEmptyVariant.QuietWeek)
    }

    @Test
    fun `emptyVariant is QuietWeek when empty feed but is in a league`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = emptyList(),
            clubs = emptyList(),
            leagues = listOf(oneLeague),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isEqualTo(FeedEmptyVariant.QuietWeek)
    }

    @Test
    fun `emptyVariant is QuietWeek when empty feed and has all of friends, clubs, leagues`() {
        val state = FeedUiState(
            feed = emptyList(),
            friends = listOf(oneFriend),
            clubs = listOf(oneClub),
            leagues = listOf(oneLeague),
            isLoading = false,
        )
        assertThat(state.emptyVariant).isEqualTo(FeedEmptyVariant.QuietWeek)
    }
}
