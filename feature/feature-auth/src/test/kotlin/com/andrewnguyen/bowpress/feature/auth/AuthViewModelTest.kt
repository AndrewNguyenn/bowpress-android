package com.andrewnguyen.bowpress.feature.auth

import app.cash.turbine.test
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.model.AuthProvider
import com.andrewnguyen.bowpress.core.model.User
import com.andrewnguyen.bowpress.core.network.ApiException
import com.andrewnguyen.bowpress.core.network.SignUpResponse
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

/**
 * Exercises the happy-path sign-up → verify → signed-in loop against a mock
 * [UserRepository], plus the main error-mapping paths. Runs in a [StandardTestDispatcher]
 * so we can `advanceUntilIdle()` the coroutines the ViewModel launches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: UserRepository = mockk(relaxed = true)
    private val googleClient: GoogleSignInClient = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sign-up then verify emits SignedIn and stores user`() = runTest(dispatcher) {
        val email = "test@example.com"
        val code = "123456"
        val verifiedUser = sampleUser(email)

        coEvery { repository.signUp(any(), email, any()) } returns SignUpResponse(
            status = "pending_verification",
            email = email,
        )
        coEvery { repository.verifyEmail(email, code) } returns verifiedUser

        val vm = AuthViewModel(repository, googleClient)

        vm.events.test {
            // --- 1. Sign up -----------------------------------------------------
            vm.signUp("Test User", email, "longenoughpw")
            val signUpEvent = awaitItem()
            assertThat(signUpEvent).isInstanceOf(AuthUiEvent.NavigateToVerify::class.java)
            assertThat((signUpEvent as AuthUiEvent.NavigateToVerify).email).isEqualTo(email)
            assertThat(vm.state.value.pendingVerificationEmail).isEqualTo(email)
            assertThat(vm.state.value.isLoading).isFalse()

            // --- 2. Verify code -------------------------------------------------
            vm.verifyEmail(email, code)
            val signedInEvent = awaitItem()
            assertThat(signedInEvent).isInstanceOf(AuthUiEvent.SignedIn::class.java)
            assertThat((signedInEvent as AuthUiEvent.SignedIn).user).isEqualTo(verifiedUser)
            assertThat(vm.state.value.signedInUser).isEqualTo(verifiedUser)
            assertThat(vm.state.value.pendingVerificationEmail).isNull()
        }

        coVerify(exactly = 1) { repository.signUp("Test User", email, "longenoughpw") }
        coVerify(exactly = 1) { repository.verifyEmail(email, code) }
    }

    @Test
    fun `sign-in with EmailNotVerified routes to verify screen`() = runTest(dispatcher) {
        val email = "unverified@example.com"
        coEvery { repository.signIn(email, any()) } throws
            ApiException.EmailNotVerified(email = email)

        val vm = AuthViewModel(repository, googleClient)

        vm.events.test {
            vm.signIn(email, "password123")
            val event = awaitItem()
            assertThat(event).isInstanceOf(AuthUiEvent.NavigateToVerify::class.java)
            assertThat((event as AuthUiEvent.NavigateToVerify).email).isEqualTo(email)
        }
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.pendingVerificationEmail).isEqualTo(email)
    }

    @Test
    fun `sign-in with Unauthorized surfaces InvalidCredentials`() = runTest(dispatcher) {
        coEvery { repository.signIn(any(), any()) } throws
            ApiException.Unauthorized()

        val vm = AuthViewModel(repository, googleClient)
        vm.signIn("a@b.com", "wrong-password")
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo(AuthError.InvalidCredentials)
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `invalid verification code sets attempts remaining`() = runTest(dispatcher) {
        coEvery { repository.verifyEmail(any(), any()) } throws
            ApiException.InvalidVerificationCode(attemptsRemaining = 2)

        val vm = AuthViewModel(repository, googleClient)
        vm.verifyEmail("a@b.com", "000000")
        dispatcher.scheduler.advanceUntilIdle()

        val err = vm.state.value.error
        assertThat(err).isInstanceOf(AuthError.InvalidVerificationCode::class.java)
        assertThat((err as AuthError.InvalidVerificationCode).attemptsRemaining).isEqualTo(2)
        assertThat(vm.state.value.verificationAttemptsRemaining).isEqualTo(2)
    }

    @Test
    fun `rate-limited sign-up sets RateLimited error`() = runTest(dispatcher) {
        coEvery { repository.signUp(any(), any(), any()) } throws
            ApiException.RateLimited()

        val vm = AuthViewModel(repository, googleClient)
        vm.signUp("T", "a@b.com", "longenoughpw")
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo(AuthError.RateLimited)
    }

    @Test
    fun `sign-in twice while loading is a no-op for the second call`() = runTest(dispatcher) {
        coEvery { repository.signIn(any(), any()) } returns sampleUser("a@b.com")

        val vm = AuthViewModel(repository, googleClient)
        vm.signIn("a@b.com", "longpassword")
        vm.signIn("a@b.com", "longpassword") // second call while first is running
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.signIn(any(), any()) }
    }

    private fun sampleUser(email: String): User = User(
        id = "user-1",
        email = email,
        name = "Test",
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        emailVerified = true,
        authProvider = AuthProvider.EMAIL,
    )
}
