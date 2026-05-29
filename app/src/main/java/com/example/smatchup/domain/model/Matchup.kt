package com.example.smatchup.domain.model

/**
 * Normalize so that charA.id < charB.id lexicographically.
 * Win rate is from charA's perspective.
 */
data class Matchup(
    val charA: String,
    val charB: String,
    val gameplanA: List<String> = emptyList(),
    val gameplanB: List<String> = emptyList(),
    val strongMovesA: List<String> = emptyList(),
    val strongMovesB: List<String> = emptyList(),
    val punishableMovesA: List<String> = emptyList(),
    val punishableMovesB: List<String> = emptyList(),
    val stagesForA: List<Stage> = emptyList(),
    val stagesForB: List<Stage> = emptyList(),
    val winRateA: Float? = null,
    val sampleSize: Int = 0,
    val majorsCount: Int = 0
) {
    init {
        require(charA < charB) { "Matchup must be stored with charA < charB lexicographically (got $charA / $charB)" }
    }
}
