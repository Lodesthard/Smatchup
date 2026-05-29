package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.data.local.dao.CacheDao
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BestPlayerRepository(
    private val loader: JsonAssetLoader,
    private val cacheDao: CacheDao,
) {

    suspend fun bestPlayerFor(charId: String): Player? = withContext(Dispatchers.IO) {
        // Cache-first: BestPlayersWorker output, computed from start.gg majors.
        cacheDao.getBestPlayer(charId)?.let {
            return@withContext Player(
                tag = it.playerTag,
                startGgId = it.startGgPlayerId,
                mainCharacter = charId,
                score = it.score,
            )
        }
        // Fallback: bundled seed before the first worker run / without a start.gg token.
        val seed = loader.seedBestPlayers()
        val tag = seed.optString(charId, "").takeIf { it.isNotEmpty() } ?: return@withContext null
        Player(tag = tag, mainCharacter = charId)
    }
}
