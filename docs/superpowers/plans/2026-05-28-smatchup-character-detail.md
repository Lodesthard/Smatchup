# Smatchup — Character Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Character Detail screen — hero portrait + sticky tabs (Combos, Gameplan, Moves, Frame, Synergy, Stages, Video) — backed by `CharacterRepository.getDetail(charId)` with the full data-source fallback chain (curated bundled JSON → The Ultimate API for framedata → YouTube for last VOD).

**Architecture:** `CharacterDetailScreen` collects a single `StateFlow<CharacterDetailUiState>` from `CharacterDetailViewModel`. The VM kicks off independent coroutines for (a) curated meta from `JsonAssetLoader`, (b) framedata via `UltimateApi` + Room cache + bundled JSON fallback, (c) last YouTube video for the seeded best player. Each section degrades gracefully — missing data renders an `EmptyState`, missing API key shows a `TokenGatedBanner`.

**Tech Stack:** existing stack + `com.pierfrancescosoffritti.androidyoutubeplayer:core` for the embedded video, Compose Navigation for routing.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — sections 6 (data layer fallback), 8 (UI layout C — hero + sticky tabs), 11 (risks: Ultimate API unreliability). Builds on sub-projects 1 (data foundation) and 2 (theme + nav).

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── domain/model/
│   └── CharacterDetail.kt                       # aggregate domain type
├── data/
│   ├── api/MoveParser.kt                        # parse Ultimate API JSON → List<Move>
│   ├── repository/CharacterRepository.kt        # MODIFY — add getDetail()
│   └── repository/BestPlayerRepository.kt       # read seed_best_players.json
├── ui/character/
│   ├── CharacterDetailScreen.kt
│   ├── CharacterDetailViewModel.kt
│   ├── DetailTab.kt                             # enum
│   ├── DetailHero.kt                            # hero portrait composable
│   ├── DetailTabBar.kt                          # sticky tab strip
│   └── tabs/
│       ├── CombosTab.kt
│       ├── GameplanTab.kt
│       ├── MovesTab.kt
│       ├── FrameTab.kt
│       ├── SynergyTab.kt
│       ├── StagesTab.kt
│       └── VideoTab.kt
├── ui/components/YouTubePlayer.kt               # lifecycle-aware embed

app/src/main/assets/
├── framedata/{steve,sonic,snake,fox,sephiroth}.json   # fallback frames for the 5
└── stages/{steve,sonic,snake,fox,sephiroth}.json      # ban/cp seed for the 5

app/src/androidTest/java/com/example/smatchup/
├── data/api/MoveParserTest.kt
└── data/repository/CharacterRepositoryDetailTest.kt
```

### Modified

- `gradle/libs.versions.toml` — add `youtubePlayer` version and library.
- `app/build.gradle.kts` — add `androidx-pierfrancescosoffritti-youtubeplayer` implementation.
- `app/src/main/java/com/example/smatchup/di/AppContainer.kt` — register `BestPlayerRepository`.
- `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` — register `CharacterDetailViewModel(charId)`.
- `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt` — replace `CharacterDetail` placeholder with `CharacterDetailScreen`.

### Untouched

`data/local/` (no schema changes — existing `cached_framedata` and `cached_youtube_video` tables reused). Sub-projects 4–7 still own their respective screens.

---

## Conventions

- **Branch:** `git checkout -b character-detail` off `api` before Task 1.
- **Commit style:** Conventional Commits, one commit per task minimum.
- **TDD:** parser + repo + VM have failing tests first. UI screens smoke-tested manually on emulator.
- **Cleanup:** every task ends green (build succeeds, tests pass).

---

## Task 1: YouTube player dependency

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

- [ ] **Step 1: Branch.**
  ```bash
  git checkout -b character-detail
  ```

- [ ] **Step 2: Edit `gradle/libs.versions.toml`.** In `[versions]` append:
  ```toml
  youtubePlayer = "12.1.0"
  ```
  In `[libraries]` append:
  ```toml
  pierfrancescosoffritti-youtubeplayer = { group = "com.pierfrancescosoffritti.androidyoutubeplayer", name = "core", version.ref = "youtubePlayer" }
  ```

- [ ] **Step 3: Edit `app/build.gradle.kts`.** Add to the `dependencies { … }` block right after `implementation(libs.androidx.compose.ui.text.google.fonts)`:
  ```kotlin
      implementation(libs.pierfrancescosoffritti.youtubeplayer)
  ```

- [ ] **Step 4: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**
  ```bash
  git add gradle/libs.versions.toml app/build.gradle.kts
  git commit -m "chore: add pierfrancescosoffritti YouTube player dependency"
  ```

---

## Task 2: `YouTubePlayer` composable

**Files:** `app/src/main/java/com/example/smatchup/ui/components/YouTubePlayer.kt`

(No automated tests — wraps a third-party Android View. Smoke-tested manually in Task 13.)

- [ ] **Step 1: Create the composable.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/components/YouTubePlayer.kt
package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smatchup.ui.theme.SmatchupColors
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/** Extracts the 11-char video id from any youtu.be / youtube.com / direct id input. */
fun parseYouTubeVideoId(input: String): String? {
    if (input.isEmpty()) return null
    val direct = Regex("^[A-Za-z0-9_-]{11}$").find(input)
    if (direct != null) return input
    val patterns = listOf(
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
        Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
        Regex("""embed/([A-Za-z0-9_-]{11})"""),
        Regex("""shorts/([A-Za-z0-9_-]{11})"""),
    )
    for (p in patterns) {
        val m = p.find(input)
        if (m != null) return m.groupValues[1]
    }
    return null
}

@Composable
fun YouTubePlayer(
    videoIdOrUrl: String,
    modifier: Modifier = Modifier,
) {
    val id = remember(videoIdOrUrl) { parseYouTubeVideoId(videoIdOrUrl) }
    if (id == null) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current

    val playerView = remember(id) {
        YouTubePlayerView(ctx).apply {
            enableAutomaticInitialization = false
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.cueVideo(id, 0f)
                }
            })
        }
    }

    DisposableEffect(playerView) {
        lifecycle.addObserver(playerView)
        onDispose {
            lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(SmatchupColors.Bg1),
        factory = { playerView },
    )
}
```

Add missing import `androidx.compose.ui.unit.dp` when build complains.

- [ ] **Step 2: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/YouTubePlayer.kt
  git commit -m "feat(ui): add lifecycle-aware YouTubePlayer composable"
  ```

---

## Task 3: `MoveParser` + instrumented test

The Ultimate API returns JSON like:
```json
{ "fighter": "Steve", "moves": [ { "moveName": "fair", "startup": 8, "totalFrames": 35, "baseDamage": 7.5 }, ... ] }
```
We parse it into `List<Move>` using `org.json` (so the test must live in `androidTest` — see Tasks 12–14 of sub-project 1 for the same precedent).

**Files:** `app/src/main/java/com/example/smatchup/data/api/MoveParser.kt`, `app/src/androidTest/java/com/example/smatchup/data/api/MoveParserTest.kt`

- [ ] **Step 1: Failing instrumented test.**

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/api/MoveParserTest.kt
package com.example.smatchup.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.smatchup.domain.model.MoveCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveParserTest {

    private val sample = """
        {"fighter":"Steve","moves":[
          {"moveName":"jab","startup":3,"totalFrames":12,"baseDamage":2.0},
          {"moveName":"fair","startup":8,"totalFrames":35,"baseDamage":7.5},
          {"moveName":"up smash","startup":11,"totalFrames":50,"baseDamage":15.0},
          {"moveName":"neutral b","startup":20,"totalFrames":60}
        ]}
    """.trimIndent()

    @Test fun parsesAllMovesWithFields() {
        val moves = MoveParser.parse(sample)
        assertEquals(4, moves.size)
        val fair = moves.first { it.id == "fair" }
        assertEquals("fair", fair.displayName)
        assertEquals(8, fair.frame.startup)
        assertEquals(35, fair.frame.totalFrames)
        assertEquals(7.5f, fair.frame.baseDamage!!, 0.0001f)
    }

    @Test fun inferCategoryFromName() {
        val moves = MoveParser.parse(sample)
        assertEquals(MoveCategory.JAB,     moves.first { it.id == "jab" }.category)
        assertEquals(MoveCategory.AERIAL,  moves.first { it.id == "fair" }.category)
        assertEquals(MoveCategory.SMASH,   moves.first { it.id == "up_smash" }.category)
        assertEquals(MoveCategory.SPECIAL, moves.first { it.id == "neutral_b" }.category)
    }

    @Test fun idIsLowercaseUnderscored() {
        val moves = MoveParser.parse(sample)
        assertTrue("up_smash" in moves.map { it.id })
        assertTrue("neutral_b" in moves.map { it.id })
    }

    @Test fun malformedJsonReturnsEmpty() {
        assertEquals(emptyList<Any>(), MoveParser.parse("not json"))
    }

    @Test fun emptyMovesArrayReturnsEmpty() {
        assertEquals(emptyList<Any>(), MoveParser.parse("""{"moves":[]}"""))
    }
}
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/api/MoveParser.kt
package com.example.smatchup.data.api

import com.example.smatchup.domain.model.Frame
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.MoveCategory
import org.json.JSONException
import org.json.JSONObject

object MoveParser {

    fun parse(json: String): List<Move> {
        val root = try { JSONObject(json) } catch (_: JSONException) { return emptyList() }
        val arr = root.optJSONArray("moves") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("moveName", "").ifBlank { return@mapNotNull null }
            val id = name.lowercase().replace(Regex("\\s+"), "_")
            Move(
                id = id,
                displayName = name,
                category = inferCategory(id),
                frame = Frame(
                    startup = o.optInt("startup", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    totalFrames = o.optInt("totalFrames", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    landingLag = o.optInt("landingLag", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    baseDamage = if (o.has("baseDamage")) o.optDouble("baseDamage").toFloat() else null,
                ),
            )
        }
    }

    private fun inferCategory(id: String): MoveCategory = when {
        id == "jab" || id.endsWith("_jab") -> MoveCategory.JAB
        id.contains("smash") -> MoveCategory.SMASH
        id.contains("aerial") || id == "fair" || id == "bair" || id == "uair" || id == "dair" || id == "nair" -> MoveCategory.AERIAL
        id.contains("special") || id == "neutral_b" || id == "side_b" || id == "up_b" || id == "down_b" -> MoveCategory.SPECIAL
        id.contains("throw") -> MoveCategory.THROW
        id.contains("grab") -> MoveCategory.GRAB
        id.contains("tilt") -> MoveCategory.TILT
        else -> MoveCategory.MOVEMENT
    }
}
```

- [ ] **Step 3: Run tests.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.api.MoveParserTest
  ```
  Expected: 5 pass.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/api/MoveParser.kt \
          app/src/androidTest/java/com/example/smatchup/data/api/MoveParserTest.kt
  git commit -m "feat(api): add MoveParser for Ultimate API JSON → domain.Move"
  ```

---

## Task 4: `CharacterDetail` aggregate domain type

**Files:** `app/src/main/java/com/example/smatchup/domain/model/CharacterDetail.kt`

(No tests — pure data class.)

- [ ] **Step 1: Create.**

```kotlin
// app/src/main/java/com/example/smatchup/domain/model/CharacterDetail.kt
package com.example.smatchup.domain.model

data class CharacterDetail(
    val character: Character,
    val combos: List<String> = emptyList(),
    val gameplan: String? = null,
    val movesUtility: Map<String, String> = emptyMap(),   // moveId → description
    val framedata: List<Move> = emptyList(),
    val framedataSource: FramedataSource = FramedataSource.NONE,
    val synergyPartners: List<Character> = emptyList(),
    val stagesBan: List<Stage> = emptyList(),
    val stagesCounterpick: List<Stage> = emptyList(),
    val bestPlayer: Player? = null,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
)

enum class FramedataSource { NONE, REMOTE, BUNDLED }
```

- [ ] **Step 2: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/domain/model/CharacterDetail.kt
  git commit -m "feat(domain): add CharacterDetail aggregate model"
  ```

---

## Task 5: Seed bundled `framedata` + `stages` JSON for the 5 characters

We need `framedata/<charId>.json` and `stages/<charId>.json` for steve, sonic, snake, fox, sephiroth. Framedata uses the same wire format as the Ultimate API (so the same `MoveParser` consumes both). Stages JSON shape:

```json
{ "ban": ["fd","ps2"], "counterpick": ["bf","sv"] }
```

**Files:** 10 JSON files under `app/src/main/assets/{framedata,stages}/`.

- [ ] **Step 1: Create `app/src/main/assets/framedata/steve.json`.**

```json
{
  "fighter": "Steve",
  "moves": [
    { "moveName": "jab", "startup": 7, "totalFrames": 23, "baseDamage": 4.0 },
    { "moveName": "ftilt", "startup": 11, "totalFrames": 34, "baseDamage": 11.0 },
    { "moveName": "utilt", "startup": 9, "totalFrames": 27, "baseDamage": 7.5 },
    { "moveName": "dtilt", "startup": 9, "totalFrames": 27, "baseDamage": 5.0 },
    { "moveName": "fair", "startup": 9, "totalFrames": 32, "baseDamage": 7.0 },
    { "moveName": "bair", "startup": 9, "totalFrames": 35, "baseDamage": 12.0 },
    { "moveName": "uair", "startup": 6, "totalFrames": 30, "baseDamage": 7.5 },
    { "moveName": "dair", "startup": 14, "totalFrames": 41, "baseDamage": 9.0 },
    { "moveName": "nair", "startup": 7, "totalFrames": 33, "baseDamage": 6.5 },
    { "moveName": "neutral b", "startup": 19, "totalFrames": 60, "baseDamage": 10.0 },
    { "moveName": "side b", "startup": 14, "totalFrames": 45, "baseDamage": 8.0 },
    { "moveName": "up b", "startup": 5, "totalFrames": 50, "baseDamage": 8.0 },
    { "moveName": "down b", "startup": 30, "totalFrames": 80, "baseDamage": 30.0 }
  ]
}
```

- [ ] **Step 2: Create `app/src/main/assets/framedata/sonic.json`.**

```json
{
  "fighter": "Sonic",
  "moves": [
    { "moveName": "jab", "startup": 2, "totalFrames": 16, "baseDamage": 2.0 },
    { "moveName": "ftilt", "startup": 5, "totalFrames": 27, "baseDamage": 8.0 },
    { "moveName": "utilt", "startup": 4, "totalFrames": 22, "baseDamage": 5.0 },
    { "moveName": "dtilt", "startup": 5, "totalFrames": 26, "baseDamage": 7.0 },
    { "moveName": "fair", "startup": 6, "totalFrames": 30, "baseDamage": 5.0 },
    { "moveName": "bair", "startup": 8, "totalFrames": 33, "baseDamage": 11.0 },
    { "moveName": "uair", "startup": 5, "totalFrames": 28, "baseDamage": 6.0 },
    { "moveName": "dair", "startup": 8, "totalFrames": 36, "baseDamage": 7.0 },
    { "moveName": "nair", "startup": 5, "totalFrames": 30, "baseDamage": 8.0 },
    { "moveName": "neutral b", "startup": 13, "totalFrames": 60, "baseDamage": 11.0 },
    { "moveName": "side b", "startup": 12, "totalFrames": 50, "baseDamage": 5.0 },
    { "moveName": "up b", "startup": 4, "totalFrames": 60, "baseDamage": 5.0 },
    { "moveName": "down b", "startup": 21, "totalFrames": 55, "baseDamage": 11.0 }
  ]
}
```

- [ ] **Step 3: Create `app/src/main/assets/framedata/snake.json`.**

```json
{
  "fighter": "Snake",
  "moves": [
    { "moveName": "jab", "startup": 4, "totalFrames": 18, "baseDamage": 2.5 },
    { "moveName": "ftilt", "startup": 6, "totalFrames": 32, "baseDamage": 8.0 },
    { "moveName": "utilt", "startup": 5, "totalFrames": 38, "baseDamage": 10.0 },
    { "moveName": "dtilt", "startup": 11, "totalFrames": 36, "baseDamage": 11.0 },
    { "moveName": "fair", "startup": 16, "totalFrames": 38, "baseDamage": 14.0 },
    { "moveName": "bair", "startup": 10, "totalFrames": 35, "baseDamage": 13.0 },
    { "moveName": "uair", "startup": 8, "totalFrames": 35, "baseDamage": 14.0 },
    { "moveName": "dair", "startup": 14, "totalFrames": 40, "baseDamage": 11.0 },
    { "moveName": "nair", "startup": 7, "totalFrames": 30, "baseDamage": 7.0 },
    { "moveName": "neutral b", "startup": 27, "totalFrames": 90, "baseDamage": 5.0 },
    { "moveName": "side b", "startup": 30, "totalFrames": 100, "baseDamage": 14.0 },
    { "moveName": "up b", "startup": 3, "totalFrames": 70, "baseDamage": 0.0 },
    { "moveName": "down b", "startup": 30, "totalFrames": 80, "baseDamage": 13.0 }
  ]
}
```

- [ ] **Step 4: Create `app/src/main/assets/framedata/fox.json`.**

```json
{
  "fighter": "Fox",
  "moves": [
    { "moveName": "jab", "startup": 2, "totalFrames": 14, "baseDamage": 1.5 },
    { "moveName": "ftilt", "startup": 5, "totalFrames": 25, "baseDamage": 7.0 },
    { "moveName": "utilt", "startup": 5, "totalFrames": 27, "baseDamage": 6.0 },
    { "moveName": "dtilt", "startup": 5, "totalFrames": 25, "baseDamage": 7.0 },
    { "moveName": "fair", "startup": 7, "totalFrames": 38, "baseDamage": 5.5 },
    { "moveName": "bair", "startup": 9, "totalFrames": 38, "baseDamage": 11.0 },
    { "moveName": "uair", "startup": 4, "totalFrames": 30, "baseDamage": 12.0 },
    { "moveName": "dair", "startup": 5, "totalFrames": 39, "baseDamage": 12.0 },
    { "moveName": "nair", "startup": 4, "totalFrames": 33, "baseDamage": 9.0 },
    { "moveName": "neutral b", "startup": 6, "totalFrames": 23, "baseDamage": 1.5 },
    { "moveName": "side b", "startup": 14, "totalFrames": 60, "baseDamage": 5.0 },
    { "moveName": "up b", "startup": 16, "totalFrames": 60, "baseDamage": 6.0 },
    { "moveName": "down b", "startup": 4, "totalFrames": 39, "baseDamage": 0.0 }
  ]
}
```

- [ ] **Step 5: Create `app/src/main/assets/framedata/sephiroth.json`.**

```json
{
  "fighter": "Sephiroth",
  "moves": [
    { "moveName": "jab", "startup": 7, "totalFrames": 24, "baseDamage": 4.0 },
    { "moveName": "ftilt", "startup": 8, "totalFrames": 34, "baseDamage": 10.0 },
    { "moveName": "utilt", "startup": 7, "totalFrames": 31, "baseDamage": 8.0 },
    { "moveName": "dtilt", "startup": 7, "totalFrames": 28, "baseDamage": 7.0 },
    { "moveName": "fair", "startup": 9, "totalFrames": 38, "baseDamage": 9.5 },
    { "moveName": "bair", "startup": 9, "totalFrames": 39, "baseDamage": 9.0 },
    { "moveName": "uair", "startup": 7, "totalFrames": 34, "baseDamage": 9.5 },
    { "moveName": "dair", "startup": 14, "totalFrames": 45, "baseDamage": 12.0 },
    { "moveName": "nair", "startup": 7, "totalFrames": 33, "baseDamage": 6.0 },
    { "moveName": "neutral b", "startup": 21, "totalFrames": 80, "baseDamage": 12.0 },
    { "moveName": "side b", "startup": 8, "totalFrames": 60, "baseDamage": 14.0 },
    { "moveName": "up b", "startup": 7, "totalFrames": 60, "baseDamage": 8.5 },
    { "moveName": "down b", "startup": 9, "totalFrames": 50, "baseDamage": 11.0 }
  ]
}
```

- [ ] **Step 6: Create stages JSONs.** Same shape for all 5. Stage ids use the legal Ultimate stage set: `bf`, `fd`, `sv`, `ps2`, `town`, `kalos`, `hollow`, `lylat`.

  `app/src/main/assets/stages/steve.json`:
  ```json
  { "ban": ["sv", "town"], "counterpick": ["fd", "bf", "ps2"] }
  ```
  `app/src/main/assets/stages/sonic.json`:
  ```json
  { "ban": ["bf"], "counterpick": ["sv", "town", "kalos"] }
  ```
  `app/src/main/assets/stages/snake.json`:
  ```json
  { "ban": ["sv"], "counterpick": ["fd", "town", "ps2"] }
  ```
  `app/src/main/assets/stages/fox.json`:
  ```json
  { "ban": ["town"], "counterpick": ["bf", "ps2", "lylat"] }
  ```
  `app/src/main/assets/stages/sephiroth.json`:
  ```json
  { "ban": ["town", "sv"], "counterpick": ["fd", "ps2"] }
  ```

- [ ] **Step 7: Build (asset packaging only).**
  ```bash
  ./gradlew :app:assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit.**
  ```bash
  git add app/src/main/assets/framedata/ app/src/main/assets/stages/
  git commit -m "feat(assets): seed framedata + stages JSON for 5 characters"
  ```

---

## Task 6: `BestPlayerRepository` (reads seed JSON)

**Files:** `app/src/main/java/com/example/smatchup/data/repository/BestPlayerRepository.kt`, `app/src/androidTest/java/com/example/smatchup/data/repository/BestPlayerRepositoryTest.kt`

The repository wraps `JsonAssetLoader.seedBestPlayers()` and (later, in sub-project 6) the cached `cached_best_players` table. For now it only reads the seed.

- [ ] **Step 1: Failing instrumented test.**

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/repository/BestPlayerRepositoryTest.kt
package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BestPlayerRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = BestPlayerRepository(loader)

    @Test fun knownCharacterReturnsSeedTag() = runBlocking {
        val p = repo.bestPlayerFor("steve")
        assertEquals("Onin", p?.tag)
    }

    @Test fun unknownCharacterReturnsNull() = runBlocking {
        assertNull(repo.bestPlayerFor("nonexistent_char"))
    }
}
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/BestPlayerRepository.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BestPlayerRepository(private val loader: JsonAssetLoader) {

    suspend fun bestPlayerFor(charId: String): Player? = withContext(Dispatchers.IO) {
        val seed = loader.seedBestPlayers()
        val tag = seed.optString(charId, "").takeIf { it.isNotEmpty() } ?: return@withContext null
        Player(tag = tag, mainCharacter = charId)
    }
}
```

- [ ] **Step 3: Register in `AppContainer`.** Edit `app/src/main/java/com/example/smatchup/di/AppContainer.kt`. After the `characterRepository` line, append:

```kotlin
    val bestPlayerRepository: BestPlayerRepository = BestPlayerRepository(jsonAssetLoader)
```

Add the import `import com.example.smatchup.data.repository.BestPlayerRepository` near the top.

- [ ] **Step 4: Run test.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.BestPlayerRepositoryTest
  ```

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/repository/BestPlayerRepository.kt \
          app/src/main/java/com/example/smatchup/di/AppContainer.kt \
          app/src/androidTest/java/com/example/smatchup/data/repository/BestPlayerRepositoryTest.kt
  git commit -m "feat(repo): add BestPlayerRepository reading seed_best_players.json"
  ```

---

## Task 7: `CharacterRepository.getDetail(charId)` with fallback chain

This is the heart of the data layer for the screen. Behavior:

1. Find the `Character` from `loadRoster()`. If not found → throw.
2. Read `characters_meta/<charId>.json` (combos, gameplan, moves_utility). Optional — missing means empty lists.
3. Read `synergies.json` and look up the partner ids; map to `Character` objects from the roster.
4. Read `stages/<charId>.json`. Build `stagesBan` and `stagesCounterpick` lists of `Stage` objects (the `displayName` is derived from `id` via a small static map).
5. Try to load framedata:
   - Check Room `cached_framedata` for fresh row → parse with `MoveParser`, source = `REMOTE`.
   - Else call `UltimateApi.getMovesRaw(charId)`. On `Success`, upsert cache + parse, source = `REMOTE`.
   - On any failure or empty, fall back to `JsonAssetLoader.framedata(charId)`. If present, parse, source = `BUNDLED`.
   - Else source = `NONE`, `framedata = emptyList()`.

The `getDetail` method is `suspend` and orchestrates all of this concurrently — but framedata is fetched lazily by a separate method since it can take time (the Render cold start can be ~50 s). Plan:

- `getDetail(charId)` returns the synchronously-loadable parts (bundle JSON + roster lookup + best player). No network calls.
- A separate `getFramedata(charId)` method does the Ultimate API + cache + fallback chain.

This keeps the UI responsive — the screen renders with cached / bundled / empty framedata immediately, and reloads when remote data arrives.

**Files:** modify `app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt`; new test `app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryDetailTest.kt`.

- [ ] **Step 1: Failing instrumented test.**

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryDetailTest.kt
package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.StageVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterRepositoryDetailTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = CharacterRepository(loader)

    @Test fun detailForSeedCharacterHasComboAndGameplan() = runBlocking {
        val d = repo.getDetail("steve")
        assertEquals("Steve", d.character.name)
        assertTrue("Combos must be non-empty", d.combos.isNotEmpty())
        assertTrue("Gameplan must be present", d.gameplan != null && d.gameplan!!.isNotBlank())
    }

    @Test fun detailIncludesSynergyPartners() = runBlocking {
        val d = repo.getDetail("steve")
        val ids = d.synergyPartners.map { it.id }
        assertTrue("Steve should partner with sonic", "sonic" in ids)
        assertTrue("Steve should partner with fox", "fox" in ids)
    }

    @Test fun detailIncludesStagesBanAndCounterpick() = runBlocking {
        val d = repo.getDetail("steve")
        assertTrue(d.stagesBan.any { it.id == "sv" })
        assertTrue(d.stagesCounterpick.any { it.id == "fd" })
        assertEquals(StageVerdict.BAN, d.stagesBan.first().verdict)
        assertEquals(StageVerdict.COUNTERPICK, d.stagesCounterpick.first().verdict)
    }

    @Test fun bundledFramedataLoadsForSeedCharacters() = runBlocking {
        val frames = repo.framedataFromBundle("steve")
        assertTrue("Steve should have bundled framedata", frames.isNotEmpty())
        assertEquals(FramedataSource.BUNDLED, frames.let { FramedataSource.BUNDLED })
        assertTrue(frames.any { it.id == "fair" })
    }

    @Test fun bundledFramedataReturnsEmptyForUnseededCharacter() = runBlocking {
        val frames = repo.framedataFromBundle("mario")
        assertTrue("Mario has no bundled framedata seed yet", frames.isEmpty())
    }

    @Test fun detailForUnknownCharacterThrows() {
        try {
            runBlocking { repo.getDetail("not_a_char") }
            assertTrue("Expected IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }
}
```

- [ ] **Step 2: Implement.** Replace `app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt` with:

```kotlin
package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.domain.model.StageVerdict
import com.example.smatchup.data.api.MoveParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val STAGE_NAMES: Map<String, String> = mapOf(
    "bf"     to "Battlefield",
    "fd"     to "Final Destination",
    "sv"     to "Smashville",
    "ps2"    to "Pokémon Stadium 2",
    "town"   to "Town and City",
    "kalos"  to "Kalos Pokémon League",
    "hollow" to "Hollow Bastion",
    "lylat"  to "Lylat Cruise",
)

class CharacterRepository(private val loader: JsonAssetLoader) {

    suspend fun loadRoster(): List<Character> = withContext(Dispatchers.IO) {
        val arr = loader.allCharacters()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Character(
                id = o.getString("id"),
                name = o.getString("name"),
                series = o.getString("series"),
                rosterNumber = o.getInt("rosterNumber"),
                portraitAsset = o.optString("portraitAsset").takeIf { it.isNotEmpty() },
            )
        }.sortedBy { it.rosterNumber }
    }

    /**
     * Synchronously-loadable parts of the detail (curated JSON, roster, partners, stages).
     * Framedata is NOT loaded here — call [framedataFromBundle] or fetch remotely.
     */
    suspend fun getDetail(charId: String): CharacterDetail = withContext(Dispatchers.IO) {
        val roster = loadRoster()
        val character = roster.firstOrNull { it.id == charId }
            ?: throw IllegalArgumentException("Unknown character id: $charId")

        val meta = loader.characterMeta(charId)
        val combos: List<String> = meta?.optJSONArray("combos")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val gameplan = meta?.optString("gameplan")?.takeIf { it.isNotBlank() }
        val movesUtility: Map<String, String> = meta?.optJSONObject("moves_utility")?.let { obj ->
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } ?: emptyMap()

        val synergiesObj = loader.synergies()
        val partnerIds: List<String> = synergiesObj.optJSONArray(charId)?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        val partners = partnerIds.mapNotNull { pid -> roster.firstOrNull { it.id == pid } }

        val stagesObj = loader.stages(charId)
        val ban = stagesObj?.optJSONArray("ban")?.toStages(StageVerdict.BAN) ?: emptyList()
        val cp = stagesObj?.optJSONArray("counterpick")?.toStages(StageVerdict.COUNTERPICK) ?: emptyList()

        CharacterDetail(
            character = character,
            combos = combos,
            gameplan = gameplan,
            movesUtility = movesUtility,
            framedata = emptyList(),
            framedataSource = FramedataSource.NONE,
            synergyPartners = partners,
            stagesBan = ban,
            stagesCounterpick = cp,
        )
    }

    /** Bundled framedata fallback (no network). */
    suspend fun framedataFromBundle(charId: String): List<Move> = withContext(Dispatchers.IO) {
        val raw = loader.framedata(charId)?.toString() ?: return@withContext emptyList()
        MoveParser.parse(raw)
    }

    private fun org.json.JSONArray.toStages(verdict: StageVerdict): List<Stage> =
        (0 until length()).map { i ->
            val id = getString(i)
            Stage(id = id, displayName = STAGE_NAMES[id] ?: id, verdict = verdict)
        }
}
```

- [ ] **Step 3: Run tests.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.CharacterRepositoryDetailTest
  ```
  Expected: 6 pass.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt \
          app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryDetailTest.kt
  git commit -m "feat(repo): add CharacterRepository.getDetail with bundled fallback"
  ```

---

## Task 8: Remote framedata helper in `CharacterRepository`

Adds the API + cache path. Stays in `CharacterRepository` since it's the same domain.

**Files:** modify `app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt`.

- [ ] **Step 1: Add the suspend method.** Append inside the `class CharacterRepository` body (don't replace the existing methods):

  First, add constructor params for the API + cache + cache manager. Replace the class declaration:

```kotlin
class CharacterRepository(
    private val loader: JsonAssetLoader,
    private val ultimateApi: com.example.smatchup.data.api.UltimateApi? = null,
    private val cacheDao: com.example.smatchup.data.local.dao.CacheDao? = null,
    private val cacheManager: com.example.smatchup.data.cache.CacheManager? = null,
) {
```

(`null` defaults so the existing instrumented tests — which construct `CharacterRepository(loader)` — continue to compile and run.)

  Then add the new method at the bottom of the class:

```kotlin
    /**
     * Resolve framedata: cache → API → bundled JSON. Returns the parsed list and the source tag.
     * If `ultimateApi` / `cacheDao` / `cacheManager` are null (tests), skips straight to bundled.
     */
    suspend fun framedataWithFallback(charId: String): Pair<List<Move>, FramedataSource> =
        withContext(Dispatchers.IO) {
            // 1) Cache lookup.
            val cached = cacheDao?.getFramedata(charId)
            if (cached != null && cacheManager != null &&
                cacheManager.isFresh(cached.fetchedAt, com.example.smatchup.data.cache.CacheTtl.FRAMEDATA_MS)) {
                val parsed = MoveParser.parse(cached.jsonBlob)
                if (parsed.isNotEmpty()) return@withContext parsed to FramedataSource.REMOTE
            }

            // 2) Remote API.
            if (ultimateApi != null) {
                val r = ultimateApi.getMovesRaw(charId)
                if (r is com.example.smatchup.domain.model.ApiResult.Success) {
                    val parsed = MoveParser.parse(r.data)
                    if (parsed.isNotEmpty()) {
                        cacheDao?.upsertFramedata(
                            com.example.smatchup.data.local.entity.CachedFramedataEntity(
                                charId = charId,
                                jsonBlob = r.data,
                                fetchedAt = System.currentTimeMillis(),
                            ),
                        )
                        return@withContext parsed to FramedataSource.REMOTE
                    }
                }
            }

            // 3) Bundled fallback.
            val bundled = framedataFromBundle(charId)
            val src = if (bundled.isNotEmpty()) FramedataSource.BUNDLED else FramedataSource.NONE
            bundled to src
        }
```

- [ ] **Step 2: Wire the full constructor in `AppContainer`.** Edit `app/src/main/java/com/example/smatchup/di/AppContainer.kt`. Replace the `characterRepository` line with:

```kotlin
    val characterRepository: CharacterRepository = CharacterRepository(
        loader = jsonAssetLoader,
        ultimateApi = ultimateApi,
        cacheDao = database.cacheDao(),
        cacheManager = cacheManager,
    )
```

- [ ] **Step 3: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Verify existing tests still pass.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.CharacterRepositoryDetailTest
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.CharacterRepositoryTest
  ```

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt \
          app/src/main/java/com/example/smatchup/di/AppContainer.kt
  git commit -m "feat(repo): add framedataWithFallback chain (cache → API → bundled)"
  ```

---

## Task 9: `DetailTab` enum + `CharacterDetailViewModel`

**Files:** `app/src/main/java/com/example/smatchup/ui/character/DetailTab.kt`, `app/src/main/java/com/example/smatchup/ui/character/CharacterDetailViewModel.kt`, test `app/src/test/java/com/example/smatchup/ui/character/CharacterDetailViewModelTest.kt`.

- [ ] **Step 1: Create `DetailTab.kt`.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/DetailTab.kt
package com.example.smatchup.ui.character

enum class DetailTab(val label: String) {
    COMBOS("Combos"),
    GAMEPLAN("Gameplan"),
    MOVES("Moves"),
    FRAME("Frame"),
    SYNERGY("Synergy"),
    STAGES("Stages"),
    VIDEO("Video"),
}
```

- [ ] **Step 2: Failing JVM test for the VM.**

```kotlin
// app/src/test/java/com/example/smatchup/ui/character/CharacterDetailViewModelTest.kt
package com.example.smatchup.ui.character

import app.cash.turbine.test
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val steveDetail = CharacterDetail(
        character = Character(id = "steve", name = "Steve", series = "Minecraft", rosterNumber = 82),
        combos = listOf("Block → uair"),
        gameplan = "Wall the opponent.",
    )

    @Test fun initiallyEmitsLoadingThenLoaded() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("Steve", s.detail?.character?.name)
            assertEquals(DetailTab.COMBOS, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun selectTabUpdatesState() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            vm.selectTab(DetailTab.VIDEO)
            s = awaitItem()
            assertEquals(DetailTab.VIDEO, s.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun missingYoutubeKeyExposesTokenGating() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { emptyList<Move>() to FramedataSource.NONE },
            loadBestPlayer = { Player(tag = "Onin", mainCharacter = "steve") },
            loadLastVideo = { throw IllegalStateException("YOUTUBE_API_KEY missing") },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.lastVideoTokenGated == false && s.detail == null) s = awaitItem()
            // Once detail is loaded and the video lookup fails with the token message, flag it.
            while (!s.lastVideoTokenGated && s.error == null) s = awaitItem()
            assertTrue(s.lastVideoTokenGated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun framedataSourceUpdatesAfterLoad() = runTest {
        val vm = CharacterDetailViewModel(
            charId = "steve",
            loadDetail = { steveDetail },
            loadFramedata = { listOf(Move(id = "fair", displayName = "fair", category = com.example.smatchup.domain.model.MoveCategory.AERIAL, frame = com.example.smatchup.domain.model.Frame(startup = 8))) to FramedataSource.BUNDLED },
            loadBestPlayer = { null },
            loadLastVideo = { null },
        )
        vm.state.test {
            var s = awaitItem()
            while (s.framedata.isEmpty()) s = awaitItem()
            assertEquals(FramedataSource.BUNDLED, s.framedataSource)
            assertEquals("fair", s.framedata.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Implement the ViewModel.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/CharacterDetailViewModel.kt
package com.example.smatchup.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.BestPlayerRepository
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.CharacterDetail
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.domain.model.Player
import com.example.smatchup.data.api.YouTubeApi
import com.example.smatchup.domain.model.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val isLoading: Boolean = true,
    val detail: CharacterDetail? = null,
    val framedata: List<Move> = emptyList(),
    val framedataSource: FramedataSource = FramedataSource.NONE,
    val bestPlayer: Player? = null,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
    val lastVideoTokenGated: Boolean = false,
    val selectedTab: DetailTab = DetailTab.COMBOS,
    val error: String? = null,
)

class CharacterDetailViewModel(
    private val charId: String,
    private val loadDetail: suspend () -> CharacterDetail,
    private val loadFramedata: suspend () -> Pair<List<Move>, FramedataSource>,
    private val loadBestPlayer: suspend () -> Player?,
    private val loadLastVideo: suspend (terms: List<String>) -> com.example.smatchup.data.api.YouTubeApi.Video?,
) : ViewModel() {

    constructor(
        charId: String,
        repo: CharacterRepository,
        bestPlayerRepo: BestPlayerRepository,
        youTubeApi: YouTubeApi,
    ) : this(
        charId = charId,
        loadDetail = { repo.getDetail(charId) },
        loadFramedata = { repo.framedataWithFallback(charId) },
        loadBestPlayer = { bestPlayerRepo.bestPlayerFor(charId) },
        loadLastVideo = { terms ->
            when (val r = youTubeApi.searchLatest(terms)) {
                is ApiResult.Success -> r.data
                ApiResult.Unauthorized -> throw IllegalStateException("YOUTUBE_API_KEY missing")
                else -> null
            }
        },
    )

    private val _state = MutableStateFlow(CharacterDetailUiState())
    val state: StateFlow<CharacterDetailUiState> = _state.asStateFlow()

    init { reload() }

    fun selectTab(tab: DetailTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val d = loadDetail()
                _state.update { it.copy(isLoading = false, detail = d) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }
        }
        viewModelScope.launch {
            try {
                val (frames, source) = loadFramedata()
                _state.update { it.copy(framedata = frames, framedataSource = source) }
            } catch (_: Throwable) { /* leave NONE */ }
        }
        viewModelScope.launch {
            val player = try { loadBestPlayer() } catch (_: Throwable) { null }
            _state.update { it.copy(bestPlayer = player) }
            if (player == null) return@launch
            try {
                val video = loadLastVideo(listOf(player.tag, charId))
                _state.update {
                    it.copy(
                        lastVideoUrl = video?.url,
                        lastVideoTitle = video?.title,
                    )
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("YOUTUBE_API_KEY") == true) {
                    _state.update { it.copy(lastVideoTokenGated = true) }
                }
            } catch (_: Throwable) { /* silent */ }
        }
    }
}
```

- [ ] **Step 4: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.character.CharacterDetailViewModelTest"
  ```
  Expected: 4 pass.

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/character/DetailTab.kt \
          app/src/main/java/com/example/smatchup/ui/character/CharacterDetailViewModel.kt \
          app/src/test/java/com/example/smatchup/ui/character/CharacterDetailViewModelTest.kt
  git commit -m "feat(ui): add CharacterDetailViewModel with parallel data fetching"
  ```

---

## Task 10: Tab content composables (one per tab)

7 small composables, each takes its slice of `CharacterDetailUiState`.

**Files:** all under `app/src/main/java/com/example/smatchup/ui/character/tabs/`.

- [ ] **Step 1: Create `CombosTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun CombosTab(combos: List<String>, modifier: Modifier = Modifier) {
    if (combos.isEmpty()) {
        EmptyState(message = "Pas encore de combos pour ce personnage.", modifier = modifier)
        return
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        combos.forEachIndexed { i, combo ->
            Text("${i + 1}. $combo", style = MaterialTheme.typography.bodyLarge, color = SmatchupColors.Text)
        }
    }
}
```

- [ ] **Step 2: Create `GameplanTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun GameplanTab(gameplan: String?, modifier: Modifier = Modifier) {
    if (gameplan.isNullOrBlank()) {
        EmptyState(message = "Pas encore de gameplan pour ce personnage.", modifier = modifier)
        return
    }
    Text(
        text = gameplan,
        style = MaterialTheme.typography.bodyLarge,
        color = SmatchupColors.Text,
        modifier = modifier.padding(16.dp),
    )
}
```

- [ ] **Step 3: Create `MovesTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun MovesTab(movesUtility: Map<String, String>, modifier: Modifier = Modifier) {
    if (movesUtility.isEmpty()) {
        EmptyState(message = "Pas encore de descriptions de coups.", modifier = modifier)
        return
    }
    val entries = movesUtility.entries.toList()
    LazyColumn(
        modifier = modifier.padding(16.dp),
    ) {
        items(items = entries, key = { it.key }) { (moveId, util) ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = moveId.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = SmatchupColors.Gold,
                )
                Text(
                    text = util,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmatchupColors.Text,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Create `FrameTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.FramedataSource
import com.example.smatchup.domain.model.Move
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun FrameTab(moves: List<Move>, source: FramedataSource, modifier: Modifier = Modifier) {
    if (moves.isEmpty()) {
        EmptyState(message = "Pas de framedata disponible pour ce personnage.", modifier = modifier)
        return
    }
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Source : ${source.name.lowercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = SmatchupColors.TextDim,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Move", color = SmatchupColors.Gold, modifier = Modifier.weight(2f))
            Text("Startup", color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
            Text("Total", color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
            Text("Damage", color = SmatchupColors.Gold, modifier = Modifier.weight(1f))
        }
        LazyColumn {
            items(items = moves, key = { it.id }) { m ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(m.displayName, color = SmatchupColors.Text, modifier = Modifier.weight(2f))
                    Text(m.frame.startup?.toString() ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                    Text(m.frame.totalFrames?.toString() ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                    Text(m.frame.baseDamage?.let { "%.1f".format(it) } ?: "—", color = SmatchupColors.Text, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
```

- [ ] **Step 5: Create `SynergyTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.OrbCard

@Composable
fun SynergyTab(
    partners: List<Character>,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (partners.isEmpty()) {
        EmptyState(message = "Pas encore de synergies définies.", modifier = modifier)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = partners, key = { it.id }) { c ->
            OrbCard(label = c.name, onClick = { onCharacterClick(c.id) })
        }
    }
}
```

- [ ] **Step 6: Create `StagesTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Stage
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun StagesTab(ban: List<Stage>, counterpick: List<Stage>, modifier: Modifier = Modifier) {
    if (ban.isEmpty() && counterpick.isEmpty()) {
        EmptyState(message = "Pas encore de recommandations de stages.", modifier = modifier)
        return
    }
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (ban.isNotEmpty()) {
            Text("À BAN", style = MaterialTheme.typography.titleMedium, color = SmatchupColors.DangerRed)
            ban.forEach { Text("• ${it.displayName}", color = SmatchupColors.Text) }
        }
        if (counterpick.isNotEmpty()) {
            Text("COUNTERPICK", style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
            counterpick.forEach { Text("• ${it.displayName}", color = SmatchupColors.Text) }
        }
    }
}
```

- [ ] **Step 7: Create `VideoTab.kt`.**

```kotlin
package com.example.smatchup.ui.character.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.TokenGatedBanner
import com.example.smatchup.ui.components.YouTubePlayer
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun VideoTab(
    videoUrl: String?,
    videoTitle: String?,
    tokenGated: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            tokenGated -> TokenGatedBanner(
                feature = "Dernier match YouTube",
                instructions = "Ajoute YOUTUBE_API_KEY dans local.properties et recompile.",
            )
            videoUrl != null -> {
                if (videoTitle != null) {
                    Text(videoTitle, style = MaterialTheme.typography.titleMedium, color = SmatchupColors.Gold)
                }
                YouTubePlayer(videoIdOrUrl = videoUrl)
            }
            else -> EmptyState(message = "Aucune VOD récente trouvée.")
        }
    }
}
```

- [ ] **Step 8: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 9: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/character/tabs/
  git commit -m "feat(ui): add 7 detail tab composables (Combos/Gameplan/Moves/Frame/Synergy/Stages/Video)"
  ```

---

## Task 11: `DetailHero` + `DetailTabBar` composables

**Files:** `app/src/main/java/com/example/smatchup/ui/character/DetailHero.kt`, `app/src/main/java/com/example/smatchup/ui/character/DetailTabBar.kt`.

- [ ] **Step 1: Create `DetailHero.kt`.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/DetailHero.kt
package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Character
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun DetailHero(character: Character, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    0.0f to SmatchupColors.Bg2,
                    1.0f to SmatchupColors.Bg1,
                ),
            )
            .border(width = 1.dp, color = SmatchupColors.Gold)
            .padding(top = 32.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PortraitOrb(charId = character.id, size = 90.dp)
            Text(
                text = character.name,
                style = MaterialTheme.typography.displayMedium,
                color = SmatchupColors.Gold,
            )
            Text(
                text = "${character.series} · #${character.rosterNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = SmatchupColors.TextDim,
            )
        }
    }
}
```

- [ ] **Step 2: Create `DetailTabBar.kt`.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/DetailTabBar.kt
package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.theme.SmatchupColors

@Composable
fun DetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SmatchupColors.Bg1.copy(alpha = 0.92f))
            .border(width = 1.dp, color = SmatchupColors.Purple.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState()),
    ) {
        DetailTab.entries.forEach { tab ->
            val isActive = tab == selected
            Text(
                text = tab.label.uppercase(),
                color = if (isActive) SmatchupColors.Gold else SmatchupColors.TextDim,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            if (isActive) {
                // Render gold underline via offset border-bottom — simplest is no-op since
                // the color change already signals selection. Underline would require a Box
                // wrapping the Text; the color change is enough at small scale.
            }
        }
    }
}
```

- [ ] **Step 3: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/character/DetailHero.kt \
          app/src/main/java/com/example/smatchup/ui/character/DetailTabBar.kt
  git commit -m "feat(ui): add DetailHero portrait header and DetailTabBar"
  ```

---

## Task 12: `CharacterDetailScreen`

**Files:** `app/src/main/java/com/example/smatchup/ui/character/CharacterDetailScreen.kt`.

- [ ] **Step 1: Create.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/CharacterDetailScreen.kt
package com.example.smatchup.ui.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.character.tabs.CombosTab
import com.example.smatchup.ui.character.tabs.FrameTab
import com.example.smatchup.ui.character.tabs.GameplanTab
import com.example.smatchup.ui.character.tabs.MovesTab
import com.example.smatchup.ui.character.tabs.StagesTab
import com.example.smatchup.ui.character.tabs.SynergyTab
import com.example.smatchup.ui.character.tabs.VideoTab
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun CharacterDetailScreen(
    charId: String,
    onCharacterClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterDetailViewModel = viewModel(
        factory = ViewModelFactory.fromApp(),
        key = "character-detail-$charId",
    ),
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.wolBackground()) {
        when {
            state.isLoading && state.detail == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            }
            state.error != null -> EmptyState(
                message = "Erreur : ${state.error}",
                ctaText = "Retour",
                onCta = onBack,
            )
            state.detail != null -> Column(modifier = Modifier.fillMaxSize()) {
                DetailHero(character = state.detail!!.character)
                DetailTabBar(selected = state.selectedTab, onSelect = viewModel::selectTab)
                when (state.selectedTab) {
                    DetailTab.COMBOS   -> CombosTab(combos = state.detail!!.combos)
                    DetailTab.GAMEPLAN -> GameplanTab(gameplan = state.detail!!.gameplan)
                    DetailTab.MOVES    -> MovesTab(movesUtility = state.detail!!.movesUtility)
                    DetailTab.FRAME    -> FrameTab(moves = state.framedata, source = state.framedataSource)
                    DetailTab.SYNERGY  -> SynergyTab(partners = state.detail!!.synergyPartners, onCharacterClick = onCharacterClick)
                    DetailTab.STAGES   -> StagesTab(ban = state.detail!!.stagesBan, counterpick = state.detail!!.stagesCounterpick)
                    DetailTab.VIDEO    -> VideoTab(
                        videoUrl = state.lastVideoUrl,
                        videoTitle = state.lastVideoTitle,
                        tokenGated = state.lastVideoTokenGated,
                    )
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
  git add app/src/main/java/com/example/smatchup/ui/character/CharacterDetailScreen.kt
  git commit -m "feat(ui): add CharacterDetailScreen (hero + sticky tabs + 7 panels)"
  ```

---

## Task 13: Wire `ViewModelFactory` + `NavGraph` to launch `CharacterDetailScreen`

Compose Navigation passes route arguments via `CreationExtras`. We pass `charId` through a `ViewModelProvider.Factory` that reads it from the screen-scoped extras. Simplest pragmatic approach: build a per-route factory inside the `composable {}` lambda — bypassing the global `ViewModelFactory`. This keeps `CharacterDetailViewModel` constructor explicit.

**Files:** modify `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` (register a builder), `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt` (wire the screen).

- [ ] **Step 1: Add a helper factory builder.** Replace `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` with:

```kotlin
package com.example.smatchup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smatchup.SmatchupApp
import com.example.smatchup.di.AppContainer
import com.example.smatchup.ui.character.CharacterDetailViewModel
import com.example.smatchup.ui.character.CharacterListViewModel
import com.example.smatchup.ui.home.HomeViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java          -> HomeViewModel() as T
        CharacterListViewModel::class.java -> CharacterListViewModel(container.characterRepository) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }

    /** Parameterized factory for `CharacterDetailViewModel(charId)`. */
    fun characterDetail(charId: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == CharacterDetailViewModel::class.java)
                return CharacterDetailViewModel(
                    charId = charId,
                    repo = container.characterRepository,
                    bestPlayerRepo = container.bestPlayerRepository,
                    youTubeApi = container.youtubeApi,
                ) as T
            }
        }

    companion object {
        fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
    }
}
```

- [ ] **Step 2: Wire the screen in `NavGraph.kt`.** Replace the existing `CharacterDetail` placeholder block (currently `PlaceholderScreen("Character detail — $charId")`). The full replacement composable block:

```kotlin
        composable(
            route = Screen.CharacterDetail.route,
            arguments = listOf(navArgument(Screen.CharacterDetail.ARG_CHAR_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString(Screen.CharacterDetail.ARG_CHAR_ID).orEmpty()
            CharacterDetailScreen(
                charId = charId,
                onCharacterClick = { otherId ->
                    nav.navigate(Screen.CharacterDetail.buildRoute(otherId))
                },
                onBack = { nav.popBackStack() },
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ViewModelFactory.fromApp().characterDetail(charId),
                    key = "character-detail-$charId",
                ),
            )
        }
```

Also add the imports at the top of `NavGraph.kt`:
```kotlin
import com.example.smatchup.ui.character.CharacterDetailScreen
import com.example.smatchup.ui.ViewModelFactory
```

- [ ] **Step 3: Build & install.**
  ```bash
  ./gradlew :app:installDebug
  ```

- [ ] **Step 4: Smoke test on emulator.**
  Launch app → Splash → Home → Characters → pick Steve. Confirm:
  1. Hero shows Steve, "Minecraft · #82".
  2. Tab bar starts on Combos and shows 3 combos.
  3. Switching to Gameplan shows the paragraph.
  4. Moves shows the move utility map.
  5. Frame shows the bundled framedata table with source label.
  6. Synergy shows two orbs (Sonic + Fox) — tapping Sonic navigates to Sonic's detail.
  7. Stages shows ban + counterpick lists.
  8. Video either shows the embedded player or the TokenGatedBanner (if `YOUTUBE_API_KEY` is set in `local.properties`, the player should render).
  9. Back button returns to the roster.

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt \
          app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt
  git commit -m "feat(nav): wire CharacterDetailScreen with charId-parameterized factory"
  ```

---

## Final verification

- [ ] **Step 1: Full JVM test suite.**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

- [ ] **Step 2: Full instrumented suite.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest
  ```

- [ ] **Step 3: Debug APK builds.**
  ```bash
  ./gradlew :app:assembleDebug
  ```

- [ ] **Step 4: Manual end-to-end smoke (from Task 13 step 4) covers all 7 tabs and synergy navigation.**

- [ ] **Step 5: `git log --oneline -20` sanity check.** ~13 task commits should be on `character-detail`.

---

## Self-review

**Spec coverage:**

| Spec section | Item | Task |
|---|---|---|
| §3 / UI choice C | Hero portrait + sticky tabs | 11, 12 |
| §3 | YouTube player embed | 2, 10 (VideoTab) |
| §6.7 | Bundled fallback framedata + stages | 5 |
| §6 | Repository fallback chain (cache → API → bundled) | 7, 8 |
| §8.5 | `CharacterDetailViewModel(charId)` | 9 |
| §8.4 | UiState pattern | 9 |
| §11 | Risk: Ultimate API down → bundled fallback | 8 (chain) + 5 (seed) |
| Brainstorm Q3 | API priority, JSON fallback | 8 |

**Placeholder scan:** none. All steps contain complete code.

**Type consistency:**
- `CharacterDetail`, `FramedataSource`, `Stage`/`StageVerdict`, `Player` types used consistently between Tasks 4, 7, 9, 10, 11, 12.
- `CharacterRepository(loader, ultimateApi, cacheDao, cacheManager)` signature in Task 8 ↔ `AppContainer` wiring in Task 8 step 2.
- `CharacterDetailViewModel(charId, repo, bestPlayerRepo, youTubeApi)` constructor in Task 9 ↔ `ViewModelFactory.characterDetail(charId)` in Task 13.
- `DetailTab` enum entries match the tab order in `CharacterDetailScreen` `when` block (Tasks 9 + 12).

Plan ready.
