package com.example.smatchup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
import com.example.smatchup.data.local.entity.CachedFramedataEntity
import com.example.smatchup.data.local.entity.CachedWinrateEntity
import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity

@Dao
interface CacheDao {

    // --- framedata ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFramedata(entity: CachedFramedataEntity)

    @Query("SELECT * FROM cached_framedata WHERE charId = :charId LIMIT 1")
    suspend fun getFramedata(charId: String): CachedFramedataEntity?

    // --- winrate ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWinrate(entity: CachedWinrateEntity)

    @Query("SELECT * FROM cached_winrate WHERE charA = :a AND charB = :b LIMIT 1")
    suspend fun getWinrate(a: String, b: String): CachedWinrateEntity?

    // --- best players ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBestPlayer(entity: CachedBestPlayerEntity)

    @Query("SELECT * FROM cached_best_players")
    suspend fun getAllBestPlayers(): List<CachedBestPlayerEntity>

    @Query("SELECT * FROM cached_best_players WHERE charId = :charId LIMIT 1")
    suspend fun getBestPlayer(charId: String): CachedBestPlayerEntity?

    // --- youtube ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideo(entity: CachedYoutubeVideoEntity)

    @Query("SELECT * FROM cached_youtube_video WHERE cacheKey = :key LIMIT 1")
    suspend fun getVideo(key: String): CachedYoutubeVideoEntity?
}
