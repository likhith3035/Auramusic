# AuraMusic — Smart Sleep Timer & Listening Control Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement a comprehensive, modern Smart Sleep Timer system in AuraMusic featuring time-based countdowns with hours/minutes and presets, track-based song count timers, non-abrupt completion (finish current song before stopping), live countdown and controls (Pause, Resume, Cancel), on-player status display, and optional inactivity sleep detection.

**Architecture:**
1. **Core State & Logic (`SmartSleepTimer.kt` in `shared`):** Build a unified, multiplatform reactive state machine powered by `MonotonicClock` and Kotlin `StateFlow`. Supports time durations, track-based counting, pause/resume/cancel, non-abrupt finish-after-track transitions, and activity timestamp tracking for optional inactivity detection.
2. **Platform Playback Controllers (`PlaybackService.kt` & `DesktopApp.kt`):** Integrate `SmartSleepTimer` into ExoPlayer / Media3 on Android and `DesktopPlaybackEngine` on Desktop. When the countdown hits 0 or the last track is reached, playback completes the active track and then gracefully pauses without cutting audio off mid-song. Also route user interactions into inactivity tracking.
3. **Modern Frosted UI (`SmartSleepTimerSheet.kt` & `NowPlayingScreen.kt`):** Build a frosted glass `PlayerDrawer` sheet in `sharedUi` featuring a 3-tab experience (Duration, Song Count, Inactivity Detection) with digital countdown rings, quick presets (5, 10, 15, 30, 45, 60, 90, 120m), custom hour/minute inputs, track steppers, live pause/resume/cancel buttons, and an on-player glowing status capsule.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Coroutines & StateFlow, Haze frosted glass, ExoPlayer / Media3 (Android), DesktopPlaybackEngine.

---

### Task 1: Create `SmartSleepTimer.kt` Core State Machine in `shared`

**Files:**
- Create: `shared/src/jvmSharedMain/kotlin/com/music/bitchord/playback/SmartSleepTimer.kt`
- Test: `shared/src/jvmTest/kotlin/com/music/bitchord/playback/SmartSleepTimerTest.kt`

**Step 1: Write the failing test**

Create `shared/src/jvmTest/kotlin/com/music/bitchord/playback/SmartSleepTimerTest.kt`:

```kotlin
package com.music.bitchord.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmartSleepTimerTest {

    @Test
    fun testInitialStateIsOff() {
        SmartSleepTimer.cancel()
        assertEquals(SleepTimerMode.Off, SmartSleepTimer.mode.value)
        assertEquals(SleepTimerStatus.OFF, SmartSleepTimer.status.value)
        assertFalse(SmartSleepTimer.isActive)
        assertNull(SmartSleepTimer.remainingMs())
    }

    @Test
    fun testStartDurationTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 30)
        assertTrue(SmartSleepTimer.isActive)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
        val mode = SmartSleepTimer.mode.value as SleepTimerMode.Duration
        assertEquals(30, mode.totalMinutes)
        assertNotNull(SmartSleepTimer.remainingMs())
        assertTrue(SmartSleepTimer.remainingMs()!! > 0)
    }

    @Test
    fun testPauseAndResumeDurationTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startDuration(minutes = 15)
        assertTrue(SmartSleepTimer.pause())
        assertEquals(SleepTimerStatus.PAUSED, SmartSleepTimer.status.value)
        val pausedRemaining = SmartSleepTimer.remainingMs()
        assertNotNull(pausedRemaining)

        assertTrue(SmartSleepTimer.resume())
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
    }

    @Test
    fun testStartSongCountTimer() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startSongCount(totalSongs = 5)
        assertTrue(SmartSleepTimer.isActive)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
        val mode = SmartSleepTimer.mode.value as SleepTimerMode.SongCount
        assertEquals(5, mode.totalSongs)
        assertEquals(0, SmartSleepTimer.completedSongs.value)
        assertEquals(5, SmartSleepTimer.remainingSongs.value)

        // Song finishes
        SmartSleepTimer.onTrackCompleted()
        assertEquals(1, SmartSleepTimer.completedSongs.value)
        assertEquals(4, SmartSleepTimer.remainingSongs.value)
        assertEquals(SleepTimerStatus.RUNNING, SmartSleepTimer.status.value)
    }

    @Test
    fun testSongCountFinalTrackTransitionsToFinishing() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.startSongCount(totalSongs = 1)
        // 1 song total: from start, remaining is 1, so the current track is already the final track
        assertTrue(SmartSleepTimer.shouldStopAfterCurrentTrack.value)

        // Once completed, it should fire stop
        val shouldStop = SmartSleepTimer.onTrackCompleted()
        assertTrue(shouldStop)
        assertEquals(SleepTimerStatus.OFF, SmartSleepTimer.status.value)
    }

    @Test
    fun testInactivityDetectionTrigger() {
        SmartSleepTimer.cancel()
        SmartSleepTimer.setInactivityDetectionEnabled(true)
        SmartSleepTimer.setInactivityThresholdMinutes(15)
        SmartSleepTimer.recordUserActivity()

        assertFalse(SmartSleepTimer.checkInactivityExceeded(currentMs = SmartSleepTimer.lastActivityMs.value + 10 * 60_000L))
        assertTrue(SmartSleepTimer.checkInactivityExceeded(currentMs = SmartSleepTimer.lastActivityMs.value + 16 * 60_000L))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:jvmTest --tests com.music.bitchord.playback.SmartSleepTimerTest`
Expected: FAIL compilation with unresolved reference `SmartSleepTimer`.

**Step 3: Write minimal implementation**

Create `shared/src/jvmSharedMain/kotlin/com/music/bitchord/playback/SmartSleepTimer.kt`:

```kotlin
package com.music.bitchord.playback

import com.music.bitchord.data.MonotonicClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mode of the smart sleep timer. */
sealed interface SleepTimerMode {
    data object Off : SleepTimerMode
    data class Duration(val totalMinutes: Int) : SleepTimerMode
    data class SongCount(val totalSongs: Int) : SleepTimerMode
}

/** Operational status of the sleep timer. */
enum class SleepTimerStatus {
    OFF,
    RUNNING,
    PAUSED,
    FINISHING_CURRENT_SONG,
}

/**
 * Universal Smart Sleep Timer and Listening Controller.
 * Shared across Android and Desktop.
 */
object SmartSleepTimer {

    val TIME_PRESETS = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    val SONG_PRESETS = listOf(1, 3, 5, 10, 15)
    val INACTIVITY_PRESETS = listOf(15, 30, 45, 60)

    private val _mode = MutableStateFlow<SleepTimerMode>(SleepTimerMode.Off)
    val mode: StateFlow<SleepTimerMode> = _mode.asStateFlow()

    private val _status = MutableStateFlow(SleepTimerStatus.OFF)
    val status: StateFlow<SleepTimerStatus> = _status.asStateFlow()

    /** Deadline on [MonotonicClock.nowMs], or null when not in active countdown. */
    private val _deadlineMs = MutableStateFlow<Long?>(null)
    val deadlineMs: StateFlow<Long?> = _deadlineMs.asStateFlow()

    /** Frozen remaining ms when timer is paused. */
    private var pausedDurationRemainingMs: Long? = null

    /** Finish current playing track completely before stopping playback. */
    val finishCurrentSongOnExpiry = MutableStateFlow(true)

    /** Signal indicating the player must stop after the currently playing track ends. */
    val shouldStopAfterCurrentTrack = MutableStateFlow(false)

    // Track-based counting
    val completedSongs = MutableStateFlow(0)
    val remainingSongs = MutableStateFlow(0)

    // Inactivity detection
    val inactivityDetectionEnabled = MutableStateFlow(false)
    val inactivityThresholdMinutes = MutableStateFlow(30)
    val lastActivityMs = MutableStateFlow(MonotonicClock.nowMs())

    val isActive: Boolean
        get() = _status.value != SleepTimerStatus.OFF

    val isPaused: Boolean
        get() = _status.value == SleepTimerStatus.PAUSED

    /** Start a time-based duration countdown. */
    fun startDuration(minutes: Int) {
        require(minutes > 0) { "Minutes must be > 0" }
        cancel()
        _mode.value = SleepTimerMode.Duration(minutes)
        _status.value = SleepTimerStatus.RUNNING
        _deadlineMs.value = MonotonicClock.nowMs() + (minutes * 60_000L)
        pausedDurationRemainingMs = null
        recordUserActivity()
    }

    /** Add extra minutes to an active duration timer. */
    fun addMinutes(minutes: Int) {
        if (_status.value == SleepTimerStatus.OFF) {
            startDuration(minutes)
            return
        }
        val currentRemaining = remainingMs() ?: 0L
        val newRemaining = currentRemaining + (minutes * 60_000L)
        if (_status.value == SleepTimerStatus.PAUSED) {
            pausedDurationRemainingMs = newRemaining
        } else {
            _deadlineMs.value = MonotonicClock.nowMs() + newRemaining
            _status.value = SleepTimerStatus.RUNNING
            shouldStopAfterCurrentTrack.value = false
        }
        recordUserActivity()
    }

    /** Start a song count timer. */
    fun startSongCount(totalSongs: Int) {
        require(totalSongs > 0) { "Total songs must be > 0" }
        cancel()
        _mode.value = SleepTimerMode.SongCount(totalSongs)
        _status.value = SleepTimerStatus.RUNNING
        completedSongs.value = 0
        remainingSongs.value = totalSongs
        shouldStopAfterCurrentTrack.value = (totalSongs == 1)
        recordUserActivity()
    }

    /** Start "Stop after this song" (shortcut for 1 song). */
    fun startAfterCurrentSong() {
        startSongCount(1)
    }

    /** Pause active timer. */
    fun pause(): Boolean {
        if (_status.value != SleepTimerStatus.RUNNING) return false
        when (_mode.value) {
            is SleepTimerMode.Duration -> {
                pausedDurationRemainingMs = remainingMs()
                _deadlineMs.value = null
                _status.value = SleepTimerStatus.PAUSED
            }
            is SleepTimerMode.SongCount -> {
                _status.value = SleepTimerStatus.PAUSED
            }
            SleepTimerMode.Off -> return false
        }
        recordUserActivity()
        return true
    }

    /** Resume paused timer. */
    fun resume(): Boolean {
        if (_status.value != SleepTimerStatus.PAUSED) return false
        when (_mode.value) {
            is SleepTimerMode.Duration -> {
                val remaining = pausedDurationRemainingMs ?: 0L
                _deadlineMs.value = MonotonicClock.nowMs() + remaining
                _status.value = SleepTimerStatus.RUNNING
                pausedDurationRemainingMs = null
            }
            is SleepTimerMode.SongCount -> {
                _status.value = SleepTimerStatus.RUNNING
            }
            SleepTimerMode.Off -> return false
        }
        recordUserActivity()
        return true
    }

    /** Cancel all sleep timer modes. */
    fun cancel() {
        _mode.value = SleepTimerMode.Off
        _status.value = SleepTimerStatus.OFF
        _deadlineMs.value = null
        pausedDurationRemainingMs = null
        shouldStopAfterCurrentTrack.value = false
        completedSongs.value = 0
        remainingSongs.value = 0
        recordUserActivity()
    }

    /** Returns remaining milliseconds for time-based timer, or null. */
    fun remainingMs(): Long? {
        if (_status.value == SleepTimerStatus.PAUSED) return pausedDurationRemainingMs
        val dl = _deadlineMs.value ?: return null
        return (dl - MonotonicClock.nowMs()).coerceAtLeast(0L)
    }

    /**
     * Called when the time countdown reaches zero.
     * @param isCurrentlyPlaying whether music is playing right now.
     * @return true if playback should pause immediately, false if waiting for current song to finish.
     */
    fun onDurationCountdownExpired(isCurrentlyPlaying: Boolean): Boolean {
        if (_status.value != SleepTimerStatus.RUNNING) return false
        if (!isCurrentlyPlaying || !finishCurrentSongOnExpiry.value) {
            cancel()
            return true
        }
        // Gracefully finish current song
        _status.value = SleepTimerStatus.FINISHING_CURRENT_SONG
        shouldStopAfterCurrentTrack.value = true
        _deadlineMs.value = null
        return false
    }

    /**
     * Called by playback service when a track actually ends (natural completion).
     * @return true if playback should stop now.
     */
    fun onTrackCompleted(): Boolean {
        if (_status.value == SleepTimerStatus.FINISHING_CURRENT_SONG || shouldStopAfterCurrentTrack.value) {
            cancel()
            return true
        }
        if (_status.value != SleepTimerStatus.RUNNING) return false

        val m = _mode.value
        if (m is SleepTimerMode.SongCount) {
            val done = completedSongs.value + 1
            completedSongs.value = done
            val left = (m.totalSongs - done).coerceAtLeast(0)
            remainingSongs.value = left
            if (left == 1) {
                // Next track is the final track: arm non-crossfade stop
                shouldStopAfterCurrentTrack.value = true
            } else if (left <= 0) {
                cancel()
                return true
            }
        }
        return false
    }

    /** Reset inactivity counter whenever user touches or controls the player. */
    fun recordUserActivity() {
        lastActivityMs.value = MonotonicClock.nowMs()
    }

    fun setInactivityDetectionEnabled(enabled: Boolean) {
        inactivityDetectionEnabled.value = enabled
        recordUserActivity()
    }

    fun setInactivityThresholdMinutes(minutes: Int) {
        require(minutes > 0)
        inactivityThresholdMinutes.value = minutes
        recordUserActivity()
    }

    fun checkInactivityExceeded(currentMs: Long = MonotonicClock.nowMs()): Boolean {
        if (!inactivityDetectionEnabled.value) return false
        val thresholdMs = inactivityThresholdMinutes.value * 60_000L
        return (currentMs - lastActivityMs.value) >= thresholdMs
    }

    /** Formats ms to MM:SS or HH:MM:SS string. */
    fun formatRemaining(remainingMs: Long): String {
        val totalSecs = (remainingMs / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:jvmTest --tests com.music.bitchord.playback.SmartSleepTimerTest`
Expected: BUILD SUCCESSFUL (All tests pass).

**Step 5: Commit**

```bash
git add shared/src/jvmSharedMain/kotlin/com/music/bitchord/playback/SmartSleepTimer.kt shared/src/jvmTest/kotlin/com/music/bitchord/playback/SmartSleepTimerTest.kt
git commit -m "feat(shared): add SmartSleepTimer core state machine and unit tests"
```

---

### Task 2: Update Android `SleepTimer.kt` & `PlaybackService.kt` Integration

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/playback/SleepTimer.kt`
- Modify: `app/src/main/java/com/music/bitchord/playback/PlaybackService.kt:5415-5430` & `3010-3020`

**Step 1: Bridge `SleepTimer.kt` to `SmartSleepTimer`**

Keep `SleepTimer` API 100% backward-compatible while connecting to `SmartSleepTimer`:

```kotlin
package com.music.bitchord.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * Android compatibility wrapper delegating to [SmartSleepTimer].
 */
object SleepTimer {
    val deadline: StateFlow<Long?> get() = SmartSleepTimer.deadlineMs
    val minutes: StateFlow<Int?> get() = kotlinx.coroutines.flow.MutableStateFlow(
        (SmartSleepTimer.mode.value as? SleepTimerMode.Duration)?.totalMinutes
    )
    val afterTrack: StateFlow<Boolean> get() = SmartSleepTimer.shouldStopAfterCurrentTrack
    val PRESETS get() = SmartSleepTimer.TIME_PRESETS
    val isRunning: Boolean get() = SmartSleepTimer.isActive

    fun start(minutes: Int) = SmartSleepTimer.startDuration(minutes)
    fun startAfterTrack() = SmartSleepTimer.startAfterCurrentSong()
    fun cancel() = SmartSleepTimer.cancel()
    fun remainingMs(): Long? = SmartSleepTimer.remainingMs()
}
```

**Step 2: Update `PlaybackService.kt` to handle non-abrupt song completion and inactivity detection**

In `PlaybackService.kt`:
1. In `watchSleepTimer()`:
   ```kotlin
   private fun watchSleepTimer() {
       scope.launch {
           SmartSleepTimer.deadlineMs.collectLatest { deadline ->
               if (deadline == null) return@collectLatest
               val remaining = deadline - MonotonicClock.nowMs()
               if (remaining > 0) delay(remaining)
               val isPlaying = player?.isPlaying == true
               val shouldPauseNow = SmartSleepTimer.onDurationCountdownExpired(isPlaying)
               if (shouldPauseNow) {
                   player?.pause()
               }
           }
       }
       // Inactivity monitor loop
       scope.launch {
           while (isActive) {
               delay(15_000L)
               if (player?.isPlaying == true && SmartSleepTimer.checkInactivityExceeded()) {
                   // Inactivity threshold reached: smoothly finish current track
                   SmartSleepTimer.shouldStopAfterCurrentTrack.value = true
               }
           }
       }
   }
   ```
2. In track change handling (`onTrackChanged` / `ended` in `PlaybackService.kt:3010-3020`):
   ```kotlin
   if (ended) {
       val shouldStop = SmartSleepTimer.onTrackCompleted()
       if (shouldStop) {
           exoPlayer.pause()
       }
   }
   ```
3. In `onEvents` / player interaction hooks:
   Call `SmartSleepTimer.recordUserActivity()` when user plays, pauses, seeks, skips, etc.

**Step 3: Verification**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL without warnings or errors.

**Step 4: Commit**

```bash
git add app/src/main/java/com/music/bitchord/playback/SleepTimer.kt app/src/main/java/com/music/bitchord/playback/PlaybackService.kt
git commit -m "feat(android): wire SmartSleepTimer into PlaybackService with non-abrupt completion and inactivity detection"
```

---

### Task 3: Update Desktop App Sleep Timer Integration

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopSleepTimer.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt:1445-1460` & `1689-1697`

**Step 1: Bridge `DesktopSleepTimer.kt` to `SmartSleepTimer`**

Update `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopSleepTimer.kt`:
Delegate `start(minutes)`, `startAfterTrack()`, `cancel()`, `remainingMs()`, `afterTrack`, `minutes` directly to `SmartSleepTimer`.

**Step 2: Update Desktop Playback Engine Listener in `DesktopApp.kt`**

1. In `onEnded = { ... }` (`DesktopApp.kt:1448`):
   ```kotlin
   val shouldStop = SmartSleepTimer.onTrackCompleted()
   if (shouldStop) {
       playbackEngine.pause()
   } else if (repeatMode == DesktopRepeatMode.ONE) {
       ...
   ```
2. In the 1-second watch loop (`DesktopApp.kt:1689-1697`):
   ```kotlin
   LaunchedEffect(Unit) {
       while (currentCoroutineContext().isActive) {
           delay(1_000L)
           val remaining = SmartSleepTimer.remainingMs()
           if (remaining != null && remaining <= 0L) {
               val isPlaying = playbackEngine.state.value.isPlaying
               val shouldPauseNow = SmartSleepTimer.onDurationCountdownExpired(isPlaying)
               if (shouldPauseNow) {
                   playbackEngine.pause()
               }
           }
           if (playbackEngine.state.value.isPlaying && SmartSleepTimer.checkInactivityExceeded()) {
               SmartSleepTimer.shouldStopAfterCurrentTrack.value = true
           }
       }
   }
   ```

**Step 3: Verification**

Run: `.\gradlew.bat :desktopApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopSleepTimer.kt desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt
git commit -m "feat(desktop): wire SmartSleepTimer into desktop audio engine"
```

---

### Task 4: Create Modern `SmartSleepTimerSheet.kt` in `sharedUi`

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/SmartSleepTimerSheet.kt`

**Details & UI Structure:**
1. Built with `PlayerDrawer` and `hazeState` for rich frosted glass aesthetics.
2. Segmented Mode Selector Tab Pill:
   - `[ ⏱ Duration ]` | `[ 🎵 Song Count ]` | `[ 🌙 Sleep Detection ]`
3. **Tab 1: Duration (Time-Based):**
   - Active/Paused Visual Countdown Hero:
     - Digital Clock: `00:24:15` with pulse dot indicator.
     - Status Chip: "Active Countdown" (Green) | "Timer Paused" (Amber) | "Finishing current song..." (Purple).
     - Live Controls Row:
       - **Pause / Resume** button with icon.
       - **+5 min** and **+15 min** quick boost pills.
       - **Cancel Timer** pill.
   - Quick Preset Chips: `5m`, `10m`, `15m`, `30m`, `45m`, `60m`, `1h 30m`, `2h`.
   - Custom Duration Input:
     - Hours Stepper: `[-] X hours [+]`
     - Minutes Stepper: `[-] Y mins [+]`
     - Prominent "Start Sleep Timer" action button.
   - "Finish current song before stopping" toggle card with explanation ("Audio will never cut off abruptly mid-verse").
4. **Tab 2: Song Count (Track-Based):**
   - Quick Preset Chips: `1 song` (After this song), `3 songs`, `5 songs`, `10 songs`, `15 songs`.
   - Custom Number Stepper: `[-] N songs [+]` (1 to 99).
   - Live Active Hero Card (when active):
     - "Playing track 2 of 5" (3 songs left).
     - Visual progress bar.
     - Pause Counter / Resume / Cancel controls.
5. **Tab 3: Smart Inactivity Detection:**
   - Enable/Disable Switch toggle (Disabled by default).
   - Explanation card: "AuraMusic monitors your player interactions. If you fall asleep and don't touch any controls for the selected period, playback smoothly stops after the current song finishes."
   - Threshold Selector: `15m`, `30m`, `45m`, `60m`, or custom minutes stepper.
   - Live status readout: "Status: Active • Last interaction X minutes ago".

**Verification:**
Run: `.\gradlew.bat :sharedUi:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

**Commit:**
```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/SmartSleepTimerSheet.kt
git commit -m "feat(ui): create frosted SmartSleepTimerSheet with duration, song count, and inactivity tabs"
```

---

### Task 5: Integrate Sleep Timer Status Pill & Sheet into `NowPlayingScreen.kt`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt`

**Details:**
1. State in `NowPlayingScreen`:
   ```kotlin
   var showSleepTimerSheet by remember { mutableStateOf(false) }
   val sleepStatus by SmartSleepTimer.status.collectAsStateWithLifecycle()
   val sleepMode by SmartSleepTimer.mode.collectAsStateWithLifecycle()
   val remainingMs by produceState(SmartSleepTimer.remainingMs(), sleepStatus) {
       while (isActive) {
           value = SmartSleepTimer.remainingMs()
           delay(500L)
       }
   }
   ```
2. In `playerOverlays`:
   ```kotlin
   if (showSleepTimerSheet) {
       SmartSleepTimerSheet(
           hazeState = playerHaze,
           onDismiss = { showSleepTimerSheet = false },
       )
   }
   ```
3. Sleep Timer Status Pill on Player Screen:
   When `SmartSleepTimer.isActive`:
   Render an animated frosted pill above the scrubber or next to playback quality:
   - `🌙 24:15 left` (for duration)
   - `🌙 Finishing song...` (when expired or waiting for track to end)
   - `🌙 Song 2 of 5 (3 left)` (for song count)
   - `🌙 Timer Paused` (when paused)
   Tapping this pill directly opens `SmartSleepTimerSheet`!
4. Beside `CircleGlyph(icon = MoreHoriz)` or in the player menu, provide a Bedtime Moon icon or menu item to open `SmartSleepTimerSheet`.
5. Update `SongActionsSheet.kt` on Android to open `SmartSleepTimerSheet` when "Sleep timer" is selected.

**Verification:**
Run: `.\gradlew.bat :sharedUi:compileKotlinJvm :app:compileDebugKotlin :desktopApp:compileKotlinJvm`
Expected: ALL BUILD SUCCESSFUL.

**Commit:**
```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt app/src/main/java/com/music/bitchord/ui/components/SongActionsSheet.kt
git commit -m "feat(player): add on-player sleep timer pill and wire SmartSleepTimerSheet into NowPlayingScreen"
```

---

### Task 6: End-to-End Verification and Edge Cases Testing

**Files:**
- Test: `shared/src/jvmTest/kotlin/com/music/bitchord/playback/SmartSleepTimerTest.kt`

**Test Cases to Verify:**
1. **Time-Based Countdown Accuracy:**
   - Setting 5, 15, 30, 45, 60, 90, 120 minutes or custom hours/minutes correctly schedules deadline.
   - Pausing timer freezes remaining time; resuming continues countdown without drift.
   - Adding +5m or +15m extends deadline seamlessly.
2. **Non-Abrupt Song Completion:**
   - When countdown hits 0 while a song is playing, song is NOT cut off immediately; it transitions to `FINISHING_CURRENT_SONG`.
   - When song naturally ends, playback pauses and no further songs play.
3. **Track-Based Song Counter:**
   - Set to 1 song: stops after current song.
   - Set to 5 songs: plays 5 complete tracks. Skips and reorders do not falsely double-count.
   - Live completed/remaining count reflects properly on UI.
4. **Inactivity Detection:**
   - Default disabled; when enabled, resets on any user interaction (`recordUserActivity`).
   - If playback continues without interaction past threshold, gracefully triggers stop after current track.
5. **Background & Screen Lock:**
   - `MonotonicClock` / `SystemClock.elapsedRealtime` ensures deadlines remain accurate even during device sleep.

**Verification Command:**
Run: `.\gradlew.bat test` (or `:shared:jvmTest`, `:app:testDebugUnitTest`, `:desktopApp:test`)
Expected: All tests pass.

---

### Execution Handoff

Plan complete and saved to `docs/plans/2026-10-09-aura-smart-sleep-timer-and-listening-control.md`.

Two execution options:
1. **Subagent-Driven (this session)** - I dispatch fresh subagents per task, review between tasks, fast iteration.
2. **Parallel Session (separate)** - Open a new session with executing-plans, batch execution with checkpoints.
