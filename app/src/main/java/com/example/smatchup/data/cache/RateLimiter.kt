// app/src/main/java/com/example/smatchup/data/cache/RateLimiter.kt
package com.example.smatchup.data.cache

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/**
 * Sliding-window token bucket. Each acquire() consumes one permit; permits expire after windowMs.
 * Suspends until a permit is available. Thread-safe via Mutex.
 */
class RateLimiter(
    private val capacity: Int,
    private val windowMs: Long,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>(capacity)

    suspend fun acquire() {
        while (true) {
            val waitMs = mutex.withLock {
                val now = clock.nowMillis()
                // drop expired timestamps
                while (timestamps.isNotEmpty() && now - timestamps.peekFirst()!! >= windowMs) {
                    timestamps.pollFirst()
                }
                if (timestamps.size < capacity) {
                    timestamps.addLast(now)
                    0L
                } else {
                    val oldest = timestamps.peekFirst()!!
                    windowMs - (now - oldest)
                }
            }
            if (waitMs <= 0L) return
            delay(waitMs)
        }
    }
}
