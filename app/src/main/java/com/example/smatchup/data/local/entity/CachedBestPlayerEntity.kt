// app/src/main/java/com/example/smatchup/data/local/entity/CachedBestPlayerEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_best_players")
data class CachedBestPlayerEntity(
    @PrimaryKey val charId: String,
    val playerTag: String,
    val startGgPlayerId: Long?,
    val score: Float,
    val computedAt: Long
)
