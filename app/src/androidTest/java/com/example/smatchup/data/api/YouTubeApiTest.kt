// app/src/androidTest/java/com/example/smatchup/data/api/YouTubeApiTest.kt
package com.example.smatchup.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YouTubeApiTest {

    private lateinit var server: MockWebServer

    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun teardown() { server.shutdown() }

    private fun fixture(name: String): String {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        return ctx.assets.open("fixtures/$name").bufferedReader().use { it.readText() }
    }

    private fun api(apiKey: String): YouTubeApi =
        YouTubeApi(client = OkHttpClient(), apiKey = apiKey, baseUrl = server.url("/").toString().trimEnd('/'))

    @Test fun missingApiKeyReturnsUnauthorized() = runBlocking {
        assertEquals(ApiResult.Unauthorized, api("").searchLatest(listOf("MkLeo")))
    }

    @Test fun successFiltersShortsAndReturnsFirstMatchingCandidate() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("youtube_channels.json")))
        server.enqueue(MockResponse().setBody(fixture("youtube_search.json")))
        server.enqueue(MockResponse().setBody(fixture("youtube_videos_details.json")))

        val r = api("key").searchLatest(listOf("MkLeo", "Steve"))
        assertTrue(r is ApiResult.Success)
        val data = (r as ApiResult.Success).data
        assertTrue(data!!.url.contains("vid_long"))
    }

    @Test fun noMatchReturnsSuccessWithNullPayload() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("youtube_channels.json")))
        server.enqueue(MockResponse().setBody("""{"items":[]}"""))
        val r = api("key").searchLatest(listOf("Nobody"))
        assertTrue(r is ApiResult.Success)
        assertNull((r as ApiResult.Success).data)
    }
}
