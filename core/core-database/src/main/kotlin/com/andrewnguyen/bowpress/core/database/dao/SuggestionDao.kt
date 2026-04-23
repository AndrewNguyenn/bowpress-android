package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.SuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {

    @Query("SELECT * FROM suggestions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM suggestions WHERE bowId = :bowId ORDER BY createdAt DESC")
    fun observeByBow(bowId: String): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM suggestions WHERE id = :id")
    suspend fun findById(id: String): SuggestionEntity?

    @Query("SELECT * FROM suggestions WHERE bowId = :bowId AND wasDismissed = 0 ORDER BY createdAt DESC")
    suspend fun findActiveByBow(bowId: String): List<SuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(suggestion: SuggestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(suggestions: List<SuggestionEntity>)

    @Query("UPDATE suggestions SET wasRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE suggestions SET wasDismissed = 1 WHERE id = :id")
    suspend fun markDismissed(id: String)

    @Query("DELETE FROM suggestions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM suggestions")
    suspend fun clear()
}
