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

    @Test fun multilineQueryProducesValidJsonBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":{"ok":true}}"""))
        val multiline = "query Foo {\n  tournament {\n    name\n  }\n}"
        val r = api("tok").query(multiline)
        assertTrue(r is ApiResult.Success)

        val recorded = server.takeRequest()
        val sent = recorded.body.readUtf8()
        // The newlines in `multiline` must be escaped to `\n` in the JSON body so that the
        // JSON parser on the server side does not see a raw line break inside a string literal.
        assertTrue("body should contain escaped \\n", sent.contains("query Foo {\\n  tournament {\\n    name\\n  }\\n}"))
        // And the body itself must be parseable JSON (no bare newlines in the string).
        assertTrue("body should not contain raw newline inside string", !sent.contains("\n  tournament"))
    }
}
