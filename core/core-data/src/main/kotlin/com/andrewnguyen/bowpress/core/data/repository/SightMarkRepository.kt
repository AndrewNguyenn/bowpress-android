package com.andrewnguyen.bowpress.core.data.repository

import com.andrewnguyen.bowpress.core.data.converters.toDto
import com.andrewnguyen.bowpress.core.data.converters.toEntity
import com.andrewnguyen.bowpress.core.database.dao.SightMarkDao
import com.andrewnguyen.bowpress.core.model.SightMark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first sight-mark store. Mirrors iOS `LocalStore` sight-mark
 * methods. The backend doesn't yet have a sight-marks endpoint —
 * pendingSync is staged on every write so the moment the API lands,
 * `flushPendingSync` can drain them without a client release.
 */
@Singleton
class SightMarkRepository @Inject constructor(
    private val dao: SightMarkDao,
) {
    fun observeByBow(bowId: String): Flow<List<SightMark>> =
        dao.observeByBow(bowId).map { rows -> rows.map { it.toDto() } }

    suspend fun getByBow(bowId: String): List<SightMark> =
        dao.findByBow(bowId).map { it.toDto() }

    suspend fun getById(id: String): SightMark? = dao.findById(id)?.toDto()

    suspend fun save(mark: SightMark) {
        dao.upsert(mark.toEntity(pendingSync = true))
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
