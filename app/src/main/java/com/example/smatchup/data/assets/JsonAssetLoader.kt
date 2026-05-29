package com.example.smatchup.data.assets

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class JsonAssetLoader(private val context: Context) {

    fun characterMeta(charId: String): JSONObject? =
        loadObject("characters_meta/$charId.json")

    fun framedata(charId: String): JSONObject? =
        loadObject("framedata/$charId.json")

    fun synergies(): JSONObject = loadObject("synergies.json")!!

    fun stages(charId: String): JSONObject? =
        loadObject("stages/$charId.json")

    fun matchup(charA: String, charB: String): JSONObject? {
        val (a, b) = if (charA <= charB) charA to charB else charB to charA
        return loadObject("matchups/${a}_${b}.json")
    }

    fun tierlist(name: String): JSONObject? = loadObject("tierlist_$name.json")

    fun majorsSlugs(): JSONArray = loadArray("majors_slugs.json")!!

    fun seedBestPlayers(): JSONObject = loadObject("seed_best_players.json")!!

    /** Full SSBU roster, ordered by canonical roster number. */
    fun allCharacters(): JSONArray = loadArray("characters.json")!!

    private fun loadObject(path: String): JSONObject? = readString(path)?.let { JSONObject(it) }
    private fun loadArray(path: String): JSONArray? = readString(path)?.let { JSONArray(it) }

    private fun readString(path: String): String? =
        try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        }
}
