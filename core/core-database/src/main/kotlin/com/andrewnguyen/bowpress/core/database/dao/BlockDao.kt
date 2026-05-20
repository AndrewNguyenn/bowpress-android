package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.BlockEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read cache for the signed-in user's mutes + blocks (§14). Like the rest of
 * the social tables this is an online-first cache — the API is the source of
 * truth and the repository upserts fresh fetches here so the "Muted & blocked"
 * list renders reactively and the DEBUG seed works offline.
 */
@Dao
interface BlockDao {

    @Query("SELECT * FROM blocks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BlockEntity>)

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM blocks")
    suspend fun clear()
}
