package com.example.smatchup.domain.model

enum class StageVerdict { BAN, COUNTERPICK, NEUTRAL }

data class Stage(
    val id: String,                   // "fd", "bf", "ps2"
    val displayName: String,
    val verdict: StageVerdict = StageVerdict.NEUTRAL
)
