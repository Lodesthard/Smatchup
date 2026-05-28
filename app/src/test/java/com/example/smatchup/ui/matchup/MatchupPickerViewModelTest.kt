package com.example.smatchup.ui.matchup

import app.cash.turbine.test
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchupPickerViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val roster = listOf(
        Character(id = "fox", name = "Fox", series = "Star Fox", rosterNumber = 8),
        Character(id = "steve", name = "Steve", series = "Minecraft", rosterNumber = 82),
    )

    private fun vm() = MatchupPickerViewModel { roster }

    @Test fun loadsRoster() = runTest {
        vm().state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(2, s.roster.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun pickingBothFillsReadyPair() = runTest {
        val vm = vm()
        vm.pick("fox")
        vm.pick("steve")
        vm.state.test {
            var s = awaitItem()
            while (s.ready == null) s = awaitItem()
            assertEquals("fox" to "steve", s.ready)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `picking same character twice keeps slotB null`() = runTest {
        val vm = vm()
        vm.pick("fox")
        vm.pick("fox")
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("fox", s.slotA)
            assertNull(s.slotB)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
