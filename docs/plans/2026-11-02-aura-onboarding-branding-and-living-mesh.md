# AuraMusic Post-Install Onboarding, Living Mesh & Branding Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the post-install onboarding flow into an authentic AuraMusic first-launch experience featuring the glowing vector waveform brand logo, "AuraMusic" identity typography, and a buttery smooth living ambient mesh gradient with fluid continuous chromatic drift.

**Architecture:**
- Create `AuraLivingMeshBackground.kt`: A zero-allocation, hardware-accelerated Compose canvas that computes 4 dynamic harmonic color orbits with continuous sinusoidal color cycling and gentle spatial Lissajous drift, providing 60/120Hz buttery smooth color transitions.
- Create `AuraWaveformBrandMark.kt`: Reusable vector soundwave brand glyph with multi-stop cyan-to-purple gradient, ambient glow, and micro-harmonic breathing.
- Redesign `AuraOnboardingScreen.kt`: Two-stage setup flow (Step 1: Nickname input for Listen Together; Step 2: Google/YouTube account connection), with crisp typographic headings ("Millions of songs, stream on AuraMusic"), pill buttons, keyboard-aware insets, and smooth slide-down dismiss animation.
- Mount in `MainActivity.kt`: Gate display on `!AppSettings.onboardingCompleted`, with transition onto the main screen once completed or skipped.
- Update `strings.xml`: Define clean, localized strings reflecting the AuraMusic identity with zero emojis.

**Tech Stack:**
- Jetpack Compose (Material 3, Animation Core, GraphicsLayer)
- Android Canvas, RadialGradient, Blur/RenderEffect
- Kotlin Multiplatform Shared Resources / Vector Drawables

---

### Task 1: Add Vector Assets & String Resources

**Files:**
- Create: `app/src/main/res/drawable/ic_google_g.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Create `ic_google_g.xml`**
Provide the standard 4-color Google G vector drawable with viewport `48x48` for the "Sign in with Google" button.

**Step 2: Update `strings.xml` with AuraMusic Identity**
Add the clean onboarding strings with the updated app name:
- `onboarding_title`: "Millions of songs,\nstream on AuraMusic"
- `onboarding_name_prompt`: "What should we call you?"
- `onboarding_name_hint`: "Your name"
- `onboarding_name_footer`: "Friends see this when you Listen Together. You can change it later in Settings."
- `onboarding_next`: "Next"
- `onboarding_connect_title`: "Connect your YouTube account"
- `onboarding_connect_body`: "Bring your library, playlists and mixes with you, and start a Listen Together party with friends."
- `onboarding_sign_in_google`: "Sign in with Google"
- `onboarding_skip_sign_in`: "Skip sign in"
- `onboarding_back`: "Back"
Strictly verify zero emojis in all strings.

---

### Task 2: Create `AuraWaveformBrandMark.kt` (Official Living Waveform Logo)

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/components/AuraWaveformBrandMark.kt`

**Step 1: Implement Harmonic Vector Soundwave Mark**
- Coordinate space: Viewport $730 \times 484$ based on Aura brand geometry from `AuraSplashScreen.kt`.
- 3 layered harmonic Bézier waves:
  - Primary wave (stroke 24, alpha 1.0) with electric cyan (`#00E5FF`) to orchid purple (`#C084FC`) gradient.
  - Secondary wave (stroke 16, alpha 0.75) with sky blue (`#38BDF8`) to purple (`#A855F7`) gradient.
  - Tertiary ambient wave (stroke 10, alpha 0.50) with teal (`#67E8F9`) to magenta (`#E879F9`) gradient.
- Optional subtle idle micro-breathing (subtle $\pm 2$dp vertical phase oscillation) for living depth.
- Reusable as a `@Composable fun AuraWaveformBrandMark(modifier: Modifier)`.

---

### Task 3: Create `AuraLivingMeshBackground.kt` (Buttery Smooth Chromatic Drift Engine)

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/onboarding/AuraLivingMeshBackground.kt`

**Step 1: Implement Continuous Harmonic Color Cycling Engine**
- Define signature Aura chromatic palettes:
  - Palette 1: Astral Cyan & Indigo (`#00E5FF`, `#7B2FF7`, `#38BDF8`, `#1E1B4B`)
  - Palette 2: Velvet Sunset (`#F43F5E`, `#F59E0B`, `#D946EF`, `#0F172A`)
  - Palette 3: Cosmic Aurora (`#10B981`, `#06B6D4`, `#8B5CF6`, `#022C22`)
  - Palette 4: Cyber Amethyst (`#C084FC`, `#38BDF8`, `#E11D48`, `#090D16`)
- Continuous phase $t$: Animate float $0f \dots 1f$ over 16,000ms infinitely using `rememberInfiniteTransition`.
- Continuous color lerp: Compute instantaneous RGB colors between consecutive palette stops using smooth sinusoidal weighting ($3t^2 - 2t^3$). Zero abrupt steps, zero timer jumps.
- 4 dynamic orbital nodes: Each node traces a gentle, non-repeating Lissajous path across the screen.
- Canvas drawing: Renders 4 overlapping radial gradient sweeps over deep obsidian background (`#070A12`) with API 31+ RenderEffect blur / fallback multi-stop radial diffusion.
- Contrast scrim: Vertical gradient overlay ($0.05$ top $\to 0.45$ bottom alpha) guaranteeing WCAG AAA text legibility for white display typography.

---

### Task 4: Create `AuraOnboardingScreen.kt`

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/onboarding/AuraOnboardingScreen.kt`

**Step 1: Layout & Interaction Architecture**
- Screen pages:
  - `PAGE_NAME` (Step 1):
    - Top AuraWaveformBrandMark (48x32dp, hidden when soft keyboard is up).
    - Headline: "Millions of songs,\nstream on AuraMusic" (40sp, display weight, crisp white).
    - Name prompt & field: Frosted capsule input field with 26sp text, `onNext` IME action, max 40 chars.
    - Footer notice: "Friends see this when you Listen Together. You can change it later in Settings."
    - Pill button: Primary white capsule with tactile spring click scale.
  - `PAGE_CONNECT` (Step 2):
    - Circular back button returning to `PAGE_NAME`.
    - Headline: "Connect your YouTube account".
    - Body: "Bring your library, playlists and mixes with you, and start a Listen Together party with friends."
    - "Sign in with Google" pill button with `ic_google_g` icon.
    - "Skip sign in" frosted secondary pill button.
- Transitions:
  - Horizontal slide & crossfade between Step 1 and Step 2 (`slideInHorizontally` + `fadeIn`).
  - Android system back handler integration (`BackHandler`).
  - Tap-outside-to-dismiss keyboard gesture (`pointerInput`).
  - Strict emoji audit: 0 emojis in any string or UI component.

---

### Task 5: Wire `AuraOnboardingScreen` into `MainActivity.kt`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Step 1: Check Onboarding State on Launch**
- Connect `AppSettings.onboardingCompleted.collectAsStateWithLifecycle()`.
- After `AuraSplashScreen` completes, if `!onboardingCompleted`:
  - Display `AuraOnboardingScreen` in full-screen overlay above `BitChordApp`.
  - When user clicks "Next" on name: save name via `ListenTogether.setNickname(chosen)`.
  - If user clicks "Sign in with Google": open `webSession = WebSessionMode.SIGN_IN`. Once signed in, complete onboarding.
  - If user clicks "Skip sign in": call `AppSettings.setOnboardingCompleted(true)`.
- Exit transition: Smooth UIKit-style vertical slide out (`slideOutVertically` over 520ms) revealing the main app interface.

---

### Task 6: Verification, Compilation & Build

**Files:**
- Test commands:
  - Run compile check: `cmd.exe /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :app:compileDevDebugKotlin"`
  - Run package build: `cmd.exe /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :app:assembleDevDebug"`
- Automated emoji check script: Verify 0 emojis across all touched files.
- Verify universal APK generation at `app/build/outputs/apk/dev/debug/AuraMusic-v3.2.0-universal.apk`.

