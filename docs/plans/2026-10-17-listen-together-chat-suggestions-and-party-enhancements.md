# Listen Together 2.0: In-Party Chat, Real Song Suggestions, Room Veto, Party Recap & Synced Audio

**Status:** Plan Proposed (Awaiting User Permission)  
**Date:** October 17, 2026  
**Target:** Android App (`app`, `sharedUi`, `shared`) & Go Party Server (`backend`)

---

## 1. Executive Summary

This plan elevates AuraMusic's **Listen Together** engine from a passive synchronized playback player into a fully interactive social listening room. It combines:
1. **Real-Time In-Party Chatting**: Fast, low-latency text chat between room members.
2. **Inline Real Song Search & Suggestion Cards**: Search YouTube Music directly inside the chat and send rich cards with `[Play Next]`, `[Add to Queue]`, and `[👍 Vote]`.
3. **Room Veto / Skip Vote System (Feature #3)**: Democracy skip control where >50% votes automatically advances to the next track.
4. **Party Recap & "Save as Playlist" (Feature #5)**: End-of-session summary showing total songs, playtime, top tracks, 1-tap playlist saving to library, and social story sharing.
5. **Synced Equalizer & Visualizer Theme (Feature #6)**: Host-selected soundstage profile and visualizer palette synchronized across all connected devices.

---

## 2. Architecture & Protocol Design

### 2.1 Backend WebSocket Protocol Extensions (`backend/`)
The existing Go server (`backend/main.go` and `backend/protocol/protocol.go`) uses lightweight JSON frames. We extend it with three new frame types:

1. **`chat` (`FrameChat = "chat"`)**:
   ```jsonc
   {
     "type": "chat",
     "id": "msg_uuid",
     "senderId": "member_id",
     "senderName": "Likki",
     "senderAvatar": "https://...",
     "timestampMs": 1757630000000,
     "text": "Check this track out!",
     "song": { // optional if message is a song recommendation
       "videoId": "kJQP7kiw5Fk",
       "title": "Despacito",
       "artist": "Luis Fonsi",
       "coverUrl": "https://...",
       "durationSec": 228
     }
   }
   ```
   - Server validates payload size (< 4KB) and broadcasts to all connected members via `hubInst.Broadcast(p.Code, frame, "")`.

2. **`vote_skip` (`FrameVoteSkip = "vote_skip"`)**:
   ```jsonc
   {
     "type": "vote_skip",
     "memberId": "member_id",
     "action": "toggle" // vote or unvote
   }
   ```
   - Server tracks active skip votes per song. If `activeVotes > len(members) / 2`, server executes `applyControl(p, member, protocol.ActionNext, frame)` and notifies everyone.

3. **`sync_eq` (`FrameSyncEq = "sync_eq"`)**:
   ```jsonc
   {
     "type": "sync_eq",
     "presetName": "Club Soundstage",
     "bandGains": [4, 3, 1, 0, -1, 1, 3, 5, 4, 3],
     "palette": "CYBER_NEON"
   }
   ```
   - Sent by host, broadcasted to room to synchronize visualizer color themes and audio tone.

---

## 3. Detailed Features & UX Flow

### Feature A: In-Party Chat & Rich Song Suggestion Drawer
* **Floating Chat Button**: An animated chat pill badge with unread counter appears on the Now Playing screen and `ListenTogetherScreen`.
* **Chat Drawer (`PartyChatSheet.kt`)**: Frosted glass bottom sheet with message bubbles, sender avatars, and timestamps.
* **Inline Song Search**:
  - Tapping the `🎵` icon opens an instant search sheet.
  - Queries `YouTubeMusicClient.search(query, filter = SearchFilter.SONGS)`.
  - Tapping a song wraps it into a rich suggestion bubble in the chat.
  - Each suggestion bubble has:
    - High-res cover art thumbnail, title, artist, duration.
    - **[ ▶ Play Next ]**: Inserts song at position `index + 1` in the shared queue.
    - **[ + Queue ]**: Appends song to the end of the shared queue.
    - **[ 👍 Vote ]**: Live counter that pulses when other members approve it.

### Feature B (Item #3): Room Veto / Skip Vote System ⏭️
* Next to the Next button on the player, a subtle **Skip Vote pill** shows `Skip (1/3)`.
* Tapping it casts the user's vote.
* When majority (>50%) is reached:
  - Audio initiates a smooth crossfade into the next song.
  - A friendly notification toast appears: `⏭️ Room voted to skip track (3/4 votes)`.
* Votes automatically reset when the track changes.

### Feature C (Item #5): Party Recap & "Save as Playlist" 💾
* The client tracks `PartySessionHistory`:
  - List of all songs played with timestamps.
  - Who recommended each track.
  - Total listening time and listener count.
* When leaving the party or tapping "Session Recap":
  - Shows an elegant recap modal:
    - `AuraMusic Jam Session • 1h 35m • 24 Tracks`
    - Top Song: Most upvoted track in the session.
    - Top DJ: Member who contributed the most queued songs.
  - **Button 1: "Save to Playlist"**: Calls `PlaylistRepository.createPlaylist("Jam with Friends (Oct 17)", songs)` and adds all tracks to the user's local library.
  - **Button 2: "Share Recap Story"**: Exports a customized 9:16 story card with the `AuraMusic by Likki` watermark for Snapchat / Instagram / WhatsApp.

### Feature D (Item #6): Synced Equalizer & Visualizer Theme 🌈
* The host has a "Room Sound & Vibe" selector in the party menu.
* Presets available:
  - `Bass Boosted Party` (Deep low-end rumble + Neon Sunset visualizer)
  - `Club Soundstage` (Vivid spatial EQ + Cyan Cyber visualizer)
  - `Acoustic Vocal` (Crisp mids and highs + Warm Amber visualizer)
  - `Midnight Chill` (Subtle low-pass + Pitch Black AMOLED Nothing OS visualizer)
* Listeners' audio engines gently transition gains, and their Now Playing visualizers pulse with the same color palette.

---

## 4. Implementation Steps

1. **Phase 1: Backend WebSocket Protocol (`backend/`)**:
   - Add `FrameChat`, `FrameVoteSkip`, and `FrameSyncEq` handling in `backend/main.go`.
   - Maintain skip vote tally per room in `backend/party/party.go`.

2. **Phase 2: Android Real-Time Models & Repository (`app/` & `shared/`)**:
   - Add `PartyChatMessage`, `PartySongRecommendation`, `PartySkipVoteState`, and `PartyRecapState` in `ListenTogether.kt`.
   - Wire incoming socket frames into reactive Kotlin `StateFlow`s.

3. **Phase 3: UI Components (`sharedUi/` & `app/`)**:
   - Create `PartyChatSheet.kt` with message history, inline search bar, and interactive song recommendation cards.
   - Add Skip Vote HUD pill in player controls.
   - Create `PartyRecapDialog.kt` with "Save as Playlist" and "Share Story" triggers.
   - Connect EQ / visualizer preset sync to `Equalizer` and `SongShareCard` themes.

4. **Phase 4: Verification & Build**:
   - Verify with Kotlin compilation and unit tests.
   - Build signed production APKs (`release/AuraMusic-v2.0-arm64.apk` and `Universal.apk`).
