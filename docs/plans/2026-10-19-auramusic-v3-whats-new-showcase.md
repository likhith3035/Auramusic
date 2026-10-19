# AuraMusic v3.0 Version Bump & Creative "What's New" Showcase Plan

**Goal:** Bump AuraMusic to Version 3.0 (`versionCode = 29`, `versionName = "3.0"`), integrate a creative, high-aesthetics "What's New in v3.0" first-launch dialog/card showcasing all the new Listen Together party features, and update the interactive changelog.

**Architecture:**
- **Version bump:** Increment `versionCode` to 29 and `versionName` to `"3.0"` in `app/build.gradle.kts`.
- **First-run detection:** Check `AppSettings` / `SharedPreferences` for `lastSeenWhatsNewVersion`. If `< 3`, display the creative showcase once on startup or when the user enters the app.
- **Showcase UI Design:** A glassmorphism modal / sheet featuring:
  - Aura glowing badge: `"AuraMusic 3.0 • The Cloud Party Update"`
  - Interactive feature bento grid:
    1. **Private Cloud Infrastructure:** Sub-50ms sync on dedicated Render backend.
    2. **In-Party Live Chat:** Instant messages with zero delay.
    3. **Live Soundbites & Floating Reactions:** Airhorn, clapping, flying heart & fire emojis.
    4. **Collaborative Democracy:** Song suggestions & community vote-skip.
    5. **New AuraMusic Invite Engine:** `auramusic://` deep linking.
  - Quick action: "Explore Listen Together" or "Start Listening" button that marks v3.0 as seen.
- **Changelog integration:** Add the comprehensive `v3.0` entry to `AURA_CHANGELOG_ENTRIES` in `AuraChangelog.kt` so users can always revisit it from Settings -> What's New.

---

### Task 1: Version Bump in `app/build.gradle.kts`
**Files:**
- Modify: `app/build.gradle.kts:82-85`
- Change `versionCode` from `28` to `29`.
- Change `versionName` from `"2.0"` to `"3.0"`.

---

### Task 2: Add v3.0 Entry to `AuraChangelog.kt`
**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/about/AuraChangelog.kt:78-85`
- Add entry with ID `auramusic-v3-cloud-party`:
  - Date: "October 19, 2026"
  - Title: "AuraMusic 3.0: Cloud Party & Real-Time Sync"
  - Tag: "Major Release"
  - Highlights:
    - Dedicated cloud WebSocket server on Render.
    - In-party real-time chat with instant optimistic message rendering.
    - Soundbites & floating emoji reactions overlay.
    - Song democracy: track suggestions and vote-skipping.
    - Rebranded invite landing pages and deep links (`auramusic://party`).

---

### Task 3: Creative "What's New in v3.0" Showcase Component
**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/about/AuraV3WhatsNewShowcase.kt`
- Key UI Elements:
  - Glassmorphic card surface with dark gradient (#12121A -> #1B182B) and purple neon border stroke.
  - Sparkling animated header with `AutoAwesome` and version badge.
  - Interactive 4-card bento grid with distinct icon colors (Cyan for Cloud, Violet for Chat, Orange for Soundbites, Green for Democracy).
  - "Try Listen Together" and "Got it" action buttons.

---

### Task 4: Hook First-Launch Trigger in `HomeScreen.kt` / `MainActivity.kt`
**Files:**
- Modify: `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt` (add `lastSeenWhatsNewVersion`)
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/HomeScreen.kt` or `MainActivity.kt`
- When the user launches v3.0 for the first time, present the creative showcase smoothly.
