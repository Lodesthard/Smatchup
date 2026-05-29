package com.example.smatchup.data.winrate

import com.example.smatchup.data.cache.CacheManager
import com.example.smatchup.data.cache.CacheTtl
import com.example.smatchup.data.local.dao.CacheDao
import com.example.smatchup.data.local.entity.CachedWinrateEntity
import com.example.smatchup.domain.model.ApiResult

class WinrateComputer(
    private val cacheDao: CacheDao?,
    private val cacheManager: CacheManager?,
    private val fetchSets: suspend (charA: String, charB: String) -> ApiResult<List<WinrateAggregator.SetResult>>,
) {

    suspend fun winrate(c1: String, c2: String): ApiResult<WinrateResult> {
        val (a, b) = if (c1 <= c2) c1 to c2 else c2 to c1

        val cached = cacheDao?.getWinrate(a, b)
        if (cached != null && cacheManager != null &&
            cacheManager.isFresh(cached.computedAt, CacheTtl.WINRATE_MS)
        ) {
            return ApiResult.Success(WinrateResult(cached.winRateA, cached.sampleSize))
        }

        return when (val r = fetchSets(a, b)) {
            is ApiResult.Success -> {
                val result = WinrateAggregator.aggregate(r.data)
                    ?: return ApiResult.NotFound
                cacheDao?.upsertWinrate(
                    CachedWinrateEntity(
                        charA = a, charB = b,
                        winRateA = result.winRateA,
                        sampleSize = result.sampleSize,
                        majorsCount = 0,
                        computedAt = System.currentTimeMillis(),
                    ),
                )
                ApiResult.Success(result)
            }
            is ApiResult.NetworkError -> r
            is ApiResult.RateLimited -> r
            is ApiResult.ParseError -> r
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.NotFound -> ApiResult.NotFound
        }
    }
}
