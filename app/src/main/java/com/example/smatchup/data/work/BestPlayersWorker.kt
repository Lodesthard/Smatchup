package com.example.smatchup.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.smatchup.BuildConfig
import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
import com.example.smatchup.SmatchupApp
import com.example.smatchup.domain.model.ApiResult
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Periodically recomputes the "best player per character" table from start.gg majors
 * (spec §6.5). Token-gated: when START_GG_TOKEN is blank the worker is a no-op success,
 * leaving the bundled seed data in place. The start.gg computation path only runs once a
 * token is provisioned and has not been exercised against the live API yet.
 */
class BestPlayersWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (BuildConfig.START_GG_TOKEN.isBlank()) return Result.success()

        val container = SmatchupApp.instance.container
        val api = container.startGgApi
        val cacheDao = container.database.cacheDao()
        val slugs = container.jsonAssetLoader.majorsSlugs()

        // playerTag -> (charId -> usageCount) and playerTag -> accumulated placement score
        val usage = HashMap<String, HashMap<String, Int>>()
        val placementScore = HashMap<String, Float>()
        var majorsProcessed = 0

        for (i in 0 until slugs.length()) {
            val slug = slugs.optString(i).ifBlank { continue }
            val standings = fetchStandings(api, slug) ?: continue
            val entrantToTag = HashMap<String, String>()

            standings.forEach { (entrantId, tag, place) ->
                entrantToTag[entrantId] = tag
                placementScore[tag] = (placementScore[tag] ?: 0f) + placementWeight(place)
            }
            fetchSelections(api, slug).forEach { (entrantId, charId) ->
                val tag = entrantToTag[entrantId] ?: return@forEach
                usage.getOrPut(tag) { HashMap() }.merge(charId, 1, Int::plus)
            }
            majorsProcessed++
        }

        if (majorsProcessed == 0) return Result.retry()

        // Per player: main char + score = placementScore * mainUsageFraction.
        // Then per char, keep the highest-scoring player.
        val bestByChar = HashMap<String, CachedBestPlayerEntity>()
        val now = System.currentTimeMillis()
        usage.forEach { (tag, charCounts) ->
            val total = charCounts.values.sum().takeIf { it > 0 } ?: return@forEach
            val (mainChar, mainCount) = charCounts.maxByOrNull { it.value } ?: return@forEach
            val score = (placementScore[tag] ?: 0f) * (mainCount.toFloat() / total)
            val current = bestByChar[mainChar]
            if (current == null || score > current.score) {
                bestByChar[mainChar] = CachedBestPlayerEntity(
                    charId = mainChar,
                    playerTag = tag,
                    startGgPlayerId = null,
                    score = score,
                    computedAt = now,
                )
            }
        }

        bestByChar.values.forEach { cacheDao.upsertBestPlayer(it) }
        return Result.success()
    }

    private suspend fun fetchStandings(
        api: com.example.smatchup.data.api.StartGgApi,
        slug: String,
    ): List<Triple<String, String, Int>>? {
        val query = """
            query Standings(${'$'}slug: String!) {
              tournament(slug: ${'$'}slug) {
                events {
                  standings(query: { perPage: 32, page: 1 }) {
                    nodes {
                      placement
                      entrant { id participants { player { gamerTag } } }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val raw = when (val r = api.query(query, JSONObject().put("slug", slug).toString())) {
            is ApiResult.Success -> r.data
            else -> return null
        }
        val out = mutableListOf<Triple<String, String, Int>>()
        val events = JSONObject(raw).optJSONObject("data")
            ?.optJSONObject("tournament")?.optJSONArray("events") ?: return out
        for (e in 0 until events.length()) {
            val nodes = events.getJSONObject(e).optJSONObject("standings")?.optJSONArray("nodes") ?: continue
            for (n in 0 until nodes.length()) {
                val node = nodes.getJSONObject(n)
                val place = node.optInt("placement", -1).takeIf { it > 0 } ?: continue
                val entrant = node.optJSONObject("entrant") ?: continue
                val id = entrant.opt("id")?.toString() ?: continue
                val tag = entrant.optJSONArray("participants")?.optJSONObject(0)
                    ?.optJSONObject("player")?.optString("gamerTag")?.ifBlank { null } ?: continue
                out.add(Triple(id, tag, place))
            }
        }
        return out
    }

    private suspend fun fetchSelections(
        api: com.example.smatchup.data.api.StartGgApi,
        slug: String,
    ): List<Pair<String, String>> {
        val query = """
            query Sets(${'$'}slug: String!, ${'$'}page: Int!) {
              tournament(slug: ${'$'}slug) {
                events {
                  sets(page: ${'$'}page, perPage: 40, sortType: STANDARD) {
                    pageInfo { totalPages }
                    nodes { games { selections { entrant { id } character { name } } } }
                  }
                }
              }
            }
        """.trimIndent()
        val out = mutableListOf<Pair<String, String>>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= MAX_PAGES) {
            val vars = JSONObject().put("slug", slug).put("page", page).toString()
            val raw = when (val r = api.query(query, vars)) {
                is ApiResult.Success -> r.data
                else -> break
            }
            val events = JSONObject(raw).optJSONObject("data")
                ?.optJSONObject("tournament")?.optJSONArray("events") ?: break
            for (e in 0 until events.length()) {
                val sets = events.getJSONObject(e).optJSONObject("sets") ?: continue
                totalPages = sets.optJSONObject("pageInfo")?.optInt("totalPages", 1) ?: 1
                val nodes = sets.optJSONArray("nodes") ?: continue
                for (n in 0 until nodes.length()) {
                    val games = nodes.getJSONObject(n).optJSONArray("games") ?: continue
                    for (g in 0 until games.length()) {
                        val sels = games.getJSONObject(g).optJSONArray("selections") ?: continue
                        for (s in 0 until sels.length()) {
                            val sel = sels.getJSONObject(s)
                            val entrantId = sel.optJSONObject("entrant")?.opt("id")?.toString() ?: continue
                            val name = sel.optJSONObject("character")?.optString("name")?.ifBlank { null } ?: continue
                            out.add(entrantId to normalizeCharId(name))
                        }
                    }
                }
            }
            page++
        }
        return out
    }

    private fun normalizeCharId(name: String): String =
        name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    companion object {
        private const val UNIQUE_NAME = "best-players-refresh"
        private const val MAX_PAGES = 6

        private fun placementWeight(place: Int): Float = when {
            place == 1 -> 100f
            place == 2 -> 70f
            place == 3 -> 50f
            place == 4 -> 40f
            place <= 8 -> 25f
            place <= 16 -> 10f
            place <= 32 -> 5f
            else -> 0f
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BestPlayersWorker>(60, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
