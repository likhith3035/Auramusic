# Nothing OS UI & Aesthetic Mode Implementation Plan

> **Goal:** Introduce a dedicated **Nothing OS UI Mode** in AuraMusic across Android and Desktop, bringing the iconic Nothing Phone design language: True AMOLED pitch-black backgrounds, Nothing Red accents (`#D71921`), dot-matrix typography (NDot style), glyph-inspired rounded squircle widgets, and minimalist dot-grid player controls.

---

## 🎨 Visual Identity & Design Principles

| Element | Nothing OS Design Language | AuraMusic Implementation |
| :--- | :--- | :--- |
| **Background** | True Pitch Black (`#000000`) | OLED zero-power black on all screens & sheets |
| **Card / Surfaces** | Squircle (`24.dp`–`32.dp`) with `1.dp` border | `#101010` surface with `1.dp` border in `#262626` |
| **Typography** | Dot-Matrix (NDot 55/57) + Clean Sans | Dotted header titles, track timers, status badges |
| **Accent Color** | Iconic Nothing Red (`#D71921` / `#E11D48`) | Active playback states, live indicators, play pill |
| **Secondary Accents** | Monochromatic White & Muted Slate Grays | `#FFFFFF`, `#A3A3A3`, `#525252` |
| **Widgets & Controls** | Pill capsules, dot sliders, glyph rings | Segmented pill buttons, dotted seekbar, glyph equalizer |

---

## 🏗️ Architecture & Component Roadmap

### Task 1: Theme & State Persistence (`ThemeMode.kt` & `AppSettings.kt`)
* Add a `ThemeMode` enum:
  ```kotlin
  enum class AuraThemeMode {
      CLASSIC_AURA, // Existing vibrant glassmorphism / Apple-inspired UI
      NOTHING_OS    // Iconic Nothing OS AMOLED dot-matrix UI
  }
  ```
* Store user preference in `AppSettings` (Android DataStore) and desktop config so it persists across launches.
* Add an instant toggle in **Settings -> Appearance**:
  * "Nothing OS UI Mode" with live preview and iconic red switch.

---

### Task 2: Nothing OS Design Primitives (`NothingDesignTokens.kt`)
Create shared Compose primitives in `sharedUi`:
* **Colors:**
  * `NothingBlack = Color(0xFF000000)`
  * `NothingSurface = Color(0xFF0F0F12)`
  * `NothingBorder = Color(0xFF26262B)`
  * `NothingRed = Color(0xFFD71921)`
  * `NothingWhite = Color(0xFFFFFFFF)`
  * `NothingGray = Color(0xFF8E8E93)`
* **Nothing Squircle Cards (`NothingCard`):**
  * Rounded container with customizable border stroke and dotted corner options.
* **Nothing Pill Buttons (`NothingPill`):**
  * Capsule shape buttons (`RoundedCornerShape(50)`) with outline styling and optional red active dot.
* **Nothing Dot-Matrix Components:**
  * `NothingDotText`: Stylized dot-matrix font / canvas rendering for song duration (`03:42`), titles, and header badges.
  * `NothingDottedSeekBar`: Minimalist seekbar featuring circular LED head and dashed/dotted progress track.

---

### Task 3: Nothing OS "Now Playing" Screen & Player Experience
Transform the music player screen when Nothing OS mode is active:
1. **Monochrome Artwork Framing:**
   * Square/squircle album art container with `24.dp` corners and a subtle `1.dp` Nothing border.
   * Optional toggle: *Monochrome / Desaturated Art* for ultra-minimalist purists.
2. **Dot-Matrix Track Info:**
   * Track title and artist rendered with crisp high-contrast layout.
   * Exact duration & elapsed time rendered in dot-matrix font style (`02:14 / 04:05`).
3. **Glyph Controls:**
   * Play/Pause inside a bold circular glyph pill with Nothing Red accent.
   * Shuffle and Repeat as segmented capsule toggles with red active indicator dots.
   * Dotted volume slider with live decibel / percentage badge.
4. **Nothing Tape Reel / LED Equalizer Animation:**
   * A retro-modern animated dot-matrix soundwave visualizer that bounces smoothly while audio plays.

---

### Task 4: Nothing OS Home Screen & Library Widgets
1. **Header & Top Bar:**
   * App title in dot matrix typography: `A U R A [ • ]` with a glowing red status dot.
   * Pill-shaped quick filter tabs: `[ ALL ]`, `[ SONGS ]`, `[ ALBUMS ]`, `[ ARTISTS ]`.
2. **Track Rows:**
   * Clean, ultra-minimalist list rows with pitch-black separation, mono index numbers (`01`, `02`, `03`), and compact action menus.
3. **Bottom Mini-Player:**
   * Sleek floating Nothing pill bar hovering above the bottom navigation bar.

---

### Task 5: Testing & Multiplatform Verification
1. **Android & Desktop Sync:**
   * Ensure the Nothing OS theme renders identically on Android mobile and Windows Desktop.
2. **Smooth Switching:**
   * Toggling between Classic Aura and Nothing OS mode applies instantly without app restart or flickering.
3. **Build & APK Verification:**
   * Recompile debug APK to verify zero performance regressions and flawless 60/120 FPS animations.

---

## 🎯 Verification Checklist
- [x] Toggle in Settings switches seamlessly between Classic and Nothing OS.
- [x] Pitch-black (`#000000`) AMOLED background applied everywhere.
- [x] Nothing Red (`#D71921`) highlights active controls.
- [x] Player controls follow Nothing capsule & dot-matrix aesthetic.
- [x] Both Android and Desktop builds compile cleanly with zero errors.
