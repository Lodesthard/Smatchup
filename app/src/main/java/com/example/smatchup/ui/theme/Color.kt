package com.example.smatchup.ui.theme

import androidx.compose.ui.graphics.Color

object SmatchupColors {
    val Bg1       = Color(0xFF0A0118)
    val Bg2       = Color(0xFF1E0838)
    val Purple    = Color(0xFFB06AFF)
    val Gold      = Color(0xFFFFD96A)
    val Text      = Color(0xFFF5ECFF)
    val TextDim   = Color(0x99F5ECFF)
    val DangerRed = Color(0xFFFF5A6A)

    val Tier: Map<String, Color> = mapOf(
        "S_PLUS" to Color(0xFFFF5A6A),
        "S"      to Color(0xFFFFAA3A),
        "A"      to Gold,
        "B"      to Color(0xFFA6E36A),
        "C"      to Color(0xFF6AD8FF),
        "D"      to Purple,
        "E"      to Color(0xFF7B7B9A),
        "F"      to Color(0xFF555573),
    )
}
