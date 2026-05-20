package com.andrewnguyen.bowpress

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.SuggestionRepository
import com.andrewnguyen.bowpress.core.data.repository.ThemePreferencesRepository
import com.andrewnguyen.bowpress.core.data.repository.UnitPreferencesRepository
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.data.seed.DevMockDataSeeder
import com.andrewnguyen.bowpress.core.data.sync.AnalyticsRefreshBus
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.model.AuthProvider
import com.andrewnguyen.bowpress.core.model.DeliveryType
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.UnitSystem
import com.andrewnguyen.bowpress.core.model.User
import com.andrewnguyen.bowpress.feature.subscription.PlayBillingManager
import com.andrewnguyen.bowpress.push.PushInitializer
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unauthenticated at start — isAuthenticated false, no hydration`() = runTest {
        val userRepo = mockk<UserRepository>(relaxed = true) {
            every { isSignedIn } returns false
            every { currentUser } returns MutableStateFlow(null)
        }
        val suggestionRepo = mockk<SuggestionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val push = mockk<PushInitializer>(relaxed = true)

        val unitRepo = mockk<UnitPreferencesRepository>(relaxed = true) {
            every { unitSystem } returns flowOf(UnitSystem.IMPERIAL)
        }
        val billing = mockk<PlayBillingManager>(relaxed = true) {
            every { entitlement } returns MutableStateFlow(Entitlement.Inactive)
        }
        val vm = AppStateViewModel(
            userRepo, suggestionRepo, unitRepo,
            mockk<ThemePreferencesRepository>(relaxed = true),
            push, billing, AnalyticsRefreshBus(),
            mockk<DevMockDataSeeder>(relaxed = true),
        )
        vm.uiState.test {
            val s = awaitItem()
            assertThat(s.isAuthenticated).isFalse()
            assertThat(s.isHydrating).isFalse()
            assertThat(s.unreadSuggestionCount).isEqualTo(0)
        }
    }

    @Test
    fun `unread count reflects only unread non-dismissed suggestions`() = runTest {
        val suggestions = listOf(
            suggestion(id = "a", wasRead = false, wasDismissed = false),
            suggestion(id = "b", wasRead = true, wasDismissed = false),
            suggestion(id = "c", wasRead = false, wasDismissed = true),
            suggestion(id = "d", wasRead = false, wasDismissed = false),
        )
        val userRepo = mockk<UserRepository>(relaxed = true) {
            every { isSignedIn } returns false
            every { currentUser } returns MutableStateFlow(null)
        }
        val suggestionRepo = mockk<SuggestionRepository> {
            every { observeAll() } returns flowOf(suggestions)
        }

        val unitRepo2 = mockk<UnitPreferencesRepository>(relaxed = true) {
            every { unitSystem } returns flowOf(UnitSystem.IMPERIAL)
        }
        val billing = mockk<PlayBillingManager>(relaxed = true) {
            every { entitlement } returns MutableStateFlow(Entitlement.Inactive)
        }
        val vm = AppStateViewModel(
            userRepo, suggestionRepo, unitRepo2,
            mockk<ThemePreferencesRepository>(relaxed = true),
            mockk(relaxed = true), billing, AnalyticsRefreshBus(),
            mockk<DevMockDataSeeder>(relaxed = true),
        )
        vm.uiState.test {
            val s = awaitItem()
            assertThat(s.unreadSuggestionCount).isEqualTo(2)
        }
    }

    @Test
    fun `signed-in at start — hydrates, currentUser mirrored from repo`() = runTest {
        val user = User(
            id = "u1",
            email = "a@b.com",
            name = "Andrew",
            createdAt = Instant.EPOCH,
            emailVerified = true,
            authProvider = AuthProvider.EMAIL,
        )
        val userFlow = MutableStateFlow<User?>(user)
        val userRepo = mockk<UserRepository>(relaxed = true) {
            every { isSignedIn } returns true
            every { currentUser } returns userFlow
            coEvery { refreshProfile() } returns user
        }
        val suggestionRepo = mockk<SuggestionRepository> {
            every { observeAll() } returns flowOf(emptyList())
        }
        val push = mockk<PushInitializer>(relaxed = true)

        val unitRepo = mockk<UnitPreferencesRepository>(relaxed = true) {
            every { unitSystem } returns flowOf(UnitSystem.IMPERIAL)
        }
        val billing = mockk<PlayBillingManager>(relaxed = true) {
            every { entitlement } returns MutableStateFlow(Entitlement.Inactive)
        }
        val vm = AppStateViewModel(
            userRepo, suggestionRepo, unitRepo,
            mockk<ThemePreferencesRepository>(relaxed = true),
            push, billing, AnalyticsRefreshBus(),
            mockk<DevMockDataSeeder>(relaxed = true),
        )
        vm.uiState.test {
            val s = awaitItem()
            assertThat(s.isAuthenticated).isTrue()
            assertThat(s.currentUser).isEqualTo(user)
        }
    }

    private fun suggestion(id: String, wasRead: Boolean, wasDismissed: Boolean) =
        AnalyticsSuggestion(
            id = id,
            bowId = "b1",
            createdAt = Instant.EPOCH,
            parameter = "drawLength",
            suggestedValue = "29.0",
            currentValue = "28.5",
            reasoning = "placeholder",
            confidence = 0.9,
            qualifier = null,
            wasRead = wasRead,
            wasDismissed = wasDismissed,
            deliveryType = DeliveryType.IN_APP,
        )
}
