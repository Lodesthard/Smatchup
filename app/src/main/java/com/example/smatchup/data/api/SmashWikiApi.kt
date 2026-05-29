// app/src/main/java/com/example/smatchup/data/api/SmashWikiApi.kt
package com.example.smatchup.data.api

import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class SmashWikiApi(
    private val client: OkHttpClient,
    private val baseUrl: String = DEFAULT_BASE,
) {
    companion object { const val DEFAULT_BASE = "https://www.ssbwiki.com/api.php" }

    suspend fun extract(pageTitle: String): ApiResult<String?> = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("prop", "extracts")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("titles", pageTitle)
            .build().toString()
        val req = Request.Builder().url(url).build()
        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                val json = JSONObject(resp.body?.string().orEmpty())
                val pages = json.optJSONObject("query")?.optJSONObject("pages")
                val firstKey = pages?.keys()?.asSequence()?.firstOrNull()
                val extract = pages?.optJSONObject(firstKey ?: "")?.optString("extract")
                ApiResult.Success(extract?.takeIf { it.isNotEmpty() })
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        }
    }
}
