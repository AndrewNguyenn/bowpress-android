package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.social.PhotoDownscaler
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
    private lateinit var photoDownscaler: PhotoDownscaler

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
        photoDownscaler = mockk(relaxed = true)
    }

    private fun viewModel() = FriendSessionDetailViewModel(repository, photoDownscaler)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load resolves a populated detail with ends and arrows`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-1") } returns populatedDetail("ss-1")

        val vm = viewModel()
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

        val vm = viewModel()
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

        val vm = viewModel()
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

        val vm = viewModel()
        vm.load("ss-3")
        testDispatcher.scheduler.advanceUntilIdle()

        val session = vm.uiState.value.detail?.session
        assertThat(session?.targetFaceType).isEqualTo(TargetFaceType.SIX_RING)
        assertThat(session?.targetLayout).isEqualTo(TargetLayout.TRIANGLE)
    }

    // -------------------------------------------------------------------------
    // Social Feed V2 — owner-editable mode (§3, §4)
    // -------------------------------------------------------------------------

    @Test
    fun `load with isOwn flags the owner-editable mode`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-own") } returns populatedDetail("ss-own")

        val vm = viewModel()
        vm.load("ss-own", isOwn = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.isOwn).isTrue()
    }

    @Test
    fun `load without isOwn stays read-only`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-ro") } returns populatedDetail("ss-ro")

        val vm = viewModel()
        vm.load("ss-ro")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.isOwn).isFalse()
    }

    @Test
    fun `saveEdit forwards new and loaded values so the repository can diff`() = runTest {
        // populatedDetail("ss-e") loads a session titled "Pre-comp check".
        coEvery { repository.getSharedSessionDetail("ss-e") } returns populatedDetail("ss-e")
        coEvery {
            repository.editSharedSession(any(), any(), any(), any(), any(), any(), any())
        } returns sharedSession("ss-e")

        val vm = viewModel()
        vm.load("ss-e", isOwn = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val place = com.andrewnguyen.bowpress.core.model.SessionLocation(
            name = "Riverside Range", latitude = 1.0, longitude = 2.0,
        )
        vm.saveEdit(title = "  Comp prep  ", description = "Felt sharp @sarah.n", location = place)
        testDispatcher.scheduler.advanceUntilIdle()

        // The ViewModel passes the new values plus the values it loaded; the
        // repository does the change-diff + trimming.
        coVerify {
            repository.editSharedSession(
                sharedSessionId = "ss-e",
                newTitle = "  Comp prep  ",
                newDescription = "Felt sharp @sarah.n",
                newLocation = place,
                originalTitle = "Pre-comp check",
                originalDescription = null,
                originalLocation = null,
            )
        }
        // A reload pulls the recomputed detail back.
        coVerify(atLeast = 2) { repository.getSharedSessionDetail("ss-e") }
        assertThat(vm.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `addPhotos downscales and uploads each picked uri`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-p") } returns populatedDetail("ss-p")
        val uriA = mockk<android.net.Uri>()
        val uriB = mockk<android.net.Uri>()
        coEvery { photoDownscaler.downscaleToJpeg(uriA) } returns byteArrayOf(1, 2, 3)
        coEvery { photoDownscaler.downscaleToJpeg(uriB) } returns byteArrayOf(4, 5, 6)
        coEvery { repository.uploadSharedSessionPhoto(any(), any()) } returns mockk(relaxed = true)
        coEvery { repository.listSharedSessionPhotos("ss-p") } returns emptyList()

        val vm = viewModel()
        vm.load("ss-p", isOwn = true)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPhotos(listOf(uriA, uriB))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.uploadSharedSessionPhoto("ss-p", byteArrayOf(1, 2, 3)) }
        coVerify { repository.uploadSharedSessionPhoto("ss-p", byteArrayOf(4, 5, 6)) }
        assertThat(vm.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `addPhotos skips a uri that cannot be downscaled and flags the error`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-bad") } returns populatedDetail("ss-bad")
        val bad = mockk<android.net.Uri>()
        coEvery { photoDownscaler.downscaleToJpeg(bad) } returns null
        coEvery { repository.listSharedSessionPhotos("ss-bad") } returns emptyList()

        val vm = viewModel()
        vm.load("ss-bad", isOwn = true)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addPhotos(listOf(bad))
        testDispatcher.scheduler.advanceUntilIdle()

        // The unreadable pick is skipped — no upload attempted.
        coVerify(exactly = 0) { repository.uploadSharedSessionPhoto(any(), any()) }
        assertThat(vm.uiState.value.error).isNotNull()
    }

    @Test
    fun `removePhoto deletes the photo and refreshes the gallery`() = runTest {
        coEvery { repository.getSharedSessionDetail("ss-rm") } returns populatedDetail("ss-rm")
        coEvery { repository.deleteSharedSessionPhoto(any(), any()) } returns Unit
        coEvery { repository.listSharedSessionPhotos("ss-rm") } returns emptyList()

        val vm = viewModel()
        vm.load("ss-rm", isOwn = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val photo = com.andrewnguyen.bowpress.core.model.ActivityPhoto(
            id = "photo-9",
            status = com.andrewnguyen.bowpress.core.model.PhotoStatus.ready,
            position = 0,
        )
        vm.removePhoto(photo)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteSharedSessionPhoto("ss-rm", "photo-9") }
        coVerify { repository.listSharedSessionPhotos("ss-rm") }
    }
}
