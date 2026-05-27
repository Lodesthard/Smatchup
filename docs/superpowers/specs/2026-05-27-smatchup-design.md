# Smatchup — Design Spec

**Date:** 2026-05-27
**Status:** Approved (brainstorm complete, awaiting user spec review)
**Package:** `com.example.smatchup` · single-module Android app · Kotlin + Jetpack Compose
**Min SDK:** 24 · **Target SDK:** 36

## 1. Purpose

Personal Android app (single-user but multi-user-capable backend) for researching Super Smash Bros. Ultimate characters and match-ups. Surfaces frame data, combos, gameplans, move utility, synergies, stages, computed tournament win-rates (since 2024-01-01), the "best player of character X" per character, and the latest VGBootcamp YouTube match for the relevant player(s). Includes official tier lists (strength + difficulty) and a personal favorites system gated by local auth.

Visual identity follows SSBU's *World of Light* screen: purple/gold mystical palette, glowing orb motifs, serif titles, dark radial backgrounds.

## 2. Scope decomposition

The project is too large for a single spec→plan→implementation cycle. It is decomposed into **seven sub-projects**, each of which will get its own follow-on spec + plan + implementation:

| # | Sub-project | Key deliverable | Blocks |
|---|---|---|---|
| 1 | **Data Foundation** | Domain models, API clients, Room DB, JSON asset loader, cache manager, rate limiter. No UI. | 2–6 |
| 2 | **Theme + Nav + Home + Char List** | World of Light theme, reusable components, NavGraph, Splash, Home (orb grid), Character List | 3–6 |
| 3 | **Character Detail** | Hero + sticky tabs screen with all character sections + YouTube embed | 6 |
| 4 | **Matchup Detail** | Picker + paired-section-cards detail screen, win-rate computer, last-clash video | 6 |
| 5 | **Tier Lists** | Strength + difficulty horizontal-rows tier list, navigates to character detail | — |
| 6 | **Auth + Favorites + Profile + BestPlayers** | Local register/login, session, favorites toggles, profile, BestPlayersWorker | — |
| 7 | **Polish** | Animations, accessibility, frontend-design final pass | — |

**Critical path:** 1 → 2 → 3 → 4 → 5 → 6 → 7. Sub-projects 3/4/5 are largely independent after 1+2.

## 3. Visual direction (locked)

| Aspect | Decision |
|---|---|
| Aesthetic | World of Light: deep purple (`#1E0838` → `#0A0118`), gold (`#FFD96A`), purple accent (`#B06AFF`), radial glows, serif titles, glowing orb avatars |
| Home layout | **Orb Grid** — 2×3 glowing orb cards: Characters · Match-up · Tier Lists · Favorites · Profile · Last Match |
| Character Detail layout | **Hero portrait + sticky tabs** — large gradient-orb hero with name & meta, sticky horizontal tab strip below (Combos, Gameplan, Moves, Frame, Synergy, Stages, Video), tab content swaps below |
| Match-up Detail layout | **Paired Section Cards** — both portraits + win-rate bar fixed at top, scrollable section cards (Gameplan / Strong / Punishable / Stages / Video), each card internally split A/B |
| Tier List layout | **Classic horizontal rows** — colored tier badge left, character grid right; toggle Strength/Difficulty at top |
| Typography | Serif (`Cinzel` via Google Fonts, fallback Georgia) for titles; system sans for body |

Mockups for each layout are stored under `.superpowers/brainstorm/` (gitignored).

## 4. Data sources

| Source | Type | Use | Auth | Live? |
|---|---|---|---|---|
| **The Ultimate API** (`https://the-ultimate-api.dreseansutton.com/`) | REST | Frame data, moves, throws, movements, stats | none | Confirmed live; flaky — fallback required |
| **start.gg GraphQL** (`https://api.start.gg/gql/alpha`) | GraphQL | Tournaments, sets, players, character usage → compute win-rates and best-player-per-character | Bearer token, 80 req/60 s, 1 000 objects/req, token 1-year TTL | Live |
| **YouTube Data v3** | REST (already wired in `YouTubeApi.kt`) | Find latest VGBootcamp video for a player or matchup keyword | API key, 10 k units/day | Live |
| **SmashWiki MediaWiki API** (`https://www.ssbwiki.com/api.php`) | REST | Character descriptions / lore (optional polish) | none | Live |
| **Bundled JSON assets** (`app/src/main/assets/…`) | local | Combos, gameplan, move utility, synergies, stages, tier lists, matchup gameplan, majors list, seed best players, framedata fallback | n/a | n/a |

Tokens go in `local.properties` (gitignored), injected via `BuildConfig`:

```properties
YOUTUBE_API_KEY=...
START_GG_TOKEN=...
```

`START_GG_TOKEN` may be empty initially — the app must gracefully gate win-rate and best-player features behind a `TokenGatedBanner` until the token is set.

## 5. Architecture

Single Gradle module organized by feature package, MVVM + Compose, no DI framework:

```
com.example.smatchup/
├── data/
│   ├── api/          # OkHttp clients: UltimateApi, StartGgApi, YouTubeApi, SmashWikiApi
│   ├── local/        # Room: SmatchupDatabase, DAOs, entities
│   ├── assets/       # JsonAssetLoader (read bundled curated JSON)
│   ├── cache/        # CacheManager (TTL logic per CacheTtl constants), RateLimiter
│   └── repository/   # CharacterRepo, MatchupRepo, TierlistRepo, AuthRepo, FavoritesRepo
├── domain/
│   └── model/        # Character, Matchup, Move, Frame, Stage, Player, TierEntry
├── ui/
│   ├── theme/        # SmatchupTheme, palette, typography, glow & wol modifiers
│   ├── components/   # OrbCard, PortraitOrb, WinBar, TierBadge, TierRow, SectionCard,
│   │                 # PairedSplit, GlowButton, LoadingOrb, EmptyState,
│   │                 # TokenGatedBanner, YouTubePlayer
│   ├── nav/          # NavGraph + Screen sealed class
│   ├── home/         # HomeScreen + HomeViewModel
│   ├── auth/         # LoginScreen, RegisterScreen, AuthViewModel
│   ├── character/    # CharacterListScreen, CharacterDetailScreen, ViewModels
│   ├── matchup/      # MatchupPickerScreen, MatchupDetailScreen, ViewModels
│   ├── tierlist/     # TierlistScreen, TierlistViewModel
│   └── favorites/    # FavoritesScreen, FavoritesViewModel
└── di/               # AppContainer (manual singleton, instantiated in Application.onCreate)
```

### Stack

- Jetpack Compose + Material3 (current)
- Compose Navigation
- ViewModels with `StateFlow<UiState<T>>` (migrate away from `mutableStateOf` in `MainViewModel` over time)
- Kotlin coroutines + `Dispatchers.IO` for network/disk
- Room (KSP, `exportSchema = true`)
- OkHttp 4.x + `org.json` (no Retrofit/Moshi to stay lean)
- AndroidX YouTube Player: `com.pierfrancescosoffritti.androidyoutubeplayer:core` for embed
- WorkManager for periodic best-player refresh

## 6. Data layer

### 6.1 Result type

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
    data class RateLimited(val retryAfter: Long?) : ApiResult<Nothing>()
    object Unauthorized : ApiResult<Nothing>()   // token missing
    data class ParseError(val msg: String) : ApiResult<Nothing>()
    object NotFound : ApiResult<Nothing>()
}
```

### 6.2 Repository fallback chain (example: `CharacterRepo.getFramedata`)

1. Look up Room cache (`cached_framedata`); if `fetchedAt + FRAMEDATA_MS > now`, return cached.
2. Call `UltimateApi.getMoves(charId)`:
   - **Success** → upsert into Room, return data.
   - **NetworkError / NotFound** → fall back to `JsonAssetLoader.framedata(charId)`.
   - **RateLimited** → one retry after `retryAfter` (default 5 s), then fall back to bundled JSON.
3. If everything fails → emit `UiState.Error` with retry callback.

### 6.3 Rate limit (start.gg)

`RateLimiter` is a token-bucket: 80 permits per rolling 60 s window. Suspend-blocks new requests until a permit is available. Wraps all `StartGgApi` calls.

### 6.4 Token gating

`StartGgApi.query(...)` short-circuits to `ApiResult.Unauthorized` when `token.isBlank()`. UI displays `TokenGatedBanner` ("Set `START_GG_TOKEN` in `local.properties` to enable win-rate") for affected features.

### 6.5 Best-player computation

`BestPlayersWorker` (`WorkManager`, periodic 60 days) computes `best_players` table:

1. Load `assets/majors_slugs.json` (user-curated list of start.gg tournament slugs since 2024-01-01).
2. For each tournament: fetch top 32 entrants + their sets.
3. For each set: extract `selections.character` (skip and flag if absent).
4. For each player: pick "main" = most-used character.
5. Score = Σ over tournaments of `placementWeight(place) * mainUsageFrequency`, where `placementWeight` is `1st=100, 2nd=70, 3rd=50, 4th=40, 5th-8th=25, 9th-16th=10, 17th-32nd=5`.
6. Group by character, take max-score player.
7. Upsert into `cached_best_players`.

Total cost ≈ 25 majors × ~5 paginated requests ≈ ~125 requests per refresh, well inside the 80/60s budget when paced.

### 6.6 Per-matchup win-rate

Lazy on user navigation to a matchup screen. Pulls same major-tournament dataset (re-uses cached pages where possible), filters sets where both characters were used by the two players, aggregates wins for charA. Cached in `cached_winrate` for 7 days.

### 6.7 Bundled JSON layout (`app/src/main/assets/`)

```
assets/
├── framedata/<charId>.json         # API-shape fallback
├── characters_meta/<charId>.json   # combos, gameplan, move_utility
├── synergies.json                  # { charId: [partnerIds] }
├── stages/<charId>.json            # { solo: {ban, cp}, vs: { <oppId>: {ban, cp} } }
├── matchups/<a>_<b>.json           # normalized so a<b lexicographically; gameplan/strong/punish
├── tierlist_strength.json          # { tiers: [{ tier: "S+", chars: [...] }, …] } — UltRank 2026 v4
├── tierlist_difficulty.json        # Game8 difficulty ranking
├── majors_slugs.json               # ["genesis-9", "evo-2024", …]
└── seed_best_players.json          # placeholder before first WorkManager refresh
```

Workflow: I generate JSON skeletons + seed data (combos/gameplan/etc.) for 5–10 popular characters (Steve, Sonic, Snake, Fox, Sephiroth, Pikachu, …) based on general public knowledge. User fills the rest progressively. App must gracefully render "data not yet available" empty states.

### 6.8 OkHttp

Singleton `OkHttpClient` with 10 s connect/read timeouts, gzip interceptor, `User-Agent: Smatchup/1.0`, and an `HttpLoggingInterceptor` only when `BuildConfig.DEBUG`.

## 7. Persistence (Room v1)

### 7.1 Entities

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val pseudo: String,   // unique
    @ColumnInfo(index = true) val email: String,    // unique
    val passwordHash: String,                       // PBKDF2 base64
    val salt: String,                               // base64
    val createdAt: Long
)

@Entity(
    tableName = "favorite_characters",
    primaryKeys = ["userId", "charId"],
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = CASCADE)]
)
data class FavoriteCharacterEntity(val userId: Long, val charId: String, val addedAt: Long)

@Entity(
    tableName = "favorite_matchups",
    primaryKeys = ["userId", "charA", "charB"],
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = CASCADE)]
)
data class FavoriteMatchupEntity(val userId: Long, val charA: String, val charB: String, val addedAt: Long)

@Entity(tableName = "cached_framedata")
data class CachedFramedataEntity(@PrimaryKey val charId: String, val jsonBlob: String, val fetchedAt: Long)

@Entity(tableName = "cached_winrate", primaryKeys = ["charA", "charB"])
data class CachedWinrateEntity(
    val charA: String, val charB: String,         // normalized: charA < charB
    val winRateA: Float, val sampleSize: Int, val majorsCount: Int,
    val computedAt: Long
)

@Entity(tableName = "cached_best_players")
data class CachedBestPlayerEntity(
    @PrimaryKey val charId: String,
    val playerTag: String, val startGgPlayerId: Long?, val score: Float, val computedAt: Long
)

@Entity(tableName = "cached_youtube_video")
data class CachedYoutubeVideoEntity(
    @PrimaryKey val cacheKey: String,             // "char:<id>" or "mu:<a>_<b>"
    val videoId: String, val title: String, val publishedAt: Long, val fetchedAt: Long
)

@Entity(tableName = "session")
data class SessionEntity(@PrimaryKey val id: Int = 0, val userId: Long?, val updatedAt: Long)
```

### 7.2 DAOs

`UserDao`, `FavoritesDao`, `CacheDao` (combined for `cached_*` tables), `SessionDao`. Suspend functions for writes; `Flow<T>` returns for observables consumed by ViewModels.

### 7.3 Password hashing

`PasswordHasher` uses `javax.crypto.SecretKeyFactory` PBKDF2WithHmacSHA256 with a 16-byte random salt per user and 120 000 iterations, producing a 256-bit hash. Both salt and hash are stored Base64. No external dependency.

### 7.4 Cache TTL constants

```kotlin
object CacheTtl {
    const val FRAMEDATA_MS    = 30L * 24 * 3600 * 1000  // 30 d
    const val WINRATE_MS      = 7L  * 24 * 3600 * 1000  // 7 d
    const val BEST_PLAYER_MS  = 60L * 24 * 3600 * 1000  // 60 d
    const val YT_VIDEO_MS     = 1L  * 24 * 3600 * 1000  // 1 d
}
```

### 7.5 Session

Singleton row in `session` table. `userId` nullable; non-null means logged in. Splash inspects this row to decide initial route.

## 8. UI layer

### 8.1 Theme

```kotlin
object SmatchupColors {
    val Bg1       = Color(0xFF0A0118)
    val Bg2       = Color(0xFF1E0838)
    val Purple    = Color(0xFFB06AFF)
    val Gold      = Color(0xFFFFD96A)
    val Text      = Color(0xFFF5ECFF)
    val TextDim   = Color(0x99F5ECFF)
    val DangerRed = Color(0xFFFF5A6A)
    val Tier      = mapOf(
        "S+" to Color(0xFFFF5A6A),
        "S"  to Color(0xFFFFAA3A),
        "A"  to Gold,
        "B"  to Color(0xFFA6E36A),
        "C"  to Color(0xFF6AD8FF),
        "D"  to Purple
    )
}
```

Custom modifiers: `Modifier.glow(color, radius, alpha)`, `Modifier.orbBackground(primary, secondary)`, `Modifier.wolBackground()`.

### 8.2 Navigation

```kotlin
sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object CharacterList  : Screen("characters")
    data class CharacterDetail(val charId: String) : Screen("char/$charId")
    object MatchupPicker  : Screen("mu/pick")
    data class MatchupDetail(val a: String, val b: String) : Screen("mu/$a/$b")
    object Tierlist       : Screen("tiers")
    object Favorites      : Screen("favorites")
    object Profile        : Screen("profile")
}
```

Root nav graph in `MainActivity`. `Splash` consults the session row and either replaces the stack with `Home` (logged in) or `Login` (logged out).

### 8.3 Reusable components

| Component | Purpose |
|---|---|
| `OrbCard(label, icon, onClick)` | Home grid cells |
| `PortraitOrb(charId, size)` | Character avatar; gradient fallback if no image asset |
| `WinBar(percentA, sample)` | Match-up header bar |
| `TierBadge(tier)` | Tier list row badge |
| `TierRow(tier, chars, onCharClick)` | Tier list row |
| `SectionCard(title, content)` | Generic section wrapper |
| `PairedSplit(leftWho, leftContent, rightWho, rightContent)` | Match-up internal A/B split |
| `GlowButton(text, onClick, variant)` | CTAs |
| `YouTubePlayer(videoId)` | Lifecycle-aware embed of `YouTubePlayerView` via `AndroidView`; placeholder when `videoId == null` |
| `LoadingOrb()` | Pulsing orb spinner |
| `EmptyState(message, cta)` | Graceful empties / errors |
| `TokenGatedBanner(feature, instructions)` | Shown when `START_GG_TOKEN` is missing for a feature |

### 8.4 UiState pattern

```kotlin
sealed class UiState<out T> {
    object Loading                                : UiState<Nothing>()
    data class Loaded<T>(val data: T)             : UiState<T>()
    data class Error(val message: String, val retry: (() -> Unit)? = null) : UiState<Nothing>()
}
```

ViewModels expose `StateFlow<UiState<DomainShape>>`. Screens collect with `collectAsStateWithLifecycle()` and render `when` branches.

### 8.5 ViewModels

`AuthViewModel`, `HomeViewModel`, `CharacterListViewModel`, `CharacterDetailViewModel(charId)`, `MatchupPickerViewModel`, `MatchupDetailViewModel(charA, charB)`, `TierlistViewModel`, `FavoritesViewModel`, `ProfileViewModel`. Parameterized VMs use an `AbstractSavedStateViewModelFactory` to thread route args.

### 8.6 Animations

- Orb pulse: `infiniteTransition` on opacity, 5 s loop, applied to `PortraitOrb` and `LoadingOrb`.
- Page transitions: fade + slight slide-up (16 dp).
- Tab change: crossfade panel content (180 ms).

## 9. Build order, file deliverables per sub-project

(See section 2 for the table. Each sub-project gets its own follow-on spec → plan → implementation cycle. Polish details are deferred to sub-project 7.)

## 10. Testing strategy

| Layer | Test type | Location |
|---|---|---|
| `PasswordHasher` | Unit (JVM) | `src/test/` |
| `CacheManager` TTL logic | Unit | `src/test/` |
| `RateLimiter` token bucket | Unit | `src/test/` |
| API parsers (`UltimateApi`, `StartGgApi`, `YouTubeApi`, `SmashWikiApi`) | Unit w/ fixtures | `src/test/resources/` |
| `WinrateComputer` aggregation | Unit | `src/test/` |
| Room DAOs | Instrumented | `src/androidTest/` |
| `JsonAssetLoader` | Instrumented (reads real assets) | `src/androidTest/` |
| Compose UI screens | **Skipped** (low ROI for solo app) | n/a |
| End-to-end smoke | Manual via `/run` skill after each sub-project | n/a |

No coverage threshold. Tests focus on logic risk: hashing, caching, aggregation, parsers.

## 11. Risks & mitigations

| Risk | Mitigation |
|---|---|
| The Ultimate API goes down permanently | Bundled fallback JSON is already in the chain. Add an offline scrape script to regenerate it from `ultimateframedata.com` when needed. |
| start.gg `selections.character` missing on some tournaments | Skip those tournaments in best-player computation; record `majorsCount` so UI can show "based on N majors". |
| YouTube daily quota exhausted | 1-day cache is aggressive; on quota error, render "Try again tomorrow" placeholder instead of erroring. |
| New UltRank tier list released | Update `tierlist_strength.json` manually; add `version` field to invalidate on read. |
| Game patch changes frame data | Rebuild bundled `framedata/<charId>.json` from the scrape script; remote cache TTL of 30 days mostly absorbs this. |
| WorkManager battery restrictions delay best-player refresh | Use periodic non-expedited request; accept best-effort timing. |

## 12. Open items deferred to follow-on specs

- Character art assets (gradient orb placeholders until imagery is bundled).
- Final polish animations / accessibility audit (sub-project 7).
- Room migrations (no migrations until v1 ships).
- Optional: SmashWiki descriptions enrichment.
- Optional: search-by-move ("which characters have a frame-3 jab?") — out of scope for v1.

## 13. Secrets & gitignore

- `local.properties` — contains both `YOUTUBE_API_KEY` and `START_GG_TOKEN`; already gitignored.
- `.superpowers/` — brainstorm artifacts (mockups, server state); added to `.gitignore` during brainstorm.
- All cached data stored in Room is per-device; nothing leaves the phone.

## 14. Non-goals (explicit)

- Cloud sync of favorites / accounts (Firebase rejected; app is solo).
- Multi-language support beyond app strings (UI is FR by default per user — fine, no i18n framework).
- Real-time tournament tracking / live match feed.
- Crowdsourced or editable character data inside the app — curated JSON is author-only.
- Production-grade obfuscation / Play Store distribution.

---

**Next step:** the user reviews this spec. On approval, this spec is committed and the `writing-plans` skill is invoked to produce the first sub-project's (`Data Foundation`) implementation plan.
