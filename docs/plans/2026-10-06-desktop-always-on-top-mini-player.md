# Desktop Always-On-Top Mini Player Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a sleek, floating, always-on-top "Mini Player" mode for the desktop application that allows users to keep their music controls visible while working in other apps, with smooth switching between Full and Mini player modes.

**Architecture:**
- **State Management:** Extend `DesktopWindowMode` with a reactive `_miniPlayer: MutableStateFlow<Boolean>` flag and helper methods (`enterMiniPlayer()`, `exitMiniPlayer()`, `toggleMiniPlayer()`).
- **Window Controller:** In `Main.kt`, observe `DesktopWindowMode.miniPlayer`. When entering Mini Player mode, save the full window size/position, update `composeWindow.isAlwaysOnTop = true`, adjust `composeWindow.minimumSize` down to (340dp, 110dp), and resize the window to compact dimensions (360dp × 115dp). When exiting, restore previous window dimensions and clear `isAlwaysOnTop`.
- **Mini Player UI Component:** Build `DesktopMiniPlayer` in `desktopApp`:
  - Compact horizontal card with blurred glass / obsidian background and subtle border.
  - Full-window draggable area so the user can grab anywhere on the widget and drag it across their monitors.
  - Thumbnail artwork with rounded corners.
  - Song title and artist with marquee / ellipsis.
  - Playback transport controls: Previous, Play/Pause, Next.
  - Like / Heart toggle.
  - Mini progress bar / seek line.
  - "Expand" button (`Icons.Rounded.OpenInFull` or `Icons.Rounded.PictureInPictureAlt`) to return to full window mode.
  - Close button to hide or close to tray.
- **Entry Points:**
  - Mini Player toggle button in the main desktop player bar / caption area.
  - Context menu / System tray menu item ("Mini Player" / "Full Player").
  - Keyboard shortcut (`Ctrl + M` or `Alt + M`).

**Tech Stack:** Compose Multiplatform Desktop, Kotlin Coroutines & StateFlow, Java AWT Windowing (`isAlwaysOnTop`, `minimumSize`, `size`).

---

### Task 1: Extend `DesktopWindowMode` with Mini Player State

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopWindowMode.kt`

**Details:**
1. Add `private val _miniPlayer = MutableStateFlow(false)`.
2. Expose `val miniPlayer: StateFlow<Boolean> = _miniPlayer`.
3. Add `fun toggleMiniPlayer()`:
   - If already in full screen, exit full screen first.
   - If maximized, restore floating placement.
   - Toggle `_miniPlayer.value = !_miniPlayer.value`.
4. Add `fun exitMiniPlayer()`: sets `_miniPlayer.value = false`.
5. Ensure `adopt(placement: WindowPlacement)` exits mini player if window is maximized by system snap.

---

### Task 2: Handle Window Size, Minimums, and Always-On-Top in `Main.kt`

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/Main.kt`

**Details:**
1. Observe `val isMini by DesktopWindowMode.miniPlayer.collectAsState()`.
2. Keep state holders for `savedWindowSize: DpSize?` and `savedWindowPosition: WindowPosition?`.
3. In a `LaunchedEffect(isMini)`:
   - When `isMini == true`:
     - Save current `state.size` and `state.position`.
     - Update `composeWindow.isAlwaysOnTop = true`.
     - Lower AWT `composeWindow.minimumSize = Dimension((MINI_WIDTH_DP * scaleX).roundToInt(), (MINI_HEIGHT_DP * scaleY).roundToInt())`.
     - Set `state.size = DpSize(360.dp, 115.dp)`.
   - When `isMini == false`:
     - Set `composeWindow.isAlwaysOnTop = false`.
     - Restore AWT `composeWindow.minimumSize = Dimension((MIN_WINDOW_WIDTH_DP * scaleX).roundToInt(), (MIN_WINDOW_HEIGHT_DP * scaleY).roundToInt())`.
     - Restore `state.size = savedWindowSize ?: DpSize(1220.dp, 780.dp)`.
     - Restore `state.position = savedWindowPosition ?: WindowPosition.PlatformDefault`.

---

### Task 3: Build the `DesktopMiniPlayer` Composable

**Files:**
- Create: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopMiniPlayer.kt`

**Details:**
1. Implement `@Composable internal fun DesktopMiniPlayer(...)`:
   - Enclose within `WindowDraggableArea` so clicking & dragging anywhere on the card moves the floating window.
   - Root container:
     - Rounded corner background (`RoundedCornerShape(14.dp)`) with obsidian glass finish (`desktopWindowGlass()` or `MaterialTheme.colorScheme.surfaceContainer`).
     - Subtle border (`1.dp` border with `Color.White.copy(alpha = 0.1f)`).
   - Layout:
     - Top row / body:
       - 64×64dp album art with rounded corners (8.dp) and playing bars indicator.
       - Column with title and artist (bold, single line, ellipsis).
       - Media controls row: Previous, Play/Pause toggle, Next.
       - Heart / Like button.
       - Expand button (`Icons.Rounded.OpenInFull`) to switch back to full player.
       - Close button (`Icons.Rounded.Close`) to minimize to tray.
     - Bottom line:
       - Thin interactive progress / seek bar showing track progress.

---

### Task 4: Connect Mini Player in `DesktopApp.kt`, Player Bar, and Tray Menu

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopTrayMenu.kt`

**Details:**
1. In `BitChordDesktopApp()` in `DesktopApp.kt`:
   - Collect `val isMiniPlayer by DesktopWindowMode.miniPlayer.collectAsState()`.
   - If `isMiniPlayer == true`, render `DesktopMiniPlayer(...)` instead of the full multi-pane layout.
   - Add a "Mini Player" button to the bottom player bar controls (beside the full screen button) with icon `Icons.Rounded.PictureInPictureAlt` and tooltip "Mini player".
2. In `DesktopTrayMenu.kt`:
   - Add a menu action: "Mini player" / "Full player" to allow toggling directly from the Windows taskbar tray icon.

---

### Task 5: Compilation and Verification

**Commands:**
1. Run Desktop Kotlin compilation:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :desktopApp:compileKotlin
   ```
   *Expected: BUILD SUCCESSFUL*

2. Run Android DevDebug build to ensure no cross-platform regression:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat compileDevDebugSources
   ```
   *Expected: BUILD SUCCESSFUL*
