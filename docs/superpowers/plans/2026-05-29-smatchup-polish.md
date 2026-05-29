# Smatchup — Polish Implementation Plan (sub-project 7)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Final polish pass — orb pulse animations, page/tab transitions, accessibility (content descriptions, touch targets, contrast), a frontend-design review pass, and optional SmashWiki lore enrichment.

**Architecture:** Pure UI layer. No new repositories or data flow. Add Compose animations (`infiniteTransition`, `Crossfade`, `AnimatedContentTransitionScope`), `contentDescription`s, and minor visual refinements. Optional SmashWiki enrichment reuses the existing `SmashWikiApi` client (already shipped, currently unused).

**Tech Stack:** existing stack. No new dependencies (Compose animation is in the BOM already).

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — §8.6 (Animations), §3 (visual direction), §12 (deferred: accessibility audit, optional SmashWiki descriptions). Builds on sub-projects 1–6 (all merged to `api`).

**Branch:** `git checkout -b polish` off `api`.

---

## Current state (audited 2026-05-29)

- Only `ui/components/LoadingOrb.kt` uses `infiniteTransition` (pulse). `PortraitOrb` is static.
- `NavGraph` `composable(...)` blocks use **no** enter/exit transitions (default instant swap).
- Character/Matchup detail tab content swaps with **no** crossfade.
- Almost no `contentDescription` (only `FavoriteHeart`). `PortraitOrb`, `OrbCard`, orbs in lists are unlabeled → screen-reader gaps.
- `SmashWikiApi.kt` exists but is wired nowhere.

---

## Task 1: Orb pulse on `PortraitOrb`

Add the §8.6 pulse (opacity infiniteTransition, ~5 s loop) to `PortraitOrb` so character avatars breathe like `LoadingOrb`. Gate behind a `pulse: Boolean = true` param so list-dense screens can disable it for perf.

**Files:** `app/src/main/java/com/example/smatchup/ui/components/PortraitOrb.kt`.

- [ ] **Step 1:** Read `LoadingOrb.kt` to copy the existing pulse idiom (keep timing/easing consistent).
- [ ] **Step 2:** Add `pulse: Boolean = true` param. When true, wrap the glow alpha in an `infiniteTransition.animateFloat` (e.g. 0.35f→0.6f, 5000 ms, `RepeatMode.Reverse`). Apply to the `.glow(...)` alpha. Default callers unchanged.
- [ ] **Step 3:** In `FavoritesScreen` + `TierlistScreen` list rows, pass `pulse = false` (many orbs at once → avoid overdraw).
- [ ] **Step 4:** `./gradlew :app:compileDebugKotlin`.
- [ ] **Step 5:** `./gradlew :app:installDebug`, eyeball Home + a character detail (hero orb should pulse; list orbs static).
- [ ] **Step 6:** Commit `feat(ui): pulse animation on PortraitOrb`.

---

## Task 2: Page transitions

Add fade + slide-up (16 dp) enter / fade exit to the `NavHost` so screen changes glide (§8.6).

**Files:** `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt`.

- [ ] **Step 1:** On `NavHost`, set default transitions:
  ```kotlin
  enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 } }
  exitTransition = { fadeOut(tween(180)) }
  popEnterTransition = { fadeIn(tween(220)) }
  popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 16 } }
  ```
  Imports: `androidx.compose.animation.*`, `androidx.compose.animation.core.tween`.
- [ ] **Step 2:** Keep `Splash → Login/Home` a plain fade (no slide) — it already `popUpTo`s the splash; a slide on first paint feels janky. Override per-composable if needed.
- [ ] **Step 3:** `./gradlew :app:compileDebugKotlin` + `installDebug`, navigate Home→Characters→detail→back; confirm smooth, no flicker.
- [ ] **Step 4:** Commit `feat(nav): fade + slide page transitions`.

---

## Task 3: Tab crossfade (Character + Matchup detail)

Crossfade tab-panel content (180 ms, §8.6) instead of instant swap.

**Files:** `app/src/main/java/com/example/smatchup/ui/character/CharacterDetailScreen.kt`, `app/src/main/java/com/example/smatchup/ui/matchup/MatchupDetailScreen.kt`.

- [ ] **Step 1:** In `CharacterDetailScreen`, wrap the tab `when (state.selectedTab)` in `Crossfade(targetState = state.selectedTab, animationSpec = tween(180)) { tab -> when (tab) { ... } }`. Import `androidx.compose.animation.Crossfade`, `androidx.compose.animation.core.tween`.
- [ ] **Step 2:** Same for `MatchupDetailScreen`'s `when (state.selectedTab)` block (the inner scrollable column content).
- [ ] **Step 3:** `compileDebugKotlin` + `installDebug`, tap through tabs on both screens; confirm crossfade.
- [ ] **Step 4:** Commit `feat(ui): crossfade detail tab content`.

---

## Task 4: Accessibility pass

Add `contentDescription`s and verify touch targets / contrast.

**Files:** `ui/components/PortraitOrb.kt`, `ui/components/OrbCard.kt`, plus call sites that pass character names (`CharacterListScreen`, `FavoritesScreen`, `TierlistScreen`, detail heroes).

- [ ] **Step 1:** Add an optional `contentDescription: String? = null` to `PortraitOrb`; when non-null, set `Modifier.semantics { contentDescription = it }` (or pass to an `Image`/`Box` semantics). Pass the character display name at every call site (e.g. `contentDescription = c.name`).
- [ ] **Step 2:** Add `contentDescription` to `OrbCard` (use its `label`).
- [ ] **Step 3:** Audit touch targets: ensure clickable orbs/rows are ≥ 48 dp (TierlistScreen orbs are 40 dp + clickable — bump the clickable area via `Modifier.size(48.dp)` wrapper or `minimumInteractiveComponentSize()`).
- [ ] **Step 4:** Run TalkBack manually on emulator (Settings → Accessibility) over Home + a detail screen; confirm orbs announce names.
- [ ] **Step 5:** Commit `feat(a11y): content descriptions + touch-target sizing`.

---

## Task 5: frontend-design review pass

Use the `frontend-design` skill for a final aesthetic review of the World-of-Light theme across screens.

- [ ] **Step 1:** Invoke the `frontend-design` skill; ask it to review Home, CharacterDetail, MatchupDetail, Tierlist, Favorites, Login/Register, Profile against the §3 visual direction (purple/gold, radial glows, serif `Cinzel` titles).
- [ ] **Step 2:** Apply its concrete suggestions (spacing, glow intensity, typography scale, empty-state polish). One commit per coherent change set.
- [ ] **Step 3:** `installDebug` + screenshot each screen; compare against spec mockups intent.
- [ ] **Step 4:** Commit `style(ui): frontend-design polish pass`.

---

## Task 6 (optional): SmashWiki lore enrichment

Wire the unused `SmashWikiApi` to show a short character description on CharacterDetail (spec §12, optional).

**Files:** `data/api/SmashWikiApi.kt` (read), `data/repository/CharacterRepository.kt`, `ui/character/CharacterDetailViewModel.kt`, a new lore section/tab.

- [ ] **Step 1:** Read `SmashWikiApi.kt` to confirm its method shape + `ApiResult` returns.
- [ ] **Step 2:** Add `CharacterRepository.lore(charId)` with Room cache (reuse `CacheManager` TTL; add a `cached_lore` entity or reuse a generic cache — decide at implementation, prefer reuse).
- [ ] **Step 3:** Surface in `CharacterDetailViewModel` state + render in a "Lore"/"Infos" section. Graceful `EmptyState` when unavailable.
- [ ] **Step 4:** Test parser with a fixture (JVM, under `src/test/resources/fixtures/`), wire UI, smoke test.
- [ ] **Step 5:** Commit `feat(character): SmashWiki lore section`.

> Skip this task if you want a tight polish-only release; it adds a network path + cache surface.

---

## Final verification

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:connectedDebugAndroidTest` (single device)
- [ ] `./gradlew :app:assembleDebug`
- [ ] Manual smoke: navigate every screen, confirm animations + TalkBack labels.
- [ ] `graphify update .`
- [ ] `git log --oneline` review, then finishing-a-development-branch (merge to `api`).

---

## Notes carried from sub-project 6

- Matchup favorite heart was wired but never manually clicked — exercise it during the Task 5 smoke pass.
- `BestPlayersWorker` start.gg computation is unverified (no `START_GG_TOKEN`). If a token gets added before this sub-project, validate the worker output appears via `BestPlayerRepository` (cache-first) on CharacterDetail.
- Compose BOM is `2024.09.00` / Foundation 1.7 — `FlowRow` ships an experimental overload that crashed at runtime once; prefer stable layout APIs (scrollable `Row`/`Column`) over `FlowRow` unless the BOM is bumped.

Plan ready.
