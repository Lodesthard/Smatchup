package com.example.smatchup.domain.model

data class Player(
    val tag: String,
    val startGgId: Long? = null,
    val mainCharacter: String? = null,    // character id
    val score: Float = 0f                  // best-player score, computed
)
