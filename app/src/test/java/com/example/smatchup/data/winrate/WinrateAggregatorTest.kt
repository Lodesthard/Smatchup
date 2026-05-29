package com.example.smatchup.data.winrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WinrateAggregatorTest {

    @Test fun emptySetsReturnsNull() {
        assertNull(WinrateAggregator.aggregate(emptyList()))
    }

    @Test fun computesRatioFromAPerspective() {
        val sets = listOf(
            WinrateAggregator.SetResult(winnerIsA = true),
            WinrateAggregator.SetResult(winnerIsA = true),
            WinrateAggregator.SetResult(winnerIsA = false),
            WinrateAggregator.SetResult(winnerIsA = true),
        )
        val r = WinrateAggregator.aggregate(sets)!!
        assertEquals(0.75f, r.winRateA, 0.0001f)
        assertEquals(4, r.sampleSize)
    }

    @Test fun allLossesIsZero() {
        val sets = listOf(WinrateAggregator.SetResult(false), WinrateAggregator.SetResult(false))
        val r = WinrateAggregator.aggregate(sets)!!
        assertEquals(0.0f, r.winRateA, 0.0001f)
        assertEquals(2, r.sampleSize)
    }
}
