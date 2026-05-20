package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.ArrowPlotDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SessionDao
import com.andrewnguyen.bowpress.core.database.dao.SessionEndDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.entities.ActivityItemEntity
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Covers the §16 friend session detail fetch on [SocialRepository] — the API
 * path and the offline/DEBUG fallback that assembles the detail from the
 * cached activity feed + local session/ends/arrows tables.
 */
class SocialRepositorySessionDetailTest {

    private lateinit var api: BowPressApi
    private lateinit var feedDao: ActivityFeedDao
    private lateinit var sessionDao: SessionDao
    private lateinit var sessionEndDao: SessionEndDao
    private lateinit var plotDao: ArrowPlotDao
    private lateinit var repo: SocialRepository

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        feedDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        sessionEndDao = mockk(relaxed = true)
        plotDao = mockk(relaxed = true)
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
            sessionDao,
            sessionEndDao,
            plotDao,
        )
    }

    private fun sharedSessionDetail() = SharedSessionDetail(
        sharedSession = SharedSession(
            id = "ss-1",
            userId = "u-1",
            sessionId = "sess-1",
            score = 196,
            xCount = 9,
            arrowCount = 18,
            distance = "50m",
            face = "10-Ring",
            shotAt = Instant.now(),
            createdAt = Instant.now(),
        ),
        ownerHandle = "sara.l",
        ownerDisplayName = "Sara Lin",
        session = null,
        ends = emptyList(),
        arrows = emptyList(),
    )

    /** A feed-row entity carrying a §15 ActivitySession JSON payload. */
    private fun feedRowEntity(sharedSessionId: String, sessionId: String) = ActivityItemEntity(
        id = "act-1",
        kind = "friend_pr",
        sourceKind = "friend",
        actorHandle = "sara.l",
        actorDisplayName = "Sara Lin",
        title = "Shared a session",
        createdAt = Instant.now(),
        sessionJson = json.encodeToString(
            com.andrewnguyen.bowpress.core.model.ActivitySession(
                sharedSessionId = sharedSessionId,
                sessionId = sessionId,
                score = 196,
                xCount = 9,
                arrowCount = 18,
                distance = "50m",
                face = "10-Ring",
            ),
        ),
        highlighted = true,
    )

    @Test
    fun `getSharedSessionDetail returns the API result when the call succeeds`() = runTest {
        coEvery { api.getSharedSessionDetail("ss-1") } returns sharedSessionDetail()

        val result = repo.getSharedSessionDetail("ss-1")

        assertThat(result.ownerHandle).isEqualTo("sara.l")
        assertThat(result.sharedSession.score).isEqualTo(196)
        coVerify { api.getSharedSessionDetail("ss-1") }
    }

    @Test
    fun `getSharedSessionDetail falls back to the cached feed + local session`() = runTest {
        coEvery { api.getSharedSessionDetail("ss-1") } throws RuntimeException("offline")
        coEvery { feedDao.getAll() } returns listOf(feedRowEntity("ss-1", "sess-1"))
        coEvery { sessionDao.findById("sess-1") } returns null // session lookup
        coEvery { sessionEndDao.findBySession(any()) } returns emptyList()
        coEvery { plotDao.findBySession(any()) } returns emptyList()

        val result = repo.getSharedSessionDetail("ss-1")

        // The stat summary is reconstructed from the cached ActivitySession.
        assertThat(result.sharedSession.sharedSessionId()).isEqualTo("ss-1")
        assertThat(result.sharedSession.score).isEqualTo(196)
        assertThat(result.ownerHandle).isEqualTo("sara.l")
        assertThat(result.ownerDisplayName).isEqualTo("Sara Lin")
    }

    @Test
    fun `fallback throws the original error when the feed has no matching row`() = runTest {
        val boom = RuntimeException("offline")
        coEvery { api.getSharedSessionDetail("ss-unknown") } throws boom
        coEvery { feedDao.getAll() } returns emptyList()

        val thrown = runCatching { repo.getSharedSessionDetail("ss-unknown") }.exceptionOrNull()

        assertThat(thrown).isEqualTo(boom)
    }
}

/** `SharedSession.id` is the shared-session id — local alias for test clarity. */
private fun SharedSession.sharedSessionId(): String = id
