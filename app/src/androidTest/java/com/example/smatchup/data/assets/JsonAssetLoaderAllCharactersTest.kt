package com.example.smatchup.data.assets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonAssetLoaderAllCharactersTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun allCharactersLoadsAndContainsExpectedFighter() {
        val roster = loader.allCharacters()
        assertTrue("Roster must be non-empty", roster.length() > 0)
        val ids = (0 until roster.length()).map { roster.getJSONObject(it).getString("id") }
        assertTrue("Roster must contain 'steve'", "steve" in ids)
        assertTrue("Roster must contain 'mario'", "mario" in ids)
    }

    @Test fun firstFighterIsMarioByRosterNumber() {
        val roster = loader.allCharacters()
        val first = roster.getJSONObject(0)
        assertEquals("mario", first.getString("id"))
        assertEquals(1, first.getInt("rosterNumber"))
    }

    @Test fun everyEntryHasRequiredFields() {
        val roster = loader.allCharacters()
        for (i in 0 until roster.length()) {
            val e = roster.getJSONObject(i)
            assertNotNull(e.getString("id"))
            assertNotNull(e.getString("name"))
            assertNotNull(e.getString("series"))
            assertTrue("rosterNumber must be positive at index $i", e.getInt("rosterNumber") > 0)
        }
    }
}
