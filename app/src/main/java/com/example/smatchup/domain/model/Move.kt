package com.example.smatchup.domain.model

enum class MoveCategory { JAB, TILT, SMASH, AERIAL, SPECIAL, GRAB, THROW, MOVEMENT }

data class Move(
    val id: String,                   // e.g. "fair", "nair", "neutral_b"
    val displayName: String,
    val category: MoveCategory,
    val frame: Frame,
    val utility: String? = null       // curated description: kill move, combo starter, etc.
)
