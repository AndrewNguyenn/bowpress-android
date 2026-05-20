package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.entities.InvitationEntity
import com.andrewnguyen.bowpress.core.model.AcceptInvitationBody
import com.andrewnguyen.bowpress.core.model.InvitationKind
import com.andrewnguyen.bowpress.core.model.InvitationStatus
import com.andrewnguyen.bowpress.core.model.SendInvitationBody
import com.andrewnguyen.bowpress.core.model.SocialInvitation
import com.andrewnguyen.bowpress.core.model.SocialPendingCount
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
 * Covers the §11 invitation flow and the §12 pending-count aggregation on
 * [SocialRepository] — including the Room fallback that keeps the badge
 * working when the API is unreachable (DEBUG / offline).
 */
class SocialRepositoryInvitationTest {

    private lateinit var api: BowPressApi
    private lateinit var profileDao: SocialProfileDao
    private lateinit var friendshipDao: FriendshipDao
    private lateinit var clubDao: ClubDao
    private lateinit var feedDao: ActivityFeedDao
    private lateinit var leagueDao: LeagueDao
    private lateinit var invitationDao: InvitationDao
    private lateinit var blockDao: BlockDao
    private lateinit var achievementDao: AchievementDao
    private lateinit var repo: SocialRepository

    private fun invite(id: String, kind: InvitationKind) = SocialInvitation(
        id = id,
        kind = kind,
        targetId = "t-$id",
        targetName = "Target $id",
        inviterUserId = "u-host",
        inviterHandle = "host.h",
        inviteeUserId = "me",
        status = InvitationStatus.pending,
        createdAt = Instant.now(),
    )

    private fun inviteEntity(id: String) = InvitationEntity(
        id = id,
        kind = "club",
        targetId = "t-$id",
        targetName = "Target $id",
        inviterUserId = "u-host",
        inviterHandle = "host.h",
        inviteeUserId = "me",
        status = "pending",
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        profileDao = mockk(relaxed = true)
        friendshipDao = mockk(relaxed = true)
        clubDao = mockk(relaxed = true)
        feedDao = mockk(relaxed = true)
        leagueDao = mockk(relaxed = true)
        invitationDao = mockk(relaxed = true)
        blockDao = mockk(relaxed = true)
        achievementDao = mockk(relaxed = true)
        repo = SocialRepository(
            api, profileDao, friendshipDao, clubDao, feedDao, leagueDao,
            invitationDao, blockDao, achievementDao,
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
        )
    }

    @Test
    fun `getInvitations replaces the Room cache on API success`() = runTest {
        val remote = listOf(invite("c1", InvitationKind.club), invite("l1", InvitationKind.league))
        coEvery { api.getInvitations() } returns remote

        val result = repo.getInvitations()

        assertThat(result.map { it.id }).containsExactly("c1", "l1")
        coVerify { invitationDao.clear() }
        coVerify { invitationDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `getInvitations falls back to the Room cache on API failure`() = runTest {
        coEvery { api.getInvitations() } throws RuntimeException("offline")
        coEvery { invitationDao.getAll() } returns listOf(inviteEntity("cached1"))

        val result = repo.getInvitations()

        assertThat(result.map { it.id }).containsExactly("cached1")
        // Cache must not be cleared when the network call failed.
        coVerify(exactly = 0) { invitationDao.clear() }
    }

    @Test
    fun `acceptInvitation calls the API and drops the cached row`() = runTest {
        coEvery { api.acceptInvitation("inv1", AcceptInvitationBody(null)) } returns
            invite("inv1", InvitationKind.club)

        repo.acceptInvitation("inv1")

        coVerify { api.acceptInvitation("inv1", AcceptInvitationBody(null)) }
        coVerify { invitationDao.deleteById("inv1") }
    }

    @Test
    fun `declineInvitation calls the API and drops the cached row`() = runTest {
        coEvery { api.declineInvitation("inv2") } returns Unit

        repo.declineInvitation("inv2")

        coVerify { api.declineInvitation("inv2") }
        coVerify { invitationDao.deleteById("inv2") }
    }

    @Test
    fun `inviteToClub posts the handle body`() = runTest {
        coEvery { api.inviteToClub("club1", SendInvitationBody("sarah.n")) } returns
            invite("inv3", InvitationKind.club)

        val result = repo.inviteToClub("club1", "sarah.n")

        assertThat(result.id).isEqualTo("inv3")
        coVerify { api.inviteToClub("club1", SendInvitationBody("sarah.n")) }
    }

    @Test
    fun `getPendingCount returns the API value when the call succeeds`() = runTest {
        coEvery { api.getPendingCount() } returns SocialPendingCount(
            friendRequests = 2, invitations = 3, total = 5,
        )

        val count = repo.getPendingCount()

        assertThat(count.total).isEqualTo(5)
        assertThat(count.friendRequests).isEqualTo(2)
        assertThat(count.invitations).isEqualTo(3)
    }

    @Test
    fun `getPendingCount aggregates from Room when the API fails`() = runTest {
        coEvery { api.getPendingCount() } throws RuntimeException("fake dev token")
        coEvery { friendshipDao.incomingPendingCount() } returns 2
        coEvery { invitationDao.pendingCount() } returns 3

        val count = repo.getPendingCount()

        // friendRequests + invitations = total
        assertThat(count.friendRequests).isEqualTo(2)
        assertThat(count.invitations).isEqualTo(3)
        assertThat(count.total).isEqualTo(5)
    }
}
