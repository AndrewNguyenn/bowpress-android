package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.andrewnguyen.bowpress.core.database.entities.LeagueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeagueDao {

    @Query("SELECT * FROM leagues ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues ORDER BY createdAt DESC")
    suspend fun getAll(): List<LeagueEntity>

    @Query("SELECT * FROM leagues WHERE id = :id")
    suspend fun findById(id: String): LeagueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LeagueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LeagueEntity>)

    @Query("DELETE FROM leagues WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM leagues WHERE id NOT IN (:ids)")
    suspend fun deleteWhereIdNotIn(ids: List<String>)

    @Query("DELETE FROM leagues")
    suspend fun clear()

    /** See [ClubDao.replaceAll] — same reconcile pattern, same rationale. */
    @Transaction
    suspend fun replaceAll(entities: List<LeagueEntity>) {
        deleteWhereIdNotIn(entities.map { it.id })
        upsertAll(entities)
    }
}
