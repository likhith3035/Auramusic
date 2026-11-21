# Smart Shuffle & Aura Connect Architecture & Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement Smart Shuffle (intelligent recommendation injection into playlist playback) and Aura Connect (local network multi-device remote control and seamless playback handoff between Phone, Desktop, and TV).

**Architecture:** 
1. **Smart Shuffle Engine**: Extends `QueueShuffle` and `YtMusicRepository.radio` with a contextual interleaving algorithm that injects 1 fresh, harmonic recommendation every 3 playlist songs with a distinct visual badge and 1-tap "Save to Playlist" action.
2. **Aura Connect Engine**: Extends the local WebSocket and `ListenTogether` protocol with an embedded zero-configuration LAN daemon (`Ktor CIO`) on Desktop/TV and an mDNS/LAN discovery client on Android to allow real-time remote playback control, volume adjustment, and lossless state handoff.

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform), Ktor WebSocket/HTTP, Media3 ExoPlayer, Signalsmith Stretch, Coroutines/StateFlow.

---

## 1. Feature Utility Analysis: Are These Actually Useful?

### Feature 1: Smart Shuffle
* **Utility Rating: 9/10 (High Daily Value)**
* **The Problem It Solves**: "Playlist Burnout". When you listen to your favorite 40-song playlist for the 20th time, normal shuffle still feels stale because you know every single track. Normal AutoPlay only helps *after* all 40 songs finish.
* **How It Works**: 
  - Shuffle icon has 3 states: `OFF` -> `SHUFFLE` -> `SMART SHUFFLE` (marked with a glowing Nothing Red star/sparkle icon).
  - Every 2 to 3 playlist songs, the engine injects 1 high-relevance recommended track generated from the playlist's average genre and tempo profile.
  - The injected song is clearly labeled with a subtle `RECOMMENDED` badge.
  - Users can tap a small `+` icon on the player or swipe to dismiss it, or save it permanently into the playlist.

### Feature 2: Aura Connect (Spotify Connect Remote Control)
* **Utility Rating: 10/10 (The Ultimate Music Superpower)**
* **The Problem It Solves**: "Device Fragmentation". When you work on your PC or relax in the living room, your computer or TV is connected to high-end studio speakers or a soundbar. Having to walk over to your keyboard every time you want to change a song, skip, or change the volume is annoying.
* **How It Works**:
  - Open AuraMusic on your phone -> Tap the Output icon in Now Playing.
  - Under devices, you see: `Phone Speaker`, `Bluetooth Speaker`, and **`Desktop PC (Windows)`** or **`Living Room TV`** detected automatically on your home Wi-Fi.
  - Tap `Desktop PC`:
    - **Mode A (Remote Control)**: Your phone becomes a remote control. Searching, tapping play, skipping, or sliding the volume on your phone instantly controls the PC.
    - **Mode B (Handoff)**: Walk in from outside listening to a track on your phone, tap your PC, and playback instantly transfers to your desktop speakers at the exact same second without missing a beat.

---

## 2. Smart Shuffle Engineering Architecture

### Data Models & State Machine
In `shared/src/commonMain/kotlin/com/music/bitchord/data/model/Models.kt`:
```kotlin
enum class ShuffleMode {
    OFF,
    STANDARD,
    SMART;

    fun next(): ShuffleMode = when (this) {
        OFF -> STANDARD
        STANDARD -> SMART
        SMART -> OFF
    }
}
```

In `shared/src/commonMain/kotlin/com/music/bitchord/data/model/Song.kt`:
Extend `QueueTier` or add flag:
```kotlin
val isSmartRecommendation: Boolean = false,
val smartSeedSourceTitle: String? = null
```

### Recommendation Injection Algorithm
In `app/src/main/java/com/music/bitchord/playback/QueueShuffle.kt`:
```kotlin
object SmartShuffleEngine {
    private const val INJECTION_CADENCE = 3 // 1 recommendation every 3 tracks

    suspend fun enrichQueue(
        originalQueue: List<Song>,
        currentIndex: Int,
        repository: YtMusicRepository
    ): List<Song> {
        if (originalQueue.isEmpty()) return originalQueue
        
        // 1. Pick seed songs from upcoming window
        val window = originalQueue.subList(
            currentIndex.coerceAtLeast(0),
            minOf(currentIndex + 6, originalQueue.size)
        )
        val seedVideoIds = window.map { it.videoId }

        // 2. Fetch recommendations for the seed
        val recommendations = mutableListOf<Song>()
        for (seedId in seedVideoIds.take(2)) {
            val result = repository.radio(seedId)
            result.getOrNull()?.let { recs ->
                val filtered = recs.filter { rec ->
                    originalQueue.none { it.videoId == rec.videoId } &&
                    recommendations.none { it.videoId == rec.videoId }
                }
                recommendations.addAll(filtered)
            }
        }

        // 3. Interleave recommendations into the queue
        val enriched = mutableListOf<Song>()
        var recIndex = 0
        for (i in originalQueue.indices) {
            enriched.add(originalQueue[i])
            if (i > currentIndex && (i - currentIndex) % INJECTION_CADENCE == 0 && recIndex < recommendations.size) {
                enriched.add(recommendations[recIndex++].copy(isSmartRecommendation = true))
            }
        }
        return enriched
    }
}
```

---

## 3. Aura Connect Engineering Architecture

### Network Discovery (Zero Configuration LAN)
1. **Desktop / TV (Host Mode)**:
   - Starts an ultra-lightweight embedded Ktor HTTP + WebSocket server on port `8765` (or next free port).
   - Broadcasts presence via UDP Multicast / mDNS with service type `_auraconnect._tcp.local` containing:
     - `deviceName`: e.g., "Kamil's PC (Windows)"
     - `deviceType`: `DESKTOP` or `TV`
     - `port`: `8765`
     - `deviceId`: Stable UUID
2. **Android Phone (Controller Mode)**:
   - Scans Wi-Fi network using Android's native `NsdManager` (Network Service Discovery).
   - Automatically populates the Audio Output sheet with detected Aura devices.

### WebSocket Communication Protocol
Bidirectional JSON messages sent over `ws://<desktop-ip>:8765/connect`:

```json
// 1. Device Handshake
{ "type": "HELLO", "clientName": "Vivo Phone", "role": "CONTROLLER" }

// 2. State Sync (Host -> Controller)
{
  "type": "PLAYBACK_STATE",
  "track": { "videoId": "abc", "title": "Starboy", "artist": "The Weeknd", "duration": 230 },
  "positionMs": 45120,
  "isPlaying": true,
  "volume": 0.85,
  "queue": [...]
}

// 3. Remote Commands (Controller -> Host)
{ "type": "COMMAND_PLAY" }
{ "type": "COMMAND_PAUSE" }
{ "type": "COMMAND_SEEK", "positionMs": 62000 }
{ "type": "COMMAND_SKIP_NEXT" }
{ "type": "COMMAND_SET_VOLUME", "volume": 0.70 }
{ "type": "COMMAND_PLAY_TRACK", "videoId": "xyz" }

// 4. Playback Handoff (Transfer active session)
{
  "type": "TRANSFER_SESSION",
  "track": { ... },
  "positionMs": 128450,
  "queue": [ ... ],
  "autoPlay": true
}
```

---

## 4. Phased Implementation Roadmap

### Phase 1: Smart Shuffle Implementation
* **Task 1.1**: Update `ShuffleMode` enum in `shared/` (`OFF`, `STANDARD`, `SMART`).
* **Task 1.2**: Update Player Shuffle button UI with 3-state cycling icon:
  - `OFF`: Dimmed standard icon.
  - `STANDARD`: Active white shuffle icon.
  - `SMART`: Nothing Red shuffle icon with subtle sparkle badge.
* **Task 1.3**: Implement `SmartShuffleEngine` in `sharedUi` / `app` with background pre-fetching and cadence interleaving.
* **Task 1.4**: Add `RECOMMENDED` pill badge on the queue and player screen with a 1-tap `+ Add to Playlist` button.

### Phase 2: Aura Connect Host Engine (Desktop & TV)
* **Task 2.1**: Implement `AuraConnectHost` using Ktor embedded server in `desktopApp` and Android TV.
* **Task 2.2**: Implement UDP/mDNS discovery broadcaster on Desktop.
* **Task 2.3**: Hook `AuraConnectHost` into desktop playback pipeline (`DesktopPlayerEngine`).

### Phase 3: Aura Connect Client & Remote Controller (Android Mobile)
* **Task 3.1**: Implement `NsdManager` LAN scanner in `app` / `shared`.
* **Task 3.2**: Add `Aura Connect Devices` section into the existing [AudioOutputSheet.kt](file:///c:/Users/kamil/OneDrive/Desktop/music%20app/sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/AudioOutputSheet.kt).
* **Task 3.3**: Implement remote playback control bar when connected to an external device.
* **Task 3.4**: Implement seamless 1-tap playback handoff with millisecond position transfer.

---

## 5. Verification & Testing Checklist

1. **Smart Shuffle Unit Tests**:
   - Verify non-empty queue interleaving preserves user-pinned tracks.
   - Verify no duplicate recommendations already in queue.
   - Verify turning Smart Shuffle OFF restores exact original queue.
2. **Aura Connect Network Tests**:
   - Verify mDNS discovery across typical home dual-band Wi-Fi routers.
   - Verify WebSocket automatic reconnection on temporary Wi-Fi drop.
   - Verify latency < 50ms for play/pause/skip commands from phone to PC.
