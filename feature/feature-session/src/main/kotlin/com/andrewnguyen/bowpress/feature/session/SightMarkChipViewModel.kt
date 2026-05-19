package com.andrewnguyen.bowpress.feature.session

import androidx.lifecycle.ViewModel
import com.andrewnguyen.bowpress.core.data.repository.SightMarkRepository
import com.andrewnguyen.bowpress.core.model.SightMark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Tiny VM that just exposes the active bow's sight marks as a Flow.
 * Lives separately from SessionViewModel so the chip can be a standalone
 * composable reachable from any screen carrying (bowId, distance).
 */
@HiltViewModel
class SightMarkChipViewModel @Inject constructor(
    private val repository: SightMarkRepository,
) : ViewModel() {
    fun marksFor(bowId: String): Flow<List<SightMark>> = repository.observeByBow(bowId)
}
