package com.andrewnguyen.bowpress.feature.social.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrewnguyen.bowpress.core.data.repository.LocationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Parity E7 — drives the once-per-device [LocationSoftPromptSheet]. The
 * scaffold collects [shouldShow] and renders the sheet when true; the
 * sheet's allow / dismiss paths both call [markSeen] so the sheet is
 * never re-presented (mirrors iOS `LocationSoftPrompt.markShown()`).
 */
@HiltViewModel
class LocationSoftPromptViewModel @Inject constructor(
    private val prefs: LocationPreferencesRepository,
) : ViewModel() {

    /**
     * True when the sheet should be presented — only the FIRST time per
     * install. The sheet doesn't read the actual system permission status;
     * a user who taps Allow then revokes in Settings has already "seen" the
     * value prop, so re-presenting on relaunch would be noise.
     */
    val shouldShow: StateFlow<Boolean> = prefs.hasSeenSoftPrompt
        .map { seen -> !seen }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun markSeen() {
        viewModelScope.launch { prefs.markSoftPromptShown() }
    }
}
