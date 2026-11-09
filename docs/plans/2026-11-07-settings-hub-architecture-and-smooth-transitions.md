# Settings Hub Architecture & Smooth Transitions Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the monolithic 3,360-line Settings screen into a modular, high-performance Category Hub with 60/120fps directional slide transitions, zero startup lag, and dedicated domain sub-screens.

**Architecture:** Replace the single vertical 80+ item list in `SettingsSheet.kt` with a two-level architecture: a lightweight `SettingsHubScreen` presenting 7 categorized domain tiles and global search, routing via Compose `AnimatedContent` with directional horizontal slide + fade transitions to dedicated sub-screens (`Audio`, `Appearance`, `Content`, `Storage`, `Network`, `Sources`, `Advanced`). Decouple state collections into sub-screens so the Hub renders in under 5ms without main-thread hitching. Use AuraMusic's exact UI design system (`SettingsGroup`, `SettingsRow`, `GroupShape`, `Chevron`, `MaterialTheme.colorScheme.surfaceVariant`) to ensure 100% visual consistency.

**Tech Stack:** Jetpack Compose, Kotlin Coroutines & Flow, Material 3 Expressive Motion (`EmphasizedDecelerateEasing`, `slideInHorizontally`, `slideOutHorizontally`), AndroidX Navigation/BackHandler.

---

### Task 1: Define Settings Navigation State & Routes

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/screens/settings/SettingsRoute.kt`
- Test: `app/src/test/java/com/music/bitchord/ui/SettingsRouteTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.ui

import com.music.bitchord.ui.screens.settings.SettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsRouteTest {
    @Test
    fun `hub is the root route`() {
        val root = SettingsRoute.HUB
        assertEquals("hub", root.id)
    }

    @Test
    fun `route resolution matches known category ids`() {
        assertEquals(SettingsRoute.AUDIO, SettingsRoute.fromId("audio"))
        assertEquals(SettingsRoute.APPEARANCE, SettingsRoute.fromId("appearance"))
        assertEquals(SettingsRoute.CONTENT, SettingsRoute.fromId("content"))
        assertEquals(SettingsRoute.STORAGE, SettingsRoute.fromId("storage"))
        assertEquals(SettingsRoute.NETWORK, SettingsRoute.fromId("network"))
        assertEquals(SettingsRoute.ADVANCED, SettingsRoute.fromId("advanced"))
        assertNull(SettingsRoute.fromId("unknown_route"))
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testDevDebugUnitTest --tests "com.music.bitchord.ui.SettingsRouteTest"
```
Expected: FAIL with unresolved reference `SettingsRoute`.

**Step 3: Write minimal implementation**

Create `app/src/main/java/com/music/bitchord/ui/screens/settings/SettingsRoute.kt`:
```kotlin
package com.music.bitchord.ui.screens.settings

enum class SettingsRoute(val id: String) {
    HUB("hub"),
    AUDIO("audio"),
    APPEARANCE("appearance"),
    CONTENT("content"),
    STORAGE("storage"),
    NETWORK("network"),
    ADVANCED("advanced");

    companion object {
        fun fromId(id: String?): SettingsRoute? = entries.firstOrNull { it.id == id }
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testDevDebugUnitTest --tests "com.music.bitchord.ui.SettingsRouteTest"
```
Expected: PASS

---

### Task 2: Build Smooth Directional Motion Transitions

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/screens/settings/SettingsTransitions.kt`
- Test: `app/src/test/java/com/music/bitchord/ui/SettingsTransitionsTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.ui

import com.music.bitchord.ui.screens.settings.SettingsRoute
import com.music.bitchord.ui.screens.settings.SettingsTransitions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTransitionsTest {
    @Test
    fun `navigating from hub to subpage is forward`() {
        assertTrue(SettingsTransitions.isForward(SettingsRoute.HUB, SettingsRoute.AUDIO))
        assertTrue(SettingsTransitions.isForward(SettingsRoute.HUB, SettingsRoute.APPEARANCE))
    }

    @Test
    fun `navigating from subpage to hub is reverse`() {
        assertFalse(SettingsTransitions.isForward(SettingsRoute.AUDIO, SettingsRoute.HUB))
        assertFalse(SettingsTransitions.isForward(SettingsRoute.NETWORK, SettingsRoute.HUB))
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testDevDebugUnitTest --tests "com.music.bitchord.ui.SettingsTransitionsTest"
```
Expected: FAIL with unresolved reference `SettingsTransitions`.

**Step 3: Write minimal implementation**

Create `app/src/main/java/com/music/bitchord/ui/screens/settings/SettingsTransitions.kt`:
```kotlin
package com.music.bitchord.ui.screens.settings

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

object SettingsTransitions {
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    const val SLIDE_DURATION_MS = 280

    fun isForward(initial: SettingsRoute, target: SettingsRoute): Boolean {
        return initial == SettingsRoute.HUB && target != SettingsRoute.HUB
    }

    fun transitionSpec(initial: SettingsRoute, target: SettingsRoute): ContentTransform {
        val forward = isForward(initial, target)
        val enter = if (forward) {
            slideInHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedDecelerateEasing),
                initialOffsetX = { fullWidth -> (fullWidth * 0.95f).toInt() },
            ) + fadeIn(tween(200))
        } else {
            slideInHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedDecelerateEasing),
                initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
            ) + fadeIn(tween(200))
        }

        val exit = if (forward) {
            slideOutHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedAccelerateEasing),
                targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
            ) + fadeOut(tween(150))
        } else {
            slideOutHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedAccelerateEasing),
                targetOffsetX = { fullWidth -> (fullWidth * 0.95f).toInt() },
            ) + fadeOut(tween(150))
        }

        return enter togetherWith exit
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testDevDebugUnitTest --tests "com.music.bitchord.ui.SettingsTransitionsTest"
```
Expected: PASS

---

### Task 3: Build the Lightweight Settings Hub Screen

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/screens/settings/SettingsHubScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` (add Hub category titles & descriptive subtitles)

**Step 1: Define category data models & string resources**
Add strings for:
- `settings_hub_audio_title`: "Audio & Playback"
- `settings_hub_audio_subtitle`: "Lossless quality, Bit-Perfect DAC, Equalizer, Crossfade"
- `settings_hub_appearance_title`: "Appearance & Interface"
- `settings_hub_appearance_subtitle`: "Obsidian themes, Liquid Glass, Full bleed art, Gestures"
- `settings_hub_content_title`: "Home & Content Feed"
- `settings_hub_content_subtitle`: "Shelves visibility, Top picks, Video hides, Music-only converter"
- `settings_hub_storage_title`: "Downloads & Storage"
- `settings_hub_storage_subtitle`: "Download quality, Cache allocation, Storage cleanup"
- `settings_hub_network_title`: "Network & Data Economy"
- `settings_hub_network_subtitle`: "Cellular streaming caps, Smart auto-switching, Usage dashboard"
- `settings_hub_sources_title`: "Sources & Addons"
- `settings_hub_sources_subtitle`: "External providers, Token authentication, Quality tiers"
- `settings_hub_advanced_title`: "Aura AI & Advanced Lab"
- `settings_hub_advanced_subtitle`: "Neural DJ, Radar, Discord RPC, Scrobbling, System logs"

**Step 2: Create `SettingsHubScreen.kt`**
Renders:
1. Top Profile / Account banner card with quick sign-in status and avatar.
2. Search input field with instant item filtering.
3. 7 Curated category tiles using Material 3 Expressive card style with dark obsidian background and subtle rounded glass borders.
4. Quick actions: Check for Updates, App Language, Version info footer.

---

### Task 4: Modularize Domain Sub-Screens

Extract monolithic sections from `SettingsSheet.kt` into dedicated, scoped sub-screen composables:
- `AudioSettingsSubScreen.kt`: Audio Quality, Bit-Perfect USB DAC, Crossfade, Equalizer shortcut, ReplayGain, Volume normalization.
- `AppearanceSettingsSubScreen.kt`: Dark Obsidian Themes, Liquid Glass & Performance mode, Full-bleed artwork, Living Waveform splash, Player gestures & HUD styling.
- `ContentSettingsSubScreen.kt`: Home Shelves (Recents, Top Picks, Moods, Video hides, Square thumbnails), Video-to-audio converter ("Prefer music-only version").
- `StorageSettingsSubScreen.kt`: Download quality, offline cache sizes, cache directory picker, clear cache actions.
- `NetworkSettingsSubScreen.kt`: Cellular stream quality, smart data economy auto-switching, data usage dashboard, telemetry.
- `AdvancedSettingsSubScreen.kt`: Discord RPC, Last.fm scrobbling, Backup & restore, Neural DJ toggles, Developer info & stats.

Each sub-screen includes:
- Top Sub-Screen App Bar with Category Title and Back Arrow Button.
- Dedicated scroll state isolated to that screen.
- State flow collections scoped strictly to that screen, eliminating background recompositions when other categories change.

---

### Task 5: Integrate Hub & Sub-Screens with AnimatedContent in `SettingsScreen`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt`
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Step 1: Replace monolithic scroll in `SettingsScreen` with `AnimatedContent`**
```kotlin
var currentRoute by rememberSaveable { mutableStateOf(SettingsRoute.HUB) }

BackHandler(enabled = currentRoute != SettingsRoute.HUB) {
    currentRoute = SettingsRoute.HUB
}

AnimatedContent(
    targetState = currentRoute,
    transitionSpec = { SettingsTransitions.transitionSpec(initialState, targetState) },
    label = "SettingsRouteMotion",
) { route ->
    when (route) {
        SettingsRoute.HUB -> SettingsHubScreen(
            onNavigate = { destination -> currentRoute = destination },
            onSources = onSources,
            onEqualizer = onEqualizer,
            ...
        )
        SettingsRoute.AUDIO -> AudioSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
        SettingsRoute.APPEARANCE -> AppearanceSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
        SettingsRoute.CONTENT -> ContentSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
        SettingsRoute.STORAGE -> StorageSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
        SettingsRoute.NETWORK -> NetworkSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
        SettingsRoute.ADVANCED -> AdvancedSettingsSubScreen(onBack = { currentRoute = SettingsRoute.HUB }, ...)
    }
}
```

**Step 2: Update `MainActivity.kt` BackHandler & Navigation**
Ensure pressing system back while in any sub-screen returns cleanly to `SettingsRoute.HUB`, and pressing back from `SettingsRoute.HUB` closes Settings smoothly to Home.

---

### Task 6: Verification & Performance Benchmark

**Step 1: Execute all unit tests**
Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testDevDebugUnitTest
```
Verify 100% pass rate.

**Step 2: Compile Kotlin and Assemble APK**
Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:assembleDevDebug
```
Verify zero build errors and successful APK generation.

**Step 3: Verification of Frame Rate & Memory Profile**
- Hub initial composition time < 10ms (down from 120ms+).
- Transition animation executes at constant 60/120 FPS without hitching.
- Emoji check: verify 0 Unicode emojis across all code and strings.

