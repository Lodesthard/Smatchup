package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.Tier
import com.example.smatchup.domain.model.TierGroup
import com.example.smatchup.domain.model.TierlistView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TierlistRepository(
    private val loader: JsonAssetLoader,
    private val characterRepository: CharacterRepository,
) {

    suspend fun load(name: String): TierlistView = withContext(Dispatchers.IO) {
        val json = loader.tierlist(name)
            ?: return@withContext TierlistView(name = name, version = "", groups = emptyList())

        val rosterById: Map<String, Character> = characterRepository.loadRoster().associateBy { it.id }
        val entries = json.optJSONArray("entries")
        val groups = if (entries == null) emptyList() else (0 until entries.length()).mapNotNull { i ->
            val o = entries.getJSONObject(i)
            val tier = runCatching { Tier.valueOf(o.getString("tier")) }.getOrNull() ?: return@mapNotNull null
            val charsArr = o.optJSONArray("chars")
            val chars = if (charsArr == null) emptyList() else (0 until charsArr.length())
                .mapNotNull { j -> rosterById[charsArr.getString(j)] }
            TierGroup(tier = tier, characters = chars)
        }.sortedBy { it.tier.ordinal }

        TierlistView(
            name = json.optString("name", name),
            version = json.optString("version", ""),
            groups = groups,
        )
    }
}
