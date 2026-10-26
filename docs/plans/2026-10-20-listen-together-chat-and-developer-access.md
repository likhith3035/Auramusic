# Listen Together Chat Enhancements, Photo Sharing & Developer Access Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement rich musical interactive chat features in Listen Together (Share Now Playing, Crowd Democracy auto-queue, live typing indicators, Jukebox-only filter, Host & Aux DJ badges), **Picture/Photo Sharing** (in-chat photo attachments with fullscreen preview), and a secret developer elevation workflow (`likhith3035.` -> DOB `29 07 2005` -> Nickname `likki` -> `iam` -> Host Promotion).

**Architecture:**
- **Backend (`backend/`):**
  - Add `ActionDevElevate = "devElevate"` in `protocol/protocol.go` and `party/party.go` allowing developer verification to promote `HostMemberId`, update nickname, and broadcast updated members frame.
  - Relay `typing` frames to broadcast ephemeral typing states.
  - Relay `imageUrl` in `chat` frame payload to all room members.
- **Shared Data Layer (`shared/.../PartyModels.kt`):**
  - Add `val imageUrl: String? = null` to `PartyChatMessage`.
  - Add `val isFounder: Boolean = false` to `PartyMember` and `PartyChatMessage`.
  - Add `PartyTypingEvent` data model.
- **Android & Desktop Core (`ListenTogether.kt`, `DesktopListenTogether.kt`):**
  - Intercept `likhith3035.` before sending so it is never transmitted over the chat wire.
  - Verify DOB (`29 07 2005` / `29072005`) and `iam` confirmation.
  - Send authenticated `devElevate` action to backend and set local nickname to `likki`.
  - Image downscaling utility: compress picked image to lightweight WebP data URI (<60KB) for fast, zero-cloud real-time sharing.
  - Add `sendTyping()` and `typingUsers` flow.
  - Add "Crowd Democracy" auto-queue trigger when a song suggestion reaches 2 upvotes.
- **UI (`PartyChatSheet.kt`, `DesktopListenTogetherUi.kt`):**
  - 📸 Photo attachment button + in-bubble photo card with tap-to-expand lightbox.
  - "Share Now Playing" quick chip above chat input.
  - Developer Terminal Dialog / Secret Elevation Sheet.
  - Live typing indicator bar (`... is typing`).
  - Jukebox Only filter tab.
  - Host (👑), Aux DJ (⚡), and Founder (🔥) badges.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Multiplatform, Coil3 (`AsyncImage`), Ktor WebSockets, Go (backend), Kotlinx Serialization.

---

### Task 1: Backend Protocol, Dev Elevation & Photo/Typing Relay

**Files:**
- Modify: `backend/protocol/protocol.go`
- Modify: `backend/party/party.go`
- Modify: `backend/main.go`

**Step 1: Add `ActionDevElevate` and `TypeTyping` to `protocol/protocol.go`**
- `ActionDevElevate = "devElevate"`
- `TypeTyping = "typing"`

**Step 2: Update `party/party.go`**
- Implement `ElevateHost(memberId string, newNickname string)` which switches `p.HostMemberId = memberId`, updates member's display name, and clears `p.AuxMemberId`.

**Step 3: Update `backend/main.go`**
- In `applyControl`: handle `ActionDevElevate`: verify passkey hash for DOB `29072005`, promote member to host, update nickname, and broadcast `membersFrame` and activity frame.
- In `chat` frame handling: preserve and relay `imageUrl` field.
- In WebSocket frame dispatcher: relay `typing` frames to other connected members.

---

### Task 2: Shared Models Update

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/music/bitchord/data/listentogether/PartyModels.kt`

**Step 1: Add `imageUrl` and `isFounder` to `PartyChatMessage`**
- `val imageUrl: String? = null`
- `val isFounder: Boolean = false`

**Step 2: Add `isFounder` to `PartyMember`**
- `val isFounder: Boolean = false`

**Step 3: Add `PartyTypingEvent`**
- `data class PartyTypingEvent(val senderId: String, val senderName: String, val isTyping: Boolean)`

---

### Task 3: Client Core: Photo Compression, Dev Elevation & Chat Flows

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/data/listentogether/ListenTogether.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopListenTogether.kt`

**Step 1: Implement Local Secret Command Interception**
- If input text equals `likhith3035.` (or `.likhith3035` / `likhith3035`), do NOT send a chat frame.
- Trigger `_showDevElevationModal.value = true`.

**Step 2: Implement `devElevate` Verification**
- Normalize DOB input (`input.filter(Char::isDigit) == "29072005"`).
- Set nickname to `likki`.
- Verify confirmation (`iam`).
- On success: send `devElevate` control frame to server, update local nickname to `likki`, and mark `isFounder = true`.

**Step 3: Photo Compression & Image Sending**
- Add `fun sendChatMessage(text: String, song: PartySongRecommendation? = null, replyTo: PartyChatMessage? = null, imageUrl: String? = null)`
- Support encoding/downscaling selected picture to lightweight compressed WebP Base64 data URI (<60KB) so image sharing works without external S3 buckets.
- Update `onFrame` for `chat` to parse `imageUrl`.

**Step 4: Typing Broadcast & Ephemeral StateFlow**
- `fun sendTyping(isTyping: Boolean)`
- `val typingUsers: StateFlow<List<String>>`

**Step 5: Auto-Queue Upvote Promotion ("Crowd Democracy")**
- When `upvotes >= 2` on a song recommendation and `!rec.autoQueued`:
  - Automatically add to queue via `queueAdd(listOf(track), playNext = false)`.
  - Broadcast celebratory announcement chat message: `🎉 "Song Title" was voted into the queue!`.

---

### Task 4: Android Party Chat UI Enhancements (`PartyChatSheet.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/PartyChatSheet.kt`

**Step 1: Add 📸 Photo Picker & Lightbox Viewer**
- Add Photo Picker launcher (`rememberLauncherForActivityResult(PickVisualMedia())`).
- Add Photo Attachment button (camera/gallery icon) in the input bar.
- In `ChatMessageBubble`: render `AsyncImage` card with rounded corners if `message.imageUrl != null`.
- Tap on photo opens full-screen image preview dialog.

**Step 2: Add "Share Now Playing" Quick Action Chip**
- Render a chip above the chat input bar showing the currently playing track title & artwork.
- Tapping it sends a `PartySongRecommendation` card to chat with 1 tap.

**Step 3: Add Jukebox Filter Tab**
- Toggle between `All Messages` and `🎵 Jukebox Only` to filter songs and show vote counts.

**Step 4: Add Live Typing Indicator**
- Render animated bouncing dots when `typingUsers.isNotEmpty()`.

**Step 5: Add Developer Elevation Dialog Modal**
- Intercepts `likhith3035.` in text input.
- Displays dark-glass terminal card asking:
  1. DOB (`29 07 2005`)
  2. Nickname (`likki`)
  3. Confirmation (`iam`)
- Submit button: `[ 👑 ELEVATE TO HOST ]` with visual feedback & Host unlock.

**Step 6: Badges for Host, Aux DJ, and Founder**
- Host: `👑 Host` badge (amber)
- Aux DJ: `⚡ Aux DJ` badge (purple)
- Founder: `🔥 Founder` badge (neon blue / gold)

---

### Task 5: Desktop Party Chat UI Parity (`DesktopListenTogetherUi.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopListenTogetherUi.kt`

**Step 1: Add Photo Attachment & Display in Desktop Chat**
- File dialog to pick image and send.
- Image display in chat bubble.

**Step 2: Add "Share Current Song" in Desktop Chat**
- Add button beside search to share current playback track.

**Step 3: Developer Terminal Modal on Desktop**
- Support `likhith3035.` secret prompt on desktop.

**Step 4: Typing Indicators & Founder/Aux Badges**
- Display badges in desktop message list.

---

### Task 6: Testing & Verification

**Files:**
- Test: `app/src/test/java/com/music/bitchord/playback/PartyHostOnlyControlTest.kt`
- Test: Build and test execution

**Step 1: Add Unit Tests for Dev Elevation & Interception**
- Test DOB normalization: `"29 07 2005"`, `"29072005"`, `"29/07/2005"`.
- Test secret command interception prevents message broadcast.
- Test image message serialization/deserialization.
- Test auto-queue threshold logic.

**Step 2: Run Gradle compilation and unit test suite**
- Command: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDevDebugUnitTest :desktopApp:compileKotlin`
