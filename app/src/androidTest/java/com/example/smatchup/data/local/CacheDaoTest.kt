package com.example.smatchup.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
import com.example.smatchup.data.local.entity.CachedFramedataEntity
import com.example.smatchup.data.local.entity.CachedWinrateEntity
import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheDaoTest {

    private lateinit var db: SmatchupDatabase

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun teardown() { db.close() }

    @Test fun framedataUpsert() = runTest {
        val dao = db.cacheDao()
        dao.upsertFramedata(CachedFramedataEntity("steve", "{}", 1L))
        assertEquals("{}", dao.getFramedata("steve")?.jsonBlob)
        dao.upsertFramedata(CachedFramedataEntity("steve", "{\"x\":1}", 2L))
        assertEquals(2L, dao.getFramedata("steve")?.fetchedAt)
    }

    @Test fun winrateRoundTrip() = runTest {
        val dao = db.cacheDao()
        dao.upsertWinrate(CachedWinrateEntity("sonic", "steve", 0.42f, 100, 10, 1L))
        val w = dao.getWinrate("sonic", "steve")
        assertNotNull(w)
        assertEquals(0.42f, w!!.winRateA, 0.0001f)
    }

    @Test fun bestPlayersListed() = runTest {
        val dao = db.cacheDao()
        dao.upsertBestPlayer(CachedBestPlayerEntity("steve", "Onin", 1L, 100f, 1L))
        dao.upsertBestPlayer(CachedBestPlayerEntity("sonic", "Sonix", 2L, 90f, 1L))
        assertEquals(2, dao.getAllBestPlayers().size)
    }

    @Test fun youtubeVideoStored() = runTest {
        val dao = db.cacheDao()
        dao.upsertVideo(CachedYoutubeVideoEntity("char:steve", "abc", "title", 0L, 1L))
        assertEquals("abc", dao.getVideo("char:steve")?.videoId)
    }
}
