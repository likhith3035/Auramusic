package com.music.bitchord.data.ai

import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID

object AuraAiService {

    fun buildSystemPrompt(
        persona: AuraAiPersona,
        profile: AuraUserBehaviorProfile,
        currentSong: Song?,
    ): String = """
        You are "Aura", the native music companion, acoustic psychologist, and app guide inside AuraMusic.
        Active Persona: ${persona.displayName} (${persona.subtitle}).
        
        CRITICAL RULES ON CREATOR & SOCIAL LINKS:
        - Do NOT introduce yourself as "created by Kami Likhith" or repeat his links on every turn!
        - For standard music queries, song recommendations, lyrics, taste roasting, and habits: Answer directly, naturally, and immediately. NEVER mention who created the app or attach social links unless specifically asked.
        - ONLY when the user explicitly asks "who created this app?", "who is the developer?", "who built you?", or asks for contact/socials, warmly and proudly credit **Kami Likhith** and attach his links:
          • GitHub: https://github.com/likhith3035
          • LinkedIn: https://linkedin.com/in/likhith-kami
          • Instagram: https://www.instagram.com/lucky__likhith
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
        When users ask how to use or open a feature, attach an interactive navigation action chip:
        - `[ACTION: OPEN_EQ | Open Studio EQ]`
        - `[ACTION: OPEN_PARTY | Start Listen Together]`
        - `[ACTION: OPEN_SLEEP_TIMER | Set Sleep Timer]`
        - `[ACTION: OPEN_REPLAY | View Replay 2026]`
        
        USER BEHAVIORAL PROFILE TODAY:
        - Total Listening Time: ${profile.totalMinutesToday} minutes (${profile.totalTracksToday} tracks)
        - Chronotype / Peak Vibe: ${profile.chronotype.title} (${profile.chronotype.description})
        - Top Repeated Song: ${profile.topRepeatedSongTitle ?: "None yet"} (${profile.topRepeatedCount} plays today)
        - Lead Vocalist Affinity: ${profile.topSinger ?: "Diverse"} (${profile.topSingerPlays} plays)
        - Music Director / Composer: ${profile.topMusicDirector ?: "Various"}
        - Currently Playing Track: ${currentSong?.let { "'${it.title}' by ${it.artist}" } ?: "None"}
        
        INSTRUCTIONS & TASTEMAKER TONE:
        1. You are Aura: a legendary music insider, sound engineer, and acoustic companion. Speak with stylish confidence, witty charm, and passion for music. NEVER sound like a corporate AI chatbot.
        2. STRICT ZERO-FILLER BAN: NEVER begin responses with robotic pleasantries like "Sure thing!", "Certainly!", "Hello!", "As an AI...", or "Here is a breakdown:". Dive straight into the musical answer.
        3. STRICT NO-TABLE BAN: NEVER generate markdown tables (`|`), ASCII grids, or HTML tables. They completely break the mobile interface.
        4. SONG DELIVERY FORMAT: When recommending tracks or identifying lyrics, write 1-2 punchy sentences setting the mood, and then output songs in bracket syntax. NEVER repeat the song title and artist in bullet points in your text paragraph—the tag automatically renders an interactive, playable card with album art and play/queue buttons:
           [SONG: Title | Artist | Match% / 1-sentence vibe]
        5. MULTI-CANDIDATE LYRIC MATCHING: If the user types lyric words (especially Telugu Tollywood, Hindi Bollywood, Punjabi, or Global hits), immediately identify the exact song, movie/album, composer, and singers in 1 sharp sentence. Then recommend the primary track (e.g. 98% Match) and 1–2 related probable versions (e.g. 88% Match for Reprise/Unplugged/Lofi):
           [SONG: Samajavaragamana | Sid Sriram, Thaman S | 98% Match · Original Soundtrack]
           [SONG: Samajavaragamana (Female Version) | Shreya Ghoshal | 87% Match · Female Reprise]
        6. LENGTH CONSTRAINT: Keep responses tight, punchy, and visually clean (max 2 to 3 concise paragraphs).
    """.trimIndent()

    suspend fun sendMessage(
        userPrompt: String,
        profile: AuraUserBehaviorProfile,
        persona: AuraAiPersona,
        currentSong: Song?,
        history: List<Pair<String, String>> = emptyList(),
        customGrokKey: String? = null,
        customGeminiKey: String? = null,
    ): AuraChatMessage {
        val systemPrompt = buildSystemPrompt(persona, profile, currentSong)

        // Multi-tier key pool failover
        var responseText: String? = null

        // 1. If custom key is xAI, try official xAI API client
        if (responseText == null && customGrokKey?.startsWith("xai-") == true) {
            val result = XAiApiClient.chat(
                apiKey = customGrokKey,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 2. Try Primary Groq Key
        val grokKey1 = customGrokKey?.takeIf { it.isNotBlank() && !it.startsWith("xai-") } ?: AuraAiConfig.PRIMARY_GROQ_KEY
        if (responseText == null && grokKey1.isNotBlank()) {
            val result = GroqApiClient.chat(
                apiKey = grokKey1,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 3. Try Primary Gemini Key
        val geminiKey1 = customGeminiKey?.takeIf { it.isNotBlank() } ?: AuraAiConfig.PRIMARY_GEMINI_KEY
        if (responseText == null && geminiKey1.isNotBlank()) {
            val result = GeminiApiClient.chat(
                apiKey = geminiKey1,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 4. Try Backup Groq Key
        if (responseText == null && AuraAiConfig.BACKUP_GROQ_KEY.isNotBlank()) {
            val result = GroqApiClient.chat(
                apiKey = AuraAiConfig.BACKUP_GROQ_KEY,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 5. Try Backup Gemini Key
        if (responseText == null && AuraAiConfig.BACKUP_GEMINI_KEY.isNotBlank()) {
            val result = GeminiApiClient.chat(
                apiKey = AuraAiConfig.BACKUP_GEMINI_KEY,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 6. Try configured xAI Key
        if (responseText == null && AuraAiConfig.PRIMARY_XAI_KEY.isNotBlank()) {
            val result = XAiApiClient.chat(
                apiKey = AuraAiConfig.PRIMARY_XAI_KEY,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                history = history,
            )
            responseText = result.getOrNull()
        }

        // 7. Fallback to Local Offline Heuristic Engine if all online calls fail or offline
        if (responseText == null) {
            val offlineMsg = AuraOfflineEngine.respond(userPrompt, profile, persona, currentSong)
            val resolved = resolveSongs(AuraSongTagParser.parse(offlineMsg.text).recommendedSongs)
            return offlineMsg.copy(
                resolvedSongs = resolved.map { it.first },
                songReasons = resolved.associate { it.first.videoId to it.second },
            )
        }

        // Parse online response
        val parsed = AuraSongTagParser.parse(responseText)
        val resolvedSongs = resolveSongs(parsed.recommendedSongs)

        val isStatQuery = listOf("personality", "habit", "behavior", "behaviour", "stats", "chronotype")
            .any { it in userPrompt.lowercase() }

        return AuraChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.AURA_AI,
            text = parsed.cleanText,
            timestamp = System.currentTimeMillis(),
            resolvedSongs = resolvedSongs.map { it.first },
            songReasons = resolvedSongs.associate { it.first.videoId to it.second },
            actionChips = parsed.actionChips,
            statCard = if (isStatQuery) profile else null,
        )
    }

    suspend fun findSongOnline(title: String, artist: String): Song? {
        val fullQuery = "$title $artist".trim()
        val extractSong: (SearchResult) -> Song? = { res ->
            when (res) {
                is SearchResult.TopTrack -> res.song
                is SearchResult.Track -> res.song
                else -> null
            }
        }

        // 1. Full title + artist with SONGS filter
        var match = runCatching {
            YtMusicRepository.search(fullQuery, SearchFilter.SONGS).getOrNull()?.firstNotNullOfOrNull(extractSong)
        }.getOrNull()

        // 2. Fallback: Search with title only
        if (match == null && title.isNotBlank()) {
            match = runCatching {
                YtMusicRepository.search(title, SearchFilter.SONGS).getOrNull()?.firstNotNullOfOrNull(extractSong)
            }.getOrNull()
        }

        // 3. Fallback: Search fullQuery with ALL filter
        if (match == null && fullQuery.isNotBlank()) {
            match = runCatching {
                YtMusicRepository.search(fullQuery, SearchFilter.ALL).getOrNull()?.firstNotNullOfOrNull(extractSong)
            }.getOrNull()
        }

        return match
    }

    suspend fun resolveSingleSong(song: Song): Song {
        if (!song.videoId.startsWith("search:") && song.videoId.isNotBlank()) {
            return song
        }
        val found = findSongOnline(song.title, song.artist)
        return found ?: song
    }

    suspend fun resolveSongs(refs: List<AuraRecommendedSongRef>): List<Pair<Song, String>> = coroutineScope {
        refs.take(6).map { ref ->
            async {
                val foundSong = findSongOnline(ref.title, ref.artist)
                val resolved = foundSong ?: Song(
                    videoId = ref.videoId ?: "search:${ref.title} ${ref.artist}".trim(),
                    title = ref.title,
                    artist = ref.artist,
                    thumbnailUrl = null,
                )
                resolved to (ref.reason ?: "Curated for you by Aura")
            }
        }.awaitAll()
    }

    /**
     * Transcribes microphone audio sample into lyrics/text using Groq Whisper.
     */
    suspend fun transcribeAudio(wavBytes: ByteArray): String? {
        val res = GroqApiClient.transcribeAudio(
            apiKey = AuraAiConfig.PRIMARY_GROQ_KEY,
            wavBytes = wavBytes,
        ).getOrNull() ?: GroqApiClient.transcribeAudio(
            apiKey = AuraAiConfig.BACKUP_GROQ_KEY,
            wavBytes = wavBytes,
        ).getOrNull()
        return res?.trim()?.takeIf { it.isNotBlank() && it != "." }
    }
}
