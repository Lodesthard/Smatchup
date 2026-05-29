package com.example.smatchup.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.example.smatchup.data.local.entity.SessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: SmatchupDatabase

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun teardown() { db.close() }

    @Test fun initiallyAbsent() = runTest {
        assertNull(db.sessionDao().get())
    }

    @Test fun upsertReplacesSingletonRow() = runTest {
        val dao = db.sessionDao()
        dao.upsert(SessionEntity(userId = 5, updatedAt = 1L))
        assertEquals(5L, dao.get()?.userId)
        dao.upsert(SessionEntity(userId = null, updatedAt = 2L))
        assertNull(dao.get()?.userId)
    }

    @Test fun observeEmitsUpdates() = runTest {
        val dao = db.sessionDao()
        dao.observe().test {
            assertNull(awaitItem())
            dao.upsert(SessionEntity(userId = 7, updatedAt = 1L))
            assertEquals(7L, awaitItem()?.userId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
