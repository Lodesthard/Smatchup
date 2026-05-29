package com.example.smatchup.ui.auth

import app.cash.turbine.test
import com.example.smatchup.data.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    @Test fun successfulLoginSetsLoggedIn() = runTest {
        val vm = AuthViewModel(
            doLogin = { _, _ -> AuthResult.Success(1L) },
            doRegister = { _, _, _ -> AuthResult.Failure("unused") },
        )
        vm.login("dim", "hunter2")
        vm.state.test {
            var s = awaitItem()
            while (!s.loggedIn && s.error == null) s = awaitItem()
            assertTrue(s.loggedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun failedLoginSetsError() = runTest {
        val vm = AuthViewModel(
            doLogin = { _, _ -> AuthResult.Failure("Identifiants invalides") },
            doRegister = { _, _, _ -> AuthResult.Failure("unused") },
        )
        vm.login("dim", "bad")
        vm.state.test {
            var s = awaitItem()
            while (s.error == null && !s.loggedIn) s = awaitItem()
            assertEquals("Identifiants invalides", s.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun registerValidatesPasswordConfirmLocally() = runTest {
        val vm = AuthViewModel(
            doLogin = { _, _ -> AuthResult.Failure("unused") },
            doRegister = { _, _, _ -> AuthResult.Success(1L) },
        )
        vm.register("dim", "dim@x.com", "hunter2", "MISMATCH")
        vm.state.test {
            var s = awaitItem()
            while (s.error == null && !s.loggedIn) s = awaitItem()
            assertNotNull(s.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
