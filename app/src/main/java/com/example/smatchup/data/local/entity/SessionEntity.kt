// app/src/main/java/com/example/smatchup/data/local/entity/SessionEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 0,            // singleton row
    val userId: Long?,
    val updatedAt: Long
)
