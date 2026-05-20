package com.andrewnguyen.bowpress.feature.social

import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.AttachmentKind
import com.andrewnguyen.bowpress.core.model.LeagueAttachment
import com.andrewnguyen.bowpress.feature.social.ui.leagues.LeagueViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Covers the §17 league attachment section on [LeagueViewModel] — load, add
 * (with kind validation surfaced as an error), and delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeagueAttachmentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SocialRepository

    private fun attachment(id: String, kind: AttachmentKind) = LeagueAttachment(
        id = id,
        leagueId = "lg-1",
        addedByUserId = "me",
        addedByHandle = "andrew.n",
        kind = kind,
        title = "Title $id",
        url = if (kind != AttachmentKind.NOTE) "https://x/$id" else null,
        note = if (kind == AttachmentKind.NOTE) "Note $id" else null,
        createdAt = Instant.now(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.observeLeagues() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadLeagueHome populates the attachments section`() = runTest {
        coEvery { repository.getLeague("lg-1") } returns mockk(relaxed = true)
        coEvery { repository.getLeagueStandings("lg-1") } returns emptyList()
        coEvery { repository.getLeagueSubmissions("lg-1") } returns emptyList()
        coEvery { repository.getLeagueAttachments("lg-1") } returns listOf(
            attachment("att-1", AttachmentKind.LINK),
            attachment("att-2", AttachmentKind.NOTE),
        )

        val vm = LeagueViewModel(repository)
        vm.loadLeagueHome("lg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.leagueHomeState.value.attachments.map { it.id })
            .containsExactly("att-1", "att-2")
    }

    @Test
    fun `addAttachment adds, reloads, and runs onAdded`() = runTest {
        coEvery { repository.getLeague("lg-1") } returns mockk(relaxed = true)
        coEvery { repository.getLeagueStandings("lg-1") } returns emptyList()
        coEvery { repository.getLeagueSubmissions("lg-1") } returns emptyList()
        coEvery { repository.getLeagueAttachments("lg-1") } returns emptyList()
        coEvery {
            repository.addLeagueAttachment("lg-1", AttachmentKind.LINK, "Rules", "https://r", null)
        } returns attachment("att-new", AttachmentKind.LINK)

        val vm = LeagueViewModel(repository)
        vm.loadLeagueHome("lg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        var onAddedRan = false
        coEvery { repository.getLeagueAttachments("lg-1") } returns listOf(
            attachment("att-new", AttachmentKind.LINK),
        )
        vm.addAttachment("lg-1", AttachmentKind.LINK, "Rules", "https://r", null) {
            onAddedRan = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.addLeagueAttachment("lg-1", AttachmentKind.LINK, "Rules", "https://r", null)
        }
        assertThat(onAddedRan).isTrue()
        assertThat(vm.leagueHomeState.value.attachments.map { it.id }).containsExactly("att-new")
    }

    @Test
    fun `an add failure surfaces the attachment error`() = runTest {
        coEvery { repository.getLeague("lg-1") } returns mockk(relaxed = true)
        coEvery { repository.getLeagueAttachments("lg-1") } returns emptyList()
        coEvery { repository.addLeagueAttachment(any(), any(), any(), any(), any()) } throws
            IllegalArgumentException("A LINK attachment needs a URL.")

        val vm = LeagueViewModel(repository)
        vm.loadLeagueHome("lg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.addAttachment("lg-1", AttachmentKind.LINK, "Rules", url = null, note = null) { }
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.leagueHomeState.value.attachmentError)
            .isEqualTo("A LINK attachment needs a URL.")
    }

    @Test
    fun `deleteAttachment drops the row optimistically`() = runTest {
        coEvery { repository.getLeague("lg-1") } returns mockk(relaxed = true)
        coEvery { repository.getLeagueStandings("lg-1") } returns emptyList()
        coEvery { repository.getLeagueSubmissions("lg-1") } returns emptyList()
        coEvery { repository.getLeagueAttachments("lg-1") } returns listOf(
            attachment("att-1", AttachmentKind.LINK),
            attachment("att-2", AttachmentKind.NOTE),
        )
        coEvery { repository.deleteLeagueAttachment("lg-1", "att-1") } returns Unit

        val vm = LeagueViewModel(repository)
        vm.loadLeagueHome("lg-1")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteAttachment("lg-1", "att-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteLeagueAttachment("lg-1", "att-1") }
        assertThat(vm.leagueHomeState.value.attachments.map { it.id }).containsExactly("att-2")
    }
}
