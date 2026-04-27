package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrewnguyen.bowpress.core.database.entities.ArrowPlotEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ArrowPlotDao {

    @Query("SELECT * FROM arrow_plots WHERE sessionId = :sessionId ORDER BY shotAt ASC")
    fun observeBySession(sessionId: String): Flow<List<ArrowPlotEntity>>

    @Query("SELECT * FROM arrow_plots ORDER BY shotAt ASC")
    fun observeAll(): Flow<List<ArrowPlotEntity>>

    @Query("SELECT * FROM arrow_plots WHERE sessionId = :sessionId ORDER BY shotAt ASC")
    suspend fun findBySession(sessionId: String): List<ArrowPlotEntity>

    @Query("SELECT * FROM arrow_plots WHERE shotAt >= :since ORDER BY shotAt ASC")
    suspend fun findSince(since: Instant): List<ArrowPlotEntity>

    @Query("SELECT * FROM arrow_plots ORDER BY shotAt ASC")
    suspend fun getAll(): List<ArrowPlotEntity>

    @Query("SELECT * FROM arrow_plots WHERE pendingSync = 1 ORDER BY shotAt ASC")
    suspend fun findPendingSync(): List<ArrowPlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plot: ArrowPlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plots: List<ArrowPlotEntity>)

    @Update
    suspend fun update(plot: ArrowPlotEntity)

    @Query("UPDATE arrow_plots SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM arrow_plots WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM arrow_plots WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM arrow_plots WHERE endId = :endId")
    suspend fun deleteByEndId(endId: String)

    @Query("DELETE FROM arrow_plots")
    suspend fun clear()
}
