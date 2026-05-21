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

    /**
     * Patch the like state on every cached feed row sharing [subjectId] — a
     * shared session writes one friend row + N club rows that the feed
     * de-dupes by subject, so a like-toggle must touch them all (§5.1). Used
     * for the optimistic like-button update.
     */
    @Query(
        "UPDATE activity_feed SET likeCount = :likeCount, likedByMe = :likedByMe " +
            "WHERE subjectId = :subjectId",
    )
    suspend fun updateLikeState(subjectId: String, likeCount: Int, likedByMe: Boolean)

    /**
     * Atomically adjust the comment count on every cached row sharing
     * [subjectId] by [delta] (§5.1). The arithmetic happens inside the single
     * SQL `UPDATE` so two concurrent comments cannot lose an increment, and
     * the count is clamped at 0 (`MAX(0, …)`) so a delete on a stale-zero row
     * never goes negative.
     */
    @Query(
        "UPDATE activity_feed SET commentCount = MAX(0, commentCount + :delta) " +
            "WHERE subjectId = :subjectId",
    )
    suspend fun adjustCommentCount(subjectId: String, delta: Int)
}
