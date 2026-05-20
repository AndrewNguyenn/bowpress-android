package com.andrewnguyen.bowpress.feature.social

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.SocialBlock
import com.andrewnguyen.bowpress.feature.social.ui.blocks.BlockViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BlockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    /** Backs `observeBlocks()` so the test can drive the reactive Room stream. */
    private lateinit var blocksFlow: MutableStateFlow<List<SocialBlock>>

    private fun block(
        id: String,
        kind: BlockKind,
        mode: BlockMode,
        targetId: String = "t-$id",
    ) = SocialBlock(
        id = id,
        userId = "me",
        kind = kind,
        targetId = targetId,
        targetName = "Target $id",
        mode = mode,
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        blocksFlow = MutableStateFlow(emptyList())
        every { repository.observeBlocks() } returns blocksFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState groups blocks by kind`() = runTest {
        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        blocksFlow.value = listOf(
            block("a1", BlockKind.archer, BlockMode.mute),
            block("a2", BlockKind.archer, BlockMode.block),
            block("c1", BlockKind.club, BlockMode.mute),
            block("l1", BlockKind.league, BlockMode.mute),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.archerBlocks.map { it.id }).containsExactly("a1", "a2")
        assertThat(state.clubBlocks.map { it.id }).containsExactly("c1")
        assertThat(state.leagueBlocks.map { it.id }).containsExactly("l1")
    }

    @Test
    fun `blockFor resolves the block on a given target`() = runTest {
        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        blocksFlow.value = listOf(
            block("a1", BlockKind.archer, BlockMode.mute, targetId = "u-9"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.blockFor("u-9")?.mode).isEqualTo(BlockMode.mute)
        assertThat(vm.uiState.value.blockFor("u-unknown")).isNull()
    }

    @Test
    fun `mute then list then unmute round-trips through the repository`() = runTest {
        coEvery { repository.createBlock(BlockKind.archer, "u-9", BlockMode.mute) } returns
            block("b1", BlockKind.archer, BlockMode.mute, targetId = "u-9")
        coEvery { repository.deleteBlock("b1") } returns Unit

        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mute — the repository call + a feed refresh.
        vm.setBlock(BlockKind.archer, "u-9", "ryan.k", BlockMode.mute)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.createBlock(BlockKind.archer, "u-9", BlockMode.mute) }

        // The Room cache stream surfaces the new row → the list shows it.
        blocksFlow.value = listOf(block("b1", BlockKind.archer, BlockMode.mute, targetId = "u-9"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.blocks.map { it.id }).containsExactly("b1")

        // Unmute — the repository delete, then the stream clears.
        vm.removeBlock("b1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.deleteBlock("b1") }

        blocksFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.uiState.value.blocks).isEmpty()
    }

    @Test
    fun `setBlock refreshes the feed so a muted actor drops out`() = runTest {
        coEvery { repository.createBlock(BlockKind.club, "club-1", BlockMode.mute) } returns
            block("b2", BlockKind.club, BlockMode.mute, targetId = "club-1")

        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setBlock(BlockKind.club, "club-1", "Metro Indoor", BlockMode.mute)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.refreshFeed() }
    }

    @Test
    fun `blocking an archer goes through createBlock with block mode`() = runTest {
        coEvery { repository.createBlock(BlockKind.archer, "u-9", BlockMode.block) } returns
            block("b3", BlockKind.archer, BlockMode.block, targetId = "u-9")

        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setBlock(BlockKind.archer, "u-9", "rival.h", BlockMode.block)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createBlock(BlockKind.archer, "u-9", BlockMode.block) }
    }

    @Test
    fun `mode change re-posts the same target with the new mode`() = runTest {
        // mute first, then escalate to block — both reach createBlock.
        coEvery { repository.createBlock(BlockKind.archer, "u-9", BlockMode.mute) } returns
            block("b1", BlockKind.archer, BlockMode.mute, targetId = "u-9")
        coEvery { repository.createBlock(BlockKind.archer, "u-9", BlockMode.block) } returns
            block("b1", BlockKind.archer, BlockMode.block, targetId = "u-9")

        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setBlock(BlockKind.archer, "u-9", "rival.h", BlockMode.mute)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.setBlock(BlockKind.archer, "u-9", "rival.h", BlockMode.block)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createBlock(BlockKind.archer, "u-9", BlockMode.mute) }
        coVerify { repository.createBlock(BlockKind.archer, "u-9", BlockMode.block) }
    }

    @Test
    fun `setBlock surfaces the error when the repository fails`() = runTest {
        coEvery { repository.createBlock(BlockKind.archer, "u-9", BlockMode.mute) } throws
            RuntimeException("400 self-block")

        val vm = BlockViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setBlock(BlockKind.archer, "u-9", "rival.h", BlockMode.mute)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.error).isEqualTo("400 self-block")
    }
}
