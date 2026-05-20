package com.andrewnguyen.bowpress.feature.social.ui.you

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.SocialProfile
import com.andrewnguyen.bowpress.core.model.SocialVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouUiState(
    val profile: SocialProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class YouViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(YouUiState(isLoading = true))
    val uiState: StateFlow<YouUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { socialRepository.getMyProfile() }
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun updateHandle(handle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            runCatching { socialRepository.updateMyProfile(handle = handle) }
                .onSuccess { p -> _uiState.update { it.copy(profile = p, isSaving = false, saveSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isSaving = false) } }
        }
    }

    fun updateVisibility(visibility: SocialVisibility) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { socialRepository.updateMyProfile(visibility = visibility) }
                .onSuccess { p -> _uiState.update { it.copy(profile = p, isSaving = false, saveSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isSaving = false) } }
        }
    }

    fun dismissError() { _uiState.update { it.copy(error = null) } }
    fun dismissSuccess() { _uiState.update { it.copy(saveSuccess = false) } }
}
