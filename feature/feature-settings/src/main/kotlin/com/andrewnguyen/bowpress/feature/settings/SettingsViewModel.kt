package com.andrewnguyen.bowpress.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.UserRepository
import com.andrewnguyen.bowpress.core.model.Entitlement
import com.andrewnguyen.bowpress.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val entitlement: Entitlement = Entitlement.Inactive,
    val loading: Boolean = false,
    val error: String? = null,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { userRepository.refreshProfile() }
                .onSuccess { user ->
                    _state.value = _state.value.copy(user = user, loading = false)
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = t.message ?: "Failed to refresh profile",
                        user = userRepository.currentUser.value,
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
}
