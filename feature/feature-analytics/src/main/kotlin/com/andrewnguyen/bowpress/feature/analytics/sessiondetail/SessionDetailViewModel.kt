package com.andrewnguyen.bowpress.feature.analytics.sessiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionEndRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.SessionEnd
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for SessionDetailScreen. iOS `SessionDetailSheet` exposes the full
 * shot distribution + group stats; this VM stages the underlying data so
 * future iters can layer in the heatmap / stat chips / replot.
 */
data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val arrows: List<ArrowPlot> = emptyList(),
    val ends: List<SessionEnd> = emptyList(),
) {
    val arrowCount: Int get() = arrows.size
    val endCount: Int get() = ends.size.coerceAtLeast(1)
}

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val plotRepo: PlotRepository,
    private val sessionEndRepo: SessionEndRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _state = MutableStateFlow(SessionDetailUiState(isLoading = true, sessionId = sessionId))
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val arrows = plotRepo.getBySession(sessionId)
            val ends = sessionEndRepo.getBySession(sessionId)
            _state.update {
                it.copy(isLoading = false, arrows = arrows, ends = ends)
            }
        }
    }
}
