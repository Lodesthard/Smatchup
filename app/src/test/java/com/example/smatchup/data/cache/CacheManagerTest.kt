// app/src/test/java/com/example/smatchup/data/cache/CacheManagerTest.kt
package com.example.smatchup.data.cache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheManagerTest {

    @Test
    fun `isFresh true when within TTL`() {
        val clock = Clock { 1_000_000L }
        val mgr = CacheManager(clock)
        assertTrue(mgr.isFresh(fetchedAt = 999_500L, ttlMs = 1_000L))
    }

    @Test
    fun `isFresh false when past TTL`() {
        val clock = Clock { 1_000_000L }
        val mgr = CacheManager(clock)
        assertFalse(mgr.isFresh(fetchedAt = 998_999L, ttlMs = 1_000L))
    }

    @Test
    fun `isFresh false when fetchedAt is in the future (clock skew)`() {
        val clock = Clock { 1_000_000L }
        val mgr = CacheManager(clock)
        assertFalse(mgr.isFresh(fetchedAt = 2_000_000L, ttlMs = 1_000L))
    }
}
