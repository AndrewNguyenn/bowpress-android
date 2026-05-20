package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.SharedSession
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.Zone
import com.andrewnguyen.bowpress.feature.social.ui.session.FriendSessionDetailViewModel
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
class FriendSessionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    private fun sharedSession(id: String) = SharedSession(
        id = id,
        userId = "u-1",
        sessionId = "sess-1",
        score = 196,
        xCount = 9,
        arrowCount = 18,
        distance = "50m",
        face = "10-Ring",
        title = "Pre-comp check",
        shotAt = Instant.now(),
        createdAt = Instant.now(),
    )

    private fun populatedDetail(
        id: String,
        faceType: TargetFaceType = TargetFaceType.TEN_RING,
        layout: TargetLayout = TargetLayout.SINGLE,
    ) = SharedSessionDetail(
        sharedSession = sharedSession(id),
        ownerHandle = "sara.l",
        ownerDisplayName = "Sara Lin",
        session = ShootingSession(
            id = "sess-1",
            bowId = "bow-1",
            bowConfigId = "cfg-1",
            arrowConfigId = "arrow-1",
            startedAt = Instant.now(),
            targetFaceType = faceType,
            targetLayout = layout,
        ),
        ends = listOf(
            SessionEnd(id = "end-1", sessionId = "sess-1", endNumber = 1, completedAt = Instant.now()),
        ),
        arrows = listOf(
            ArrowPlot(
                id = "p1", sessionId = "sess-1", bowConfigId = "cfg-1", arrowConfigId = "arrow-1",
                ring = 11, zone = Zone.CENTER, plotX = 0.01, plotY = -0.02, endId = "end-1",
                shotAt = Instant.now(),
            ),
        ),
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
    fun `load resolves a populated detail with ends and arrows`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-1") } returns populatedDetail("ss-1")

        val vm = FriendSessionDetailViewModel(repository)
        vm.load("ss-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.detail?.ownerHandle).isEqualTo("sara.l")
        assertThat(state.detail?.session).isNotNull()
        assertThat(state.detail?.ends).hasSize(1)
        assertThat(state.detail?.arrows).hasSize(1)
        coVerify { repository.getSharedSessionDetail("ss-1") }
    }

    @Test
    fun `load resolves a deleted-session detail with the stat summary only`() = runTest {
        // Owner deleted the underlying session — session/ends/arrows empty.
        coEvery { repository.getSharedSessionDetail("ss-2") } returns SharedSessionDetail(
            sharedSession = sharedSession("ss-2"),
            ownerHandle = "marcus.t",
            ownerDisplayName = "Marcus T",
            session = null,
        )

        val vm = FriendSessionDetailViewModel(repository)
        vm.load("ss-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.detail?.session).isNull()
        assertThat(state.detail?.ends).isEmpty()
        assertThat(state.detail?.arrows).isEmpty()
        // Stat summary still resolves.
        assertThat(state.detail?.sharedSession?.score).isEqualTo(196)
    }

    @Test
    fun `a fetch failure surfaces the error and stops loading`() = runTest {
        coEvery { repository.getSharedSessionDetail(any()) } throws RuntimeException("not found")

        val vm = FriendSessionDetailViewModel(repository)
        vm.load("ss-missing")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.error).isEqualTo("not found")
        assertThat(state.isLoading).isFalse()
        assertThat(state.detail).isNull()
    }

    @Test
    fun `the session's target face type and layout flow through to uiState`() = runTest {
        // A 6-ring inner face shot on a 3-spot Vegas triangle.
        coEvery { repository.getSharedSessionDetail("ss-3") } returns populatedDetail(
            id = "ss-3",
            faceType = TargetFaceType.SIX_RING,
            layout = TargetLayout.TRIANGLE,
        )

        val vm = FriendSessionDetailViewModel(repository)
        vm.load("ss-3")
        testDispatcher.scheduler.advanceUntilIdle()

        val session = vm.uiState.value.detail?.session
        assertThat(session?.targetFaceType).isEqualTo(TargetFaceType.SIX_RING)
        assertThat(session?.targetLayout).isEqualTo(TargetLayout.TRIANGLE)
    }
}
