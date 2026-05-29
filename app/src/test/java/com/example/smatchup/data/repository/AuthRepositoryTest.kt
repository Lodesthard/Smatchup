package com.example.smatchup.data.repository

import com.example.smatchup.data.local.dao.SessionDao
import com.example.smatchup.data.local.dao.UserDao
import com.example.smatchup.data.local.entity.SessionEntity
import com.example.smatchup.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeUserDao : UserDao {
    val rows = mutableListOf<UserEntity>()
    private var nextId = 1L
    override suspend fun insert(user: UserEntity): Long {
        if (rows.any { it.pseudo == user.pseudo || it.email == user.email })
            throw RuntimeException("UNIQUE constraint failed")
        val withId = user.copy(id = nextId++)
        rows.add(withId)
        return withId.id
    }
    override suspend fun findByPseudo(pseudo: String) = rows.firstOrNull { it.pseudo == pseudo }
    override suspend fun findByEmail(email: String) = rows.firstOrNull { it.email == email }
    override suspend fun findById(id: Long) = rows.firstOrNull { it.id == id }
}

private class FakeSessionDao : SessionDao {
    val flow = MutableStateFlow<SessionEntity?>(null)
    override suspend fun upsert(session: SessionEntity) { flow.value = session }
    override suspend fun get(): SessionEntity? = flow.value
    override fun observe(): Flow<SessionEntity?> = flow
}

class AuthRepositoryTest {

    private fun repo() = AuthRepository(FakeUserDao(), FakeSessionDao())

    @Test fun registerThenSessionHasUser() = runBlocking {
        val r = repo()
        val res = r.register("dim", "dim@x.com", "hunter2")
        assertTrue(res is AuthResult.Success)
        assertEquals((res as AuthResult.Success).userId, r.currentUserId())
    }

    @Test fun registerRejectsDuplicatePseudo() = runBlocking {
        val r = repo()
        r.register("dim", "a@x.com", "hunter2")
        val res = r.register("dim", "b@x.com", "hunter2")
        assertTrue(res is AuthResult.Failure)
    }

    @Test fun loginByEmailSucceeds() = runBlocking {
        val r = repo()
        r.register("dim", "dim@x.com", "hunter2")
        r.logout()
        val res = r.login("dim@x.com", "hunter2")
        assertTrue(res is AuthResult.Success)
    }

    @Test fun loginByPseudoSucceeds() = runBlocking {
        val r = repo()
        r.register("dim", "dim@x.com", "hunter2")
        r.logout()
        assertTrue(r.login("dim", "hunter2") is AuthResult.Success)
    }

    @Test fun loginWrongPasswordFails() = runBlocking {
        val r = repo()
        r.register("dim", "dim@x.com", "hunter2")
        r.logout()
        assertTrue(r.login("dim", "nope") is AuthResult.Failure)
    }

    @Test fun logoutClearsSession() = runBlocking {
        val r = repo()
        r.register("dim", "dim@x.com", "hunter2")
        r.logout()
        assertNull(r.currentUserId())
    }
}
