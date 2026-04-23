package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrewnguyen.bowpress.core.database.entities.BowConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BowConfigDao {

    @Query("SELECT * FROM bow_configurations WHERE bowId = :bowId ORDER BY createdAt DESC")
    fun observeByBow(bowId: String): Flow<List<BowConfigEntity>>

    @Query("SELECT * FROM bow_configurations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BowConfigEntity>>

    @Query("SELECT * FROM bow_configurations WHERE bowId = :bowId ORDER BY createdAt DESC")
    suspend fun findByBow(bowId: String): List<BowConfigEntity>

    @Query("SELECT * FROM bow_configurations WHERE id = :id")
    suspend fun findById(id: String): BowConfigEntity?

    @Query("SELECT * FROM bow_configurations WHERE pendingSync = 1 ORDER BY createdAt ASC")
    suspend fun findPendingSync(): List<BowConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: BowConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(configs: List<BowConfigEntity>)

    @Update
    suspend fun update(config: BowConfigEntity)

    @Query("UPDATE bow_configurations SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM bow_configurations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bow_configurations")
    suspend fun clear()
}
