# Aura AI Companion & Behavior Analysis Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a state-of-the-art AI Music Chatbot ("Aura AI") that provides deep psychological and behavioral analysis of the user's listening habits, features an ultra-premium glassmorphic chat UI with real-time markdown and stat cards, embeds interactive playable song recommendations directly inside the chat feed, and includes natural language mix curation with customizable DJ personas.

**Architecture:** A layered multiplatform architecture where `AuraBehaviorEngine` extracts listening statistics, repeat cycles, chronotypes, and artist affinities from `ListeningStats` and `DailyListeningModels`. `AuraAiService` formats this data into a rich behavioral context prompt for Google Gemini (with an offline heuristic fallback engine when network or API key is absent) and parses structured song tags. `YtMusicRepository` resolves song tags into high-resolution playable `Song` objects. Compose Multiplatform components in `sharedUi` render a glassmorphic chat overlay with interactive `AuraSongCard` widgets, animated typing cursors, and 1-tap playback controls wired into `QueueCoordinator`.

**Tech Stack:** Kotlin Multiplatform (Android & JVM Desktop), Compose Multiplatform, Material3, Ktor Client (OkHttp/Engine), Kotlinx Serialization, Kotlinx Coroutines & StateFlow, Coil3 image loading, Media3 / QueueCoordinator playback.

> [!IMPORTANT]
> **100% Native App UI Commitment:** The entire AI chatbot experience is built strictly as native Jetpack Compose / Compose Multiplatform UI (`sharedUi`). It uses AuraMusic's glassmorphic design system, Haze blurs, theme-reactive colors, spring animations, and native interactive `AuraSongCard` widgets with direct playback binding — zero webviews, zero external embeds.

> [!TIP]
> **Optimal Entry Point Placement (Multi-Touchpoint Hybrid):**
> 1. **Primary Global Button:** An animated glowing Aura Sparkle orb in the Top App Bar (right beside Settings) accessible anywhere in 1 tap.
> 2. **Featured Discovery Banner:** A card in the Home Highlights Carousel (`AuraFeature.AURA_AI`) introducing behavior analysis, taste roasts, and smart mixes.
> 3. **Contextual In-Player Button:** A sparkle icon inside `NowPlayingScreen` allowing 1-tap track breakdown and similar track recommendations.
> 4. **Optional Pinned Bottom Tab:** Configurable via AppSettings for users who prefer a dedicated bottom tab.

---

### Key Feature Specifications

1. **Secret Developer Gate & AI Access Unlock:**
   - **Gated Access by Default:** The AI Chatbot is completely hidden and locked by default (`AppSettings.auraAiUnlocked = false`). Without unlocking, no sparkle icon, no home card, and no AI routes exist in the UI.
   - **Secret Activation Phrase:** In the Settings screen search bar, the user types `"auramusic by likhith"` (case-insensitive, matched with or without spaces, e.g. `"auramusic by likhith"`, `"auramusicbylikhith"`, `"AuraMusic By Likhith"`).
   - **Unlock Trigger:** On pressing Enter / Submit (or live exact match):
     - Sets `AppSettings.setAuraAiUnlocked(true)` persistently in SharedPreferences.
     - Triggers haptic feedback and displays a celebration dialog: `"✨ Aura AI Unlocked! Welcome to the inner circle."`
     - Immediately exposes the Top App Bar Sparkle Orb, the Home Highlights Carousel card, and AI Settings.
     - Includes a "Relock AI Access" toggle in developer/AI settings if the user ever wants to re-hide it.

2. **Deep Listening Behavior & Music Psychology Analysis:**
   - **Chronotype & Peak Hours:** Detects whether the user is a "Midnight Dreamer", "Morning Motivator", or "Afternoon Focuser" based on 24-hour and monthly hourly distribution.
   - **Obsession / Repeat Quotient:** Quantifies repeat habits (e.g., `"You listened to 'Starboy' 14 times today — 62% of your total listening!"`).
   - **Artist & Director Affinity:** Analyzes why the user gravitates towards specific vocalists (e.g. Arijit Singh) and music directors/composers (e.g. Pritam, A.R. Rahman, Metro Boomin) using `MusicDirectorClassifier`.
   - **Taste Roasting & Playful Critiques:** "Roast My Taste" mode delivers witty, hilarious, yet deeply accurate breakdowns of guilty pleasures, 1-song loops, and sudden genre shifts.
   - **Vibe Trajectory:** Tracks emotional shifts across days (e.g. high-energy gym beats at 7 AM fading into melancholy acoustic indie at 11 PM).

2. **Better Chat Bot Answer UI (Ultra-Modern & Glassmorphic):**
   - **Aura Orb Header:** Animated glowing gradient avatar orb indicating AI status (thinking, streaming, ready).
   - **Context Banner:** Sticky pill displaying the currently playing song with quick action chips (`"Analyze this song"`, `"Explain lyrics"`, `"Find matching tracks"`).
   - **Visual Stat Callouts:** In-bubble cards showing listening minutes, repeat count progress bars, and top artist badges.
   - **Typing Cursor & Smooth Markdown:** Real-time character streaming with glowing cursor (`▊`), rich typography, bullet points, and accent highlights.
   - **Prompt Discovery Carousel:** Quick pills like `🔮 Analyze My Taste`, `🔥 Roast Me`, `🌙 Midnight Mix`, `🔁 Why Do I Repeat Songs?`.

3. **Beautiful Songs Selection In-Chat:**
   - **Interactive Playable Song Cards (`AuraSongCard`):**
     - Full rounded album art with soft glow.
     - Track title, artist, duration, and AI reason tag (e.g. *"Matches your 11 PM melancholic mood"*).
     - **▶️ 1-Tap Play:** Instantly streams track via `QueueCoordinator`.
     - **➕ Add to Queue:** Adds song directly into active queue without stopping current track.
     - **❤️ Favorite:** Toggles like state.
   - **Curated Mix Carousel (`AuraMixCard`):**
     - When the AI generates a multi-song playlist (e.g., "5 Songs For Rainy Night"), renders an expandable horizontal card slider with a **"Play All as Mix"** button and **"Save to Library"** button.

4. **"With More Things" (Aura AI Superpowers):**
   - **Currently Playing Intelligence:** Full awareness of the active track, playback position, and upcoming queue.
   - **DJ Personas:** Switch between *Aura (Empathetic Guide)*, *Savage Critic (Roaster)*, *Musicologist (History & Composition)*, and *Night Owl DJ*.
   - **Dual Engine (Online Groq/Gemini + Offline Heuristics):**
     - Full Groq Cloud (Llama 3.3 70B) & Google Gemini 1.5/2.0 API streaming integration via Ktor with 4-key failover pool.
     - Built-in offline heuristic analyzer that parses local `ListeningStats` directly, allowing deep behavioral analysis even without internet or API key!
   - **Developer Attribution & Social Links:** Complete knowledge of creator **Kami Likhith** with 1-tap links to GitHub, LinkedIn, and Instagram.

5. **⚡ God-Level Feature 1: Aura Radar (Shazam Ambient Audio & Hum-to-Search):**
   - **Acoustic Audio Fingerprinting:** Captures a 4-second audio sample via microphone (`AudioRecord`), matches against global/regional databases (Telugu, Hindi, English, Punjabi, etc.) in ~1.5s.
   - **AI Humming Recognition:** Allows humming, whistling, or singing a melody to find the exact song.
   - **1-Tap Instant Stream:** Seamlessly queries `YtMusicRepository` and starts playback in AuraMusic, queues it, or asks Aura AI for similar vibe tracks.
   - **In-Chat & Top Bar Access:** Accessible via a pulsing radar button inside the Aura AI chat and the Search screen.

6. **⚡ God-Level Feature 2: AI Stem Separator & Live Karaoke Mode (ONNX Runtime):**
   - **Vocal & Instrumental Separation:** Uses on-device ONNX Runtime (`com.microsoft.onnxruntime:onnxruntime:1.20.0`) to split any playing song (streamed or local) into separate audio stems:
     - 🎤 **Vocals / Lead Singing**
     - 🥁 **Beat / Drums & Percussion**
     - 🎸 **Bass & Instrumental Backing Track**
   - **Real Karaoke Slider:** In `NowPlayingScreen`, users can slide vocals to 0% to sing over original studio instrumentals with live synced lyrics!
   - **Acapella Isolator:** Mute instruments to hear pristine, raw vocals.

7. **⚡ God-Level Feature 3: Aura Neural DJ (BPM & Beat-Matched Seamless Crossfade):**
   - **Harmonic & Tempo Alignment:** Analyzes BPM (tempo) and musical key (Camelot wheel) of the current track and next track.
   - **Beat-Drop Mixing:** Synchronizes the downbeats so the drop of the incoming song hits precisely on beat with the outgoing song—just like a world-class live DJ at Tomorrowland.
   - **Non-Stop Club Flow:** Eliminates awkward pauses or abrupt volume cuts between songs.

---

### Task 1: Behavior Metrics & Personality Extraction Engine (`AuraBehaviorEngine`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraBehaviorEngine.kt`
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraBehaviorModels.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraBehaviorEngineTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.DailyHighlightsSummary
import com.music.bitchord.data.stats.DailyPlayRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraBehaviorEngineTest {

    private fun mockSong(id: String, title: String, artist: String): Song =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null)

    @Test
    fun testChronotypeDetectionNightOwl() {
        // Peak hour 23 (11 PM)
        val highlights = DailyHighlightsSummary(
            topRepeatedSong = mockSong("s1", "Midnight City", "M83"),
            topRepeatedCount = 5,
            peakHour = 23,
            totalPlayedMs = 7_200_000L,
            totalPlays = 15,
        )
        val profile = AuraBehaviorEngine.analyzeDailyProfile(highlights)
        assertEquals(AuraChronotype.NIGHT_OWL, profile.chronotype)
        assertTrue(profile.personalityTags.contains("Late-Night Melophile"))
    }

    @Test
    fun testRepeatObsessionIndex() {
        val song = mockSong("s2", "Blinding Lights", "The Weeknd")
        val highlights = DailyHighlightsSummary(
            topRepeatedSong = song,
            topRepeatedCount = 12,
            totalPlays = 15,
            totalPlayedMs = 3_600_000L,
        )
        val profile = AuraBehaviorEngine.analyzeDailyProfile(highlights)
        // 12 / 15 plays = 80% repeat quotient
        assertTrue(profile.repeatQuotient > 0.7f)
        assertTrue(profile.personalityTags.contains("Obsessive Looper"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraBehaviorEngineTest"`
Expected: FAIL with compilation error (unresolved `AuraBehaviorEngine`).

**Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraBehaviorModels.kt`:
```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

enum class AuraChronotype(val title: String, val description: String, val emoji: String) {
    EARLY_BIRD("Early Bird", "Energized morning listener fueled by upbeat rhythms", "🌅"),
    AFTERNOON_FLOW("Daytime Focuser", "Productive daytime listener balancing flow and focus", "☀️"),
    EVENING_UNWIND("Evening Chiller", "Golden hour relaxer unwinding after a long day", "🌆"),
    NIGHT_OWL("Midnight Dreamer", "Deep nocturnal melophile lost in moody soundscapes", "🌙"),
    ALL_DAY("Continuous Vibe", "Non-stop music companion listening around the clock", "⚡"),
}

@Serializable
data class AuraUserBehaviorProfile(
    val chronotype: AuraChronotype,
    val repeatQuotient: Float, // 0.0 to 1.0 (proportion of plays spent on top repeated track)
    val totalMinutesToday: Long,
    val topRepeatedSongTitle: String?,
    val topRepeatedArtist: String?,
    val topRepeatedCount: Int,
    val topSinger: String?,
    val topMusicDirector: String?,
    val personalityTags: List<String>,
    val behavioralSummaryText: String,
    val roastSummaryText: String,
)
```

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraBehaviorEngine.kt`:
```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.stats.DailyHighlightsSummary

object AuraBehaviorEngine {

    fun analyzeDailyProfile(highlights: DailyHighlightsSummary): AuraUserBehaviorProfile {
        val peak = highlights.peakHour ?: 14
        val chronotype = when (peak) {
            in 5..10 -> AuraChronotype.EARLY_BIRD
            in 11..16 -> AuraChronotype.AFTERNOON_FLOW
            in 17..21 -> AuraChronotype.EVENING_UNWIND
            else -> AuraChronotype.NIGHT_OWL
        }

        val totalPlays = highlights.totalPlays.coerceAtLeast(1)
        val repeatQuotient = (highlights.topRepeatedCount.toFloat() / totalPlays).coerceIn(0f, 1f)

        val tags = mutableListOf<String>()
        if (chronotype == AuraChronotype.NIGHT_OWL) tags.add("Late-Night Melophile")
        if (chronotype == AuraChronotype.EARLY_BIRD) tags.add("Morning Riser")

        if (repeatQuotient >= 0.4f && highlights.topRepeatedCount >= 3) {
            tags.add("Obsessive Looper")
        } else if (repeatQuotient < 0.15f && totalPlays >= 8) {
            tags.add("Eclectic Explorer")
        }

        if (!highlights.topMusicDirector.isNullOrBlank()) {
            tags.add("Composer Connoisseur")
        }

        val summary = buildString {
            append("You've tuned in for ${highlights.totalMinutes} minutes today across ${highlights.totalTracksCount} tracks. ")
            if (highlights.topRepeatedSong != null && highlights.topRepeatedCount > 1) {
                append("Your ultimate comfort loop is '${highlights.topRepeatedSong.title}' (${highlights.topRepeatedCount} plays). ")
            }
            if (!highlights.topSinger.isNullOrBlank()) {
                append("Your lead vocal obsession is ${highlights.topSinger}. ")
            }
            if (!highlights.topMusicDirector.isNullOrBlank()) {
                append("Arranged under the sonic touch of ${highlights.topMusicDirector}.")
            }
        }

        val roast = buildString {
            if (repeatQuotient > 0.5f && highlights.topRepeatedSong != null) {
                append("Did you forget other songs exist? You played '${highlights.topRepeatedSong.title}' ${highlights.topRepeatedCount} times today. Are you going through something, or is your replay button stuck?")
            } else if (chronotype == AuraChronotype.NIGHT_OWL) {
                append("Listening to music past midnight again? Your sleep schedule is as broken as an unbuffered stream, but at least your soundtrack is immaculate.")
            } else {
                append("A respectable listening session! Though bouncing between genres this fast suggests your attention span might need its own EQ curve.")
            }
        }

        return AuraUserBehaviorProfile(
            chronotype = chronotype,
            repeatQuotient = repeatQuotient,
            totalMinutesToday = highlights.totalMinutes,
            topRepeatedSongTitle = highlights.topRepeatedSong?.title,
            topRepeatedArtist = highlights.topRepeatedSong?.artist,
            topRepeatedCount = highlights.topRepeatedCount,
            topSinger = highlights.topSinger,
            topMusicDirector = highlights.topMusicDirector,
            personalityTags = tags,
            behavioralSummaryText = summary,
            roastSummaryText = roast,
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraBehaviorEngineTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/ai/ shared/src/commonTest/kotlin/com/music/bitchord/data/ai/
git commit -m "feat(ai): add AuraBehaviorEngine for listening habit and personality profiling"
```

---

### Task 2: AI Chat Models & Structured Song Tag Parsing (`AuraChatModels`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraChatModels.kt`
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraSongTagParser.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraSongTagParserTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuraSongTagParserTest {

    @Test
    fun testExtractsSongTagsFromAiResponse() {
        val text = """
            Here are two songs tailored for your midnight mood:
            [SONG: Starboy | The Weeknd | Moody synth-heavy late night anthem]
            [SONG: Tum Se | Sachin-Jigar, Raghav Chaitanya | Soft acoustic romantic confession]
            Enjoy these tracks!
        """.trimIndent()

        val parsed = AuraSongTagParser.parse(text)
        assertEquals(2, parsed.recommendedSongs.size)
        assertEquals("Starboy", parsed.recommendedSongs[0].title)
        assertEquals("The Weeknd", parsed.recommendedSongs[0].artist)
        assertEquals("Moody synth-heavy late night anthem", parsed.recommendedSongs[0].reason)
        assertEquals("Tum Se", parsed.recommendedSongs[1].title)
        assertTrue(parsed.cleanText.contains("Here are two songs"))
        assertTrue(!parsed.cleanText.contains("[SONG:"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraSongTagParserTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraChatModels.kt`:
```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

enum class ChatSender {
    USER,
    AURA_AI,
    SYSTEM,
}

enum class AuraAiPersona(val displayName: String, val subtitle: String, val emoji: String) {
    COMPANION("Aura Companion", "Empathetic, intuitive music guide", "🔮"),
    SAVAGE_ROASTER("Savage Critic", "Witty, blunt roasts of your taste", "🔥"),
    MUSICOLOGIST("Musicologist", "Deep technical analysis & music history", "🎼"),
    NIGHT_DJ("Night DJ", "Hypnotic, late-night sonic curator", "🎧"),
}

@Serializable
data class AuraRecommendedSongRef(
    val title: String,
    val artist: String,
    val reason: String? = null,
    val videoId: String? = null,
)

data class AuraParsedAiMessage(
    val cleanText: String,
    val recommendedSongs: List<AuraRecommendedSongRef>,
)

data class AuraChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedSongs: List<Song> = emptyList(),
    val songReasons: Map<String, String> = emptyMap(),
    val statCard: AuraUserBehaviorProfile? = null,
    val isStreaming: Boolean = false,
)
```

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraSongTagParser.kt`:
```kotlin
package com.music.bitchord.data.ai

object AuraSongTagParser {
    // Matches: [SONG: Title | Artist | Reason/Vibe]
    private val SONG_REGEX = Regex("""\[SONG:\s*([^|\]]+)\s*\|\s*([^|\]]+)(?:\s*\|\s*([^\]]+))?\]""")

    fun parse(rawText: String): AuraParsedAiMessage {
        val songs = mutableListOf<AuraRecommendedSongRef>()
        val matches = SONG_REGEX.findAll(rawText)

        for (match in matches) {
            val title = match.groupValues[1].trim()
            val artist = match.groupValues[2].trim()
            val reason = match.groupValues.getOrNull(3)?.trim()?.ifEmpty { null }
            if (title.isNotEmpty() && artist.isNotEmpty()) {
                songs.add(AuraRecommendedSongRef(title = title, artist = artist, reason = reason))
            }
        }

        val clean = rawText.replace(SONG_REGEX, "").trim()
        return AuraParsedAiMessage(cleanText = clean, recommendedSongs = songs)
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraSongTagParserTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraChatModels.kt shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraSongTagParser.kt shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraSongTagParserTest.kt
git commit -m "feat(ai): add Aura chat models and song tag parser"
```

---

### Task 3: Offline Heuristic Music Intelligence Engine (`AuraOfflineEngine`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraOfflineEngine.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraOfflineEngineTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.DailyHighlightsSummary
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraOfflineEngineTest {

    @Test
    fun testGenerateOfflineRoast() {
        val highlights = DailyHighlightsSummary(
            topRepeatedSong = Song(videoId = "v1", title = "Despacito", artist = "Luis Fonsi", thumbnailUrl = null),
            topRepeatedCount = 10,
            totalPlays = 12,
            peakHour = 2,
            totalPlayedMs = 2_400_000L,
        )
        val profile = AuraBehaviorEngine.analyzeDailyProfile(highlights)
        val reply = AuraOfflineEngine.respond(
            userPrompt = "Roast my taste",
            profile = profile,
            persona = AuraAiPersona.SAVAGE_ROASTER,
            currentSong = null
        )
        assertNotNull(reply)
        assertTrue(reply.text.contains("Despacito") || reply.text.contains("sleep"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraOfflineEngineTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraOfflineEngine.kt`:
```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import java.util.UUID

object AuraOfflineEngine {

    fun respond(
        userPrompt: String,
        profile: AuraUserBehaviorProfile,
        persona: AuraAiPersona,
        currentSong: Song?,
    ): AuraChatMessage {
        val lower = userPrompt.lowercase()

        val responseText = when {
            "roast" in lower -> {
                buildString {
                    append("🔥 **Aura Taste Roast:**\n\n")
                    append(profile.roastSummaryText)
                    if (profile.topRepeatedCount > 3 && profile.topRepeatedSongTitle != null) {
                        append("\n\n*Stat Alert:* ${profile.topRepeatedCount} loops of '${profile.topRepeatedSongTitle}' by ${profile.topRepeatedArtist}. We're staging an intervention!")
                    }
                }
            }
            "personality" in lower || "habit" in lower || "behavior" in lower || "behaviour" in lower || "chronotype" in lower -> {
                buildString {
                    append("🔮 **Your Listening Persona: ${profile.chronotype.title} ${profile.chronotype.emoji}**\n\n")
                    append("${profile.chronotype.description}\n\n")
                    append("📊 **Key Insights:**\n")
                    append("• **Listening Time Today:** ${profile.totalMinutesToday} minutes\n")
                    if (profile.topRepeatedSongTitle != null) {
                        append("• **Loop Obsession:** '${profile.topRepeatedSongTitle}' (${profile.topRepeatedCount} plays)\n")
                    }
                    if (profile.topSinger != null) {
                        append("• **Favorite Lead Vocalist:** ${profile.topSinger}\n")
                    }
                    if (profile.topMusicDirector != null) {
                        append("• **Lead Composer:** ${profile.topMusicDirector}\n")
                    }
                    append("• **Aura Tags:** ${profile.personalityTags.joinToString(" · ") { "#$it" }}\n\n")
                    append(profile.behavioralSummaryText)
                }
            }
            "recommend" in lower || "mix" in lower || "suggest" in lower -> {
                buildString {
                    append("✨ Based on your **${profile.chronotype.title}** energy today, here are hand-picked tracks for your groove:\n\n")
                    if (profile.topRepeatedArtist != null) {
                        append("[SONG: Starboy | The Weeknd | Pulsing electronic late night confidence]\n")
                        append("[SONG: Tum Hi Ho | Arijit Singh | Deep emotional acoustic ballad]\n")
                        append("[SONG: Nightcall | Kavinsky | Cyberpunk neon nocturnal drive]\n")
                    } else {
                        append("[SONG: Blinding Lights | The Weeknd | High energy retro synthwave]\n")
                        append("[SONG: Kesariya | Pritam, Arijit Singh | Golden melodic euphoria]\n")
                    }
                    append("\nTap any song card to play or add directly to your queue!")
                }
            }
            currentSong != null && ("this song" in lower || "current" in lower || "meaning" in lower) -> {
                buildString {
                    append("🎶 **Analyzing Currently Playing Track:**\n\n")
                    append("**${currentSong.title}** by **${currentSong.artist}**\n\n")
                    append("This track seamlessly complements your current ${profile.chronotype.title.lowercase()} listening mood. It creates a smooth sonic transition with evocative vocals and tight rhythmic arrangement.")
                }
            }
            "developer" in lower || "creator" in lower || "who made" in lower || "likhith" in lower || "kami" in lower || "social" in lower || "github" in lower || "instagram" in lower || "linkedin" in lower -> {
                buildString {
                    append("👑 **About the Creator:**\n\n")
                    append("AuraMusic was designed and developed by **Kami Likhith**.\n\n")
                    append("He architected AuraMusic as the ultimate ad-free, high-fidelity music sanctuary—combining studio-grade audio processing, cloud party sync, and AI music intelligence.\n\n")
                    append("🌐 **Connect with Kami Likhith:**\n")
                    append("• 🐙 **GitHub:** [likhith3035](https://github.com/likhith3035)\n")
                    append("• 💼 **LinkedIn:** [likhith-kami](https://linkedin.com/in/likhith-kami)\n")
                    append("• 📸 **Instagram:** [@lucky__likhith](https://www.instagram.com/lucky__likhith)\n\n")
                    append("[ACTION: OPEN_URL:https://github.com/likhith3035 | GitHub]\n")
                    append("[ACTION: OPEN_URL:https://www.instagram.com/lucky__likhith | Instagram]\n")
                    append("[ACTION: OPEN_URL:https://linkedin.com/in/likhith-kami | LinkedIn]")
                }
            }
            "feature" in lower || "what can you do" in lower || "help" in lower || "equalizer" in lower || "eq" in lower || "party" in lower || "listen together" in lower || "sleep timer" in lower -> {
                buildString {
                    append("✨ **AuraMusic Superpowers:**\n\n")
                    append("• **🎛️ 10-Band Studio EQ:** Parametric curves, bass boost, and audiophile presets.\n")
                    append("• **📻 Listen Together:** Real-time cloud sync with live soundbites, chat, and voting.\n")
                    append("• **🎤 Synced Karaoke Lyrics:** Word-by-word karaoke, live translation & story cards.\n")
                    append("• **🌙 Smart Sleep Timer:** Gentle volume fadeout as you drift to sleep.\n")
                    append("• **📊 Replay 2026:** Comprehensive daily, monthly, and yearly habit analytics.\n")
                    append("• **🔮 Aura AI:** Behavioral analysis, habit roasts, and smart music curation!\n\n")
                    append("Tell me which feature you'd like to explore or ask for guidance!")
                }
            }
            else -> {
                buildString {
                    append("Hey! I'm **Aura**, your musical companion created by **Kami Likhith**. 🎵\n\n")
                    append("Today you've immersed yourself in **${profile.totalMinutesToday} minutes** of music. ")
                    if (profile.topRepeatedSongTitle != null) {
                        append("Your top track right now is '${profile.topRepeatedSongTitle}'. ")
                    }
                    append("\n\nYou can ask me to **analyze your listening habits**, **roast your taste**, **explain lyrics**, **curate an instant mix**, or **guide you through app features**!")
                }
            }
        }

        val parsed = AuraSongTagParser.parse(responseText)
        return AuraChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.AURA_AI,
            text = parsed.cleanText,
            timestamp = System.currentTimeMillis(),
            statCard = if ("personality" in lower || "behavior" in lower || "habit" in lower) profile else null,
        )
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraOfflineEngineTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraOfflineEngine.kt shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraOfflineEngineTest.kt
git commit -m "feat(ai): add offline heuristic response engine for instantaneous local music intelligence"
```

---

### Task 4: Gemini AI Service & InnerTube Song Resolver (`AuraAiService`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/GeminiApiClient.kt`
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraAiService.kt`
- Test: `shared/src/jvmTest/kotlin/com/music/bitchord/data/ai/AuraAiServiceTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuraAiServiceTest {
    @Test
    fun testBuildSystemPromptContainsBehaviorMetrics() {
        val prompt = AuraAiService.buildSystemPrompt(
            persona = AuraAiPersona.SAVAGE_ROASTER,
            totalMinutes = 120,
            topSong = "Sunflower - Post Malone",
            repeatCount = 8,
            topSinger = "Post Malone",
            topDirector = "Louis Bell",
            chronotype = "Night Owl",
            currentSong = "Rockstar"
        )
        assertTrue(prompt.contains("Savage Critic"))
        assertTrue(prompt.contains("Sunflower - Post Malone"))
        assertTrue(prompt.contains("[SONG: Title | Artist | Reason]"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:jvmTest --tests "com.music.bitchord.data.ai.AuraAiServiceTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/GeminiApiClient.kt`:
- Implements HTTP POST to `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={API_KEY}` via existing Ktor OkHttp client.
- Sends conversation history as contents (`parts: [{ text }]`).
- Parses JSON response using `kotlinx.serialization`.

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraAiService.kt`:
```kotlin
package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.SearchFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object AuraAiService {

    fun buildSystemPrompt(
        persona: AuraAiPersona,
        totalMinutes: Long,
        topSong: String?,
        repeatCount: Int,
        topSinger: String?,
        topDirector: String?,
        chronotype: String,
        currentSong: String?,
    ): String = """
        You are "Aura", the native AI companion, music psychologist, and app guide built inside AuraMusic.
        Developer & Creator: Kami Likhith.
        Active Persona: ${persona.displayName} (${persona.subtitle}).
        
        ABOUT THE CREATOR & AURAMUSIC:
        - AuraMusic was architected and developed by **Kami Likhith** as a premier, ad-free, high-fidelity music streaming sanctuary.
        - When asked about the developer, creator, socials, or contact, warmly, proudly, and accurately credit **Kami Likhith** and share his official links:
          • GitHub: https://github.com/likhith3035
          • LinkedIn: https://linkedin.com/in/likhith-kami
          • Instagram: https://www.instagram.com/lucky__likhith
        - Attach action chips so users can open his profiles directly:
          [ACTION: OPEN_URL:https://github.com/likhith3035 | GitHub]
          [ACTION: OPEN_URL:https://www.instagram.com/lucky__likhith | Instagram]
          [ACTION: OPEN_URL:https://linkedin.com/in/likhith-kami | LinkedIn]
        
        AURAMUSIC APP FEATURES & CAPABILITIES KNOWLEDGE:
        - **10-Band Studio EQ:** Parametric equalizer, bass boost, spatial curves, and audiophile sound tuning presets.
        - **Listen Together (Party Mode):** Ultra-low latency synced listening rooms on Render cloud, live party chat, floating emoji reactions, real-time soundboard audio clips, and democratic jukebox voting.
        - **Live Synced Karaoke Lyrics:** Word-by-word synced karaoke, live translation into any language, and Instagram/WhatsApp lyric story card generator.
        - **Smart Sleep Timer:** Sleep timer with gentle fadeout so audio softly dissipates without waking the listener.
        - **Replay 2026 Recap:** 1-day today highlights, monthly/yearly listening statistics, top singers, top music directors/composers, and shareable hologram story cards.
        - **Offline & Downloads:** High-res FLAC & Opus downloads, device storage audio browser, WebDAV cloud sync, and smart cache management.
        - **Design Aesthetics:** Liquid glassmorphism, Nothing OS dot-matrix mode, dynamic sleeve-derived theme palettes.
        
        IN-CHAT ACTION NAVIGATION:
        When users ask how to use or open a feature, you can attach an interactive navigation action chip:
        - `[ACTION: OPEN_EQ | Open Studio EQ]`
        - `[ACTION: OPEN_PARTY | Start Listen Together]`
        - `[ACTION: OPEN_SLEEP_TIMER | Set Sleep Timer]`
        - `[ACTION: OPEN_REPLAY | View Replay 2026]`
        
        USER BEHAVIORAL PROFILE TODAY:
        - Total Listening Time: $totalMinutes minutes
        - Chronotype / Peak Vibe: $chronotype
        - Top Repeated Song: ${topSong ?: "None yet"} ($repeatCount plays today)
        - Lead Vocalist Affinity: ${topSinger ?: "Diverse"}
        - Music Director / Producer: ${topDirector ?: "Various"}
        - Currently Playing Track: ${currentSong ?: "None"}
        
        INSTRUCTIONS:
        1. Always format song recommendations using this EXACT bracket syntax so the UI can embed rich, interactive playable song cards:
           [SONG: Title | Artist | 1-sentence vibe/reasoning]
        2. Answer questions about app features authoritatively, guiding users on how to make the most of AuraMusic.
        3. Match the tone of your active persona (${persona.displayName}).
        4. Keep responses concise, visually beautiful with Markdown bolding, bullet points, and emoji tags.
    """.trimIndent()

    suspend fun resolveSongs(refs: List<AuraRecommendedSongRef>): List<Pair<Song, String>> {
        val results = mutableListOf<Pair<Song, String>>()
        for (ref in refs) {
            val query = "${ref.title} ${ref.artist}"
            val searchResult = runCatching {
                YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()?.firstOrNull()?.song
            }.getOrNull()

            if (searchResult != null) {
                results.add(searchResult to (ref.reason ?: "Recommended by Aura AI"))
            } else {
                // Fallback lightweight song representation
                val fallbackSong = Song(
                    videoId = ref.videoId ?: "search:$query",
                    title = ref.title,
                    artist = ref.artist,
                    thumbnailUrl = null,
                )
                results.add(fallbackSong to (ref.reason ?: "Recommended by Aura AI"))
            }
        }
        return results
    }
}
```

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:jvmTest --tests "com.music.bitchord.data.ai.AuraAiServiceTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/ai/ shared/src/jvmTest/kotlin/com/music/bitchord/data/ai/
git commit -m "feat(ai): implement AuraAiService prompt builder and song tag resolver"
```

---

### Task 5: Developer Gate (`AuraAccessGate`), AppSettings & Grok Dual-Key Pool

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraAccessGate.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraAccessGateTest.kt`
- Modify: `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/AppUi.kt`
- Modify: `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt:400-415`

**Step 1: Write the failing test for `AuraAccessGate`**

```kotlin
package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuraAccessGateTest {

    @Test
    fun testUnlockPhraseWithSpaces() {
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("auramusic by likhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("AuraMusic By Likhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("  auramusic   by   likhith  "))
    }

    @Test
    fun testUnlockPhraseWithoutSpaces() {
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("auramusicbylikhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("AuraMusicByLikhith"))
    }

    @Test
    fun testNormalSearchQueryDoesNotUnlock() {
        assertFalse(AuraAccessGate.isAuraAiUnlockCode("lossless audio"))
        assertFalse(AuraAccessGate.isAuraAiUnlockCode("theme mode"))
        assertFalse(AuraAccessGate.isAuraAiUnlockCode(""))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraAccessGateTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**

Create `shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraAccessGate.kt`:
```kotlin
package com.music.bitchord.data.ai

object AuraAccessGate {
    private const val SECRET_KEY = "auramusicbylikhith"

    /**
     * Checks if the query matches the developer unlock secret,
     * ignoring whitespace and casing.
     */
    fun isAuraAiUnlockCode(query: String): Boolean {
        if (query.isBlank()) return false
        val normalized = query.lowercase().replace(Regex("\\s+"), "")
        return normalized == SECRET_KEY
    }
}
```

In `AppSettings.kt`, add:
- `val auraAiUnlocked = MutableStateFlow<Boolean>(false)`
- `val grokApiKey1 = MutableStateFlow<String>("")`
- `val grokApiKey2 = MutableStateFlow<String>("")`
- `val auraAiPersona = MutableStateFlow<AuraAiPersona>(AuraAiPersona.COMPANION)`
- `fun setAuraAiUnlocked(unlocked: Boolean)`
- `fun setGrokApiKey1(key: String)`
- `fun setGrokApiKey2(key: String)`
- `fun setAuraAiPersona(persona: AuraAiPersona)`

In `SettingsSheet.kt`, hook into `SearchField`:
```kotlin
var showAuraUnlockDialog by remember { mutableStateOf(false) }

SearchField(
    query = searchQuery,
    onQueryChange = { searchQuery = it },
    onSubmit = {
        if (AuraAccessGate.isAuraAiUnlockCode(searchQuery)) {
            AppSettings.setAuraAiUnlocked(true)
            showAuraUnlockDialog = true
        }
    },
    placeholder = stringResource(R.string.settings_search_hint),
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
)
```

**Step 4: Run tests and verify they pass**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.ai.AuraAccessGateTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/ai/AuraAccessGate.kt shared/src/commonTest/kotlin/com/music/bitchord/data/ai/AuraAccessGateTest.kt app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt
git commit -m "feat(ai): add AuraAccessGate secret unlock mechanism and Grok dual-key settings"
```

---

### Task 6: Premium In-Chat Song Cards & Mix Carousel UI (`AuraSongCard` & `AuraMixCard`)

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraSongCard.kt`
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraMixCard.kt`

**Step 1: Implement `AuraSongCard.kt`**
- Glassmorphic card surface with subtle outline gradient (`Color(0xFF8A2387)` to `Color(0xFFE94057)` at 20% opacity).
- 54.dp high-res album artwork with rounded 12.dp corners and Coil3 AsyncImage loader.
- Song title (semibold white) and artist name (subtle grey).
- Vibe reasoning tag badge in pill format (e.g. `✨ Late-night melancholic synth`).
- 1-tap **Play Button** (circle button with ▶️ icon): Calls `onPlaySong(song)`.
- 1-tap **Add to Queue Button** (➕ icon): Calls `onAddToQueue(song)`.
- Playing animation indicator: If `currentSong?.videoId == song.videoId && isPlaying`, displays animated glowing equalizer bars.

**Step 2: Implement `AuraMixCard.kt`**
- Horizontal scrollable row (`LazyRow`) of `AuraSongCard` items when multiple songs are recommended together.
- Header row with:
  - "Curated Aura Mix" badge + total song count (e.g. `5 songs`).
  - **"▶️ Play Mix"** button that enqueues the entire batch via `onPlayAll(songs)`.
  - **"💾 Save Playlist"** button that creates a user playlist titled `"Aura AI: [Mix Name]"`.

**Step 3: Commit**

```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraSongCard.kt sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraMixCard.kt
git commit -m "feat(ai): build interactive glassmorphic in-chat AuraSongCard and AuraMixCard components"
```

---

### Task 7: Better Chat Bot Answer UI (`AuraChatBubble`, `AuraStatCard`, `AuraChatScreen`)

**Files:**
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatBubble.kt`
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraStatCard.kt`
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatScreen.kt`

**Step 1: Implement `AuraStatCard.kt`**
- Interactive behavioral visualization card rendered directly inside the chat message when user asks about habits or personality.
- Displays:
  - Chronotype badge (`🌙 Midnight Dreamer` with glowing gradient).
  - Listening time circular/linear progress bar.
  - Top loop card (cover art + repeat count `🔁 14 plays`).
  - Composer affinity tag (`Arranged by Pritam`).
  - Personality tags (`#ObsessiveLooper`, `#LateNightMelophile`).

**Step 2: Implement `AuraChatBubble.kt`**
- User bubble: right-aligned, sleek obsidian/dark glass bubble with violet accent glow, text wrapped comfortably.
- AI bubble:
  - Header: Glowing Aura glyph + timestamp + Persona badge.
  - Formatted text rendering: handles bolding `**`, headers `#`, bullet points `•`, quotes, and italicized notes cleanly.
  - Streaming cursor animation: subtle blinking cursor `▊` when message is still streaming.
  - Integrated `AuraStatCard` (if message has behavioral data).
  - Integrated `AuraSongCard` list or `AuraMixCard` (for music recommendations).
  - Action footer: Quick "Copy text" and "Regenerate" icons.

**Step 3: Implement `AuraChatScreen.kt`**
- Full screen or bottom sheet overlay with translucent dark background and Haze blur.
- **Top Header Bar**:
  - Pulsing animated Aura Orb (gradient canvas shader with breathing animation).
  - Title: "Aura AI" + subtitle "Your Musical Companion & Psychologist".
  - Persona selector dropdown menu (Switch between *Aura*, *Savage Roaster*, *Musicologist*, *Night DJ*).
  - API Key / Settings button.
  - Close button (X) / Back arrow.
- **Sticky Context Banner**:
  - If a song is currently playing, a floating frosted glass pill shows: `🎶 Playing: [Song Title] - [Artist]` with quick prompt pills (`"Analyze Vibe"`, `"Explain Meaning"`, `"Find Similar"`).
- **Quick Prompts Horizontal Row**:
  - Scrollable chips: `🔮 Analyze My Taste`, `🔥 Roast Me`, `🌙 Midnight Mix`, `🔁 Why Do I Repeat Tracks?`, `👑 Real Top Singer`.
  - Tapping a chip immediately sends the message!
- **Chat Feed**:
  - `LazyColumn` with automatic smooth scroll to bottom on new message.
- **Input Field**:
  - Modern rounded pill with glowing gradient outline upon focus.
  - Placeholder: `"Ask about your habits, request a mix, or roast your taste..."`.
  - Send button with spring haptic animation.

**Step 4: Commit**

```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatBubble.kt sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraStatCard.kt sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/ai/AuraChatScreen.kt
git commit -m "feat(ai): build ultra-premium glassmorphic AuraChatScreen with markdown, stat cards, and prompt suggestions"
```

---

### Task 8: Navigation, Home Highlights Carousel, Top Bar & Now Playing Integration

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/home/AuraGreetingAndHighlights.kt`
- Modify: `app/src/main/java/com/music/bitchord/MainActivity.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/LibraryScreen.kt`

**Step 1: Add `AuraFeature.AURA_AI` to `AuraGreetingAndHighlights.kt`**
```kotlin
AURA_AI(
    title = "Aura AI Chat & Vibe",
    subtitle = "Deep listening analysis, taste roast & smart mixes",
    badge = "AI COPILOT",
    actionLabel = "Chat",
    icon = "🔮",
    gradientColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
)
```

**Step 2: Add Sparkle Aura AI Action Icon to Top App Bars**
- Add sparkle button (`✨` / `ic_sparkle`) in `HomeScreen` and `LibraryScreen` top bars alongside settings.
- Tapping triggers `onOpenAuraAi()`.

**Step 3: Wire in `MainActivity.kt`**
- Add state `var showAuraAi by rememberSaveable { mutableStateOf(false) }`.
- Handle `AuraFeature.AURA_AI -> showAuraAi = true`.
- In `MainActivity.kt`, render `AuraChatScreen` inside a modal animated overlay when `showAuraAi == true`.
- Wire playback actions:
  - `onPlaySong = { song -> playFrom(listOf(song), 0, QueueSource("Aura AI", PlaybackSourceType.AI_CHAT)) }`
  - `onPlayAll = { songs -> playFrom(songs, 0, QueueSource("Aura AI Mix", PlaybackSourceType.AI_CHAT)) }`
  - `onAddToQueue = { song -> player.queueCoordinator.enqueueNext(listOf(song)) }`

**Step 4: Commit**

```bash
git add sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/home/AuraGreetingAndHighlights.kt app/src/main/java/com/music/bitchord/MainActivity.kt sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/LibraryScreen.kt
git commit -m "feat(ai): integrate Aura AI entry points across Home carousel, top bars, and MainActivity modal overlay"
```

---

### Task 9: Desktop Multiplatform Integration & End-to-End Verification

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt`
- Test: `desktopApp/src/test/kotlin/com/music/bitchord/desktop/DesktopAuraAiTest.kt`

**Step 1: Add Desktop entry point**
- In `DesktopApp.kt`, add `AURA_AI` to desktop destination or add top-right sparkle action button in the desktop title bar.
- Render `AuraChatScreen` in a sleek desktop slide-over panel or centered dialog.
- Connect desktop playback to `desktopPlayer.playSongs(songs, 0)`.

**Step 2: Write End-to-End Test for Desktop & CommonMain**
- Create `DesktopAuraAiTest.kt` verifying:
  1. Offline heuristic returns valid behavior breakdown with correct minutes and top repeated track.
  2. Song tag parser correctly extracts songs and reason strings.
  3. Persona switching preserves state and updates conversation tone.

**Step 3: Run all unit tests**

Run: `.\gradlew.bat test` (or targeted module tests).
Expected: PASS.

**Step 4: Commit**

```bash
git add desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt desktopApp/src/test/kotlin/com/music/bitchord/desktop/DesktopAuraAiTest.kt
git commit -m "feat(ai): complete desktop multiplatform support and verify Aura AI end-to-end"
```

---

### Task 10: Aura Radar (Shazam Ambient Audio Fingerprinting & Hum-to-Search)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/radar/AuraRadarModels.kt`
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/data/radar/AuraRadarService.kt`
- Create: `app/src/main/java/com/music/bitchord/audio/radar/AndroidAudioRecorder.kt`
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/radar/AuraRadarOverlay.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/data/radar/AuraRadarServiceTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.data.radar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuraRadarServiceTest {
    @Test
    fun testParseRecognitionResult() {
        val mockJson = """
            {
               "track": {
                  "title": "Chuttamalle",
                  "subtitle": "Anirudh Ravichander, Shilpa Rao",
                  "key": "devara_chuttamalle",
                  "genres": { "primary": "Tollywood" }
               }
            }
        """.trimIndent()
        val result = AuraRadarService.parseRecognitionResponse(mockJson)
        assertNotNull(result)
        assertEquals("Chuttamalle", result.title)
        assertEquals("Anirudh Ravichander, Shilpa Rao", result.artist)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.radar.AuraRadarServiceTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**
- `AuraRadarModels.kt`: Data classes `RadarTrackResult(title, artist, album, score, durationMs)`.
- `AuraRadarService.kt`: Handles fingerprint generation and querying Shazam/ACRCloud endpoints.
- `AndroidAudioRecorder.kt`: Captures 4.0 seconds of 44.1kHz 16-bit mono PCM audio from microphone.
- `AuraRadarOverlay.kt`: Glassmorphic radar ripple animation with glowing pulsing concentric circles. Renders the resolved `AuraSongCard` upon match with 1-tap **▶ Play** and **➕ Queue** buttons.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.data.radar.AuraRadarServiceTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/data/radar/ app/src/main/java/com/music/bitchord/audio/radar/ sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/radar/
git commit -m "feat(radar): add Aura Radar Shazam acoustic fingerprinting and ambient recognition"
```

---

### Task 11: AI Audio Stem Separator & Live Karaoke Mode (ONNX Runtime)

**Files:**
- Create: `shared/src/jvmSharedMain/kotlin/com/music/bitchord/audio/stems/AuraStemSeparator.kt`
- Create: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/AuraKaraokeSheet.kt`
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/NowPlayingScreen.kt`
- Test: `shared/src/jvmTest/kotlin/com/music/bitchord/audio/stems/AuraStemSeparatorTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.audio.stems

import kotlin.test.Test
import kotlin.test.assertTrue

class AuraStemSeparatorTest {
    @Test
    fun testStemVolumeScaling() {
        val gains = AuraStemSeparator.computeGainModifiers(vocalPercent = 0.0f, instrumentalPercent = 1.0f)
        assertTrue(gains.vocalGain <= 0.01f)
        assertTrue(gains.instrumentalGain >= 0.99f)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:jvmTest --tests "com.music.bitchord.audio.stems.AuraStemSeparatorTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Connects to `com.microsoft.onnxruntime:onnxruntime` (already compiled into `:shared`).
- `AuraStemSeparator.kt`: Lightweight 2-stem model separating vocals from accompaniment.
- `AuraKaraokeSheet.kt`: Modern glassmorphic slider controls:
  - 🎤 **Vocals Volume:** 0% (Karaoke Studio Instrumental) to 100% (Original) to 150% (Acapella Boost).
  - 🥁 **Beat & Melody Volume:** 0% to 100%.
- Placed right inside `NowPlayingScreen` with live synced lyrics for the ultimate karaoke experience!

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:jvmTest --tests "com.music.bitchord.audio.stems.AuraStemSeparatorTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/jvmSharedMain/kotlin/com/music/bitchord/audio/stems/ sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/AuraKaraokeSheet.kt
git commit -m "feat(karaoke): implement on-device AI stem separator and live vocal remover via ONNX Runtime"
```

---

### Task 12: Aura Neural DJ (BPM & Harmonic Beat-Synced Drop Crossfader)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/playback/dj/AuraNeuralDjEngine.kt`
- Create: `shared/src/commonMain/kotlin/com/music/bitchord/playback/dj/CamelotWheel.kt`
- Modify: `app/src/main/java/com/music/bitchord/playback/QueueCoordinator.kt`
- Test: `shared/src/commonTest/kotlin/com/music/bitchord/playback/dj/AuraNeuralDjEngineTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.music.bitchord.playback.dj

import kotlin.test.Test
import kotlin.test.assertTrue

class AuraNeuralDjEngineTest {
    @Test
    fun testHarmonicCompatibilityCamelotWheel() {
        // 8A (A minor) matches 8A, 8B (C major), 7A (D minor), 9A (E minor)
        assertTrue(CamelotWheel.isHarmonicallyCompatible("8A", "8B"))
        assertTrue(CamelotWheel.isHarmonicallyCompatible("8A", "9A"))
    }

    @Test
    fun testComputeBpmRampTransition() {
        val transition = AuraNeuralDjEngine.calculateTransition(
            currentBpm = 120f,
            nextBpm = 124f,
            crossfadeDurationSeconds = 6
        )
        assertTrue(transition.isFeasible)
        assertTrue(transition.targetBpm == 124f)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.playback.dj.AuraNeuralDjEngineTest"`
Expected: FAIL.

**Step 3: Write minimal implementation**
- `CamelotWheel.kt`: Musical key compatibility logic (harmonic mixing).
- `AuraNeuralDjEngine.kt`: Aligns the downbeats and drop transitions of the queued tracks.
- `QueueCoordinator.kt`: Applies dynamic volume and tempo ramp at track transition boundary so music never cuts off or pauses.

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:allTests --tests "com.music.bitchord.playback.dj.AuraNeuralDjEngineTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/music/bitchord/playback/dj/ app/src/main/java/com/music/bitchord/playback/QueueCoordinator.kt
git commit -m "feat(dj): add Aura Neural DJ harmonic camelot mixing and beat-matched crossfade engine"
```

---

### Execution Handoff

Plan complete and saved to `docs/plans/2026-10-27-aura-ai-companion-and-behavior-analysis.md`. Two execution options:

1. **Subagent-Driven (this session)** - I dispatch fresh subagents per task, review between tasks, and maintain fast iteration.
2. **Parallel Session (separate)** - Open a new session with executing-plans for batch execution with checkpoints.

Which approach would you like to proceed with?

