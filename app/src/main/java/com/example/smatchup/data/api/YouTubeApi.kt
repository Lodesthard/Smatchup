// app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt
package com.example.smatchup.data.api

import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class YouTubeApi(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE,
) {

    companion object {
        const val DEFAULT_BASE = "https://www.googleapis.com/youtube/v3"
        private const val HANDLE = "vgbootcamp"
        private const val FALLBACK_CHANNEL_ID = "UCj1J3QuIftjOq9iv_rr7Egw"
    }

    data class Video(val url: String, val title: String, val videoId: String)

    @Volatile private var cachedChannelId: String? = null

    suspend fun searchLatest(terms: List<String>): ApiResult<Video?> {
        if (apiKey.isBlank()) return ApiResult.Unauthorized
        return withContext(Dispatchers.IO) {
            try {
                val channelId = resolveChannelId()
                val q = terms.joinToString(" ")
                val searchUrl = "$baseUrl/search".toHttpUrl().newBuilder()
                    .addQueryParameter("part", "snippet")
                    .addQueryParameter("channelId", channelId)
                    .addQueryParameter("type", "video")
                    .addQueryParameter("order", "date")
                    .addQueryParameter("maxResults", "10")
                    .addQueryParameter("q", q)
                    .addQueryParameter("key", apiKey)
                    .build().toString()
                val searchBody = JSONObject(execute(searchUrl))
                val items = searchBody.optJSONArray("items") ?: JSONArray()
                if (items.length() == 0) return@withContext ApiResult.Success(null)

                val candidates = (0 until items.length()).map { i ->
                    val item = items.getJSONObject(i)
                    val videoId = item.getJSONObject("id").getString("videoId")
                    val title = item.getJSONObject("snippet").optString("title", "")
                    videoId to title
                }

                val ids = candidates.joinToString(",") { it.first }
                val detailsUrl = "$baseUrl/videos".toHttpUrl().newBuilder()
                    .addQueryParameter("part", "contentDetails,liveStreamingDetails")
                    .addQueryParameter("id", ids)
                    .addQueryParameter("key", apiKey)
                    .build().toString()
                val detailItems = JSONObject(execute(detailsUrl)).optJSONArray("items") ?: JSONArray()
                val excluded = (0 until detailItems.length())
                    .map { detailItems.getJSONObject(it) }
                    .filter { it.has("liveStreamingDetails") || isShort(it.optJSONObject("contentDetails")?.optString("duration") ?: "") }
                    .map { it.getString("id") }
                    .toSet()

                val match = candidates
                    .filter { it.first !in excluded }
                    .firstOrNull { (_, title) ->
                        val norm = normalize(title)
                        terms.all { norm.contains(normalize(it)) }
                    }

                val video = match?.let { (videoId, title) ->
                    Video(url = "https://www.youtube.com/watch?v=$videoId", title = title, videoId = videoId)
                }
                ApiResult.Success(video)
            } catch (e: IOException) {
                ApiResult.NetworkError(e)
            }
        }
    }

    private fun resolveChannelId(): String {
        cachedChannelId?.let { return it }
        val resolved = runCatching {
            val url = "$baseUrl/channels".toHttpUrl().newBuilder()
                .addQueryParameter("part", "id")
                .addQueryParameter("forHandle", HANDLE)
                .addQueryParameter("key", apiKey)
                .build().toString()
            val body = execute(url)
            val items = JSONObject(body).optJSONArray("items")
            items?.takeIf { it.length() > 0 }?.getJSONObject(0)?.optString("id")?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        val final = resolved ?: FALLBACK_CHANNEL_ID
        cachedChannelId = final
        return final
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('0', 'o')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun isShort(duration: String): Boolean {
        if (duration.isBlank()) return false
        val h = Regex("(\\d+)H").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
        val m = Regex("(\\d+)M").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
        val s = Regex("(\\d+)S").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
        return h * 3600 + m * 60 + s <= 60
    }

    private fun execute(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${body.take(200)}")
            return body
        }
    }
}
