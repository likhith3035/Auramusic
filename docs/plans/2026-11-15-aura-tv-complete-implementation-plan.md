# AuraMusic TV Master Implementation Plan
## Complete 10-Foot Experience for Android TV, LG webOS, and Samsung Tizen

**Goal:** Transform AuraMusic into a flagship Smart TV music streaming app matching the interaction design and visual fidelity of Spotify TV, Apple Music TV, and Amazon Music TV across **Android TV/Google TV**, **LG webOS**, and **Samsung Tizen OS**.

---

### 1. Architectural Strategy

Smart TVs run in three distinct operating systems:
1. **LG Smart TVs (webOS)**: Executes Web applications packaged as `.ipk`.
2. **Samsung Smart TVs (Tizen OS)**: Executes Web applications packaged as `.wgt`.
3. **Android TV & Google TV (Sony, TCL, Xiaomi, Chromecast, Fire TV)**: Executes Android runtime applications packaged as `.apk`.

To achieve 100% feature and visual parity with zero code divergence across all three TV ecosystems:
- **Core TV Application (`tvApp/`)**: A GPU-accelerated HTML5/CSS3/ES6 spatial engine designed for 1080p and 4K displays.
- **Android TV Native Host (`app/`)**: An Android Leanback TV Activity embedding the TV engine with Android hardware media decoding, local `WebViewAssetLoader`, and D-pad key forwarding.
- **WebOS & Tizen Packages**: Packaged with `appinfo.json` and `config.xml` respectively.

---

### 2. Execution Phases

#### Phase 1: 10-Foot UI Polish & Advanced Spatial D-Pad Navigation
- 5% overscan safety padding (96px horizontal, 54px vertical) to prevent TV bezel clipping.
- Smooth 6% scale zoom and cyan/white halo glow on focused cards.
- Focus memory so pressing Back returns to the previously focused item.
- Boundary catchers: Left at edge opens sidebar; Down from bottom shelf opens player bar.

#### Phase 2: Flagship Now Playing & Live Synced Karaoke Lyrics
- Side-by-side layout: Large album artwork, track title, artist, bit-perfect badge on the left; live auto-scrolling lyrics on the right.
- Dynamic ambient canvas: Animated blurred gradient mesh extracted from album artwork colors.
- OLED burn-in protection: Auto-dimming of static controls after 2 minutes of idle time and 5-minute subtle pixel shifting.

#### Phase 3: TV On-Screen Grid Keyboard & Instant Search
- 10-foot on-screen D-pad keyboard (A-Z, 0-9, Space, Delete, Clear).
- Instant search queries updating live shelves as characters are entered.
- Seamless remote navigation between keyboard grid and search results.

#### Phase 4: Android TV Leanback Integration
- `AndroidManifest.xml` updates: `android.software.leanback` and `LEANBACK_LAUNCHER`.
- 1080p TV banner asset.
- `AuraTvActivity.kt`: Native Leanback activity running the TV engine with hardware acceleration.

#### Phase 5: Packaging Automation & Verification
- `build-tv.ps1`: Automated build script for Android TV `.apk`, LG webOS `.ipk`, and Samsung Tizen `.wgt`.
- End-to-end syntax and build verification.
