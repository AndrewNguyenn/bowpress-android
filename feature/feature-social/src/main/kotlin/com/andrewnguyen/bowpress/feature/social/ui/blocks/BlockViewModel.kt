package com.andrewnguyen.bowpress.feature.social.ui.blocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.model.BlockKind
import com.andrewnguyen.bowpress.core.model.BlockMode
import com.andrewnguyen.bowpress.core.model.SocialBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mutes + blocks for the signed-in user (contract §14).
 *
 * Shared by the "Muted & blocked" managed list and the per-target Mute/Block
 * affordances on the Friend, Club, and League screens. The list is sourced
 * from the Room cache via [SocialRepository.observeBlocks] so a mute placed on
 * one screen shows up everywhere live; [loadBlocks] does the online refresh.
 */
data class BlocksUiState(
    val blocks: List<SocialBlock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val archerBlocks: List<SocialBlock> get() = blocks.filter { it.kind == BlockKind.archer }
    val clubBlocks: List<SocialBlock> get() = blocks.filter { it.kind == BlockKind.club }
    val leagueBlocks: List<SocialBlock> get() = blocks.filter { it.kind == BlockKind.league }

    /** The block on [targetId], if any — drives the per-target affordance state. */
    fun blockFor(targetId: String): SocialBlock? = blocks.firstOrNull { it.targetId == targetId }
}

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlocksUiState(isLoading = true))
    val uiState: StateFlow<BlocksUiState> = _uiState.asStateFlow()

    init {
        // Reactive: the Room cache is the read source, so a create/delete from
        // any screen flows back into every BlockViewModel instance.
        viewModelScope.launch {
            socialRepository.observeBlocks().collect { rows ->
                _uiState.update { it.copy(blocks = rows) }
            }
        }
        loadBlocks()
    }

    /** Online refresh of the mute/block list. */
    fun loadBlocks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { socialRepository.getBlocks() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Mute or block a target. Re-posting an existing target updates its mode.
     * The activity feed is refreshed afterward so a newly-muted actor's items
     * drop out immediately (the API excludes muted sources from `/social/feed`).
     */
    fun setBlock(kind: BlockKind, targetId: String, targetName: String, mode: BlockMode) {
        viewModelScope.launch {
            runCatching {
                socialRepository.createBlock(kind, targetId, mode)
                socialRepository.refreshFeed()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    /** Unmute / unblock — removes the block row. */
    fun removeBlock(id: String) {
        viewModelScope.launch {
            runCatching {
                socialRepository.deleteBlock(id)
                socialRepository.refreshFeed()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
