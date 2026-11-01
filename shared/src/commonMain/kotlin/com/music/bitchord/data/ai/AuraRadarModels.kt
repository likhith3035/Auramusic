package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song

/**
 * State machine for Aura Radar (Shazam acoustic listening).
 */
enum class AuraRadarState {
    IDLE,
    LISTENING,
    ANALYZING,
    FOUND,
    NOT_FOUND,
    ERROR
}

/**
 * Specific reason codes for NOT_FOUND / ERROR states, allowing the UI to show
 * a helpful message rather than a generic "not found" error.
 */
enum class AuraRadarFailureReason {
    PERMISSION_DENIED,       // Microphone permission not granted
    NO_AUDIO_CAPTURED,       // Recording returned empty bytes
    SILENCE_DETECTED,        // Whisper returned blank / "." — no lyrics heard
    NO_CATALOG_MATCH,        // Lyrics transcribed but no song found in catalog
    NETWORK_ERROR,           // API call failed
    UNKNOWN,
}

/**
 * Individual ranked candidate match with probability score.
 */
data class AuraRadarCandidate(
    val song: Song,
    val confidence: Float, // 0.0f to 1.0f (e.g. 0.98f -> 98%)
    val matchReason: String? = null,
)

/**
 * Result returned by Aura Radar when an acoustic match is identified.
 */
data class AuraRadarResult(
    val song: Song,
    val confidence: Float,
    val recognizedLyric: String? = null,
    val acousticVibe: String? = null,
    val candidates: List<AuraRadarCandidate> = emptyList(),
)
