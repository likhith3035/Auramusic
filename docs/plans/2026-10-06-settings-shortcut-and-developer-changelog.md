# Settings Top-Bar Shortcut & Developer Changelog ("kami likhith") Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a dedicated Settings gear icon beside the profile icon in the top navigation bar on both Android and Desktop, attribute Lead Developer to **kami likhith**, and showcase an interactive "What's New & Enhancements" timeline with precise dates and timestamps for all features added from yesterday to today.

**Architecture:** Shared Compose Multiplatform components in `sharedUi` (`AuraChangelog.kt`) to ensure Android (`:app`) and Desktop (`:desktopApp`) share the exact same clean changelog models and UI cards. Direct top-bar action bar integration in `MainActivity.kt` and `DesktopApp.kt` places the Settings gear icon immediately adjacent to the account avatar for friction-free access.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Multiplatform, Material3, Coil3.

---

### Task 1: Create Shared Multiplatform Developer & Changelog Domain & UI (`AuraChangelog.kt`)

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/about/AuraChangelog.kt`

**Step 1: Define the Domain Model**
- Create `data class ChangelogEntry(val id: String, val date: String, val time: String, val timestampIso: String, val title: String, val tag: String, val description: String, val highlights: List<String>)`.
- Populate chronological entries from yesterday (Oct 5, 2026) through today (Oct 6, 2026):
  1. **Oct 06, 2026 · 20:25 IST**: Direct Settings Top-Bar Icon & Developer Hub (Settings gear icon beside profile avatar, Developer credit to **kami likhith**, interactive What's New changelog).
  2. **Oct 06, 2026 · 19:55 IST**: Performance & Zero-Crash Architecture (Eliminated nested scroll conflict, parent artwork palette reuse, in-memory ConcurrentHashMap cache, 3-second safe network timeouts).
  3. **Oct 06, 2026 · 19:10 IST**: Song Details & Credits Studio (Release date/year, contributing singers & artists with interactive artist chips, Hi-Res/Lossless badges, behind-the-track synopsis).
  4. **Oct 06, 2026 · 14:15 IST**: AuraMusic by likhith Rebranding & Signature Splash Animation (Minimal obsidian soundwave logo bloom, butter-smooth exit handoff, tap-to-skip, Universal Android APKs & Windows portable packages).
  5. **Oct 05, 2026 · 18:30 IST**: New Release Radar & Social Sync (Background WorkManager notification worker for new songs from followed artists, Spotify canvas backdrops, synced lyrics offset controls).

**Step 2: Build `@Composable fun DeveloperProfileCard`**
- Obsidian/elevated glass card highlighting **kami likhith** as Lead Developer & Maintainer.
- Badges: `Lead Developer`, `AuraMusic Creator`.
- Direct action buttons for GitHub (`https://github.com/likhith3035`) and Portfolio (`https://devlikhith.vercel.app/`).
- "What's New & Enhancements" toggle button / entry point with a badge count ("5 Recent Updates").

**Step 3: Build `@Composable fun AuraChangelogTimeline`**
- Timeline view with glowing node points, dates, timestamps, category pills (`Performance`, `Feature`, `UI/UX`, `Rebrand`), and bulleted breakdown for each update.

---

### Task 2: Place Settings Icon Beside Profile Icon on Android (`MainActivity.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt:3710-3730`

**Step 1: Inspect TopBar action row in `MainActivity.kt`**
- Currently, `TopBarDownloadButton` and `TopBarAccountButton` are placed sequentially.
- When signed in, tapping `TopBarAccountButton` opens `showAccountSelector` which buries settings inside a multi-step dialog.

**Step 2: Add Dedicated Settings IconButton**
- Insert `IconButton(onClick = { showSettings = true })` right beside `TopBarAccountButton`:
  ```kotlin
  TopBarDownloadButton(onClick = { showDownloadManager = true })
  IconButton(
      onClick = { showSettings = true },
  ) {
      Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = stringResource(R.string.settings),
          tint = MaterialTheme.colorScheme.onSurface,
      )
  }
  TopBarAccountButton(
      account = account,
      onClick = {
          if (signedIn) {
              viewModel.loadChannels()
              showAccountSelector = true
          } else showSettings = true
      },
      onSwipeProfile = { forward -> viewModel.stepProfile(forward) },
  )
  ```
- Result: Users now see the Settings gear icon right next to their profile picture at all times across all tabs.

---

### Task 3: Place Settings Icon Beside Profile Icon on Desktop (`DesktopApp.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt:4200-4215`

**Step 1: Inspect TopBar action row in `DesktopApp.kt`**
- In the top action bar: Lyrics, Queue, and `DesktopAccountButton(avatar = accountAvatar, onClick = onOpenAccounts)`.

**Step 2: Add Dedicated Settings DesktopToolbarButton**
- Insert `DesktopToolbarButton(onClick = onOpenSettings, size = 32.dp)` right beside `DesktopAccountButton`:
  ```kotlin
  DesktopToolbarButton(onClick = onOpenSettings, size = 32.dp) {
      Icon(
          Icons.Rounded.Settings,
          DesktopStrings["settings", "Settings"],
          tint = if (settingsOpen) Color.White else DesktopSecondary,
          modifier = Modifier.size(19.dp),
      )
  }
  DesktopAccountButton(avatar = accountAvatar, onClick = onOpenAccounts)
  ```
- Result: Desktop users now have instant access to Settings directly from the top bar next to their profile avatar in addition to the sidebar.

---

### Task 4: Integrate Developer Profile & Live Changelog in Android Settings (`SettingsSheet.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt:1430-1465`

**Step 1: Replace plain text colophon with `DeveloperProfileCard` & `AuraChangelogTimeline`**
- Replace the legacy footer with:
  1. `DeveloperProfileCard(developerName = "kami likhith", onOpenChangelog = { showChangelogSheet = true })`.
  2. Embed or present the full `AuraChangelogTimeline` modal/sheet showing all features and timestamps from yesterday and today.
- Add search indexing keyword `"developer"`, `"kami likhith"`, `"changelog"`, `"what's new"` to ensure search in settings matches instantly.

---

### Task 5: Integrate Developer Profile & Live Changelog in Desktop Settings (`DesktopApp.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt:6010-6040`

**Step 1: Update Desktop Settings footer**
- Add `DeveloperProfileCard` in the desktop settings view.
- Allow desktop users to view the "What's New & Enhancements" changelog timeline directly in the app.

---

### Task 6: Build Verification & Artifact Validation

**Steps:**
1. Compile `:sharedUi:compileKotlinJvm` to ensure shared models and composables compile cleanly.
2. Compile `:desktopApp:compileKotlin` to ensure Desktop compiles without errors.
3. Assemble `:app:assembleDevDebug` to generate updated Android APKs.
4. Assemble `:desktopApp:windowsPortableAssemble` to verify Desktop runnable distribution.
5. Verify both APKs and Desktop packages are ready for testing.
