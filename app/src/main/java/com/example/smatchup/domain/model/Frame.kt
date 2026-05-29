package com.example.smatchup.domain.model

data class Frame(
    val startup: Int? = null,         // first hitbox frame
    val active: IntRange? = null,     // active hitbox window
    val totalFrames: Int? = null,
    val landingLag: Int? = null,
    val endLag: Int? = null,          // recovery frames after the move starts (≈ total − startup)
    val onShield: Int? = null,        // frame advantage when the move hits a shield (− = punishable)
    val baseDamage: Float? = null,
    val baseKnockback: Float? = null,
    val knockbackGrowth: Float? = null,
    val angle: Int? = null            // launch angle degrees
)
