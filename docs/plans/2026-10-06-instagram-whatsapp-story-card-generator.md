# Instagram & WhatsApp 9:16 Story Lyric Card Generator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform the mobile lyrics sharing experience into an Instagram and WhatsApp 9:16 Story Lyric Card Generator with 9:16 story-first defaults, multiple visual card themes, and direct one-tap sharing intents to Instagram Stories and WhatsApp.

**Architecture:**
- **Story Styling Engine:** Extend `LyricsShareCard.kt` with a `StoryStyle` enum (`AMBIENT_BLUR`, `OBSIDIAN_DARK`, `VIBRANT_AURA`) to control background lighting, gradient curves, and typography contrasts on the 1080×1920 canvas.
- **Social Intent Dispatchers:** In `AndroidLyricsShareSheet.kt`, implement direct intent launchers:
  - Instagram Stories: `com.instagram.share.ADD_TO_STORY` with `setDataAndType(uri, "image/*")` and package targeting `com.instagram.android`.
  - WhatsApp: `Intent.ACTION_SEND` targeting `com.whatsapp` or `com.whatsapp.w4b` with `EXTRA_STREAM`.
  - Package detection: dynamically check whether Instagram and WhatsApp are installed on the device to render dedicated action buttons.
- **Story-First UI in Player Drawer:**
  - Default `story = true` (1080×1920 9:16 aspect ratio).
  - Theme picker row (Ambient, Obsidian, Vibrant) to customize the aesthetic before sharing.
  - Dedicated action row with branded Instagram and WhatsApp buttons alongside Save to Gallery and System Share.

**Tech Stack:** Android Native Intents (`ADD_TO_STORY`, `ACTION_SEND`), Android Bitmap Canvas (1080×1920), Jetpack Compose / Compose Multiplatform, FileProvider URI sharing.

---

### Task 1: Add Story Styles & Theming to `LyricsShareCard.kt`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/LyricsShareCard.kt`

**Details:**
1. Define `enum class StoryStyle { AMBIENT_BLUR, OBSIDIAN_DARK, VIBRANT_AURA }`.
2. Update `renderLyricsShareCard`:
   - Accept `style: StoryStyle = StoryStyle.AMBIENT_BLUR`.
   - In `drawBackdrop(canvas, cover, ...)`:
     - `AMBIENT_BLUR`: Soft blurred album art with subtle top and bottom radial gradient vignette.
     - `OBSIDIAN_DARK`: Deep pure dark obsidian backdrop with a soft centered colored glow around the album art.
     - `VIBRANT_AURA`: Richer saturation and gradient warmth inspired by the album art palette.
3. Keep default `story = true` for 9:16 format.

---

### Task 2: Implement Social Intent Helpers in `AndroidLyricsShareSheet.kt`

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/AndroidLyricsShareSheet.kt`

**Details:**
1. Helper function `isPackageInstalled(context: Context, packageName: String): Boolean`.
2. Helper function `shareToInstagramStory(context: Context, uri: Uri)`:
   - Construct `Intent("com.instagram.share.ADD_TO_STORY")`.
   - Set data and type `setDataAndType(uri, "image/*")`.
   - Add flag `FLAG_GRANT_READ_URI_PERMISSION`.
   - Set package `com.instagram.android`.
   - Launch activity with fallback to general chooser if Instagram activity is unavailable.
3. Helper function `shareToWhatsApp(context: Context, uri: Uri)`:
   - Check `com.whatsapp` or `com.whatsapp.w4b`.
   - Construct `Intent(Intent.ACTION_SEND)` with `EXTRA_STREAM = uri`, `type = "image/*"`.
   - Launch activity with fallback to general chooser.

---

### Task 3: Enhance `AndroidLyricsShareSheet` UI with Theme Picker and Social Actions

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/player/AndroidLyricsShareSheet.kt`

**Details:**
1. Set initial `var story by remember(request) { mutableStateOf(true) }` so cards default to 9:16 vertical story format.
2. Add state `var selectedStyle by remember(request) { mutableStateOf(StoryStyle.AMBIENT_BLUR) }`.
3. Render a horizontal style picker (FilterChips / tabs) for Ambient, Obsidian, and Vibrant styles.
4. Detect `isInstagramInstalled` and `isWhatsAppInstalled`.
5. Render a branded social action grid:
   - If Instagram is installed: "Instagram Story" button with Instagram gradient accent.
   - If WhatsApp is installed: "WhatsApp" button with WhatsApp green accent.
   - "Save Image" (Download icon) to save to gallery.
   - "More..." (Share icon) to open Android system share sheet.
6. Provide an aspect ratio toggle ("9:16 Story" vs "Compact Strip").

---

### Task 4: Add Supporting String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Details:**
1. Add strings:
   - `<string name="share_instagram_story">Instagram Story</string>`
   - `<string name="share_whatsapp">WhatsApp</string>`
   - `<string name="story_style_ambient">Ambient</string>`
   - `<string name="story_style_obsidian">Obsidian</string>`
   - `<string name="story_style_vibrant">Vibrant</string>`
   - `<string name="format_story">9:16 Story</string>`
   - `<string name="format_compact">Compact</string>`

---

### Task 5: Compilation and Verification

**Commands:**
1. Android Build:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat compileDevDebugSources
   ```
   *Expected: BUILD SUCCESSFUL*

2. Desktop Build:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :desktopApp:compileKotlin
   ```
   *Expected: BUILD SUCCESSFUL*
