# Smatchup — Auth + Favorites + Profile Implementation Plan (sub-project 6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local accounts (register/login with pseudo + email + password), a session that gates the app, a favorites system (characters + matchups) with toggle hearts on the detail screens, a Favorites browse screen, and a Profile screen with logout.

**Architecture:** `PasswordHasher` (PBKDF2, no deps) backs `AuthRepository`, which uses the already-shipped `UserDao` + `SessionDao` to register/login/logout and expose the current user id. `FavoritesRepository` wraps the already-shipped `FavoritesDao` (normalizing matchup order). The Splash screen consults the session row to route to Home (logged in) or Login (logged out). A small `FavoriteToggleViewModel` powers heart buttons overlaid on `CharacterDetailScreen` and `MatchupDetailScreen`. `FavoritesViewModel`/`ProfileViewModel` drive their screens. All ViewModels are wired through the existing manual-DI `AppContainer` + `ViewModelFactory`.

**Tech Stack:** existing stack (Kotlin, Compose Material3, Room, coroutines, `org.json`, `javax.crypto`). No new dependencies — `Icons.Filled.Favorite` / `Icons.Outlined.FavoriteBorder` ship in `material-icons-core`.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — sections 6.4/7.1/7.2/7.3 (users, favorites, session, hashing), 7.5 (session routing), 8.2/8.3/8.5 (nav, components, ViewModels). Builds on sub-projects 1–5. Room entities + DAOs (`UserDao`, `FavoritesDao`, `SessionDao`) already exist from sub-project 1.

**Out of scope (deferred):** `BestPlayersWorker` (spec §6.5). It requires `START_GG_TOKEN`, which is blank in this build; `BestPlayerRepository` already serves seed data, and `StartGgApi` gates to `Unauthorized`. Adding WorkManager scaffolding now would be unverifiable. Revisit when a token is provisioned.

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── data/auth/PasswordHasher.kt
├── data/repository/AuthRepository.kt
├── data/repository/FavoritesRepository.kt
├── ui/auth/AuthViewModel.kt
├── ui/auth/LoginScreen.kt
├── ui/auth/RegisterScreen.kt
├── ui/favorites/FavoritesViewModel.kt
├── ui/favorites/FavoritesScreen.kt
├── ui/profile/ProfileViewModel.kt
├── ui/profile/ProfileScreen.kt
├── ui/components/FavoriteHeart.kt
└── ui/components/FavoriteToggleViewModel.kt

app/src/test/java/com/example/smatchup/
├── data/auth/PasswordHasherTest.kt
├── data/repository/AuthRepositoryTest.kt
├── data/repository/FavoritesRepositoryTest.kt
└── ui/auth/AuthViewModelTest.kt
```

### Modified

- `di/AppContainer.kt` — register `passwordHasher`, `authRepository`, `favoritesRepository`.
- `ui/ViewModelFactory.kt` — register `AuthViewModel`, `FavoritesViewModel`, `ProfileViewModel`, and parameterized `favoriteCharacter(charId)` / `favoriteMatchup(a,b)`.
- `ui/splash/SplashScreen.kt` — route based on session.
- `ui/nav/NavGraph.kt` — wire Login, Register, Favorites, Profile; splash routing.
- `ui/character/CharacterDetailScreen.kt` — overlay `FavoriteHeart`.
- `ui/matchup/MatchupDetailScreen.kt` — overlay `FavoriteHeart`.

### Untouched

All Room entities + DAOs (`UserDao`, `FavoritesDao`, `SessionDao`, plus the `users` / `favorite_characters` / `favorite_matchups` / `session` tables) already exist from sub-project 1.

---

## Conventions

- **Branch:** `git checkout -b auth-favorites` off `api`.
- One commit per task. TDD for `PasswordHasher`, `AuthRepository`, `FavoritesRepository`, `AuthViewModel`. UI smoke-tested on emulator.
- DAOs are Kotlin `interface`s, so JVM tests use hand-written in-memory fakes (no Robolectric / no instrumented test needed for repos).
- **Single-device rule:** never run two `connectedDebugAndroidTest` at once.

---

## Task 1: `PasswordHasher`

PBKDF2WithHmacSHA256, 120 000 iterations, 256-bit, 16-byte random salt, Base64 storage. Pure JVM (`javax.crypto`), so unit-testable directly.

**Files:** `app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt`, `app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt
package com.example.smatchup.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test fun verifySucceedsForCorrectPassword() {
        val salt = hasher.generateSalt()
        val hash = hasher.hash("hunter2", salt)
        assertTrue(hasher.verify("hunter2", salt, hash))
    }

    @Test fun verifyFailsForWrongPassword() {
        val salt = hasher.generateSalt()
        val hash = hasher.hash("hunter2", salt)
        assertFalse(hasher.verify("wrong", salt, hash))
    }

    @Test fun differentSaltsProduceDifferentHashes() {
        val h1 = hasher.hash("hunter2", hasher.generateSalt())
        val h2 = hasher.hash("hunter2", hasher.generateSalt())
        assertNotEquals(h1, h2)
    }

    @Test fun sameSaltAndPasswordIsDeterministic() {
        val salt = hasher.generateSalt()
        assertEquals(hasher.hash("hunter2", salt), hasher.hash("hunter2", salt))
    }
}
```

- [ ] **Step 2: Run, verify it fails.** `./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.auth.PasswordHasherTest"` → FAIL (unresolved `PasswordHasher`).

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt
package com.example.smatchup.data.auth

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher {

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean =
        hash(password, salt) == expectedHash

    private companion object {
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
    }
}
```

> **Note:** `android.util.Base64` is stubbed (returns 0/throws) under plain JVM unit tests. If the test fails on `Base64`, the project already runs unit tests against android.jar stubs only when `testOptions.unitTests.isReturnDefaultValues` is set. To keep this test pure-JVM, replace `android.util.Base64` with `java.util.Base64` (`Base64.getEncoder().encodeToString(...)` / `Base64.getDecoder().decode(...)`), which is available on both JVM and Android (API 26+; this app's minSdk is 24, but `java.util.Base64` is desugared by AGP). Use `java.util.Base64` to be safe:

```kotlin
import java.util.Base64

fun generateSalt(): String {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return Base64.getEncoder().encodeToString(salt)
}
// hash(): Base64.getEncoder().encodeToString(hashBytes)
//         Base64.getDecoder().decode(salt)
```

Implement with `java.util.Base64` (drop the `android.util.Base64` import).

- [ ] **Step 4: Run, verify pass.** Same command → 4 PASS.

- [ ] **Step 5: Commit.**

```bash
git checkout -b auth-favorites
git add app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt \
        app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt
git commit -m "feat(auth): add PasswordHasher (PBKDF2)"
```

---

## Task 2: `AuthRepository`

Register, login (by pseudo OR email), logout, current-user lookup, session observation. Uses `UserDao` + `SessionDao` + `PasswordHasher`.

**Files:** `app/src/main/java/com/example/smatchup/data/repository/AuthRepository.kt`, `app/src/test/java/com/example/smatchup/data/repository/AuthRepositoryTest.kt`.

- [ ] **Step 1: Failing JVM test (with in-memory DAO fakes).**

```kotlin
// app/src/test/java/com/example/smatchup/data/repository/AuthRepositoryTest.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.auth.PasswordHasher
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

    private fun repo() = AuthRepository(FakeUserDao(), FakeSessionDao(), PasswordHasher())

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
```

- [ ] **Step 2: Run, verify it fails.** `./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.repository.AuthRepositoryTest"` → FAIL (unresolved `AuthRepository` / `AuthResult`).

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/AuthRepository.kt
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
    private val hasher: PasswordHasher,
) {

    val session: Flow<Long?> = sessionDao.observe().map { it?.userId }

    suspend fun register(pseudo: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            if (userDao.findByPseudo(pseudo) != null) return@withContext AuthResult.Failure("Pseudo déjà pris")
            if (userDao.findByEmail(email) != null) return@withContext AuthResult.Failure("Email déjà utilisé")
            val salt = hasher.generateSalt()
            val user = UserEntity(
                pseudo = pseudo,
                email = email,
                passwordHash = hasher.hash(password, salt),
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
            if (!hasher.verify(password, user.salt, user.passwordHash))
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
```

- [ ] **Step 4: Run, verify pass.** Same command → 6 PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/data/repository/AuthRepository.kt \
        app/src/test/java/com/example/smatchup/data/repository/AuthRepositoryTest.kt
git commit -m "feat(auth): add AuthRepository (register/login/logout/session)"
```

---

## Task 3: `FavoritesRepository`

Toggle + observe favorites for characters and matchups. Matchup ids are normalized so `charA <= charB` lexicographically (matches the entity contract).

**Files:** `app/src/main/java/com/example/smatchup/data/repository/FavoritesRepository.kt`, `app/src/test/java/com/example/smatchup/data/repository/FavoritesRepositoryTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/data/repository/FavoritesRepositoryTest.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.local.dao.FavoritesDao
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeFavoritesDao : FavoritesDao {
    val chars = MutableStateFlow<List<FavoriteCharacterEntity>>(emptyList())
    val mus = MutableStateFlow<List<FavoriteMatchupEntity>>(emptyList())

    override suspend fun addCharacter(fav: FavoriteCharacterEntity) {
        chars.value = chars.value.filterNot { it.userId == fav.userId && it.charId == fav.charId } + fav
    }
    override suspend fun removeCharacter(userId: Long, charId: String) {
        chars.value = chars.value.filterNot { it.userId == userId && it.charId == charId }
    }
    override fun observeCharacters(userId: Long): Flow<List<FavoriteCharacterEntity>> =
        chars.map { list -> list.filter { it.userId == userId } }
    override suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean =
        chars.value.any { it.userId == userId && it.charId == charId }

    override suspend fun addMatchup(fav: FavoriteMatchupEntity) {
        mus.value = mus.value.filterNot { it.userId == fav.userId && it.charA == fav.charA && it.charB == fav.charB } + fav
    }
    override suspend fun removeMatchup(userId: Long, charA: String, charB: String) {
        mus.value = mus.value.filterNot { it.userId == userId && it.charA == charA && it.charB == charB }
    }
    override fun observeMatchups(userId: Long): Flow<List<FavoriteMatchupEntity>> =
        mus.map { list -> list.filter { it.userId == userId } }
    override suspend fun isMatchupFavorite(userId: Long, charA: String, charB: String): Boolean =
        mus.value.any { it.userId == userId && it.charA == charA && it.charB == charB }
}

class FavoritesRepositoryTest {

    private fun repo() = FavoritesRepository(FakeFavoritesDao())

    @Test fun toggleCharacterAddsThenRemoves() = runBlocking {
        val r = repo()
        assertTrue(r.toggleCharacter(1L, "fox"))   // now favorite
        assertTrue(r.isCharacterFavorite(1L, "fox"))
        assertFalse(r.toggleCharacter(1L, "fox"))  // removed
        assertFalse(r.isCharacterFavorite(1L, "fox"))
    }

    @Test fun matchupOrderIsNormalized() = runBlocking {
        val r = repo()
        r.toggleMatchup(1L, "sonic", "fox")        // stored as fox/sonic
        assertTrue(r.isMatchupFavorite(1L, "fox", "sonic"))
        assertTrue(r.isMatchupFavorite(1L, "sonic", "fox"))  // either order resolves
    }

    @Test fun observeCharactersReturnsIds() = runBlocking {
        val r = repo()
        r.toggleCharacter(1L, "fox")
        r.toggleCharacter(1L, "steve")
        val ids = r.observeCharacterIds(1L).first()
        assertEquals(setOf("fox", "steve"), ids.toSet())
    }
}
```

- [ ] **Step 2: Run, verify it fails.** `./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.repository.FavoritesRepositoryTest"` → FAIL.

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/FavoritesRepository.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.local.dao.FavoritesDao
import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val dao: FavoritesDao) {

    /** Returns the new favorite state (true = now a favorite). */
    suspend fun toggleCharacter(userId: Long, charId: String): Boolean {
        return if (dao.isCharacterFavorite(userId, charId)) {
            dao.removeCharacter(userId, charId)
            false
        } else {
            dao.addCharacter(FavoriteCharacterEntity(userId, charId, System.currentTimeMillis()))
            true
        }
    }

    suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean =
        dao.isCharacterFavorite(userId, charId)

    fun observeCharacterIds(userId: Long): Flow<List<String>> =
        dao.observeCharacters(userId).map { list -> list.map { it.charId } }

    /** Returns the new favorite state (true = now a favorite). */
    suspend fun toggleMatchup(userId: Long, a: String, b: String): Boolean {
        val (x, y) = normalize(a, b)
        return if (dao.isMatchupFavorite(userId, x, y)) {
            dao.removeMatchup(userId, x, y)
            false
        } else {
            dao.addMatchup(FavoriteMatchupEntity(userId, x, y, System.currentTimeMillis()))
            true
        }
    }

    suspend fun isMatchupFavorite(userId: Long, a: String, b: String): Boolean {
        val (x, y) = normalize(a, b)
        return dao.isMatchupFavorite(userId, x, y)
    }

    fun observeMatchupPairs(userId: Long): Flow<List<Pair<String, String>>> =
        dao.observeMatchups(userId).map { list -> list.map { it.charA to it.charB } }

    private fun normalize(a: String, b: String): Pair<String, String> =
        if (a <= b) a to b else b to a
}
```

- [ ] **Step 4: Run, verify pass.** Same command → 3 PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/data/repository/FavoritesRepository.kt \
        app/src/test/java/com/example/smatchup/data/repository/FavoritesRepositoryTest.kt
git commit -m "feat(favorites): add FavoritesRepository (toggle + observe, normalized matchups)"
```

---

## Task 4: Wire `AppContainer`

**Files:** modify `app/src/main/java/com/example/smatchup/di/AppContainer.kt`.

- [ ] **Step 1: Add imports** after the existing repository imports:

```kotlin
import com.example.smatchup.data.auth.PasswordHasher
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.FavoritesRepository
```

- [ ] **Step 2: Register singletons** after `val tierlistRepository: ...`:

```kotlin
    val passwordHasher: PasswordHasher = PasswordHasher()
    val authRepository: AuthRepository = AuthRepository(
        userDao = database.userDao(),
        sessionDao = database.sessionDao(),
        hasher = passwordHasher,
    )
    val favoritesRepository: FavoritesRepository = FavoritesRepository(database.favoritesDao())
```

- [ ] **Step 3: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/di/AppContainer.kt
git commit -m "feat(di): register auth + favorites repositories"
```

---

## Task 5: `AuthViewModel`

Holds form state + result. Validates input client-side, calls `AuthRepository`.

**Files:** `app/src/main/java/com/example/smatchup/ui/auth/AuthViewModel.kt`, `app/src/test/java/com/example/smatchup/ui/auth/AuthViewModelTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/ui/auth/AuthViewModelTest.kt
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
            assertNotNull(s.error)   // client-side mismatch, never hits repo
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run, verify it fails.** `./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.auth.AuthViewModelTest"` → FAIL.

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/auth/AuthViewModel.kt
package com.example.smatchup.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val loggedIn: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val doLogin: suspend (identifier: String, password: String) -> AuthResult,
    private val doRegister: suspend (pseudo: String, email: String, password: String) -> AuthResult,
) : ViewModel() {

    constructor(repo: AuthRepository) : this(
        doLogin = { id, pw -> repo.login(id, pw) },
        doRegister = { p, e, pw -> repo.register(p, e, pw) },
    )

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Remplis tous les champs") }
            return
        }
        run { id, pw -> doLogin(id, pw) }.let { } // placeholder removed below
        submit { doLogin(identifier, password) }
    }

    fun register(pseudo: String, email: String, password: String, confirm: String) {
        when {
            pseudo.isBlank() || email.isBlank() || password.isBlank() ->
                { _state.update { it.copy(error = "Remplis tous les champs") }; return }
            !email.contains("@") ->
                { _state.update { it.copy(error = "Email invalide") }; return }
            password.length < 6 ->
                { _state.update { it.copy(error = "Mot de passe trop court (min 6)") }; return }
            password != confirm ->
                { _state.update { it.copy(error = "Les mots de passe ne correspondent pas") }; return }
        }
        submit { doRegister(pseudo, email, password) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    private fun submit(block: suspend () -> AuthResult) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val res = block()) {
                is AuthResult.Success -> _state.update { it.copy(isLoading = false, loggedIn = true) }
                is AuthResult.Failure -> _state.update { it.copy(isLoading = false, error = res.reason) }
            }
        }
    }
}
```

> **Fix the `login` body** — remove the stray `run {…}.let { }` placeholder line; the real `login` is:

```kotlin
    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Remplis tous les champs") }
            return
        }
        submit { doLogin(identifier, password) }
    }
```

> **Fix the `register` `when`** — Kotlin `when` branches that both run a statement and `return` must be a block. Write it as guard `if`s instead:

```kotlin
    fun register(pseudo: String, email: String, password: String, confirm: String) {
        if (pseudo.isBlank() || email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Remplis tous les champs") }; return
        }
        if (!email.contains("@")) { _state.update { it.copy(error = "Email invalide") }; return }
        if (password.length < 6) { _state.update { it.copy(error = "Mot de passe trop court (min 6)") }; return }
        if (password != confirm) { _state.update { it.copy(error = "Les mots de passe ne correspondent pas") }; return }
        submit { doRegister(pseudo, email, password) }
    }
```

Use these corrected bodies in the implementation (do not include the placeholder line).

- [ ] **Step 4: Run, verify pass.** Same command → 3 PASS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/auth/AuthViewModel.kt \
        app/src/test/java/com/example/smatchup/ui/auth/AuthViewModelTest.kt
git commit -m "feat(auth): add AuthViewModel with client-side validation"
```

---

## Task 6: `LoginScreen` + `RegisterScreen`

World-of-Light styled forms using `GlowButton`, `wolBackground`, `SmatchupColors`.

**Files:** `app/src/main/java/com/example/smatchup/ui/auth/LoginScreen.kt`, `app/src/main/java/com/example/smatchup/ui/auth/RegisterScreen.kt`.

- [ ] **Step 1: Create `LoginScreen.kt`.**

```kotlin
package com.example.smatchup.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.GlowButton
import com.example.smatchup.ui.components.GlowButtonVariant
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoggedIn() }

    Column(
        modifier = modifier.wolBackground().fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Smatchup", style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold)
        Text("Connexion", style = MaterialTheme.typography.titleLarge, color = SmatchupColors.Text)

        OutlinedTextField(
            value = identifier, onValueChange = { identifier = it },
            label = { Text("Pseudo ou email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(it, color = SmatchupColors.DangerRed) }

        GlowButton(
            text = if (state.isLoading) "..." else "Se connecter",
            onClick = { viewModel.login(identifier, password) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        GlowButton(
            text = "Créer un compte",
            onClick = onGoToRegister,
            variant = GlowButtonVariant.GHOST,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 2: Create `RegisterScreen.kt`.**

```kotlin
package com.example.smatchup.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.GlowButton
import com.example.smatchup.ui.components.GlowButtonVariant
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()
    var pseudo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onRegistered() }

    Column(
        modifier = modifier.wolBackground().fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Inscription", style = MaterialTheme.typography.displaySmall, color = SmatchupColors.Gold)

        OutlinedTextField(pseudo, { pseudo = it }, label = { Text("Pseudo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirmer le mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = SmatchupColors.DangerRed) }

        GlowButton(
            text = if (state.isLoading) "..." else "S'inscrire",
            onClick = { viewModel.register(pseudo, email, password, confirm) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        GlowButton(text = "J'ai déjà un compte", onClick = onGoToLogin, variant = GlowButtonVariant.GHOST, modifier = Modifier.fillMaxWidth())
    }
}
```

- [ ] **Step 3: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/auth/LoginScreen.kt \
        app/src/main/java/com/example/smatchup/ui/auth/RegisterScreen.kt
git commit -m "feat(auth): add Login + Register screens"
```

---

## Task 7: `FavoriteToggleViewModel` + `FavoriteHeart`

A small VM driving a heart button for either a character or a matchup. Created via parameterized factory methods.

**Files:** `app/src/main/java/com/example/smatchup/ui/components/FavoriteToggleViewModel.kt`, `app/src/main/java/com/example/smatchup/ui/components/FavoriteHeart.kt`.

- [ ] **Step 1: Create `FavoriteToggleViewModel.kt`.**

```kotlin
package com.example.smatchup.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoriteToggleViewModel(
    private val isFav: suspend (userId: Long) -> Boolean,
    private val toggle: suspend (userId: Long) -> Boolean,
    private val currentUserId: suspend () -> Long?,
) : ViewModel() {

    constructor(authRepo: AuthRepository, favRepo: FavoritesRepository, charId: String) : this(
        isFav = { uid -> favRepo.isCharacterFavorite(uid, charId) },
        toggle = { uid -> favRepo.toggleCharacter(uid, charId) },
        currentUserId = { authRepo.currentUserId() },
    )

    constructor(authRepo: AuthRepository, favRepo: FavoritesRepository, charA: String, charB: String) : this(
        isFav = { uid -> favRepo.isMatchupFavorite(uid, charA, charB) },
        toggle = { uid -> favRepo.toggleMatchup(uid, charA, charB) },
        currentUserId = { authRepo.currentUserId() },
    )

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            _isFavorite.value = isFav(uid)
        }
    }

    fun toggle() {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            _isFavorite.value = toggle(uid)
        }
    }
}
```

- [ ] **Step 2: Create `FavoriteHeart.kt`.**

```kotlin
package com.example.smatchup.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun FavoriteHeart(
    viewModel: FavoriteToggleViewModel,
    modifier: Modifier = Modifier,
) {
    val isFavorite by viewModel.isFavorite.collectAsState()
    IconButton(onClick = viewModel::toggle, modifier = modifier) {
        if (isFavorite) {
            Icon(Icons.Filled.Favorite, contentDescription = "Retirer des favoris", tint = SmatchupColors.Gold)
        } else {
            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Ajouter aux favoris", tint = SmatchupColors.TextDim)
        }
    }
}
```

- [ ] **Step 3: Build** (also needs factory methods from Task 9, so this compiles standalone now; wiring happens in Tasks 9–10). `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/components/FavoriteToggleViewModel.kt \
        app/src/main/java/com/example/smatchup/ui/components/FavoriteHeart.kt
git commit -m "feat(favorites): add FavoriteToggleViewModel + FavoriteHeart component"
```

---

## Task 8: `FavoritesViewModel` + `FavoritesScreen`

Observes the session, then the user's favorite character ids + matchup pairs, resolving ids to `Character` for display via `CharacterRepository.loadRoster()`.

**Files:** `app/src/main/java/com/example/smatchup/ui/favorites/FavoritesViewModel.kt`, `app/src/main/java/com/example/smatchup/ui/favorites/FavoritesScreen.kt`.

- [ ] **Step 1: Create `FavoritesViewModel.kt`.**

```kotlin
package com.example.smatchup.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.data.repository.FavoritesRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val characters: List<Character> = emptyList(),
    val matchups: List<Pair<Character, Character>> = emptyList(),
)

class FavoritesViewModel(
    private val authRepo: AuthRepository,
    private val favRepo: FavoritesRepository,
    private val characterRepo: CharacterRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepo.currentUserId() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val byId = characterRepo.loadRoster().associateBy { it.id }
            combine(
                favRepo.observeCharacterIds(uid),
                favRepo.observeMatchupPairs(uid),
            ) { charIds, pairs ->
                FavoritesUiState(
                    isLoading = false,
                    characters = charIds.mapNotNull { byId[it] },
                    matchups = pairs.mapNotNull { (a, b) ->
                        val ca = byId[a]; val cb = byId[b]
                        if (ca != null && cb != null) ca to cb else null
                    },
                )
            }.collect { merged -> _state.value = merged }
        }
    }
}
```

- [ ] **Step 2: Create `FavoritesScreen.kt`.**

```kotlin
package com.example.smatchup.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun FavoritesScreen(
    onCharacterClick: (String) -> Unit,
    onMatchupClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.wolBackground().fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Favoris", style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold, modifier = Modifier.padding(top = 24.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.characters.isEmpty() && state.matchups.isEmpty() ->
                EmptyState(message = "Aucun favori pour l'instant.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (state.characters.isNotEmpty()) {
                    item { Text("Personnages", style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Purple) }
                    items(items = state.characters, key = { "c-${it.id}" }) { c -> CharacterFavRow(c, onCharacterClick) }
                }
                if (state.matchups.isNotEmpty()) {
                    item { Text("Match-ups", style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Purple) }
                    items(items = state.matchups, key = { "m-${it.first.id}-${it.second.id}" }) { (a, b) ->
                        MatchupFavRow(a, b, onMatchupClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterFavRow(c: Character, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(c.id) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PortraitOrb(charId = c.id, size = 40.dp)
        Text(c.name, style = MaterialTheme.typography.bodyLarge, color = SmatchupColors.Text)
    }
}

@Composable
private fun MatchupFavRow(a: Character, b: Character, onClick: (String, String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(a.id, b.id) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PortraitOrb(charId = a.id, size = 36.dp)
        Text("vs", style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.TextDim)
        PortraitOrb(charId = b.id, size = 36.dp)
        Text("${a.name} / ${b.name}", style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.Text)
    }
}
```

- [ ] **Step 3: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/favorites/FavoritesViewModel.kt \
        app/src/main/java/com/example/smatchup/ui/favorites/FavoritesScreen.kt
git commit -m "feat(favorites): add FavoritesViewModel + FavoritesScreen"
```

---

## Task 9: `ProfileViewModel` + `ProfileScreen` + ViewModelFactory wiring

Profile shows the logged-in user (pseudo, email) and a logout button. This task also registers **all** new ViewModels in `ViewModelFactory`.

**Files:** `app/src/main/java/com/example/smatchup/ui/profile/ProfileViewModel.kt`, `app/src/main/java/com/example/smatchup/ui/profile/ProfileScreen.kt`, modify `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt`.

- [ ] **Step 1: Create `ProfileViewModel.kt`.**

```kotlin
package com.example.smatchup.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val pseudo: String = "",
    val email: String = "",
    val loggedOut: Boolean = false,
)

class ProfileViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val u = authRepo.currentUser()
            _state.value = ProfileUiState(
                isLoading = false,
                pseudo = u?.pseudo.orEmpty(),
                email = u?.email.orEmpty(),
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}
```

- [ ] **Step 2: Create `ProfileScreen.kt`.**

```kotlin
package com.example.smatchup.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.GlowButton
import com.example.smatchup.ui.components.GlowButtonVariant
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Column(
        modifier = modifier.wolBackground().fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Profil", style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold, modifier = Modifier.padding(top = 24.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
        } else {
            Text(state.pseudo, style = MaterialTheme.typography.titleLarge, color = SmatchupColors.Text)
            Text(state.email, style = MaterialTheme.typography.bodyMedium, color = SmatchupColors.TextDim)
            GlowButton(
                text = "Se déconnecter",
                onClick = viewModel::logout,
                variant = GlowButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

- [ ] **Step 3: Register ViewModels in `ViewModelFactory.kt`.** Add imports:

```kotlin
import com.example.smatchup.ui.auth.AuthViewModel
import com.example.smatchup.ui.components.FavoriteToggleViewModel
import com.example.smatchup.ui.favorites.FavoritesViewModel
import com.example.smatchup.ui.profile.ProfileViewModel
```

Add to the `create()` `when` (before the `else`):

```kotlin
        AuthViewModel::class.java      -> AuthViewModel(container.authRepository) as T
        FavoritesViewModel::class.java -> FavoritesViewModel(
            container.authRepository, container.favoritesRepository, container.characterRepository
        ) as T
        ProfileViewModel::class.java   -> ProfileViewModel(container.authRepository) as T
```

Add two parameterized factory methods (after `characterDetail(...)`):

```kotlin
    fun favoriteCharacter(charId: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == FavoriteToggleViewModel::class.java)
                return FavoriteToggleViewModel(
                    container.authRepository, container.favoritesRepository, charId
                ) as T
            }
        }

    fun favoriteMatchup(charA: String, charB: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == FavoriteToggleViewModel::class.java)
                return FavoriteToggleViewModel(
                    container.authRepository, container.favoritesRepository, charA, charB
                ) as T
            }
        }
```

- [ ] **Step 4: Build.** `./gradlew :app:compileDebugKotlin` → SUCCESS.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/profile/ProfileViewModel.kt \
        app/src/main/java/com/example/smatchup/ui/profile/ProfileScreen.kt \
        app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt
git commit -m "feat(profile): add Profile screen + register auth/favorites/profile ViewModels"
```

---

## Task 10: Splash session routing + NavGraph wiring

Splash decides Home vs Login. Wire Login, Register, Favorites, Profile routes. Add favorite hearts to the two detail screens.

**Files:** modify `ui/splash/SplashScreen.kt`, `ui/nav/NavGraph.kt`, `ui/character/CharacterDetailScreen.kt`, `ui/matchup/MatchupDetailScreen.kt`.

- [ ] **Step 1: Update `SplashScreen.kt`** to resolve the session. Replace the signature + `LaunchedEffect`:

```kotlin
import com.example.smatchup.SmatchupApp
// ...
@Composable
fun SplashScreen(
    onResolved: (loggedIn: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sessionCheck: suspend () -> Boolean = { SmatchupApp.instance.container.authRepository.currentUserId() != null },
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600)
        onResolved(sessionCheck())
    }
    // ... existing Column body unchanged ...
}
```

- [ ] **Step 2: Update the Splash composable in `NavGraph.kt`:**

```kotlin
        composable(Screen.Splash.route) {
            SplashScreen(onResolved = { loggedIn ->
                val dest = if (loggedIn) Screen.Home.route else Screen.Login.route
                nav.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
```

- [ ] **Step 3: Add Login/Register/Favorites/Profile composables in `NavGraph.kt`.** Add imports:

```kotlin
import com.example.smatchup.ui.auth.LoginScreen
import com.example.smatchup.ui.auth.RegisterScreen
import com.example.smatchup.ui.favorites.FavoritesScreen
import com.example.smatchup.ui.profile.ProfileScreen
```

Replace the four sub-project-6 placeholders:

```kotlin
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onGoToRegister = { nav.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = { nav.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onGoToLogin = { nav.popBackStack() },
            )
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onCharacterClick = { charId -> nav.navigate(Screen.CharacterDetail.buildRoute(charId)) },
                onMatchupClick = { a, b -> nav.navigate(Screen.MatchupDetail.buildRoute(a, b)) },
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLoggedOut = { nav.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
            )
        }
```

(Remove the old `PlaceholderScreen("Favorites …")`, `("Profile …")`, `("Login …")`, `("Register …")` lines.)

- [ ] **Step 4: Add the heart to `CharacterDetailScreen.kt`.** Add imports:

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.FavoriteHeart
import com.example.smatchup.ui.components.FavoriteToggleViewModel
```

Inside the `Box(modifier = modifier.wolBackground())`, after the `when { … }` block (still inside the Box, so it overlays top-right), add:

```kotlin
        if (state.detail != null) {
            FavoriteHeart(
                viewModel = viewModel(
                    factory = ViewModelFactory.fromApp().favoriteCharacter(charId),
                    key = "fav-char-$charId",
                ),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )
        }
```

> `FavoriteToggleViewModel` import is needed for the typed `viewModel<FavoriteToggleViewModel>(...)` call; if Kotlin can infer the type from `FavoriteHeart`'s parameter, the explicit import is still required for the factory's `require(modelClass == FavoriteToggleViewModel::class.java)` path. Add the import.

- [ ] **Step 5: Add the heart to `MatchupDetailScreen.kt`.** Open the file, confirm it has a root `Box` with `Modifier.wolBackground()` (like CharacterDetailScreen). If it uses a `Column` root, wrap the existing content in `Box(modifier.wolBackground()) { … }`. Add the same imports as Step 4 plus read the screen's `charA`/`charB` params (they are passed into the composable — confirm names by reading the function signature). Overlay:

```kotlin
        FavoriteHeart(
            viewModel = viewModel(
                factory = ViewModelFactory.fromApp().favoriteMatchup(charA, charB),
                key = "fav-mu-$charA-$charB",
            ),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
        )
```

> **Read `MatchupDetailScreen.kt` first** to get the exact parameter names for the two character ids and the root layout. Match them; do not assume.

- [ ] **Step 6: Build + install + smoke test.**

```bash
./gradlew :app:installDebug
```

Manual checks on emulator (single device):
1. **Fresh launch (logged out)** → Splash → **Login** screen (not Home). *(To force logged-out on a device that already has a session, clear app data: `adb shell pm clear com.example.smatchup`.)*
2. **Register** a user (pseudo + email + password + confirm) → lands on **Home**.
3. Kill + relaunch → Splash routes straight to **Home** (session persisted).
4. Open a **character detail** → tap the **heart** (fills gold) → back.
5. Home → **Favoris** → the character appears; tap it → its detail opens.
6. Build a **matchup**, open detail, tap its **heart** → Favoris shows the matchup row.
7. Home → **Profil** → shows pseudo + email → **Se déconnecter** → returns to **Login**.
8. Re-login with the same credentials → Home; favorites still present.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/java/com/example/smatchup/ui/splash/SplashScreen.kt \
        app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt \
        app/src/main/java/com/example/smatchup/ui/character/CharacterDetailScreen.kt \
        app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailScreen.kt
git commit -m "feat(nav): session-gated splash + wire auth/favorites/profile + detail hearts"
```

---

## Final verification

- [ ] **Step 1: JVM suite.** `./gradlew :app:testDebugUnitTest` — all green (existing + 16 new: 4 hasher, 6 auth, 3 favorites, 3 auth VM).
- [ ] **Step 2: Instrumented suite (single run).** `./gradlew :app:connectedDebugAndroidTest` — existing tests still pass (no new instrumented tests added).
- [ ] **Step 3: APK.** `./gradlew :app:assembleDebug`.
- [ ] **Step 4: Manual smoke (Task 10 step 6), full pass.**
- [ ] **Step 5: `git log --oneline -12` check.**
- [ ] **Step 6:** `graphify update .`

---

## Self-review

**Spec coverage:**

| Spec section | Item | Task |
|---|---|---|
| §7.3 | PBKDF2 password hashing | 1 |
| §7.1/7.2 | users table + UserDao (register/login) | 2 (reuses existing DAO) |
| §6.4-adjacent / §7.5 | session row, logged-in routing | 2 (session), 10 (splash) |
| §7.1 | favorite_characters / favorite_matchups | 3 (reuses existing DAO) |
| §8.3 | favorites toggle on detail screens | 7 (heart), 10 (wiring) |
| §8.2 | Login/Register/Favorites/Profile routes | 6, 8, 9, 10 |
| §8.5 | AuthViewModel, FavoritesViewModel, ProfileViewModel | 5, 8, 9 |
| §8.1/8.3 | WoL-styled screens reusing GlowButton/EmptyState/PortraitOrb | 6, 8, 9 |
| §6.5 | BestPlayersWorker | **Deferred** (no START_GG_TOKEN; documented above) |

**Placeholder scan:** Task 5 intentionally shows a broken first draft of `AuthViewModel` then corrects it inline (the `submit`/guard-`if` versions are authoritative — do not commit the stray `run{}.let{}` line or the `when`-with-return draft). No other placeholders.

**Type consistency:**
- `AuthResult.Success(userId: Long)` / `AuthResult.Failure(reason: String)` consistent across Tasks 2, 5.
- `AuthRepository(userDao, sessionDao, hasher)` matches AppContainer wiring (Task 4) and test fakes (Task 2).
- `FavoritesRepository(dao)` with `toggleCharacter`/`toggleMatchup` returning `Boolean`, `observeCharacterIds`/`observeMatchupPairs` — consistent across Tasks 3, 7, 8.
- `FavoriteToggleViewModel` two secondary constructors (char / matchup) match the two factory methods `favoriteCharacter`/`favoriteMatchup` (Tasks 7, 9, 10).
- `PasswordHasher.generateSalt()/hash(pw,salt)/verify(pw,salt,hash)` consistent across Tasks 1, 2.
- DAO fakes implement the real interface signatures verified against `UserDao`/`SessionDao`/`FavoritesDao` (Tasks 2, 3).
- `ViewModelFactory.fromApp()` + parameterized factory pattern matches the existing `characterDetail`/`matchupDetail` style.

Plan ready.
