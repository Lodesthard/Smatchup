# Smatchup — Data Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the dataless, UI-less foundation layer that every later sub-project depends on: domain models, Room persistence, API clients (with `ApiResult` + rate-limit + fallback), `JsonAssetLoader`, `CacheManager`, `PasswordHasher`, and the `AppContainer` singleton.

**Architecture:** Single Gradle module. MVVM data layer only — no Composable, no ViewModel, no nav. Repositories deliberately deferred to later sub-projects (they would need UI to be testable end-to-end). All async work uses `kotlin.coroutines` + `Dispatchers.IO`. All cache TTLs centralized in `CacheTtl`. All API clients return `ApiResult<T>`. `start.gg` calls go through `RateLimiter` (token bucket 80/60 s).

**Tech Stack:** Kotlin 2.2.10, AGP 9.2.1, Room 2.7.0 + KSP 2.2.10-2.0.2, OkHttp 4.12.0 + logging-interceptor + mockwebserver, kotlinx-coroutines 1.9.0 + coroutines-test, Turbine 1.1.0, JUnit 4, AndroidX test core/runner/rules.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` (sections 5, 6, 7, 10).

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── SmatchupApp.kt                                   # Application subclass; instantiates AppContainer
├── di/AppContainer.kt                               # Manual singleton holder
├── domain/model/
│   ├── ApiResult.kt                                 # sealed class + helpers
│   ├── Character.kt                                 # data class
│   ├── Move.kt
│   ├── Frame.kt
│   ├── Matchup.kt
│   ├── Stage.kt
│   ├── Player.kt
│   └── TierEntry.kt
├── data/
│   ├── api/
│   │   ├── HttpClientProvider.kt                    # singleton OkHttpClient
│   │   ├── UltimateApi.kt                           # REST → framedata fallback chain (chain itself in repo later)
│   │   ├── StartGgApi.kt                            # GraphQL + RateLimiter + token gating
│   │   ├── YouTubeApi.kt                            # REFACTOR existing
│   │   └── SmashWikiApi.kt                          # MediaWiki TextExtracts (descriptions)
│   ├── local/
│   │   ├── SmatchupDatabase.kt
│   │   ├── entity/
│   │   │   ├── UserEntity.kt
│   │   │   ├── FavoriteCharacterEntity.kt
│   │   │   ├── FavoriteMatchupEntity.kt
│   │   │   ├── CachedFramedataEntity.kt
│   │   │   ├── CachedWinrateEntity.kt
│   │   │   ├── CachedBestPlayerEntity.kt
│   │   │   ├── CachedYoutubeVideoEntity.kt
│   │   │   └── SessionEntity.kt
│   │   └── dao/
│   │       ├── UserDao.kt
│   │       ├── FavoritesDao.kt
│   │       ├── CacheDao.kt
│   │       └── SessionDao.kt
│   ├── assets/JsonAssetLoader.kt
│   ├── cache/
│   │   ├── Clock.kt                                 # tiny interface for test-time
│   │   ├── CacheTtl.kt                              # const TTLs
│   │   ├── CacheManager.kt                          # isFresh / shouldRefresh helpers
│   │   └── RateLimiter.kt                           # token bucket
│   └── auth/PasswordHasher.kt
└── (MainActivity.kt unchanged for this sub-project)

app/src/main/assets/
├── framedata/.gitkeep                               # populated in sub-project 3 (and progressively)
├── characters_meta/steve.json                       # seed (5 chars)
├── characters_meta/sonic.json
├── characters_meta/snake.json
├── characters_meta/fox.json
├── characters_meta/sephiroth.json
├── synergies.json                                   # seed
├── stages/.gitkeep
├── matchups/.gitkeep
├── tierlist_strength.json                           # seed: UltRank 2026 v4 sketch
├── tierlist_difficulty.json                         # seed: Game8 sketch
├── majors_slugs.json                                # seed list
└── seed_best_players.json                           # placeholder map

app/src/test/java/com/example/smatchup/
├── domain/model/ApiResultTest.kt
├── data/auth/PasswordHasherTest.kt
├── data/cache/CacheManagerTest.kt
├── data/cache/RateLimiterTest.kt
├── data/api/UltimateApiTest.kt
├── data/api/StartGgApiTest.kt
├── data/api/YouTubeApiTest.kt
└── data/api/SmashWikiApiTest.kt

app/src/test/resources/fixtures/
├── ultimate_api_steve_moves.json
├── startgg_tournament_sets.json
├── startgg_error_rate_limit.json
├── youtube_search.json
├── youtube_videos_details.json
├── youtube_channels.json
└── smashwiki_extract.json

app/src/androidTest/java/com/example/smatchup/
├── data/local/UserDaoTest.kt
├── data/local/FavoritesDaoTest.kt
├── data/local/CacheDaoTest.kt
├── data/local/SessionDaoTest.kt
└── data/assets/JsonAssetLoaderTest.kt
```

### Modified

- `gradle/libs.versions.toml` — add Room, KSP, OkHttp logging-interceptor, mockwebserver, coroutines-test, turbine, androidx-test, room-testing.
- `build.gradle.kts` (root) — register KSP plugin.
- `app/build.gradle.kts` — apply KSP, declare Room schema dir, inject `START_GG_TOKEN` into `BuildConfig`, add dependencies.
- `app/src/main/AndroidManifest.xml` — `android:name=".SmatchupApp"` on `<application>`.
- `app/src/main/java/com/example/smatchup/YouTubeApi.kt` — refactored, moved to `data/api/`.

### Untouched in this sub-project

`MainActivity.kt`, `MainViewModel.kt`, `ui/theme/**`, Compose UI. Sub-project 2 will wire those into the foundation.

---

## Conventions

- **Branch:** create a new branch `data-foundation` off `api` before Task 1. Stay on it until the sub-project merges.
- **Commit style:** Conventional Commits (`feat:`, `test:`, `chore:`, `refactor:`). Each task = at least one commit.
- **TDD loop:** write failing test → run → write code → run → commit.
- **DRY:** when several DAO tests follow the same pattern, the plan shows the full code once and points to the pattern thereafter.
- **Encoding:** all files UTF-8 LF. Git may convert to CRLF on Windows — accept the warning.

---

## Task 1: Gradle setup (KSP, Room, test deps)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Branch.**

  ```bash
  git checkout -b data-foundation
  ```

- [ ] **Step 2: Edit `gradle/libs.versions.toml`.** Replace entire file with:

  ```toml
  [versions]
  agp = "9.2.1"
  coreKtx = "1.18.0"
  junit = "4.13.2"
  junitVersion = "1.3.0"
  espressoCore = "3.7.0"
  lifecycleRuntimeKtx = "2.10.0"
  activityCompose = "1.13.0"
  kotlin = "2.2.10"
  ksp = "2.2.10-2.0.2"
  composeBom = "2024.09.00"
  okhttp = "4.12.0"
  kotlinxCoroutines = "1.9.0"
  lifecycleViewmodelCompose = "2.10.0"
  room = "2.7.0"
  turbine = "1.1.0"
  androidxTest = "1.6.1"
  androidxTestRunner = "1.6.2"
  androidxTestRules = "1.6.1"

  [libraries]
  androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
  junit = { group = "junit", name = "junit", version.ref = "junit" }
  androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
  androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
  androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
  androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
  androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
  androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
  androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
  androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
  androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
  androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
  androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
  androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
  androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
  okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
  okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
  okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
  kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
  kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
  room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
  room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
  room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
  room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
  turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
  androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTest" }
  androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTestRunner" }
  androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTestRules" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
  ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
  ```

- [ ] **Step 3: Edit root `build.gradle.kts`.** Replace with:

  ```kotlin
  plugins {
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.kotlin.compose) apply false
      alias(libs.plugins.ksp) apply false
  }
  ```

- [ ] **Step 4: Edit `app/build.gradle.kts`.** Add the KSP plugin, `START_GG_TOKEN` injection, Room schema dir, and the new dependencies. Replace the file with:

  ```kotlin
  import java.util.Properties

  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.compose)
      alias(libs.plugins.ksp)
  }

  val localProps = Properties().apply {
      val f = rootProject.file("local.properties")
      if (f.exists()) f.inputStream().use { load(it) }
  }
  val youtubeApiKey: String = localProps.getProperty("YOUTUBE_API_KEY") ?: ""
  val startGgToken: String = localProps.getProperty("START_GG_TOKEN") ?: ""

  android {
      namespace = "com.example.smatchup"
      compileSdk {
          version = release(36) {
              minorApiLevel = 1
          }
      }

      defaultConfig {
          applicationId = "com.example.smatchup"
          minSdk = 24
          targetSdk = 36
          versionCode = 1
          versionName = "1.0"

          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

          buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
          buildConfigField("String", "START_GG_TOKEN", "\"$startGgToken\"")
      }

      buildTypes {
          release {
              isMinifyEnabled = false
              proguardFiles(
                  getDefaultProguardFile("proguard-android-optimize.txt"),
                  "proguard-rules.pro"
              )
          }
      }
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_11
          targetCompatibility = JavaVersion.VERSION_11
      }
      buildFeatures {
          compose = true
          buildConfig = true
      }
  }

  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
  }

  dependencies {
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.lifecycle.runtime.ktx)
      implementation(libs.androidx.lifecycle.viewmodel.compose)
      implementation(libs.androidx.activity.compose)
      implementation(platform(libs.androidx.compose.bom))
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)
      implementation(libs.androidx.compose.ui.tooling.preview)
      implementation(libs.androidx.compose.material3)
      implementation(libs.kotlinx.coroutines.android)
      implementation(libs.okhttp)
      implementation(libs.okhttp.logging)
      implementation(libs.room.runtime)
      implementation(libs.room.ktx)
      ksp(libs.room.compiler)

      testImplementation(libs.junit)
      testImplementation(libs.kotlinx.coroutines.test)
      testImplementation(libs.okhttp.mockwebserver)
      testImplementation(libs.turbine)

      androidTestImplementation(libs.androidx.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(libs.androidx.test.core)
      androidTestImplementation(libs.androidx.test.runner)
      androidTestImplementation(libs.androidx.test.rules)
      androidTestImplementation(libs.kotlinx.coroutines.test)
      androidTestImplementation(libs.room.testing)
      androidTestImplementation(platform(libs.androidx.compose.bom))
      androidTestImplementation(libs.androidx.compose.ui.test.junit4)

      debugImplementation(libs.androidx.compose.ui.tooling)
      debugImplementation(libs.androidx.compose.ui.test.manifest)
  }
  ```

- [ ] **Step 5: Sync and build.**

  ```bash
  ./gradlew help --quiet
  ```

  Expected: `BUILD SUCCESSFUL`. If Gradle complains about KSP version, double-check the `ksp` version in `libs.versions.toml` matches your installed Kotlin (`2.2.10-2.0.2`).

- [ ] **Step 6: Commit.**

  ```bash
  git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
  git commit -m "chore: add Room/KSP and data-layer test dependencies"
  ```

---

## Task 2: `ApiResult` sealed class + tests

**Files:**
- Create: `app/src/main/java/com/example/smatchup/domain/model/ApiResult.kt`
- Test:   `app/src/test/java/com/example/smatchup/domain/model/ApiResultTest.kt`

- [ ] **Step 1: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/domain/model/ApiResultTest.kt
  package com.example.smatchup.domain.model

  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertTrue
  import org.junit.Test

  class ApiResultTest {
      @Test
      fun `map transforms Success payload`() {
          val r: ApiResult<Int> = ApiResult.Success(2)
          val mapped = r.map { it * 3 }
          assertEquals(ApiResult.Success(6), mapped)
      }

      @Test
      fun `map leaves Error untouched`() {
          val r: ApiResult<Int> = ApiResult.Unauthorized
          val mapped = r.map { it * 3 }
          assertTrue(mapped is ApiResult.Unauthorized)
      }

      @Test
      fun `getOrNull returns data on Success and null otherwise`() {
          assertEquals(7, ApiResult.Success(7).getOrNull())
          assertEquals(null, ApiResult.NotFound.getOrNull())
          assertEquals(null, ApiResult.RateLimited(null).getOrNull())
      }
  }
  ```

- [ ] **Step 2: Run, verify FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.domain.model.ApiResultTest"
  ```

  Expected: compilation failure (`ApiResult` does not exist).

- [ ] **Step 3: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/domain/model/ApiResult.kt
  package com.example.smatchup.domain.model

  sealed class ApiResult<out T> {
      data class Success<T>(val data: T) : ApiResult<T>()
      data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
      data class RateLimited(val retryAfter: Long?) : ApiResult<Nothing>()
      object Unauthorized : ApiResult<Nothing>()
      data class ParseError(val msg: String) : ApiResult<Nothing>()
      object NotFound : ApiResult<Nothing>()

      inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
          is Success -> Success(transform(data))
          is NetworkError -> this
          is RateLimited -> this
          is ParseError -> this
          Unauthorized -> Unauthorized
          NotFound -> NotFound
      }

      fun getOrNull(): T? = (this as? Success)?.data
  }
  ```

- [ ] **Step 4: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.domain.model.ApiResultTest"
  ```

  Expected: 3 tests pass.

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/domain/model/ApiResult.kt \
          app/src/test/java/com/example/smatchup/domain/model/ApiResultTest.kt
  git commit -m "feat(domain): add ApiResult sealed class with map/getOrNull"
  ```

---

## Task 3: Domain data classes

No tests (plain Kotlin data classes). Each class is a one-shot creation; commit them together at the end of the task.

**Files (all in `app/src/main/java/com/example/smatchup/domain/model/`):**
- Create: `Character.kt`, `Move.kt`, `Frame.kt`, `Matchup.kt`, `Stage.kt`, `Player.kt`, `TierEntry.kt`

- [ ] **Step 1: Create `Character.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  data class Character(
      val id: String,           // canonical id, lowercase, no spaces, e.g. "steve"
      val name: String,         // display name e.g. "Steve"
      val series: String,       // e.g. "Minecraft"
      val rosterNumber: Int,    // canonical SSBU roster order
      val portraitAsset: String? = null
  )
  ```

- [ ] **Step 2: Create `Frame.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  data class Frame(
      val startup: Int? = null,         // first hitbox frame
      val active: IntRange? = null,     // active hitbox window
      val totalFrames: Int? = null,
      val landingLag: Int? = null,
      val baseDamage: Float? = null,
      val baseKnockback: Float? = null,
      val knockbackGrowth: Float? = null,
      val angle: Int? = null            // launch angle degrees
  )
  ```

- [ ] **Step 3: Create `Move.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  enum class MoveCategory { JAB, TILT, SMASH, AERIAL, SPECIAL, GRAB, THROW, MOVEMENT }

  data class Move(
      val id: String,                   // e.g. "fair", "nair", "neutral_b"
      val displayName: String,
      val category: MoveCategory,
      val frame: Frame,
      val utility: String? = null       // curated description: kill move, combo starter, etc.
  )
  ```

- [ ] **Step 4: Create `Stage.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  enum class StageVerdict { BAN, COUNTERPICK, NEUTRAL }

  data class Stage(
      val id: String,                   // "fd", "bf", "ps2"
      val displayName: String,
      val verdict: StageVerdict = StageVerdict.NEUTRAL
  )
  ```

- [ ] **Step 5: Create `Matchup.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  /**
   * Normalize so that charA.id < charB.id lexicographically.
   * Win rate is from charA's perspective.
   */
  data class Matchup(
      val charA: String,
      val charB: String,
      val gameplanA: List<String> = emptyList(),
      val gameplanB: List<String> = emptyList(),
      val strongMovesA: List<String> = emptyList(),
      val strongMovesB: List<String> = emptyList(),
      val punishableMovesA: List<String> = emptyList(),
      val punishableMovesB: List<String> = emptyList(),
      val stagesForA: List<Stage> = emptyList(),
      val stagesForB: List<Stage> = emptyList(),
      val winRateA: Float? = null,
      val sampleSize: Int = 0,
      val majorsCount: Int = 0
  ) {
      init {
          require(charA <= charB) { "Matchup must be stored with charA <= charB (got $charA / $charB)" }
      }
  }
  ```

- [ ] **Step 6: Create `Player.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  data class Player(
      val tag: String,
      val startGgId: Long? = null,
      val mainCharacter: String? = null,    // character id
      val score: Float = 0f                  // best-player score, computed
  )
  ```

- [ ] **Step 7: Create `TierEntry.kt`.**

  ```kotlin
  package com.example.smatchup.domain.model

  enum class Tier { S_PLUS, S, A, B, C, D, E, F }

  data class TierEntry(val charId: String, val tier: Tier)

  data class TierList(
      val name: String,                 // "strength" or "difficulty"
      val version: String,              // e.g. "UltRank 2026 v4"
      val updatedAt: Long,
      val entries: List<TierEntry>
  )
  ```

- [ ] **Step 8: Build to verify all compile.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/domain/model/
  git commit -m "feat(domain): add Character, Move, Frame, Stage, Matchup, Player, TierEntry"
  ```

---

## Task 4: `PasswordHasher` (PBKDF2)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt`
- Test:   `app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt`

- [ ] **Step 1: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt
  package com.example.smatchup.data.auth

  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertFalse
  import org.junit.Assert.assertNotEquals
  import org.junit.Assert.assertTrue
  import org.junit.Test

  class PasswordHasherTest {

      @Test
      fun `newSalt produces 16 bytes base64 different each call`() {
          val s1 = PasswordHasher.newSalt()
          val s2 = PasswordHasher.newSalt()
          assertNotEquals(s1, s2)
          // base64 of 16 bytes = 24 chars with padding
          assertEquals(24, s1.length)
      }

      @Test
      fun `hash is deterministic for same password and salt`() {
          val salt = PasswordHasher.newSalt()
          val a = PasswordHasher.hash("hunter2", salt)
          val b = PasswordHasher.hash("hunter2", salt)
          assertEquals(a, b)
      }

      @Test
      fun `verify returns true with correct password`() {
          val salt = PasswordHasher.newSalt()
          val h = PasswordHasher.hash("hunter2", salt)
          assertTrue(PasswordHasher.verify("hunter2", salt, h))
      }

      @Test
      fun `verify returns false with wrong password`() {
          val salt = PasswordHasher.newSalt()
          val h = PasswordHasher.hash("hunter2", salt)
          assertFalse(PasswordHasher.verify("hunter3", salt, h))
      }

      @Test
      fun `different salts give different hashes for same password`() {
          val a = PasswordHasher.hash("hunter2", PasswordHasher.newSalt())
          val b = PasswordHasher.hash("hunter2", PasswordHasher.newSalt())
          assertNotEquals(a, b)
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.auth.PasswordHasherTest"
  ```

- [ ] **Step 3: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt
  package com.example.smatchup.data.auth

  import java.security.SecureRandom
  import java.util.Base64
  import javax.crypto.SecretKeyFactory
  import javax.crypto.spec.PBEKeySpec

  object PasswordHasher {

      private const val ALGORITHM = "PBKDF2WithHmacSHA256"
      private const val ITERATIONS = 120_000
      private const val KEY_LENGTH_BITS = 256
      private const val SALT_BYTES = 16

      private val factory: SecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM)
      private val rng = SecureRandom()
      private val encoder: Base64.Encoder = Base64.getEncoder()
      private val decoder: Base64.Decoder = Base64.getDecoder()

      fun newSalt(): String {
          val bytes = ByteArray(SALT_BYTES)
          rng.nextBytes(bytes)
          return encoder.encodeToString(bytes)
      }

      fun hash(password: String, saltBase64: String): String {
          val saltBytes = decoder.decode(saltBase64)
          val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
          val key = factory.generateSecret(spec)
          return encoder.encodeToString(key.encoded)
      }

      fun verify(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
          val computed = hash(password, saltBase64).toByteArray()
          val expected = expectedHashBase64.toByteArray()
          if (computed.size != expected.size) return false
          var diff = 0
          for (i in computed.indices) diff = diff or (computed[i].toInt() xor expected[i].toInt())
          return diff == 0
      }
  }
  ```

- [ ] **Step 4: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.auth.PasswordHasherTest"
  ```

  Expected: 5 tests pass.

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/auth/PasswordHasher.kt \
          app/src/test/java/com/example/smatchup/data/auth/PasswordHasherTest.kt
  git commit -m "feat(auth): add PBKDF2 PasswordHasher with constant-time verify"
  ```

---

## Task 5: `Clock` + `CacheTtl`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/cache/Clock.kt`
- Create: `app/src/main/java/com/example/smatchup/data/cache/CacheTtl.kt`

(No tests — `Clock` is a one-method interface, `CacheTtl` is constants.)

- [ ] **Step 1: Create `Clock.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/cache/Clock.kt
  package com.example.smatchup.data.cache

  fun interface Clock {
      fun nowMillis(): Long
  }

  object SystemClock : Clock {
      override fun nowMillis(): Long = System.currentTimeMillis()
  }
  ```

- [ ] **Step 2: Create `CacheTtl.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/cache/CacheTtl.kt
  package com.example.smatchup.data.cache

  object CacheTtl {
      const val FRAMEDATA_MS    = 30L * 24L * 3600L * 1000L
      const val WINRATE_MS      = 7L  * 24L * 3600L * 1000L
      const val BEST_PLAYER_MS  = 60L * 24L * 3600L * 1000L
      const val YT_VIDEO_MS     = 1L  * 24L * 3600L * 1000L
  }
  ```

- [ ] **Step 3: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/cache/Clock.kt \
          app/src/main/java/com/example/smatchup/data/cache/CacheTtl.kt
  git commit -m "feat(cache): add Clock abstraction and CacheTtl constants"
  ```

---

## Task 6: `CacheManager`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/cache/CacheManager.kt`
- Test:   `app/src/test/java/com/example/smatchup/data/cache/CacheManagerTest.kt`

- [ ] **Step 1: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/cache/CacheManagerTest.kt
  package com.example.smatchup.data.cache

  import org.junit.Assert.assertFalse
  import org.junit.Assert.assertTrue
  import org.junit.Test

  class CacheManagerTest {

      @Test
      fun `isFresh true when within TTL`() {
          val clock = Clock { 1_000_000L }
          val mgr = CacheManager(clock)
          assertTrue(mgr.isFresh(fetchedAt = 999_500L, ttlMs = 1_000L))
      }

      @Test
      fun `isFresh false when past TTL`() {
          val clock = Clock { 1_000_000L }
          val mgr = CacheManager(clock)
          assertFalse(mgr.isFresh(fetchedAt = 998_999L, ttlMs = 1_000L))
      }

      @Test
      fun `isFresh false when fetchedAt is in the future (clock skew)`() {
          val clock = Clock { 1_000_000L }
          val mgr = CacheManager(clock)
          assertFalse(mgr.isFresh(fetchedAt = 2_000_000L, ttlMs = 1_000L))
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.cache.CacheManagerTest"
  ```

- [ ] **Step 3: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/cache/CacheManager.kt
  package com.example.smatchup.data.cache

  class CacheManager(private val clock: Clock = SystemClock) {

      fun isFresh(fetchedAt: Long, ttlMs: Long): Boolean {
          val now = clock.nowMillis()
          if (fetchedAt > now) return false
          return now - fetchedAt < ttlMs
      }

      fun shouldRefresh(fetchedAt: Long, ttlMs: Long): Boolean = !isFresh(fetchedAt, ttlMs)
  }
  ```

- [ ] **Step 4: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.cache.CacheManagerTest"
  ```

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/cache/CacheManager.kt \
          app/src/test/java/com/example/smatchup/data/cache/CacheManagerTest.kt
  git commit -m "feat(cache): add CacheManager with isFresh/shouldRefresh"
  ```

---

## Task 7: `RateLimiter` (token bucket 80/60 s)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/cache/RateLimiter.kt`
- Test:   `app/src/test/java/com/example/smatchup/data/cache/RateLimiterTest.kt`

- [ ] **Step 1: Write the failing test (uses coroutines-test).**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/cache/RateLimiterTest.kt
  package com.example.smatchup.data.cache

  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.advanceTimeBy
  import kotlinx.coroutines.test.runTest
  import org.junit.Assert.assertEquals
  import org.junit.Test

  @OptIn(ExperimentalCoroutinesApi::class)
  class RateLimiterTest {

      @Test
      fun `allows up to capacity within window`() = runTest {
          val clock = Clock { currentTime }
          val limiter = RateLimiter(capacity = 3, windowMs = 1_000L, clock = clock)
          repeat(3) { limiter.acquire() }
          // 3 acquires within window: instant; no virtual time advance from acquire itself
          assertEquals(0L, currentTime)
      }

      @Test
      fun `suspends until window slides when bucket empty`() = runTest {
          val clock = Clock { currentTime }
          val limiter = RateLimiter(capacity = 2, windowMs = 1_000L, clock = clock)
          limiter.acquire()                  // t=0
          advanceTimeBy(200)                 // t=200
          limiter.acquire()                  // t=200
          // 3rd should wait until t=1000 (the first permit's window expires)
          advanceTimeBy(800)                 // t=1000
          limiter.acquire()
          assertEquals(1_000L, currentTime)
      }
  }
  ```

- [ ] **Step 2: Run, expect FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.cache.RateLimiterTest"
  ```

- [ ] **Step 3: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/cache/RateLimiter.kt
  package com.example.smatchup.data.cache

  import kotlinx.coroutines.delay
  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import java.util.ArrayDeque

  /**
   * Sliding-window token bucket. Each acquire() consumes one permit; permits expire after windowMs.
   * Suspends until a permit is available. Thread-safe via Mutex.
   */
  class RateLimiter(
      private val capacity: Int,
      private val windowMs: Long,
      private val clock: Clock,
  ) {
      private val mutex = Mutex()
      private val timestamps = ArrayDeque<Long>(capacity)

      suspend fun acquire() {
          while (true) {
              val waitMs = mutex.withLock {
                  val now = clock.nowMillis()
                  // drop expired timestamps
                  while (timestamps.isNotEmpty() && now - timestamps.peekFirst() >= windowMs) {
                      timestamps.pollFirst()
                  }
                  if (timestamps.size < capacity) {
                      timestamps.addLast(now)
                      0L
                  } else {
                      val oldest = timestamps.peekFirst()
                      windowMs - (now - oldest)
                  }
              }
              if (waitMs <= 0L) return
              delay(waitMs)
          }
      }
  }
  ```

- [ ] **Step 4: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.cache.RateLimiterTest"
  ```

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/cache/RateLimiter.kt \
          app/src/test/java/com/example/smatchup/data/cache/RateLimiterTest.kt
  git commit -m "feat(cache): add sliding-window RateLimiter token bucket"
  ```

---

## Task 8: `HttpClientProvider`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/api/HttpClientProvider.kt`

(No test — straight wiring; the real test is when the API clients use it in their own tests.)

- [ ] **Step 1: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/api/HttpClientProvider.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.BuildConfig
  import okhttp3.OkHttpClient
  import okhttp3.logging.HttpLoggingInterceptor
  import java.util.concurrent.TimeUnit

  object HttpClientProvider {

      val client: OkHttpClient by lazy { build() }

      private fun build(): OkHttpClient = OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(10, TimeUnit.SECONDS)
          .addInterceptor { chain ->
              val req = chain.request().newBuilder()
                  .header("User-Agent", "Smatchup/1.0")
                  .build()
              chain.proceed(req)
          }
          .apply {
              if (BuildConfig.DEBUG) {
                  addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
              }
          }
          .build()
  }
  ```

- [ ] **Step 2: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/HttpClientProvider.kt
  git commit -m "feat(api): add HttpClientProvider singleton with UA + debug logging"
  ```

---

## Task 9: Room entities + database

**Files:**
- Create: 8 entity files under `app/src/main/java/com/example/smatchup/data/local/entity/`
- Create: `app/src/main/java/com/example/smatchup/data/local/SmatchupDatabase.kt` (without DAOs yet — DAOs come in Task 10)

- [ ] **Step 1: Create `UserEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/UserEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.ColumnInfo
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey

  @Entity(
      tableName = "users",
      indices = [Index(value = ["pseudo"], unique = true), Index(value = ["email"], unique = true)]
  )
  data class UserEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      @ColumnInfo(name = "pseudo") val pseudo: String,
      @ColumnInfo(name = "email") val email: String,
      @ColumnInfo(name = "passwordHash") val passwordHash: String,
      @ColumnInfo(name = "salt") val salt: String,
      @ColumnInfo(name = "createdAt") val createdAt: Long
  )
  ```

- [ ] **Step 2: Create `FavoriteCharacterEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/FavoriteCharacterEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.ForeignKey

  @Entity(
      tableName = "favorite_characters",
      primaryKeys = ["userId", "charId"],
      foreignKeys = [ForeignKey(
          entity = UserEntity::class,
          parentColumns = ["id"],
          childColumns = ["userId"],
          onDelete = ForeignKey.CASCADE
      )]
  )
  data class FavoriteCharacterEntity(
      val userId: Long,
      val charId: String,
      val addedAt: Long
  )
  ```

- [ ] **Step 3: Create `FavoriteMatchupEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/FavoriteMatchupEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.ForeignKey

  @Entity(
      tableName = "favorite_matchups",
      primaryKeys = ["userId", "charA", "charB"],
      foreignKeys = [ForeignKey(
          entity = UserEntity::class,
          parentColumns = ["id"],
          childColumns = ["userId"],
          onDelete = ForeignKey.CASCADE
      )]
  )
  data class FavoriteMatchupEntity(
      val userId: Long,
      val charA: String,        // normalized: charA <= charB lexicographically
      val charB: String,
      val addedAt: Long
  )
  ```

- [ ] **Step 4: Create `CachedFramedataEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/CachedFramedataEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.PrimaryKey

  @Entity(tableName = "cached_framedata")
  data class CachedFramedataEntity(
      @PrimaryKey val charId: String,
      val jsonBlob: String,
      val fetchedAt: Long
  )
  ```

- [ ] **Step 5: Create `CachedWinrateEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/CachedWinrateEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity

  @Entity(tableName = "cached_winrate", primaryKeys = ["charA", "charB"])
  data class CachedWinrateEntity(
      val charA: String,
      val charB: String,
      val winRateA: Float,
      val sampleSize: Int,
      val majorsCount: Int,
      val computedAt: Long
  )
  ```

- [ ] **Step 6: Create `CachedBestPlayerEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/CachedBestPlayerEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.PrimaryKey

  @Entity(tableName = "cached_best_players")
  data class CachedBestPlayerEntity(
      @PrimaryKey val charId: String,
      val playerTag: String,
      val startGgPlayerId: Long?,
      val score: Float,
      val computedAt: Long
  )
  ```

- [ ] **Step 7: Create `CachedYoutubeVideoEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/CachedYoutubeVideoEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.PrimaryKey

  @Entity(tableName = "cached_youtube_video")
  data class CachedYoutubeVideoEntity(
      @PrimaryKey val cacheKey: String,       // "char:<id>" or "mu:<a>_<b>"
      val videoId: String,
      val title: String,
      val publishedAt: Long,
      val fetchedAt: Long
  )
  ```

- [ ] **Step 8: Create `SessionEntity.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/entity/SessionEntity.kt
  package com.example.smatchup.data.local.entity

  import androidx.room.Entity
  import androidx.room.PrimaryKey

  @Entity(tableName = "session")
  data class SessionEntity(
      @PrimaryKey val id: Int = 0,            // singleton row
      val userId: Long?,
      val updatedAt: Long
  )
  ```

- [ ] **Step 9: Create `SmatchupDatabase.kt` (without DAOs yet — they'll be wired up in Task 10).**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/SmatchupDatabase.kt
  package com.example.smatchup.data.local

  import androidx.room.Database
  import androidx.room.RoomDatabase
  import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
  import com.example.smatchup.data.local.entity.CachedFramedataEntity
  import com.example.smatchup.data.local.entity.CachedWinrateEntity
  import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity
  import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
  import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
  import com.example.smatchup.data.local.entity.SessionEntity
  import com.example.smatchup.data.local.entity.UserEntity

  @Database(
      entities = [
          UserEntity::class,
          FavoriteCharacterEntity::class,
          FavoriteMatchupEntity::class,
          CachedFramedataEntity::class,
          CachedWinrateEntity::class,
          CachedBestPlayerEntity::class,
          CachedYoutubeVideoEntity::class,
          SessionEntity::class
      ],
      version = 1,
      exportSchema = true
  )
  abstract class SmatchupDatabase : RoomDatabase() {
      // DAOs added in Task 10
  }
  ```

- [ ] **Step 10: Build to make sure Room generates the implementation.**

  ```bash
  ./gradlew :app:compileDebugKotlin :app:kspDebugKotlin
  ```

  Expected: `BUILD SUCCESSFUL`. A `schemas/com.example.smatchup.data.local.SmatchupDatabase/1.json` file should appear.

- [ ] **Step 11: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/local/ app/schemas/
  git commit -m "feat(local): add Room entities and SmatchupDatabase v1"
  ```

---

## Task 10: DAOs + instrumented tests

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/local/dao/UserDao.kt`
- Create: `app/src/main/java/com/example/smatchup/data/local/dao/FavoritesDao.kt`
- Create: `app/src/main/java/com/example/smatchup/data/local/dao/CacheDao.kt`
- Create: `app/src/main/java/com/example/smatchup/data/local/dao/SessionDao.kt`
- Modify: `app/src/main/java/com/example/smatchup/data/local/SmatchupDatabase.kt` (add abstract DAO accessors)
- Test:   `app/src/androidTest/java/com/example/smatchup/data/local/UserDaoTest.kt`
- Test:   `app/src/androidTest/java/com/example/smatchup/data/local/FavoritesDaoTest.kt`
- Test:   `app/src/androidTest/java/com/example/smatchup/data/local/CacheDaoTest.kt`
- Test:   `app/src/androidTest/java/com/example/smatchup/data/local/SessionDaoTest.kt`

This task requires a connected device or emulator. Boot one (or use Android Studio's AVD) before running `connectedAndroidTest`.

- [ ] **Step 1: Create `UserDao.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/dao/UserDao.kt
  package com.example.smatchup.data.local.dao

  import androidx.room.Dao
  import androidx.room.Insert
  import androidx.room.OnConflictStrategy
  import androidx.room.Query
  import com.example.smatchup.data.local.entity.UserEntity

  @Dao
  interface UserDao {
      @Insert(onConflict = OnConflictStrategy.ABORT)
      suspend fun insert(user: UserEntity): Long

      @Query("SELECT * FROM users WHERE pseudo = :pseudo LIMIT 1")
      suspend fun findByPseudo(pseudo: String): UserEntity?

      @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
      suspend fun findByEmail(email: String): UserEntity?

      @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
      suspend fun findById(id: Long): UserEntity?
  }
  ```

- [ ] **Step 2: Create `FavoritesDao.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/dao/FavoritesDao.kt
  package com.example.smatchup.data.local.dao

  import androidx.room.Dao
  import androidx.room.Insert
  import androidx.room.OnConflictStrategy
  import androidx.room.Query
  import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
  import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
  import kotlinx.coroutines.flow.Flow

  @Dao
  interface FavoritesDao {

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun addCharacter(fav: FavoriteCharacterEntity)

      @Query("DELETE FROM favorite_characters WHERE userId = :userId AND charId = :charId")
      suspend fun removeCharacter(userId: Long, charId: String)

      @Query("SELECT * FROM favorite_characters WHERE userId = :userId ORDER BY addedAt DESC")
      fun observeCharacters(userId: Long): Flow<List<FavoriteCharacterEntity>>

      @Query("""
          SELECT EXISTS(SELECT 1 FROM favorite_characters
                       WHERE userId = :userId AND charId = :charId)
      """)
      suspend fun isCharacterFavorite(userId: Long, charId: String): Boolean

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun addMatchup(fav: FavoriteMatchupEntity)

      @Query("""
          DELETE FROM favorite_matchups
          WHERE userId = :userId AND charA = :charA AND charB = :charB
      """)
      suspend fun removeMatchup(userId: Long, charA: String, charB: String)

      @Query("SELECT * FROM favorite_matchups WHERE userId = :userId ORDER BY addedAt DESC")
      fun observeMatchups(userId: Long): Flow<List<FavoriteMatchupEntity>>

      @Query("""
          SELECT EXISTS(SELECT 1 FROM favorite_matchups
                       WHERE userId = :userId AND charA = :charA AND charB = :charB)
      """)
      suspend fun isMatchupFavorite(userId: Long, charA: String, charB: String): Boolean
  }
  ```

- [ ] **Step 3: Create `CacheDao.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/dao/CacheDao.kt
  package com.example.smatchup.data.local.dao

  import androidx.room.Dao
  import androidx.room.Insert
  import androidx.room.OnConflictStrategy
  import androidx.room.Query
  import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
  import com.example.smatchup.data.local.entity.CachedFramedataEntity
  import com.example.smatchup.data.local.entity.CachedWinrateEntity
  import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity

  @Dao
  interface CacheDao {

      // --- framedata ---
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsertFramedata(entity: CachedFramedataEntity)

      @Query("SELECT * FROM cached_framedata WHERE charId = :charId LIMIT 1")
      suspend fun getFramedata(charId: String): CachedFramedataEntity?

      // --- winrate ---
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsertWinrate(entity: CachedWinrateEntity)

      @Query("SELECT * FROM cached_winrate WHERE charA = :a AND charB = :b LIMIT 1")
      suspend fun getWinrate(a: String, b: String): CachedWinrateEntity?

      // --- best players ---
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsertBestPlayer(entity: CachedBestPlayerEntity)

      @Query("SELECT * FROM cached_best_players")
      suspend fun getAllBestPlayers(): List<CachedBestPlayerEntity>

      @Query("SELECT * FROM cached_best_players WHERE charId = :charId LIMIT 1")
      suspend fun getBestPlayer(charId: String): CachedBestPlayerEntity?

      // --- youtube ---
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsertVideo(entity: CachedYoutubeVideoEntity)

      @Query("SELECT * FROM cached_youtube_video WHERE cacheKey = :key LIMIT 1")
      suspend fun getVideo(key: String): CachedYoutubeVideoEntity?
  }
  ```

- [ ] **Step 4: Create `SessionDao.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/local/dao/SessionDao.kt
  package com.example.smatchup.data.local.dao

  import androidx.room.Dao
  import androidx.room.Insert
  import androidx.room.OnConflictStrategy
  import androidx.room.Query
  import com.example.smatchup.data.local.entity.SessionEntity
  import kotlinx.coroutines.flow.Flow

  @Dao
  interface SessionDao {

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsert(session: SessionEntity)

      @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")
      suspend fun get(): SessionEntity?

      @Query("SELECT * FROM session WHERE id = 0 LIMIT 1")
      fun observe(): Flow<SessionEntity?>
  }
  ```

- [ ] **Step 5: Wire DAOs into `SmatchupDatabase.kt`.** Replace file with:

  ```kotlin
  package com.example.smatchup.data.local

  import androidx.room.Database
  import androidx.room.RoomDatabase
  import com.example.smatchup.data.local.dao.CacheDao
  import com.example.smatchup.data.local.dao.FavoritesDao
  import com.example.smatchup.data.local.dao.SessionDao
  import com.example.smatchup.data.local.dao.UserDao
  import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
  import com.example.smatchup.data.local.entity.CachedFramedataEntity
  import com.example.smatchup.data.local.entity.CachedWinrateEntity
  import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity
  import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
  import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
  import com.example.smatchup.data.local.entity.SessionEntity
  import com.example.smatchup.data.local.entity.UserEntity

  @Database(
      entities = [
          UserEntity::class,
          FavoriteCharacterEntity::class,
          FavoriteMatchupEntity::class,
          CachedFramedataEntity::class,
          CachedWinrateEntity::class,
          CachedBestPlayerEntity::class,
          CachedYoutubeVideoEntity::class,
          SessionEntity::class
      ],
      version = 1,
      exportSchema = true
  )
  abstract class SmatchupDatabase : RoomDatabase() {
      abstract fun userDao(): UserDao
      abstract fun favoritesDao(): FavoritesDao
      abstract fun cacheDao(): CacheDao
      abstract fun sessionDao(): SessionDao
  }
  ```

- [ ] **Step 6: Write failing test `UserDaoTest.kt`.**

  ```kotlin
  // app/src/androidTest/java/com/example/smatchup/data/local/UserDaoTest.kt
  package com.example.smatchup.data.local

  import androidx.room.Room
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.example.smatchup.data.local.entity.UserEntity
  import kotlinx.coroutines.test.runTest
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNotEquals
  import org.junit.Assert.assertNull
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class UserDaoTest {

      private lateinit var db: SmatchupDatabase

      @Before fun setup() {
          val ctx = InstrumentationRegistry.getInstrumentation().context
          db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
              .allowMainThreadQueries()
              .build()
      }

      @After fun teardown() { db.close() }

      @Test fun insertAndFindByPseudo() = runTest {
          val dao = db.userDao()
          val id = dao.insert(UserEntity(pseudo = "lode", email = "l@x.io", passwordHash = "h", salt = "s", createdAt = 1))
          assertNotEquals(0L, id)
          val u = dao.findByPseudo("lode")
          assertEquals("l@x.io", u?.email)
      }

      @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
      fun duplicatePseudoFails() = runTest {
          val dao = db.userDao()
          dao.insert(UserEntity(pseudo = "a", email = "a1@x", passwordHash = "h", salt = "s", createdAt = 0))
          dao.insert(UserEntity(pseudo = "a", email = "a2@x", passwordHash = "h", salt = "s", createdAt = 0))
      }

      @Test fun findUnknownReturnsNull() = runTest {
          assertNull(db.userDao().findByPseudo("nobody"))
      }
  }
  ```

- [ ] **Step 7: Write `FavoritesDaoTest.kt`.**

  ```kotlin
  // app/src/androidTest/java/com/example/smatchup/data/local/FavoritesDaoTest.kt
  package com.example.smatchup.data.local

  import androidx.room.Room
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import app.cash.turbine.test
  import com.example.smatchup.data.local.entity.FavoriteCharacterEntity
  import com.example.smatchup.data.local.entity.FavoriteMatchupEntity
  import com.example.smatchup.data.local.entity.UserEntity
  import kotlinx.coroutines.test.runTest
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertFalse
  import org.junit.Assert.assertTrue
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class FavoritesDaoTest {

      private lateinit var db: SmatchupDatabase
      private var userId: Long = 0

      @Before fun setup() = runTest {
          val ctx = InstrumentationRegistry.getInstrumentation().context
          db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
              .allowMainThreadQueries().build()
          userId = db.userDao().insert(UserEntity(pseudo = "u", email = "u@x", passwordHash = "h", salt = "s", createdAt = 0))
      }
      @After fun teardown() { db.close() }

      @Test fun addAndObserveCharacters() = runTest {
          val dao = db.favoritesDao()
          dao.observeCharacters(userId).test {
              assertEquals(emptyList<FavoriteCharacterEntity>(), awaitItem())
              dao.addCharacter(FavoriteCharacterEntity(userId, "steve", 1L))
              val list = awaitItem()
              assertEquals(1, list.size)
              assertEquals("steve", list[0].charId)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test fun removeCharacterTogglesFlag() = runTest {
          val dao = db.favoritesDao()
          dao.addCharacter(FavoriteCharacterEntity(userId, "steve", 1L))
          assertTrue(dao.isCharacterFavorite(userId, "steve"))
          dao.removeCharacter(userId, "steve")
          assertFalse(dao.isCharacterFavorite(userId, "steve"))
      }

      @Test fun matchupCrud() = runTest {
          val dao = db.favoritesDao()
          dao.addMatchup(FavoriteMatchupEntity(userId, "sonic", "steve", 1L))
          assertTrue(dao.isMatchupFavorite(userId, "sonic", "steve"))
          dao.removeMatchup(userId, "sonic", "steve")
          assertFalse(dao.isMatchupFavorite(userId, "sonic", "steve"))
      }
  }
  ```

- [ ] **Step 8: Write `CacheDaoTest.kt`.**

  ```kotlin
  // app/src/androidTest/java/com/example/smatchup/data/local/CacheDaoTest.kt
  package com.example.smatchup.data.local

  import androidx.room.Room
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.example.smatchup.data.local.entity.CachedBestPlayerEntity
  import com.example.smatchup.data.local.entity.CachedFramedataEntity
  import com.example.smatchup.data.local.entity.CachedWinrateEntity
  import com.example.smatchup.data.local.entity.CachedYoutubeVideoEntity
  import kotlinx.coroutines.test.runTest
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNotNull
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class CacheDaoTest {

      private lateinit var db: SmatchupDatabase

      @Before fun setup() {
          val ctx = InstrumentationRegistry.getInstrumentation().context
          db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
              .allowMainThreadQueries().build()
      }
      @After fun teardown() { db.close() }

      @Test fun framedataUpsert() = runTest {
          val dao = db.cacheDao()
          dao.upsertFramedata(CachedFramedataEntity("steve", "{}", 1L))
          assertEquals("{}", dao.getFramedata("steve")?.jsonBlob)
          dao.upsertFramedata(CachedFramedataEntity("steve", "{\"x\":1}", 2L))
          assertEquals(2L, dao.getFramedata("steve")?.fetchedAt)
      }

      @Test fun winrateRoundTrip() = runTest {
          val dao = db.cacheDao()
          dao.upsertWinrate(CachedWinrateEntity("sonic", "steve", 0.42f, 100, 10, 1L))
          val w = dao.getWinrate("sonic", "steve")
          assertNotNull(w)
          assertEquals(0.42f, w!!.winRateA, 0.0001f)
      }

      @Test fun bestPlayersListed() = runTest {
          val dao = db.cacheDao()
          dao.upsertBestPlayer(CachedBestPlayerEntity("steve", "Onin", 1L, 100f, 1L))
          dao.upsertBestPlayer(CachedBestPlayerEntity("sonic", "Sonix", 2L, 90f, 1L))
          assertEquals(2, dao.getAllBestPlayers().size)
      }

      @Test fun youtubeVideoStored() = runTest {
          val dao = db.cacheDao()
          dao.upsertVideo(CachedYoutubeVideoEntity("char:steve", "abc", "title", 0L, 1L))
          assertEquals("abc", dao.getVideo("char:steve")?.videoId)
      }
  }
  ```

- [ ] **Step 9: Write `SessionDaoTest.kt`.**

  ```kotlin
  // app/src/androidTest/java/com/example/smatchup/data/local/SessionDaoTest.kt
  package com.example.smatchup.data.local

  import androidx.room.Room
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import app.cash.turbine.test
  import com.example.smatchup.data.local.entity.SessionEntity
  import kotlinx.coroutines.test.runTest
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNull
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class SessionDaoTest {

      private lateinit var db: SmatchupDatabase

      @Before fun setup() {
          val ctx = InstrumentationRegistry.getInstrumentation().context
          db = Room.inMemoryDatabaseBuilder(ctx, SmatchupDatabase::class.java)
              .allowMainThreadQueries().build()
      }
      @After fun teardown() { db.close() }

      @Test fun initiallyAbsent() = runTest {
          assertNull(db.sessionDao().get())
      }

      @Test fun upsertReplacesSingletonRow() = runTest {
          val dao = db.sessionDao()
          dao.upsert(SessionEntity(userId = 5, updatedAt = 1L))
          assertEquals(5L, dao.get()?.userId)
          dao.upsert(SessionEntity(userId = null, updatedAt = 2L))
          assertNull(dao.get()?.userId)
      }

      @Test fun observeEmitsUpdates() = runTest {
          val dao = db.sessionDao()
          dao.observe().test {
              assertNull(awaitItem())
              dao.upsert(SessionEntity(userId = 7, updatedAt = 1L))
              assertEquals(7L, awaitItem()?.userId)
              cancelAndIgnoreRemainingEvents()
          }
      }
  }
  ```

- [ ] **Step 10: Boot the emulator/device, then run instrumented tests.**

  ```bash
  ./gradlew :app:connectedDebugAndroidTest --tests "com.example.smatchup.data.local.*"
  ```

  Expected: all DAO tests pass.

- [ ] **Step 11: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/local/dao/ \
          app/src/main/java/com/example/smatchup/data/local/SmatchupDatabase.kt \
          app/src/androidTest/java/com/example/smatchup/data/local/
  git commit -m "feat(local): add DAOs (User, Favorites, Cache, Session) with instrumented tests"
  ```

---

## Task 11: `JsonAssetLoader` + seed bundled JSON

**Files:**
- Create: `app/src/main/assets/characters_meta/steve.json`, `sonic.json`, `snake.json`, `fox.json`, `sephiroth.json`
- Create: `app/src/main/assets/synergies.json`, `tierlist_strength.json`, `tierlist_difficulty.json`, `majors_slugs.json`, `seed_best_players.json`
- Create: `app/src/main/assets/framedata/.gitkeep`, `app/src/main/assets/stages/.gitkeep`, `app/src/main/assets/matchups/.gitkeep`
- Create: `app/src/main/java/com/example/smatchup/data/assets/JsonAssetLoader.kt`
- Test:   `app/src/androidTest/java/com/example/smatchup/data/assets/JsonAssetLoaderTest.kt`

- [ ] **Step 1: Create the seed asset files.** Use these contents (verify each is valid JSON):

  `app/src/main/assets/characters_meta/steve.json`:
  ```json
  {
    "id": "steve",
    "name": "Steve",
    "series": "Minecraft",
    "combos": [
      "Block pillar → up-air → up-special (kill confirm with iron)",
      "Down-tilt → up-air at low %",
      "Side-B (iron axe) → reads"
    ],
    "gameplan": "Mine resources behind blocks, wall the opponent out with side-B, anvil for raw kills.",
    "moves_utility": {
      "fair": "Combo extender; safe on shield with TNT.",
      "uair": "Combo finisher, kills at high %.",
      "side_b": "Iron axe = ranged disjoint; gold = fast but weak.",
      "anvil": "Raw kill option from above, callouts only."
    }
  }
  ```

  `app/src/main/assets/characters_meta/sonic.json`:
  ```json
  {
    "id": "sonic",
    "name": "Sonic",
    "series": "Sonic the Hedgehog",
    "combos": [
      "Up-throw → up-air at low %",
      "Down-throw → fair",
      "Spindash (B) → reads into bair"
    ],
    "gameplan": "Snowball lead with speed, condition with spindash, bair for kill.",
    "moves_utility": {
      "bair": "Primary kill move.",
      "spindash": "Whiff bait; do NOT throw out raw vs grounded opponents.",
      "uair": "Combo extender, juggle tool."
    }
  }
  ```

  `app/src/main/assets/characters_meta/snake.json`:
  ```json
  {
    "id": "snake",
    "name": "Snake",
    "series": "Metal Gear",
    "combos": [
      "Down-throw → up-tilt at low %",
      "Grenade pickup → tilt or smash",
      "Nikita → ledge punish"
    ],
    "gameplan": "Stage-control with grenades + C4. Force approach with explosives. Up-tilt for kill.",
    "moves_utility": {
      "utilt": "Primary kill move, kills very early.",
      "ftilt": "Two-hit jab-like poke.",
      "side_b": "Nikita; remote-piloted recovery harass."
    }
  }
  ```

  `app/src/main/assets/characters_meta/fox.json`:
  ```json
  {
    "id": "fox",
    "name": "Fox",
    "series": "Star Fox",
    "combos": [
      "Drag-down nair → up-tilt → up-air",
      "Up-throw → up-air",
      "Shine → grab / shine → up-smash"
    ],
    "gameplan": "Apply constant pressure with frame-1 nair and lasers. Convert any opening to a kill ladder.",
    "moves_utility": {
      "nair": "Frame 4 multi-hit; drag-down for combos.",
      "shine": "Reflector; OoS option.",
      "upsmash": "Kill option after up-throw / shine."
    }
  }
  ```

  `app/src/main/assets/characters_meta/sephiroth.json`:
  ```json
  {
    "id": "sephiroth",
    "name": "Sephiroth",
    "series": "Final Fantasy",
    "combos": [
      "Down-tilt → up-air",
      "Up-throw → up-air (kill confirm at high %)",
      "Side-B (Octaslash) read"
    ],
    "gameplan": "Disjoint wall with Masamune. Stay just out of opponent's range, fish for kills with neutral-B (Flare).",
    "moves_utility": {
      "fair": "Big disjointed swing; safe on shield spaced.",
      "neutral_b": "Flare/Megaflare; can be charged & released for huge damage.",
      "side_b": "Octaslash; mix-up burst movement."
    }
  }
  ```

  `app/src/main/assets/synergies.json`:
  ```json
  {
    "steve": ["sonic", "fox"],
    "sonic": ["steve", "snake"],
    "snake": ["sonic", "sephiroth"],
    "fox": ["steve", "sephiroth"],
    "sephiroth": ["snake", "fox"]
  }
  ```

  `app/src/main/assets/tierlist_strength.json`:
  ```json
  {
    "name": "strength",
    "version": "UltRank 2026 v4 (seed)",
    "updatedAt": 1746489600000,
    "entries": [
      { "tier": "S_PLUS", "chars": ["steve", "sonic", "snake"] },
      { "tier": "S",      "chars": ["fox", "sephiroth"] }
    ]
  }
  ```

  `app/src/main/assets/tierlist_difficulty.json`:
  ```json
  {
    "name": "difficulty",
    "version": "Game8 2026 (seed)",
    "updatedAt": 1746489600000,
    "entries": [
      { "tier": "S",      "chars": ["fox", "steve"] },
      { "tier": "A",      "chars": ["sephiroth"] },
      { "tier": "B",      "chars": ["sonic", "snake"] }
    ]
  }
  ```

  `app/src/main/assets/majors_slugs.json`:
  ```json
  ["genesis-9", "evo-2024", "tipped-off-14", "ludwig-smash-invitational-2", "riptide-2024"]
  ```

  `app/src/main/assets/seed_best_players.json`:
  ```json
  { "steve": "Onin", "sonic": "Sonix", "snake": "MkLeo", "fox": "Light", "sephiroth": "Sparg0" }
  ```

  Empty placeholders:
  - `app/src/main/assets/framedata/.gitkeep` (empty)
  - `app/src/main/assets/stages/.gitkeep` (empty)
  - `app/src/main/assets/matchups/.gitkeep` (empty)

- [ ] **Step 2: Write the failing test.**

  ```kotlin
  // app/src/androidTest/java/com/example/smatchup/data/assets/JsonAssetLoaderTest.kt
  package com.example.smatchup.data.assets

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNotNull
  import org.junit.Assert.assertNull
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class JsonAssetLoaderTest {

      private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)

      @Test fun characterMetaSteveLoads() {
          val obj = loader.characterMeta("steve")
          assertNotNull(obj)
          assertEquals("Steve", obj!!.getString("name"))
      }

      @Test fun unknownCharacterReturnsNull() {
          assertNull(loader.characterMeta("not-a-real-character"))
      }

      @Test fun synergiesParsed() {
          val obj = loader.synergies()
          assertEquals(listOf("sonic", "fox"), obj.getJSONArray("steve").let {
              List(it.length()) { i -> it.getString(i) }
          })
      }

      @Test fun strengthTierlistParsed() {
          val obj = loader.tierlist("strength")
          assertEquals("strength", obj!!.getString("name"))
      }

      @Test fun majorsSlugsLoad() {
          val arr = loader.majorsSlugs()
          assertEquals(5, arr.length())
      }

      @Test fun seedBestPlayersLoad() {
          val obj = loader.seedBestPlayers()
          assertEquals("Onin", obj.getString("steve"))
      }
  }
  ```

- [ ] **Step 3: Implement `JsonAssetLoader.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/assets/JsonAssetLoader.kt
  package com.example.smatchup.data.assets

  import android.content.Context
  import org.json.JSONArray
  import org.json.JSONObject
  import java.io.IOException

  class JsonAssetLoader(private val context: Context) {

      fun characterMeta(charId: String): JSONObject? =
          loadObject("characters_meta/$charId.json")

      fun framedata(charId: String): JSONObject? =
          loadObject("framedata/$charId.json")

      fun synergies(): JSONObject = loadObject("synergies.json")!!

      fun stages(charId: String): JSONObject? =
          loadObject("stages/$charId.json")

      fun matchup(charA: String, charB: String): JSONObject? {
          val (a, b) = if (charA <= charB) charA to charB else charB to charA
          return loadObject("matchups/${a}_${b}.json")
      }

      fun tierlist(name: String): JSONObject? = loadObject("tierlist_$name.json")

      fun majorsSlugs(): JSONArray = loadArray("majors_slugs.json")!!

      fun seedBestPlayers(): JSONObject = loadObject("seed_best_players.json")!!

      private fun loadObject(path: String): JSONObject? = readString(path)?.let { JSONObject(it) }
      private fun loadArray(path: String): JSONArray? = readString(path)?.let { JSONArray(it) }

      private fun readString(path: String): String? =
          try {
              context.assets.open(path).bufferedReader().use { it.readText() }
          } catch (e: IOException) {
              null
          }
  }
  ```

- [ ] **Step 4: Run instrumented tests.**

  ```bash
  ./gradlew :app:connectedDebugAndroidTest --tests "com.example.smatchup.data.assets.JsonAssetLoaderTest"
  ```

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/assets/ \
          app/src/main/java/com/example/smatchup/data/assets/ \
          app/src/androidTest/java/com/example/smatchup/data/assets/
  git commit -m "feat(assets): add JsonAssetLoader and seed JSON for 5 characters + tierlists"
  ```

---

## Task 12: `UltimateApi`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/api/UltimateApi.kt`
- Create: `app/src/test/resources/fixtures/ultimate_api_steve_moves.json`
- Test:   `app/src/test/java/com/example/smatchup/data/api/UltimateApiTest.kt`

- [ ] **Step 1: Add the fixture.** Write to `app/src/test/resources/fixtures/ultimate_api_steve_moves.json`:

  ```json
  {
    "fighterId": 81,
    "fighter": "Steve",
    "moves": [
      { "moveName": "fair", "startup": 8, "totalFrames": 35, "baseDamage": 7.5 },
      { "moveName": "uair", "startup": 6, "totalFrames": 30, "baseDamage": 8.0 }
    ]
  }
  ```

- [ ] **Step 2: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/api/UltimateApiTest.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.test.runTest
  import okhttp3.OkHttpClient
  import okhttp3.mockwebserver.MockResponse
  import okhttp3.mockwebserver.MockWebServer
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertTrue
  import org.junit.Before
  import org.junit.Test

  class UltimateApiTest {

      private lateinit var server: MockWebServer

      @Before fun setup() {
          server = MockWebServer()
          server.start()
      }
      @After fun teardown() { server.shutdown() }

      private fun fixture(name: String): String =
          this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

      private fun apiClient(): UltimateApi =
          UltimateApi(client = OkHttpClient(), baseUrl = server.url("/").toString().trimEnd('/'))

      @Test fun fetchMovesSuccess() = runTest {
          server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("ultimate_api_steve_moves.json")))
          val result = apiClient().getMovesRaw("steve")
          assertTrue(result is ApiResult.Success)
          val body = (result as ApiResult.Success).data
          assertTrue(body.contains("\"fighter\":\"Steve\""))
      }

      @Test fun http404MapsToNotFound() = runTest {
          server.enqueue(MockResponse().setResponseCode(404))
          val result = apiClient().getMovesRaw("ghost")
          assertEquals(ApiResult.NotFound, result)
      }

      @Test fun http500MapsToNetworkError() = runTest {
          server.enqueue(MockResponse().setResponseCode(500))
          val r = apiClient().getMovesRaw("steve")
          assertTrue(r is ApiResult.NetworkError)
      }
  }
  ```

- [ ] **Step 3: Run, expect FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.UltimateApiTest"
  ```

- [ ] **Step 4: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/api/UltimateApi.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import java.io.IOException

  class UltimateApi(
      private val client: OkHttpClient,
      private val baseUrl: String = DEFAULT_BASE,
  ) {

      companion object {
          const val DEFAULT_BASE = "https://the-ultimate-api.dreseansutton.com"
      }

      /**
       * Returns the raw JSON body for a fighter's moves. Parsing into [Move] happens in the repo
       * (lazy) since the API schema may evolve. NetworkError for 5xx / IOException, NotFound for 404.
       */
      suspend fun getMovesRaw(charId: String): ApiResult<String> = withContext(Dispatchers.IO) {
          val req = Request.Builder().url("$baseUrl/api/characters/name/$charId").build()
          try {
              client.newCall(req).execute().use { resp ->
                  when {
                      resp.isSuccessful -> ApiResult.Success(resp.body?.string().orEmpty())
                      resp.code == 404 -> ApiResult.NotFound
                      else -> ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                  }
              }
          } catch (e: IOException) {
              ApiResult.NetworkError(e)
          }
      }
  }
  ```

- [ ] **Step 5: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.UltimateApiTest"
  ```

- [ ] **Step 6: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/UltimateApi.kt \
          app/src/test/java/com/example/smatchup/data/api/UltimateApiTest.kt \
          app/src/test/resources/fixtures/ultimate_api_steve_moves.json
  git commit -m "feat(api): add UltimateApi with raw-JSON fetch and ApiResult mapping"
  ```

---

## Task 13: `StartGgApi` (GraphQL + token gating + RateLimiter)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/api/StartGgApi.kt`
- Create: `app/src/test/resources/fixtures/startgg_tournament_sets.json`
- Create: `app/src/test/resources/fixtures/startgg_error_rate_limit.json`
- Test:   `app/src/test/java/com/example/smatchup/data/api/StartGgApiTest.kt`

- [ ] **Step 1: Add fixtures.**

  `app/src/test/resources/fixtures/startgg_tournament_sets.json`:
  ```json
  {
    "data": {
      "tournament": {
        "events": [{
          "sets": {
            "nodes": [
              { "id": 1, "winnerId": 100, "slots": [{"entrant":{"id":100,"name":"PlayerA"}},{"entrant":{"id":200,"name":"PlayerB"}}] }
            ]
          }
        }]
      }
    }
  }
  ```

  `app/src/test/resources/fixtures/startgg_error_rate_limit.json`:
  ```json
  { "errors": [ { "message": "Rate limit exceeded - api-token" } ] }
  ```

- [ ] **Step 2: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/api/StartGgApiTest.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.data.cache.Clock
  import com.example.smatchup.data.cache.RateLimiter
  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.test.runTest
  import okhttp3.OkHttpClient
  import okhttp3.mockwebserver.MockResponse
  import okhttp3.mockwebserver.MockWebServer
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertTrue
  import org.junit.Before
  import org.junit.Test

  class StartGgApiTest {

      private lateinit var server: MockWebServer

      @Before fun setup() { server = MockWebServer(); server.start() }
      @After fun teardown() { server.shutdown() }

      private fun fixture(name: String): String =
          this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

      private fun api(token: String): StartGgApi {
          val limiter = RateLimiter(capacity = 80, windowMs = 60_000L, clock = Clock { 0L })
          return StartGgApi(
              client = OkHttpClient(),
              endpoint = server.url("/gql").toString(),
              token = token,
              rateLimiter = limiter
          )
      }

      @Test fun blankTokenReturnsUnauthorized() = runTest {
          val r = api("").query("query { __typename }")
          assertEquals(ApiResult.Unauthorized, r)
      }

      @Test fun successReturnsRawJson() = runTest {
          server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("startgg_tournament_sets.json")))
          val r = api("tok").query("query { __typename }")
          assertTrue(r is ApiResult.Success)
          assertTrue((r as ApiResult.Success).data.contains("\"tournament\""))
      }

      @Test fun rateLimitMessageMapsToRateLimited() = runTest {
          server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("startgg_error_rate_limit.json")))
          val r = api("tok").query("q")
          assertTrue(r is ApiResult.RateLimited)
      }

      @Test fun http500MapsToNetworkError() = runTest {
          server.enqueue(MockResponse().setResponseCode(500))
          val r = api("tok").query("q")
          assertTrue(r is ApiResult.NetworkError)
      }
  }
  ```

- [ ] **Step 3: Run, expect FAIL.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.StartGgApiTest"
  ```

- [ ] **Step 4: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/api/StartGgApi.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.data.cache.RateLimiter
  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import okhttp3.MediaType.Companion.toMediaType
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import okhttp3.RequestBody.Companion.toRequestBody
  import org.json.JSONObject
  import java.io.IOException

  class StartGgApi(
      private val client: OkHttpClient,
      private val endpoint: String = DEFAULT_ENDPOINT,
      private val token: String,
      private val rateLimiter: RateLimiter,
  ) {
      companion object {
          const val DEFAULT_ENDPOINT = "https://api.start.gg/gql/alpha"
          private val JSON = "application/json; charset=utf-8".toMediaType()
      }

      suspend fun query(graphql: String, variables: JSONObject = JSONObject()): ApiResult<String> {
          if (token.isBlank()) return ApiResult.Unauthorized
          rateLimiter.acquire()

          val body = JSONObject()
              .put("query", graphql)
              .put("variables", variables)
              .toString()
              .toRequestBody(JSON)

          val req = Request.Builder()
              .url(endpoint)
              .header("Authorization", "Bearer $token")
              .post(body)
              .build()

          return withContext(Dispatchers.IO) {
              try {
                  client.newCall(req).execute().use { resp ->
                      val text = resp.body?.string().orEmpty()
                      when {
                          !resp.isSuccessful -> ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                          text.contains("Rate limit exceeded") -> ApiResult.RateLimited(null)
                          else -> ApiResult.Success(text)
                      }
                  }
              } catch (e: IOException) {
                  ApiResult.NetworkError(e)
              }
          }
      }
  }
  ```

- [ ] **Step 5: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.StartGgApiTest"
  ```

- [ ] **Step 6: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/StartGgApi.kt \
          app/src/test/java/com/example/smatchup/data/api/StartGgApiTest.kt \
          app/src/test/resources/fixtures/startgg_tournament_sets.json \
          app/src/test/resources/fixtures/startgg_error_rate_limit.json
  git commit -m "feat(api): add StartGgApi GraphQL with token gating and rate limiter"
  ```

---

## Task 14: Refactor `YouTubeApi`

**Files:**
- Modify: `app/src/main/java/com/example/smatchup/YouTubeApi.kt` → move to `app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt` and adapt return type.
- Create: `app/src/test/resources/fixtures/youtube_search.json`, `youtube_videos_details.json`, `youtube_channels.json`
- Test:   `app/src/test/java/com/example/smatchup/data/api/YouTubeApiTest.kt`

- [ ] **Step 1: Add fixtures.**

  `app/src/test/resources/fixtures/youtube_channels.json`:
  ```json
  { "items": [ { "id": "UCj1J3QuIftjOq9iv_rr7Egw" } ] }
  ```

  `app/src/test/resources/fixtures/youtube_search.json`:
  ```json
  {
    "items": [
      {
        "id": { "videoId": "vid_long" },
        "snippet": { "title": "MkLeo (Joker) vs Onin (Steve)" }
      },
      {
        "id": { "videoId": "vid_short" },
        "snippet": { "title": "Short Highlights" }
      }
    ]
  }
  ```

  `app/src/test/resources/fixtures/youtube_videos_details.json`:
  ```json
  {
    "items": [
      { "id": "vid_long",  "contentDetails": { "duration": "PT12M3S" } },
      { "id": "vid_short", "contentDetails": { "duration": "PT45S" } }
    ]
  }
  ```

- [ ] **Step 2: Delete the old file.**

  ```bash
  git mv app/src/main/java/com/example/smatchup/YouTubeApi.kt app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt
  ```

  If `git mv` complains because the source isn't tracked yet (untracked), run `mv` directly and re-add. (The pre-existing `YouTubeApi.kt` was untracked — see initial status.)

- [ ] **Step 3: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/api/YouTubeApiTest.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.test.runTest
  import okhttp3.OkHttpClient
  import okhttp3.mockwebserver.MockResponse
  import okhttp3.mockwebserver.MockWebServer
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertNull
  import org.junit.Assert.assertTrue
  import org.junit.Before
  import org.junit.Test

  class YouTubeApiTest {

      private lateinit var server: MockWebServer

      @Before fun setup() { server = MockWebServer(); server.start() }
      @After fun teardown() { server.shutdown() }

      private fun fixture(name: String): String =
          this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

      private fun api(apiKey: String): YouTubeApi =
          YouTubeApi(client = OkHttpClient(), apiKey = apiKey, baseUrl = server.url("/").toString().trimEnd('/'))

      @Test fun missingApiKeyReturnsUnauthorized() = runTest {
          assertEquals(ApiResult.Unauthorized, api("").searchLatest(listOf("MkLeo")))
      }

      @Test fun successFiltersShortsAndReturnsFirstMatchingCandidate() = runTest {
          server.enqueue(MockResponse().setBody(fixture("youtube_channels.json")))
          server.enqueue(MockResponse().setBody(fixture("youtube_search.json")))
          server.enqueue(MockResponse().setBody(fixture("youtube_videos_details.json")))

          val r = api("key").searchLatest(listOf("MkLeo", "Steve"))
          assertTrue(r is ApiResult.Success)
          val data = (r as ApiResult.Success).data
          assertTrue(data!!.url.contains("vid_long"))
      }

      @Test fun noMatchReturnsSuccessWithNullPayload() = runTest {
          server.enqueue(MockResponse().setBody(fixture("youtube_channels.json")))
          server.enqueue(MockResponse().setBody("""{"items":[]}"""))
          val r = api("key").searchLatest(listOf("Nobody"))
          assertTrue(r is ApiResult.Success)
          assertNull((r as ApiResult.Success).data)
      }
  }
  ```

- [ ] **Step 4: Rewrite the file as a class returning `ApiResult<Video?>`.** Replace `app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt` with:

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import okhttp3.HttpUrl.Companion.toHttpUrl
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import org.json.JSONArray
  import org.json.JSONObject
  import java.io.IOException

  class YouTubeApi(
      private val client: OkHttpClient,
      private val apiKey: String,
      private val baseUrl: String = DEFAULT_BASE,
  ) {

      companion object {
          const val DEFAULT_BASE = "https://www.googleapis.com/youtube/v3"
          private const val HANDLE = "vgbootcamp"
          private const val FALLBACK_CHANNEL_ID = "UCj1J3QuIftjOq9iv_rr7Egw"
      }

      data class Video(val url: String, val title: String, val videoId: String)

      @Volatile private var cachedChannelId: String? = null

      suspend fun searchLatest(terms: List<String>): ApiResult<Video?> {
          if (apiKey.isBlank()) return ApiResult.Unauthorized
          return withContext(Dispatchers.IO) {
              try {
                  val channelId = resolveChannelId()
                  val q = terms.joinToString(" ")
                  val searchUrl = "$baseUrl/search".toHttpUrl().newBuilder()
                      .addQueryParameter("part", "snippet")
                      .addQueryParameter("channelId", channelId)
                      .addQueryParameter("type", "video")
                      .addQueryParameter("order", "date")
                      .addQueryParameter("maxResults", "10")
                      .addQueryParameter("q", q)
                      .addQueryParameter("key", apiKey)
                      .build().toString()
                  val searchBody = JSONObject(execute(searchUrl))
                  val items = searchBody.optJSONArray("items") ?: JSONArray()
                  if (items.length() == 0) return@withContext ApiResult.Success(null)

                  val candidates = (0 until items.length()).map { i ->
                      val item = items.getJSONObject(i)
                      val videoId = item.getJSONObject("id").getString("videoId")
                      val title = item.getJSONObject("snippet").optString("title", "")
                      videoId to title
                  }

                  val ids = candidates.joinToString(",") { it.first }
                  val detailsUrl = "$baseUrl/videos".toHttpUrl().newBuilder()
                      .addQueryParameter("part", "contentDetails,liveStreamingDetails")
                      .addQueryParameter("id", ids)
                      .addQueryParameter("key", apiKey)
                      .build().toString()
                  val detailItems = JSONObject(execute(detailsUrl)).optJSONArray("items") ?: JSONArray()
                  val excluded = (0 until detailItems.length())
                      .map { detailItems.getJSONObject(it) }
                      .filter { it.has("liveStreamingDetails") || isShort(it.optJSONObject("contentDetails")?.optString("duration") ?: "") }
                      .map { it.getString("id") }
                      .toSet()

                  val match = candidates
                      .filter { it.first !in excluded }
                      .firstOrNull { (_, title) ->
                          val norm = normalize(title)
                          terms.all { norm.contains(normalize(it)) }
                      }

                  val video = match?.let { (videoId, title) ->
                      Video(url = "https://www.youtube.com/watch?v=$videoId", title = title, videoId = videoId)
                  }
                  ApiResult.Success(video)
              } catch (e: IOException) {
                  ApiResult.NetworkError(e)
              }
          }
      }

      private fun resolveChannelId(): String {
          cachedChannelId?.let { return it }
          val resolved = runCatching {
              val url = "$baseUrl/channels".toHttpUrl().newBuilder()
                  .addQueryParameter("part", "id")
                  .addQueryParameter("forHandle", HANDLE)
                  .addQueryParameter("key", apiKey)
                  .build().toString()
              val body = execute(url)
              val items = JSONObject(body).optJSONArray("items")
              items?.takeIf { it.length() > 0 }?.getJSONObject(0)?.optString("id")?.takeIf { it.isNotEmpty() }
          }.getOrNull()
          val final = resolved ?: FALLBACK_CHANNEL_ID
          cachedChannelId = final
          return final
      }

      private fun normalize(s: String): String =
          s.lowercase()
              .replace('0', 'o')
              .replace('-', ' ')
              .replace('_', ' ')
              .replace(Regex("\\s+"), " ")
              .trim()

      private fun isShort(duration: String): Boolean {
          if (duration.isBlank()) return false
          val h = Regex("(\\d+)H").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
          val m = Regex("(\\d+)M").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
          val s = Regex("(\\d+)S").find(duration)?.groupValues?.get(1)?.toLong() ?: 0L
          return h * 3600 + m * 60 + s <= 60
      }

      private fun execute(url: String): String {
          val req = Request.Builder().url(url).build()
          client.newCall(req).execute().use { resp ->
              val body = resp.body?.string().orEmpty()
              if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${body.take(200)}")
              return body
          }
      }
  }
  ```

- [ ] **Step 5: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.YouTubeApiTest"
  ```

- [ ] **Step 6: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/YouTubeApi.kt \
          app/src/test/java/com/example/smatchup/data/api/YouTubeApiTest.kt \
          app/src/test/resources/fixtures/youtube_channels.json \
          app/src/test/resources/fixtures/youtube_search.json \
          app/src/test/resources/fixtures/youtube_videos_details.json
  # If the old YouTubeApi.kt remains untracked, ensure it's not there:
  rm -f app/src/main/java/com/example/smatchup/YouTubeApi.kt
  git commit -m "refactor(api): move YouTubeApi to data/api, return ApiResult<Video?>"
  ```

---

## Task 15: `SmashWikiApi` minimal stub

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/api/SmashWikiApi.kt`
- Create: `app/src/test/resources/fixtures/smashwiki_extract.json`
- Test:   `app/src/test/java/com/example/smatchup/data/api/SmashWikiApiTest.kt`

- [ ] **Step 1: Add fixture.**

  ```json
  {
    "query": {
      "pages": {
        "12345": {
          "pageid": 12345,
          "title": "Steve (SSBU)",
          "extract": "Steve is a playable character in Super Smash Bros. Ultimate."
        }
      }
    }
  }
  ```

  Save to `app/src/test/resources/fixtures/smashwiki_extract.json`.

- [ ] **Step 2: Write the failing test.**

  ```kotlin
  // app/src/test/java/com/example/smatchup/data/api/SmashWikiApiTest.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.test.runTest
  import okhttp3.OkHttpClient
  import okhttp3.mockwebserver.MockResponse
  import okhttp3.mockwebserver.MockWebServer
  import org.junit.After
  import org.junit.Assert.assertEquals
  import org.junit.Assert.assertTrue
  import org.junit.Before
  import org.junit.Test

  class SmashWikiApiTest {

      private lateinit var server: MockWebServer

      @Before fun setup() { server = MockWebServer(); server.start() }
      @After fun teardown() { server.shutdown() }

      private fun fixture(name: String): String =
          this::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

      private fun api(): SmashWikiApi =
          SmashWikiApi(client = OkHttpClient(), baseUrl = server.url("/api.php").toString())

      @Test fun extractReturnsPlainText() = runTest {
          server.enqueue(MockResponse().setBody(fixture("smashwiki_extract.json")))
          val r = api().extract("Steve (SSBU)")
          assertTrue(r is ApiResult.Success)
          assertEquals("Steve is a playable character in Super Smash Bros. Ultimate.", (r as ApiResult.Success).data)
      }
  }
  ```

- [ ] **Step 3: Implement.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/data/api/SmashWikiApi.kt
  package com.example.smatchup.data.api

  import com.example.smatchup.domain.model.ApiResult
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import okhttp3.HttpUrl.Companion.toHttpUrl
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import org.json.JSONObject
  import java.io.IOException

  class SmashWikiApi(
      private val client: OkHttpClient,
      private val baseUrl: String = DEFAULT_BASE,
  ) {
      companion object { const val DEFAULT_BASE = "https://www.ssbwiki.com/api.php" }

      suspend fun extract(pageTitle: String): ApiResult<String?> = withContext(Dispatchers.IO) {
          val url = baseUrl.toHttpUrl().newBuilder()
              .addQueryParameter("action", "query")
              .addQueryParameter("prop", "extracts")
              .addQueryParameter("exintro", "1")
              .addQueryParameter("explaintext", "1")
              .addQueryParameter("format", "json")
              .addQueryParameter("titles", pageTitle)
              .build().toString()
          val req = Request.Builder().url(url).build()
          try {
              client.newCall(req).execute().use { resp ->
                  if (!resp.isSuccessful) return@withContext ApiResult.NetworkError(IOException("HTTP ${resp.code}"))
                  val json = JSONObject(resp.body?.string().orEmpty())
                  val pages = json.optJSONObject("query")?.optJSONObject("pages")
                  val firstKey = pages?.keys()?.asSequence()?.firstOrNull()
                  val extract = pages?.optJSONObject(firstKey ?: "")?.optString("extract")
                  ApiResult.Success(extract?.takeIf { it.isNotEmpty() })
              }
          } catch (e: IOException) {
              ApiResult.NetworkError(e)
          }
      }
  }
  ```

- [ ] **Step 4: Run, verify PASS.**

  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.api.SmashWikiApiTest"
  ```

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/SmashWikiApi.kt \
          app/src/test/java/com/example/smatchup/data/api/SmashWikiApiTest.kt \
          app/src/test/resources/fixtures/smashwiki_extract.json
  git commit -m "feat(api): add SmashWikiApi TextExtracts wrapper"
  ```

---

## Task 16: `AppContainer` + `SmatchupApp` + manifest wiring

**Files:**
- Create: `app/src/main/java/com/example/smatchup/SmatchupApp.kt`
- Create: `app/src/main/java/com/example/smatchup/di/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`

(No tests — pure wiring. The smoke test is the app launching.)

- [ ] **Step 1: Create `AppContainer.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/di/AppContainer.kt
  package com.example.smatchup.di

  import android.content.Context
  import androidx.room.Room
  import com.example.smatchup.BuildConfig
  import com.example.smatchup.data.api.HttpClientProvider
  import com.example.smatchup.data.api.SmashWikiApi
  import com.example.smatchup.data.api.StartGgApi
  import com.example.smatchup.data.api.UltimateApi
  import com.example.smatchup.data.api.YouTubeApi
  import com.example.smatchup.data.assets.JsonAssetLoader
  import com.example.smatchup.data.cache.CacheManager
  import com.example.smatchup.data.cache.RateLimiter
  import com.example.smatchup.data.cache.SystemClock
  import com.example.smatchup.data.local.SmatchupDatabase

  class AppContainer(context: Context) {

      private val appContext = context.applicationContext

      val database: SmatchupDatabase = Room.databaseBuilder(
          appContext,
          SmatchupDatabase::class.java,
          "smatchup.db"
      ).build()

      val cacheManager: CacheManager = CacheManager(SystemClock)
      val jsonAssetLoader: JsonAssetLoader = JsonAssetLoader(appContext)

      private val httpClient = HttpClientProvider.client
      private val startGgRateLimiter = RateLimiter(capacity = 80, windowMs = 60_000L, clock = SystemClock)

      val ultimateApi   = UltimateApi(httpClient)
      val startGgApi    = StartGgApi(httpClient, token = BuildConfig.START_GG_TOKEN, rateLimiter = startGgRateLimiter)
      val youtubeApi    = YouTubeApi(httpClient, apiKey = BuildConfig.YOUTUBE_API_KEY)
      val smashWikiApi  = SmashWikiApi(httpClient)
  }
  ```

- [ ] **Step 2: Create `SmatchupApp.kt`.**

  ```kotlin
  // app/src/main/java/com/example/smatchup/SmatchupApp.kt
  package com.example.smatchup

  import android.app.Application
  import com.example.smatchup.di.AppContainer

  class SmatchupApp : Application() {
      lateinit var container: AppContainer
          private set

      override fun onCreate() {
          super.onCreate()
          instance = this
          container = AppContainer(this)
      }

      companion object {
          lateinit var instance: SmatchupApp
              private set
      }
  }
  ```

- [ ] **Step 3: Register the Application in the manifest.** Edit `app/src/main/AndroidManifest.xml` and add `android:name=".SmatchupApp"` to the `<application>` tag:

  ```xml
  <application
      android:name=".SmatchupApp"
      android:allowBackup="true"
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:fullBackupContent="@xml/backup_rules"
      android:icon="@mipmap/ic_launcher"
      android:label="@string/app_name"
      android:roundIcon="@mipmap/ic_launcher_round"
      android:supportsRtl="true"
      android:theme="@style/Theme.Smatchup">
      ...
  </application>
  ```

- [ ] **Step 4: Smoke build + install.**

  ```bash
  ./gradlew :app:assembleDebug
  ```

  Expected: `BUILD SUCCESSFUL`. If a device is connected, also run:

  ```bash
  ./gradlew :app:installDebug
  ```

  Launch the app — `MainActivity` still shows the current Compose default screen (sub-project 2's responsibility to replace it), but the app should boot without crash. `SmatchupApp.onCreate` will instantiate the database and APIs.

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/SmatchupApp.kt \
          app/src/main/java/com/example/smatchup/di/AppContainer.kt \
          app/src/main/AndroidManifest.xml
  git commit -m "feat(di): add AppContainer singleton wired through SmatchupApp"
  ```

---

## Final verification

- [ ] **Step 1: Run the full JVM test suite.**

  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

  Expected: all tests pass.

- [ ] **Step 2: Run the full instrumented suite (device required).**

  ```bash
  ./gradlew :app:connectedDebugAndroidTest
  ```

  Expected: all tests pass.

- [ ] **Step 3: Build debug APK once more end-to-end.**

  ```bash
  ./gradlew :app:assembleDebug
  ```

- [ ] **Step 4: Quick `git log --oneline` check.** All 16 task commits should be on the `data-foundation` branch.

- [ ] **Step 5: Final note for sub-project 2.** Sub-project 2 will:
  - Replace `MainActivity` to host the `NavGraph`.
  - Wire ViewModels through a factory that reads `SmatchupApp.instance.container`.
  - Implement the SmatchupTheme + the reusable component library.

  Nothing in this sub-project should be removed; sub-project 2 only adds.

---

## Self-review (post-write)

**1. Spec coverage:**
- Models from §5.1 of the spec → Task 3 + Task 2 ✓
- API result type → Task 2 ✓
- All four API clients → Tasks 12/13/14/15 ✓
- Room entities + DAOs + DB → Tasks 9/10 ✓
- JsonAssetLoader + bundled assets → Task 11 ✓
- CacheManager + Clock + CacheTtl → Tasks 5/6 ✓
- RateLimiter token bucket → Task 7 ✓
- PasswordHasher → Task 4 ✓
- AppContainer + Application class → Task 16 ✓
- `START_GG_TOKEN` BuildConfig field + gating → Tasks 1 + 13 ✓
- HttpClientProvider → Task 8 ✓
- Spec section 10 testing strategy → Tasks 4/6/7/10/11/12/13/14/15 (unit + instrumented) ✓
- Spec section 6 fallback chain — chain itself is in `CharacterRepo`, which is **deferred** to sub-project 2/3 (per scope: this sub-project provides the pieces, the chain is the consumer). Noted in plan header.

**2. Placeholder scan:** none of the "No Placeholders" patterns appear. Code blocks are full.

**3. Type consistency:**
- `ApiResult<T>` signature is consistent across Tasks 2, 12, 13, 14, 15.
- `Clock` interface is consistent across Tasks 5, 6, 7, 16.
- `RateLimiter(capacity, windowMs, clock)` constructor signature consistent across Tasks 7, 13, 16.
- DAO names (`userDao()`, `favoritesDao()`, `cacheDao()`, `sessionDao()`) consistent in DB definition (Task 10) and `AppContainer` references implicitly.
- `CachedFramedataEntity.charId` is the primary key (Task 9), matched by `CacheDao.getFramedata(charId)` (Task 10).

Plan ready.
