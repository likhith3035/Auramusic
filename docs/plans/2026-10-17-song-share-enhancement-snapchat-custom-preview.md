# Song Share Enhancement Implementation Plan: Authentic Brand Logos, Snapchat "Snap Song" Story Integration, & Ultra-Customizable Preview

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 
1. Replace all crude/approximate social brand icons (Instagram, Snapchat, WhatsApp, Threads) in the share sheet with authentic, official, pixel-perfect vector glyphs and brand aesthetics.
2. Fix Snapchat sharing so the song is sent directly to the **Snap Camera / Story preview** ("Snap Song" wrap-style like Spotify) instead of opening a direct friend chat picker ("chat for all").
3. Build an ultra-customizable song share preview engine that exceeds Spotify and Apple Music with switchable card layouts (Glassmorphic Story, Vinyl Record Slip, Retro Cassette Tape, Minimalist Lyric Poster), 9:16 vs 1:1 aspect ratios, soundwave styles, and an in-sheet audio preview player.

**Architecture:**
- **Authentic Brand Logos (`SongShareSheet.kt`):** Rebuild `InstagramIcon`, `SnapchatIcon`, `WhatsAppIcon`, and `ThreadsIcon` using official SVG path specifications (e.g. Simple Icons / official brand guidelines), rendering the authentic Ghostface Chillah for Snapchat, the official camera glyph for Instagram, the official speech-bubble phone for WhatsApp, and the official `@` swirl for Threads.
- **Snapchat "Snap Song" Story Route (`SongShareSheet.kt`):** Switch from generic `ACTION_SEND` (which forces direct chat selection) to Snapchat's camera/story landing intent (`com.snapchat.android.LandingPageActivity` / `com.snapchat.android.action.STORY_INTENT`) with `interactive_asset_uri` / stream URI and `attachmentUrl` deep link back to the track, matching Spotify's wrap/story flow.
- **Ultra-Customizable Preview Engine (`SongShareCard.kt`, `SongShareSheet.kt`):** Extend `SongShareCard.kt` to support multiple card templates (`SongShareTemplate.GLASS_STORY`, `VINYL_RECORD`, `RETRO_CASSETTE`, `EDITORIAL_LYRICS`, `BENTO_SQUARE`), aspect ratios (`9:16` vs `1:1`), customizable soundwave styles (Bars, Sine, Pulse, Off), and live audio preview playback in `SongShareSheet.kt`.

**Tech Stack:** Kotlin, Jetpack Compose, Android Canvas & Vector Drawables, FileProvider, Android Targeted Intents.

---

### Task 1: Authentic, Official Social Brand Logos in `SongShareSheet.kt`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/SongShareSheet.kt:622-770`

**Problem Statement:**
The social icons currently in `SongShareSheet.kt` were created as rough manual curve approximations (`curveTo`, `quadTo` in 24x24):
- **Snapchat:** A distorted blob that doesn't look like the official Snapchat Ghost (`Ghostface Chillah`).
- **Instagram:** A basic wireframe square with a crude circle instead of the official Instagram camera contour.
- **WhatsApp:** A simplified handset outline inside a rough bubble.
- **Threads:** An incomplete hand-drawn loop.

**Solution:**
Replace them with the exact, mathematically authentic vector paths matching official brand assets:
1. **Snapchat (`SnapchatIcon`):** Official Snap Ghost path (`Ghostface Chillah`) with correct wavy base, arms, and head curvature, rendered with crisp black outline on the official `#FFFC00` yellow button.
2. **Instagram (`InstagramIcon`):** Official Instagram camera glyph with correct corner radius, concentric lens, and top-right flash dot on the official 4-stop sunset gradient.
3. **WhatsApp (`WhatsAppIcon`):** Official WhatsApp speech-bubble silhouette with phone handset at the authentic 45-degree angle on `#25D366`.
4. **Threads (`ThreadsIcon`):** Official Meta Threads continuous spiral `@` glyph on deep black `#101010`.

---

### Task 2: Snapchat "Snap Song" Integration (Camera / Story Preview, Not Direct Chat)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/SongShareSheet.kt:568-583`
- Modify: `app/src/main/AndroidManifest.xml:50-58`

**Problem Statement:**
- Current implementation uses:
  ```kotlin
  val intent = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, uri)
      setPackage("com.snapchat.android")
      ...
  }
  ```
- Android routes this generic `ACTION_SEND` into Snapchat's direct messaging recipient chooser ("Send to Chat" / friends list).
- Spotify and Apple Music bypass direct chat by dispatching to Snapchat's camera / snap story editor with the rendered card as a sticker/canvas and the track deep-link attached.

**Solution:**
1. Update `shareToSnapchat` to target `com.snapchat.android.LandingPageActivity` and provide Snapchat story extras (`attachmentUrl`, `captionText`, `interactive_asset_uri`).
2. When the user taps Snapchat, the Snapchat camera/preview screen opens immediately with the song story card ready to snap or add to "My Story", with the song link attached.
3. Fallback logic: If direct landing is intercepted, fall back to package send, then system share.

---

### Task 3: Ultra-Customizable Song Preview Suite (Surpassing Spotify & Apple Music)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/SongShareCard.kt`
- Modify: `app/src/main/java/com/music/bitchord/ui/player/SongShareSheet.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Customization Features:**
1. **Layout / Card Template Picker:**
   - **Glassmorphic Story (Default):** Floating translucent glass card with drop shadow, large cover art, title, artist, live soundwaves, and refined brand watermark.
   - **Vinyl Record Slip:** Realistic grooved black vinyl record sliding out of the album sleeve, with circular album art in the center and groove highlights.
   - **Retro Cyber-Cassette:** Translucent audio cassette tape layout with tape reels, authentic handwritten-style track name, and glowing neon accents.
   - **Editorial Lyric Poster:** Typography-centric layout focusing on 1-6 chosen lines of lyrics with elegant quotation marks.
   - **Bento Square (1:1):** Compact grid showing album art, stats, duration, and waveform for feed posts.
2. **Aspect Ratio Switcher:**
   - **9:16 Story:** Formatted for Snapchat Stories, Instagram Stories, WhatsApp Status.
   - **1:1 Square:** Formatted for Instagram Feed posts, X (Twitter), Discord embeds.
3. **Soundwave & Metadata Customization:**
   - Visualizer styles: Dynamic Bars, Neon Sine Wave, Ambient Pulse Dots, or Off.
   - Optional duration / listening progress pill (e.g. `01:42 / 03:50`).
4. **Interactive In-Sheet Audio Preview:**
   - A mini play/pause preview button right inside the bottom sheet, allowing the user to listen to the song while designing their card before sharing.

---

### Task 4: Testing & Verification
1. **Visual Accuracy:** Verify all 4 social brand logos in `SongShareSheet` match the real official brand assets.
2. **Snapchat Routing:** Verify opening Snapchat from the share sheet routes straight into the Snap Camera / Story editor rather than a chat picker.
3. **Customization Fluidity:** Verify switching templates, aspect ratios, and palettes updates the card preview instantly with no lag or memory leaks.
4. **Build Verification:** Run `./gradlew compileProdDebugKotlin` to ensure clean compilation.
