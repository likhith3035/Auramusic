package com.music.bitchord.data.ai

/**
 * Parses structured tags emitted by Groq/Gemini/Offline engines:
 * - Song cards: `[SONG: Title | Artist | Vibe/Reasoning]`
 * - Navigation/Action chips: `[ACTION: TYPE:PAYLOAD | Label]` or `[ACTION: TYPE | Label]`
 */
object AuraSongTagParser {
    private val SONG_REGEX = Regex("""\[SONG:\s*([^|\]]+)\s*\|\s*([^|\]]+)(?:\s*\|\s*([^\]]+))?\]""", RegexOption.IGNORE_CASE)
    private val ACTION_REGEX = Regex("""\[ACTION:\s*([^|\]]+)\s*\|\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)

    fun parse(rawText: String): AuraParsedAiMessage {
        val songs = mutableListOf<AuraRecommendedSongRef>()
        val actions = mutableListOf<AuraActionRef>()

        // 1. Extract songs
        for (match in SONG_REGEX.findAll(rawText)) {
            val title = match.groupValues[1].trim()
            val artist = match.groupValues[2].trim()
            val reason = match.groupValues.getOrNull(3)?.trim()?.ifEmpty { null }
            if (title.isNotEmpty() && artist.isNotEmpty()) {
                songs.add(AuraRecommendedSongRef(title = title, artist = artist, reason = reason))
            }
        }

        // 2. Extract actions
        for (match in ACTION_REGEX.findAll(rawText)) {
            val rawType = match.groupValues[1].trim()
            val label = match.groupValues[2].trim()
            val parts = rawType.split(":", limit = 2)
            val type = parts[0].trim().uppercase()
            val payload = parts.getOrNull(1)?.trim()
            if (type.isNotEmpty() && label.isNotEmpty()) {
                actions.add(AuraActionRef(type = type, label = label, payload = payload))
            }
        }

        // 3. Clean user-facing text
        var clean = rawText
            .replace(SONG_REGEX, "")
            .replace(ACTION_REGEX, "")

        // Clean out orphan bullet markers or numbers left behind after tag removal
        clean = clean.lines()
            .filterNot { line -> line.trim().matches(Regex("""^([\-*•\d.]+|\d+\))\s*$""")) }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

        return AuraParsedAiMessage(
            cleanText = clean,
            recommendedSongs = songs,
            actionChips = actions,
        )
    }
}
