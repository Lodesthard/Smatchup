// app/src/main/java/com/example/smatchup/data/cache/Clock.kt
package com.example.smatchup.data.cache

fun interface Clock {
    fun nowMillis(): Long
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
