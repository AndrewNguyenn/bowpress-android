package com.andrewnguyen.bowpress.feature.analytics.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.BowConfiguration
import com.andrewnguyen.bowpress.feature.analytics.navigation.AnalyticsRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Single point on the timeline chart — derived from a [BowConfiguration] + server-computed score. */
data class TimelinePoint(
    val configId: String,
    val label: String,
    val createdAt: Instant,
    val score: Double,
)

data class ScoreTimelineUiState(
    val bow: Bow? = null,
    val points: List<TimelinePoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ScoreTimelineViewModel @Inject constructor(
    private val bowRepository: BowRepository,
    private val configRepository: BowConfigRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bowId: String = requireNotNull(savedStateHandle[AnalyticsRoutes.Args.BowId]) {
        "bowId argument is required for ScoreTimeline"
    }

    private val bowFlow = MutableStateFlow<Bow?>(null)

    val uiState: StateFlow<ScoreTimelineUiState> =
        combine(configRepository.observeByBow(bowId), bowFlow) { configs, bow ->
            ScoreTimelineUiState(
                bow = bow,
                points = configs.toPoints(),
                isLoading = false,
                error = null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScoreTimelineUiState(),
        )

    init {
        viewModelScope.launch {
            bowFlow.value = runCatching { bowRepository.getBow(bowId) }.getOrNull()
            runCatching { configRepository.refreshForBow(bowId) }
        }
    }

    /** Map configs into chart points. Configurations with no `avgArrowScore` are dropped. */
    private fun List<BowConfiguration>.toPoints(): List<TimelinePoint> =
        asSequence()
            .mapNotNull { cfg ->
                val score = cfg.avgArrowScore ?: return@mapNotNull null
                TimelinePoint(
                    configId = cfg.id,
                    label = cfg.label ?: cfg.id.take(8),
                    createdAt = cfg.createdAt,
                    score = score,
                )
            }
            .sortedBy { it.createdAt }
            .toList()
}
