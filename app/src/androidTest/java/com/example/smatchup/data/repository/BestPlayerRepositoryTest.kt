package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BestPlayerRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = BestPlayerRepository(loader)

    @Test fun knownCharacterReturnsSeedTag() = runBlocking {
        val p = repo.bestPlayerFor("steve")
        assertEquals("Onin", p?.tag)
    }

    @Test fun unknownCharacterReturnsNull() = runBlocking {
        assertNull(repo.bestPlayerFor("nonexistent_char"))
    }
}
