package com.andrewnguyen.bowpress.core.data.seed

import com.andrewnguyen.bowpress.core.model.CourseStation

/**
 * Public seam over the DEBUG `DevMockData` fixtures: the station lists of
 * every mock 3D course in the social feed.
 *
 * `DevMockData` is module-internal, but the `app` module needs the mock
 * courses' geography at startup to seed synthetic terrain grids (so the
 * feed's course maps draw contours). Mirrors the data iOS reaches via
 * `DevSocialMockData.course3DGallery` + `course3DSharedStations`.
 */
object DevMockCourses {

    /** Stations of every mock 3D course carried by the activity feed. */
    val all3DCourseStations: List<List<CourseStation>>
        get() = DevMockData.activityFeed
            .mapNotNull { it.session }
            .filter { it.isCourse }
            .mapNotNull { it.stations?.takeIf { stations -> stations.isNotEmpty() } }
}
