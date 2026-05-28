package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CharacterRepository(private val loader: JsonAssetLoader) {

    suspend fun loadRoster(): List<Character> = withContext(Dispatchers.IO) {
        val arr = loader.allCharacters()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Character(
                id = o.getString("id"),
                name = o.getString("name"),
                series = o.getString("series"),
                rosterNumber = o.getInt("rosterNumber"),
                portraitAsset = o.optString("portraitAsset").takeIf { it.isNotEmpty() },
            )
        }.sortedBy { it.rosterNumber }
    }
}
