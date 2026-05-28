# Smatchup — Matchup Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Match-up flow — a picker for choosing two characters and a detail screen (paired section cards: gameplan / strong / punishable / stages per character, plus a win-rate bar and the last-clash video). Win-rate is computed from start.gg tournament sets (token-gated, cached); curated matchup text comes from bundled JSON.

**Architecture:** `MatchupPickerScreen` lets the user pick char A then char B and navigates to `MatchupDetail`. `MatchupRepository.getMatchup(a,b)` loads curated paired text from `matchups/<a>_<b>.json` (normalized a<b). Win-rate is resolved lazily by `WinrateComputer`, which short-circuits to `Unauthorized` when `START_GG_TOKEN` is blank (current state → `TokenGatedBanner`) and otherwise aggregates start.gg set results via the pure `WinrateAggregator`. Results cache in the existing `cached_winrate` table.

**Tech Stack:** existing stack. No new dependencies.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — sections 6.4 (token gating), 6.6 (per-MU winrate), 8 (UI layout B — paired section cards). Builds on sub-projects 1–3.

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── data/
│   ├── repository/MatchupRepository.kt
│   └── winrate/
│       ├── WinrateAggregator.kt          # pure function: sets → ratio
│       └── WinrateComputer.kt            # orchestration: token gate, cache, aggregate
├── ui/components/
│   ├── WinBar.kt                         # A% | B% bar
│   ├── SectionCard.kt                    # gold-bordered titled card
│   └── PairedSplit.kt                    # internal A/B split
├── ui/matchup/
│   ├── MatchupPickerScreen.kt
│   ├── MatchupPickerViewModel.kt
│   ├── MatchupDetailScreen.kt
│   ├── MatchupDetailViewModel.kt
│   └── MatchupDetailTab.kt              # enum (Gameplan/Strong/Punish/Stages/Video)

app/src/main/assets/matchups/
├── sonic_steve.json
├── fox_steve.json
└── snake_sonic.json

app/src/test/java/com/example/smatchup/
├── data/winrate/WinrateAggregatorTest.kt
├── data/winrate/WinrateComputerTest.kt
├── ui/matchup/MatchupPickerViewModelTest.kt
└── ui/matchup/MatchupDetailViewModelTest.kt

app/src/androidTest/java/com/example/smatchup/
└── data/repository/MatchupRepositoryTest.kt
```

### Modified

- `app/src/main/java/com/example/smatchup/di/AppContainer.kt` — register `MatchupRepository`, `WinrateComputer`.
- `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` — register `MatchupPickerViewModel`, parameterized `MatchupDetailViewModel(a,b)`.
- `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt` — replace `MatchupPicker` + `MatchupDetail` placeholders.

### Untouched

`data/local/` schema (reuses `cached_winrate`). Favorites star deferred to sub-project 6.

---

## Conventions

- **Branch:** `git checkout -b matchup-detail` off `api`.
- One commit per task. TDD for aggregator/computer/VMs. UI screens smoke-tested.
- **Single-device test rule (learned in sub-project 3):** NEVER run two `connectedDebugAndroidTest` invocations at once — they put the emulator `offline` and corrupt result XML. Run instrumented tests one at a time, foreground.

---

## Task 1: Seed matchup JSON

Shape (normalized so charA < charB lexicographically):
```json
{
  "charA": "sonic", "charB": "steve",
  "gameplanA": ["..."], "gameplanB": ["..."],
  "strongMovesA": ["..."], "strongMovesB": ["..."],
  "punishableMovesA": ["..."], "punishableMovesB": ["..."],
  "stagesForA": { "ban": ["bf"], "counterpick": ["sv"] },
  "stagesForB": { "ban": ["sv"], "counterpick": ["fd"] }
}
```

**Files:** 3 JSONs under `app/src/main/assets/matchups/`.

- [ ] **Step 1: Create `app/src/main/assets/matchups/sonic_steve.json`.**

```json
{
  "charA": "sonic",
  "charB": "steve",
  "gameplanA": ["Force Steve to approach by holding center", "Whiff-punish blocks with bair", "Edgeguard hard — Steve recovery is exploitable"],
  "gameplanB": ["Wall with side-B and blocks", "Don't chase spindash; shield and punish", "Use anvil to cover landings"],
  "strongMovesA": ["bair", "spindash mixups", "fast OoS up-B"],
  "strongMovesB": ["side-b (iron)", "anvil", "block pillar"],
  "punishableMovesA": ["raw spindash on shield", "committed dair"],
  "punishableMovesB": ["slow OoS", "telegraphed anvil"],
  "stagesForA": { "ban": ["bf"], "counterpick": ["sv", "town"] },
  "stagesForB": { "ban": ["sv"], "counterpick": ["fd", "ps2"] }
}
```

- [ ] **Step 2: Create `app/src/main/assets/matchups/fox_steve.json`.**

```json
{
  "charA": "fox",
  "charB": "steve",
  "gameplanA": ["Pressure with lasers to deny mining", "Frame-1 nair to break the wall", "Convert openings to up-air ladders"],
  "gameplanB": ["Wall fast characters with blocks", "Don't get opened up — Fox kills off one touch", "Anvil to cover Fox's landings"],
  "strongMovesA": ["nair", "up-smash", "shine OoS"],
  "strongMovesB": ["block pillar", "side-b", "up-tilt"],
  "punishableMovesA": ["committed side-b", "whiffed up-smash"],
  "punishableMovesB": ["slow OoS", "predictable mining"],
  "stagesForA": { "ban": ["town"], "counterpick": ["bf", "ps2"] },
  "stagesForB": { "ban": ["sv"], "counterpick": ["fd"] }
}
```

- [ ] **Step 3: Create `app/src/main/assets/matchups/snake_sonic.json`.**

```json
{
  "charA": "snake",
  "charB": "sonic",
  "gameplanA": ["Lay grenades and C4 to wall Sonic's approach", "Up-tilt for early kills when Sonic commits", "Edgeguard with Nikita"],
  "gameplanB": ["Pick up grenades and throw them back", "Bait DACUS and committed tilts", "Run down Snake's slow frame data"],
  "strongMovesA": ["up-tilt", "grenade pickups", "f-tilt"],
  "strongMovesB": ["bair", "spindash whiff-punish", "fast movement"],
  "punishableMovesA": ["slow aerials", "committed up-tilt"],
  "punishableMovesB": ["raw spindash", "dair landings"],
  "stagesForA": { "ban": ["sv"], "counterpick": ["fd", "town"] },
  "stagesForB": { "ban": ["bf"], "counterpick": ["sv", "kalos"] }
}
```

- [ ] **Step 4: Build (asset packaging).**
  ```bash
  ./gradlew :app:assembleDebug
  ```

- [ ] **Step 5: Commit.**
  ```bash
  git checkout -b matchup-detail
  git add app/src/main/assets/matchups/
  git commit -m "feat(assets): seed matchup JSON for 3 character pairs"
  ```

---

## Task 2: `MatchupRepository.getMatchup`

Loads curated matchup JSON, normalizes the pair, returns a `Matchup` domain object (winRate left null — filled by the computer). When no JSON exists for the pair, returns a `Matchup` with empty lists (so the UI shows graceful empties).

**Files:** `app/src/main/java/com/example/smatchup/data/repository/MatchupRepository.kt`; test `app/src/androidTest/java/com/example/smatchup/data/repository/MatchupRepositoryTest.kt`.

- [ ] **Step 1: Failing instrumented test.**

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/repository/MatchupRepositoryTest.kt
package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchupRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = MatchupRepository(loader)

    @Test fun loadsCuratedMatchupNormalized() = runBlocking {
        // Request in reverse order — repo must normalize to sonic < steve.
        val mu = repo.getMatchup("steve", "sonic")
        assertEquals("sonic", mu.charA)
        assertEquals("steve", mu.charB)
        assertTrue(mu.gameplanA.isNotEmpty())
        assertTrue(mu.gameplanB.isNotEmpty())
        assertTrue(mu.strongMovesA.isNotEmpty())
        assertTrue(mu.punishableMovesB.isNotEmpty())
    }

    @Test fun stagesParsedWithVerdicts() = runBlocking {
        val mu = repo.getMatchup("sonic", "steve")
        assertTrue(mu.stagesForA.any { it.verdict == StageVerdict.BAN })
        assertTrue(mu.stagesForB.any { it.verdict == StageVerdict.COUNTERPICK })
    }

    @Test fun unknownPairReturnsEmptyMatchup() = runBlocking {
        val mu = repo.getMatchup("mario", "luigi")
        assertEquals("luigi", mu.charA)   // normalized: luigi < mario
        assertEquals("mario", mu.charB)
        assertTrue(mu.gameplanA.isEmpty())
        assertTrue(mu.gameplanB.isEmpty())
    }
}
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/MatchupRepository.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Matchup
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val STAGE_NAMES: Map<String, String> = mapOf(
    "bf" to "Battlefield", "fd" to "Final Destination", "sv" to "Smashville",
    "ps2" to "Pokémon Stadium 2", "town" to "Town and City",
    "kalos" to "Kalos Pokémon League", "hollow" to "Hollow Bastion", "lylat" to "Lylat Cruise",
)

class MatchupRepository(private val loader: JsonAssetLoader) {

    suspend fun getMatchup(c1: String, c2: String): Matchup = withContext(Dispatchers.IO) {
        val (a, b) = if (c1 <= c2) c1 to c2 else c2 to c1
        val json = loader.matchup(a, b)
        if (json == null) {
            return@withContext Matchup(charA = a, charB = b)
        }
        Matchup(
            charA = a,
            charB = b,
            gameplanA = json.stringList("gameplanA"),
            gameplanB = json.stringList("gameplanB"),
            strongMovesA = json.stringList("strongMovesA"),
            strongMovesB = json.stringList("strongMovesB"),
            punishableMovesA = json.stringList("punishableMovesA"),
            punishableMovesB = json.stringList("punishableMovesB"),
            stagesForA = json.optJSONObject("stagesForA").toStages(),
            stagesForB = json.optJSONObject("stagesForB").toStages(),
        )
    }

    private fun JSONObject.stringList(key: String): List<String> =
        optJSONArray(key)?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()

    private fun JSONObject?.toStages(): List<Stage> {
        if (this == null) return emptyList()
        val ban = stagesOf("ban", StageVerdict.BAN)
        val cp = stagesOf("counterpick", StageVerdict.COUNTERPICK)
        return ban + cp
    }

    private fun JSONObject.stagesOf(key: String, verdict: StageVerdict): List<Stage> =
        optJSONArray(key)?.let { arr ->
            (0 until arr.length()).map { i ->
                val id = arr.getString(i)
                Stage(id = id, displayName = STAGE_NAMES[id] ?: id, verdict = verdict)
            }
        } ?: emptyList()
}
```

- [ ] **Step 3: Run.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.MatchupRepositoryTest
  ```
  Expected: 3 pass.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/repository/MatchupRepository.kt \
          app/src/androidTest/java/com/example/smatchup/data/repository/MatchupRepositoryTest.kt
  git commit -m "feat(repo): add MatchupRepository loading curated matchup JSON"
  ```

---

## Task 3: `WinrateAggregator` (pure function)

**Files:** `app/src/main/java/com/example/smatchup/data/winrate/WinrateAggregator.kt`; test `app/src/test/java/com/example/smatchup/data/winrate/WinrateAggregatorTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/data/winrate/WinrateAggregatorTest.kt
package com.example.smatchup.data.winrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WinrateAggregatorTest {

    @Test fun emptySetsReturnsNull() {
        assertNull(WinrateAggregator.aggregate(emptyList()))
    }

    @Test fun computesRatioFromAPerspective() {
        val sets = listOf(
            WinrateAggregator.SetResult(winnerIsA = true),
            WinrateAggregator.SetResult(winnerIsA = true),
            WinrateAggregator.SetResult(winnerIsA = false),
            WinrateAggregator.SetResult(winnerIsA = true),
        )
        val r = WinrateAggregator.aggregate(sets)!!
        assertEquals(0.75f, r.winRateA, 0.0001f)
        assertEquals(4, r.sampleSize)
    }

    @Test fun allLossesIsZero() {
        val sets = listOf(WinrateAggregator.SetResult(false), WinrateAggregator.SetResult(false))
        val r = WinrateAggregator.aggregate(sets)!!
        assertEquals(0.0f, r.winRateA, 0.0001f)
        assertEquals(2, r.sampleSize)
    }
}
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/winrate/WinrateAggregator.kt
package com.example.smatchup.data.winrate

data class WinrateResult(val winRateA: Float, val sampleSize: Int)

object WinrateAggregator {

    /** One tournament set between charA's player and charB's player. */
    data class SetResult(val winnerIsA: Boolean)

    fun aggregate(sets: List<SetResult>): WinrateResult? {
        if (sets.isEmpty()) return null
        val aWins = sets.count { it.winnerIsA }
        return WinrateResult(winRateA = aWins.toFloat() / sets.size, sampleSize = sets.size)
    }
}
```

- [ ] **Step 3: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.winrate.WinrateAggregatorTest"
  ```
  Expected: 3 pass.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/winrate/WinrateAggregator.kt \
          app/src/test/java/com/example/smatchup/data/winrate/WinrateAggregatorTest.kt
  git commit -m "feat(winrate): add pure WinrateAggregator"
  ```

---

## Task 4: `WinrateComputer` (token gate + cache + aggregate)

`WinrateComputer` is injected with a `fetchSets` suspend lambda so it's JVM-testable (no real network). Production wires it to start.gg via `AppContainer`. It:
1. normalizes the pair (a<b),
2. checks `cached_winrate` (returns cached if fresh),
3. calls `fetchSets(a,b)`; on `Unauthorized` → returns `Unauthorized` (current state, no token),
4. aggregates with `WinrateAggregator`, caches, returns.

**Files:** `app/src/main/java/com/example/smatchup/data/winrate/WinrateComputer.kt`; test `app/src/test/java/com/example/smatchup/data/winrate/WinrateComputerTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/data/winrate/WinrateComputerTest.kt
package com.example.smatchup.data.winrate

import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WinrateComputerTest {

    @Test fun unauthorizedFetchPropagates() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ -> ApiResult.Unauthorized },
        )
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.Unauthorized)
    }

    @Test fun aggregatesSuccessfulFetch() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ ->
                ApiResult.Success(
                    listOf(
                        WinrateAggregator.SetResult(true),
                        WinrateAggregator.SetResult(false),
                        WinrateAggregator.SetResult(true),
                    ),
                )
            },
        )
        // Request reverse order — computer normalizes to sonic < steve, so winnerIsA refers to sonic.
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.Success)
        val result = (r as ApiResult.Success).data
        assertEquals(3, result.sampleSize)
        assertEquals(2f / 3f, result.winRateA, 0.0001f)
    }

    @Test fun emptySetsReturnsNotFound() = runBlocking {
        val computer = WinrateComputer(
            cacheDao = null, cacheManager = null,
            fetchSets = { _, _ -> ApiResult.Success(emptyList()) },
        )
        val r = computer.winrate("steve", "sonic")
        assertTrue(r is ApiResult.NotFound)
    }
}
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/winrate/WinrateComputer.kt
package com.example.smatchup.data.winrate

import com.example.smatchup.data.cache.CacheManager
import com.example.smatchup.data.cache.CacheTtl
import com.example.smatchup.data.local.dao.CacheDao
import com.example.smatchup.data.local.entity.CachedWinrateEntity
import com.example.smatchup.domain.model.ApiResult

class WinrateComputer(
    private val cacheDao: CacheDao?,
    private val cacheManager: CacheManager?,
    private val fetchSets: suspend (charA: String, charB: String) -> ApiResult<List<WinrateAggregator.SetResult>>,
) {

    suspend fun winrate(c1: String, c2: String): ApiResult<WinrateResult> {
        val (a, b) = if (c1 <= c2) c1 to c2 else c2 to c1

        val cached = cacheDao?.getWinrate(a, b)
        if (cached != null && cacheManager != null &&
            cacheManager.isFresh(cached.computedAt, CacheTtl.WINRATE_MS)
        ) {
            return ApiResult.Success(WinrateResult(cached.winRateA, cached.sampleSize))
        }

        return when (val r = fetchSets(a, b)) {
            is ApiResult.Success -> {
                val result = WinrateAggregator.aggregate(r.data)
                    ?: return ApiResult.NotFound
                cacheDao?.upsertWinrate(
                    CachedWinrateEntity(
                        charA = a, charB = b,
                        winRateA = result.winRateA,
                        sampleSize = result.sampleSize,
                        majorsCount = 0,
                        computedAt = System.currentTimeMillis(),
                    ),
                )
                ApiResult.Success(result)
            }
            is ApiResult.NetworkError -> r
            is ApiResult.RateLimited -> r
            is ApiResult.ParseError -> r
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.NotFound -> ApiResult.NotFound
        }
    }
}
```

- [ ] **Step 3: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.winrate.WinrateComputerTest"
  ```
  Expected: 3 pass.

- [ ] **Step 4: Wire in `AppContainer`.** Edit `app/src/main/java/com/example/smatchup/di/AppContainer.kt`. Add imports:
  ```kotlin
  import com.example.smatchup.data.repository.MatchupRepository
  import com.example.smatchup.data.winrate.WinrateAggregator
  import com.example.smatchup.data.winrate.WinrateComputer
  import com.example.smatchup.domain.model.ApiResult
  ```
  After `bestPlayerRepository`, append:
  ```kotlin
      val matchupRepository: MatchupRepository = MatchupRepository(jsonAssetLoader)

      val winrateComputer: WinrateComputer = WinrateComputer(
          cacheDao = database.cacheDao(),
          cacheManager = cacheManager,
          fetchSets = { _, _ ->
              // start.gg matchup-set aggregation is not yet wired (no token in this build).
              // When START_GG_TOKEN is present, replace this with a real query+parse that
              // returns ApiResult.Success(List<WinrateAggregator.SetResult>). For now the
              // StartGgApi gates on a blank token and returns Unauthorized.
              when (val r = startGgApi.query("query { __typename }")) {
                  ApiResult.Unauthorized -> ApiResult.Unauthorized
                  is ApiResult.Success -> ApiResult.Success(emptyList<WinrateAggregator.SetResult>())
                  is ApiResult.NetworkError -> r
                  is ApiResult.RateLimited -> r
                  is ApiResult.ParseError -> r
                  ApiResult.NotFound -> ApiResult.NotFound
              }
          },
      )
  ```

- [ ] **Step 5: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 6: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/winrate/WinrateComputer.kt \
          app/src/test/java/com/example/smatchup/data/winrate/WinrateComputerTest.kt \
          app/src/main/java/com/example/smatchup/di/AppContainer.kt
  git commit -m "feat(winrate): add WinrateComputer with token gate + cache; wire container"
  ```

---

## Task 5: `WinBar` + `SectionCard` + `PairedSplit` components

**Files:** 3 files under `app/src/main/java/com/example/smatchup/ui/components/`.

- [ ] **Step 1: Create `WinBar.kt`.**

```kotlin
package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

/**
 * Win-rate bar from charA's perspective. [percentA] in 0..1. [sample] is the set count.
 */
@Composable
fun WinBar(percentA: Float, sample: Int, modifier: Modifier = Modifier) {
    val a = percentA.coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(a.coerceAtLeast(0.001f))
                .fillMaxWidth()
                .background(SmatchupColors.Gold),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "${(a * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = SmatchupColors.Bg1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight((1f - a).coerceAtLeast(0.001f))
                .fillMaxWidth()
                .background(SmatchupColors.DangerRed),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                "${((1 - a) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = SmatchupColors.Bg1,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Create `SectionCard.kt`.**

```kotlin
package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SmatchupColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .background(SmatchupColors.Purple.copy(alpha = 0.10f))
            .padding(12.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = SmatchupColors.Gold,
        )
        content()
    }
}
```

- [ ] **Step 3: Create `PairedSplit.kt`.**

```kotlin
package com.example.smatchup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

/**
 * Two columns side by side, one per character. Each side shows a header (the character name)
 * and a list of strings.
 */
@Composable
fun PairedSplit(
    leftWho: String,
    leftItems: List<String>,
    rightWho: String,
    rightItems: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Side(who = leftWho, items = leftItems, modifier = Modifier.weight(1f))
        Side(who = rightWho, items = rightItems, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Side(who: String, items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = who,
            style = MaterialTheme.typography.labelMedium,
            color = SmatchupColors.TextDim,
        )
        if (items.isEmpty()) {
            Text("—", color = SmatchupColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        } else {
            items.forEach { Text("• $it", color = SmatchupColors.Text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
```

- [ ] **Step 4: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/WinBar.kt \
          app/src/main/java/com/example/smatchup/ui/components/SectionCard.kt \
          app/src/main/java/com/example/smatchup/ui/components/PairedSplit.kt
  git commit -m "feat(ui): add WinBar, SectionCard, PairedSplit components"
  ```

---

## Task 6: `MatchupPickerViewModel`

Holds the roster + two selection slots (slotA, slotB). When both are filled, exposes a `ready` pair. Reuses `CharacterRepository.loadRoster()`.

**Files:** `app/src/main/java/com/example/smatchup/ui/matchup/MatchupPickerViewModel.kt`; test `app/src/test/java/com/example/smatchup/ui/matchup/MatchupPickerViewModelTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/ui/matchup/MatchupPickerViewModelTest.kt
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

    @Test fun pickingSameCharacterTwiceKeepsSlotB Null() = runTest {
        val vm = vm()
        vm.pick("fox")
        vm.pick("fox")
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("fox", s.slotA)
            assertNull("Cannot pick same char for both slots", s.slotB)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

Note: the test function name with a space inside backticks is intentional Kotlin test style. Use exactly: `` `pickingSameCharacterTwiceKeepsSlotB Null` `` — wait, that has a stray space. Use this exact name instead: `` `picking same character twice keeps slotB null` ``. Rewrite that third test header as:

```kotlin
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
```

(Use the rewritten version; ignore the earlier malformed name.)

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/matchup/MatchupPickerViewModel.kt
package com.example.smatchup.ui.matchup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchupPickerUiState(
    val isLoading: Boolean = true,
    val roster: List<Character> = emptyList(),
    val slotA: String? = null,
    val slotB: String? = null,
    val ready: Pair<String, String>? = null,
)

class MatchupPickerViewModel(
    private val loadRoster: suspend () -> List<Character>,
) : ViewModel() {

    constructor(repo: CharacterRepository) : this(loadRoster = { repo.loadRoster() })

    private val _state = MutableStateFlow(MatchupPickerUiState())
    val state: StateFlow<MatchupPickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val r = try { loadRoster() } catch (_: Throwable) { emptyList() }
            _state.update { it.copy(isLoading = false, roster = r) }
        }
    }

    fun pick(charId: String) {
        _state.update { s ->
            when {
                s.slotA == null -> s.copy(slotA = charId)
                s.slotA == charId -> s   // can't pick the same char for both
                s.slotB == null -> {
                    val ready = s.slotA to charId
                    s.copy(slotB = charId, ready = ready)
                }
                else -> s.copy(slotA = charId, slotB = null, ready = null) // restart selection
            }
        }
    }

    fun reset() {
        _state.update { it.copy(slotA = null, slotB = null, ready = null) }
    }
}
```

- [ ] **Step 3: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.matchup.MatchupPickerViewModelTest"
  ```
  Expected: 3 pass.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/matchup/MatchupPickerViewModel.kt \
          app/src/test/java/com/example/smatchup/ui/matchup/MatchupPickerViewModelTest.kt
  git commit -m "feat(ui): add MatchupPickerViewModel with two-slot selection"
  ```

---

## Task 7: `MatchupDetailTab` + `MatchupDetailViewModel`

**Files:** `app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailTab.kt`, `MatchupDetailViewModel.kt`; test `app/src/test/java/com/example/smatchup/ui/matchup/MatchupDetailViewModelTest.kt`.

- [ ] **Step 1: Create `MatchupDetailTab.kt`.**

```kotlin
package com.example.smatchup.ui.matchup

enum class MatchupDetailTab(val label: String) {
    GAMEPLAN("Gameplan"),
    STRONG("Strong"),
    PUNISH("Punish"),
    STAGES("Stages"),
    VIDEO("Video"),
}
```

- [ ] **Step 2: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/ui/matchup/MatchupDetailViewModelTest.kt
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
```

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailViewModel.kt
package com.example.smatchup.ui.matchup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.data.repository.BestPlayerRepository
import com.example.smatchup.data.repository.MatchupRepository
import com.example.smatchup.data.winrate.WinrateComputer
import com.example.smatchup.data.winrate.WinrateResult
import com.example.smatchup.domain.model.ApiResult
import com.example.smatchup.domain.model.Matchup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchupDetailUiState(
    val isLoading: Boolean = true,
    val matchup: Matchup? = null,
    val winRateA: Float? = null,
    val winrateSample: Int = 0,
    val winrateTokenGated: Boolean = false,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
    val selectedTab: MatchupDetailTab = MatchupDetailTab.GAMEPLAN,
    val error: String? = null,
)

class MatchupDetailViewModel(
    private val charA: String,
    private val charB: String,
    private val loadMatchup: suspend () -> Matchup,
    private val loadWinrate: suspend () -> ApiResult<WinrateResult>,
    private val loadVideo: suspend () -> YouTubeApi.Video?,
) : ViewModel() {

    constructor(
        charA: String,
        charB: String,
        matchupRepo: MatchupRepository,
        winrateComputer: WinrateComputer,
        bestPlayerRepo: BestPlayerRepository,
        youTubeApi: YouTubeApi,
    ) : this(
        charA = charA,
        charB = charB,
        loadMatchup = { matchupRepo.getMatchup(charA, charB) },
        loadWinrate = { winrateComputer.winrate(charA, charB) },
        loadVideo = {
            val pa = bestPlayerRepo.bestPlayerFor(charA)?.tag
            val pb = bestPlayerRepo.bestPlayerFor(charB)?.tag
            val terms = listOfNotNull(pa, pb).ifEmpty { listOf(charA, charB) }
            when (val r = youTubeApi.searchLatest(terms)) {
                is ApiResult.Success -> r.data
                else -> null
            }
        },
    )

    private val _state = MutableStateFlow(MatchupDetailUiState())
    val state: StateFlow<MatchupDetailUiState> = _state.asStateFlow()

    init { reload() }

    fun selectTab(tab: MatchupDetailTab) = _state.update { it.copy(selectedTab = tab) }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val mu = loadMatchup()
                _state.update { it.copy(isLoading = false, matchup = mu) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }
        }
        viewModelScope.launch {
            when (val r = loadWinrate()) {
                is ApiResult.Success -> _state.update {
                    it.copy(winRateA = r.data.winRateA, winrateSample = r.data.sampleSize)
                }
                ApiResult.Unauthorized -> _state.update { it.copy(winrateTokenGated = true) }
                else -> { /* leave null */ }
            }
        }
        viewModelScope.launch {
            val video = try { loadVideo() } catch (_: Throwable) { null }
            _state.update { it.copy(lastVideoUrl = video?.url, lastVideoTitle = video?.title) }
        }
    }
}
```

- [ ] **Step 4: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.matchup.MatchupDetailViewModelTest"
  ```
  Expected: 4 pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailTab.kt \
          app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailViewModel.kt \
          app/src/test/java/com/example/smatchup/ui/matchup/MatchupDetailViewModelTest.kt
  git commit -m "feat(ui): add MatchupDetailViewModel with winrate + video fetch"
  ```

---

## Task 8: `MatchupPickerScreen`

**Files:** `app/src/main/java/com/example/smatchup/ui/matchup/MatchupPickerScreen.kt`.

- [ ] **Step 1: Create.**

```kotlin
package com.example.smatchup.ui.matchup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun MatchupPickerScreen(
    onPairReady: (a: String, b: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchupPickerViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.ready) {
        state.ready?.let { (a, b) ->
            onPairReady(a, b)
            viewModel.reset()
        }
    }

    Column(modifier = modifier.wolBackground().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Match-up",
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )
        val prompt = when {
            state.slotA == null -> "Choisis le 1er personnage"
            else -> "Choisis le 2e personnage (vs ${state.slotA})"
        }
        Text(prompt, color = SmatchupColors.TextDim, style = MaterialTheme.typography.bodyLarge)

        if (state.isLoading) {
            LoadingOrb()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = state.roster, key = { it.id }) { c ->
                    OrbCard(label = c.name, onClick = { viewModel.pick(c.id) })
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/matchup/MatchupPickerScreen.kt
  git commit -m "feat(ui): add MatchupPickerScreen with two-step selection"
  ```

---

## Task 9: `MatchupDetailScreen`

Header (two portraits + VS + win-rate bar or token banner) fixed at top, then the tab bar, then the selected paired-section panel.

**Files:** `app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailScreen.kt`.

- [ ] **Step 1: Create.**

```kotlin
package com.example.smatchup.ui.matchup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Matchup
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PairedSplit
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.components.SectionCard
import com.example.smatchup.ui.components.TokenGatedBanner
import com.example.smatchup.ui.components.WinBar
import com.example.smatchup.ui.components.YouTubePlayer
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun MatchupDetailScreen(
    viewModel: MatchupDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.wolBackground()) {
        when {
            state.isLoading && state.matchup == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.error != null ->
                EmptyState(message = "Erreur : ${state.error}", ctaText = "Retour", onCta = onBack)
            state.matchup != null -> {
                val mu = state.matchup!!
                Column(modifier = Modifier.fillMaxSize()) {
                    Header(mu = mu, winRateA = state.winRateA, sample = state.winrateSample, tokenGated = state.winrateTokenGated)
                    TabBar(selected = state.selectedTab, onSelect = viewModel::selectTab)
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (state.selectedTab) {
                            MatchupDetailTab.GAMEPLAN -> SectionCard("Gameplan") {
                                PairedSplit(mu.charA, mu.gameplanA, mu.charB, mu.gameplanB)
                            }
                            MatchupDetailTab.STRONG -> SectionCard("Coups forts") {
                                PairedSplit(mu.charA, mu.strongMovesA, mu.charB, mu.strongMovesB)
                            }
                            MatchupDetailTab.PUNISH -> SectionCard("Punissables") {
                                PairedSplit(mu.charA, mu.punishableMovesA, mu.charB, mu.punishableMovesB)
                            }
                            MatchupDetailTab.STAGES -> SectionCard("Stages") {
                                PairedSplit(
                                    mu.charA, mu.stagesForA.map { "${it.verdict.name.lowercase()}: ${it.displayName}" },
                                    mu.charB, mu.stagesForB.map { "${it.verdict.name.lowercase()}: ${it.displayName}" },
                                )
                            }
                            MatchupDetailTab.VIDEO -> {
                                if (state.lastVideoUrl != null) {
                                    if (state.lastVideoTitle != null) {
                                        Text(state.lastVideoTitle!!, style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
                                    }
                                    YouTubePlayer(videoIdOrUrl = state.lastVideoUrl!!)
                                } else {
                                    EmptyState(message = "Aucune VOD récente trouvée.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(mu: Matchup, winRateA: Float?, sample: Int, tokenGated: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            CharCol(mu.charA)
            Text("VS", style = MaterialTheme.typography.displayMedium, color = SmatchupColors.Gold)
            CharCol(mu.charB)
        }
        when {
            winRateA != null -> WinBar(percentA = winRateA, sample = sample)
            tokenGated -> TokenGatedBanner(
                feature = "Win-rate",
                instructions = "Ajoute START_GG_TOKEN dans local.properties pour activer le calcul de win-rate.",
            )
        }
    }
}

@Composable
private fun CharCol(charId: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PortraitOrb(charId = charId, size = 56.dp)
        Text(charId.replaceFirstChar { it.uppercase() }, color = SmatchupColors.Text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TabBar(selected: MatchupDetailTab, onSelect: (MatchupDetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SmatchupColors.Bg1.copy(alpha = 0.92f))
            .border(1.dp, SmatchupColors.Purple.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState()),
    ) {
        MatchupDetailTab.entries.forEach { tab ->
            Text(
                text = tab.label.uppercase(),
                color = if (tab == selected) SmatchupColors.Gold else SmatchupColors.TextDim,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { onSelect(tab) }.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailScreen.kt
  git commit -m "feat(ui): add MatchupDetailScreen (dual header + winbar + paired sections)"
  ```

---

## Task 10: Wire `ViewModelFactory` + `NavGraph`

**Files:** modify `ViewModelFactory.kt`, `NavGraph.kt`.

- [ ] **Step 1: Register VMs in `ViewModelFactory.kt`.** Add to the `create()` `when`:
  ```kotlin
        MatchupPickerViewModel::class.java -> MatchupPickerViewModel(container.characterRepository) as T
  ```
  Add the import `import com.example.smatchup.ui.matchup.MatchupPickerViewModel`.

  Add a parameterized builder method (mirror `characterDetail`):
  ```kotlin
    fun matchupDetail(charA: String, charB: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == com.example.smatchup.ui.matchup.MatchupDetailViewModel::class.java)
                return com.example.smatchup.ui.matchup.MatchupDetailViewModel(
                    charA = charA,
                    charB = charB,
                    matchupRepo = container.matchupRepository,
                    winrateComputer = container.winrateComputer,
                    bestPlayerRepo = container.bestPlayerRepository,
                    youTubeApi = container.youtubeApi,
                ) as T
            }
        }
  ```

- [ ] **Step 2: Wire `NavGraph.kt`.** Add imports:
  ```kotlin
  import com.example.smatchup.ui.matchup.MatchupDetailScreen
  import com.example.smatchup.ui.matchup.MatchupPickerScreen
  ```
  Replace the `MatchupPicker` placeholder block:
  ```kotlin
        composable(Screen.MatchupPicker.route) {
            MatchupPickerScreen(onPairReady = { a, b ->
                nav.navigate(Screen.MatchupDetail.buildRoute(a, b))
            })
        }
  ```
  Replace the `MatchupDetail` placeholder block:
  ```kotlin
        composable(
            route = Screen.MatchupDetail.route,
            arguments = listOf(
                navArgument(Screen.MatchupDetail.ARG_CHAR_A) { type = NavType.StringType },
                navArgument(Screen.MatchupDetail.ARG_CHAR_B) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val a = backStackEntry.arguments?.getString(Screen.MatchupDetail.ARG_CHAR_A).orEmpty()
            val b = backStackEntry.arguments?.getString(Screen.MatchupDetail.ARG_CHAR_B).orEmpty()
            MatchupDetailScreen(
                onBack = { nav.popBackStack() },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ViewModelFactory.fromApp().matchupDetail(a, b),
                    key = "matchup-detail-$a-$b",
                ),
            )
        }
  ```

- [ ] **Step 3: Build + install + smoke test.**
  ```bash
  ./gradlew :app:installDebug
  ```
  Launch → Home → Match-up → pick Sonic → pick Steve. Confirm:
  1. Detail header shows both portraits + "VS".
  2. Win-rate area shows the `TokenGatedBanner` (no START_GG_TOKEN).
  3. Gameplan tab shows the paired split (Sonic | Steve).
  4. Strong / Punish / Stages tabs show their paired splits.
  5. Video tab shows "Aucune VOD récente" or the player (if YOUTUBE_API_KEY set).
  6. Back returns to the picker.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt \
          app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt
  git commit -m "feat(nav): wire MatchupPicker + MatchupDetail screens"
  ```

---

## Final verification

- [ ] **Step 1: JVM suite.** `./gradlew :app:testDebugUnitTest`
- [ ] **Step 2: Instrumented suite (single run, foreground).** `./gradlew :app:connectedDebugAndroidTest`
- [ ] **Step 3: APK.** `./gradlew :app:assembleDebug`
- [ ] **Step 4: Manual smoke (Task 10 step 3).**
- [ ] **Step 5: `git log --oneline -12` check.**

---

## Self-review

**Spec coverage:**

| Spec section | Item | Task |
|---|---|---|
| §8 (layout B) | Paired section cards | 5, 9 |
| §8 | Win-rate bar in header | 5 (WinBar), 9 |
| §6.6 | Per-MU winrate aggregation + cache | 3, 4 |
| §6.4 | Token gating (Unauthorized → banner) | 4, 7, 9 |
| §3 | MatchupPicker (2 portraits) | 6, 8 |
| §6.7 | Bundled matchup JSON | 1, 2 |
| §8 (video) | Last clash video | 7 (loadVideo), 9 (VideoTab) |

**Placeholder scan:** none. The `WinrateComputer.fetchSets` in `AppContainer` is a documented stub that returns `Unauthorized`/empty until a start.gg token is present — this is the honest current capability, not a plan placeholder. The malformed test name in Task 6 step 1 is explicitly corrected inline (use the rewritten `picking same character twice keeps slotB null`).

**Type consistency:**
- `WinrateResult(winRateA, sampleSize)` consistent across Tasks 3, 4, 7.
- `WinrateAggregator.SetResult(winnerIsA)` consistent across Tasks 3, 4.
- `WinrateComputer(cacheDao, cacheManager, fetchSets)` constructor consistent in Tasks 4 + AppContainer wiring.
- `MatchupDetailViewModel(charA, charB, matchupRepo, winrateComputer, bestPlayerRepo, youTubeApi)` consistent in Tasks 7 + 10.
- `MatchupPickerViewModel(repo)` / `(loadRoster)` consistent in Tasks 6 + 10.
- `Matchup` field names (gameplanA/B, strongMovesA/B, punishableMovesA/B, stagesForA/B) match the domain model from sub-project 1 and are used identically in Tasks 2, 7, 9.
- `MatchupDetailTab` entries match the `when` in `MatchupDetailScreen` (Tasks 7 + 9).

Plan ready.
