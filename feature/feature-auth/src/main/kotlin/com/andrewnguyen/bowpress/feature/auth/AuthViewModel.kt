package com.andrewnguyen.bowpress.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Auth landing / email-sign-in / verify-email screens.
 *
 * Contract:
 *   - Screens observe [state]; read-only.
 *   - Screens call the `on…` methods on user actions.
 *   - One-shot events (navigate to verify, signed in) come through [events].
 *   - Errors are mapped from [ApiException] → [AuthError] so the view doesn't
 *     need to know the network layer's vocabulary.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val googleSignInClient: GoogleSignInClient,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    private var resendCooldownJob: Job? = null

    // -------------------------------------------------------------------------
    // Mode + field edits
    // -------------------------------------------------------------------------

    fun setMode(mode: AuthUiState.Mode) {
        _state.update { it.copy(mode = mode, error = null) }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // -------------------------------------------------------------------------
    // Email / password
    // -------------------------------------------------------------------------

    /**
     * Sign in with an email + password. On success emits [AuthUiEvent.SignedIn].
     * On `EmailNotVerified` routes to the Verify screen instead.
     */
    fun signIn(email: String, password: String) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null, email = email) }
        viewModelScope.launch {
            try {
                val user = userRepository.signIn(email, password)
                _state.update { it.copy(isLoading = false, signedInUser = user) }
                _events.emit(AuthUiEvent.SignedIn(user))
            } catch (e: ApiException.EmailNotVerified) {
                val target = e.email ?: email
                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingVerificationEmail = target,
                        error = null,
                    )
                }
                _events.emit(AuthUiEvent.NavigateToVerify(target))
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toAuthError()) }
            }
        }
    }

    /**
     * Sign up with name + email + password. On success the backend returns 202
     * and we route the user to VerifyEmail.
     */
    fun signUp(name: String, email: String, password: String) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null, email = email) }
        viewModelScope.launch {
            try {
                val result = userRepository.signUp(name.trim(), email, password)
                val target = result.email.ifBlank { email }
                _state.update {
                    it.copy(
                        isLoading = false,
                        pendingVerificationEmail = target,
                    )
                }
                _events.emit(AuthUiEvent.NavigateToVerify(target))
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toAuthError()) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Verify email
    // -------------------------------------------------------------------------

    fun verifyEmail(email: String, code: String) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val user = userRepository.verifyEmail(email, code)
                _state.update {
                    it.copy(
                        isLoading = false,
                        signedInUser = user,
                        pendingVerificationEmail = null,
                        verificationAttemptsRemaining = null,
                    )
                }
                _events.emit(AuthUiEvent.SignedIn(user))
            } catch (e: ApiException.InvalidVerificationCode) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        verificationAttemptsRemaining = e.attemptsRemaining,
                        error = AuthError.InvalidVerificationCode(e.attemptsRemaining),
                    )
                }
            } catch (e: ApiException.VerificationCodeExpired) {
                _state.update {
                    it.copy(isLoading = false, error = AuthError.VerificationCodeExpired)
                }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toAuthError()) }
            }
        }
    }

    /**
     * Resend the verification code and start a [RESEND_COOLDOWN_SECONDS] cooldown.
     * Repeat calls within the window are silently ignored.
     */
    fun resendVerification(email: String) {
        if (_state.value.resendCooldownSeconds > 0 || _state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                userRepository.resendVerification(email)
                _state.update { it.copy(isLoading = false) }
                startResendCooldown()
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toAuthError()) }
            }
        }
    }

    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            var remaining = RESEND_COOLDOWN_SECONDS
            while (remaining > 0) {
                _state.update { it.copy(resendCooldownSeconds = remaining) }
                delay(1_000L)
                remaining -= 1
            }
            _state.update { it.copy(resendCooldownSeconds = 0) }
        }
    }

    // -------------------------------------------------------------------------
    // Google Sign-In
    // -------------------------------------------------------------------------

    /**
     * Kick off Google Sign-In. Must be called with an Activity [Context] so
     * Credential Manager can present its picker.
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val idToken = googleSignInClient.fetchIdToken(activityContext)
                if (idToken == null) {
                    // User cancelled — drop back to idle, no error.
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }
                val user = userRepository.signInWithGoogle(idToken)
                _state.update { it.copy(isLoading = false, signedInUser = user) }
                _events.emit(AuthUiEvent.SignedIn(user))
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.toAuthError()) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Error mapping
    // -------------------------------------------------------------------------

    private fun Throwable.toAuthError(): AuthError = when (this) {
        is ApiException.Unauthorized -> AuthError.InvalidCredentials
        is ApiException.RateLimited -> AuthError.RateLimited
        is ApiException.InvalidVerificationCode -> AuthError.InvalidVerificationCode(attemptsRemaining)
        is ApiException.VerificationCodeExpired -> AuthError.VerificationCodeExpired
        is GoogleSignInException ->
            AuthError.UiMessage(message ?: "Google Sign-In failed")
        else -> AuthError.UiMessage(message ?: "Something went wrong. Please try again.")
    }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 30
    }
}
