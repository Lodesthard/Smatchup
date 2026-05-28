package com.example.smatchup.data.winrate

import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WinrateComputerTest {

    @Test fun unauthorizedFetchPropagates() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ -> ApiResult.Unauthorized },
        )
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.Unauthorized)
    }

    @Test fun aggregatesSuccessfulFetch() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ ->
                ApiResult.Success(
                    listOf(
                        WinrateAggregator.SetResult(true),
                        WinrateAggregator.SetResult(false),
                        WinrateAggregator.SetResult(true),
                    ),
                )
            },
        )
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.Success)
        val result = (r as ApiResult.Success).data
        assertEquals(3, result.sampleSize)
        assertEquals(2f / 3f, result.winRateA, 0.0001f)
    }

    @Test fun emptySetsReturnsNotFound() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ -> ApiResult.Success(emptyList()) },
        )
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.NotFound)
    }
}
