package com.example.smatchup.data.repository

import com.example.smatchup.data.api.MoveParser
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val STAGE_NAMES: Map<String, String> = mapOf(
    "bf"     to "Battlefield",
    "fd"     to "Final Destination",
    "sv"     to "Smashville",
    "ps2"    to "Pokémon Stadium 2",
    "town"   to "Town and City",
    "kalos"  to "Kalos Pokémon League",
    "hollow" to "Hollow Bastion",
    "lylat"  to "Lylat Cruise",
)

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

    suspend fun getDetail(charId: String): CharacterDetail = withContext(Dispatchers.IO) {
        val roster = loadRoster()
        val character = roster.firstOrNull { it.id == charId }
            ?: throw IllegalArgumentException("Unknown character id: $charId")

        val meta = loader.characterMeta(charId)
        val combos: List<String> = meta?.optJSONArray("combos")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val gameplan = meta?.optString("gameplan")?.takeIf { it.isNotBlank() }
        val movesUtility: Map<String, String> = meta?.optJSONObject("moves_utility")?.let { obj ->
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } ?: emptyMap()

        val synergiesObj = loader.synergies()
        val partnerIds: List<String> = synergiesObj.optJSONArray(charId)?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val partners = partnerIds.mapNotNull { pid -> roster.firstOrNull { it.id == pid } }

        val stagesObj = loader.stages(charId)
        val ban = stagesObj?.optJSONArray("ban")?.toStages(StageVerdict.BAN) ?: emptyList()
        val cp = stagesObj?.optJSONArray("counterpick")?.toStages(StageVerdict.COUNTERPICK) ?: emptyList()

        CharacterDetail(
            character = character,
            combos = combos,
            gameplan = gameplan,
            movesUtility = movesUtility,
            framedata = emptyList(),
            framedataSource = FramedataSource.NONE,
            synergyPartners = partners,
            stagesBan = ban,
            stagesCounterpick = cp,
        )
    }

    suspend fun framedataFromBundle(charId: String): List<Move> = withContext(Dispatchers.IO) {
        val raw = loader.framedata(charId)?.toString() ?: return@withContext emptyList()
        MoveParser.parse(raw)
    }

    private fun org.json.JSONArray.toStages(verdict: StageVerdict): List<Stage> =
        (0 until length()).map { i ->
            val id = getString(i)
            Stage(id = id, displayName = STAGE_NAMES[id] ?: id, verdict = verdict)
        }
}
