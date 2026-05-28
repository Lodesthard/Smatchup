// app/src/main/java/com/example/smatchup/di/AppContainer.kt
package com.example.smatchup.di

import android.content.Context
import androidx.room.Room
import com.example.smatchup.BuildConfig
import com.example.smatchup.data.api.HttpClientProvider
import com.example.smatchup.data.api.SmashWikiApi
import com.example.smatchup.data.api.StartGgApi
import com.example.smatchup.data.api.UltimateApi
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.data.cache.CacheManager
import com.example.smatchup.data.cache.RateLimiter
import com.example.smatchup.data.cache.SystemClock
import com.example.smatchup.data.local.SmatchupDatabase
import com.example.smatchup.data.repository.BestPlayerRepository
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.data.repository.MatchupRepository
import com.example.smatchup.data.repository.TierlistRepository
import com.example.smatchup.data.winrate.WinrateAggregator
import com.example.smatchup.data.winrate.WinrateComputer
import com.example.smatchup.domain.model.ApiResult

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: SmatchupDatabase = Room.databaseBuilder(
        appContext,
        SmatchupDatabase::class.java,
        "smatchup.db"
    ).build()

    val cacheManager: CacheManager = CacheManager(SystemClock)
    val jsonAssetLoader: JsonAssetLoader = JsonAssetLoader(appContext)

    private val httpClient = HttpClientProvider.client
    private val startGgRateLimiter = RateLimiter(capacity = 80, windowMs = 60_000L, clock = SystemClock)

    val ultimateApi   = UltimateApi(httpClient)
    val startGgApi    = StartGgApi(httpClient, token = BuildConfig.START_GG_TOKEN, rateLimiter = startGgRateLimiter)
    val youtubeApi    = YouTubeApi(httpClient, apiKey = BuildConfig.YOUTUBE_API_KEY)
    val smashWikiApi  = SmashWikiApi(httpClient)

    val characterRepository: CharacterRepository = CharacterRepository(
        loader = jsonAssetLoader,
        ultimateApi = ultimateApi,
        cacheDao = database.cacheDao(),
        cacheManager = cacheManager,
    )
    val bestPlayerRepository: BestPlayerRepository = BestPlayerRepository(jsonAssetLoader)

    val matchupRepository: MatchupRepository = MatchupRepository(jsonAssetLoader)

    val tierlistRepository: TierlistRepository = TierlistRepository(jsonAssetLoader, characterRepository)

    val winrateComputer: WinrateComputer = WinrateComputer(
        cacheDao = database.cacheDao(),
        cacheManager = cacheManager,
        fetchSets = { _, _ ->
            // start.gg matchup-set aggregation not yet wired (no token in this build).
            // StartGgApi gates on a blank token and returns Unauthorized. When a token is
            // present, replace this with a real query+parse returning the set results.
            when (val r = startGgApi.query("query { __typename }")) {
                ApiResult.Unauthorized -> ApiResult.Unauthorized
                is ApiResult.Success -> ApiResult.Success(emptyList<WinrateAggregator.SetResult>())
                is ApiResult.NetworkError -> r
                is ApiResult.RateLimited -> r
                is ApiResult.ParseError -> r
                ApiResult.NotFound -> ApiResult.NotFound
            }
        },
    )
}
