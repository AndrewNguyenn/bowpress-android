package com.andrewnguyen.bowpress.core.data.converters

import com.andrewnguyen.bowpress.core.model.ActivityItem
import com.andrewnguyen.bowpress.core.model.ActivityKind
import com.andrewnguyen.bowpress.core.model.ActivityPhoto
import com.andrewnguyen.bowpress.core.model.ActivitySession
import com.andrewnguyen.bowpress.core.model.ActivitySourceKind
import com.andrewnguyen.bowpress.core.model.PhotoStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Social Feed V2 — verifies the `ActivityItem` ⇄ `ActivityItemEntity` converter
 * round-trips the new `titleIsCustom`, `isOwn`, and `photos` fields through the
 * Room cache shape.
 */
class SocialConvertersTest {

    private fun item(
        titleIsCustom: Boolean = false,
        isOwn: Boolean = false,
        photos: List<ActivityPhoto> = emptyList(),
    ) = ActivityItem(
        id = "act-1",
        kind = ActivityKind.friend_session,
        sourceKind = ActivitySourceKind.friend,
        actorHandle = "sara.l",
        actorDisplayName = "Sara Lin",
        title = "Saturday 70m practice",
        createdAt = Instant.parse("2026-05-01T10:00:00Z"),
        session = ActivitySession(
            sharedSessionId = "ss-1",
            sessionId = "sess-1",
            score = 548,
            xCount = 12,
            arrowCount = 60,
            photos = photos,
        ),
        actorUserId = "u-1",
        titleIsCustom = titleIsCustom,
        isOwn = isOwn,
    )

    @Test
    fun `titleIsCustom and isOwn survive the round-trip`() {
        val original = item(titleIsCustom = true, isOwn = true)
        val restored = original.toEntity().toDto()
        assertThat(restored.titleIsCustom).isTrue()
        assertThat(restored.isOwn).isTrue()
    }

    @Test
    fun `the flags default to false when not set`() {
        val restored = item().toEntity().toDto()
        assertThat(restored.titleIsCustom).isFalse()
        assertThat(restored.isOwn).isFalse()
    }

    @Test
    fun `the photo gallery survives the round-trip in order`() {
        // The gallery round-trips inside the serialized ActivitySession blob —
        // there is no separate photosJson column.
        val photos = listOf(
            ActivityPhoto(id = "ph-a", status = PhotoStatus.ready, position = 0),
            ActivityPhoto(id = "ph-b", status = PhotoStatus.pending, position = 1),
        )
        val restored = item(photos = photos).toEntity().toDto()
        assertThat(restored.session?.photos).isEqualTo(photos)
    }

    @Test
    fun `a session with no photos decodes to an empty gallery`() {
        val restored = item(photos = emptyList()).toEntity().toDto()
        assertThat(restored.session?.photos).isEmpty()
    }
}
