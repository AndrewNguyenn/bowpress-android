package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrewnguyen.bowpress.core.database.entities.ArrowConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArrowConfigDao {

    @Query("SELECT * FROM arrow_configurations ORDER BY label ASC")
    fun observeAll(): Flow<List<ArrowConfigEntity>>

    @Query("SELECT * FROM arrow_configurations ORDER BY label ASC")
    suspend fun getAll(): List<ArrowConfigEntity>

    @Query("SELECT * FROM arrow_configurations WHERE id = :id")
    suspend fun findById(id: String): ArrowConfigEntity?

    @Query("SELECT * FROM arrow_configurations WHERE pendingSync = 1 ORDER BY label ASC")
    suspend fun findPendingSync(): List<ArrowConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ArrowConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(configs: List<ArrowConfigEntity>)

    @Update
    suspend fun update(config: ArrowConfigEntity)

    @Query("UPDATE arrow_configurations SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM arrow_configurations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM arrow_configurations")
    suspend fun clear()
}
