# Aura Action Sheet UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign the 3-dots track and browse actions popup into a cohesive, native Aura Glass Action Sheet that strictly matches AuraMusic's dark glassmorphic design language, Nothing OS aesthetics, rich track header, and quick action pills with zero emojis and a buttery-smooth physics-based spring transition animation.

**Architecture:** Replace the cramped 256dp iOS-styled popover (`IosMenuPopover`) with a native `AuraActionSheet`. The sheet renders on mobile screens as an ergonomic bottom glass sheet with continuous squircle curvature, top drag handle, rich artwork header, horizontal quick-action pill bar, branded Aura Superpowers showcase (AI Companion, Neural DJ, Vocal Stem Karaoke using official Material icons), and grouped Material 3 action rows while preserving GPU backdrop blur sampling and delivering 120Hz buttery physics animation curves.

**Tech Stack:** Jetpack Compose, Compose Material 3, ContinuousCornerShape, SquirclePillShape, SnapshotBackdrop / Haze, Kotlin Multiplatform.

---

### Design Constraints
- **Zero Emojis**: Never use Unicode emojis in icons, text, headers, or badges. All visual indicators must use high-fidelity vector icons (`ImageVector`: `Icons.Rounded.*`) and clean typography.
- **Buttery Transition Animation**:
  - Enter: Coordinated scrim fade (`tween(260ms)`) + bottom-up vertical slide with subtle scale (`0.94f -> 1.0f`) using low-bouncy spring (`dampingRatio = 0.82f, stiffness = 380f`) for an organic, responsive feel.
  - Exit: Swift decelerated collapse (`tween(180ms, FastOutLinearInEasing)`) with zero frame drops.
  - Drag-to-Dismiss: Fluid interactive vertical drag with velocity tracking. Flinging down or dragging past 90dp triggers smooth completion.
  - Dynamic Blur Ramp: Backdrop blur progressively intensifies in lockstep with sheet travel.

---

### Task 1: Create `AuraActionSheet` with Buttery Physics & Gesture Handling
**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/components/AuraActionSheet.kt`
- Modify: `app/src/main/java/com/music/bitchord/ui/components/IosMenu.kt` (export shared backdrop helpers)

**Step-by-step implementation:**
1. Define `AuraActionSheet`:
   - Backdrop scrim: `Color.Black.copy(alpha = 0.42f)` animated with `animateFloatAsState(targetValue, tween(260))`.
   - Card container:
     - Shape: `ContinuousCornerShape(topStart = 28.dp, topEnd = 28.dp)` for bottom-sheet mode on mobile; `ContinuousCornerShape(24.dp)` on tablets/desktop (max width `440.dp`).
     - Background: Deep obsidian glass (`Color(0xFF0F111A).copy(alpha = 0.90f)`) with crisp hairline border (`Color.White.copy(alpha = 0.12f)`).
     - Live backdrop blur: Samples `backdrop` (`menuBackdropFrost`) or `hazeState` (`optimizedHazeEffect`).
   - Top drag handle:
     - Rounded pill `36.dp x 4.dp`, color `Color.White.copy(alpha = 0.28f)`, centered at top with 12.dp vertical padding.
   - Animation physics:
     - Sheet offset Animatable: starts at full height, springs to 0 using `spring(dampingRatio = 0.82f, stiffness = 380f)`.
     - Drag gesture: `detectVerticalDragGestures` with velocity estimation.
   - Back button & outside-tap handling: Seamlessly dismisses through reverse transition.

---

### Task 2: Build `AuraTrackHeader` & `AuraQuickActionsBar`
**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/components/AuraMenuComponents.kt`

**Step-by-step implementation:**
1. `AuraTrackHeader`:
   - 54dp album art with `ContinuousCornerShape(12.dp)` and hairline border.
   - Title in `MaterialTheme.typography.titleMedium` with `FontWeight.SemiBold`.
   - Explicit badge (`[E]`) rendered as a micro vector badge when applicable.
   - Artist name and duration in secondary label color.
   - Close icon button (`Icons.Rounded.Close`) on the right.
2. `AuraQuickActionsBar`:
   - 5 squircle pill buttons in a balanced row:
     - **Favorite**: `Icons.Rounded.Favorite` (active) / `Icons.Rounded.FavoriteBorder` (inactive), glowing with palette accent.
     - **Dislike**: `Icons.Rounded.ThumbDown` / `Icons.Rounded.ThumbDownOffAlt`.
     - **Download**: `Icons.Rounded.Download` / `Icons.Rounded.DownloadDone`.
     - **Radio**: `Icons.Rounded.Radio`.
     - **Share**: `Icons.Rounded.Share`.
   - Pill background: `Color.White.copy(alpha = 0.08f)` with `ContinuousCornerShape(16.dp)`, tactile ripple, and text labels below icons.

---

### Task 3: Redesign `SongActionsMenu` with Aura Superpowers & Clean Vectors
**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/components/SongActionsMenu.kt`

**Step-by-step implementation:**
1. Remove all SF Symbols and replace with standard Material 3 rounded vector icons (`Icons.Rounded.*`).
2. Add **Aura Superpowers Showcase**:
   - Distinctive gradient/highlighted cards for:
     - **Aura AI Companion**: `Icons.Rounded.AutoAwesome`, badge `AI COMPANION`, subtitle `Acoustic psychology, roasts and recommendations`.
     - **Aura Neural DJ**: `Icons.Rounded.DiscFull`, badge `HARMONIC MIX`, subtitle `Camelot Wheel transitions and BPM matching`.
     - **Vocal Stem Karaoke**: `Icons.Rounded.Mic`, badge `STEM SEPARATOR`, subtitle `Real-time vocal extraction`.
3. Categorized Action Groups:
   - **Playback Mode**: Convert to Video / Switch to Audio cut with toggle pill (`Icons.Rounded.Videocam` / `Icons.Rounded.GraphicEq`).
   - **Queue**: Play Next (`Icons.Rounded.QueueMusic`), Add to Queue (`Icons.Rounded.PlaylistAdd`).
   - **Playlists & Library**: Add to Playlist (`Icons.Rounded.BookmarkAdd`), Open Album (`Icons.Rounded.Album`), Open Artist (`Icons.Rounded.Person`).
   - **Management**: Sleep Timer (`Icons.Rounded.Bedtime`), Song Credits (`Icons.Rounded.Info`), Remove from Cache (`Icons.Rounded.DeleteOutline`).
4. Row layout: `52.dp` min height, touch-friendly 16dp horizontal padding, clean Material 3 typography with Nothing font support if enabled.

---

### Task 4: Redesign `BrowseActionsMenu` for Albums, Playlists & Artists
**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/components/BrowseActionsMenu.kt`

**Step-by-step implementation:**
1. Header displaying collection artwork (circle for artists, squircle for albums/playlists), title, and metadata ("Album", "Playlist", or "Artist").
2. Quick actions: Play (`Icons.Rounded.PlayArrow`), Shuffle (`Icons.Rounded.Shuffle`), Download All (`Icons.Rounded.Download`), Share (`Icons.Rounded.Share`).
3. Secondary actions: Add all to queue, Add to library, Pin to favorites.
4. Replace all iOS table styling with Aura glass rows.

---

### Task 5: Integrate `AuraActionSheet` in `MainActivity.kt`
**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Step-by-step implementation:**
1. Replace `IosMenuPopover` invocations with `AuraActionSheet`:
   - Inside the player sheet: bound to `playerBackdrop` and `livePlayerBackdrop`.
   - At the root page level: bound to `pageBackdrop` and `hazeState`.
2. Connect `songActions` and `browseActions` state flows.
3. Validate all action callbacks continue working seamlessly.

---

### Task 6: Compilation, Visual Audit & Verification
**Steps:**
1. Compile `:app:compileDevDebugKotlin` to verify zero compiler errors.
2. Build debug APK `:app:assembleDevDebug`.
3. Verify:
   - Zero emojis in any component or string.
   - Buttery smooth enter/exit animation on 60Hz and 120Hz displays.
   - Frosted blur reflects player artwork and page background.
   - Drag down to dismiss feels natural and frictionless.
