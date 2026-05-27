package com.example.smatchup.domain.model

data class Frame(
    val startup: Int? = null,         // first hitbox frame
    val active: IntRange? = null,     // active hitbox window
    val totalFrames: Int? = null,
    val landingLag: Int? = null,
    val baseDamage: Float? = null,
    val baseKnockback: Float? = null,
    val knockbackGrowth: Float? = null,
    val angle: Int? = null            // launch angle degrees
)
