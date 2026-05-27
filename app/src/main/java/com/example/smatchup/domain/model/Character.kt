package com.example.smatchup.domain.model

data class Character(
    val id: String,           // canonical id, lowercase, no spaces, e.g. "steve"
    val name: String,         // display name e.g. "Steve"
    val series: String,       // e.g. "Minecraft"
    val rosterNumber: Int,    // canonical SSBU roster order
    val portraitAsset: String? = null
)
