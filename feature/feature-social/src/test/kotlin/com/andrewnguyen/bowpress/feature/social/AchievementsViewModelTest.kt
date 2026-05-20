package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementsViewModel
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
class AchievementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    private fun achievement(id: String, kind: AchievementKind) = Achievement(
        id = id,
        userId = "u-1",
        sharedSessionId = "ss-$id",
        kind = kind,
        label = "Label $id",
        value = 100,
        sublabel = "sub",
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMine populates the trophy case from getMyAchievements`() = runTest {
        coEvery { repository.getMyAchievements() } returns listOf(
            achievement("a1", AchievementKind.score_pr),
            achievement("a2", AchievementKind.streak),
        )

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.achievements.map { it.id }).containsExactly("a1", "a2")
        coVerify { repository.getMyAchievements() }
    }

    @Test
    fun `loadForFriend fetches the friend's achievements`() = runTest {
        coEvery { repository.getFriendAchievements("u-9") } returns listOf(
            achievement("f1", AchievementKind.x_pr),
        )

        val vm = AchievementsViewModel(repository)
        vm.loadForFriend("u-9")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.achievements.map { it.id }).containsExactly("f1")
        coVerify { repository.getFriendAchievements("u-9") }
    }

    @Test
    fun `an empty trophy case is a valid loaded state`() = runTest {
        coEvery { repository.getMyAchievements() } returns emptyList()

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.achievements).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `a fetch failure surfaces the error and stops loading`() = runTest {
        coEvery { repository.getMyAchievements() } throws RuntimeException("network down")

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.error).isEqualTo("network down")
        assertThat(state.isLoading).isFalse()
        assertThat(state.achievements).isEmpty()
    }
}
