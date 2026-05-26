package com.andrewnguyen.bowpress.core.data.sync

import com.andrewnguyen.bowpress.core.data.repository.ArrowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowConfigRepository
import com.andrewnguyen.bowpress.core.data.repository.BowRepository
import com.andrewnguyen.bowpress.core.data.repository.CourseStationRepository
import com.andrewnguyen.bowpress.core.data.repository.PlotRepository
import com.andrewnguyen.bowpress.core.data.repository.SessionRepository
import com.andrewnguyen.bowpress.core.model.SessionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pull every entity needed for offline-first reads. Mirrors iOS
 * `LocalHydration.hydrateFromAPI` (Services/LocalHydration.swift) so a fresh
 * install or a relaunch on a new device reconstitutes the local store before
 * any screen reads from it.
 *
 * Without this, sessions land in Room but the per-session arrow_plots /
 * session_ends / course_stations tables stay empty — the Session log then
 * shows 0/0 because [SessionRepository.observeCompleted] recomputes
 * arrowCount off the live plot table.
 *
 * Every fetch is wrapped in [runCatching]: a transient network failure
 * leaves the local store as-is rather than blocking app launch, matching
 * iOS's `try?` swallow-and-continue pattern.
 */
@Singleton
class LocalHydration @Inject constructor(
    private val bowRepository: BowRepository,
    private val bowConfigRepository: BowConfigRepository,
    private val arrowConfigRepository: ArrowConfigRepository,
    private val sessionRepository: SessionRepository,
    private val plotRepository: PlotRepository,
    private val courseStationRepository: CourseStationRepository,
) {
    suspend fun hydrateFromApi() {
        runCatching { bowRepository.refreshFromRemote() }
        for (bow in bowRepository.getBows()) {
            runCatching { bowConfigRepository.refreshForBow(bow.id) }
        }
        runCatching { arrowConfigRepository.refreshFromRemote() }
        runCatching { sessionRepository.refreshFromRemote() }
        for (session in sessionRepository.getAll()) {
            if (session.sessionType == SessionType.THREE_D_COURSE) {
                runCatching { courseStationRepository.refreshForSession(session.id) }
            } else {
                runCatching { plotRepository.refreshForSession(session.id) }
            }
        }
    }
}
