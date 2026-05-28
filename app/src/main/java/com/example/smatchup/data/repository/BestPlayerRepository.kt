package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BestPlayerRepository(private val loader: JsonAssetLoader) {

    suspend fun bestPlayerFor(charId: String): Player? = withContext(Dispatchers.IO) {
        val seed = loader.seedBestPlayers()
        val tag = seed.optString(charId, "").takeIf { it.isNotEmpty() } ?: return@withContext null
        Player(tag = tag, mainCharacter = charId)
    }
}
