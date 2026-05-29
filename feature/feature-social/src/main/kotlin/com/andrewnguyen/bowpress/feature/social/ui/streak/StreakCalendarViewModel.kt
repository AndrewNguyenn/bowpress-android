package com.andrewnguyen.bowpress.feature.social.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.StreakCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * Backs the monthly streak calendar. Tracks the displayed [YearMonth],
 * fetches the per-month payload, and clamps navigation to the ~12-month
 * window the API keeps data for.
 */
@HiltViewModel
class StreakCalendarViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    data class UiState(
        val displayed: YearMonth,
        val calendar: StreakCalendar? = null,
        val isLoading: Boolean = false,
        val failed: Boolean = false,
    ) {
        val canGoNext: Boolean get() = displayed < YearMonth.now()
        // The API loads ~52 weeks back; cap navigation at 11 months prior
        // so the archer never pages into a guaranteed-empty month.
        val canGoPrev: Boolean get() = displayed > YearMonth.now().minusMonths(11)
    }

    private val _uiState = MutableStateFlow(UiState(displayed = YearMonth.now()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun prevMonth() {
        if (!_uiState.value.canGoPrev) return
        _uiState.update { it.copy(displayed = it.displayed.minusMonths(1)) }
        load()
    }

    fun nextMonth() {
        if (!_uiState.value.canGoNext) return
        _uiState.update { it.copy(displayed = it.displayed.plusMonths(1)) }
        load()
    }

    private fun load() {
        val ym = _uiState.value.displayed
        _uiState.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            runCatching { socialRepository.getStreakCalendar(ym.year, ym.monthValue) }
                .onSuccess { cal ->
                    // Ignore a stale response if the archer paged on while it
                    // was in flight.
                    if (_uiState.value.displayed == ym) {
                        _uiState.update { it.copy(calendar = cal, isLoading = false) }
                    }
                }
                .onFailure {
                    if (_uiState.value.displayed == ym) {
                        _uiState.update { it.copy(isLoading = false, failed = true) }
                    }
                }
        }
    }
}
