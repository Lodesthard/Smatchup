package com.example.smatchup.ui.tierlist

import app.cash.turbine.test
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.Tier
import com.example.smatchup.domain.model.TierGroup
import com.example.smatchup.domain.model.TierlistView
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
class TierlistViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val steve = Character(id = "steve", name = "Steve", series = "Minecraft", rosterNumber = 82)

    private fun viewFor(name: String) = TierlistView(
        name = name, version = "v1",
        groups = listOf(TierGroup(Tier.S_PLUS, listOf(steve))),
    )

    @Test fun defaultsToStrengthAndLoads() = runTest {
        val vm = TierlistViewModel { name -> viewFor(name) }
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("strength", s.selectedName)
            assertEquals(1, s.view?.groups?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun selectingDifficultyReloads() = runTest {
        val vm = TierlistViewModel { name -> viewFor(name) }
        vm.select("difficulty")
        vm.state.test {
            var s = awaitItem()
            while (s.selectedName != "difficulty" || s.isLoading) s = awaitItem()
            assertEquals("difficulty", s.view?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
