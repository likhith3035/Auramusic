package com.music.bitchord.data.ai

/**
 * State of AI stem separator and live karaoke audio parameters.
 */
data class AuraStemState(
    val vocalVolume: Float = 1.0f, // 0.0 (mute vocal) to 1.0 (full)
    val instrumentalVolume: Float = 1.0f, // 0.0 (mute instrumental) to 1.0 (full)
    val pitchShiftSemitones: Int = 0, // -6 to +6 semitones
    val karaokeModeEnabled: Boolean = false,
    val micMonitorEnabled: Boolean = false,
)
