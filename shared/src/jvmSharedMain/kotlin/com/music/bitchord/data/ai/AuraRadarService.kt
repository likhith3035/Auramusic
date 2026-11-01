package com.music.bitchord.data.ai

import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.delay

/**
 * Acoustic identification service for Aura Radar (Shazam / Hum-to-search).
 *
 * Pipeline:
 *   1. Search YouTube Music catalog directly for the raw query (lyrics / hum text).
 *   2. If no clear word match in the top result, ask Groq LLM to identify the song
 *      from the lyric fragment — covers transliteration, phonetic spelling, partial lyrics.
 *   3. Parse the AI response and search the catalog again with the clean title+artist.
 *   4. Score confidence heuristically and return up to 4 ranked candidates.
 *
 * Failure reasons are surfaced via AuraRadarFailureReason so the UI can show
 * specific, actionable messages instead of a generic "not found".
 */
object AuraRadarService {

    /**
     * Identifies a song from lyric fragments, hummed melody tags, or audio fingerprint query.
     *
     * Returns null only on unexpected exceptions — callers should check [AuraRadarResult.failureReason]
     * for structured NOT_FOUND cases.
     */
    suspend fun identifySong(
        query: String,
        simulatedDelay: Boolean = false,
    ): AuraRadarResult? {
        if (simulatedDelay) delay(400)

        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return null

        return try {
            resolveIdentification(cleanQuery)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveIdentification(cleanQuery: String): AuraRadarResult? {
        // Step 1: Direct catalog search
        val directSongs = searchSongCandidates(cleanQuery).toMutableList()
        var topMatch: Song? = directSongs.firstOrNull()
        val allMatchingSongs = directSongs.toMutableList()

        // Determine if the direct search produced a strong match
        val hasClearWordMatch = topMatch != null && cleanQuery.split(" ").any { word ->
            word.length > 3 && topMatch!!.title.contains(word, ignoreCase = true)
        }

        // Step 2: If direct search didn't clearly match, ask Groq AI to identify the song
        if (!hasClearWordMatch) {
            val aiIdentified = runCatching {
                GroqApiClient.chat(
                    apiKey = AuraAiConfig.PRIMARY_GROQ_KEY,
                    systemPrompt = buildLyricIdentifierSystemPrompt(),
                    userPrompt = "Identify song with lyric fragment: $cleanQuery",
                    model = AuraAiConfig.GROQ_MODEL,
                ).getOrNull() ?: GroqApiClient.chat(
                    apiKey = AuraAiConfig.BACKUP_GROQ_KEY,
                    systemPrompt = buildLyricIdentifierSystemPrompt(),
                    userPrompt = "Identify song with lyric fragment: $cleanQuery",
                    model = AuraAiConfig.GROQ_MODEL,
                ).getOrNull()
            }.getOrNull()

            if (!aiIdentified.isNullOrBlank()) {
                val cleanedQuery = extractCleanSongQuery(aiIdentified)
                if (cleanedQuery.isNotBlank()) {
                    val aiSongs = searchSongCandidates(cleanedQuery)
                    if (aiSongs.isNotEmpty()) {
                        topMatch = aiSongs.first()
                        // Put AI-identified songs at the front of the candidate list
                        allMatchingSongs.addAll(0, aiSongs)
                    }
                }
            }
        }

        val resolvedTop = topMatch ?: return null

        val distinctSongs = allMatchingSongs.distinctBy { it.videoId }.take(4)

        // Confidence scoring — heuristic based on word overlap with query
        val primaryConfidence = when {
            cleanQuery.lowercase() in resolvedTop.title.lowercase() -> 0.98f
            cleanQuery.split(" ").count { w ->
                w.length > 3 && resolvedTop.title.contains(w, ignoreCase = true)
            } >= 2 -> 0.95f
            cleanQuery.lowercase() in (resolvedTop.artist.lowercase()) -> 0.93f
            else -> 0.88f
        }

        val candidates = distinctSongs.mapIndexed { index, song ->
            val conf = when (index) {
                0 -> primaryConfidence
                1 -> (primaryConfidence - 0.10f).coerceAtLeast(0.80f)
                2 -> (primaryConfidence - 0.20f).coerceAtLeast(0.70f)
                else -> (primaryConfidence - 0.30f).coerceAtLeast(0.60f)
            }
            val reason = when (index) {
                0 -> if (cleanQuery.lowercase() in song.title.lowercase()) "Exact Match" else "Top Acoustic Match"
                1 -> "High-Probability Version"
                2 -> "Alternative Rendition"
                else -> "Melodic Correlation"
            }
            AuraRadarCandidate(song = song, confidence = conf, matchReason = reason)
        }

        return AuraRadarResult(
            song = resolvedTop,
            confidence = primaryConfidence,
            recognizedLyric = cleanQuery,
            acousticVibe = "Identified via Aura Acoustic Neural Engine",
            candidates = candidates,
        )
    }

    private fun buildLyricIdentifierSystemPrompt(): String =
        "You are an expert song identifier specializing in Telugu, Hindi, Tamil, Punjabi, and global music. " +
        "Given a lyric fragment (which may be phonetically spelled, transliterated, or partial), identify the exact song. " +
        "Output ONLY the format: Title by Artist — nothing else. No quotes, no markdown, no explanations. " +
        "Examples: Samajavaragamana by Sid Sriram | Blinding Lights by The Weeknd | Kesariya by Arijit Singh"

    private suspend fun searchSongCandidates(query: String): List<Song> {
        // Primary: search with Songs filter for clean results
        val songResults = runCatching {
            YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
        }.getOrNull()?.mapNotNull { res ->
            when (res) {
                is SearchResult.TopTrack -> res.song
                is SearchResult.Track -> res.song
                else -> null
            }
        }.orEmpty()

        if (songResults.isNotEmpty()) {
            return songResults.distinctBy { it.videoId }
        }

        // Fallback: broad catalog search if Songs filter returned nothing
        val allResults = runCatching {
            YtMusicRepository.search(query, SearchFilter.ALL).getOrNull()
        }.getOrNull()?.mapNotNull { res ->
            when (res) {
                is SearchResult.TopTrack -> res.song
                is SearchResult.Track -> res.song
                else -> null
            }
        }.orEmpty()

        return allResults.distinctBy { it.videoId }
    }

    private fun extractCleanSongQuery(raw: String): String {
        // Handle [SONG: Title | Artist] tag format if present
        val songTagRegex = Regex("""\[SONG:\s*([^|\]]+)(?:\|\s*([^|\]]+))?""")
        val tagMatch = songTagRegex.find(raw)
        if (tagMatch != null) {
            val title = tagMatch.groupValues.getOrNull(1)?.trim().orEmpty()
            val artist = tagMatch.groupValues.getOrNull(2)?.trim().orEmpty()
            return "$title $artist".trim()
        }

        // Take the first non-empty line, strip markdown and common prefixes
        val firstLine = raw.lines()
            .map { it.replace("**", "").replace("*", "").trim() }
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            ?: raw

        return firstLine
            .removePrefix("Song:")
            .removePrefix("Track:")
            .removePrefix("Title:")
            // "Title by Artist" → "Title Artist" for better search
            .replace(Regex("""(?i)\s+by\s+"""), " ")
            .replace(Regex("""\s*\|\s*"""), " ")
            .trim()
    }
}
