package com.example.smatchup.ui.character

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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val sampleRoster = listOf(
        Character(id = "mario",       name = "Mario",       series = "Super Mario", rosterNumber = 1),
        Character(id = "donkey_kong", name = "Donkey Kong", series = "Donkey Kong", rosterNumber = 2),
        Character(id = "steve",       name = "Steve",       series = "Minecraft",   rosterNumber = 82),
    )

    private fun newVm() = CharacterListViewModel { sampleRoster }

    @Test fun initialStateExposesFullRoster() = runTest {
        val vm = newVm()
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals(3, s.visible.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun queryFiltersByCaseInsensitiveNameSubstring() = runTest {
        val vm = newVm()
        vm.setQuery("ste")
        vm.state.test {
            var s = awaitItem()
            while (s.visible.size != 1) s = awaitItem()
            assertEquals("steve", s.visible.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun blankQueryRestoresFullRoster() = runTest {
        val vm = newVm()
        vm.setQuery("ste")
        vm.setQuery("")
        vm.state.test {
            var s = awaitItem()
            while (s.visible.size != 3) s = awaitItem()
            assertEquals(3, s.visible.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
