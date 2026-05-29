package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = CharacterRepository(loader)

    @Test fun loadRosterReturnsNonEmptyOrderedList() = runBlocking {
        val roster = repo.loadRoster()
        assertTrue("Expected non-empty roster", roster.isNotEmpty())
        roster.zipWithNext { a, b -> assertTrue(a.rosterNumber < b.rosterNumber) }
        assertEquals("mario", roster.first().id)
    }

    @Test fun loadRosterReturnsTypedCharacters() = runBlocking {
        val roster = repo.loadRoster()
        val steve = roster.first { it.id == "steve" }
        assertEquals("Steve", steve.name)
        assertEquals("Minecraft", steve.series)
    }
}
