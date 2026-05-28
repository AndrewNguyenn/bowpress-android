package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.ExportJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportJobDao {

    /** Reactive stream of every job, newest first — backs the feed chip. */
    @Query("SELECT * FROM export_jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExportJobEntity>>

    @Query("SELECT * FROM export_jobs ORDER BY createdAt DESC")
    suspend fun getAll(): List<ExportJobEntity>

    @Query("SELECT * FROM export_jobs WHERE id = :id")
    suspend fun findById(id: String): ExportJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM export_jobs")
    suspend fun clear()
}
