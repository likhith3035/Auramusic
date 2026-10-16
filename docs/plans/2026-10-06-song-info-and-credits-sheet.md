# Song Info & Credits Sheet Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Provide a comprehensive, beautifully styled "Song Details & Credits" sheet for each song displaying release date/year, all contributing singers/artists, album information, audio stream specifications, and track trivia.

**Architecture:** Create a shared repository resolver `SongDetailsResolver.kt` in `shared` that gathers full track metadata from the watch response and album browse cache (release date, views, contributors, album info, and audio stream format). Build a shared `@Composable SongInfoSheet` in `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/SongInfoSheet.kt` with dynamic ambient artwork glow, interactive contributor chips, audio quality badges, and quick action shortcuts. Wire it into `SongActionsSheet.kt` (Android long-press / 3-dot menus), `NowPlayingScreen.kt` (player header), and Desktop track menus.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Jetpack Compose (`ModalBottomSheet`, `ArtworkPalette`, `AsyncImage`), InnerTube API, Android SDK, Compose Desktop.

---

### Wishlist Suggestions for this Feature (Enhancing the Experience to Pro-Grade)

1. **Hi-Res / Lossless Audio Spec Badge:**
   - Display an Apple Music / Tidal-style badge indicating exact stream specs (e.g. `24-bit / 96 kHz FLAC Lossless` or `256 kbps Opus / AAC`, audio loudness normalization, and stream source).
2. **Interactive Contributor Chips:**
   - Each singer, featured artist, or band member appears as a pill with their avatar; clicking any artist navigates straight to their discography or artist radio.
3. **Song Trivia & Editorial ("Behind the Track"):**
   - When editorial notes or album blurbs exist on YouTube Music, display an expandable "About this track" card.
4. **1-Tap Quick Action Studio:**
   - Instant action buttons: *Go to Album*, *Jump to Lyrics*, *Share Story Card*, *Copy Link*, and *Start Radio*.
5. **Dynamic Ambient Artwork Glow:**
   - The top banner and card borders softly radiate the dominant colors of the song's album art using `ArtworkPalette`.

---

### Task 1: Create `SongDetails` Model and Resolver

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/model/SongDetails.kt`
- Modify: `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/YtMusicRepository.kt`

**Details:**
1. Define data class `SongDetails`:
   ```kotlin
   data class SongDetails(
       val song: Song,
       val releaseDateOrYear: String? = null,
       val artists: List<ArtistRef> = emptyList(),
       val albumName: String? = null,
       val albumId: String? = null,
       val durationText: String? = null,
       val audioQuality: String? = null,
       val streamSource: String? = null,
       val viewCountText: String? = null,
       val likeCountText: String? = null,
       val description: String? = null,
   )
   ```
2. In `YtMusicRepository`, add `suspend fun resolveSongDetails(song: Song): SongDetails`:
   - Extracts existing `song.artists`, `song.albumName`, `song.albumId`.
   - If `albumId` is present, look up the album header to extract release year and editorial description.
   - If `song.artists` is incomplete, resolve credits from `trackLinks(song.videoId)`.
   - Formats audio stream quality from `StreamResolver` or active player state.

**Verification:**
- Run unit test or verify compilation via `.\gradlew.bat :shared:compileKotlinJvm`.

---

### Task 2: Build the Shared `@Composable SongInfoSheet` UI

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/SongInfoSheet.kt`

**Details:**
1. Create `@Composable fun SongInfoSheet(song: Song, onDismiss: () -> Unit, onOpenArtist: (String) -> Unit, onOpenAlbum: (String) -> Unit, onShareCard: (() -> Unit)? = null)`
2. Layout structure:
   - **Header Card:**
     - High-res cover art (rounded 16.dp with subtle border).
     - Song title (Bold 20.sp).
     - Primary artist & Album name.
     - Audio format pill (`LOSSLESS` / `HIGH QUALITY 256K`).
   - **Metadata Grid:**
     - *Release Date / Year*: e.g., "October 2024" or year tag.
     - *Duration*: e.g., "3:45".
     - *Streams / Views*: e.g., "45M views" (if available).
     - *Source*: "YouTube Music" / "Lossless FLAC" / "Local Audio".
   - **Contributing Artists & Singers Section:**
     - Horizontal or wrapped row of interactive artist chips.
     - Clicking opens artist page.
   - **About / Editorial Description:**
     - Clean typography displaying editorial track notes if available.
   - **Quick Actions Row:**
     - [Open Album] · [Artist Radio] · [Share Song] · [Copy Link]

**Verification:**
- Compile `sharedUi` via `.\gradlew.bat :sharedUi:compileKotlinJvm`.

---

### Task 3: Wire Song Details into Android App (`SongActionsSheet.kt` & `NowPlayingScreen.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/components/SongActionsSheet.kt`
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Details:**
1. In `SongActionsSheet.kt`:
   - Add action row:
     ```kotlin
     ActionRow(
         icon = Icons.Rounded.Info,
         label = stringResource(R.string.song_details_and_credits),
         accent = palette.accent,
         onClick = onOpenDetails,
     )
     ```
2. When selected, display `SongInfoSheet` as a layered bottom sheet.
3. In `MainActivity.kt`, wire navigation handlers for `onOpenArtist`, `onOpenAlbum`, and `onShareCard`.

**Verification:**
- Build Android Dev Debug APK: `.\gradlew.bat :app:assembleDevDebug`.

---

### Task 4: Wire Song Details into Desktop App (`DesktopApp.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`

**Details:**
1. Add "Song info & credits" to the track context menu and player action buttons on Desktop.
2. Render `SongInfoSheet` inside a modal dialog or floating panel when clicked.

**Verification:**
- Compile Desktop Kotlin target: `.\gradlew.bat :desktopApp:compileKotlin`.

---

### Task 5: End-to-End Build & Compilation Verification

**Commands:**
1. Android APK: `.\gradlew.bat :app:assembleDevDebug`
2. Desktop App: `.\gradlew.bat :desktopApp:compileKotlin`
