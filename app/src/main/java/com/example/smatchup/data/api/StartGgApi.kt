// app/src/main/java/com/example/smatchup/data/api/StartGgApi.kt
package com.example.smatchup.data.api

import com.example.smatchup.data.cache.RateLimiter
import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class StartGgApi(
    private val client: OkHttpClient,
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val token: String,
    private val rateLimiter: RateLimiter,
) {
    companion object {
        const val DEFAULT_ENDPOINT = "https://api.start.gg/gql/alpha"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun query(graphql: String, variables: String = "{}"): ApiResult<String> {
        if (token.isBlank()) return ApiResult.Unauthorized
        rateLimiter.acquire()

        // Build request JSON without org.json (Android-only stub, unavailable in JVM unit tests)
        val escapedQuery = graphql.replace("\\", "\\\\").replace("\"", "\\\"")
        val bodyJson = """{"query":"$escapedQuery","variables":$variables}"""
        val body = bodyJson.toRequestBody(JSON)

        val req = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    when {
                        !resp.isSuccessful -> ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                        text.contains("Rate limit exceeded") -> ApiResult.RateLimited(null)
                        else -> ApiResult.Success(text)
                    }
                }
            } catch (e: IOException) {
                ApiResult.NetworkError(e)
            }
        }
    }
}
