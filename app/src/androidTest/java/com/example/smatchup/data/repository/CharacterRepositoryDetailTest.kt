package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterRepositoryDetailTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = CharacterRepository(loader)

    @Test fun detailForSeedCharacterHasComboAndGameplan() = runBlocking {
        val d = repo.getDetail("steve")
        assertEquals("Steve", d.character.name)
        assertTrue("Combos must be non-empty", d.combos.isNotEmpty())
        assertTrue("Gameplan must be present", d.gameplan != null && d.gameplan!!.isNotBlank())
    }

    @Test fun detailIncludesSynergyPartners() = runBlocking {
        val d = repo.getDetail("steve")
        val ids = d.synergyPartners.map { it.id }
        assertTrue("Steve should partner with sonic", "sonic" in ids)
        assertTrue("Steve should partner with fox", "fox" in ids)
    }

    @Test fun detailIncludesStagesBanAndCounterpick() = runBlocking {
        val d = repo.getDetail("steve")
        assertTrue(d.stagesBan.any { it.id == "sv" })
        assertTrue(d.stagesCounterpick.any { it.id == "fd" })
        assertEquals(StageVerdict.BAN, d.stagesBan.first().verdict)
        assertEquals(StageVerdict.COUNTERPICK, d.stagesCounterpick.first().verdict)
    }

    @Test fun bundledFramedataLoadsForSeedCharacters() = runBlocking {
        val frames = repo.framedataFromBundle("steve")
        assertTrue("Steve should have bundled framedata", frames.isNotEmpty())
        assertTrue(frames.any { it.id == "fair" })
    }

    @Test fun bundledFramedataReturnsEmptyForUnseededCharacter() = runBlocking {
        val frames = repo.framedataFromBundle("mario")
        assertTrue("Mario has no bundled framedata seed yet", frames.isEmpty())
    }

    @Test fun detailForUnknownCharacterThrows() {
        try {
            runBlocking { repo.getDetail("not_a_char") }
            assertTrue("Expected IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }
}
