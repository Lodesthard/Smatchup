package com.example.smatchup.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import com.example.smatchup.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesDaoTest {

    private lateinit var db: SmatchupDatabase
    private var userId: Long = 0

    @Before fun setup() = runTest {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
            .allowMainThreadQueries().build()
        userId = db.userDao().insert(UserEntity(pseudo = "u", email = "u@x", passwordHash = "h", salt = "s", createdAt = 0))
    }
    @After fun teardown() { db.close() }

    @Test fun addAndObserveCharacters() = runTest {
        val dao = db.favoritesDao()
        dao.observeCharacters(userId).test {
            assertEquals(emptyList<FavoriteCharacterEntity>(), awaitItem())
            dao.addCharacter(FavoriteCharacterEntity(userId, "steve", 1L))
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("steve", list[0].charId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun removeCharacterTogglesFlag() = runTest {
        val dao = db.favoritesDao()
        dao.addCharacter(FavoriteCharacterEntity(userId, "steve", 1L))
        assertTrue(dao.isCharacterFavorite(userId, "steve"))
        dao.removeCharacter(userId, "steve")
        assertFalse(dao.isCharacterFavorite(userId, "steve"))
    }

    @Test fun matchupCrud() = runTest {
        val dao = db.favoritesDao()
        dao.addMatchup(FavoriteMatchupEntity(userId, "sonic", "steve", 1L))
        assertTrue(dao.isMatchupFavorite(userId, "sonic", "steve"))
        dao.removeMatchup(userId, "sonic", "steve")
        assertFalse(dao.isMatchupFavorite(userId, "sonic", "steve"))
    }
}
