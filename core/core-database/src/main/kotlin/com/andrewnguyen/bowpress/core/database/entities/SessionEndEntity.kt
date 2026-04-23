package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Mirrors iOS `PersistentEnd`. */
@Entity(
    tableName = "session_ends",
    indices = [Index("sessionId")],
)
data class SessionEndEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val endNumber: Int,
    val notes: String? = null,
    val completedAt: Instant,
    val pendingSync: Boolean = false,
)
