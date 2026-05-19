package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM clubs WHERE pendingSync = 1")
    suspend fun findPendingSync(): List<ClubEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClubEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ClubEntity>)

    @Query("DELETE FROM clubs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clubs")
    suspend fun clear()
}
