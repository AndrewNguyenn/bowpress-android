package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.InvitationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read cache for club/league invitations (§11). Like the rest of the social
 * tables, this is an online-first cache — the API is the source of truth and
 * the repository upserts fresh fetches here so the UI can render reactively
 * and the DEBUG seed can populate the badge offline.
 */
@Dao
interface InvitationDao {

    @Query("SELECT * FROM invitations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvitationEntity>>

    @Query("SELECT * FROM invitations ORDER BY createdAt DESC")
    suspend fun getAll(): List<InvitationEntity>

    @Query("SELECT COUNT(*) FROM invitations WHERE status = 'pending'")
    suspend fun pendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InvitationEntity>)

    @Query("DELETE FROM invitations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invitations")
    suspend fun clear()
}
