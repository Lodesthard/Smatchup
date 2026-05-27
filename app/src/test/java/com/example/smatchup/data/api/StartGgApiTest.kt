// app/src/test/java/com/example/smatchup/data/api/StartGgApiTest.kt
package com.example.smatchup.data.api

import com.example.smatchup.data.cache.Clock
import com.example.smatchup.data.cache.RateLimiter
import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StartGgApiTest {

    private lateinit var server: MockWebServer

    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun teardown() { server.shutdown() }

    private fun fixture(name: String): String =
        this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

    private fun api(token: String): StartGgApi {
        val limiter = RateLimiter(capacity = 80, windowMs = 60_000L, clock = Clock { 0L })
        return StartGgApi(
            client = OkHttpClient(),
            endpoint = server.url("/gql").toString(),
            token = token,
            rateLimiter = limiter
        )
    }

    @Test fun blankTokenReturnsUnauthorized() = runTest {
        val r = api("").query("query { __typename }")
        assertEquals(ApiResult.Unauthorized, r)
    }

    @Test fun successReturnsRawJson() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("startgg_tournament_sets.json")))
        val r = api("tok").query("query { __typename }")
        assertTrue(r is ApiResult.Success)
        assertTrue((r as ApiResult.Success).data.contains("\"tournament\""))
    }

    @Test fun rateLimitMessageMapsToRateLimited() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("startgg_error_rate_limit.json")))
        val r = api("tok").query("q")
        assertTrue(r is ApiResult.RateLimited)
    }

    @Test fun http500MapsToNetworkError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val r = api("tok").query("q")
        assertTrue(r is ApiResult.NetworkError)
    }
}
