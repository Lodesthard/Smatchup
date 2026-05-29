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
class MatchupRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = MatchupRepository(loader)

    @Test fun loadsCuratedMatchupNormalized() = runBlocking {
        val mu = repo.getMatchup("steve", "sonic")
        assertEquals("sonic", mu.charA)
        assertEquals("steve", mu.charB)
        assertTrue(mu.gameplanA.isNotEmpty())
        assertTrue(mu.gameplanB.isNotEmpty())
        assertTrue(mu.strongMovesA.isNotEmpty())
        assertTrue(mu.punishableMovesB.isNotEmpty())
    }

    @Test fun stagesParsedWithVerdicts() = runBlocking {
        val mu = repo.getMatchup("sonic", "steve")
        assertTrue(mu.stagesForA.any { it.verdict == StageVerdict.BAN })
        assertTrue(mu.stagesForB.any { it.verdict == StageVerdict.COUNTERPICK })
    }

    @Test fun unknownPairReturnsEmptyMatchup() = runBlocking {
        val mu = repo.getMatchup("mario", "luigi")
        assertEquals("luigi", mu.charA)
        assertEquals("mario", mu.charB)
        assertTrue(mu.gameplanA.isEmpty())
        assertTrue(mu.gameplanB.isEmpty())
    }
}
