# Implementation Plan: Listen Together Next-Gen Upgrades

**Features Selected:**
1. **Live Floating Reactions (Emoji Sparks)**
2. **"Democracy Queue" (Auto-Queue on Upvotes)**
4. **Synced Live Karaoke Lyrics & Verse-Jump**
5. **Floating Mini Party Pill (In-App Overlay)**
6. **Smart DJ Autopilot (Continuous Playback Fallback)**

---

## 1. Architecture & Protocol Additions

### Protocol Frames (`backend/protocol/protocol.go`)
- `FrameReaction = "reaction"`:
  - Client -> Server: `{"type":"reaction","emoji":"🔥"}`
  - Server -> Client: `{"type":"reaction","senderId":"...","senderName":"...","emoji":"🔥","timestampMs":123456789}`
- `ActionSetDemocracyQueue = "setDemocracyQueue"`:
  - Host toggle to enable/disable auto-queueing song recommendations that reach the upvote threshold (default: 2 upvotes).

### Shared Data Models (`shared/.../PartyModels.kt`)
- `PartyReaction`:
  ```kotlin
  @Serializable
  data class PartyReaction(
      val id: String = "",
      val senderId: String = "",
      val senderName: String = "",
      val emoji: String = "",
      val timestampMs: Long = 0,
  )
  ```
- Add `democracyQueue: Boolean = true` and `autoQueueThreshold: Int = 2` to `PartyPlayback` or room state.
- Add `autoQueued: Boolean = false` to `PartyChatMessage` so duplicate enqueues are prevented.

---

## 2. Feature Details

### Feature 1: Live Floating Reactions (Emoji Sparks)
- **Reaction Picker**:
  - A quick-access pill strip (`🔥`, `❤️`, `💃`, `🎧`, `⚡`, `🎉`) floating at the bottom right of `PartyChatSheet` and `ListenTogetherScreen`.
- **Canvas / Particle Animation**:
  - Smooth floating animations rising from the bottom with randomized horizontal sinusoidal drift, fading out over 2.5 seconds.
  - Haptic feedback tick on tap.

### Feature 2: "Democracy Queue" (Auto-Queue on Upvotes)
- In `backend/party/party.go`:
  - When `FrameClientChat` upvote is processed:
  - If the message contains a `song` recommendation, and `len(upvoterIds) >= 2` (or room threshold), and not already queued:
  - The server automatically adds the track to the party queue with `queuedByName = "Democracy Vote"`.
  - An activity log item is broadcast: `"[Timestamp] Democracy: Auto-queued 'Song Title' (2 votes)"`.
  - Android & Desktop show a celebratory "+Auto Queued" badge on the chat card.

### Feature 4: Synced Live Karaoke Lyrics
- Inside `ListenTogetherScreen` and the player:
  - Add a dedicated "Synced Lyrics" button.
  - Subscribes to `ListenTogether.partyPositionMs()`.
  - Active line auto-scrolls in exact sync across all devices.
  - If the member is host (or room allows seeking), clicking any lyric line seeks playback for the entire party in sync.

### Feature 5: Floating Mini Party Pill (In-App Overlay)
- In `MainActivity.kt`:
  - Renders a floating pill when `state.inParty == true` and the user is browsing outside the Listen Together screen.
  - Displays: Animated sound wave bars `ılılı`, `#ROOM_CODE`, member count, and unread chat badge.
  - Clicking the pill instantly opens the Party Hub or Chat Sheet.

### Feature 6: Smart DJ Autopilot (Continuous Playback Fallback)
- In `ListenTogether.kt` & `DesktopListenTogether.kt`:
  - When the queue index is approaching the end and no more tracks remain:
  - The host device queries YouTube Music recommendations / radio based on the current track and session history.
  - Automatically appends 5 matching tracks so playback never stops awkwardly.

---

## 3. Implementation Steps

1. **Backend (`backend/`)**:
   - Add `FrameReaction` handling and broadcast.
   - Add Democracy Queue auto-queue logic on upvotes.
2. **Shared Models (`shared/`)**:
   - Add `PartyReaction` model and update `PartyChatMessage`.
3. **Android Client (`app/`)**:
   - Add reaction emitter and floating particle animation overlay.
   - Add floating mini party pill in `MainActivity.kt`.
   - Implement DJ Autopilot in `ListenTogether.kt`.
   - Connect synced party position to lyrics view.
4. **Desktop Client (`desktopApp/`)**:
   - Add reaction button bar and reaction stream.
   - Parity for Democracy Queue badges and Autopilot queue refills.
5. **Testing & Release**:
   - Verify compilation (`:app:compileProdReleaseKotlin`, `:desktopApp:compileKotlin`).
   - Sign release APKs.
