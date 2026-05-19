package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.ActivityItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityFeedDao {

    @Query("SELECT * FROM activity_feed ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActivityItemEntity>>

    @Query("SELECT * FROM activity_feed ORDER BY createdAt DESC")
    suspend fun getAll(): List<ActivityItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ActivityItemEntity>)

    @Query("DELETE FROM activity_feed")
    suspend fun clear()
}
