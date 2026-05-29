// app/src/main/java/com/example/smatchup/data/local/entity/FavoriteMatchupEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "favorite_matchups",
    primaryKeys = ["userId", "charA", "charB"],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FavoriteMatchupEntity(
    val userId: Long,
    val charA: String,        // normalized: charA <= charB lexicographically
    val charB: String,
    val addedAt: Long
)
