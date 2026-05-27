// app/src/main/java/com/example/smatchup/data/local/entity/CachedWinrateEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity

@Entity(tableName = "cached_winrate", primaryKeys = ["charA", "charB"])
data class CachedWinrateEntity(
    val charA: String,
    val charB: String,
    val winRateA: Float,
    val sampleSize: Int,
    val majorsCount: Int,
    val computedAt: Long
)
