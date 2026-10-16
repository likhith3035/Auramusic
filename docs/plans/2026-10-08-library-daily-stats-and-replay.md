# Library 1-Day Highlights & Daily Replay Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a dedicated 1-Day (24-Hour / Today) Listening Highlights card to the Library tab featuring the most repeated song, top singer, top music director, daily listening time, and a 1-tap "Play Today's Mix" action, while also enabling "Today" period filtering in Replay.

**Architecture:** Maintain a lightweight, rolling 24-hour daily playback event log alongside monthly buckets in `ListeningStats`. Create a `MusicDirectorClassifier` to distinguish lead vocalists/singers from music directors, composers, and producers. Render a modern glassmorphic `DailyHighlightsCard` in `LibraryScreen` that shows the top repeated track, top singer, top music director, and total minutes, wired with 1-tap playback of the day's mix.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Jetpack Compose, Kotlinx Serialization, Android SDK, Media3 / QueueCoordinator, Desktop Compose.

---

### Key Feature Specifications

1. **Top Repeated Song (1 Day / 24 Hours):**
   - Identifies the track with the highest play count in the last 24 hours.
   - Displays cover art thumbnail, song title, artist, and a distinctive badge: `"🔁 Repeated X times today"` (or `"Most played today: X plays"`).
   - Direct tap plays the song immediately.

2. **Top Singer (1 Day):**
   - Identifies the primary vocalist / lead singer listened to most over the last 24 hours.
   - Displays singer name, avatar/art, and listening minutes/plays.

3. **Top Music Director / Composer (1 Day):**
   - Intelligently parses credited composers, producers, and music directors (e.g. Pritam, A.R. Rahman, Anirudh Ravichander, Sachin-Jigar, Metro Boomin, etc.) from multi-artist credits or track metadata.
   - Displays director name and today's track/listening count.

4. **Other Related Daily Stats:**
   - **Total Listening Time:** e.g., `"1h 15m listened today"`.
   - **Total Tracks & Unique Songs:** e.g., `"18 songs · 7 repeated"`.
   - **Peak Listening Hour / Vibe:** e.g., `"Peak listening: 9:00 PM"`.
   - **"Play Today's Mix" Button:** 1-tap playback action that queues up all songs listened to in the last 24 hours ordered by play count.

5. **Replay Period Integration:**
   - Adds `TODAY("Today (1 Day)")` to `ReplayPeriod` so the user can also switch the full Replay stories, charts, and summary to the current 24-hour window.

---

### Task 1: Music Director & Contributor Classifier

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/stats/MusicDirectorClassifier.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/stats/MusicDirectorClassifierTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicDirectorClassifierTest {
    @Test
    fun testClassifiesSingerAndMusicDirector() {
        val result = MusicDirectorClassifier.classify(
            artistString = "Arijit Singh, Pritam",
            creditedArtists = listOf("Arijit Singh", "Pritam")
        )
        assertEquals("Arijit Singh", result.singer)
        assertEquals("Pritam", result.musicDirector)
    }

    @Test
    fun testSoloArtistActsAsBoth() {
        val result = MusicDirectorClassifier.classify(
            artistString = "The Weeknd",
            creditedArtists = listOf("The Weeknd")
        )
        assertEquals("The Weeknd", result.singer)
        assertEquals("The Weeknd", result.musicDirector)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests`
Expected: FAIL with compilation error (unresolved `MusicDirectorClassifier`).

**Step 3: Write minimal implementation**

```kotlin
package com.music.bitchord.data.stats

data class ArtistRoles(
    val singer: String,
    val musicDirector: String?,
)

object MusicDirectorClassifier {
    private val KNOWN_DIRECTORS = setOf(
        "pritam", "a.r. rahman", "ar rahman", "anirudh ravichander", "sachin-jigar",
        "sachin jigar", "devi sri prasad", "dsp", "vishal-shekhar", "vishal shekhar",
        "shankar-ehsaan-loy", "amit trivedi", "ajay-atul", "hiphop tamizha",
        "harris jayaraj", "yuvan shankar raja", "santhosh narayanan", "thaman s",
        "m.m. keeravaani", "ilaiyaraaja", "vidyasagar", "d. imman", "g.v. prakash kumar",
        "mithoon", "tanishk bagchi", "rochak kohli", "amaal mallik", "b praak", "jaani",
        "metro boomin", "finneas", "dr. dre", "max martin", "jack antonoff", "timbaland",
        "pharrell williams", "kanye west", "mike dean", "ludwig goransson", "hans zimmer"
    )

    fun classify(artistString: String, creditedArtists: List<String> = emptyList()): ArtistRoles {
        val allNames = (creditedArtists + artistString.split(Regex(""",|&|x|feat\.?|ft\.?""", RegexOption.IGNORE_CASE)))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (allNames.isEmpty()) return ArtistRoles(singer = "Unknown Artist", musicDirector = null)
        if (allNames.size == 1) return ArtistRoles(singer = allNames.first(), musicDirector = allNames.first())

        val matchedDirector = allNames.firstOrNull { it.lowercase() in KNOWN_DIRECTORS }
        val singer = allNames.firstOrNull { it != matchedDirector } ?: allNames.first()
        val director = matchedDirector ?: allNames.getOrNull(1)

        return ArtistRoles(singer = singer, musicDirector = director)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/stats/MusicDirectorClassifier.kt
git commit -m "feat: add MusicDirectorClassifier for distinguishing singers and directors"
```

---

### Task 2: Rolling 24-Hour Daily Listening Tracker in `ListeningStats`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/stats/DailyListeningModels.kt`
- Modify: `app/src/main/java/com/music/bitchord/data/stats/ListeningStats.kt`
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopListeningStats.kt`

**Step 1: Write the models for 24-hour daily statistics**

Create `DailyListeningModels.kt`:
```kotlin
package com.music.bitchord.data.stats

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

@Serializable
data class DailyPlayRecord(
    val videoId: String,
    val title: String,
    val artist: String,
    val art: String? = null,
    val timestamp: Long,
    val playedMs: Long,
    val countsAsPlay: Boolean,
    val singer: String,
    val musicDirector: String? = null,
)

data class DailyHighlightsSummary(
    val topRepeatedSong: Song?,
    val topRepeatedCount: Int,
    val topSinger: String?,
    val topSingerMinutes: Long,
    val topMusicDirector: String?,
    val topMusicDirectorMinutes: Long,
    val totalPlayedMs: Long,
    val totalTracksCount: Int,
    val peakHour: Int?,
    val todaySongs: List<Song>,
) {
    val totalMinutes: Long get() = totalPlayedMs / 60_000L
    val isEmpty: Boolean get() = topRepeatedSong == null && totalPlayedMs <= 0L
}
```

**Step 2: Add rolling 24-hour daily event logging to `ListeningStats.kt`**

In `ListeningStats.kt`:
1. Add `private val dailyPlays = mutableListOf<DailyPlayRecord>()` stored in `listening/daily_log.json`.
2. On `record(song, playedMs, countsAsPlay)`:
   - Classify roles using `MusicDirectorClassifier.classify(song.artist, song.artists.map { it.name })`.
   - Append `DailyPlayRecord`.
   - Prune records older than 24 hours (`timestamp < now - 24 * 60 * 60 * 1000L`).
3. Add `suspend fun dailyHighlights(): DailyHighlightsSummary`:
   - Aggregates by `videoId` to find `topRepeatedSong` and `topRepeatedCount`.
   - Aggregates by `singer` to find `topSinger` and listening minutes.
   - Aggregates by `musicDirector` to find `topMusicDirector` and listening minutes.
   - Computes total minutes, peak listening hour, and ordered list of today's songs.
4. Add `TODAY("Today (1 Day)")` to `ReplayPeriod` enum.

**Step 3: Add corresponding support to `DesktopListeningStats.kt`**

Mirror the 24-hour rolling tracker and `dailyHighlights()` method for Desktop JVM.

**Step 4: Verify Compilation**

Run: `.\gradlew.bat :app:compileDebugKotlin :desktopApp:compileKotlin`
Expected: BUILD SUCCESSFUL.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/stats/DailyListeningModels.kt app/src/main/java/com/music/bitchord/data/stats/ListeningStats.kt desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopListeningStats.kt
git commit -m "feat: add 24-hour daily listening tracker and highlights calculation"
```

---

### Task 3: Build the UI Component: `DailyHighlightsCard.kt`

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/library/DailyHighlightsCard.kt`

**Step 1: Implement `DailyHighlightsCard` with Rich Glassmorphism Aesthetics**

Features to implement:
- Gradient badge banner: `✨ TODAY'S HIGHLIGHTS · 24 HOURS` with total listening time pill (`e.g. 48m`).
- **Hero Repeated Song Row**:
  - Rounded cover artwork (with glowing border).
  - Title and Artist.
  - Distinctive pill: `🔁 Repeated X times today` or `🔥 Most Played Today`.
  - Quick-play button directly on the artwork thumbnail.
- **Key Contributor Badges Row**:
  - 🎤 **Top Singer Pill**: `Singer: Arijit Singh (28m)`.
  - 🎼 **Top Music Director Pill**: `Music Director: Pritam (34m)`.
  - ⏰ **Peak Vibe Hour**: `Peak at 9:00 PM`.
- **Action Bar**:
  - `[▶️ Play Today's Mix]` (primary gradient button).
  - `[📖 Full Replay]` (secondary glass button).
- Empty State:
  - Subtle and motivating prompt when no tracks were played in the last 24h yet: `"Play a song today to unlock your 24-hour highlights!"`.

**Step 2: Verify UI compilation**

Run: `.\gradlew.bat :sharedUi:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/library/DailyHighlightsCard.kt
git commit -m "feat: add DailyHighlightsCard for Library screen"
```

---

### Task 4: Integrate Daily Highlights into Android Library (`LibraryPhoneParts.kt` & `MainActivity.kt`)

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/LibraryPhoneParts.kt`
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`

**Step 1: Update `LibraryReplayEntry` or add `DailyHighlightsSection`**

In `LibraryPhoneParts.kt`:
- Add `dailyHighlights: DailyHighlightsSummary?` parameter to `LibraryReplayEntry` (or render as top shelf item in Library).
- Render `DailyHighlightsCard` above the Replay credit cards so it is immediately visible when opening Library.
- Provide callback `onPlayTodayMix: () -> Unit` and `onPlaySong: (Song) -> Unit`.

**Step 2: Wire playback and state in `MainActivity.kt`**

In `MainActivity.kt`:
- Collect `dailyHighlights` from `ListeningStats.dailyHighlights()`.
- Implement `onPlayTodayMix`: queues `dailyHighlights.todaySongs` into the player and starts playback immediately.
- Implement `onPlaySong`: starts playback of the top repeated song.

**Step 3: Verify Compilation on Android**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add app/src/main/java/com/music/bitchord/ui/screens/LibraryPhoneParts.kt app/src/main/java/com/music/bitchord/MainActivity.kt
git commit -m "feat: wire daily 1-day highlights into Android Library screen and player"
```

---

### Task 5: Integrate into Desktop Library (`DesktopApp.kt`)

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`

**Step 1: Wire Daily Highlights in Desktop Library View**

- Collect daily highlights from `DesktopListeningStats.dailyHighlights()`.
- Display `DailyHighlightsCard` at the top of `DesktopDestination.LIBRARY`.
- Wire `onPlayTodayMix` and `onPlaySong` to desktop player coordinator.

**Step 2: Verify Desktop Compilation**

Run: `.\gradlew.bat :desktopApp:compileKotlin`
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt
git commit -m "feat: wire daily 1-day highlights into Desktop Library screen"
```

---

### Task 6: Full Verification, APK Assembly, and Release Note

**Files:**
- Run full tests: `.\gradlew.bat test`
- Build release APK: `.\gradlew.bat assembleRelease` or `assembleDebug`
- Verify outputs in `release/` and document changes.

**Step 1: Run complete test suite**

Run: `.\gradlew.bat :shared:allTests :desktopApp:test`
Expected: All tests pass.

**Step 2: Build APK**

Run: `.\gradlew.bat :app:assembleRelease`
Expected: APK generated in `app/build/outputs/apk/release/`.

**Step 3: Final Commit**

```bash
git commit -m "feat: complete 1-day library highlights with top repeated song, singer, and music director"
```
