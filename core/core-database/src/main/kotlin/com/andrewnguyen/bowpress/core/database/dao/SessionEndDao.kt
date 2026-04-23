package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.SessionEndEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEndDao {

    @Query("SELECT * FROM session_ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun observeBySession(sessionId: String): Flow<List<SessionEndEntity>>

    @Query("SELECT * FROM session_ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    suspend fun findBySession(sessionId: String): List<SessionEndEntity>

    @Query("SELECT * FROM session_ends WHERE pendingSync = 1 ORDER BY completedAt ASC")
    suspend fun findPendingSync(): List<SessionEndEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(end: SessionEndEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(ends: List<SessionEndEntity>)

    @Query("UPDATE session_ends SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM session_ends WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM session_ends")
    suspend fun clear()
}
