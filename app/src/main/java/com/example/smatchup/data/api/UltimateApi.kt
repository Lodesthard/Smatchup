// app/src/main/java/com/example/smatchup/data/api/UltimateApi.kt
package com.example.smatchup.data.api

import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class UltimateApi(
    private val client: OkHttpClient,
    private val baseUrl: String = DEFAULT_BASE,
) {

    companion object {
        const val DEFAULT_BASE = "https://the-ultimate-api.dreseansutton.com"
    }

    /**
     * Returns the raw JSON body for a fighter's moves. Parsing into [Move] happens in the repo
     * (lazy) since the API schema may evolve. NetworkError for 5xx / IOException, NotFound for 404.
     */
    suspend fun getMovesRaw(charId: String): ApiResult<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$baseUrl/api/characters/name/$charId").build()
        try {
            client.newCall(req).execute().use { resp ->
                when {
                    resp.isSuccessful -> ApiResult.Success(resp.body?.string().orEmpty())
                    resp.code == 404 -> ApiResult.NotFound
                    else -> ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                }
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        }
    }
}
