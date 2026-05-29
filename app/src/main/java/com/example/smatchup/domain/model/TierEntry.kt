package com.example.smatchup.domain.model

enum class Tier { S_PLUS, S, A, B, C, D, E, F }

data class TierEntry(val charId: String, val tier: Tier)

data class TierList(
    val name: String,                 // "strength" or "difficulty"
    val version: String,              // e.g. "UltRank 2026 v4"
    val updatedAt: Long,
    val entries: List<TierEntry>
)
