package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable

/**
 * Message sender type.
 */
enum class ChatSender {
    USER,
    AURA_AI,
    SYSTEM,
}

/**
 * Customizable DJ / AI personalities for Aura.
 */
enum class AuraAiPersona(val displayName: String, val subtitle: String, val emoji: String) {
    COMPANION("Aura Companion", "Empathetic, intuitive music guide", "🔮"),
    SAVAGE_ROASTER("Savage Critic", "Witty, blunt roasts of your taste", "🔥"),
    MUSICOLOGIST("Musicologist", "Deep technical analysis & composition", "🎼"),
    NIGHT_DJ("Night DJ", "Hypnotic, late-night sonic curator", "🎧"),
}

/**
 * Action type parsed from AI responses to trigger native app actions or URLs.
 */
@Serializable
data class AuraActionRef(
    val type: String, // e.g. "OPEN_EQ", "OPEN_PARTY", "OPEN_SLEEP_TIMER", "OPEN_REPLAY", "OPEN_URL"
    val label: String,
    val payload: String? = null, // e.g. "https://github.com/likhith3035"
)

/**
 * Structured song tag parsed from AI responses.
 */
@Serializable
data class AuraRecommendedSongRef(
    val title: String,
    val artist: String,
    val reason: String? = null,
    val videoId: String? = null,
)

/**
 * Result of parsing raw AI text into user-facing markdown and structured components.
 */
data class AuraParsedAiMessage(
    val cleanText: String,
    val recommendedSongs: List<AuraRecommendedSongRef>,
    val actionChips: List<AuraActionRef> = emptyList(),
)

/**
 * In-memory chat message displayed in the AuraChatScreen feed.
 */
data class AuraChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedSongs: List<Song> = emptyList(),
    val songReasons: Map<String, String> = emptyMap(),
    val actionChips: List<AuraActionRef> = emptyList(),
    val statCard: AuraUserBehaviorProfile? = null,
    val isStreaming: Boolean = false,
)
