package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.database.dao.AchievementDao
import com.andrewnguyen.bowpress.core.database.dao.ActivityFeedDao
import com.andrewnguyen.bowpress.core.database.dao.BlockDao
import com.andrewnguyen.bowpress.core.database.dao.ClubDao
import com.andrewnguyen.bowpress.core.database.dao.FriendshipDao
import com.andrewnguyen.bowpress.core.database.dao.InvitationDao
import com.andrewnguyen.bowpress.core.database.dao.LeagueDao
import com.andrewnguyen.bowpress.core.database.dao.SocialProfileDao
import com.andrewnguyen.bowpress.core.database.entities.BlockEntity
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.CreateBlockBody
import com.andrewnguyen.bowpress.core.model.SocialBlock
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
 * Covers the §14 mute/block flow on [SocialRepository] — create/update,
 * delete, the Room read cache + offline fallback, and the rule that blocking
 * an archer severs the cached friendship row.
 */
class SocialRepositoryBlockTest {

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

    private fun block(id: String, kind: BlockKind, mode: BlockMode) = SocialBlock(
        id = id,
        userId = "me",
        kind = kind,
        targetId = "t-$id",
        targetName = "Target $id",
        mode = mode,
        createdAt = Instant.now(),
    )

    private fun blockEntity(id: String, mode: String) = BlockEntity(
        id = id,
        userId = "me",
        kind = "archer",
        targetId = "t-$id",
        targetName = "Target $id",
        mode = mode,
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
            mockk(relaxed = true), // photoCache
            mockk(relaxed = true),
        )
    }

    @Test
    fun `getBlocks replaces the Room cache on API success`() = runTest {
        val remote = listOf(
            block("a1", BlockKind.archer, BlockMode.mute),
            block("c1", BlockKind.club, BlockMode.block),
        )
        coEvery { api.getBlocks() } returns remote

        val result = repo.getBlocks()

        assertThat(result.map { it.id }).containsExactly("a1", "c1")
        coVerify { blockDao.clear() }
        coVerify { blockDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `getBlocks falls back to the Room cache on API failure`() = runTest {
        coEvery { api.getBlocks() } throws RuntimeException("offline")
        coEvery { blockDao.getAll() } returns listOf(blockEntity("cached1", "mute"))

        val result = repo.getBlocks()

        assertThat(result.map { it.id }).containsExactly("cached1")
        // Cache must not be cleared when the network call failed.
        coVerify(exactly = 0) { blockDao.clear() }
    }

    @Test
    fun `createBlock mutes an archer and caches the row`() = runTest {
        coEvery {
            api.createBlock(CreateBlockBody(BlockKind.archer, "u-9", BlockMode.mute))
        } returns block("b1", BlockKind.archer, BlockMode.mute)

        repo.createBlock(BlockKind.archer, "u-9", BlockMode.mute)

        coVerify { api.createBlock(CreateBlockBody(BlockKind.archer, "u-9", BlockMode.mute)) }
        coVerify { blockDao.upsert(match { it.id == "b1" }) }
        // A mute does NOT sever the friendship.
        coVerify(exactly = 0) { friendshipDao.deleteByOtherUserId(any()) }
    }

    @Test
    fun `blocking an archer severs the cached friendship row`() = runTest {
        coEvery {
            api.createBlock(CreateBlockBody(BlockKind.archer, "u-9", BlockMode.block))
        } returns block("b2", BlockKind.archer, BlockMode.block)

        repo.createBlock(BlockKind.archer, "u-9", BlockMode.block)

        coVerify { friendshipDao.deleteByOtherUserId("u-9") }
    }

    @Test
    fun `blocking a club does not touch the friendship cache`() = runTest {
        coEvery {
            api.createBlock(CreateBlockBody(BlockKind.club, "club-1", BlockMode.block))
        } returns block("b3", BlockKind.club, BlockMode.block)

        repo.createBlock(BlockKind.club, "club-1", BlockMode.block)

        // Severing only applies to archer blocks.
        coVerify(exactly = 0) { friendshipDao.deleteByOtherUserId(any()) }
    }

    @Test
    fun `createBlock re-post updates the mode`() = runTest {
        // Re-posting an existing target with a new mode — the API upserts and
        // returns the row at its new mode; the repo caches that.
        coEvery {
            api.createBlock(CreateBlockBody(BlockKind.archer, "u-9", BlockMode.block))
        } returns block("b1", BlockKind.archer, BlockMode.block)

        val result = repo.createBlock(BlockKind.archer, "u-9", BlockMode.block)

        assertThat(result.mode).isEqualTo(BlockMode.block)
        coVerify { blockDao.upsert(match { it.mode == "block" }) }
    }

    @Test
    fun `deleteBlock removes the row remotely and from the cache`() = runTest {
        coEvery { api.deleteBlock("b1") } returns Unit

        repo.deleteBlock("b1")

        coVerify { api.deleteBlock("b1") }
        coVerify { blockDao.deleteById("b1") }
    }
}
