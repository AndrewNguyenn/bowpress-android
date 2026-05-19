package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.SightMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SightMarkDao {

    @Query("SELECT * FROM sight_marks WHERE bowId = :bowId ORDER BY distance ASC")
    fun observeByBow(bowId: String): Flow<List<SightMarkEntity>>

    @Query("SELECT * FROM sight_marks WHERE bowId = :bowId ORDER BY distance ASC")
    suspend fun findByBow(bowId: String): List<SightMarkEntity>

    @Query("SELECT * FROM sight_marks WHERE id = :id")
    suspend fun findById(id: String): SightMarkEntity?

    @Query("SELECT * FROM sight_marks WHERE pendingSync = 1 ORDER BY createdAt ASC")
    suspend fun findPendingSync(): List<SightMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mark: SightMarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(marks: List<SightMarkEntity>)

    @Query("UPDATE sight_marks SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM sight_marks WHERE id = :id")
    suspend fun deleteById(id: String)
}
