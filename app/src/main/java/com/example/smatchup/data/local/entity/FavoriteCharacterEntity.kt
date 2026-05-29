// app/src/main/java/com/example/smatchup/data/local/entity/FavoriteCharacterEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "favorite_characters",
    primaryKeys = ["userId", "charId"],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FavoriteCharacterEntity(
    val userId: Long,
    val charId: String,
    val addedAt: Long
)
