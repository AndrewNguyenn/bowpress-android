package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrewnguyen.bowpress.core.database.entities.BowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BowDao {

    @Query("SELECT * FROM bows ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BowEntity>>

    @Query("SELECT * FROM bows ORDER BY createdAt DESC")
    suspend fun getAll(): List<BowEntity>

    @Query("SELECT * FROM bows WHERE id = :id")
    suspend fun findById(id: String): BowEntity?

    @Query("SELECT * FROM bows WHERE pendingSync = 1 ORDER BY createdAt ASC")
    suspend fun findPendingSync(): List<BowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bow: BowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bows: List<BowEntity>)

    @Update
    suspend fun update(bow: BowEntity)

    @Query("UPDATE bows SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM bows WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bows")
    suspend fun clear()
}
