package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read cache for §15 achievements (trophy case). Online-first like the rest of
 * the social tables — the API is the source of truth; the repository upserts
 * fresh fetches here so the trophy case renders reactively and the DEBUG seed
 * works offline. Rows are keyed by `userId` so the signed-in user's case and a
 * friend's case can coexist in one table.
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getForUser(userId: String): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AchievementEntity>)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
