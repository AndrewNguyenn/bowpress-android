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

    /**
     * The cached profile for whoever is signed in. Only one profile is ever
     * cached locally (the current user's), so this is an id-free fallback for
     * surfacing the seeded/cached profile when a remote refresh fails.
     */
    @Query("SELECT * FROM social_profiles LIMIT 1")
    suspend fun findAny(): SocialProfileEntity?

    @Query("SELECT * FROM social_profiles LIMIT 1")
    fun observeAny(): Flow<SocialProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SocialProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SocialProfileEntity>)

    @Query("DELETE FROM social_profiles WHERE userId = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM social_profiles")
    suspend fun clear()
}
