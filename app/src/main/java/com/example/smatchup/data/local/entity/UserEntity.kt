// app/src/main/java/com/example/smatchup/data/local/entity/UserEntity.kt
package com.example.smatchup.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["pseudo"], unique = true), Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pseudo") val pseudo: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "passwordHash") val passwordHash: String,
    @ColumnInfo(name = "salt") val salt: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long
)
