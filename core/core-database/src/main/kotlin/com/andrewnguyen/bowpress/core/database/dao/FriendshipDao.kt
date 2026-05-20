package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andrewnguyen.bowpress.core.database.entities.FriendshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendshipDao {

    @Query("SELECT * FROM friendships WHERE status = 'accepted' ORDER BY createdAt DESC")
    fun observeFriends(): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships WHERE status = 'pending' ORDER BY createdAt DESC")
    fun observePendingRequests(): Flow<List<FriendshipEntity>>

    /** Incoming pending friend requests only — feeds the Social-tab badge (§12). */
    @Query("SELECT COUNT(*) FROM friendships WHERE status = 'pending' AND direction = 'incoming'")
    suspend fun incomingPendingCount(): Int

    @Query("SELECT * FROM friendships ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships WHERE status = 'accepted' ORDER BY createdAt DESC")
    suspend fun getFriends(): List<FriendshipEntity>

    @Query("SELECT * FROM friendships WHERE id = :id")
    suspend fun findById(id: String): FriendshipEntity?

    @Query("SELECT * FROM friendships WHERE otherUserId = :otherUserId LIMIT 1")
    suspend fun findByOtherUserId(otherUserId: String): FriendshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FriendshipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FriendshipEntity>)

    @Query("DELETE FROM friendships WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM friendships WHERE otherUserId = :otherUserId")
    suspend fun deleteByOtherUserId(otherUserId: String)

    @Query("DELETE FROM friendships")
    suspend fun clear()
}
