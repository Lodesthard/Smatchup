// app/src/androidTest/java/com/example/smatchup/data/api/SmashWikiApiTest.kt
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmashWikiApiTest {

    private lateinit var server: MockWebServer

    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun teardown() { server.shutdown() }

    private fun fixture(name: String): String {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        return ctx.assets.open("fixtures/$name").bufferedReader().use { it.readText() }
    }

    private fun api(): SmashWikiApi =
        SmashWikiApi(client = OkHttpClient(), baseUrl = server.url("/api.php").toString())

    @Test fun extractReturnsPlainText() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("smashwiki_extract.json")))
        val r = api().extract("Steve (SSBU)")
        assertTrue(r is ApiResult.Success)
        assertEquals("Steve is a playable character in Super Smash Bros. Ultimate.", (r as ApiResult.Success).data)
    }
}
