# Smatchup — Tier Lists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Tier Lists screen — a Strength/Difficulty toggle over classic horizontal tier rows (colored badge + character orbs), where tapping a character opens its detail page.

**Architecture:** `TierlistRepository.load(name)` reads the bundled `tierlist_<name>.json` (already shipped in sub-project 1), resolves character ids against the roster, and returns a UI-ready `TierlistView` (ordered tier groups, each with its `Character` list). `TierlistViewModel` holds the selected list ("strength" | "difficulty") and re-loads on toggle. `TierlistScreen` renders horizontal rows and routes taps to `CharacterDetail`.

**Tech Stack:** existing stack. No new dependencies.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — sections 3 (UI layout A — horizontal rows, Strength/Difficulty toggle), 6.7 (bundled tierlist JSON). Builds on sub-projects 1–4. Reuses the `Tier` enum + `tierlist_*.json` from sub-project 1.

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── domain/model/TierlistView.kt          # TierGroup + TierlistView (UI-ready)
├── data/repository/TierlistRepository.kt
├── ui/components/TierBadge.kt
├── ui/tierlist/
│   ├── TierlistScreen.kt
│   └── TierlistViewModel.kt

app/src/test/java/com/example/smatchup/ui/tierlist/TierlistViewModelTest.kt
app/src/androidTest/java/com/example/smatchup/data/repository/TierlistRepositoryTest.kt
```

### Modified

- `app/src/main/java/com/example/smatchup/di/AppContainer.kt` — register `TierlistRepository`.
- `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` — register `TierlistViewModel`.
- `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt` — replace `Tierlist` placeholder with `TierlistScreen`.

### Untouched

`tierlist_strength.json` / `tierlist_difficulty.json` already exist (sub-project 1). `Tier` enum already exists (sub-project 1, `TierEntry.kt`).

---

## Conventions

- **Branch:** `git checkout -b tierlists` off `api`.
- One commit per task. TDD for repo + VM. UI smoke-tested.
- **Single-device rule:** never run two `connectedDebugAndroidTest` at once.

---

## Task 1: `TierlistView` model + `TierlistRepository`

The bundled JSON shape (already shipped):
```json
{ "name": "strength", "version": "UltRank 2026 v4 (seed)", "updatedAt": 1746489600000,
  "entries": [ { "tier": "S_PLUS", "chars": ["steve","sonic","snake"] }, { "tier": "S", "chars": ["fox","sephiroth"] } ] }
```

`TierlistRepository.load(name)` returns a `TierlistView` whose groups are ordered by the `Tier` enum ordinal (S_PLUS first) and whose `characters` are resolved against the roster (unknown ids dropped).

**Files:** `app/src/main/java/com/example/smatchup/domain/model/TierlistView.kt`, `app/src/main/java/com/example/smatchup/data/repository/TierlistRepository.kt`, `app/src/androidTest/java/com/example/smatchup/data/repository/TierlistRepositoryTest.kt`.

- [ ] **Step 1: Create the model.**

```kotlin
// app/src/main/java/com/example/smatchup/domain/model/TierlistView.kt
package com.example.smatchup.domain.model

data class TierGroup(val tier: Tier, val characters: List<Character>)

data class TierlistView(
    val name: String,
    val version: String,
    val groups: List<TierGroup>,
)
```

- [ ] **Step 2: Failing instrumented test.**

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/repository/TierlistRepositoryTest.kt
package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Tier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierlistRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val charRepo = CharacterRepository(loader)
    private val repo = TierlistRepository(loader, charRepo)

    @Test fun strengthListLoadsWithGroupsOrdered() = runBlocking {
        val view = repo.load("strength")
        assertEquals("strength", view.name)
        assertTrue(view.groups.isNotEmpty())
        // First group must be the highest tier present (S_PLUS in the seed).
        assertEquals(Tier.S_PLUS, view.groups.first().tier)
    }

    @Test fun charactersResolvedToDomainObjects() = runBlocking {
        val view = repo.load("strength")
        val sPlus = view.groups.first { it.tier == Tier.S_PLUS }
        val names = sPlus.characters.map { it.name }
        assertTrue("Steve should be S+", "Steve" in names)
    }

    @Test fun difficultyListLoads() = runBlocking {
        val view = repo.load("difficulty")
        assertEquals("difficulty", view.name)
        assertTrue(view.groups.isNotEmpty())
    }

    @Test fun unknownListReturnsEmptyGroups() = runBlocking {
        val view = repo.load("nonexistent")
        assertTrue(view.groups.isEmpty())
    }
}
```

- [ ] **Step 3: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/TierlistRepository.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import com.example.smatchup.domain.model.Tier
import com.example.smatchup.domain.model.TierGroup
import com.example.smatchup.domain.model.TierlistView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TierlistRepository(
    private val loader: JsonAssetLoader,
    private val characterRepository: CharacterRepository,
) {

    suspend fun load(name: String): TierlistView = withContext(Dispatchers.IO) {
        val json = loader.tierlist(name)
            ?: return@withContext TierlistView(name = name, version = "", groups = emptyList())

        val rosterById: Map<String, Character> = characterRepository.loadRoster().associateBy { it.id }
        val entries = json.optJSONArray("entries")
        val groups = if (entries == null) emptyList() else (0 until entries.length()).mapNotNull { i ->
            val o = entries.getJSONObject(i)
            val tier = runCatching { Tier.valueOf(o.getString("tier")) }.getOrNull() ?: return@mapNotNull null
            val charsArr = o.optJSONArray("chars")
            val chars = if (charsArr == null) emptyList() else (0 until charsArr.length())
                .mapNotNull { j -> rosterById[charsArr.getString(j)] }
            TierGroup(tier = tier, characters = chars)
        }.sortedBy { it.tier.ordinal }

        TierlistView(
            name = json.optString("name", name),
            version = json.optString("version", ""),
            groups = groups,
        )
    }
}
```

- [ ] **Step 4: Run.**
  ```bash
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.TierlistRepositoryTest
  ```
  Expected: 4 pass.

- [ ] **Step 5: Wire `AppContainer`.** After `matchupRepository`, append:
  ```kotlin
      val tierlistRepository: TierlistRepository = TierlistRepository(jsonAssetLoader, characterRepository)
  ```
  Add import `import com.example.smatchup.data.repository.TierlistRepository`.

- [ ] **Step 6: Commit.**
  ```bash
  git checkout -b tierlists
  git add app/src/main/java/com/example/smatchup/domain/model/TierlistView.kt \
          app/src/main/java/com/example/smatchup/data/repository/TierlistRepository.kt \
          app/src/main/java/com/example/smatchup/di/AppContainer.kt \
          app/src/androidTest/java/com/example/smatchup/data/repository/TierlistRepositoryTest.kt
  git commit -m "feat(repo): add TierlistRepository + TierlistView model"
  ```

---

## Task 2: `TierBadge` component

A colored, rounded badge showing the tier label (S+, S, A, …), colored from `SmatchupColors.Tier`.

**Files:** `app/src/main/java/com/example/smatchup/ui/components/TierBadge.kt`.

- [ ] **Step 1: Create.**

```kotlin
package com.example.smatchup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smatchup.domain.model.Tier
import com.example.smatchup.ui.theme.SmatchupColors

private fun Tier.label(): String = when (this) {
    Tier.S_PLUS -> "S+"
    Tier.S -> "S"; Tier.A -> "A"; Tier.B -> "B"; Tier.C -> "C"
    Tier.D -> "D"; Tier.E -> "E"; Tier.F -> "F"
}

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    val color = SmatchupColors.Tier[tier.name] ?: Color.Gray
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tier.label(),
            style = MaterialTheme.typography.titleMedium,
            color = SmatchupColors.Bg1,
        )
    }
}
```

- [ ] **Step 2: Build.**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/TierBadge.kt
  git commit -m "feat(ui): add TierBadge component"
  ```

---

## Task 3: `TierlistViewModel`

Holds the selected list name and the loaded `TierlistView`. `select(name)` reloads.

**Files:** `app/src/main/java/com/example/smatchup/ui/tierlist/TierlistViewModel.kt`, test `app/src/test/java/com/example/smatchup/ui/tierlist/TierlistViewModelTest.kt`.

- [ ] **Step 1: Failing JVM test.**

```kotlin
// app/src/test/java/com/example/smatchup/ui/tierlist/TierlistViewModelTest.kt
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
```

- [ ] **Step 2: Implement.**

```kotlin
// app/src/main/java/com/example/smatchup/ui/tierlist/TierlistViewModel.kt
package com.example.smatchup.ui.tierlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.TierlistRepository
import com.example.smatchup.domain.model.TierlistView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TierlistUiState(
    val isLoading: Boolean = true,
    val selectedName: String = "strength",
    val view: TierlistView? = null,
    val error: String? = null,
)

class TierlistViewModel(
    private val load: suspend (name: String) -> TierlistView,
) : ViewModel() {

    constructor(repo: TierlistRepository) : this(load = { repo.load(it) })

    private val _state = MutableStateFlow(TierlistUiState())
    val state: StateFlow<TierlistUiState> = _state.asStateFlow()

    init { reload() }

    fun select(name: String) {
        _state.update { it.copy(selectedName = name) }
        reload()
    }

    private fun reload() {
        val name = _state.value.selectedName
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val v = load(name)
                _state.update { it.copy(isLoading = false, view = v) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
```

- [ ] **Step 3: Run.**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.tierlist.TierlistViewModelTest"
  ```
  Expected: 2 pass.

- [ ] **Step 4: Register in `ViewModelFactory`.** Add to the `create()` `when`:
  ```kotlin
        TierlistViewModel::class.java -> TierlistViewModel(container.tierlistRepository) as T
  ```
  Add import `import com.example.smatchup.ui.tierlist.TierlistViewModel`.

- [ ] **Step 5: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/tierlist/TierlistViewModel.kt \
          app/src/test/java/com/example/smatchup/ui/tierlist/TierlistViewModelTest.kt \
          app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt
  git commit -m "feat(ui): add TierlistViewModel with strength/difficulty toggle"
  ```

---

## Task 4: `TierlistScreen`

Top toggle (Strength | Difficulty) + horizontal rows (badge left, character orbs right). Tapping an orb routes to `CharacterDetail`.

**Files:** `app/src/main/java/com/example/smatchup/ui/tierlist/TierlistScreen.kt`.

- [ ] **Step 1: Create.**

```kotlin
package com.example.smatchup.ui.tierlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.domain.model.TierGroup
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.PortraitOrb
import com.example.smatchup.ui.components.TierBadge
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun TierlistScreen(
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TierlistViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.wolBackground().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tier List",
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )
        Toggle(selected = state.selectedName, onSelect = viewModel::select)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOrb() }
            state.view == null || state.view!!.groups.isEmpty() ->
                EmptyState(message = "Tier list indisponible.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                items(items = state.view!!.groups, key = { it.tier.name }) { group ->
                    TierRow(group = group, onCharacterClick = onCharacterClick)
                }
            }
        }
    }
}

@Composable
private fun Toggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SmatchupColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
    ) {
        ToggleOpt("strength", "Force", selected, onSelect, Modifier.weight(1f))
        ToggleOpt("difficulty", "Difficulté", selected, onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun ToggleOpt(name: String, label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    val active = name == selected
    Box(
        modifier = modifier
            .background(if (active) SmatchupColors.Purple.copy(alpha = 0.4f) else SmatchupColors.Bg2)
            .clickable { onSelect(name) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) SmatchupColors.Gold else SmatchupColors.TextDim,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TierRow(group: TierGroup, onCharacterClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SmatchupColors.Bg2.copy(alpha = 0.5f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TierBadge(tier = group.tier)
        FlowRow(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            group.characters.forEach { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PortraitOrb(
                        charId = c.id,
                        size = 40.dp,
                        modifier = Modifier.clickable { onCharacterClick(c.id) },
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
  Note: `FlowRow` is stable in Compose Foundation 1.7 (current BOM). No experimental opt-in needed.

- [ ] **Step 3: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/tierlist/TierlistScreen.kt
  git commit -m "feat(ui): add TierlistScreen with toggle + horizontal rows"
  ```

---

## Task 5: Wire `NavGraph`

**Files:** modify `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt`.

- [ ] **Step 1: Add the import.**
  ```kotlin
  import com.example.smatchup.ui.tierlist.TierlistScreen
  ```

- [ ] **Step 2: Replace the `Tierlist` placeholder.** Replace:
  ```kotlin
        composable(Screen.Tierlist.route)  { PlaceholderScreen("Tier lists — sub-project 5") }
  ```
  with:
  ```kotlin
        composable(Screen.Tierlist.route) {
            TierlistScreen(onCharacterClick = { charId ->
                nav.navigate(Screen.CharacterDetail.buildRoute(charId))
            })
        }
  ```

- [ ] **Step 3: Build + install + smoke test.**
  ```bash
  ./gradlew :app:installDebug
  ```
  Launch → Home → Tier Lists. Confirm:
  1. Title "Tier List" + toggle (Force | Difficulté).
  2. Strength shows S+ row (Steve, Sonic, Snake orbs) and S row (Fox, Sephiroth).
  3. Tapping "Difficulté" reloads with the difficulty groups.
  4. Tapping a character orb opens its CharacterDetail.
  5. Back returns to the tier list.

- [ ] **Step 4: Commit.**
  ```bash
  git add app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt
  git commit -m "feat(nav): wire TierlistScreen"
  ```

---

## Final verification

- [ ] **Step 1: JVM suite.** `./gradlew :app:testDebugUnitTest`
- [ ] **Step 2: Instrumented suite (single run).** `./gradlew :app:connectedDebugAndroidTest`
- [ ] **Step 3: APK.** `./gradlew :app:assembleDebug`
- [ ] **Step 4: Manual smoke (Task 5 step 3).**
- [ ] **Step 5: `git log --oneline -8` check.**

---

## Self-review

**Spec coverage:**

| Spec section | Item | Task |
|---|---|---|
| §3 (layout A) | Horizontal tier rows | 4 (TierRow) |
| §3 | Strength/Difficulty toggle | 3 (VM), 4 (Toggle) |
| §3 | Tap character → CharacterDetail | 4, 5 |
| §6.7 | Bundled tierlist JSON | 1 (reuses existing assets) |
| §8.3 | TierBadge component | 2 |

**Placeholder scan:** none. All code complete.

**Type consistency:**
- `TierlistView(name, version, groups)` + `TierGroup(tier, characters)` consistent across Tasks 1, 3, 4.
- `TierlistRepository(loader, characterRepository)` constructor consistent in Tasks 1 + AppContainer wiring.
- `TierlistRepository.load(name)` signature matches the VM's `load` lambda and `TierlistViewModel(repo)` constructor (Tasks 1, 3).
- `TierlistViewModel(load)` / `(repo)` consistent in Tasks 3 + ViewModelFactory.
- `Tier` enum (S_PLUS, S, A, B, C, D, E, F) reused from sub-project 1; `SmatchupColors.Tier` keyed by `Tier.name` (Task 2) matches the map defined in sub-project 2's `Color.kt`.
- `PortraitOrb(charId, size, modifier)` signature matches the component from sub-project 2.

Plan ready.
