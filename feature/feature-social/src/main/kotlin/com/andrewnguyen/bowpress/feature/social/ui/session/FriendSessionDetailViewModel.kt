package com.andrewnguyen.bowpress.feature.social.ui.session

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.SocialRepository
import com.andrewnguyen.bowpress.core.data.social.PhotoDownscaler
import com.andrewnguyen.bowpress.core.model.ActivityPhoto
import com.andrewnguyen.bowpress.core.model.SessionLocation
import com.andrewnguyen.bowpress.core.model.SharedSessionDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the shared-session detail screen — §16 (a friend's session) plus
 * Social Feed V2 §3/§4 (the owner-editable mode: edit title/location, manage
 * the multi-photo gallery).
 */
data class FriendSessionDetailUiState(
    val detail: SharedSessionDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * True when the signed-in user owns this shared session — only an owner
     * sees the edit affordance and the photo-management controls.
     */
    val isOwn: Boolean = false,
    /** True while an edit (title/location) or a photo op is in flight. */
    val isSaving: Boolean = false,
)

@HiltViewModel
class FriendSessionDetailViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val photoDownscaler: PhotoDownscaler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendSessionDetailUiState(isLoading = true))
    val uiState: StateFlow<FriendSessionDetailUiState> = _uiState.asStateFlow()

    private var sharedSessionId: String = ""

    /**
     * Photo loader for the gallery — fetches the Bearer-gated display JPEG
     * through the repository.
     */
    val photoLoader = SessionPhotoLoader { ssId, photoId ->
        socialRepository.fetchSharedSessionPhotoBytes(ssId, photoId)
    }

    /**
     * Resolve the detail for [sharedSessionId]. [isOwn] is forwarded from the
     * tapped feed row — an own row routes here in editable mode.
     */
    fun load(sharedSessionId: String, isOwn: Boolean = false) {
        this.sharedSessionId = sharedSessionId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isOwn = isOwn) }
            runCatching { socialRepository.getSharedSessionDetail(sharedSessionId) }
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    /**
     * Owner edit (§3) — apply a new [title] and [location]. A blank title or a
     * null location clears that field. The repository diffs against the
     * loaded summary and PATCHes only what actually changed, so a location-only
     * edit does not trigger a spurious session rename. On success the detail is
     * reloaded so the header reflects the change.
     */
    fun saveEdit(title: String, location: SessionLocation?) {
        val ssId = sharedSessionId.ifBlank { return }
        val loaded = _uiState.value.detail?.sharedSession
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                socialRepository.editSharedSession(
                    sharedSessionId = ssId,
                    newTitle = title,
                    newLocation = location,
                    // The values the screen loaded — the repository diffs
                    // against these to send a true partial update.
                    originalTitle = loaded?.title,
                    originalLocation = loaded?.location,
                )
            }.onSuccess {
                reloadDetailKeepingPhotos()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }

    /**
     * Owner photo add (§4) — downscale each picked [uris] entry and upload it.
     *
     * The §4 cap is 8 photos per shared session. The picker only bounds a
     * *single* pick, so a second pick could still push the total over 8 — the
     * batch is trimmed here to the remaining capacity before any upload. Picks
     * that cannot be read are skipped. The gallery is refreshed after **each**
     * successful upload so photos appear as they land rather than all at once.
     */
    fun addPhotos(uris: List<Uri>) {
        val ssId = sharedSessionId.ifBlank { return }
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val currentCount = _uiState.value.detail?.photos?.size ?: 0
            val remaining = (MAX_PHOTOS - currentCount).coerceAtLeast(0)
            val accepted = uris.take(remaining)
            val droppedForCap = uris.size - accepted.size

            var failures = 0
            for (uri in accepted) {
                val bytes = photoDownscaler.downscaleToJpeg(uri)
                if (bytes == null) {
                    failures++
                    continue
                }
                runCatching { socialRepository.uploadSharedSessionPhoto(ssId, bytes) }
                    .onSuccess {
                        // Surface each photo as it lands.
                        refreshPhotos()
                    }
                    .onFailure { failures++ }
            }

            val message = when {
                droppedForCap > 0 && failures > 0 ->
                    "$failures photo(s) failed; $droppedForCap skipped (limit $MAX_PHOTOS)."
                droppedForCap > 0 ->
                    "$droppedForCap photo(s) skipped — the limit is $MAX_PHOTOS."
                failures > 0 -> "$failures photo(s) could not be added."
                else -> null
            }
            _uiState.update {
                it.copy(isSaving = false, error = message ?: it.error)
            }
        }
    }

    /** Owner photo remove (§4) — delete [photo] and refresh the gallery. */
    fun removePhoto(photo: ActivityPhoto) {
        val ssId = sharedSessionId.ifBlank { return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching { socialRepository.deleteSharedSessionPhoto(ssId, photo.id) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            refreshPhotos()
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    /** Re-pull the photo list and merge it onto the current detail. */
    private suspend fun refreshPhotos() {
        val ssId = sharedSessionId.ifBlank { return }
        val photos = runCatching { socialRepository.listSharedSessionPhotos(ssId) }
            .getOrNull()
            ?.map { ActivityPhoto(id = it.id, status = it.status, position = it.position) }
            ?: return
        _uiState.update { state ->
            val detail = state.detail ?: return@update state
            state.copy(detail = detail.copy(photos = photos))
        }
    }

    /**
     * Reload the detail after a title/location edit, preserving the photo
     * list already on screen (the §3 PATCH response carries no photos).
     */
    private suspend fun reloadDetailKeepingPhotos() {
        val ssId = sharedSessionId.ifBlank { return }
        val existingPhotos = _uiState.value.detail?.photos.orEmpty()
        runCatching { socialRepository.getSharedSessionDetail(ssId) }
            .onSuccess { fresh ->
                val merged = if (fresh.photos.isEmpty() && existingPhotos.isNotEmpty()) {
                    fresh.copy(photos = existingPhotos)
                } else {
                    fresh
                }
                _uiState.update { it.copy(detail = merged, isSaving = false) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        /** Max photos per shared session — mirrors the API contract §4 cap. */
        const val MAX_PHOTOS = 8
    }
}
