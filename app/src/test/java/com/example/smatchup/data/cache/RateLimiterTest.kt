// app/src/test/java/com/example/smatchup/data/cache/RateLimiterTest.kt
package com.example.smatchup.data.cache

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    @Test
    fun `allows up to capacity within window`() = runTest {
        val clock = Clock { currentTime }
        val limiter = RateLimiter(capacity = 3, windowMs = 1_000L, clock = clock)
        repeat(3) { limiter.acquire() }
        // 3 acquires within window: instant; no virtual time advance from acquire itself
        assertEquals(0L, currentTime)
    }

    @Test
    fun `suspends until window slides when bucket empty`() = runTest {
        val clock = Clock { currentTime }
        val limiter = RateLimiter(capacity = 2, windowMs = 1_000L, clock = clock)
        limiter.acquire()                  // t=0
        advanceTimeBy(200)                 // t=200
        limiter.acquire()                  // t=200
        // 3rd should wait until t=1000 (the first permit's window expires)
        advanceTimeBy(800)                 // t=1000
        limiter.acquire()
        assertEquals(1_000L, currentTime)
    }
}
