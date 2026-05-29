package com.example.smatchup.ui.character

import app.cash.turbine.test
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.Frame
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.MoveCategory
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val steveDetail = CharacterDetail(
        character = Character(id = "steve", name = "Steve", series = "Minecraft", rosterNumber = 82),
        combos = listOf("Block → uair"),
        gameplan = "Wall the opponent.",
    )

    @Test fun initiallyEmitsLoadingThenLoaded() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("Steve", s.detail?.character?.name)
            assertEquals(DetailTab.COMBOS, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun selectTabUpdatesState() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.selectTab(DetailTab.VIDEO)
            s = awaitItem()
            assertEquals(DetailTab.VIDEO, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun missingYoutubeKeyExposesTokenGating() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { Player(tag = "Onin", mainCharacter = "steve") },
            loadLastVideo = { throw IllegalStateException("YOUTUBE_API_KEY missing") },
        )
        vm.state.test {
            var s = awaitItem()
            while (!s.lastVideoTokenGated) s = awaitItem()
            assertTrue(s.lastVideoTokenGated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun framedataSourceUpdatesAfterLoad() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = {
                listOf(Move(id = "fair", displayName = "fair", category = MoveCategory.AERIAL, frame = Frame(startup = 8))) to FramedataSource.BUNDLED
            },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.framedata.isEmpty()) s = awaitItem()
            assertEquals(FramedataSource.BUNDLED, s.framedataSource)
            assertEquals("fair", s.framedata.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
