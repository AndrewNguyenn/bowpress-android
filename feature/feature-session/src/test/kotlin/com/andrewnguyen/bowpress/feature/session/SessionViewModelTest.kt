package com.andrewnguyen.bowpress.feature.session

import android.app.Application
import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionSetupPreferencesRepository
import com.andrewnguyen.bowpress.core.data.social.SocialSessionSharer
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowType
import com.andrewnguyen.bowpress.core.model.SessionEnd
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import com.andrewnguyen.bowpress.core.model.TargetFaceType
import com.andrewnguyen.bowpress.core.model.TargetLayout
import com.andrewnguyen.bowpress.core.model.Zone
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
            app = mockk<Application>(relaxed = true),
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

    // ---- Live-session ends ----

    private val activeSession = ShootingSession(
        id = "s1", bowId = "bow-compound", bowConfigId = "bc1", arrowConfigId = "ac1",
        startedAt = Instant.parse("2026-05-21T10:00:00Z"),
    )

    private fun arrow(id: String, ring: Int, endId: String?, offsetMin: Long): ArrowPlot =
        ArrowPlot(
            id = id, sessionId = "s1", bowConfigId = "bc1", arrowConfigId = "ac1",
            ring = ring, zone = Zone.CENTER, plotX = 0.0, plotY = 0.0, endId = endId,
            shotAt = activeSession.startedAt.plusSeconds(offsetMin * 60),
        )

    /**
     * A view model bound to an active session, with controllable plot + end
     * streams so the ends behaviour can be driven and verified.
     */
    private class ActiveVm(
        val vm: SessionViewModel,
        val plots: MutableStateFlow<List<ArrowPlot>>,
        val ends: MutableStateFlow<List<SessionEnd>>,
        val sessionEndRepo: SessionEndRepository,
        val plotRepo: PlotRepository,
        val sessionRepo: SessionRepository,
    )

    private fun activeViewModel(initialPlots: List<ArrowPlot>): ActiveVm {
        val plots = MutableStateFlow(initialPlots)
        val ends = MutableStateFlow<List<SessionEnd>>(emptyList())
        val bowRepo = mockk<BowRepository>(relaxed = true) {
            every { observeBows() } returns flowOf(emptyList())
        }
        val arrowConfigRepo = mockk<ArrowConfigRepository>(relaxed = true) {
            every { observeAll() } returns flowOf(emptyList())
        }
        val sessionRepo = mockk<SessionRepository>(relaxed = true) {
            every { observeActiveSession() } returns flowOf(activeSession)
        }
        val plotRepo = mockk<PlotRepository>(relaxed = true) {
            every { observeBySession(any()) } returns plots
        }
        val sessionEndRepo = mockk<SessionEndRepository>(relaxed = true) {
            every { observeBySession(any()) } returns ends
        }
        val setupPrefs = mockk<SessionSetupPreferencesRepository>(relaxed = true) {
            every { lastTargetLayout } returns flowOf(TargetLayout.SINGLE)
        }
        val vm = SessionViewModel(
            app = mockk<Application>(relaxed = true),
            bowRepo = bowRepo,
            arrowConfigRepo = arrowConfigRepo,
            bowConfigRepo = mockk<BowConfigRepository>(relaxed = true),
            sessionRepo = sessionRepo,
            plotRepo = plotRepo,
            sessionEndRepo = sessionEndRepo,
            socialSessionSharer = mockk<SocialSessionSharer>(relaxed = true),
            sessionSetupPrefs = setupPrefs,
        )
        return ActiveVm(vm, plots, ends, sessionEndRepo, plotRepo, sessionRepo)
    }

    @Test
    fun `completeEnd records end 1 and stamps every in-progress arrow with its id`() =
        runTest(testDispatcher) {
            val plots = listOf(
                arrow("a1", 10, endId = null, offsetMin = 0),
                arrow("a2", 11, endId = null, offsetMin = 1),
                arrow("a3", 9, endId = null, offsetMin = 2),
            )
            val h = activeViewModel(plots)
            runCurrent()
            // All three arrows are the in-progress end before completion.
            assertThat(h.vm.uiState.value.currentEndArrows).hasSize(3)
            assertThat(h.vm.uiState.value.currentEndNumber).isEqualTo(1)

            val endSlot = slot<SessionEnd>()
            val stamped = mutableListOf<ArrowPlot>()
            io.mockk.coEvery { h.sessionEndRepo.saveEnd(capture(endSlot)) } returns Unit
            io.mockk.coEvery { h.plotRepo.savePlot(capture(stamped)) } returns Unit

            h.vm.completeEnd()
            runCurrent()

            // One SessionEnd recorded with the next end number.
            assertThat(endSlot.captured.endNumber).isEqualTo(1)
            assertThat(endSlot.captured.sessionId).isEqualTo("s1")
            // Every in-progress arrow re-saved, stamped with that end's id.
            assertThat(stamped.map { it.id }).containsExactly("a1", "a2", "a3")
            assertThat(stamped.all { it.endId == endSlot.captured.id }).isTrue()
        }

    @Test
    fun `completeEnd is a no-op when the in-progress end is empty`() =
        runTest(testDispatcher) {
            val h = activeViewModel(initialPlots = emptyList())
            runCurrent()
            h.vm.completeEnd()
            runCurrent()
            coVerify(exactly = 0) { h.sessionEndRepo.saveEnd(any()) }
        }

    @Test
    fun `deleteEnd routes through the repository for the active session`() =
        runTest(testDispatcher) {
            val h = activeViewModel(initialPlots = emptyList())
            runCurrent()
            h.vm.deleteEnd("e1")
            runCurrent()
            coVerify { h.sessionRepo.deleteEnd(sessionId = "s1", endId = "e1") }
        }

    @Test
    fun `removeLastArrow only drops the last in-progress arrow, never a completed end`() =
        runTest(testDispatcher) {
            // a1 belongs to a completed end e1; a2 is in-progress.
            val h = activeViewModel(
                listOf(
                    arrow("a1", 10, endId = "e1", offsetMin = 0),
                    arrow("a2", 9, endId = null, offsetMin = 1),
                ),
            )
            h.ends.value = listOf(
                SessionEnd("e1", "s1", 1, completedAt = activeSession.startedAt),
            )
            runCurrent()

            val deleted = mutableListOf<ArrowPlot>()
            io.mockk.coEvery { h.plotRepo.deletePlot(capture(deleted)) } returns Unit
            h.vm.removeLastArrow()
            runCurrent()
            // Only the in-progress a2 is dropped — a1 (in a completed end) is safe.
            assertThat(deleted.map { it.id }).containsExactly("a2")
        }

    // ---- Store-backed harness — mock repos that actually "persist" so the
    // end-of-session auto-complete can be verified against persisted state. ----

    /** A view model whose plot + end repos accumulate saves like a real store. */
    private class StoreBackedVm(
        val vm: SessionViewModel,
        /** Every ArrowPlot row in the "store", latest write per id wins. */
        val persistedPlots: Map<String, ArrowPlot>,
        /** Every SessionEnd row in the "store". */
        val persistedEnds: List<SessionEnd>,
    )

    private fun storeBackedViewModel(
        initialPlots: List<ArrowPlot>,
        initialEnds: List<SessionEnd> = emptyList(),
    ): () -> StoreBackedVm {
        // Mutable "tables". savePlot upserts by id; saveEnd appends.
        val plotTable = LinkedHashMap<String, ArrowPlot>()
        initialPlots.forEach { plotTable[it.id] = it }
        val endTable = initialEnds.toMutableList()
        val plots = MutableStateFlow(plotTable.values.toList())
        val ends = MutableStateFlow(endTable.toList())

        val sessionRepo = mockk<SessionRepository>(relaxed = true) {
            every { observeActiveSession() } returns flowOf(activeSession)
        }
        val plotRepo = mockk<PlotRepository>(relaxed = true) {
            every { observeBySession(any()) } returns plots
            io.mockk.coEvery { savePlot(any()) } answers {
                val p = firstArg<ArrowPlot>()
                plotTable[p.id] = p
                plots.value = plotTable.values.toList()
            }
        }
        val sessionEndRepo = mockk<SessionEndRepository>(relaxed = true) {
            every { observeBySession(any()) } returns ends
            io.mockk.coEvery { saveEnd(any()) } answers {
                endTable += firstArg<SessionEnd>()
                ends.value = endTable.toList()
            }
        }
        val vm = SessionViewModel(
            app = mockk<Application>(relaxed = true),
            bowRepo = mockk<BowRepository>(relaxed = true) {
                every { observeBows() } returns flowOf(emptyList())
            },
            arrowConfigRepo = mockk<ArrowConfigRepository>(relaxed = true) {
                every { observeAll() } returns flowOf(emptyList())
            },
            bowConfigRepo = mockk<BowConfigRepository>(relaxed = true),
            sessionRepo = sessionRepo,
            plotRepo = plotRepo,
            sessionEndRepo = sessionEndRepo,
            socialSessionSharer = mockk<SocialSessionSharer>(relaxed = true),
            sessionSetupPrefs = mockk<SessionSetupPreferencesRepository>(relaxed = true) {
                every { lastTargetLayout } returns flowOf(TargetLayout.SINGLE)
            },
        )
        return { StoreBackedVm(vm, plotTable.toMap(), endTable.toList()) }
    }

    @Test
    fun `endSession auto-completes a trailing in-progress end — persisted end recorded, every plot stamped`() =
        runTest(testDispatcher) {
            // Three arrows plotted into the in-progress end; the archer never
            // tapped "Finish End" before hitting Finish.
            val snapshot = storeBackedViewModel(
                listOf(
                    arrow("a1", 10, endId = null, offsetMin = 0),
                    arrow("a2", 11, endId = null, offsetMin = 1),
                    arrow("a3", 9, endId = null, offsetMin = 2),
                ),
            )
            val h = snapshot()
            runCurrent()
            h.vm.endSession(notes = "", feelTags = emptyList())
            runCurrent()

            val state = snapshot()
            // (a) The trailing end is persisted — one SessionEnd recorded.
            assertThat(state.persistedEnds).hasSize(1)
            assertThat(state.persistedEnds.single().endNumber).isEqualTo(1)
            // (b) Every persisted ArrowPlot row carries a non-null endId,
            //     pointing at that recorded end.
            val endId = state.persistedEnds.single().id
            assertThat(state.persistedPlots).hasSize(3)
            assertThat(state.persistedPlots.values.all { it.endId == endId }).isTrue()
        }

    @Test
    fun `endSession with an already-finished last end records no extra end`() =
        runTest(testDispatcher) {
            // All arrows already belong to completed end e1 — nothing trailing.
            val e1 = SessionEnd("e1", "s1", 1, completedAt = activeSession.startedAt)
            val snapshot = storeBackedViewModel(
                initialPlots = listOf(
                    arrow("a1", 10, endId = "e1", offsetMin = 0),
                    arrow("a2", 11, endId = "e1", offsetMin = 1),
                ),
                initialEnds = listOf(e1),
            )
            val h = snapshot()
            runCurrent()
            h.vm.endSession(notes = "", feelTags = emptyList())
            runCurrent()
            // The in-progress end was empty → endSession records no new end;
            // only the pre-existing e1 remains.
            assertThat(snapshot().persistedEnds.map { it.id }).containsExactly("e1")
        }

    @Test
    fun `completeEnd is idempotent — a concurrent double-invoke records only one end`() =
        runTest(testDispatcher) {
            // saveEnd suspends (yield) so the first completeEnd is genuinely
            // in-flight — past its isEmpty() check, holding `completingEnd` —
            // when the second is dispatched. This is the real double-tap
            // race: two click handlers each launch into viewModelScope.
            val plotTable = LinkedHashMap<String, ArrowPlot>().apply {
                put("a1", arrow("a1", 10, endId = null, offsetMin = 0))
                put("a2", arrow("a2", 11, endId = null, offsetMin = 1))
            }
            val endTable = mutableListOf<SessionEnd>()
            val plots = MutableStateFlow(plotTable.values.toList())
            val ends = MutableStateFlow<List<SessionEnd>>(emptyList())
            val plotRepo = mockk<PlotRepository>(relaxed = true) {
                every { observeBySession(any()) } returns plots
                io.mockk.coEvery { savePlot(any()) } answers {
                    val p = firstArg<ArrowPlot>()
                    plotTable[p.id] = p
                    plots.value = plotTable.values.toList()
                }
            }
            val sessionEndRepo = mockk<SessionEndRepository>(relaxed = true) {
                every { observeBySession(any()) } returns ends
                io.mockk.coEvery { saveEnd(any()) } coAnswers {
                    kotlinx.coroutines.yield()  // a real suspension point
                    endTable += firstArg<SessionEnd>()
                    ends.value = endTable.toList()
                }
            }
            val vm = SessionViewModel(
                app = mockk<Application>(relaxed = true),
                bowRepo = mockk<BowRepository>(relaxed = true) {
                    every { observeBows() } returns flowOf(emptyList())
                },
                arrowConfigRepo = mockk<ArrowConfigRepository>(relaxed = true) {
                    every { observeAll() } returns flowOf(emptyList())
                },
                bowConfigRepo = mockk<BowConfigRepository>(relaxed = true),
                sessionRepo = mockk<SessionRepository>(relaxed = true) {
                    every { observeActiveSession() } returns flowOf(activeSession)
                },
                plotRepo = plotRepo,
                sessionEndRepo = sessionEndRepo,
                socialSessionSharer = mockk<SocialSessionSharer>(relaxed = true),
                sessionSetupPrefs = mockk<SessionSetupPreferencesRepository>(relaxed = true) {
                    every { lastTargetLayout } returns flowOf(TargetLayout.SINGLE)
                },
            )
            runCurrent()
            // Two click handlers, each launching completeEnd concurrently.
            launch { vm.completeEnd() }
            launch { vm.completeEnd() }
            runCurrent()
            // The synchronous `completingEnd` guard drops the second — only
            // one SessionEnd is recorded.
            assertThat(endTable).hasSize(1)
        }
}
