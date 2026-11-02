# AuraMusic Screenshot Asset Catalog

This directory holds the official screenshots referenced by the main `README.md` for both Android and Desktop distributions.

All screenshot image files must be saved in PNG format (`.png`) using the exact filenames listed below.

---

## Required Screenshots Specification

### 1. `mobile_now_playing.png`
* **Platform**: Android
* **Orientation**: Portrait (9:16 or 9:20 mobile screen)
* **Screen to Open**: Now Playing Screen (tap on any playing song)
* **What to Show**:
  * High-resolution album artwork in the center
  * Dynamic living ambient mesh background (soft glowing gradient extracted from album art)
  * Song title, artist name, and album title
  * Audio track progress seeker and time labels
  * Playback transport controls (Previous, Play/Pause, Next, Shuffle, Repeat)
  * The top `[ AUDIO | VIDEO ]` mode toggle pill

---

### 2. `mobile_video_gestures.png`
* **Platform**: Android
* **Orientation**: Landscape (Fullscreen video mode)
* **Screen to Open**: Video mode in fullscreen (tap `[ VIDEO ]` or rotate device sideways)
* **What to Show**:
  * Active fullscreen video playback
  * The vertical HUD capsule visible on screen (e.g. while swiping left for Brightness or right for Volume)
  * The top `[ FILL ]` / `[ FIT ]` and `[ LOCK ]` pill controls
  * Optional: 2.0x speed boost indicator during long press

---

### 3. `mobile_lyrics_romaji.png`
* **Platform**: Android
* **Orientation**: Portrait
* **Screen to Open**: Lyrics Screen (swipe up on Now Playing or tap the lyrics icon)
* **What to Show**:
  * Line-by-line synchronized scrolling lyrics
  * Romanized transliteration (Romaji for Japanese, Hangul Romanization, or Pinyin) displayed below the original script lines
  * Frosted glass background

---

### 4. `mobile_queue_automix.png`
* **Platform**: Android
* **Orientation**: Portrait
* **Screen to Open**: Queue & Automix Sheet (tap the queue list icon)
* **What to Show**:
  * Current playing track at top
  * Next upcoming tracks in the queue
  * Automix / SmartMix banner with transition analysis or waveform preview
  * Reorder drag handles

---

### 5. `mobile_data_economy.png`
* **Platform**: Android
* **Orientation**: Portrait
* **Screen to Open**: Settings -> Data Usage & Network Economy (or trigger the top floating cellular banner)
* **What to Show**:
  * The top floating frosted banner: `[ CELLULAR DETECTED: SWITCHED TO STUDIO AUDIO (SAVED ~40 MB) ]` with `[ WATCH ANYWAY ]` button, OR the Data Usage Telemetry card in Settings showing Cellular vs. Wi-Fi megabyte breakdown and total MB saved

---

### 6. `mobile_audio_signal_chain.png`
* **Platform**: Android
* **Orientation**: Portrait
* **Screen to Open**: Audio Settings / Output Signal Chain Dialog
* **What to Show**:
  * Audio Signal Chain inspector dialog displaying input format, sample rate (e.g., 44.1 kHz or 96 kHz), bit depth (24-bit), and Bit-Perfect USB DAC direct output status
  * 10-band Equalizer sliders or presets card

---

### 7. `mobile_developer_credits.png`
* **Platform**: Android
* **Orientation**: Portrait
* **Screen to Open**: Settings -> About / Changelog
* **What to Show**:
  * The Developer Profile card honoring Kami Likhith (`@likhith3035`)
  * GitHub link, version v3.2.0 badge, and release highlights

---

### 8. `desktop_main_interface.png`
* **Platform**: Windows Desktop (or Linux/macOS)
* **Orientation**: Landscape (16:9 or 16:10 desktop widescreen)
* **Screen to Open**: AuraMusic Desktop Main Window
* **What to Show**:
  * Sidebar navigation (Home, Explore, Library, Playlists, Settings)
  * Central content area with song tiles, albums, or search results
  * Persistent bottom player bar with album art, transport buttons, progress slider, and volume slider
  * Right-side queue drawer or lyrics sidebar

---

### 9. `desktop_player_settings.png`
* **Platform**: Windows Desktop
* **Orientation**: Landscape (Desktop window)
* **Screen to Open**: Desktop Settings -> Audio / Integration
* **What to Show**:
  * Audio backend configuration (WASAPI Exclusive / ASIO bit-perfect output)
  * Discord Rich Presence toggle
  * Keyboard shortcut reference cheatsheet

