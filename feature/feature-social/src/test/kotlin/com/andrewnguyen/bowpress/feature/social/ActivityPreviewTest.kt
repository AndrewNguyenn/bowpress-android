package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.feature.social.ui.feed.ActivityPreview
import com.andrewnguyen.bowpress.feature.social.ui.feed.activityPreview
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * §18 — the typed feed-row preview selection: a 3D course → course block,
 * a range session → target face, a non-session row → none.
 */
class ActivityPreviewTest {

    private fun item(session: ActivitySession?): ActivityItem = ActivityItem(
        id = "act-1",
        kind = ActivityKind.friend_session,
        sourceKind = ActivitySourceKind.friend,
        actorHandle = "sara.l",
        actorDisplayName = "Sara Lin",
        title = "Shared a session",
        createdAt = Instant.parse("2026-05-19T08:00:00Z"),
        session = session,
    )

    private fun session(
        discipline: String?,
        sessionId: String = "s1",
        endRings: List<List<Int>>? = null,
    ): ActivitySession = ActivitySession(
        sharedSessionId = "ss1",
        sessionId = sessionId,
        score = 285,
        xCount = 12,
        arrowCount = 90,
        distance = "20yd",
        face = "6-ring",
        discipline = discipline,
        endRings = endRings,
    )

    @Test
    fun `isEmpty is true only for None`() {
        assertThat(ActivityPreview.None.isEmpty).isTrue()
        assertThat(ActivityPreview.Photo.isEmpty).isFalse()
        assertThat(
            ActivityPreview.Target(
                face = "6-ring", distance = "20yd", score = 285, endRings = null,
            ).isEmpty,
        ).isFalse()
        assertThat(ActivityPreview.Course(score = 64, stations = 6).isEmpty).isFalse()
    }

    @Test
    fun `a session with a target-paper photo picks the photo preview`() {
        // sess_devon_1 is the mock session registered in TargetPhotoCatalog;
        // the photo preview wins over the range discipline preview.
        val preview = activityPreview(
            item(session(discipline = "range", sessionId = "sess_devon_1")),
        )
        assertThat(preview).isEqualTo(ActivityPreview.Photo)
    }

    @Test
    fun `range session picks the target preview, carrying the scorecard ends`() {
        val rings = listOf(listOf(11, 10, 10), listOf(10, 9, 9))
        val preview = activityPreview(item(session(discipline = "range", endRings = rings)))
        assertThat(preview).isInstanceOf(ActivityPreview.Target::class.java)
        val target = preview as ActivityPreview.Target
        assertThat(target.face).isEqualTo("6-ring")
        assertThat(target.distance).isEqualTo("20yd")
        assertThat(target.score).isEqualTo(285)
        assertThat(target.endRings).isEqualTo(rings)
    }

    @Test
    fun `3d course session picks the course preview`() {
        val preview = activityPreview(item(session(discipline = "3d_course")))
        assertThat(preview).isInstanceOf(ActivityPreview.Course::class.java)
        val course = preview as ActivityPreview.Course
        assertThat(course.score).isEqualTo(285)
        // Stations come from the arrow count — one shot per station.
        assertThat(course.stations).isEqualTo(90)
    }

    @Test
    fun `a session with no discipline falls back to the target preview`() {
        // A pre-v1.6 payload omits discipline → treated as a range session.
        val preview = activityPreview(item(session(discipline = null)))
        assertThat(preview).isInstanceOf(ActivityPreview.Target::class.java)
    }

    @Test
    fun `a non-session row has no preview`() {
        assertThat(activityPreview(item(session = null))).isEqualTo(ActivityPreview.None)
    }
}
