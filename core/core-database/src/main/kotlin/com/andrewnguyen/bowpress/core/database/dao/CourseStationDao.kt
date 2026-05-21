package com.andrewnguyen.bowpress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrewnguyen.bowpress.core.database.entities.CourseStationEntity
import kotlinx.coroutines.flow.Flow

/** Mirrors iOS `LocalStore`'s course-station CRUD. */
@Dao
interface CourseStationDao {

    @Query("SELECT * FROM course_stations ORDER BY sessionId ASC, stationNumber ASC")
    fun observeAll(): Flow<List<CourseStationEntity>>

    @Query("SELECT * FROM course_stations WHERE sessionId = :sessionId ORDER BY stationNumber ASC")
    fun observeBySession(sessionId: String): Flow<List<CourseStationEntity>>

    @Query("SELECT * FROM course_stations WHERE sessionId = :sessionId ORDER BY stationNumber ASC")
    suspend fun findBySession(sessionId: String): List<CourseStationEntity>

    @Query("SELECT * FROM course_stations WHERE pendingSync = 1 ORDER BY shotAt ASC")
    suspend fun findPendingSync(): List<CourseStationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(station: CourseStationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stations: List<CourseStationEntity>)

    @Update
    suspend fun update(station: CourseStationEntity)

    @Query("UPDATE course_stations SET pendingSync = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM course_stations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM course_stations WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
