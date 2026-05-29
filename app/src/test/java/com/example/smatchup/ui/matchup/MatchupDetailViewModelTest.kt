package com.example.smatchup.ui.matchup

import app.cash.turbine.test
import com.example.smatchup.data.winrate.WinrateResult
import com.example.smatchup.domain.model.ApiResult
import com.example.smatchup.domain.model.Matchup
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
class MatchupDetailViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val mu = Matchup(
        charA = "sonic", charB = "steve",
        gameplanA = listOf("hold center"), gameplanB = listOf("wall"),
    )

    @Test fun loadsMatchupAndDefaultsToGameplanTab() = runTest {
        val vm = MatchupDetailViewModel(
            charA = "sonic", charB = "steve",
            loadMatchup = { mu },
            loadWinrate = { ApiResult.Unauthorized },
            loadVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("sonic", s.matchup?.charA)
            assertEquals(MatchupDetailTab.GAMEPLAN, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun winrateUnauthorizedSetsTokenGated() = runTest {
        val vm = MatchupDetailViewModel(
            charA = "sonic", charB = "steve",
            loadMatchup = { mu },
            loadWinrate = { ApiResult.Unauthorized },
            loadVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (!s.winrateTokenGated) s = awaitItem()
            assertTrue(s.winrateTokenGated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun winrateSuccessPopulatesBar() = runTest {
        val vm = MatchupDetailViewModel(
            charA = "sonic", charB = "steve",
            loadMatchup = { mu },
            loadWinrate = { ApiResult.Success(WinrateResult(winRateA = 0.6f, sampleSize = 10)) },
            loadVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.winRateA == null) s = awaitItem()
            assertEquals(0.6f, s.winRateA!!, 0.0001f)
            assertEquals(10, s.winrateSample)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun selectTabUpdates() = runTest {
        val vm = MatchupDetailViewModel(
            charA = "sonic", charB = "steve",
            loadMatchup = { mu },
            loadWinrate = { ApiResult.Unauthorized },
            loadVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.selectTab(MatchupDetailTab.STAGES)
            s = awaitItem()
            assertEquals(MatchupDetailTab.STAGES, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
