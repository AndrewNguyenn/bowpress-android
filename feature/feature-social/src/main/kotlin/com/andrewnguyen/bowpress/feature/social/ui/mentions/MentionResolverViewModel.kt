package com.andrewnguyen.bowpress.feature.social.ui.mentions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.HandleSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared `@`-mention plumbing (mentions contract §3) — handle search for the
 * autocomplete and handle→profile resolution for a tapped mention span.
 * Reused by the feed (post titles), the comments thread (comment bodies), and
 * the session-setup name field.
 *
 * Resolution is best-effort — an unresolved handle (no real archer, the caller
 * themselves, offline) simply does not navigate, matching the contract's
 * "an unresolved `@foo` renders as plain text / no-op tap".
 */
@HiltViewModel
class MentionResolverViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    /**
     * Handle search backing an `@`-autocomplete composer (§3.1). [prefix] is
     * the in-progress `@token` text; returns up to 8 friends-first
     * suggestions, empty on a blank prefix or a failed request.
     */
    suspend fun searchHandles(prefix: String): List<HandleSuggestion> =
        runCatching { socialRepository.searchHandles(prefix) }.getOrDefault(emptyList())

    /** The in-flight mention resolve, if any — see [openMention]. */
    private var resolveJob: Job? = null

    /**
     * Resolve [handle] and, on success, invoke [onResolved] with the archer's
     * `userId`. A handle that resolves to nothing (or fails) is a silent
     * no-op. The `@` prefix is tolerated.
     *
     * A single-flight guard: two quick taps on mention spans would otherwise
     * each launch a resolve and each push a profile route, double-stacking the
     * back stack. The previous resolve is cancelled so only the latest tap
     * navigates.
     */
    fun openMention(handle: String, onResolved: (userId: String) -> Unit) {
        val clean = handle.removePrefix("@").trim()
        if (clean.isEmpty()) return
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            val profile = socialRepository.resolveHandle(clean)
            if (profile != null) onResolved(profile.userId)
        }
    }
}
