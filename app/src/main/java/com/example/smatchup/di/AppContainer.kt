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
import com.example.smatchup.data.repository.CharacterRepository

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

    val characterRepository: CharacterRepository = CharacterRepository(jsonAssetLoader)
}
