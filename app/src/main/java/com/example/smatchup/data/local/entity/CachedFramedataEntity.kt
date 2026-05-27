// app/src/main/java/com/example/smatchup/data/local/entity/CachedFramedataEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_framedata")
data class CachedFramedataEntity(
    @PrimaryKey val charId: String,
    val jsonBlob: String,
    val fetchedAt: Long
)
