package com.andrewnguyen.bowpress.feature.session.threed

import com.andrewnguyen.bowpress.core.designsystem.coursemap.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.CourseStationRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.CourseStation
import com.andrewnguyen.bowpress.core.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One labelled bucket in a 3D-analytics breakdown. */
data class ThreeDBucket(val label: String, val average: Double, val count: Int)

data class ThreeDAnalyticsUiState(
    val courseCount: Int = 0,
    val stationCount: Int = 0,
    val averagePerStation: Double = 0.0,
    val byAngle: List<ThreeDBucket> = emptyList(),
    val byDistance: List<ThreeDBucket> = emptyList(),
)

/**
 * Mirrors iOS `ThreeDAnalyticsView`'s data — performance across every
 * completed 3D course, broken down by shot angle and by distance band.
 */
@HiltViewModel
class ThreeDAnalyticsViewModel @Inject constructor(
    sessionRepo: SessionRepository,
    courseStationRepo: CourseStationRepository,
) : ViewModel() {

    val uiState: StateFlow<ThreeDAnalyticsUiState> =
        combine(
            sessionRepo.observeCompleted(),
            courseStationRepo.observeAll(),
        ) { sessions, stations ->
            val courseIds = sessions
                .filter { it.sessionType == SessionType.THREE_D_COURSE }
                .mapTo(HashSet()) { it.id }
            val courseStations = stations.filter { it.sessionId in courseIds }
            ThreeDAnalyticsUiState(
                courseCount = courseIds.size,
                stationCount = courseStations.size,
                averagePerStation = courseStations.averageRing(),
                byAngle = angleBuckets(courseStations),
                byDistance = distanceBuckets(courseStations),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreeDAnalyticsUiState())

    private fun List<CourseStation>.averageRing(): Double =
        if (isEmpty()) 0.0 else sumOf { it.ring }.toDouble() / size

    private fun angleBuckets(stations: List<CourseStation>): List<ThreeDBucket> {
        val bins = linkedMapOf(
            "Steep down" to stations.filter { it.angleDegrees <= -15 },
            "Downhill" to stations.filter { it.angleDegrees > -15 && it.angleDegrees <= -5 },
            "Level" to stations.filter { it.angleDegrees > -5 && it.angleDegrees < 5 },
            "Uphill" to stations.filter { it.angleDegrees >= 5 && it.angleDegrees < 15 },
            "Steep up" to stations.filter { it.angleDegrees >= 15 },
        )
        return bins.mapNotNull { (label, group) ->
            if (group.isEmpty()) null else ThreeDBucket(label, group.averageRing(), group.size)
        }
    }

    private fun distanceBuckets(stations: List<CourseStation>): List<ThreeDBucket> {
        // Group on the metres-normalized ranged distance; unranged stations skipped.
        val ranged = stations.filter { it.estimatedDistance != null }
        val bins = linkedMapOf(
            "≤ 20" to ranged.filter { metres(it) <= 20 },
            "20–35" to ranged.filter { metres(it) > 20 && metres(it) <= 35 },
            "35–50" to ranged.filter { metres(it) > 35 && metres(it) <= 50 },
            "> 50" to ranged.filter { metres(it) > 50 },
        )
        return bins.mapNotNull { (label, group) ->
            if (group.isEmpty()) null else ThreeDBucket(label, group.averageRing(), group.size)
        }
    }

    private fun metres(station: CourseStation): Double =
        ThreeDGeometry.distanceMeters(station.estimatedDistance ?: 0.0, station.distanceUnit)
}
