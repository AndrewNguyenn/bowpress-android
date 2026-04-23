package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.database.dao.SuggestionDao
import com.andrewnguyen.bowpress.core.model.AnalyticsSuggestion
import com.andrewnguyen.bowpress.core.network.ApplyResult
import com.andrewnguyen.bowpress.core.network.BowPressApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionRepository @Inject constructor(
    private val dao: SuggestionDao,
    private val bowConfigRepository: BowConfigRepository,
    private val api: BowPressApi,
) {
    fun observeByBow(bowId: String): Flow<List<AnalyticsSuggestion>> =
        dao.observeByBow(bowId).map { rows -> rows.map { it.toDto() } }

    fun observeAll(): Flow<List<AnalyticsSuggestion>> =
        dao.observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun getById(id: String): AnalyticsSuggestion? = dao.findById(id)?.toDto()

    suspend fun save(suggestion: AnalyticsSuggestion) {
        dao.upsert(suggestion.toEntity())
    }

    suspend fun refreshForBow(bowId: String) {
        val remote = api.fetchSuggestions(bowId)
        dao.upsertAll(remote.map { it.toEntity() })
    }

    suspend fun markRead(id: String) {
        dao.markRead(id)
        runCatching { api.markSuggestionRead(id) }
    }

    suspend fun dismiss(id: String) {
        dao.markDismissed(id)
        runCatching { api.dismissSuggestion(id) }
    }

    /**
     * Materialize the suggestion server-side. Mirrors iOS `APIClient.applySuggestion` —
     * persist `newConfig` locally and overwrite the existing suggestion entity with the
     * returned (applied=true) version.
     */
    suspend fun apply(bowId: String, id: String): ApplyResult {
        val result = api.applySuggestion(bowId, id)
        bowConfigRepository.saveConfig(result.newConfig.copy())
        dao.upsert(result.suggestion.toEntity())
        return result
    }
}
