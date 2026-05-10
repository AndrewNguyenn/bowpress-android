package com.andrewnguyen.bowpress.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.preferences.NotificationPreferencesRepository
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.User
import com.andrewnguyen.bowpress.feature.subscription.PlayBillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val entitlement: Entitlement = Entitlement.Inactive,
    val loading: Boolean = false,
    val error: String? = null,
    val signedOut: Boolean = false,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationPreferences: NotificationPreferencesRepository,
    private val billingManager: PlayBillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        // Pre-flip the user/entitlement off whatever's already cached so the
        // Settings screen has the right state on first frame — refresh() then
        // bumps them with a fresh network read (or no-op on failure). Mirrors
        // iOS SettingsView reading directly from AppState.
        SettingsUiState(
            user = userRepository.currentUser.value,
            entitlement = billingManager.entitlement.value,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        observeUser()
        observeEntitlement()
        observeNotificationsPreference()
        refresh()
    }

    /**
     * Mirror `UserRepository.currentUser` into state. The repository is the
     * single source of truth — `refresh()` updates it on success, `signOut`
     * clears it, and `seedDevAuth()` pre-fills it in DEBUG. Observing the
     * flow keeps Settings in sync with all three paths without manually
     * reading `.value` at each one.
     */
    private fun observeUser() {
        userRepository.currentUser
            .onEach { user -> _state.value = _state.value.copy(user = user) }
            .launchIn(viewModelScope)
    }

    /**
     * Mirror `PlayBillingManager.entitlement` so the "Subscription" row
     * renders Pro Monthly / Pro Annual / Free correctly. Without this the
     * Settings screen sees its own hardcoded `Entitlement.Inactive`
     * default — even when DEBUG seeded the manager with ActiveDevDebug.
     */
    private fun observeEntitlement() {
        billingManager.entitlement
            .onEach { entitlement -> _state.value = _state.value.copy(entitlement = entitlement) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { userRepository.refreshProfile() }
                .onSuccess { user ->
                    _state.value = _state.value.copy(user = user, loading = false)
                }
                .onFailure { t ->
                    // No need to re-set user here — observeUser() already
                    // mirrors UserRepository.currentUser into state. On a 401,
                    // refreshProfile() throws without writing _currentUser, so
                    // the seeded dev user (DEBUG) or last good user (release)
                    // remains correctly in state.
                    _state.value = _state.value.copy(
                        loading = false,
                        error = t.message ?: "Failed to refresh profile",
                    )
                }
        }
    }

    fun signOut() {
        userRepository.signOut()
        _state.value = _state.value.copy(signedOut = true, user = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            notificationPreferences.setEnabled(enabled)
        }
    }

    private fun observeNotificationsPreference() {
        viewModelScope.launch {
            notificationPreferences.notificationsEnabled.collect { enabled ->
                _state.value = _state.value.copy(notificationsEnabled = enabled)
            }
        }
    }
}
