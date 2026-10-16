# Time-of-Day Greetings & Best Feature Spotlight Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Greet users with dynamic, time-aware personalized greetings ("Good morning", "Good afternoon", "Good evening", "Good night" with contextual mood subtitles and account names) and introduce an interactive "Feature Spotlight" carousel highlighting AuraMusic's flagship capabilities (Nothing OS Mode, Replay 2026, Synced Karaoke Lyrics, Listen Together, 10-Band Studio EQ, Sleep Timer, and Story Share).

**Architecture:** Shared Compose Multiplatform components in `sharedUi` (`AuraGreetingAndHighlights.kt`) to provide identical greeting logic, time calculation, feature domain models, and aesthetic presentation across Android (`:app`) and Windows Desktop (`:desktopApp`). Integrated at the top of the Home feed (`HomeScreen.kt`) with platform-specific action routing in `MainActivity.kt` and `DesktopApp.kt`.

**Tech Stack:** Kotlin Compose Multiplatform, Material 3, Java Time (`Calendar` / `LocalTime`), Nothing OS Design System tokens, StateFlow.

---

### Task 1: Create Shared Multiplatform Greeting & Feature Spotlight Components

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/home/AuraGreetingAndHighlights.kt`

**Step 1: Define Time-of-Day Domain Model**
- Implement `enum class TimeOfDay(val greeting: String, val subtitle: String, val emoji: String)`:
  - `MORNING`: 05:00–11:59 ("Good morning", "Start your morning with energizing rhythms", "🌅")
  - `AFTERNOON`: 12:00–16:59 ("Good afternoon", "Power through your day with focus beats", "☀️")
  - `EVENING`: 17:00–21:59 ("Good evening", "Unwind and recharge with your chill mix", "🌆")
  - `NIGHT`: 22:00–04:59 ("Good night", "Late night reflections & soft ambient tunes", "🌙")
- Provide `TimeOfDay.current(): TimeOfDay` checking `Calendar.getInstance().get(Calendar.HOUR_OF_DAY)`.

**Step 2: Define Feature Spotlight Domain Model**
- Create `enum class AuraFeature`:
  - `NOTHING_OS` ("Nothing OS Mode", "AMOLED pitch-black, dot-matrix timer & red glyphs", "Try Now", badge: "AESTHETIC")
  - `REPLAY_2026` ("2026 Replay Recap", "Your top artists, tracks, and 3D hologram card", "View Recap", badge: "SPECIAL")
  - `KARAOKE_LYRICS` ("Live Synced Lyrics", "Sing along with real-time word-by-word karaoke", "View Lyrics", badge: "LIVE")
  - `LISTEN_TOGETHER` ("Listen Together", "Host a synced music session with friends anywhere", "Join Party", badge: "SOCIAL")
  - `EQUALIZER_FX` ("10-Band Studio EQ", "Bass boost, virtualizer, and custom sound curves", "Tune Audio", badge: "STUDIO")
  - `SLEEP_TIMER` ("Smart Sleep Timer", "Gently fades music out as you fall asleep", "Set Timer", badge: "NIGHT")
  - `STORY_SHARE` ("Social Story Cards", "Turn songs and favorite lyrics into story art", "Create Card", badge: "SHARE")
  - `DISCORD_PRESENCE` ("Discord Rich Presence", "Broadcast live track status on your Discord profile", "Connect", badge: "STREAM")

**Step 3: Implement `@Composable fun AuraGreetingHeader`**
- Parameters: `accountName: String?`, `timeOfDay: TimeOfDay`, `nothingOsMode: Boolean`, `modifier: Modifier`.
- Format greeting string: If `accountName` is non-blank, extract first name (`"Good morning, $firstName"`), else fallback to `"Good morning"`.
- Display mood subtitle: e.g. "Start your morning with energizing rhythms".
- Display glowing ambient time badge with time-of-day emoji and current hour indicator.
- Support Nothing OS mode:
  - Upper case spaced dot-matrix styling: `G O O D  M O R N I N G`.
  - Glowing Nothing Red LED indicator dot.
  - High-contrast monochromatic typography.

**Step 4: Implement `@Composable fun FeatureHighlightsStrip`**
- Parameters: `onFeatureClick: (AuraFeature) -> Unit`, `nothingOsMode: Boolean`, `modifier: Modifier`.
- Horizontal scrollable shelf with smooth card padding (`Arrangement.spacedBy(12.dp)`).
- Each card displays:
  - Category pill badge (e.g. `AESTHETIC`, `SPECIAL`, `LIVE`).
  - Feature title and concise subtitle.
  - Action pill button with clear call-to-action (e.g. `Try Now`, `View Recap`).
  - Tactile ripple/press interaction and haptic feedback.
  - In Nothing OS mode: Rendered as `NothingCard` with `1.dp` `NothingThemeTokens.Border` and `NothingPill`.

---

### Task 2: Integrate Greeting & Feature Highlights into `HomeScreen.kt`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt`

**Step 1: Add new parameters to `HomeScreen`**
- `accountName: String? = null`
- `onFeatureClick: ((AuraFeature) -> Unit)? = null`

**Step 2: Render Greeting and Feature Spotlight at Feed Header**
- In `HomeScreen`'s `LazyColumn`:
  - Replace static `HomeTitle(title)` with `AuraGreetingHeader(accountName = accountName, ...)`.
  - Add an item containing `FeatureHighlightsStrip(onFeatureClick = { ... })` directly beneath the greeting.
  - Ensure desktop users also receive the greeting header and feature highlights even when `title == null`.

---

### Task 3: Connect Actions & Handlers in Android (`MainActivity.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Step 1: Pass `accountName = account?.name` to `HomeScreen`**
- Forward user's Google/YouTube account display name so greetings are personalized.

**Step 2: Implement `onFeatureClick` in `MainActivity`**
- Route feature taps directly to corresponding actions:
  - `AuraFeature.NOTHING_OS` -> Toggle `AppSettings.setNothingOsMode(!AppSettings.nothingOsMode.value)` with toast/haptic confirmation.
  - `AuraFeature.REPLAY_2026` -> Open Replay 2026 sheet (`showReplay = true`).
  - `AuraFeature.KARAOKE_LYRICS` -> Expand player and switch to lyrics view.
  - `AuraFeature.LISTEN_TOGETHER` -> Open Listen Together connection modal.
  - `AuraFeature.EQUALIZER_FX` -> Launch system/custom Equalizer.
  - `AuraFeature.SLEEP_TIMER` -> Open Sleep Timer dialog.
  - `AuraFeature.STORY_SHARE` -> Trigger Song Share card sheet.
  - `AuraFeature.DISCORD_PRESENCE` -> Open Discord connection settings.

---

### Task 4: Connect Actions & Handlers in Desktop (`DesktopApp.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`

**Step 1: Pass `accountName = activeAccount?.name` to `HomeScreen`**
- Forward user's active account name on desktop.

**Step 2: Implement `onFeatureClick` in `DesktopApp`**
- Route desktop actions:
  - `AuraFeature.NOTHING_OS` -> Toggle `DesktopAppUiHost.setNothingOsMode(...)`.
  - `AuraFeature.REPLAY_2026` -> Show Replay credit card dialog.
  - `AuraFeature.KARAOKE_LYRICS` -> Toggle lyrics side panel (`lyricsOpen = true`).
  - `AuraFeature.LISTEN_TOGETHER` -> Show Listen Together party dialog.
  - `AuraFeature.EQUALIZER_FX` -> Show Desktop Equalizer overlay (`overlays.equalizer = true`).
  - `AuraFeature.SLEEP_TIMER` -> Show Desktop Sleep Timer dialog.
  - `AuraFeature.DISCORD_PRESENCE` -> Open Discord settings overlay (`overlays.discord = true`).

---

### Task 5: Multiplatform Build Verification & Packaging

**Step 1: Compile Desktop JVM**
- Run `.\gradlew.bat :desktopApp:compileKotlin`
- Verify 0 compilation errors.

**Step 2: Compile Android Dev Target**
- Run `.\gradlew.bat :app:compileDevDebugKotlin`
- Verify 0 compilation errors.

**Step 3: Assemble Android Debug APK**
- Run `.\gradlew.bat assembleDevDebug`
- Verify build success and APK artifact generation.

---

## 🎯 Verification Checklist
- [ ] Time-of-day greeting automatically adapts to local time (Morning, Afternoon, Evening, Night).
- [ ] Personalized first name displayed when account is signed in ("Good morning, Ada!").
- [ ] Subtitle displays contextual mood phrase ("Start your morning with energizing rhythms", etc.).
- [ ] Feature Spotlight carousel showcases AuraMusic's flagship features with direct action triggers.
- [ ] Full aesthetic consistency in both Classic Aura theme and Nothing OS mode.
- [ ] Works identically across Android and Windows Desktop.
- [ ] Clean compilation on both `:desktopApp:compileKotlin` and `:app:compileDevDebugKotlin`.
