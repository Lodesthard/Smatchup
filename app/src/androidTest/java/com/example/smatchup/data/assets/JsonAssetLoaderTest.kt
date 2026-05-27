package com.example.smatchup.data.assets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonAssetLoaderTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun characterMetaSteveLoads() {
        val obj = loader.characterMeta("steve")
        assertNotNull(obj)
        assertEquals("Steve", obj!!.getString("name"))
    }

    @Test fun unknownCharacterReturnsNull() {
        assertNull(loader.characterMeta("not-a-real-character"))
    }

    @Test fun synergiesParsed() {
        val obj = loader.synergies()
        assertEquals(listOf("sonic", "fox"), obj.getJSONArray("steve").let {
            List(it.length()) { i -> it.getString(i) }
        })
    }

    @Test fun strengthTierlistParsed() {
        val obj = loader.tierlist("strength")
        assertEquals("strength", obj!!.getString("name"))
    }

    @Test fun majorsSlugsLoad() {
        val arr = loader.majorsSlugs()
        assertEquals(5, arr.length())
    }

    @Test fun seedBestPlayersLoad() {
        val obj = loader.seedBestPlayers()
        assertEquals("Onin", obj.getString("steve"))
    }
}
