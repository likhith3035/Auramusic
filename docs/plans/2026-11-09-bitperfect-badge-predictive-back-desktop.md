# AuraMusic: Bit-Perfect Player Badge, Predictive Back & Desktop Enhancements Plan

> **Goal**: 
> 1. Elevate audiophile feedback with a live, glowing **Bit-Perfect DAC Status Badge** in the Now Playing screen and Audio Output sheet.
> 2. Implement **Android 14/15+ Predictive Back Gesture** navigation in Settings with hardware-accelerated scale preview, plus **Deep-Linking** from player controls into specific Settings sub-pages.
> 3. Enhance **Desktop App (KMP)** with backspace/escape navigation parity and assemble the standalone **Windows Portable Release Package**.

---

## 1. Architectural Design

```
+-----------------------------------------------------------------------------------------+
|                                    AuraMusic Ecosystem                                  |
+-----------------------------------------------------------------------------------------+
                                              |
       +--------------------------------------+------------------------------------+
       |                                                                           |
       v                                                                           v
+-----------------------------+                                            +-----------------------------+
|        Android App          |                                            |         Desktop App         |
+-----------------------------+                                            +-----------------------------+
| • BitPerfectOutput          |                                            | • WASAPI Exclusive          |
| • Android 14/15 USB DAC     |                                            | • Backspace/Esc Navigation  |
| • Predictive Back Handler   |                                            | • Windows Portable Build    |
+-----------------------------+                                            +-----------------------------+
               \                                                                          /
                \                                                                        /
                 v                                                                      v
        +--------------------------------------------------------------------------------+
        |                          shared / sharedUi Layer                               |
        +--------------------------------------------------------------------------------+
        | • NerdStats.bitPerfect: StateFlow<BitPerfectTelemetry?>                        |
        | • PlaybackQualityLabel: "Bit-Perfect 192kHz · 24-bit" + Shimmer                |
        | • AudioOutputSheet: Active USB DAC hardware bypass pill                        |
        | • SettingsRoute Deep-Linking: AUDIO, APPEARANCE, CONTENT                       |
        +--------------------------------------------------------------------------------+
```

---

## 2. Phase-by-Phase Implementation Plan

### Phase 1: Bit-Perfect DAC Status Badge & Player Telemetry (Option 2)

#### 1.1 Shared Telemetry Model in `shared`
- **File**: `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/NerdStats.kt`
- Define `BitPerfectTelemetry`:
  ```kotlin
  data class BitPerfectTelemetry(
      val active: Boolean = false,
      val sampleRateHz: Int = 0,
      val bitDepth: Int = 0,
      val deviceName: String? = null,
      val isDacLimited: Boolean = false,
  )
  val bitPerfect = MutableStateFlow<BitPerfectTelemetry?>(null)
  ```

#### 1.2 Status Synchronization in `app`
- **File**: `app/src/main/java/com/music/bitchord/playback/audio/bitperfect/BitPerfectOutput.kt`
- In `publish(status: BitPerfectOutput.Status)`:
  - If `status.active`, update `NerdStats.bitPerfect.value` with active sample rate and bit depth.
  - If inactive, update `NerdStats.bitPerfect.value = null`.
- In `publishIdle()`:
  - Reset `NerdStats.bitPerfect.value = null`.

#### 1.3 UI Badge Rendering in `sharedUi`
- **File**: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/PlayerControls.kt`
- In `PlaybackQualityLabel` & `LosslessOrStats`:
  - Collect `val bitPerfect by NerdStats.bitPerfect.collectAsStateWithLifecycle()`.
  - When `bitPerfect?.active == true`, render the specialized `Bit-Perfect` label:
    - Text: `Bit-Perfect ${bitPerfect.sampleRateHz / 1000}kHz · ${bitPerfect.bitDepth}-bit` (or `Bit-Perfect` fallback if rate unknown).
    - Styling: Ambient cyan/obsidian shimmer with `Icons.Rounded.GraphicEq` audiophile icon.
- **File**: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/AudioOutputSheet.kt`
  - Display an active "Bit-Perfect Hardware Direct" badge next to the connected USB DAC.

---

### Phase 2: Settings Deep-Linking & Predictive Back Navigation (Option 3)

#### 2.1 Android 14/15+ Predictive Back Navigation
- **File**: `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt`
- Update `SettingsScreen` to accept `initialRoute: SettingsRoute = SettingsRoute.HUB`.
- Implement `PredictiveBackHandler`:
  ```kotlin
  var backProgress by remember { mutableFloatStateOf(0f) }

  PredictiveBackHandler(enabled = currentRoute != SettingsRoute.HUB) { progress ->
      try {
          progress.collect { backEvent ->
              backProgress = backEvent.progress
          }
          currentRoute = SettingsRoute.HUB
      } catch (e: CancellationException) {
          // user cancelled swipe
      } finally {
          backProgress = 0f
      }
  }
  ```
- Wrap sub-page content in `Modifier.graphicsLayer`:
  - `scaleX = 1f - backProgress * 0.06f`
  - `scaleY = 1f - backProgress * 0.06f`
  - `alpha = 1f - backProgress * 0.15f`
  - Delivers seamless hardware-accelerated swipe preview back to the Category Hub.

#### 2.2 Deep-Linking from Player Controls & Top Bar
- **File**: `app/src/main/java/com/music/bitchord/MainActivity.kt`
- Introduce `var settingsInitialRoute by rememberSaveable { mutableStateOf(SettingsRoute.HUB) }`.
- Wire player actions:
  - Long press on `AudioOutputPill`: `settingsInitialRoute = SettingsRoute.AUDIO; showSettings = true`.
  - Equalizer shortcut: `settingsInitialRoute = SettingsRoute.AUDIO; showSettings = true`.
  - Theme/Appearance actions: `settingsInitialRoute = SettingsRoute.APPEARANCE; showSettings = true`.
- Pass `initialRoute = settingsInitialRoute` to `SettingsScreen`.

---

### Phase 3: Desktop App Enhancements & Windows Portable Release (Option 4)

#### 3.1 Keyboard Navigation Parity
- **File**: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`
- Add `Key.Backquote` / `Key.Backspace` support to `onKeyEvent`:
  - When inside any `overlays.settingsPage`, pressing `Backspace` steps back to `DesktopSettingsPage.MAIN` without affecting text fields.

#### 3.2 Standalone Windows Portable Distribution Build
- Verify and execute `./gradlew :desktopApp:windowsPortable`.
- Confirm packaging into `desktopApp/build/distributions/AuraMusic-3.2.0-windows-portable.zip` (or updated 3.3.0).

---

## 3. Verification & Testing Matrix

| Component | Target Verification | Command / Tool |
|---|---|---|
| Bit-Perfect Telemetry | Unit test status updates & format string | `:app:testDevDebugUnitTest --tests "com.music.bitchord.playback.audio.bitperfect.*"` |
| NerdStats Flow | Unit test BitPerfectTelemetry flow & assertions | `:shared:testJvmTest` |
| Settings Deep-Link | Unit test route transitions & initial route | `:app:testDevDebugUnitTest --tests "com.music.bitchord.ui.SettingsRouteTest"` |
| Desktop App | Compile & unit tests | `:desktopApp:test` and `:desktopApp:compileKotlin` |
| Android Build | DevDebug APK assembly | `:app:assembleDevDebug` |
| Zero Emojis | Strict zero Unicode emoji check across all files | Python AST/regex scanner |

---

## 4. Commitments & Constraints
- **Zero Emojis**: Strictly 0 Unicode emojis in all code, resources, messages, and artifacts.
- **Obsidian Dark Theme Tokens**: 100% adherence to AuraMusic dark obsidian tokens (`surfaceVariant`, `GroupShape`, `bodyLarge`, `onSurfaceVariant`).
- **Zero Regressions**: Bit-Perfect DAC modes, USB drivers, equalizer, and player gestures remain 100% operational.

