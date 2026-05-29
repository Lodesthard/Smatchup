// app/src/main/java/com/example/smatchup/data/cache/CacheManager.kt
package com.example.smatchup.data.cache

class CacheManager(private val clock: Clock = SystemClock) {

    fun isFresh(fetchedAt: Long, ttlMs: Long): Boolean {
        val now = clock.nowMillis()
        if (fetchedAt > now) return false
        return now - fetchedAt < ttlMs
    }

    fun shouldRefresh(fetchedAt: Long, ttlMs: Long): Boolean = !isFresh(fetchedAt, ttlMs)
}
