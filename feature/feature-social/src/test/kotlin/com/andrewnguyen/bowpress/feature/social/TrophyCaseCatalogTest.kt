package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.TrophyCategory
import com.andrewnguyen.bowpress.core.model.TrophyDef
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementsUiState
import com.andrewnguyen.bowpress.feature.social.ui.achievements.AchievementsViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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

/**
 * Tests for the §18 collectible trophy case:
 * - earned-vs-locked partition logic
 * - "N of 12 collected" distinct-kind count
 * - [AchievementsViewModel] loads catalogue alongside earned achievements
 * - Catalogue fallback when API unavailable
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrophyCaseCatalogTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun achievement(id: String, kind: AchievementKind) = Achievement(
        id = id,
        userId = "u-1",
        sharedSessionId = "ss-$id",
        kind = kind,
        label = "Label $id",
        value = 42,
        sublabel = "sub",
        createdAt = Instant.now(),
    )

    private fun trophyDef(kind: String, category: TrophyCategory = TrophyCategory.skill) = TrophyDef(
        kind = kind,
        name = "Trophy $kind",
        description = "Earn by doing $kind things.",
        tiers = listOf(1, 5, 10),
        category = category,
    )

    private val full12Catalog: List<TrophyDef> = listOf(
        trophyDef("score_pr",          TrophyCategory.skill),
        trophyDef("x_pr",              TrophyCategory.skill),
        trophyDef("flawless",          TrophyCategory.skill),
        trophyDef("sharpshooter",      TrophyCategory.skill),
        trophyDef("arrows_milestone",  TrophyCategory.milestone),
        trophyDef("sessions_milestone",TrophyCategory.milestone),
        trophyDef("marathon",          TrophyCategory.milestone),
        trophyDef("streak",            TrophyCategory.streak),
        trophyDef("weeks_active",      TrophyCategory.streak),
        trophyDef("comeback",          TrophyCategory.streak),
        trophyDef("first_distance",    TrophyCategory.exploration),
        trophyDef("distance_explorer", TrophyCategory.exploration),
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

    // ── Earned-vs-locked partition ────────────────────────────────────────────

    @Test
    fun `earned partition contains only kinds present in the achievement list`() {
        val earned = listOf(
            achievement("a1", AchievementKind.score_pr),
            achievement("a2", AchievementKind.streak),
        )
        val earnedKinds = earned.map { it.kind.name }.toSet()

        val lockedDefs = full12Catalog.filter { it.kind !in earnedKinds }
        val earnedDefs = full12Catalog.filter { it.kind in earnedKinds }

        assertThat(earnedDefs).hasSize(2)
        assertThat(lockedDefs).hasSize(10)
        assertThat(earnedDefs.map { it.kind }).containsExactly("score_pr", "streak")
    }

    @Test
    fun `distinct-kind count equals number of unique earned AchievementKind names`() {
        // Same kind earned twice should only count once in the "N of 12" header.
        val earned = listOf(
            achievement("a1", AchievementKind.score_pr),
            achievement("a2", AchievementKind.score_pr),   // duplicate kind
            achievement("a3", AchievementKind.streak),
            achievement("a4", AchievementKind.first_distance),
        )
        val distinctEarnedKindCount = earned.map { it.kind.name }.toSet().size
        assertThat(distinctEarnedKindCount).isEqualTo(3)
    }

    @Test
    fun `all 12 catalogue entries are present in full12Catalog fixture`() {
        assertThat(full12Catalog).hasSize(12)
        val expectedKinds = setOf(
            "score_pr", "x_pr", "flawless", "sharpshooter",
            "arrows_milestone", "sessions_milestone", "marathon",
            "streak", "weeks_active", "comeback",
            "first_distance", "distance_explorer",
        )
        assertThat(full12Catalog.map { it.kind }.toSet()).isEqualTo(expectedKinds)
    }

    @Test
    fun `catalogue groups correctly into 4 categories`() {
        val byCategory = full12Catalog.groupBy { it.category }
        assertThat(byCategory[TrophyCategory.skill]).hasSize(4)
        assertThat(byCategory[TrophyCategory.milestone]).hasSize(3)
        assertThat(byCategory[TrophyCategory.streak]).hasSize(3)
        assertThat(byCategory[TrophyCategory.exploration]).hasSize(2)
    }

    // ── ViewModel loads catalogue alongside earned achievements ───────────────

    @Test
    fun `loadMine fetches earned achievements and catalogue in parallel`() = runTest {
        coEvery { repository.getMyAchievements() } returns listOf(
            achievement("a1", AchievementKind.score_pr),
        )
        coEvery { repository.getTrophyCatalog() } returns full12Catalog

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.achievements.map { it.id }).containsExactly("a1")
        assertThat(state.catalog).hasSize(12)
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadForFriend fetches friend's achievements and catalogue`() = runTest {
        coEvery { repository.getFriendAchievements("u-9") } returns listOf(
            achievement("f1", AchievementKind.x_pr),
            achievement("f2", AchievementKind.streak),
        )
        coEvery { repository.getTrophyCatalog() } returns full12Catalog

        val vm = AchievementsViewModel(repository)
        vm.loadForFriend("u-9")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.achievements.map { it.kind }).containsExactly(
            AchievementKind.x_pr, AchievementKind.streak,
        )
        assertThat(state.catalog).hasSize(12)
    }

    @Test
    fun `empty earned list with full catalogue shows 0 of 12 collected`() = runTest {
        coEvery { repository.getMyAchievements() } returns emptyList()
        coEvery { repository.getTrophyCatalog() } returns full12Catalog

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.achievements).isEmpty()
        assertThat(state.catalog).hasSize(12)
        val earnedDistinctCount = state.achievements.map { it.kind.name }.toSet().size
        assertThat(earnedDistinctCount).isEqualTo(0)
    }

    @Test
    fun `earned fetch failure surfaces error and catalog stays empty`() = runTest {
        // When the earned achievements fetch fails, the error is surfaced and the
        // catalogue is never fetched (load short-circuits on the earn failure).
        coEvery { repository.getMyAchievements() } throws RuntimeException("network error")

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.error).isEqualTo("network error")
        assertThat(state.isLoading).isFalse()
        assertThat(state.achievements).isEmpty()
        assertThat(state.catalog).isEmpty()
    }

    @Test
    fun `catalog fetch failure degrades gracefully — earned achievements still shown`() = runTest {
        // Catalogue unavailable should not block the earned list.
        coEvery { repository.getMyAchievements() } returns listOf(
            achievement("a1", AchievementKind.score_pr),
        )
        coEvery { repository.getTrophyCatalog() } throws RuntimeException("catalog unavailable")

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.achievements.map { it.id }).containsExactly("a1")
        assertThat(state.catalog).isEmpty()  // degraded — no catalog, but no error shown
    }
}
