// app/src/main/java/com/example/smatchup/data/local/entity/CachedYoutubeVideoEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_youtube_video")
data class CachedYoutubeVideoEntity(
    @PrimaryKey val cacheKey: String,       // "char:<id>" or "mu:<a>_<b>"
    val videoId: String,
    val title: String,
    val publishedAt: Long,
    val fetchedAt: Long
)
