package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Matchup
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val STAGE_NAMES: Map<String, String> = mapOf(
    "bf" to "Battlefield", "fd" to "Final Destination", "sv" to "Smashville",
    "ps2" to "Pokémon Stadium 2", "town" to "Town and City",
    "kalos" to "Kalos Pokémon League", "hollow" to "Hollow Bastion", "lylat" to "Lylat Cruise",
)

class MatchupRepository(private val loader: JsonAssetLoader) {

    suspend fun getMatchup(c1: String, c2: String): Matchup = withContext(Dispatchers.IO) {
        val (a, b) = if (c1 <= c2) c1 to c2 else c2 to c1
        val json = loader.matchup(a, b)
            ?: return@withContext Matchup(charA = a, charB = b)
        Matchup(
            charA = a,
            charB = b,
            gameplanA = json.stringList("gameplanA"),
            gameplanB = json.stringList("gameplanB"),
            strongMovesA = json.stringList("strongMovesA"),
            strongMovesB = json.stringList("strongMovesB"),
            punishableMovesA = json.stringList("punishableMovesA"),
            punishableMovesB = json.stringList("punishableMovesB"),
            stagesForA = json.optJSONObject("stagesForA").toStages(),
            stagesForB = json.optJSONObject("stagesForB").toStages(),
        )
    }

    private fun JSONObject.stringList(key: String): List<String> =
        optJSONArray(key)?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()

    private fun JSONObject?.toStages(): List<Stage> {
        if (this == null) return emptyList()
        return stagesOf("ban", StageVerdict.BAN) + stagesOf("counterpick", StageVerdict.COUNTERPICK)
    }

    private fun JSONObject.stagesOf(key: String, verdict: StageVerdict): List<Stage> =
        optJSONArray(key)?.let { arr ->
            (0 until arr.length()).map { i ->
                val id = arr.getString(i)
                Stage(id = id, displayName = STAGE_NAMES[id] ?: id, verdict = verdict)
            }
        } ?: emptyList()
}
