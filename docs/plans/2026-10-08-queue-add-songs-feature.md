# In-Player Queue "Add Songs" Feature Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Allow users, while listening to any song, to tap the "three lines" queue icon in the player, tap an "Add Songs" button inside the Queue view, search/browse songs in an interactive picker sheet, and instantly add chosen tracks to the Up Next queue.

**Architecture:** 
1. Build a high-performance `@Composable QueueAddSongsSheet` in `sharedUi` using `PlayerDrawer`, featuring an instant search input, suggestion chips, live song search results from `YtMusicRepository`, and 1-tap "+ Add" action buttons with haptic feedback and "Added" visual confirmations.
2. Update `PlayerQueue.kt` (`InlineQueue`) with a modern "+ Add Songs" action button alongside the "Clear" button in the Queue header.
3. Wire the sheet state and `onAddToQueue: (Song) -> Unit` through `NowPlayingScreen.kt` (supporting both Portrait and Landscape player modes).
4. Connect to platform playback coordinators in Android `MainActivity.kt` and `DesktopApp.kt`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Jetpack Compose, InnerTube / `YtMusicRepository`, `QueueCoordinator`, Haze frosted glass materials.

---

### Key User Experience Highlights

1. **Seamless In-Player Workflow:**
   - The user never leaves the player or interrupts audio playback. The song keeps playing smoothly while they search and pick tracks.
2. **Instant Search & 1-Tap Add:**
   - Live query debouncing, song art thumbnail, title, artist, and duration display.
   - Tapping "+ Add" immediately enqueues the song into `QueueTier.USER_QUEUE` right after the current song or existing user queue.
3. **Multi-Add with Live Feedback:**
   - Tapping "+ Add" changes to an animated checkmark with an "Added" pill, allowing users to queue multiple songs in a single session.
4. **Instant Queue Reflection:**
   - Dismissing the sheet immediately reveals the newly added songs in the "Next in Queue" section.

---

### Task 1: Create `QueueAddSongsSheet.kt` in `sharedUi`

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/QueueAddSongsSheet.kt`

**Details:**
1. Use `PlayerDrawer` with `hazeState` to maintain the frosted glass aesthetic.
2. Include a sleek search text bar with clear button (`Icons.Rounded.Search`, `Icons.Rounded.Close`).
3. Connect search query to `YtMusicRepository.search(query, SearchFilter.SONGS)` with coroutine debounce (300ms).
4. Display a `LazyColumn` of matching tracks with thumbnail, title, artist, and duration.
5. Provide a "+ Add" button for each row. When clicked:
   - Invokes `onAddToQueue(song)`.
   - Triggers `Haptic.Tap`.
   - Adds song ID to a `remember { mutableStateSetOf<String>() }` to display a glowing "Added ✓" badge.
6. Include quick suggestion chips / trending genres when the search box is empty.

**Verification:**
- Compile via `.\gradlew.bat :sharedUi:compileKotlinJvm`.

---

### Task 2: Add "+ Add Songs" Button to `PlayerQueue.kt` (`InlineQueue`)

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerQueue.kt`

**Details:**
1. Update `InlineQueue` parameters to accept:
   ```kotlin
   onOpenAddSongs: (() -> Unit)? = null,
   ```
2. In the Queue header row (`PlayerQueue.kt:262-275`), add a compact, frosted "+ Add Songs" pill button:
   ```kotlin
   Row(
       modifier = Modifier.fillMaxWidth(),
       verticalAlignment = Alignment.CenterVertically,
   ) {
       Text(
           text = stringResource(Res.string.queue),
           style = MaterialTheme.typography.titleLarge,
           color = Color.White,
           modifier = Modifier.weight(1f),
       )
       Row(
           horizontalArrangement = Arrangement.spacedBy(8.dp),
           verticalAlignment = Alignment.CenterVertically,
       ) {
           if (!controlsLocked && onOpenAddSongs != null) {
               QueueAddSongsButton(onClick = onOpenAddSongs)
           }
           if (tracks.user.isNotEmpty()) {
               QueueClearButton(MaterialTheme.typography.titleMedium, controlsLocked, onClear)
           }
       }
   }
   ```
3. If the user's upcoming queue is empty, also render an atmospheric empty-state card encouraging the user to tap "+ Add Songs to Queue".

**Verification:**
- Compile via `.\gradlew.bat :sharedUi:compileKotlinJvm`.

---

### Task 3: Wire State and Drawer into `NowPlayingScreen.kt`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt`

**Details:**
1. Add `onAddToQueue: ((Song) -> Unit)? = null` parameter to `NowPlayingScreen`.
2. Add state `var addSongsOpen by remember { mutableStateOf(false) }`.
3. Pass `onOpenAddSongs = { addSongsOpen = true }` to `InlineQueue` in both portrait and landscape branches.
4. When `addSongsOpen` is true, render `QueueAddSongsSheet(hazeState, onAddToQueue = { onAddToQueue?.invoke(it) }, onDismiss = { addSongsOpen = false })` in the player overlay layer.

**Verification:**
- Compile via `.\gradlew.bat :sharedUi:compileKotlinJvm`.

---

### Task 4: Connect Call Sites in Android and Desktop Apps

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`

**Details:**
1. In `MainActivity.kt`: Pass `onAddToQueue = addToQueue` to `NowPlayingScreen`.
2. In `DesktopApp.kt`: Pass `onAddToQueue = ::addToQueue` to `NowPlayingScreen`.

**Verification:**
- Compile both targets:
  - `.\gradlew.bat :desktopApp:compileKotlin`
  - `.\gradlew.bat :app:compileDevDebugKotlin`
  - `.\gradlew.bat assembleDevDebug`
