package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.Achievement
import com.andrewnguyen.bowpress.core.model.AchievementKind
import com.andrewnguyen.bowpress.core.model.TrophyCategory
import com.andrewnguyen.bowpress.core.model.TrophyDef
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
 * Tests for the §18 trophy case:
 * - the 24-kind catalogue covers every [AchievementKind] across 7 categories
 * - earned-only grouping — only earned kinds appear, grouped by category
 * - distinct-kind "N collected" count
 * - [AchievementsViewModel] loads catalogue alongside earned achievements
 * - catalogue fallback when the API is unavailable
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

    private fun categoryOf(kind: AchievementKind): TrophyCategory = when (kind) {
        AchievementKind.score_pr, AchievementKind.x_pr, AchievementKind.flawless,
        AchievementKind.sharpshooter, AchievementKind.xs_milestone -> TrophyCategory.skill
        AchievementKind.arrows_milestone, AchievementKind.sessions_milestone,
        AchievementKind.marathon -> TrophyCategory.milestone
        AchievementKind.streak, AchievementKind.weeks_active,
        AchievementKind.comeback -> TrophyCategory.streak
        AchievementKind.first_distance, AchievementKind.distance_explorer -> TrophyCategory.exploration
        AchievementKind.course_first, AchievementKind.course_milestone,
        AchievementKind.course_explorer, AchievementKind.course_marathon,
        AchievementKind.course_pr -> TrophyCategory.course
        AchievementKind.league_first_finish, AchievementKind.league_champion,
        AchievementKind.league_podium -> TrophyCategory.competition
        AchievementKind.club_founder, AchievementKind.club_host_growth,
        AchievementKind.club_member -> TrophyCategory.community
        AchievementKind.unknown -> TrophyCategory.unknown
    }

    /** The 24 server-defined kinds — excludes the `unknown` client fallback. */
    private val serverKinds: List<AchievementKind> =
        AchievementKind.entries.filter { it != AchievementKind.unknown }

    /** A catalogue covering every server kind — mirrors `GET /social/trophies`. */
    private val fullCatalog: List<TrophyDef> = serverKinds.map { kind ->
        TrophyDef(
            kind = kind.name,
            name = "Trophy ${kind.name}",
            description = "Earn by doing ${kind.name} things.",
            tiers = listOf(1, 5, 10),
            category = categoryOf(kind),
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Catalogue shape ───────────────────────────────────────────────────────

    @Test
    fun `catalogue covers every server AchievementKind across 7 categories`() {
        assertThat(fullCatalog).hasSize(24)
        assertThat(fullCatalog.map { it.kind }.toSet())
            .isEqualTo(serverKinds.map { it.name }.toSet())
        // Every category except the client-only `unknown` fallback.
        assertThat(fullCatalog.map { it.category }.toSet())
            .isEqualTo(TrophyCategory.entries.toSet() - TrophyCategory.unknown)
    }

    @Test
    fun `catalogue groups into the seven categories`() {
        val byCategory = fullCatalog.groupBy { it.category }
        assertThat(byCategory[TrophyCategory.skill]).hasSize(5)
        assertThat(byCategory[TrophyCategory.milestone]).hasSize(3)
        assertThat(byCategory[TrophyCategory.streak]).hasSize(3)
        assertThat(byCategory[TrophyCategory.exploration]).hasSize(2)
        assertThat(byCategory[TrophyCategory.course]).hasSize(5)
        assertThat(byCategory[TrophyCategory.competition]).hasSize(3)
        assertThat(byCategory[TrophyCategory.community]).hasSize(3)
    }

    // ── Earned-only grouping ──────────────────────────────────────────────────

    @Test
    fun `only earned kinds appear, grouped by category`() {
        val earned = listOf(
            achievement("a1", AchievementKind.score_pr),       // skill
            achievement("a2", AchievementKind.streak),          // streak
            achievement("a3", AchievementKind.league_champion), // competition
        )
        val earnedKinds = earned.map { it.kind.name }.toSet()
        val shown = fullCatalog.filter { it.kind in earnedKinds }

        assertThat(shown).hasSize(3)
        assertThat(shown.map { it.category }.toSet())
            .containsExactly(TrophyCategory.skill, TrophyCategory.streak, TrophyCategory.competition)
    }

    @Test
    fun `distinct-kind count equals number of unique earned AchievementKind names`() {
        // Same kind earned twice still counts once in the "N collected" header.
        val earned = listOf(
            achievement("a1", AchievementKind.score_pr),
            achievement("a2", AchievementKind.score_pr),   // duplicate kind
            achievement("a3", AchievementKind.streak),
            achievement("a4", AchievementKind.club_founder),
        )
        val distinctEarnedKindCount = earned.map { it.kind.name }.toSet().size
        assertThat(distinctEarnedKindCount).isEqualTo(3)
    }

    // ── ViewModel loads catalogue alongside earned achievements ───────────────

    @Test
    fun `loadMine fetches earned achievements and catalogue`() = runTest {
        coEvery { repository.getMyAchievements() } returns listOf(
            achievement("a1", AchievementKind.score_pr),
        )
        coEvery { repository.getTrophyCatalog() } returns fullCatalog

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.achievements.map { it.id }).containsExactly("a1")
        assertThat(state.catalog).hasSize(24)
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadForFriend fetches friend's achievements and catalogue`() = runTest {
        coEvery { repository.getFriendAchievements("u-9") } returns listOf(
            achievement("f1", AchievementKind.x_pr),
            achievement("f2", AchievementKind.streak),
        )
        coEvery { repository.getTrophyCatalog() } returns fullCatalog

        val vm = AchievementsViewModel(repository)
        vm.loadForFriend("u-9")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.achievements.map { it.kind }).containsExactly(
            AchievementKind.x_pr, AchievementKind.streak,
        )
        assertThat(state.catalog).hasSize(24)
    }

    @Test
    fun `empty earned list still loads the catalogue`() = runTest {
        coEvery { repository.getMyAchievements() } returns emptyList()
        coEvery { repository.getTrophyCatalog() } returns fullCatalog

        val vm = AchievementsViewModel(repository)
        vm.loadMine()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.achievements).isEmpty()
        assertThat(state.catalog).hasSize(24)
    }

    @Test
    fun `earned fetch failure surfaces error and catalog stays empty`() = runTest {
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
        assertThat(state.catalog).isEmpty()
    }
}
