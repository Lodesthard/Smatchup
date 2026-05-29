package com.example.smatchup.domain.model

data class TierGroup(val tier: Tier, val characters: List<Character>)

data class TierlistView(
    val name: String,
    val version: String,
    val groups: List<TierGroup>,
)
