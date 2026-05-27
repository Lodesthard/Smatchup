// app/src/test/java/com/example/smatchup/data/api/UltimateApiTest.kt
package com.example.smatchup.data.api

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

class UltimateApiTest {

    private lateinit var server: MockWebServer

    @Before fun setup() {
        server = MockWebServer()
        server.start()
    }
    @After fun teardown() { server.shutdown() }

    private fun fixture(name: String): String =
        this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

    private fun apiClient(): UltimateApi =
        UltimateApi(client = OkHttpClient(), baseUrl = server.url("/").toString().trimEnd('/'))

    @Test fun fetchMovesSuccess() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("ultimate_api_steve_moves.json")))
        val result = apiClient().getMovesRaw("steve")
        assertTrue(result is ApiResult.Success)
        val body = (result as ApiResult.Success).data
        assertTrue(body.contains("\"fighter\":\"Steve\""))
    }

    @Test fun http404MapsToNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = apiClient().getMovesRaw("ghost")
        assertEquals(ApiResult.NotFound, result)
    }

    @Test fun http500MapsToNetworkError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val r = apiClient().getMovesRaw("steve")
        assertTrue(r is ApiResult.NetworkError)
    }
}
