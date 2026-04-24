package com.andrewnguyen.bowpress.feature.analytics.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.ShootingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** UI model — one row per session. Pre-computed so the composable stays dumb. */
data class SessionRow(
    val id: String,
    val startedAt: Instant,
    val bowId: String,
    val bowName: String,
    val arrowConfigLabel: String,
    val arrowCount: Int,
    val feelTags: List<String>,
    val notes: String,
)

/** Monthly group header. */
data class SessionGroup(
    val header: String,
    val rows: List<SessionRow>,
)

data class HistoricalSessionsUiState(
    val groups: List<SessionGroup> = emptyList(),
    val bows: List<Bow> = emptyList(),
    val activeBowFilter: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HistoricalSessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val bowRepository: BowRepository,
) : ViewModel() {

    private val bowFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoricalSessionsUiState> = combine(
        sessionRepository.observeCompleted(),
        bowRepository.observeBows(),
        bowFilter,
    ) { sessions, bows, filterBowId ->
        val filtered = if (filterBowId == null) sessions else sessions.filter { it.bowId == filterBowId }
        HistoricalSessionsUiState(
            groups = groupByMonth(filtered, bows),
            bows = bows,
            activeBowFilter = filterBowId,
            isLoading = false,
            error = null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoricalSessionsUiState(),
    )

    init {
        refresh()
    }

    fun setBowFilter(bowId: String?) {
        bowFilter.value = bowId
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { sessionRepository.refreshFromRemote() }
            runCatching { bowRepository.refreshFromRemote() }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(id)
        }
    }

    fun updateSession(id: String, notes: String, feelTags: List<String>) {
        viewModelScope.launch {
            sessionRepository.updateSession(id, notes, feelTags)
        }
    }

    private fun groupByMonth(
        sessions: List<ShootingSession>,
        bows: List<Bow>,
    ): List<SessionGroup> {
        val bowIdToName = bows.associate { it.id to it.name }
        val sorted = sessions.sortedByDescending { it.startedAt }
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US).withZone(ZoneId.systemDefault())
        return sorted
            .groupBy { fmt.format(it.startedAt) }
            .map { (header, items) ->
                SessionGroup(
                    header = header,
                    rows = items.map { session ->
                        SessionRow(
                            id = session.id,
                            startedAt = session.startedAt,
                            bowId = session.bowId,
                            bowName = bowIdToName[session.bowId] ?: "Unknown bow",
                            arrowConfigLabel = session.arrowConfigId,
                            arrowCount = session.arrowCount,
                            feelTags = session.feelTags,
                            notes = session.notes,
                        )
                    },
                )
            }
    }
}
