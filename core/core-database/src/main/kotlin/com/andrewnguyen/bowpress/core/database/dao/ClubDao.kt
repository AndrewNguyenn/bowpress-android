package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.andrewnguyen.bowpress.core.database.entities.ClubEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {

    @Query("SELECT * FROM clubs ORDER BY name ASC")
    fun observeAll(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs ORDER BY name ASC")
    suspend fun getAll(): List<ClubEntity>

    @Query("SELECT * FROM clubs WHERE id = :id")
    suspend fun findById(id: String): ClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClubEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ClubEntity>)

    @Query("DELETE FROM clubs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clubs WHERE id NOT IN (:ids)")
    suspend fun deleteWhereIdNotIn(ids: List<String>)

    @Query("DELETE FROM clubs")
    suspend fun clear()

    /**
     * Replace the cached club set with [entities] in one transaction:
     * any local row whose id isn't in the new set is removed, then the
     * new set is upserted. Used by `refreshClubs` so a club deleted
     * server-side actually disappears from the device — without this,
     * the cache only ever grows (upserts add/update but never reconcile
     * deletions).
     */
    @Transaction
    suspend fun replaceAll(entities: List<ClubEntity>) {
        deleteWhereIdNotIn(entities.map { it.id })
        upsertAll(entities)
    }
}
