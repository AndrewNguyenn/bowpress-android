package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.entities.AchievementEntity
import com.andrewnguyen.bowpress.core.database.entities.SocialProfileEntity
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.ShareSessionBody
import com.andrewnguyen.bowpress.core.model.ShareSessionResult
import com.andrewnguyen.bowpress.core.model.SharedSession
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
 * Covers the §15 share-session flow and the trophy-case fetch on
 * [SocialRepository] — including the Room offline fallback that keeps the
 * trophy case populated on a DEBUG fake token.
 */
class SocialRepositoryAchievementTest {

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

    private fun achievement(id: String, userId: String, kind: AchievementKind) = Achievement(
        id = id,
        userId = userId,
        sharedSessionId = "ss-$id",
        kind = kind,
        label = "Label $id",
        value = 100,
        sublabel = "sub",
        createdAt = Instant.now(),
    )

    private fun achievementEntity(id: String, userId: String) = AchievementEntity(
        id = id,
        userId = userId,
        sharedSessionId = "ss-$id",
        kind = "score_pr",
        label = "Label $id",
        value = 100,
        sublabel = "sub",
        createdAt = Instant.now(),
    )

    private fun shareResult(achievements: List<Achievement>) = ShareSessionResult(
        sharedSession = SharedSession(
            id = "ss-1",
            userId = "me",
            sessionId = "sess-1",
            score = 548,
            xCount = 12,
            arrowCount = 60,
            distance = "50m",
            face = "10-Ring",
            title = null,
            shotAt = Instant.now(),
            createdAt = Instant.now(),
        ),
        achievements = achievements,
        activityId = "act-1",
        headline = "New PR",
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
    fun `shareSession posts the body and returns the result`() = runTest {
        val body = ShareSessionBody(
            sessionId = "sess-1", score = 548, xCount = 12, arrowCount = 60,
            distance = "50m", face = "10-Ring",
        )
        coEvery { api.shareSession(body) } returns
            shareResult(listOf(achievement("a1", "me", AchievementKind.score_pr)))

        val result = repo.shareSession(body)

        assertThat(result.sharedSession.sessionId).isEqualTo("sess-1")
        assertThat(result.achievements.map { it.id }).containsExactly("a1")
        coVerify { api.shareSession(body) }
    }

    @Test
    fun `getMyAchievements caches the rows on API success`() = runTest {
        coEvery { api.getMyAchievements() } returns listOf(
            achievement("a1", "me", AchievementKind.score_pr),
            achievement("a2", "me", AchievementKind.streak),
        )

        val result = repo.getMyAchievements()

        assertThat(result.map { it.id }).containsExactly("a1", "a2")
        // Stale rows for "me" are cleared, then the fresh ones cached.
        coVerify { achievementDao.clearForUser("me") }
        coVerify { achievementDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `getMyAchievements falls back to the Room cache on API failure`() = runTest {
        coEvery { api.getMyAchievements() } throws RuntimeException("fake dev token")
        // getMyProfile resolves the user id for the cache lookup — it too
        // falls back to the cached/seeded profile.
        coEvery { api.getSocialProfile() } throws RuntimeException("offline")
        coEvery { profileDao.findAny() } returns SocialProfileEntity(
            userId = "me",
            handle = "andrew.n",
            displayName = "Andrew Nguyen",
            joinedAt = Instant.now(),
            visibility = "friends",
        )
        coEvery { achievementDao.getForUser("me") } returns
            listOf(achievementEntity("cached1", "me"))

        val result = repo.getMyAchievements()

        assertThat(result.map { it.id }).containsExactly("cached1")
        coVerify(exactly = 0) { achievementDao.clearForUser(any()) }
    }

    @Test
    fun `getFriendAchievements caches under the friend's id`() = runTest {
        coEvery { api.getFriendAchievements("u-9") } returns listOf(
            achievement("f1", "u-9", AchievementKind.x_pr),
        )

        val result = repo.getFriendAchievements("u-9")

        assertThat(result.map { it.id }).containsExactly("f1")
        coVerify { achievementDao.clearForUser("u-9") }
    }

    @Test
    fun `getFriendAchievements falls back to the cache on API failure`() = runTest {
        coEvery { api.getFriendAchievements("u-9") } throws RuntimeException("offline")
        coEvery { achievementDao.getForUser("u-9") } returns
            listOf(achievementEntity("cachedF", "u-9"))

        val result = repo.getFriendAchievements("u-9")

        assertThat(result.map { it.id }).containsExactly("cachedF")
    }
}
