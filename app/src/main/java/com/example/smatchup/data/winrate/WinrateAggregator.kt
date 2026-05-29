package com.example.smatchup.data.winrate

data class WinrateResult(val winRateA: Float, val sampleSize: Int)

object WinrateAggregator {

    /** One tournament set between charA's player and charB's player. */
    data class SetResult(val winnerIsA: Boolean)

    fun aggregate(sets: List<SetResult>): WinrateResult? {
        if (sets.isEmpty()) return null
        val aWins = sets.count { it.winnerIsA }
        return WinrateResult(winRateA = aWins.toFloat() / sets.size, sampleSize = sets.size)
    }
}
