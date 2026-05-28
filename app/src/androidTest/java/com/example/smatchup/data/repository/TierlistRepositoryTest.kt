package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Tier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierlistRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val charRepo = CharacterRepository(loader)
    private val repo = TierlistRepository(loader, charRepo)

    @Test fun strengthListLoadsWithGroupsOrdered() = runBlocking {
        val view = repo.load("strength")
        assertEquals("strength", view.name)
        assertTrue(view.groups.isNotEmpty())
        assertEquals(Tier.S_PLUS, view.groups.first().tier)
    }

    @Test fun charactersResolvedToDomainObjects() = runBlocking {
        val view = repo.load("strength")
        val sPlus = view.groups.first { it.tier == Tier.S_PLUS }
        val names = sPlus.characters.map { it.name }
        assertTrue("Steve should be S+", "Steve" in names)
    }

    @Test fun difficultyListLoads() = runBlocking {
        val view = repo.load("difficulty")
        assertEquals("difficulty", view.name)
        assertTrue(view.groups.isNotEmpty())
    }

    @Test fun unknownListReturnsEmptyGroups() = runBlocking {
        val view = repo.load("nonexistent")
        assertTrue(view.groups.isEmpty())
    }
}
