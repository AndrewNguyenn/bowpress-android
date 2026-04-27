package com.andrewnguyen.bowpress.feature.analytics.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.ArrowPlot
import com.andrewnguyen.bowpress.core.model.Bow
import com.andrewnguyen.bowpress.core.model.ShootingDistance
import com.andrewnguyen.bowpress.core.model.ShootingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/**
 * One session row in the Session log ledger. Pre-computed so the composable
 * stays dumb — avg ring, X%, per-arrow bar strip, delta vs previous are all
 * baked here. Mirrors iOS `SessionLogRow`.
 */
data class SessionRow(
    val id: String,
    val startedAt: Instant,
    val bowId: String,
    val bowName: String,
    val arrowConfigLabel: String,
    val arrowCount: Int,
    val feelTags: List<String>,
    val notes: String,
    val distance: ShootingDistance?,
    val title: String?,
    val rings: List<Int>,
    val avgRing: Double,
    val xCount: Int,
    val xPct: Int,
    val previousAvg: Double?,
    val isBest: Boolean,
)

/** One group header + its rows. */
data class SessionGroup(
    val header: String,
    val bucket: GroupBucket,
    val rangeLabel: String,
    val rows: List<SessionRow>,
)

/** Group buckets drive whether we render the month-rollup heatmap above this group. */
enum class GroupBucket { THIS_WEEK, LAST_WEEK, MONTHLY }

/**
 * Summary of the current month's sessions. Fed into the month-rollup heatmap.
 * Mirrors iOS `monthboxView`.
 */
data class MonthRollup(
    val monthLabel: String,
    val sessionCount: Int,
    val avgRing: Double,
    val daysInMonth: Int,
    val todayDay: Int,
    /** Day (1..30) → arrows logged that day. */
    val arrowsByDay: Map<Int, Int>,
)

data class HistoricalSessionsUiState(
    val groups: List<SessionGroup> = emptyList(),
    val bows: List<Bow> = emptyList(),
    val activeBowFilter: String? = null,
    val totalArrows: Int = 0,
    val totalSessions: Int = 0,
    val sinceLabel: String = "—",
    val monthRollup: MonthRollup? = null,
    /** Index into `groups` where the month-rollup should be rendered above. */
    val monthRollupInsertIndex: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoricalSessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val bowRepository: BowRepository,
    private val plotRepository: PlotRepository,
) : ViewModel() {

    private val bowFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoricalSessionsUiState> = combine(
        sessionRepository.observeCompleted(),
        bowRepository.observeBows(),
        plotRepository.observeAll(),
        bowFilter,
    ) { sessions, bows, plots, filterBowId ->
        val filtered = if (filterBowId == null) sessions else sessions.filter { it.bowId == filterBowId }
        build(filtered, bows, plots, filterBowId)
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

    private fun build(
        sessions: List<ShootingSession>,
        bows: List<Bow>,
        allPlots: List<ArrowPlot>,
        filterBowId: String?,
    ): HistoricalSessionsUiState {
        val bowById = bows.associateBy { it.id }
        val plotsBySession = allPlots.groupBy { it.sessionId }
        val sorted = sessions.sortedByDescending { it.startedAt }

        // Compute per-session avg (ignoring X = 11 collapses to 10 for averaging).
        val avgBySession: Map<String, Double> = sorted.associate { session ->
            val p = plotsBySession[session.id].orEmpty()
            val avg = if (p.isEmpty()) 0.0
            else p.sumOf { minOf(it.ring, 10) }.toDouble() / p.size
            session.id to avg
        }
        val historicalBest = avgBySession.values.maxOrNull() ?: 0.0

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val weekFields = WeekFields.of(Locale.US)
        val currentWeek = today.get(weekFields.weekOfWeekBasedYear())
        val currentYear = today.year
        val lastWeekDate = today.minusWeeks(1)

        fun bucketFor(instant: Instant): Pair<String, GroupBucket> {
            val date = instant.atZone(zoneId).toLocalDate()
            val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
            val year = date.year
            return when {
                weekOfYear == currentWeek && year == currentYear -> "THIS WEEK" to GroupBucket.THIS_WEEK
                weekOfYear == lastWeekDate.get(weekFields.weekOfWeekBasedYear()) && year == lastWeekDate.year ->
                    "LAST WEEK" to GroupBucket.LAST_WEEK
                else -> {
                    val label = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                    label.uppercase(Locale.US) to GroupBucket.MONTHLY
                }
            }
        }

        // Track insertion order of buckets (newest first).
        val orderedBuckets = linkedMapOf<String, Pair<GroupBucket, MutableList<ShootingSession>>>()
        for (session in sorted) {
            val (header, bucket) = bucketFor(session.startedAt)
            val entry = orderedBuckets.getOrPut(header) { bucket to mutableListOf() }
            entry.second.add(session)
        }

        val idToPrev: Map<String, ShootingSession?> = run {
            val map = mutableMapOf<String, ShootingSession?>()
            for (i in sorted.indices) {
                map[sorted[i].id] = sorted.getOrNull(i + 1)
            }
            map
        }

        val groups = orderedBuckets.map { (header, pair) ->
            val (bucket, items) = pair
            val bestId = items.maxByOrNull { avgBySession[it.id] ?: 0.0 }?.id
            val bestAvg = items.maxOfOrNull { avgBySession[it.id] ?: 0.0 } ?: 0.0
            val rangeLabel = computeRangeLabel(items, zoneId)
            SessionGroup(
                header = header,
                bucket = bucket,
                rangeLabel = rangeLabel,
                rows = items.map { session ->
                    val rings = plotsBySession[session.id].orEmpty().map { it.ring }
                    val xCount = rings.count { it == 11 }
                    val xPct = if (rings.isNotEmpty()) (xCount * 100 / rings.size) else 0
                    val avg = avgBySession[session.id] ?: 0.0
                    val prev = idToPrev[session.id]
                    val prevAvg = prev?.let { avgBySession[it.id] }
                    val isBest = when {
                        items.size == 1 -> bestId == session.id && avg >= historicalBest - 0.001
                        else -> session.id == bestId && avg == bestAvg && avg > 0.0
                    }
                    val title: String? = session.distance?.let { "Range · ${it.label}" }
                    SessionRow(
                        id = session.id,
                        startedAt = session.startedAt,
                        bowId = session.bowId,
                        bowName = bowById[session.bowId]?.name ?: "bow",
                        arrowConfigLabel = session.arrowConfigId,
                        arrowCount = session.arrowCount,
                        feelTags = session.feelTags,
                        notes = session.notes,
                        distance = session.distance,
                        title = title,
                        rings = rings,
                        avgRing = avg,
                        xCount = xCount,
                        xPct = xPct,
                        previousAvg = prevAvg?.takeIf { it > 0.0 },
                        isBest = isBest,
                    )
                },
            )
        }

        val monthInsertIndex = groups.indexOfFirst { it.bucket == GroupBucket.MONTHLY }
            .takeIf { it >= 0 }

        // Month rollup (for the current month).
        val thisMonthSessions = sessions.filter {
            val d = it.startedAt.atZone(zoneId).toLocalDate()
            d.month == today.month && d.year == today.year
        }
        val monthRollup: MonthRollup? = if (thisMonthSessions.isEmpty()) null else {
            val arrowsByDay = thisMonthSessions.groupBy {
                it.startedAt.atZone(zoneId).toLocalDate().dayOfMonth
            }.mapValues { (_, list) -> list.sumOf { it.arrowCount } }
            val avg = thisMonthSessions.mapNotNull { avgBySession[it.id] }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
            MonthRollup(
                monthLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                sessionCount = thisMonthSessions.size,
                avgRing = avg,
                daysInMonth = today.month.length(today.isLeapYear),
                todayDay = today.dayOfMonth,
                arrowsByDay = arrowsByDay,
            )
        }

        val sinceDate = sessions.minOfOrNull { it.startedAt }
        val sinceLabel = sinceDate?.atZone(zoneId)?.toLocalDate()
            ?.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            ?: "—"

        return HistoricalSessionsUiState(
            groups = groups,
            bows = bows,
            activeBowFilter = filterBowId,
            totalArrows = sessions.sumOf { it.arrowCount },
            totalSessions = sessions.size,
            sinceLabel = sinceLabel,
            monthRollup = monthRollup,
            monthRollupInsertIndex = monthInsertIndex,
            isLoading = false,
            error = null,
        )
    }

    private fun computeRangeLabel(items: List<ShootingSession>, zoneId: ZoneId): String {
        if (items.isEmpty()) return ""
        val sortedAsc = items.sortedBy { it.startedAt }
        val first = sortedAsc.first().startedAt.atZone(zoneId).toLocalDate()
        val last = sortedAsc.last().startedAt.atZone(zoneId).toLocalDate()
        val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val count = items.size
        val suffix = " · $count session${if (count == 1) "" else "s"}"
        return if (first == last) {
            first.format(fmt).lowercase(Locale.US) + suffix
        } else {
            "${first.format(fmt).lowercase(Locale.US)} — ${last.format(fmt).lowercase(Locale.US)}$suffix"
        }
    }
}

