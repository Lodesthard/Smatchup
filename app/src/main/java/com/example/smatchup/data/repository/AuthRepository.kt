package com.example.smatchup.data.repository

import com.example.smatchup.data.auth.PasswordHasher
import com.example.smatchup.data.local.dao.SessionDao
import com.example.smatchup.data.local.dao.UserDao
import com.example.smatchup.data.local.entity.SessionEntity
import com.example.smatchup.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface AuthResult {
    data class Success(val userId: Long) : AuthResult
    data class Failure(val reason: String) : AuthResult
}

class AuthRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
) {

    val session: Flow<Long?> = sessionDao.observe().map { it?.userId }

    suspend fun register(pseudo: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            if (userDao.findByPseudo(pseudo) != null) return@withContext AuthResult.Failure("Pseudo déjà pris")
            if (userDao.findByEmail(email) != null) return@withContext AuthResult.Failure("Email déjà utilisé")
            val salt = PasswordHasher.newSalt()
            val user = UserEntity(
                pseudo = pseudo,
                email = email,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                createdAt = System.currentTimeMillis(),
            )
            try {
                val id = userDao.insert(user)
                setSession(id)
                AuthResult.Success(id)
            } catch (e: Throwable) {
                AuthResult.Failure("Compte déjà existant")
            }
        }

    suspend fun login(identifier: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val user = userDao.findByPseudo(identifier) ?: userDao.findByEmail(identifier)
                ?: return@withContext AuthResult.Failure("Identifiants invalides")
            if (!PasswordHasher.verify(password, user.salt, user.passwordHash))
                return@withContext AuthResult.Failure("Identifiants invalides")
            setSession(user.id)
            AuthResult.Success(user.id)
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionDao.upsert(SessionEntity(id = 0, userId = null, updatedAt = System.currentTimeMillis()))
    }

    suspend fun currentUserId(): Long? = withContext(Dispatchers.IO) { sessionDao.get()?.userId }

    suspend fun currentUser(): UserEntity? = withContext(Dispatchers.IO) {
        sessionDao.get()?.userId?.let { userDao.findById(it) }
    }

    private suspend fun setSession(userId: Long) {
        sessionDao.upsert(SessionEntity(id = 0, userId = userId, updatedAt = System.currentTimeMillis()))
    }
}
