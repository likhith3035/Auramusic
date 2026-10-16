# Spotify-Style Song & Story Social Share Sheet Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement a Spotify-style interactive popup when sharing a song, featuring a live 9:16 Story card preview with toggleable [ Song | Lyrics ] modes, dynamic color palette swatches, and direct one-tap sharing to WhatsApp, Instagram Stories, Snapchat, Threads, Copy Link, and System Share.

**Architecture:**
- **Bitmap Rendering Engine (`SongShareCard.kt`):** A high-performance software canvas generator rendering a 1080×1920 (9:16) image with:
  - **Song Mode:** High-resolution album cover art centered, song title in bold typography, artist credits, and an authentic AuraMusic logo watermark.
  - **Lyrics Mode:** Mini album artwork with song header, bold lyrics passage, and AuraMusic brand badge.
  - **Color Themes:** `DYNAMIC_COVER` (dominant album hue), `VIBRANT_GRADIENT` (mesh/radial gradient), `OBSIDIAN_DARK` (deep OLED black with colored glow), and `AURA_CYAN` (signature neon cyan/purple gradient).
- **Interactive Bottom Sheet UI (`SongShareSheet.kt`):**
  - Live card preview with loading indicator.
  - Segmented pill toggle: `[ Song | Lyrics ]`.
  - Palette circle picker row for real-time background customization.
  - Social action row with circular branded buttons:
    - **Copy link:** Copies `https://music.youtube.com/watch?v=...` to clipboard with toast and haptic feedback.
    - **WhatsApp:** Direct one-tap share to `com.whatsapp` / `com.whatsapp.w4b` with rendered image + caption.
    - **Snapchat:** Direct one-tap share to `com.snapchat.android`.
    - **Instagram Stories:** Native `com.instagram.share.ADD_TO_STORY` intent with interactive asset URI.
    - **Threads:** Direct share to Threads (`com.instagram.barcelona`).
    - **More...:** Android system share sheet (`Intent.createChooser`).
    - **Save Image:** Direct save to device gallery via MediaStore.
- **Integration Points:**
  - `MainActivity.kt`: Trigger `SongShareSheet` whenever the user taps "Share" on any track in `SongActionsSheet` or player menu.
  - `AndroidManifest.xml`: Add `<queries>` for Snapchat and Threads packages.

**Tech Stack:** Kotlin, Jetpack Compose, Android Canvas 1080×1920 Bitmap rendering, Android Intents (`ADD_TO_STORY`, `ACTION_SEND`), FileProvider, MediaStore, Coil 3 image loading.

---

### Task 1: Add App Queries to `AndroidManifest.xml`

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:50-62`

**Details:**
Ensure `<queries>` includes:
- `<package android:name="com.snapchat.android" />`
- `<package android:name="com.instagram.barcelona" />`
- `<package android:name="org.telegram.messenger" />`

---

### Task 2: Create the 1080×1920 Card Renderer (`SongShareCard.kt`)

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/player/SongShareCard.kt`

**Details:**
1. Define enums:
   - `enum class SongShareMode { SONG, LYRICS }`
   - `enum class SongSharePalette { DYNAMIC, VIBRANT, OBSIDIAN, AURA_CYAN }`
2. Implement `renderSongShareCard(context, song, mode, palette, lyricsLines)`:
   - Allocates `1080×1920` bitmap.
   - Draws chosen backdrop:
     - `DYNAMIC`: Dominant cover color solid or soft gradient.
     - `VIBRANT`: Radial mesh of extracted cover palette colors.
     - `OBSIDIAN`: Dark OLED `#0B0E14` with ambient colored halo.
     - `AURA_CYAN`: Deep space dark blue `#070B18` with neon cyan `#00E5FF` and purple `#A855F7` ambient waves.
   - For `SongShareMode.SONG`:
     - Centered large rounded cover art (approx 680×680px with 36px corner radius and subtle drop shadow).
     - Bold Title (54sp, white, ellipsised).
     - Artist subtitle (34sp, 85% white).
     - Bottom footer: AuraMusic soundwave logo + "AuraMusic".
   - For `SongShareMode.LYRICS`:
     - Top mini cover art (140×140px) + Title + Artist.
     - Prominent bold lyrics lines in center.
     - Bottom footer: AuraMusic logo badge.

---

### Task 3: Create the UI Modal Bottom Sheet (`SongShareSheet.kt`)

**Files:**
- Create: `app/src/main/java/com/music/bitchord/ui/player/SongShareSheet.kt`

**Details:**
1. `PlayerDrawer` / `ModalBottomSheet` with drag handle.
2. Centered live preview image of the rendered card (`aspectRatio(9f / 16f)`).
3. Segmented control `[ Song | Lyrics ]`.
4. Circular color swatch row with selection indicator rings.
5. Branded circular action buttons:
   - Copy Link (Dark grey circle with Link icon).
   - WhatsApp (WhatsApp green circle `#25D366` with Chat icon).
   - Snapchat (Snapchat yellow `#FFFC00` with Ghost icon).
   - Stories (Instagram gradient `#E1306C` / `#833AB4` with Camera icon).
   - Threads / Messages (`#000000` with Threads icon).
   - More (Dark grey circle with Share icon).
6. Implement intent dispatchers:
   - `shareToInstagramStory(context, uri)`
   - `shareToWhatsApp(context, uri, text)`
   - `shareToSnapchat(context, uri)`
   - `shareToThreads(context, uri, text)`
   - `shareToSystem(context, uri, title, text)`
   - `saveToGallery(context, bitmap, title)`

---

### Task 4: Hook Up Share Sheet in `MainActivity.kt`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Details:**
1. Maintain state `var songToShare by remember { mutableStateOf<Song?>(null) }`.
2. In `songMenuBody`:
   - Replace the instant `Intent.createChooser` text share with:
     ```kotlin
     val share: () -> Unit = {
         songToShare = song
         songActions = null
     }
     ```
3. In `MainActivity` root composition:
   - Render `SongShareSheet` when `songToShare != null`.
   - Pass currently loaded lyrics from `viewModel.lyrics` if sharing the current track.

---

### Task 5: Add Localized Strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Details:**
Add user-facing strings:
- `share_song_title`: "Share song"
- `share_mode_song`: "Song"
- `share_mode_lyrics`: "Lyrics"
- `share_copy_link`: "Copy link"
- `share_whatsapp`: "WhatsApp"
- `share_snapchat`: "Snapchat"
- `share_instagram_stories`: "Stories"
- `share_threads`: "Threads"
- `share_more`: "More"
- `share_link_copied`: "Link copied to clipboard"
- `share_lyrics_unavailable`: "Lyrics not available for this song"

---

### Task 6: Build & Test on Device

**Commands:**
- `.\gradlew.bat assembleDevDebug`
- `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\dev\debug\app-dev-arm64-v8a-debug.apk"`
- Launch app, tap 3 dots on a song -> tap "Share", verify the popup, preview card, color toggles, and social share actions.
