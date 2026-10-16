# AuraMusic Pro Splash Animation & Dual Platform (APK & Desktop) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a sleek, minimal, professional opening splash animation featuring the animated AuraMusic logo, "AuraMusic", and "by likhith" byline, and integrate it seamlessly across both Android APK and Compose Desktop while maintaining unified branding.

**Architecture:** Build a shared `@Composable AuraSplashScreen` in `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/splash/AuraSplashScreen.kt` so that both Android (`:app`) and Desktop (`:desktopApp`) share the exact same high-performance Compose animation logic. The splash animation features a minimal obsidian background, a gentle scale & fade for the soundwave logo with a subtle luminous aura glow, elegant typography fade-in for "AuraMusic" and "by likhith", and a smooth fade-out exit into the pre-loaded main interface.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Jetpack Compose Animation (`Animatable`, `animateFloatAsState`, `FastOutSlowInEasing`, `LinearOutSlowInEasing`), Android SDK, Desktop JVM (Compose Desktop), Gradle.

---

### Task 1: Create Shared Minimal Pro Splash Screen Animation (`AuraSplashScreen.kt`)

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/splash/AuraSplashScreen.kt`

**Details:**
1. Create `@Composable fun AuraSplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier)` in `com.music.bitchord.ui.splash`.
2. Architecture of the animation:
   - **Backdrop:** Minimal, distraction-free deep dark background (`Color(0xFF08090C)`).
   - **Logo Animation (0ms - 600ms):**
     - Initial scale: `0.86f` -> smoothly eases to `1.0f` using `FastOutSlowInEasing`.
     - Alpha: `0.0f` -> `1.0f` in 400ms.
     - Logo rendering: The authentic AuraMusic 3-wave soundwave logo (drawn with vector or canvas paths matching `ic_logo.xml`).
     - Ambient Aura Glow: A soft, subtle radial gradient glow behind the logo (Cyan `#00E5FF` at 20% alpha, Violet `#8B5CF6` at 12% alpha) that pulses gently (`0.95f` to `1.05f`).
   - **Title & Byline Animation (300ms - 900ms):**
     - "AuraMusic" title: Bold, clean, premium letter-spacing (`1.5.sp`), fading from `alpha = 0f` to `1f` with subtle vertical drift (`offsetY` +10dp -> 0dp).
     - "by likhith" subtitle: Refined, understated, tracking `2.sp`, muted elegance (`Color(0xB3FFFFFF)`), fading in between 500ms and 950ms.
   - **Hold & Exit Transition (1200ms - 1550ms):**
     - Holds briefly for a confident, pro feel.
     - Over 350ms, the entire splash overlay smoothly fades out (`alpha` from `1f` to `0f`).
     - Once fade-out finishes, triggers `onFinished()`.
   - **Interaction:**
     - User tap/click anywhere can instantly trigger exit animation or call `onFinished()` so power users are never blocked.

**Verification:**
- Unit or compilation test verifying `AuraSplashScreen` compiles without missing symbols on both JVM and Android targets.

---

### Task 2: Integrate Splash Animation into Android APK (`MainActivity.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Details:**
1. In `MainActivity.setContent`:
   - Declare `var showSplash by rememberSaveable { mutableStateOf(true) }`.
   - In the root layout, place `BitChordApp(...)` inside the base container.
   - Overlay `if (showSplash) AuraSplashScreen(onFinished = { showSplash = false })`.
2. Benefit: The heavy initialization of Room database, network clients, and Compose layout occurs concurrently during the ~1.4s splash animation, eliminating initial frame drop and delivering an ultra-smooth first frame.

**Verification:**
- Assemble Android Dev Debug APK: `.\gradlew.bat :app:assembleDevDebug`
- Verify APK compiles cleanly.

---

### Task 3: Integrate Splash Animation into Desktop App (`Main.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/Main.kt`

**Details:**
1. In `Main.kt` within `Window(...)`:
   - Declare `var showSplash by remember { mutableStateOf(true) }`.
   - Wrap `BitChordDesktopApp()` inside a `Box(Modifier.fillMaxSize())`.
   - Render `if (showSplash) AuraSplashScreen(onFinished = { showSplash = false })` on top.
2. Ensure window title remains `"AuraMusic"`.

**Verification:**
- Compile Desktop JVM target: `.\gradlew.bat :desktopApp:compileKotlinJvm`
- Verify 0 errors.

---

### Task 4: Harmonize "AuraMusic by likhith" Branding in UI and Settings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `desktopApp/src/main/resources/strings/desktop-en.xml`
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt` or About dialog

**Details:**
1. Ensure the About / Header sections cleanly state "AuraMusic" with the byline "by likhith".
2. Maintain the GPL-3 open-source license attribution ("Based on BitChord by kushagrasinghx").
3. Verify all status bars, window headers, and notifications consistently display the "AuraMusic" name.

**Verification:**
- String checks and resource compilation.

---

### Task 5: End-to-End Build Verification for Both Platforms

**Commands:**
1. **Android APK:**
   ```powershell
   .\gradlew.bat :app:assembleDevDebug
   ```
   Confirm output APK exists at `app\build\outputs\apk\dev\debug\app-dev-debug.apk`.

2. **Desktop Application:**
   ```powershell
   .\gradlew.bat :desktopApp:compileKotlinJvm
   ```
   Confirm compilation succeeds with 0 errors.
