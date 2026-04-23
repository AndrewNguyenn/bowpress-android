package com.andrewnguyen.bowpress.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrewnguyen.bowpress.core.model.BowType
import java.time.Instant

/** Mirrors iOS `PersistentBow`. */
@Entity(tableName = "bows")
data class BowEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val bowType: BowType = BowType.COMPOUND,
    val brand: String = "",
    val model: String = "",
    val createdAt: Instant,
    val pendingSync: Boolean = false,
)
