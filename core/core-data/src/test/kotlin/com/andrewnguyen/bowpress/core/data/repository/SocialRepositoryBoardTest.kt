package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.ClubAnnouncement
import com.andrewnguyen.bowpress.core.model.CreateAnnouncementBody
import com.andrewnguyen.bowpress.core.model.CreateAttachmentBody
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.core.model.UpdateAnnouncementBody
import android.content.Context
import android.content.pm.ApplicationInfo
import com.andrewnguyen.bowpress.core.network.BowPressApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Covers the §17 club announcement board + league attachment flows on
 * [SocialRepository] — post/list/pin/delete for the board, add/list/delete
 * plus kind validation for attachments.
 */
class SocialRepositoryBoardTest {

    private lateinit var api: BowPressApi
    private lateinit var repo: SocialRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        // ApplicationInfo.flags is a plain Java field; set it directly on a real instance.
        val appInfo = ApplicationInfo().also { it.flags = ApplicationInfo.FLAG_DEBUGGABLE }
        val debugContext = mockk<Context> {
            every { applicationInfo } returns appInfo
        }
        repo = SocialRepository(
            api,
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), // photoCache
            debugContext,
        )
    }

    private fun announcement(id: String, pinned: Boolean, createdAt: Instant) = ClubAnnouncement(
        id = id,
        clubId = "club-1",
        authorUserId = "u-host",
        authorHandle = "host.h",
        authorDisplayName = "Host",
        body = "Body $id",
        pinned = pinned,
        createdAt = createdAt,
    )

    private fun attachment(id: String, kind: AttachmentKind) = LeagueAttachment(
        id = id,
        leagueId = "lg-1",
        addedByUserId = "u-host",
        addedByHandle = "host.h",
        kind = kind,
        title = "Title $id",
        url = if (kind != AttachmentKind.NOTE) "https://x/$id" else null,
        note = if (kind == AttachmentKind.NOTE) "Note $id" else null,
        createdAt = Instant.now(),
    )

    // ── Club announcement board ──────────────────────────────────────────────

    @Test
    fun `getClubAnnouncements sorts pinned first, then newest`() = runTest {
        val older = announcement("a-old", pinned = false, createdAt = Instant.parse("2026-05-01T00:00:00Z"))
        val newer = announcement("a-new", pinned = false, createdAt = Instant.parse("2026-05-10T00:00:00Z"))
        val pinned = announcement("a-pin", pinned = true, createdAt = Instant.parse("2026-04-01T00:00:00Z"))
        coEvery { api.getClubAnnouncements("club-1") } returns listOf(older, pinned, newer)

        val result = repo.getClubAnnouncements("club-1")

        // Pinned leads even though it's the oldest; the rest are newest-first.
        assertThat(result.map { it.id }).containsExactly("a-pin", "a-new", "a-old").inOrder()
    }

    @Test
    fun `getClubAnnouncements falls back to seeded data on API failure`() = runTest {
        coEvery { api.getClubAnnouncements("club_001") } throws RuntimeException("offline")

        // club_001 is the dev-hosted club in DevMockData — the fallback serves it.
        val result = repo.getClubAnnouncements("club_001")

        assertThat(result).isNotEmpty()
        // Still pinned-first after the fallback sort.
        assertThat(result.first().pinned).isTrue()
    }

    @Test
    fun `postClubAnnouncement posts the body and pin flag`() = runTest {
        coEvery {
            api.postClubAnnouncement("club-1", CreateAnnouncementBody("Range closed", pinned = true))
        } returns announcement("a-1", pinned = true, createdAt = Instant.now())

        val result = repo.postClubAnnouncement("club-1", "Range closed", pinned = true)

        assertThat(result.id).isEqualTo("a-1")
        coVerify { api.postClubAnnouncement("club-1", CreateAnnouncementBody("Range closed", pinned = true)) }
    }

    @Test
    fun `setClubAnnouncementPinned PATCHes the pin flag`() = runTest {
        coEvery {
            api.updateClubAnnouncement("club-1", "a-1", UpdateAnnouncementBody(pinned = false))
        } returns announcement("a-1", pinned = false, createdAt = Instant.now())

        val result = repo.setClubAnnouncementPinned("club-1", "a-1", pinned = false)

        assertThat(result.pinned).isFalse()
        coVerify { api.updateClubAnnouncement("club-1", "a-1", UpdateAnnouncementBody(pinned = false)) }
    }

    @Test
    fun `deleteClubAnnouncement calls the API`() = runTest {
        coEvery { api.deleteClubAnnouncement("club-1", "a-1") } returns Unit

        repo.deleteClubAnnouncement("club-1", "a-1")

        coVerify { api.deleteClubAnnouncement("club-1", "a-1") }
    }

    // ── League attachments ───────────────────────────────────────────────────

    @Test
    fun `getLeagueAttachments sorts newest first`() = runTest {
        val older = attachment("att-old", AttachmentKind.NOTE)
            .copy(createdAt = Instant.parse("2026-05-01T00:00:00Z"))
        val newer = attachment("att-new", AttachmentKind.LINK)
            .copy(createdAt = Instant.parse("2026-05-10T00:00:00Z"))
        coEvery { api.getLeagueAttachments("lg-1") } returns listOf(older, newer)

        val result = repo.getLeagueAttachments("lg-1")

        assertThat(result.map { it.id }).containsExactly("att-new", "att-old").inOrder()
    }

    @Test
    fun `getLeagueAttachments falls back to seeded data on API failure`() = runTest {
        coEvery { api.getLeagueAttachments("lg_001") } throws RuntimeException("offline")

        // lg_001 is the dev-hosted league in DevMockData — the fallback serves it.
        val result = repo.getLeagueAttachments("lg_001")

        assertThat(result).isNotEmpty()
    }

    @Test
    fun `addLeagueAttachment posts a link with its url`() = runTest {
        val body = CreateAttachmentBody(
            kind = AttachmentKind.LINK,
            title = "Rules",
            url = "https://rules",
            note = null,
        )
        coEvery { api.postLeagueAttachment("lg-1", body) } returns attachment("att-1", AttachmentKind.LINK)

        val result = repo.addLeagueAttachment("lg-1", AttachmentKind.LINK, "Rules", url = "https://rules")

        assertThat(result.id).isEqualTo("att-1")
        coVerify { api.postLeagueAttachment("lg-1", body) }
    }

    @Test
    fun `addLeagueAttachment rejects a link with no url`() = runTest {
        val thrown = runCatching {
            repo.addLeagueAttachment("lg-1", AttachmentKind.LINK, "Rules", url = null)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
        // The API is never called when validation fails.
        coVerify(exactly = 0) { api.postLeagueAttachment(any(), any()) }
    }

    @Test
    fun `addLeagueAttachment rejects a note with no note text`() = runTest {
        val thrown = runCatching {
            repo.addLeagueAttachment("lg-1", AttachmentKind.NOTE, "Reminder", note = null)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { api.postLeagueAttachment(any(), any()) }
    }

    @Test
    fun `addLeagueAttachment posts a note with its text`() = runTest {
        val body = CreateAttachmentBody(
            kind = AttachmentKind.NOTE,
            title = "Reminder",
            url = null,
            note = "Submit by Sunday",
        )
        coEvery { api.postLeagueAttachment("lg-1", body) } returns attachment("att-2", AttachmentKind.NOTE)

        val result = repo.addLeagueAttachment("lg-1", AttachmentKind.NOTE, "Reminder", note = "Submit by Sunday")

        assertThat(result.id).isEqualTo("att-2")
        coVerify { api.postLeagueAttachment("lg-1", body) }
    }

    @Test
    fun `deleteLeagueAttachment calls the API`() = runTest {
        coEvery { api.deleteLeagueAttachment("lg-1", "att-1") } returns Unit

        repo.deleteLeagueAttachment("lg-1", "att-1")

        coVerify { api.deleteLeagueAttachment("lg-1", "att-1") }
    }
}
