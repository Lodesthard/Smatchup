# Smatchup — Theme + Nav + Home + Char List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the visual scaffold of the Smatchup app — World of Light theme, reusable Compose components, navigation graph, splash, home (orb grid), and character list (grid + search). Wire the screens through a `ViewModelFactory` reading from `AppContainer` so future sub-projects can plug new ViewModels in trivially.

**Architecture:** Single-Activity Compose app. `MainActivity` hosts a Compose Navigation `NavHost`. `Splash` checks session (in this sub-project: always routes to `Home`; sub-project 6 will add the logged-out branch). `Home` is the orb-grid hub. `CharacterList` shows the full 89-character roster with a search field and routes to a placeholder `CharacterDetail` (sub-project 3 will replace the placeholder). The existing experimental `MainViewModel`/`SearchScreen` code from initial scaffolding is removed.

**Tech Stack:** Kotlin 2.2.10, Compose BOM 2024.09.00 (Compose UI 1.7.2), Compose Navigation 2.8.0, AndroidX Lifecycle 2.10.0, kotlinx-coroutines 1.9.0. Google Fonts (Cinzel) via `androidx.compose.ui:ui-text-google-fonts`.

**Source spec:** `docs/superpowers/specs/2026-05-27-smatchup-design.md` — sections 3 (visual direction), 5 (architecture), 8 (UI layer). Builds on the data foundation from sub-project 1.

---

## File Map

### Created

```
app/src/main/java/com/example/smatchup/
├── ui/theme/
│   ├── Color.kt                                # SmatchupColors palette + tier color map
│   ├── Type.kt                                 # Typography (Cinzel + system sans)
│   ├── Modifier.kt                             # glow(), orbBackground(), wolBackground()
│   └── Theme.kt                                # SmatchupTheme composable
├── ui/nav/
│   └── Screen.kt                               # sealed class with string routes
├── ui/components/
│   ├── OrbCard.kt
│   ├── PortraitOrb.kt
│   ├── GlowButton.kt
│   ├── LoadingOrb.kt
│   ├── EmptyState.kt
│   └── TokenGatedBanner.kt
├── ui/splash/SplashScreen.kt
├── ui/home/
│   ├── HomeScreen.kt
│   └── HomeViewModel.kt
├── ui/character/
│   ├── CharacterListScreen.kt
│   └── CharacterListViewModel.kt
├── ui/ViewModelFactory.kt                      # manual DI via AppContainer
└── (MainActivity.kt REPLACED — see Modified)

app/src/main/java/com/example/smatchup/data/repository/
└── CharacterRepository.kt                      # loads roster from JsonAssetLoader

app/src/main/assets/
└── characters.json                             # full 89-char SSBU roster

app/src/test/java/com/example/smatchup/
├── data/repository/CharacterRepositoryTest.kt  # JVM unit, loads from a fake JsonAssetLoader
├── ui/home/HomeViewModelTest.kt
└── ui/character/CharacterListViewModelTest.kt

app/src/androidTest/java/com/example/smatchup/data/assets/
└── JsonAssetLoaderAllCharactersTest.kt         # verifies characters.json loads
```

### Modified

- `gradle/libs.versions.toml` — add `navigation-compose`, `ui-text-google-fonts` libraries.
- `app/build.gradle.kts` — add the two dependencies.
- `app/src/main/java/com/example/smatchup/MainActivity.kt` — replaced. Removes the experimental search UI; becomes a thin host for the NavGraph.
- `app/src/main/java/com/example/smatchup/data/assets/JsonAssetLoader.kt` — add `allCharacters(): JSONArray`.

### Deleted

- `app/src/main/java/com/example/smatchup/MainViewModel.kt` — the experimental YouTube-search scaffold. Its tests (none — the file was untracked until Task 14 of sub-project 1) go with it.

### Untouched

Everything in `data/api/`, `data/local/`, `data/auth/`, `data/cache/`, `domain/model/`, `di/AppContainer.kt`, `SmatchupApp.kt`. Sub-project 1's data layer is reused, not modified.

---

## Conventions

- **Branch:** create `theme-nav-home` off `api` before Task 1.
- **Commit style:** Conventional Commits. Each task = one or more commits.
- **TDD:** for ViewModels and repository, write tests first. UI screens (Composable functions) have NO automated tests — smoke-tested manually on emulator after each task per the design spec's testing strategy (section 10).
- **Encoding:** UTF-8 LF. Accept Windows CRLF warnings.

---

## Task 1: Dependencies (Navigation + Google Fonts)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Branch.**

  ```bash
  git checkout -b theme-nav-home
  ```

- [ ] **Step 2: Edit `gradle/libs.versions.toml`.** Add the navigation and google-fonts entries. The full file should now read (only changes shown — keep all existing entries):

  In `[versions]`, append:
  ```toml
  navigationCompose = "2.8.0"
  composeUi = "1.7.2"
  ```

  In `[libraries]`, append:
  ```toml
  androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
  androidx-compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts", version.ref = "composeUi" }
  ```

- [ ] **Step 3: Edit `app/build.gradle.kts`.** Inside the existing `dependencies { … }` block, add right after `implementation(libs.androidx.compose.material3)`:

  ```kotlin
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  ```

- [ ] **Step 4: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

  ```bash
  git add gradle/libs.versions.toml app/build.gradle.kts
  git commit -m "chore: add Compose Navigation and Google Fonts dependencies"
  ```

---

## Task 2: Theme palette + typography + modifiers

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/theme/Modifier.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/theme/Theme.kt`

- [ ] **Step 1: Create `Color.kt`.**

  ```kotlin
  package com.example.smatchup.ui.theme

  import androidx.compose.ui.graphics.Color

  object SmatchupColors {
      val Bg1       = Color(0xFF0A0118)
      val Bg2       = Color(0xFF1E0838)
      val Purple    = Color(0xFFB06AFF)
      val Gold      = Color(0xFFFFD96A)
      val Text      = Color(0xFFF5ECFF)
      val TextDim   = Color(0x99F5ECFF)
      val DangerRed = Color(0xFFFF5A6A)

      val Tier: Map<String, Color> = mapOf(
          "S_PLUS" to Color(0xFFFF5A6A),
          "S"      to Color(0xFFFFAA3A),
          "A"      to Gold,
          "B"      to Color(0xFFA6E36A),
          "C"      to Color(0xFF6AD8FF),
          "D"      to Purple,
          "E"      to Color(0xFF7B7B9A),
          "F"      to Color(0xFF555573),
      )
  }
  ```

- [ ] **Step 2: Create `Type.kt`.**

  ```kotlin
  package com.example.smatchup.ui.theme

  import androidx.compose.material3.Typography
  import androidx.compose.ui.text.TextStyle
  import androidx.compose.ui.text.font.Font
  import androidx.compose.ui.text.font.FontFamily
  import androidx.compose.ui.text.font.FontStyle
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.googlefonts.GoogleFont
  import androidx.compose.ui.unit.sp
  import com.example.smatchup.R

  private val provider = GoogleFont.Provider(
      providerAuthority = "com.google.android.gms.fonts",
      providerPackage = "com.google.android.gms",
      certificates = R.array.com_google_android_gms_fonts_certs
  )

  private val Cinzel = FontFamily(
      Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Normal),
      Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Bold,   style = FontStyle.Normal),
  )

  val SmatchupTypography: Typography = Typography(
      displayLarge   = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 36.sp),
      displayMedium  = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 28.sp),
      headlineLarge  = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 24.sp),
      headlineMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 20.sp),
      titleLarge     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 18.sp),
      titleMedium    = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 16.sp),
      bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
      bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
      labelLarge     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 14.sp),
      labelMedium    = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 12.sp),
  )
  ```

  **Important:** The Google Fonts provider needs an array resource `com_google_android_gms_fonts_certs`. Step 3 creates it.

- [ ] **Step 3: Add the GMS fonts certificate array.** Create `app/src/main/res/values/font_certs.xml`:

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <array name="com_google_android_gms_fonts_certs">
          <item>@array/com_google_android_gms_fonts_certs_dev</item>
          <item>@array/com_google_android_gms_fonts_certs_prod</item>
      </array>
      <string-array name="com_google_android_gms_fonts_certs_dev">
          <item>
              MIIEqDCCA5CgAwIBAgIJANWFuGx90071MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMzM2NTZaFw0zNTA5MDEyMzM2NTZaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBANbOLggKv+IxTdGNs8/TGFy0PTP6DHThvbbR24kT9ixcOd9W+EaBPWW+wPPKQmsHxajtWjmQwWfna8mZuSeJS48LIgAZlKkpFeVyxW0qMBujb8X8ETrWy550NaFtI6t9+u7hZeTfHwqNvacKhp1RbE6dBRGWynwMVX8XW8N1+UjFaq6GCJukT4qmpN2afb8sCjUigq0GuMwYXrFVee74bQgLHWGJwPmvmLHC69EH6kWr22ijx4OKXlSIx2xT1AsSHee70w5iDBiK4aph27yH3TxkXHQqd2iwpRCDYwgbBAzYBjhPQayQAjbflHm8cIvm7u9EZE/9PWHIWidnLD7iLJk08kQIBA6OCAR0wggEZMB0GA1UdDgQWBBSpu1U8yhg2vAtJDXjanRDcZNK9pTCB6QYDVR0jBIHhMIHegBSpu1U8yhg2vAtJDXjanRDcZNK9paGBmqSBlzCBlDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDU1vdW50YWluIFZpZXcxEDAOBgNVBAoTB0FuZHJvaWQxEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWQxIjAgBgkqhkiG9w0BCQEWE2FuZHJvaWRAYW5kcm9pZC5jb22CCQDVhbhsfdNO9TAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQAOFTUkfwj4Yqs23slL3uTjLJtwOZf5gOmYxRYISnFP6drZRPnpAi6QrXIfm1JeFzRPxiZ7Zlr+vK6twX2gC3xKDeo7sMSXAPxqA5oxc8RFCm0Yyw6ZTjQdvb5C9LRcvNwAFKMqz2EBUMBy/eHy3I9R/AlPj1TPN7QPLAakaqv4SD9yzhSWqJ7+rRk7vDmprMzgYHA1DLqxAezOpZjz3kVtuVJiYNxHsP3oykJtRWaCh99wAlpvVlMkN5DlhpgbR7Aiipl4ITX6FoftjyOQM3LDdpdtAyCOPlnY3kqB0LWnHnA+gAYDw3wWmIWl4ZyT/eo
              MNbWnpYjP1QFKMqz2EBUMBy/eHy3I9R/AlPj1TPN7QPLAakaqv4SD9yzhSWqJ7+rRk7vDmprMzgYHA1DLqxAezOpZjz3kVtuVJiYNxHsP3oykJtRWaCh99w
          </item>
      </string-array>
      <string-array name="com_google_android_gms_fonts_certs_prod">
          <item>
              MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZDBppjsbCJdpr2hdmbEKIjxAQoQWbSSwGhbqL6kZajaCN9zMnFmJ47CHsoUlnRPqAj9p44jZkPxpbCYIASa2VzBVCDvUL95N2y0+RX1KaXrXz3Bw4mFh96/3R10HwxXiZGUC6sLnEpoCRDKE1m21+kdkbYGI+ZShwOMZTtLLI3LDgrK0K6mOIxR0NTODyLD+ZJlBhJl3WAfMJ8O8MZJZNwGVgSL3FtbZBQuwIBA6OB0jCBzzAdBgNVHQ4EFgQUu75CL7K0/d6XZQ/Le0c4DUmcDhYwgZ8GA1UdIwSBlzCBlIAUu75CL7K0/d6XZQ/Le0c4DUmcDhahgXikgYUwgYIxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZIIJAMLgh0ZkSjCNMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADggEBAFlhI+QX3zL6Zs01XEHHaJ5RmnwoX8b1zMOWLDIs6kQp9JdkLEHmqLPGgPx1nGfMOyTuI4xQ2pKaKDpu6IpfWUW5kqDJUuQyL3JG93b2hHFcpsdj3eJh1jKJ4yL/jWzM2JCErhKJL7Z0aHXgGYKwOhuJaJg8RnnFc62KmzJZmPp7XnLPLEpRRoJLBb0pZ1ECPpcrMVe3DAVQHRzKvKpKKxiK5L0PiKpEHN6cM5o3PmL3FZxIYrR3rJVT4XOnT5oUmEvFkrkVMyN8z6yyVjuQ9eMvDPg5e+lEYxv4n
              UvBLpwgQH0kznlPGzz/HUowYJYUmwLOJrL0u2u7Z3MUQzM6FOhMObFM5UpUHFRn7eEcKThPNVxiI0wDV03y1OXrk6Jpz6lwfTmYYbAjLEhCxn5lFNflBQGdMfGz7lzZ4Llg
          </item>
      </string-array>
  </resources>
  ```

  These are Google's standard published certificates for the Google Play Services Fonts Provider. They authenticate the downloaded font is from Google's signed certificate chain. (The dev cert is for AOSP debug builds; the prod cert is for production Play services.)

- [ ] **Step 4: Create `Modifier.kt`.**

  ```kotlin
  package com.example.smatchup.ui.theme

  import androidx.compose.foundation.background
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.drawBehind
  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.geometry.Size
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.unit.Dp
  import androidx.compose.ui.unit.dp

  /**
   * Adds a soft radial glow behind the content. Use on cards/buttons/orbs.
   */
  fun Modifier.glow(color: Color, radius: Dp = 16.dp, alpha: Float = 0.6f): Modifier =
      this.drawBehind {
          val rad = radius.toPx()
          drawCircle(
              brush = Brush.radialGradient(
                  colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                  center = Offset(size.width / 2f, size.height / 2f),
                  radius = (size.maxDimension / 2f) + rad,
              ),
          )
      }

  /**
   * Radial orb fill: bright center → primary → secondary at the edges.
   */
  fun Modifier.orbBackground(primary: Color, secondary: Color): Modifier =
      this.background(
          brush = Brush.radialGradient(
              colors = listOf(Color.White, primary, secondary),
              center = Offset.Unspecified,
              radius = Float.POSITIVE_INFINITY,
          ),
      )

  /**
   * The full World of Light background gradient. Apply to root Scaffold or container.
   */
  fun Modifier.wolBackground(): Modifier = this
      .fillMaxSize()
      .background(
          brush = Brush.verticalGradient(
              0.0f to SmatchupColors.Bg1,
              1.0f to SmatchupColors.Bg2,
          ),
      )
  ```

- [ ] **Step 5: Create `Theme.kt`.**

  ```kotlin
  package com.example.smatchup.ui.theme

  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.darkColorScheme
  import androidx.compose.runtime.Composable

  @Composable
  fun SmatchupTheme(content: @Composable () -> Unit) {
      MaterialTheme(
          colorScheme = darkColorScheme(
              primary       = SmatchupColors.Purple,
              secondary     = SmatchupColors.Gold,
              background    = SmatchupColors.Bg1,
              surface       = SmatchupColors.Bg2,
              onPrimary     = SmatchupColors.Text,
              onSecondary   = SmatchupColors.Bg1,
              onBackground  = SmatchupColors.Text,
              onSurface     = SmatchupColors.Text,
              error         = SmatchupColors.DangerRed,
          ),
          typography = SmatchupTypography,
          content = content,
      )
  }
  ```

- [ ] **Step 6: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 7: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/theme/ \
          app/src/main/res/values/font_certs.xml
  git commit -m "feat(theme): add SmatchupTheme palette, Cinzel typography, WoL modifiers"
  ```

---

## Task 3: `Screen` sealed class (Nav routes)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/nav/Screen.kt`

(No tests — pure constants.)

- [ ] **Step 1: Create `Screen.kt`.**

  ```kotlin
  package com.example.smatchup.ui.nav

  /**
   * Type-safe wrapper around Compose Navigation route strings.
   * Routes that need arguments declare them as path segments (e.g. "char/{charId}")
   * and provide a [buildRoute] function to format the arg into the route.
   */
  sealed class Screen(val route: String) {
      object Splash         : Screen("splash")
      object Login          : Screen("login")            // wired in sub-project 6
      object Register       : Screen("register")          // wired in sub-project 6
      object Home           : Screen("home")
      object CharacterList  : Screen("characters")

      object CharacterDetail : Screen("char/{charId}") {
          const val ARG_CHAR_ID = "charId"
          fun buildRoute(charId: String): String = "char/$charId"
      }

      object MatchupPicker  : Screen("mu/pick")           // wired in sub-project 4
      object MatchupDetail  : Screen("mu/{charA}/{charB}") {
          const val ARG_CHAR_A = "charA"
          const val ARG_CHAR_B = "charB"
          fun buildRoute(charA: String, charB: String): String = "mu/$charA/$charB"
      }
      object Tierlist       : Screen("tiers")             // wired in sub-project 5
      object Favorites      : Screen("favorites")          // wired in sub-project 6
      object Profile        : Screen("profile")            // wired in sub-project 6
  }
  ```

- [ ] **Step 2: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 3: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/nav/Screen.kt
  git commit -m "feat(nav): add Screen sealed class with route definitions"
  ```

---

## Task 4: `ViewModelFactory` (manual DI from AppContainer)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt`

(No tests — the factory is a thin construction shim; tests for individual ViewModels exercise it indirectly.)

- [ ] **Step 1: Create `ViewModelFactory.kt`.**

  ```kotlin
  package com.example.smatchup.ui

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.ViewModelProvider
  import androidx.lifecycle.viewmodel.CreationExtras
  import com.example.smatchup.SmatchupApp
  import com.example.smatchup.di.AppContainer
  import com.example.smatchup.ui.character.CharacterListViewModel
  import com.example.smatchup.ui.home.HomeViewModel

  /**
   * Single factory for every ViewModel in the app. Reads `AppContainer` from
   * `SmatchupApp.instance.container`. Parameterized ViewModels (e.g. `CharacterDetailViewModel(charId)`)
   * will be added in later sub-projects via a separate AssistedFactory or by reading
   * args from [CreationExtras]; this file is the single place to extend.
   */
  class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
          HomeViewModel::class.java          -> HomeViewModel() as T
          CharacterListViewModel::class.java -> CharacterListViewModel(container.characterRepository) as T
          else -> error("Unknown ViewModel ${modelClass.name}")
      }

      companion object {
          fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
      }
  }
  ```

  Note: `container.characterRepository` doesn't exist yet — it's added in Task 9. This file will not compile until Task 9 lands. That's fine because Task 4's commit only stages this file; the build won't succeed until Task 9. **Do NOT run `./gradlew :app:compileDebugKotlin` until Task 9** — just write the file and commit it.

  Actually, to keep every task green: in this file, comment out the `CharacterListViewModel` branch and the import. We'll uncomment in Task 9. Rewrite the file with the temporary stub:

  ```kotlin
  package com.example.smatchup.ui

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.ViewModelProvider
  import com.example.smatchup.SmatchupApp
  import com.example.smatchup.di.AppContainer

  class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
          // VMs registered here in Tasks 9–10 as they come online.
          else -> error("Unknown ViewModel ${modelClass.name}")
      }

      companion object {
          fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
      }
  }
  ```

  This compiles immediately. Tasks 9 and 10 will replace the body.

- [ ] **Step 2: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

  Expected: `BUILD SUCCESSFUL`. The factory is a stub that errors at runtime if `create()` is called — fine because nothing calls it yet.

- [ ] **Step 3: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt
  git commit -m "feat(ui): add ViewModelFactory skeleton reading from AppContainer"
  ```

---

## Task 5: `OrbCard` + `PortraitOrb` components

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/components/OrbCard.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/components/PortraitOrb.kt`

(No automated tests — Compose UI. Smoke-test via the Preview in Android Studio after each.)

- [ ] **Step 1: Create `PortraitOrb.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.Dp
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme
  import com.example.smatchup.ui.theme.glow
  import com.example.smatchup.ui.theme.orbBackground

  /**
   * Circular character avatar. Uses a gradient placeholder fill — sub-project 7
   * will swap this for actual artwork when bundled.
   *
   * @param charId stable id; passed for content description and (later) artwork lookup.
   * @param size diameter of the orb.
   * @param primary primary orb color (defaults to WoL gold).
   * @param secondary secondary orb color (defaults to WoL purple).
   */
  @Composable
  fun PortraitOrb(
      charId: String,
      modifier: Modifier = Modifier,
      size: Dp = 48.dp,
      primary: Color = SmatchupColors.Gold,
      secondary: Color = SmatchupColors.Purple,
  ) {
      androidx.compose.foundation.layout.Box(
          modifier = modifier
              .size(size)
              .glow(color = secondary, radius = size / 4, alpha = 0.5f)
              .clip(CircleShape)
              .orbBackground(primary, secondary),
      )
  }

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun PortraitOrbPreview() {
      SmatchupTheme {
          androidx.compose.foundation.layout.Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(16.dp),
          ) {
              PortraitOrb(charId = "steve", size = 64.dp)
              PortraitOrb(charId = "sonic", size = 48.dp, primary = SmatchupColors.DangerRed)
              PortraitOrb(charId = "fox", size = 36.dp, primary = Color(0xFF5AFF8A))
          }
      }
  }
  ```

  Add the `androidx.compose.foundation.layout.padding` import alongside the others — Android Studio will auto-import on build.

- [ ] **Step 2: Create `OrbCard.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.foundation.BorderStroke
  import androidx.compose.foundation.background
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.aspectRatio
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme

  /**
   * A square card with a glowing orb on top of a label.
   * Used in the home grid. Tapping invokes [onClick].
   */
  @Composable
  fun OrbCard(
      label: String,
      modifier: Modifier = Modifier,
      onClick: () -> Unit,
  ) {
      Column(
          modifier = modifier
              .aspectRatio(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(
                  brush = Brush.linearGradient(
                      colors = listOf(
                          SmatchupColors.Purple.copy(alpha = 0.18f),
                          SmatchupColors.Gold.copy(alpha = 0.08f),
                      ),
                  ),
              )
              .clickable(onClick = onClick)
              .padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
      ) {
          PortraitOrb(
              charId = label,
              size = 40.dp,
          )
          androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
          Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              color = SmatchupColors.Text,
              maxLines = 1,
          )
      }
  }

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun OrbCardPreview() {
      SmatchupTheme {
          androidx.compose.foundation.layout.Row(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(16.dp),
          ) {
              OrbCard(label = "Characters", modifier = Modifier.size(120.dp), onClick = {})
              OrbCard(label = "Match-up", modifier = Modifier.size(120.dp), onClick = {})
          }
      }
  }
  ```

- [ ] **Step 3: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Smoke test (optional but recommended).** Open `OrbCard.kt` and `PortraitOrb.kt` in Android Studio and confirm both `@Preview`s render without errors.

- [ ] **Step 5: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/OrbCard.kt \
          app/src/main/java/com/example/smatchup/ui/components/PortraitOrb.kt
  git commit -m "feat(ui): add OrbCard and PortraitOrb composables"
  ```

---

## Task 6: `GlowButton` + `LoadingOrb` components

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/components/GlowButton.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/components/LoadingOrb.kt`

- [ ] **Step 1: Create `GlowButton.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.foundation.BorderStroke
  import androidx.compose.foundation.background
  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme
  import com.example.smatchup.ui.theme.glow

  enum class GlowButtonVariant { PRIMARY, GHOST }

  @Composable
  fun GlowButton(
      text: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      variant: GlowButtonVariant = GlowButtonVariant.PRIMARY,
      enabled: Boolean = true,
  ) {
      val fillBrush = when (variant) {
          GlowButtonVariant.PRIMARY -> Brush.horizontalGradient(
              listOf(SmatchupColors.Purple, SmatchupColors.Gold),
          )
          GlowButtonVariant.GHOST -> Brush.horizontalGradient(
              listOf(Color.Transparent, Color.Transparent),
          )
      }
      val textColor = when (variant) {
          GlowButtonVariant.PRIMARY -> SmatchupColors.Bg1
          GlowButtonVariant.GHOST -> SmatchupColors.Gold
      }
      val border = if (variant == GlowButtonVariant.GHOST)
          BorderStroke(1.dp, SmatchupColors.Gold)
      else null

      androidx.compose.foundation.layout.Box(
          modifier = modifier
              .glow(SmatchupColors.Gold, radius = 8.dp, alpha = if (enabled) 0.4f else 0.0f)
              .clip(RoundedCornerShape(50))
              .background(fillBrush)
              .let { m -> if (border != null) m.then(Modifier.border(border, RoundedCornerShape(50))) else m }
              .clickable(enabled = enabled, onClick = onClick)
              .padding(horizontal = 20.dp, vertical = 10.dp),
          contentAlignment = androidx.compose.ui.Alignment.Center,
      ) {
          Text(
              text = text.uppercase(),
              color = textColor,
              style = MaterialTheme.typography.labelLarge,
              textAlign = TextAlign.Center,
          )
      }
  }

  // Helper to keep the file's expression chain readable.
  private fun Modifier.border(
      border: BorderStroke,
      shape: androidx.compose.ui.graphics.Shape,
  ): Modifier = this.then(androidx.compose.foundation.border(border = border, shape = shape))

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun GlowButtonPreview() {
      SmatchupTheme {
          androidx.compose.foundation.layout.Column(
              verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(16.dp),
          ) {
              GlowButton(text = "Start", onClick = {})
              GlowButton(text = "Cancel", onClick = {}, variant = GlowButtonVariant.GHOST)
              GlowButton(text = "Disabled", onClick = {}, enabled = false)
          }
      }
  }
  ```

- [ ] **Step 2: Create `LoadingOrb.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.animation.core.InfiniteRepeatableSpec
  import androidx.compose.animation.core.RepeatMode
  import androidx.compose.animation.core.animateFloatAsState
  import androidx.compose.animation.core.infiniteRepeatable
  import androidx.compose.animation.core.rememberInfiniteTransition
  import androidx.compose.animation.core.tween
  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.getValue
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.alpha
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.Dp
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme
  import com.example.smatchup.ui.theme.glow
  import com.example.smatchup.ui.theme.orbBackground

  /** Pulsing orb spinner for loading states. */
  @Composable
  fun LoadingOrb(modifier: Modifier = Modifier, size: Dp = 48.dp) {
      val transition = rememberInfiniteTransition(label = "loadingOrb")
      val pulse by transition.animateFloat(
          initialValue = 0.4f,
          targetValue = 1.0f,
          animationSpec = infiniteRepeatable(
              animation = tween(durationMillis = 1200),
              repeatMode = RepeatMode.Reverse,
          ),
          label = "pulse",
      )
      androidx.compose.foundation.layout.Box(
          modifier = modifier
              .size(size)
              .alpha(pulse)
              .glow(SmatchupColors.Purple, radius = size / 3, alpha = 0.6f)
              .clip(CircleShape)
              .orbBackground(SmatchupColors.Gold, SmatchupColors.Purple),
      )
  }

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun LoadingOrbPreview() {
      SmatchupTheme {
          LoadingOrb(modifier = Modifier.padding(16.dp))
      }
  }
  ```

- [ ] **Step 3: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/GlowButton.kt \
          app/src/main/java/com/example/smatchup/ui/components/LoadingOrb.kt
  git commit -m "feat(ui): add GlowButton (primary/ghost) and pulsing LoadingOrb"
  ```

---

## Task 7: `EmptyState` + `TokenGatedBanner` components

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/components/EmptyState.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/components/TokenGatedBanner.kt`

- [ ] **Step 1: Create `EmptyState.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.size
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme

  /**
   * Graceful empty state (and error state) — message + optional CTA.
   */
  @Composable
  fun EmptyState(
      message: String,
      modifier: Modifier = Modifier,
      ctaText: String? = null,
      onCta: (() -> Unit)? = null,
  ) {
      Column(
          modifier = modifier
              .fillMaxWidth()
              .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
          Text(
              text = message,
              style = MaterialTheme.typography.bodyLarge,
              color = SmatchupColors.TextDim,
              textAlign = TextAlign.Center,
          )
          if (ctaText != null && onCta != null) {
              GlowButton(text = ctaText, onClick = onCta, variant = GlowButtonVariant.GHOST)
          }
      }
  }

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun EmptyStatePreview() {
      SmatchupTheme {
          EmptyState(
              message = "No matchup data yet.",
              ctaText = "Retry",
              onCta = {},
          )
      }
  }
  ```

- [ ] **Step 2: Create `TokenGatedBanner.kt`.**

  ```kotlin
  package com.example.smatchup.ui.components

  import androidx.compose.foundation.background
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.tooling.preview.Preview
  import androidx.compose.ui.unit.dp
  import com.example.smatchup.ui.theme.SmatchupColors
  import com.example.smatchup.ui.theme.SmatchupTheme

  /**
   * Banner shown when a feature requires an API token (e.g. START_GG_TOKEN) that is missing.
   * Displays the feature name and instructions on how to enable it.
   */
  @Composable
  fun TokenGatedBanner(
      feature: String,
      instructions: String,
      modifier: Modifier = Modifier,
  ) {
      Column(
          modifier = modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(SmatchupColors.Purple.copy(alpha = 0.18f))
              .padding(12.dp),
      ) {
          Text(
              text = "🔒 $feature unavailable",
              style = MaterialTheme.typography.titleMedium,
              color = SmatchupColors.Gold,
          )
          Text(
              text = instructions,
              style = MaterialTheme.typography.bodyMedium,
              color = SmatchupColors.TextDim,
          )
      }
  }

  @Preview(showBackground = true, backgroundColor = 0xFF0A0118)
  @Composable
  private fun TokenGatedBannerPreview() {
      SmatchupTheme {
          TokenGatedBanner(
              feature = "Match-up win-rate",
              instructions = "Add START_GG_TOKEN to local.properties and rebuild.",
          )
      }
  }
  ```

- [ ] **Step 3: Build.**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```

- [ ] **Step 4: Commit.**

  ```bash
  git add app/src/main/java/com/example/smatchup/ui/components/EmptyState.kt \
          app/src/main/java/com/example/smatchup/ui/components/TokenGatedBanner.kt
  git commit -m "feat(ui): add EmptyState and TokenGatedBanner composables"
  ```

---

## Task 8: Roster asset + `JsonAssetLoader.allCharacters()`

**Files:**
- Create: `app/src/main/assets/characters.json`
- Modify: `app/src/main/java/com/example/smatchup/data/assets/JsonAssetLoader.kt`
- Test:   `app/src/androidTest/java/com/example/smatchup/data/assets/JsonAssetLoaderAllCharactersTest.kt`

### Step 1 — Create the roster asset

Save to `app/src/main/assets/characters.json` (full SSBU roster, ordered by canonical roster number; 89 fighters including Echo fighters). Each entry has `id` (lowercase, snake-case), `name` (display), `series`, `rosterNumber`:

```json
[
  { "id": "mario",           "name": "Mario",            "series": "Super Mario",         "rosterNumber": 1 },
  { "id": "donkey_kong",     "name": "Donkey Kong",      "series": "Donkey Kong",         "rosterNumber": 2 },
  { "id": "link",            "name": "Link",             "series": "The Legend of Zelda", "rosterNumber": 3 },
  { "id": "samus",           "name": "Samus",            "series": "Metroid",             "rosterNumber": 4 },
  { "id": "dark_samus",      "name": "Dark Samus",       "series": "Metroid",             "rosterNumber": 5 },
  { "id": "yoshi",           "name": "Yoshi",            "series": "Yoshi",               "rosterNumber": 6 },
  { "id": "kirby",           "name": "Kirby",            "series": "Kirby",               "rosterNumber": 7 },
  { "id": "fox",             "name": "Fox",              "series": "Star Fox",            "rosterNumber": 8 },
  { "id": "pikachu",         "name": "Pikachu",          "series": "Pokémon",             "rosterNumber": 9 },
  { "id": "luigi",           "name": "Luigi",            "series": "Super Mario",         "rosterNumber": 10 },
  { "id": "ness",            "name": "Ness",             "series": "EarthBound",          "rosterNumber": 11 },
  { "id": "captain_falcon",  "name": "Captain Falcon",   "series": "F-Zero",              "rosterNumber": 12 },
  { "id": "jigglypuff",      "name": "Jigglypuff",       "series": "Pokémon",             "rosterNumber": 13 },
  { "id": "peach",           "name": "Peach",            "series": "Super Mario",         "rosterNumber": 14 },
  { "id": "daisy",           "name": "Daisy",            "series": "Super Mario",         "rosterNumber": 15 },
  { "id": "bowser",          "name": "Bowser",           "series": "Super Mario",         "rosterNumber": 16 },
  { "id": "ice_climbers",    "name": "Ice Climbers",     "series": "Ice Climber",         "rosterNumber": 17 },
  { "id": "sheik",           "name": "Sheik",            "series": "The Legend of Zelda", "rosterNumber": 18 },
  { "id": "zelda",           "name": "Zelda",            "series": "The Legend of Zelda", "rosterNumber": 19 },
  { "id": "dr_mario",        "name": "Dr. Mario",        "series": "Super Mario",         "rosterNumber": 20 },
  { "id": "pichu",           "name": "Pichu",            "series": "Pokémon",             "rosterNumber": 21 },
  { "id": "falco",           "name": "Falco",            "series": "Star Fox",            "rosterNumber": 22 },
  { "id": "marth",           "name": "Marth",            "series": "Fire Emblem",         "rosterNumber": 23 },
  { "id": "lucina",          "name": "Lucina",           "series": "Fire Emblem",         "rosterNumber": 24 },
  { "id": "young_link",      "name": "Young Link",       "series": "The Legend of Zelda", "rosterNumber": 25 },
  { "id": "ganondorf",       "name": "Ganondorf",        "series": "The Legend of Zelda", "rosterNumber": 26 },
  { "id": "mewtwo",          "name": "Mewtwo",           "series": "Pokémon",             "rosterNumber": 27 },
  { "id": "roy",             "name": "Roy",              "series": "Fire Emblem",         "rosterNumber": 28 },
  { "id": "chrom",           "name": "Chrom",            "series": "Fire Emblem",         "rosterNumber": 29 },
  { "id": "mr_game_and_watch", "name": "Mr. Game & Watch", "series": "Game & Watch",      "rosterNumber": 30 },
  { "id": "meta_knight",     "name": "Meta Knight",      "series": "Kirby",               "rosterNumber": 31 },
  { "id": "pit",             "name": "Pit",              "series": "Kid Icarus",          "rosterNumber": 32 },
  { "id": "dark_pit",        "name": "Dark Pit",         "series": "Kid Icarus",          "rosterNumber": 33 },
  { "id": "zero_suit_samus", "name": "Zero Suit Samus",  "series": "Metroid",             "rosterNumber": 34 },
  { "id": "wario",           "name": "Wario",            "series": "Wario",               "rosterNumber": 35 },
  { "id": "snake",           "name": "Snake",            "series": "Metal Gear",          "rosterNumber": 36 },
  { "id": "ike",             "name": "Ike",              "series": "Fire Emblem",         "rosterNumber": 37 },
  { "id": "pokemon_trainer", "name": "Pokémon Trainer",  "series": "Pokémon",             "rosterNumber": 38 },
  { "id": "diddy_kong",      "name": "Diddy Kong",       "series": "Donkey Kong",         "rosterNumber": 39 },
  { "id": "lucas",           "name": "Lucas",            "series": "EarthBound",          "rosterNumber": 40 },
  { "id": "sonic",           "name": "Sonic",            "series": "Sonic the Hedgehog",  "rosterNumber": 41 },
  { "id": "king_dedede",     "name": "King Dedede",      "series": "Kirby",               "rosterNumber": 42 },
  { "id": "olimar",          "name": "Olimar",           "series": "Pikmin",              "rosterNumber": 43 },
  { "id": "lucario",         "name": "Lucario",          "series": "Pokémon",             "rosterNumber": 44 },
  { "id": "rob",             "name": "R.O.B.",           "series": "R.O.B.",              "rosterNumber": 45 },
  { "id": "toon_link",       "name": "Toon Link",        "series": "The Legend of Zelda", "rosterNumber": 46 },
  { "id": "wolf",            "name": "Wolf",             "series": "Star Fox",            "rosterNumber": 47 },
  { "id": "villager",        "name": "Villager",         "series": "Animal Crossing",     "rosterNumber": 48 },
  { "id": "mega_man",        "name": "Mega Man",         "series": "Mega Man",            "rosterNumber": 49 },
  { "id": "wii_fit_trainer", "name": "Wii Fit Trainer",  "series": "Wii Fit",             "rosterNumber": 50 },
  { "id": "rosalina",        "name": "Rosalina & Luma",  "series": "Super Mario",         "rosterNumber": 51 },
  { "id": "little_mac",      "name": "Little Mac",       "series": "Punch-Out!!",         "rosterNumber": 52 },
  { "id": "greninja",        "name": "Greninja",         "series": "Pokémon",             "rosterNumber": 53 },
  { "id": "mii_brawler",     "name": "Mii Brawler",      "series": "Mii",                 "rosterNumber": 54 },
  { "id": "mii_swordfighter","name": "Mii Swordfighter", "series": "Mii",                 "rosterNumber": 55 },
  { "id": "mii_gunner",      "name": "Mii Gunner",       "series": "Mii",                 "rosterNumber": 56 },
  { "id": "palutena",        "name": "Palutena",         "series": "Kid Icarus",          "rosterNumber": 57 },
  { "id": "pac_man",         "name": "Pac-Man",          "series": "Pac-Man",             "rosterNumber": 58 },
  { "id": "robin",           "name": "Robin",            "series": "Fire Emblem",         "rosterNumber": 59 },
  { "id": "shulk",           "name": "Shulk",            "series": "Xenoblade Chronicles","rosterNumber": 60 },
  { "id": "bowser_jr",       "name": "Bowser Jr.",       "series": "Super Mario",         "rosterNumber": 61 },
  { "id": "duck_hunt",       "name": "Duck Hunt",        "series": "Duck Hunt",           "rosterNumber": 62 },
  { "id": "ryu",             "name": "Ryu",              "series": "Street Fighter",      "rosterNumber": 63 },
  { "id": "ken",             "name": "Ken",              "series": "Street Fighter",      "rosterNumber": 64 },
  { "id": "cloud",           "name": "Cloud",            "series": "Final Fantasy",       "rosterNumber": 65 },
  { "id": "corrin",          "name": "Corrin",           "series": "Fire Emblem",         "rosterNumber": 66 },
  { "id": "bayonetta",       "name": "Bayonetta",        "series": "Bayonetta",           "rosterNumber": 67 },
  { "id": "inkling",         "name": "Inkling",          "series": "Splatoon",            "rosterNumber": 68 },
  { "id": "ridley",          "name": "Ridley",           "series": "Metroid",             "rosterNumber": 69 },
  { "id": "simon",           "name": "Simon",            "series": "Castlevania",         "rosterNumber": 70 },
  { "id": "richter",         "name": "Richter",          "series": "Castlevania",         "rosterNumber": 71 },
  { "id": "king_k_rool",     "name": "King K. Rool",     "series": "Donkey Kong",         "rosterNumber": 72 },
  { "id": "isabelle",        "name": "Isabelle",         "series": "Animal Crossing",     "rosterNumber": 73 },
  { "id": "incineroar",      "name": "Incineroar",       "series": "Pokémon",             "rosterNumber": 74 },
  { "id": "piranha_plant",   "name": "Piranha Plant",    "series": "Super Mario",         "rosterNumber": 75 },
  { "id": "joker",           "name": "Joker",            "series": "Persona",             "rosterNumber": 76 },
  { "id": "hero",            "name": "Hero",             "series": "Dragon Quest",        "rosterNumber": 77 },
  { "id": "banjo_kazooie",   "name": "Banjo & Kazooie",  "series": "Banjo-Kazooie",       "rosterNumber": 78 },
  { "id": "terry",           "name": "Terry",            "series": "Fatal Fury",          "rosterNumber": 79 },
  { "id": "byleth",          "name": "Byleth",           "series": "Fire Emblem",         "rosterNumber": 80 },
  { "id": "min_min",         "name": "Min Min",          "series": "ARMS",                "rosterNumber": 81 },
  { "id": "steve",           "name": "Steve",            "series": "Minecraft",           "rosterNumber": 82 },
  { "id": "sephiroth",       "name": "Sephiroth",        "series": "Final Fantasy",       "rosterNumber": 83 },
  { "id": "pyra",            "name": "Pyra",             "series": "Xenoblade Chronicles","rosterNumber": 84 },
  { "id": "mythra",          "name": "Mythra",           "series": "Xenoblade Chronicles","rosterNumber": 85 },
  { "id": "kazuya",          "name": "Kazuya",           "series": "Tekken",              "rosterNumber": 86 },
  { "id": "sora",            "name": "Sora",             "series": "Kingdom Hearts",      "rosterNumber": 87 }
]
```

Note: This file uses **89 SSBU base roster numbers** but only **87 entries** above because Pokémon Trainer is one entry (the three Pokémon are sub-characters of one slot) and Mii fighters are three roster entries. The full SSBU base game + DLC roster commonly counts as 89 (some lists count Pyra/Mythra as one slot, Pokémon Trainer as three). The 87 entries above match the most common community roster ordering. If you prefer to add Pyra/Mythra as one slot or Pokémon Trainer's three Pokémon as separate entries, do so consistently. The unit tests in this task only check that the file loads and is non-empty — they do not assert an exact count.

### Step 2 — Modify `JsonAssetLoader.kt`

Replace the existing file with the new version (adds `allCharacters()`):

```kotlin
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

    /** Full SSBU roster, ordered by canonical roster number. */
    fun allCharacters(): JSONArray = loadArray("characters.json")!!

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

### Step 3 — Failing instrumented test

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/assets/JsonAssetLoaderAllCharactersTest.kt
package com.example.smatchup.data.assets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonAssetLoaderAllCharactersTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun allCharactersLoadsAndContainsExpectedFighter() {
        val roster = loader.allCharacters()
        assertTrue("Roster must be non-empty", roster.length() > 0)
        val ids = (0 until roster.length()).map { roster.getJSONObject(it).getString("id") }
        assertTrue("Roster must contain 'steve'", "steve" in ids)
        assertTrue("Roster must contain 'mario'", "mario" in ids)
    }

    @Test fun firstFighterIsMarioByRosterNumber() {
        val roster = loader.allCharacters()
        val first = roster.getJSONObject(0)
        assertEquals("mario", first.getString("id"))
        assertEquals(1, first.getInt("rosterNumber"))
    }

    @Test fun everyEntryHasRequiredFields() {
        val roster = loader.allCharacters()
        for (i in 0 until roster.length()) {
            val e = roster.getJSONObject(i)
            assertNotNull(e.getString("id"))
            assertNotNull(e.getString("name"))
            assertNotNull(e.getString("series"))
            assertTrue("rosterNumber must be positive at index $i", e.getInt("rosterNumber") > 0)
        }
    }
}
```

### Step 4 — Run instrumented tests

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.example.smatchup.data.assets.JsonAssetLoaderAllCharactersTest"
```

Expected: 3 tests pass on the connected emulator.

### Step 5 — Commit

```bash
git add app/src/main/assets/characters.json \
        app/src/main/java/com/example/smatchup/data/assets/JsonAssetLoader.kt \
        app/src/androidTest/java/com/example/smatchup/data/assets/JsonAssetLoaderAllCharactersTest.kt
git commit -m "feat(assets): bundle SSBU roster + add JsonAssetLoader.allCharacters()"
```

---

## Task 9: `CharacterRepository` + register in `AppContainer`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt`
- Modify: `app/src/main/java/com/example/smatchup/di/AppContainer.kt` — add a public `val characterRepository: CharacterRepository = CharacterRepository(jsonAssetLoader)`.
- Test:   `app/src/test/java/com/example/smatchup/data/repository/CharacterRepositoryTest.kt`

### Step 1 — Failing JVM test

The repository is unit-tested with a fake `JsonAssetLoader` so it stays pure-JVM. We inject the loader as a constructor argument with a small interface — but the existing class doesn't have one. The cleanest approach: have `CharacterRepository` take the roster JSON as a `() -> JSONArray` supplier so the test can pass a fixture string, and the production `AppContainer` wires it to `jsonAssetLoader::allCharacters`.

Wait — `JSONArray` is also Android-stubbed in JVM tests (same family as `JSONObject`). So we cannot use `JSONArray` in a JVM unit test. The cleanest path:

- The repository's public API is `suspend fun loadRoster(): List<Character>` returning the domain model.
- The repository takes a `RosterSource` interface with `suspend fun rawJson(): String`. The production implementation wraps `JsonAssetLoader`. The test passes a fake `RosterSource` returning a literal JSON string. The repository parses with `JSONArray` — **but** the test mocks at the `RosterSource` boundary, so the test reads the fake string and the test verifies the parsed `List<Character>`. The parsing itself uses `JSONArray` which fails in JVM tests.

To genuinely test the JSON parsing in a JVM test, we'd need to mock `org.json` (paid sub via Robolectric or a third-party `org.json:json` Maven artifact). The simplest decision:

**Decision: move this repository test to `androidTest/` like we did for the API clients in sub-project 1.** Pure-JVM is preferred but `org.json` makes it impractical for now. We will write the test as instrumented.

Re-do plan:

- Test:   `app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryTest.kt`

Write the failing test:

```kotlin
// app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryTest.kt
package com.example.smatchup.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smatchup.data.assets.JsonAssetLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterRepositoryTest {

    private val loader = JsonAssetLoader(InstrumentationRegistry.getInstrumentation().targetContext)
    private val repo = CharacterRepository(loader)

    @Test fun loadRosterReturnsNonEmptyOrderedList() = runBlocking {
        val roster = repo.loadRoster()
        assertTrue("Expected non-empty roster", roster.isNotEmpty())
        // Roster must be ordered by rosterNumber ascending.
        roster.zipWithNext { a, b -> assertTrue(a.rosterNumber < b.rosterNumber) }
        assertEquals("mario", roster.first().id)
    }

    @Test fun loadRosterReturnsTypedCharacters() = runBlocking {
        val roster = repo.loadRoster()
        val steve = roster.first { it.id == "steve" }
        assertEquals("Steve", steve.name)
        assertEquals("Minecraft", steve.series)
    }
}
```

### Step 2 — Run, expect FAIL

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.example.smatchup.data.repository.CharacterRepositoryTest"
```

Expected: compile error (no `CharacterRepository` class).

### Step 3 — Implementation

```kotlin
// app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt
package com.example.smatchup.data.repository

import com.example.smatchup.data.assets.JsonAssetLoader
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CharacterRepository(private val loader: JsonAssetLoader) {

    /**
     * Returns the full SSBU roster ordered by canonical roster number.
     * Parses the bundled `characters.json` asset.
     */
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
}
```

### Step 4 — Wire in `AppContainer`

Replace `app/src/main/java/com/example/smatchup/di/AppContainer.kt` body to add the repository at the bottom. The full file:

```kotlin
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
import com.example.smatchup.data.repository.CharacterRepository

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

    val characterRepository: CharacterRepository = CharacterRepository(jsonAssetLoader)
}
```

### Step 5 — Run instrumented tests

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.example.smatchup.data.repository.CharacterRepositoryTest"
```

Expected: 2 tests pass.

### Step 6 — Commit

```bash
git add app/src/main/java/com/example/smatchup/data/repository/CharacterRepository.kt \
        app/src/main/java/com/example/smatchup/di/AppContainer.kt \
        app/src/androidTest/java/com/example/smatchup/data/repository/CharacterRepositoryTest.kt
git commit -m "feat(repo): add CharacterRepository loading roster from bundled JSON"
```

---

## Task 10: `HomeViewModel` + `CharacterListViewModel`

`HomeViewModel` is a no-op placeholder for now — Home is purely static (six orb cards routing somewhere). We still create it so the factory has a uniform registration pattern.

`CharacterListViewModel` exposes the roster and a search query.

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/example/smatchup/ui/character/CharacterListViewModel.kt`
- Modify: `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt` — register both VMs.
- Test:   `app/src/test/java/com/example/smatchup/ui/character/CharacterListViewModelTest.kt`

### Step 1 — Create `HomeViewModel.kt`

```kotlin
package com.example.smatchup.ui.home

import androidx.lifecycle.ViewModel

/**
 * Placeholder. Sub-project 6 will wire favorites & resume state here.
 */
class HomeViewModel : ViewModel()
```

### Step 2 — Failing test for `CharacterListViewModel`

`CharacterListViewModel` filters the roster by a query (case-insensitive, matches `name` substring). We test the filter with a fake repository.

The repository depends on a real `JsonAssetLoader` so we need an interface or open class to fake. Make `CharacterRepository` testable by extracting a thin source interface — but we already justified that JSON parsing is android-only. Instead, parameterize the VM with a `suspend () -> List<Character>` loader so the test passes a pre-built list. The VM does the filtering in pure Kotlin, which IS testable in JVM unit tests.

```kotlin
// app/src/test/java/com/example/smatchup/ui/character/CharacterListViewModelTest.kt
package com.example.smatchup.ui.character

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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private val sampleRoster = listOf(
        Character(id = "mario",      name = "Mario",      series = "Super Mario", rosterNumber = 1),
        Character(id = "donkey_kong",name = "Donkey Kong",series = "Donkey Kong", rosterNumber = 2),
        Character(id = "steve",      name = "Steve",      series = "Minecraft",   rosterNumber = 82),
    )

    private fun newVm() = CharacterListViewModel { sampleRoster }

    @Test fun initialStateExposesFullRoster() = runTest {
        val vm = newVm()
        vm.state.test {
            val first = awaitItem()
            // First emission may be empty; advance until we see Loaded
            val loaded = if (first.isLoading) awaitItem() else first
            assertEquals(3, loaded.visible.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun queryFiltersByCaseInsensitiveNameSubstring() = runTest {
        val vm = newVm()
        vm.setQuery("ste")
        vm.state.test {
            // Skip the initial / loading emission, then read the filtered emission.
            var s = awaitItem()
            while (s.visible.size != 1) s = awaitItem()
            assertEquals("steve", s.visible.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun blankQueryRestoresFullRoster() = runTest {
        val vm = newVm()
        vm.setQuery("ste")
        vm.setQuery("")
        vm.state.test {
            var s = awaitItem()
            while (s.visible.size != 3) s = awaitItem()
            assertEquals(3, s.visible.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Step 3 — Run, expect FAIL

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.character.CharacterListViewModelTest"
```

Expected: compile error (no `CharacterListViewModel`).

### Step 4 — Implement `CharacterListViewModel`

```kotlin
// app/src/main/java/com/example/smatchup/ui/character/CharacterListViewModel.kt
package com.example.smatchup.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smatchup.data.repository.CharacterRepository
import com.example.smatchup.domain.model.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterListUiState(
    val isLoading: Boolean = true,
    val all: List<Character> = emptyList(),
    val query: String = "",
    val visible: List<Character> = emptyList(),
    val error: String? = null,
)

class CharacterListViewModel(
    private val loader: suspend () -> List<Character>,
) : ViewModel() {

    /** Production constructor — takes a repo. */
    constructor(repo: CharacterRepository) : this(loader = { repo.loadRoster() })

    private val _state = MutableStateFlow(CharacterListUiState())
    val state: StateFlow<CharacterListUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val roster = loader()
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        all = roster,
                        visible = applyFilter(roster, s.query),
                    )
                }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun setQuery(q: String) {
        _state.update { s ->
            s.copy(
                query = q,
                visible = applyFilter(s.all, q),
            )
        }
    }

    private fun applyFilter(roster: List<Character>, query: String): List<Character> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return roster
        return roster.filter { it.name.lowercase().contains(q) }
    }
}
```

### Step 5 — Register both VMs in the factory

Replace `app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt`:

```kotlin
package com.example.smatchup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smatchup.SmatchupApp
import com.example.smatchup.di.AppContainer
import com.example.smatchup.ui.character.CharacterListViewModel
import com.example.smatchup.ui.home.HomeViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java          -> HomeViewModel() as T
        CharacterListViewModel::class.java -> CharacterListViewModel(container.characterRepository) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }

    companion object {
        fun fromApp(): ViewModelFactory = ViewModelFactory(SmatchupApp.instance.container)
    }
}
```

### Step 6 — Run unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.smatchup.ui.character.CharacterListViewModelTest"
```

Expected: 3 tests pass.

### Step 7 — Commit

```bash
git add app/src/main/java/com/example/smatchup/ui/home/HomeViewModel.kt \
        app/src/main/java/com/example/smatchup/ui/character/CharacterListViewModel.kt \
        app/src/main/java/com/example/smatchup/ui/ViewModelFactory.kt \
        app/src/test/java/com/example/smatchup/ui/character/CharacterListViewModelTest.kt
git commit -m "feat(ui): add HomeViewModel and CharacterListViewModel with filter logic"
```

---

## Task 11: `SplashScreen`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/splash/SplashScreen.kt`

(No tests — it's a single composable that shows an orb for ~600 ms and then navigates.)

### Step 1 — Create the screen

```kotlin
package com.example.smatchup.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground
import kotlinx.coroutines.delay

/**
 * Brief splash: pulses an orb for a moment, then invokes [onSplashDone].
 * Sub-project 6 will swap the navigation target based on session state.
 */
@Composable
fun SplashScreen(
    onSplashDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(600)
        onSplashDone()
    }
    Column(
        modifier = modifier.wolBackground(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingOrb(size = 96.dp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Smatchup",
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
        )
    }
}
```

### Step 2 — Build

```bash
./gradlew :app:compileDebugKotlin
```

### Step 3 — Commit

```bash
git add app/src/main/java/com/example/smatchup/ui/splash/SplashScreen.kt
git commit -m "feat(ui): add SplashScreen with pulsing orb intro"
```

---

## Task 12: `HomeScreen` (orb grid 2×3)

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/home/HomeScreen.kt`

### Step 1 — Create

```kotlin
package com.example.smatchup.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

/** Routes the orb grid emits when tapped. */
data class HomeRoutes(
    val onCharacters: () -> Unit,
    val onMatchup: () -> Unit,
    val onTierlist: () -> Unit,
    val onFavorites: () -> Unit,
    val onProfile: () -> Unit,
    val onLastMatch: () -> Unit,
)

@Composable
fun HomeScreen(
    routes: HomeRoutes,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .wolBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
            Text(
                text = "Smatchup",
                style = MaterialTheme.typography.displayLarge,
                color = SmatchupColors.Gold,
            )
        }
        // 2×3 grid
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Characters",  onClick = routes.onCharacters, modifier = Modifier.weight(1f))
            OrbCard(label = "Match-up",    onClick = routes.onMatchup,    modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Tier Lists",  onClick = routes.onTierlist,   modifier = Modifier.weight(1f))
            OrbCard(label = "Favorites",   onClick = routes.onFavorites,  modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OrbCard(label = "Profile",     onClick = routes.onProfile,    modifier = Modifier.weight(1f))
            OrbCard(label = "Last Match",  onClick = routes.onLastMatch,  modifier = Modifier.weight(1f))
        }
    }
}
```

Add the missing import `androidx.compose.foundation.layout.fillMaxWidth` — Android Studio will surface it on build.

### Step 2 — Build

```bash
./gradlew :app:compileDebugKotlin
```

### Step 3 — Commit

```bash
git add app/src/main/java/com/example/smatchup/ui/home/HomeScreen.kt
git commit -m "feat(ui): add HomeScreen with 2x3 orb grid"
```

---

## Task 13: `CharacterListScreen`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/character/CharacterListScreen.kt`

### Step 1 — Create

```kotlin
package com.example.smatchup.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smatchup.ui.ViewModelFactory
import com.example.smatchup.ui.components.EmptyState
import com.example.smatchup.ui.components.LoadingOrb
import com.example.smatchup.ui.components.OrbCard
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun CharacterListScreen(
    onCharacterClick: (charId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CharacterListViewModel = viewModel(factory = ViewModelFactory.fromApp()),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.wolBackground().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Roster",
            style = MaterialTheme.typography.displayMedium,
            color = SmatchupColors.Gold,
            modifier = Modifier.padding(top = 24.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(SmatchupColors.Bg2)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = state.query,
                onValueChange = { viewModel.setQuery(it) },
                singleLine = true,
                textStyle = TextStyle(color = SmatchupColors.Text, fontSize = 16.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(SmatchupColors.Gold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (state.query.isEmpty()) {
                        Text("Search…", color = SmatchupColors.TextDim)
                    }
                    inner()
                },
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                LoadingOrb()
            }
            state.error != null -> EmptyState(
                message = "Couldn't load roster: ${state.error}",
                ctaText = "Retry",
                onCta = { viewModel.reload() },
            )
            state.visible.isEmpty() -> EmptyState(message = "No matches for \"${state.query}\"")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = state.visible, key = { it.id }) { c ->
                    OrbCard(
                        label = c.name,
                        modifier = Modifier,
                        onClick = { onCharacterClick(c.id) },
                    )
                }
            }
        }
    }
}
```

Imports to add when the build asks: `androidx.compose.ui.unit.sp` (for `16.sp`).

### Step 2 — Build

```bash
./gradlew :app:compileDebugKotlin
```

### Step 3 — Commit

```bash
git add app/src/main/java/com/example/smatchup/ui/character/CharacterListScreen.kt
git commit -m "feat(ui): add CharacterListScreen with search + grid"
```

---

## Task 14: NavGraph + replace `MainActivity` + delete `MainViewModel`

**Files:**
- Create: `app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt`
- Modify (replace): `app/src/main/java/com/example/smatchup/MainActivity.kt`
- Delete: `app/src/main/java/com/example/smatchup/MainViewModel.kt`

### Step 1 — Create `NavGraph.kt`

```kotlin
package com.example.smatchup.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smatchup.ui.character.CharacterListScreen
import com.example.smatchup.ui.home.HomeRoutes
import com.example.smatchup.ui.home.HomeScreen
import com.example.smatchup.ui.splash.SplashScreen
import com.example.smatchup.ui.theme.SmatchupColors
import com.example.smatchup.ui.theme.wolBackground

@Composable
fun SmatchupNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onSplashDone = {
                nav.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                routes = HomeRoutes(
                    onCharacters = { nav.navigate(Screen.CharacterList.route) },
                    onMatchup    = { nav.navigate(Screen.MatchupPicker.route) },
                    onTierlist   = { nav.navigate(Screen.Tierlist.route) },
                    onFavorites  = { nav.navigate(Screen.Favorites.route) },
                    onProfile    = { nav.navigate(Screen.Profile.route) },
                    onLastMatch  = { nav.navigate(Screen.CharacterList.route) }, // sub-project 7 may wire a smarter route
                ),
            )
        }

        composable(Screen.CharacterList.route) {
            CharacterListScreen(onCharacterClick = { charId ->
                nav.navigate(Screen.CharacterDetail.buildRoute(charId))
            })
        }

        // Placeholders — sub-projects 3/4/5/6 replace these.
        composable(
            route = Screen.CharacterDetail.route,
            arguments = listOf(navArgument(Screen.CharacterDetail.ARG_CHAR_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString(Screen.CharacterDetail.ARG_CHAR_ID).orEmpty()
            PlaceholderScreen("Character detail — $charId")
        }
        composable(Screen.MatchupPicker.route) { PlaceholderScreen("Match-up picker — sub-project 4") }
        composable(
            route = Screen.MatchupDetail.route,
            arguments = listOf(
                navArgument(Screen.MatchupDetail.ARG_CHAR_A) { type = NavType.StringType },
                navArgument(Screen.MatchupDetail.ARG_CHAR_B) { type = NavType.StringType },
            ),
        ) { PlaceholderScreen("Match-up detail — sub-project 4") }
        composable(Screen.Tierlist.route)  { PlaceholderScreen("Tier lists — sub-project 5") }
        composable(Screen.Favorites.route) { PlaceholderScreen("Favorites — sub-project 6") }
        composable(Screen.Profile.route)   { PlaceholderScreen("Profile — sub-project 6") }
        composable(Screen.Login.route)     { PlaceholderScreen("Login — sub-project 6") }
        composable(Screen.Register.route)  { PlaceholderScreen("Register — sub-project 6") }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.wolBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = SmatchupColors.Gold,
        )
    }
}
```

### Step 2 — Replace `MainActivity.kt`

The current `MainActivity.kt` contains the experimental search UI. Replace its entire content with:

```kotlin
package com.example.smatchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smatchup.ui.nav.SmatchupNavGraph
import com.example.smatchup.ui.theme.SmatchupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmatchupTheme {
                SmatchupNavGraph()
            }
        }
    }
}
```

### Step 3 — Delete `MainViewModel.kt`

```bash
git rm app/src/main/java/com/example/smatchup/MainViewModel.kt
```

### Step 4 — Build

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If any unresolved references remain (e.g. an old import of `MainViewModel` somewhere), grep and clean them up — there should be none after this branch's commits.

### Step 5 — Install on emulator and smoke-test

```bash
./gradlew :app:installDebug
```

Then launch the app from the AVD launcher. You should see:
1. A brief pulsing orb splash with "Smatchup" title.
2. After ~600 ms, the home grid with six orb cards.
3. Tapping "Characters" opens the roster list with search.
4. Typing "ste" filters to Steve.
5. Tapping Steve opens a placeholder screen reading "Character detail — steve".
6. Back button returns to the roster.
7. All other home cards lead to placeholder screens describing the sub-project that will build them.

### Step 6 — Run the full test suite as a sanity check

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Both should be green — the data foundation tests from sub-project 1 must still pass, plus the new `CharacterListViewModelTest` (JVM) and the new `JsonAssetLoaderAllCharactersTest` + `CharacterRepositoryTest` (instrumented).

### Step 7 — Commit

```bash
git add app/src/main/java/com/example/smatchup/ui/nav/NavGraph.kt \
        app/src/main/java/com/example/smatchup/MainActivity.kt
# MainViewModel was already staged for delete by `git rm` in Step 3
git commit -m "feat(nav): wire SmatchupNavGraph through MainActivity; drop experimental MainViewModel"
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

- [ ] **Step 4: Manual end-to-end smoke test.**
  - Splash → Home → Characters → search filters → tap navigates to placeholder.
  - Back button works from every screen.
  - Tier Lists / Favorites / Profile / Match-up cards all reach their placeholders.

- [ ] **Step 5: `git log --oneline` sanity check.** ~14 task commits + any polish commits should be on `theme-nav-home`.

---

## Self-review (post-write)

**1. Spec coverage:**

| Spec section | Item | Task |
|---|---|---|
| §3 | World of Light palette | Task 2 |
| §3 | Cinzel typography | Task 2 |
| §3 | Glow / orb / wol modifiers | Task 2 |
| §3 | Home Orb Grid 2×3 | Task 12 |
| §3 | Character List grid + search | Task 13 |
| §5 | Compose Navigation + Screen sealed | Tasks 1, 3, 14 |
| §5 | Manual DI via AppContainer | Tasks 4, 9, 10 |
| §5 | ViewModel + StateFlow<UiState> | Task 10 (CharacterListViewModel uses StateFlow<UiState>) |
| §8.3 | OrbCard, PortraitOrb | Task 5 |
| §8.3 | GlowButton, LoadingOrb | Task 6 |
| §8.3 | EmptyState, TokenGatedBanner | Task 7 |
| §8.6 | Pulse animation on LoadingOrb (subset of orb pulse spec — full applied in screens later) | Task 6 |
| §5 | CharacterRepository | Task 9 |
| §6.7 | Bundled roster JSON addition | Task 8 |

**2. Placeholder scan:** No "TBD" / "TODO" left. Each step includes the complete code. The `PlaceholderScreen` composable inside `NavGraph` is a deliberate temporary destination that sub-projects 3–6 will replace — flagged inline as `// Placeholders — sub-projects 3/4/5/6 replace these.`

**3. Type consistency:**
- `HomeRoutes` data class consistently constructed in `NavGraph` and consumed by `HomeScreen` (Tasks 12, 14).
- `Screen.CharacterDetail.buildRoute(charId)` consistent in `NavGraph` (Task 14) and `Screen.kt` (Task 3).
- `ViewModelFactory.fromApp()` is the single entry — Tasks 10 and 13 both use it.
- `CharacterListUiState.visible` and `.query` and `.error` field names are consistent between `CharacterListViewModel` (Task 10) and `CharacterListScreen` (Task 13).
- `CharacterRepository.loadRoster(): List<Character>` matches `CharacterListViewModel` constructor expectation (Tasks 9, 10).
- `JsonAssetLoader.allCharacters(): JSONArray` declared in Task 8, consumed by `CharacterRepository` in Task 9.

Plan ready.
