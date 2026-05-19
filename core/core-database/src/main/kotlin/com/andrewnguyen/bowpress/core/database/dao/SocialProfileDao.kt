package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.SocialProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialProfileDao {

    @Query("SELECT * FROM social_profiles WHERE userId = :userId")
    fun observe(userId: String): Flow<SocialProfileEntity?>

    @Query("SELECT * FROM social_profiles WHERE userId = :userId")
    suspend fun findById(userId: String): SocialProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SocialProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SocialProfileEntity>)

    @Query("DELETE FROM social_profiles WHERE userId = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM social_profiles")
    suspend fun clear()
}
