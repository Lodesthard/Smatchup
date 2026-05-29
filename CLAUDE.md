# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```powershell
# Build debug APK
.\gradlew assembleDebug

# Build release APK
.\gradlew assembleRelease

# Run unit tests
.\gradlew test

# Run instrumented tests (requires device/emulator)
.\gradlew connectedAndroidTest

# Run a single JVM test class
.\gradlew :app:testDebugUnitTest --tests "com.example.smatchup.data.repository.AuthRepositoryTest"

# Run a single instrumented test class (needs emulator)
.\gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.smatchup.data.repository.BestPlayerRepositoryTest

# Install and run on connected device
.\gradlew installDebug
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 36
- **Build:** Gradle with Kotlin DSL (`build.gradle.kts`)
- **Dependencies:** Version catalog at `gradle/libs.versions.toml`

## Architecture

Single-activity app. MVVM + manual DI + Compose Navigation.

- DI: `di/AppContainer.kt` (manual singletons), built in `SmatchupApp.onCreate`, accessed via `SmatchupApp.instance.container`
- ViewModels: expose `StateFlow<UiState>`; constructor takes lambdas for testability + a secondary `(repo)` constructor for prod
- VM creation: `ViewModelFactory.fromApp()`; parameterized VMs via factory methods (e.g. `characterDetail(id)`, `favoriteMatchup(a,b)`)
- Nav: `ui/nav/NavGraph.kt` + `Screen` sealed class; Splash routes on session (logged in → Home, else Login)
- Repos in `data/repository/`; Room DAOs are `interface`s; bundled JSON in `app/src/main/assets/`

## Key Conventions

- Package: `com.example.smatchup`
- All UI in Compose composables; no XML layouts
- Dependency versions added to `gradle/libs.versions.toml`, not hardcoded in `build.gradle.kts`

## Testing patterns

- DAOs are `interface`s → fake them in-memory for **JVM** repo/VM tests (no Room/Robolectric). See `AuthRepositoryTest`.
- Repo tests needing real Room: instrumented, `Room.inMemoryDatabaseBuilder(...)`. See `BestPlayerRepositoryTest`.
- VM tests: turbine + `kotlinx-coroutines-test` (`Dispatchers.setMain(UnconfinedTestDispatcher())`).
- Big features: spec → per-subproject plan in `docs/superpowers/plans/` → execute task-by-task. UI is smoke-tested, not unit-tested (spec §10).

## Emulator smoke testing

- Run adb via **PowerShell**, not the Bash tool — git-bash mangles `/sdcard/...` into a Windows path. adb at `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`.
- Screenshot: `adb shell screencap -p /sdcard/s.png; adb pull /sdcard/s.png "$env:TEMP\s.png"` then Read the PNG.
- Drive UI: `adb shell input tap X Y` / `input text` / `input keyevent 4`. Screen is 1080x2400 — scale tap coords from the screenshot.
- Force logged-out: `adb shell pm clear com.example.smatchup`.

## Gotchas

- **`FlowRow` crashes at runtime** on Compose BOM 2024.09.00 (`NoSuchMethodError`, experimental `FlowRowOverflow` overload). Use a scrollable `Row`/`Column` instead.
- Material icons: only **core** is on the classpath — `Icons.Filled.*` only, no `Icons.Outlined.*` / extended.
- For JVM-testable hashing/crypto use `java.util.Base64` (not `android.util.Base64`, which is stubbed in unit tests).

## Secrets / API Keys

- Store in `local.properties`: `YOUTUBE_API_KEY=...` (gitignored)
- Injected into `BuildConfig` via `app/build.gradle.kts`; `buildConfig = true` already enabled
- Access in code: `BuildConfig.YOUTUBE_API_KEY`

## Networking

- HTTP: OkHttp directly (`okhttp:4.12.0`), no Retrofit
- JSON: `org.json` (Android built-in), no Gson/Moshi
- Network permission: `INTERNET` in `AndroidManifest.xml`
- YouTube Data API v3 targets VGBootcamp channel (handle `vgbootcamp`, fallback ID `UCj1J3QuIftjOq9iv_rr7Egw`)

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
