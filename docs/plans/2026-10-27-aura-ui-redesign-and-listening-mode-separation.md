# Aura UI Redesign & Listening Mode Separation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Overhaul the Aura AI visual experience by replacing the placeholder emoji icon with a bespoke luxury animated Aura Orb, cleanly separating ambient acoustic listening from lyric typing in Aura Radar, embedding a floating mini player pill inside the AI chatbot, and applying a refined dark obsidian/indigo color palette.

**Architecture:** 
1. Build a custom canvas/vector `AuraOrbIcon` in `sharedUi` to replace the cheap `Text("✨")` placeholder in `MainActivity.kt` and `AuraChatScreen.kt`.
2. Refactor `AuraRadarOverlay` into a dual-mode tabbed interface: Tab 1 (Ambient Acoustic Radar with immersive full-screen pulse and no input clutter) and Tab 2 (Dedicated Lyric & Hum Search with smart filter chips and instant card results).
3. Introduce `AuraChatMiniPlayer` pinned as a floating glassmorphic pill directly above the chat message bar, displaying live album artwork, title, artist, animated equalizer wave, and play/pause controls.
4. Upgrade `AuraChatScreen`, `AuraChatBubble`, and `AuraSongCard` color tokens to an obsidian/deep slate palette with electric indigo (`#5243E8`) and cyber cyan (`#00F5D4`) accents.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose / Compose Multiplatform, Canvas Drawing, Kotlinx Coroutines, Coil3 Image Loading.

---

### Task 1: Create Luxury Animated `AuraOrbIcon`

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraOrbIcon.kt`
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt:3950-3966`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatScreen.kt:250-264`

**Step 1: Write the failing unit test for `AuraOrbIcon` presence and state**
- File: `shared/src/jvmTest/kotlin/com/music/bitchord/data/ai/AuraOrbIconTest.kt`
- Test that the icon dimensions and color constants conform to design tokens.

**Step 2: Run test to verify it fails**
- Command: `cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :shared:jvmTest --tests *AuraOrbIconTest*"`
- Expected: FAIL (class/tokens not found).

**Step 3: Implement `AuraOrbIcon`**
- Create `@Composable fun AuraOrbIcon(modifier: Modifier, isThinking: Boolean, isPlaying: Boolean)` with dual concentric orbital rings, central radiant glowing iris in Indigo (`#6366F1`) & Cyan (`#00F5D4`), and subtle harmonic breathing scale animation.
- Replace `Text("✨")` in `MainActivity.kt` and `AuraChatScreen.kt` with `AuraOrbIcon`.

**Step 4: Verify compilation & test**
- Command: `cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :sharedUi:compileKotlinJvm :app:compileDevDebugKotlin"`
- Expected: BUILD SUCCESSFUL.

---

### Task 2: Separate Aura Radar into Distinct "Ambient Listening" and "Lyric Search" Modes

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraRadarOverlay.kt`
- Modify: `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/ai/AuraRadarService.kt`

**Step 1: Define Radar Tab Mode Enum & State**
- Add `enum class RadarMode { AMBIENT_LISTEN, LYRIC_SEARCH }`.
- Ambient mode: 100% focused on acoustic listening. Sonar wave animation, frequency ring, "Listening to nearby audio..." with tap-to-cancel and retry. No text input box on the screen!
- Lyric Search mode: Clean search header with text input field ("Type 3+ words of lyrics..."), language chips ("Telugu", "Hindi", "English", "All"), instant live matches.

**Step 2: Implement Clean Segmented Tab Bar at the top of `AuraRadarOverlay`**
- Two pill tabs: `[🎙️ Ambient Audio]` and `[✍️ Search Lyrics]`.
- Smooth crossfade transitions between modes.

**Step 3: Test and compile**
- Run: `cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :sharedUi:compileKotlinJvm"`
- Expected: BUILD SUCCESSFUL.

---

### Task 3: Implement In-Chat Floating Mini-Player Pill

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatMiniPlayer.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatScreen.kt:364-410` and bottom bar layout.
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt:5480-5517`

**Step 1: Create `AuraChatMiniPlayer.kt`**
- Floating glass pill docked right above the chat input bar.
- Dimensions: Height ~52dp, rounded 16dp corners, subtle border (`#2A344A`), background `#121726` with alpha 0.95.
- Left: 38dp rounded album art + live miniature animated 3-bar equalizer bars.
- Center: Song title and artist with single-line ellipsis.
- Right: Play/Pause button (`Icons.Rounded.PlayArrow` / `Pause`) and optional close/expand icon.
- Tapping the pill triggers full player preview or quick action sheet.

**Step 2: Remove the clunky top-of-screen "▶ Playing:" text banner from `AuraChatScreen.kt`**
- Move playback controls to the floating mini-player pill above the bottom message input field.
- Wire `onTogglePlayPause: () -> Unit` into `AuraChatScreen` and pass `player.togglePlayPause()` from `MainActivity.kt`.

**Step 3: Test and compile**
- Run: `cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :app:compileDevDebugKotlin :desktopApp:compileKotlin"`
- Expected: BUILD SUCCESSFUL.

---

### Task 4: Complete Palette Overhaul for Chat Bubbles, Cards, and Backgrounds

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatScreen.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatBubble.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraSongCard.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraMixCard.kt`

**Step 1: Replace saturated magenta/purple colors with luxury dark palette tokens**
- Replace `#E94057` and `#8A2387` with:
  - Base: `#090B10`
  - Elevated Cards: `#121624` with border `#1F283C`
  - User Bubble: Gradient `#5243E8` &rarr; `#4335D6`
  - Accent / Primary Action: Cyber Cyan (`#00F5D4`) / Electric Iris (`#6366F1`)
  - Text: Primary `#F8FAFC`, Secondary `#94A3B8`, Monospace `#38BDF8`

**Step 2: Polish `AuraSongCard` & `AuraMixCard`**
- 48dp album art with subtle shadow.
- Clear circular play button in glowing emerald/cyan.
- Modern minimalist action tags.

**Step 3: Verify overall build & run test suite**
- Command: `cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& gradlew.bat :shared:testDebugUnitTest :shared:jvmTest :app:compileDevDebugKotlin :desktopApp:compileKotlin"`
- Expected: 100% tests pass, zero compilation errors.
