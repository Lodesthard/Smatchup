package com.example.smatchup.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: SmatchupDatabase

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun teardown() { db.close() }

    @Test fun insertAndFindByPseudo() = runTest {
        val dao = db.userDao()
        val id = dao.insert(UserEntity(pseudo = "lode", email = "l@x.io", passwordHash = "h", salt = "s", createdAt = 1))
        assertNotEquals(0L, id)
        val u = dao.findByPseudo("lode")
        assertEquals("l@x.io", u?.email)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun duplicatePseudoFails() = runTest {
        val dao = db.userDao()
        dao.insert(UserEntity(pseudo = "a", email = "a1@x", passwordHash = "h", salt = "s", createdAt = 0))
        dao.insert(UserEntity(pseudo = "a", email = "a2@x", passwordHash = "h", salt = "s", createdAt = 0))
    }

    @Test fun findUnknownReturnsNull() = runTest {
        assertNull(db.userDao().findByPseudo("nobody"))
    }
}
