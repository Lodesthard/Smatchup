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

    /**
     * Send a GraphQL request. Returns the raw response body wrapped in [ApiResult].
     *
     * @param graphql the GraphQL query/mutation source.
     * @param variables a pre-serialized JSON object string for the GraphQL variables
     *                  (e.g. `JSONObject(mapOf("id" to 42)).toString()`). Defaults to `"{}"`.
     *                  Using a String rather than `org.json.JSONObject` keeps this class testable
     *                  in JVM unit tests, where `JSONObject` is stubbed.
     */
    suspend fun query(graphql: String, variables: String = "{}"): ApiResult<String> {
        if (token.isBlank()) return ApiResult.Unauthorized
        rateLimiter.acquire()

        val escapedQuery = jsonEscape(graphql)
        val body = """{"query":"$escapedQuery","variables":$variables}"""
            .toRequestBody(JSON)

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

    /** Minimal JSON string-literal escaping per RFC 8259 section 7. */
    private fun jsonEscape(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"'  -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '' -> sb.append("\\f")
                else -> {
                    if (c.code < 0x20) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }
}
