# Dynamic Time-of-Day Greetings & Opening Splash Screen Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement two core features: (1) Rich, context-aware **Dynamic Time-of-Day Greetings** ("Good morning", "Good afternoon", "Good evening", "Good night") on the Home screen with warm, rotating mood subtitles and personalized user names; and (2) A stunning **Opening App Splash Screen** powered by the user's uploaded "AuraMusic BY LIKHITH" neon glowing soundwave artwork with high-end entrance scale and exit dissolve transitions.

**Architecture:** Shared Compose Multiplatform implementation in `sharedUi`:
1. `sharedUi/.../ui/splash/AuraSplashScreen.kt`: Upgraded to display the high-resolution `aura_splash_screen.png` image with a deep AMOLED pitch-black background, gentle scale/fade entrance, luminous neon glow aura, and butter-smooth alpha handoff to the preloaded main screen.
2. `sharedUi/.../ui/home/AuraGreetingAndHighlights.kt`: Calculates local time-of-day (`MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`), picks high-quality curated mood responses, integrates account first name, and formats for both Classic Aura and Nothing OS modes.
3. `sharedUi/.../ui/screens/HomeScreen.kt`: Embeds the greeting and feature highlight carousel directly at the feed header.
4. `app/.../MainActivity.kt` & `desktopApp/.../Main.kt`: Wire the splash screen and platform action routing.

**Tech Stack:** Kotlin Compose Multiplatform, Material3, Compose Resources (`Res.drawable.aura_splash_screen`), StateFlow, Java Time (`Calendar` / `LocalTime`).

---

### Task 1: Integrate Uploaded Artwork into Shared Opening Splash Screen (`AuraSplashScreen.kt`)

**Files:**
- Asset: `sharedUi/src/commonMain/composeResources/drawable/aura_splash_screen.png` (already copied from user upload)
- Asset: `app/src/main/res/drawable-nodpi/aura_splash_screen.png` (for Android)
- Asset: `desktopApp/src/main/composeResources/drawable/aura_splash_screen.png` (for Desktop)
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/splash/AuraSplashScreen.kt`

**Step 1: Inspect and Upgrade `AuraSplashScreen.kt`**
- Replace the current programmatic text and generic vector icon with the full-fidelity uploaded artwork:
  - Background: Pitch-black AMOLED `#000000`.
  - Image: `painterResource(Res.drawable.aura_splash_screen)` rendered with `ContentScale.Fit` inside a centered container.
  - Motion sequence:
    - **0ms – 400ms (Entrance):** Alpha animates `0f -> 1f`, scale animates `0.92f -> 1.0f` using `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`.
    - **Luminous Aura Bloom:** A subtle radial gradient pulse behind the soundwave expanding with the scale to give an authentic luminous glow.
    - **400ms – 1300ms (Appreciation Hold):** 900ms hold allowing the user to appreciate the "AuraMusic BY LIKHITH" branding while main UI finishes pre-warming.
    - **1300ms – 1600ms (Exit Dissolve):** Smooth fade-out (`alpha -> 0f`) reveals the pre-rendered Home screen seamlessly.
    - **Instant Skip:** Tap/click anywhere dismisses immediately.

---

### Task 2: Build Rich Dynamic Time-of-Day Greetings Domain & UI (`AuraGreetingAndHighlights.kt`)

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/home/AuraGreetingAndHighlights.kt`

**Step 1: Time-of-Day Enum & High-Quality Responses**
- Define `TimeOfDay`:
  - `MORNING` (05:00 – 11:59):
    - Greeting: `"Good morning"`
    - Emoji: `🌅`
    - Subtitle options:
      - `"Start your morning with energizing rhythms"`
      - `"Fresh beats brewed for your day"`
      - `"Your morning coffee soundtrack is ready"`
  - `AFTERNOON` (12:00 – 16:59):
    - Greeting: `"Good afternoon"`
    - Emoji: `☀️`
    - Subtitle options:
      - `"Power through your afternoon with focus beats"`
      - `"Midday energy boost & favorite grooves"`
      - `"Stay in the zone with your daily soundtrack"`
  - `EVENING` (17:00 – 21:59):
    - Greeting: `"Good evening"`
    - Emoji: `🌆`
    - Subtitle options:
      - `"Unwind and recharge with your chill mix"`
      - `"Golden hour melodies for a calm evening"`
      - `"Time to slow down and let the music play"`
  - `NIGHT` (22:00 – 04:59):
    - Greeting: `"Good night"`
    - Emoji: `🌙`
    - Subtitle options:
      - `"Late night reflections & soft ambient tunes"`
      - `"Dim the lights and immerse in deep soundscapes"`
      - `"Midnight melodies for peaceful dreaming"`

**Step 2: Account Name Personalization**
- Extract first name from `accountName` (e.g. `"Likhith Kami"` -> `"Likhith"`).
- When present: `"Good morning, Likhith"`.
- When absent: `"Good morning"`.

**Step 3: Dual-Theme Styling**
- **Classic Aura Theme:**
  - Modern typography (`MaterialTheme.typography.displaySmall` / `displayMedium`).
  - Glowing ambient pill badge with time icon (`🌅 08:30 AM`).
  - Warm, subtle gradient subtitle.
- **Nothing OS Theme:**
  - Dot-matrix uppercase styling: `G O O D  M O R N I N G`.
  - Red LED indicator dot (`NothingThemeTokens.Red`).
  - High-contrast monochromatic slate subtitle (`NothingThemeTokens.MutedText`).

**Step 4: Interactive Feature Spotlight Carousel**
- Feature discovery cards:
  1. **Nothing OS UI Mode** (`AESTHETIC`) -> 1-tap live theme toggle.
  2. **2026 Replay Recap** (`SPECIAL`) -> Opens Replay 3D hologram card.
  3. **Live Synced Karaoke Lyrics** (`LIVE`) -> Explores real-time synchronized lyrics.
  4. **Listen Together / Party Sync** (`SOCIAL`) -> Connects music room with friends.
  5. **10-Band Studio Equalizer** (`STUDIO`) -> Opens audio tuning & bass boost.
  6. **Smart Sleep Timer** (`NIGHT`) -> Sets bedtime sleep timer.
  7. **Social Story Generator** (`SHARE`) -> Creates Instagram/WhatsApp story card.
  8. **Discord Rich Presence** (`STREAM`) -> Configures live Discord playback status.

---

### Task 3: Embed Greeting & Feature Spotlight in `HomeScreen.kt`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt`

**Step 1: Add Parameters**
- Add `accountName: String? = null` and `onFeatureClick: ((AuraFeature) -> Unit)? = null` to `HomeScreen`.

**Step 2: Replace Static Header with Dynamic Greeting & Highlights**
- Insert `AuraGreetingHeader(...)` at the top item of the feed `LazyColumn`.
- Insert `FeatureHighlightsStrip(...)` directly beneath the greeting.
- Ensure both Android and Desktop receive the dynamic greeting regardless of whether `title` is supplied.

---

### Task 4: Connect Handlers in Android (`MainActivity.kt`) & Desktop (`DesktopApp.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`

**Step 1: Android Integration**
- Forward `accountName = account?.name` to `HomeScreen`.
- Wire `onFeatureClick` to toggle Nothing OS, open Replay sheet, open Equalizer, trigger Sleep Timer, or launch Listen Together.

**Step 2: Desktop Integration**
- Forward `accountName = activeAccount?.name` to `HomeScreen`.
- Wire `onFeatureClick` to corresponding Desktop overlays (`overlays.equalizer`, `overlays.discord`, replay dialog, lyrics panel).

---

### Task 5: Compilation, Validation & Build Verification

**Step 1: Compile Desktop JVM**
- Run `.\gradlew.bat :desktopApp:compileKotlin`

**Step 2: Compile Android Dev Target**
- Run `.\gradlew.bat :app:compileDevDebugKotlin`

**Step 3: Assemble Android Debug APK**
- Run `.\gradlew.bat assembleDevDebug`

---

## 🎯 Verification Checklist
- [ ] Opening app splash screen showcases the uploaded "AuraMusic BY LIKHITH" neon artwork with smooth entrance scaling and exit dissolve.
- [ ] Tap-to-skip works immediately on the splash screen without any lag.
- [ ] Dynamic greeting adapts to current local hour (Morning, Afternoon, Evening, Night).
- [ ] Subtitle provides rich, warm contextual mood phrases.
- [ ] Personalized first name appears when signed in ("Good morning, Likhith!").
- [ ] Feature Spotlight carousel allows users to easily discover and launch AuraMusic features.
- [ ] Full parity between Android and Desktop platforms.
- [ ] Zero compilation errors on `:desktopApp:compileKotlin` and `:app:compileDevDebugKotlin`.
