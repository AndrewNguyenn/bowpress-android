package com.andrewnguyen.bowpress.feature.session

import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionSetupPreferencesRepository
import com.andrewnguyen.bowpress.core.data.social.SocialSessionSharer
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Behaviour tests for [SessionViewModel] setup-screen state — focused on the
 * layout/face/distance combo sync that keeps a multi-spot Vegas layout from
 * leaking onto an off-combo session.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val compoundBow = Bow(
        id = "bow-compound", userId = "me", name = "Hoyt",
        bowType = BowType.COMPOUND, createdAt = Instant.now(),
    )
    private val recurveBow = Bow(
        id = "bow-recurve", userId = "me", name = "Win&Win",
        bowType = BowType.RECURVE, createdAt = Instant.now(),
    )

    @Before fun setUp() = Dispatchers.setMain(testDispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(): SessionViewModel {
        val bowRepo = mockk<BowRepository>(relaxed = true) {
            every { observeBows() } returns flowOf(emptyList())
        }
        val arrowConfigRepo = mockk<ArrowConfigRepository>(relaxed = true) {
            every { observeAll() } returns flowOf(emptyList())
        }
        val sessionRepo = mockk<SessionRepository>(relaxed = true) {
            every { observeActiveSession() } returns flowOf(null)
        }
        val plotRepo = mockk<PlotRepository>(relaxed = true) {
            every { observeBySession(any()) } returns flowOf(emptyList())
        }
        val setupPrefs = mockk<SessionSetupPreferencesRepository>(relaxed = true) {
            every { lastTargetLayout } returns flowOf(TargetLayout.SINGLE)
        }
        return SessionViewModel(
            bowRepo = bowRepo,
            arrowConfigRepo = arrowConfigRepo,
            bowConfigRepo = mockk<BowConfigRepository>(relaxed = true),
            sessionRepo = sessionRepo,
            plotRepo = plotRepo,
            sessionEndRepo = mockk<SessionEndRepository>(relaxed = true),
            socialSessionSharer = mockk<SocialSessionSharer>(relaxed = true),
            sessionSetupPrefs = setupPrefs,
        )
    }

    @Test
    fun `picking 20yd + 6-ring + triangle, then a recurve bow collapses the layout to single`() =
        runTest(testDispatcher) {
            val vm = newViewModel()
            runCurrent()

            // Compound bow → smart-default 6-ring face; at 20yd that is the
            // multi-spot combo. The face is NOT manually overridden, so the
            // next bow switch is free to re-apply the smart default.
            vm.selectBow(compoundBow)
            vm.selectDistance(ShootingDistance.YARDS_20)
            vm.selectLayout(TargetLayout.TRIANGLE)
            runCurrent()
            assertThat(vm.uiState.value.selectedFaceType).isEqualTo(TargetFaceType.SIX_RING)
            assertThat(vm.uiState.value.selectedLayout).isEqualTo(TargetLayout.TRIANGLE)

            // Switching to a recurve bow flips the smart-default face to
            // 10-ring — which is off-combo for multi-spot, so the stale
            // TRIANGLE layout must collapse to SINGLE.
            vm.selectBow(recurveBow)
            runCurrent()
            assertThat(vm.uiState.value.selectedFaceType).isEqualTo(TargetFaceType.TEN_RING)
            assertThat(vm.uiState.value.selectedLayout).isEqualTo(TargetLayout.SINGLE)
        }

    @Test
    fun `leaving the 20yd + 6-ring combo via distance collapses a stale triangle`() =
        runTest(testDispatcher) {
            val vm = newViewModel()
            runCurrent()

            vm.selectBow(compoundBow)   // smart-default 6-ring face
            vm.selectDistance(ShootingDistance.YARDS_20)
            vm.selectLayout(TargetLayout.VERTICAL)
            runCurrent()
            assertThat(vm.uiState.value.selectedLayout).isEqualTo(TargetLayout.VERTICAL)

            // Moving to 50m is off-combo — the layout collapses to SINGLE.
            vm.selectDistance(ShootingDistance.METERS_50)
            runCurrent()
            assertThat(vm.uiState.value.selectedLayout).isEqualTo(TargetLayout.SINGLE)
        }
}
