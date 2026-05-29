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

/** Semantic auth failures; resolved to a localized message in the UI layer. */
enum class AuthError {
    FIELDS_REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    INVALID_CREDENTIALS,
    PSEUDO_TAKEN,
    EMAIL_TAKEN,
    ACCOUNT_EXISTS,
    UNKNOWN,
}

sealed interface AuthResult {
    data class Success(val userId: Long) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

class AuthRepository(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
) {

    val session: Flow<Long?> = sessionDao.observe().map { it?.userId }

    suspend fun register(pseudo: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            if (userDao.findByPseudo(pseudo) != null) return@withContext AuthResult.Failure(AuthError.PSEUDO_TAKEN)
            if (userDao.findByEmail(email) != null) return@withContext AuthResult.Failure(AuthError.EMAIL_TAKEN)
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
                AuthResult.Failure(AuthError.ACCOUNT_EXISTS)
            }
        }

    suspend fun login(identifier: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val user = userDao.findByPseudo(identifier) ?: userDao.findByEmail(identifier)
                ?: return@withContext AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
            if (!PasswordHasher.verify(password, user.salt, user.passwordHash))
                return@withContext AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
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
