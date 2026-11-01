package com.music.bitchord.data.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI Audio Engine managing real-time stem levels (vocals vs instrumental) and pitch shifting.
 */
object AuraStemEngine {
    private val _stemState = MutableStateFlow(AuraStemState())
    val stemState: StateFlow<AuraStemState> = _stemState.asStateFlow()

    fun setVocalVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _stemState.value = _stemState.value.copy(
            vocalVolume = clamped,
            karaokeModeEnabled = clamped < 0.2f,
        )
    }

    fun setInstrumentalVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _stemState.value = _stemState.value.copy(instrumentalVolume = clamped)
    }

    fun setPitchShift(semitones: Int) {
        val clamped = semitones.coerceIn(-6, 6)
        _stemState.value = _stemState.value.copy(pitchShiftSemitones = clamped)
    }

    fun toggleKaraokeMode() {
        val currentlyKaraoke = _stemState.value.karaokeModeEnabled
        if (currentlyKaraoke) {
            // Restore normal balance
            _stemState.value = _stemState.value.copy(
                vocalVolume = 1.0f,
                instrumentalVolume = 1.0f,
                karaokeModeEnabled = false,
            )
        } else {
            // Cut vocals for karaoke sing-along
            _stemState.value = _stemState.value.copy(
                vocalVolume = 0.05f,
                instrumentalVolume = 1.0f,
                karaokeModeEnabled = true,
            )
        }
    }

    fun toggleAcapellaMode() {
        val isAcapella = _stemState.value.instrumentalVolume < 0.2f
        if (isAcapella) {
            _stemState.value = _stemState.value.copy(
                vocalVolume = 1.0f,
                instrumentalVolume = 1.0f,
            )
        } else {
            _stemState.value = _stemState.value.copy(
                vocalVolume = 1.0f,
                instrumentalVolume = 0.05f,
            )
        }
    }

    fun resetStems() {
        _stemState.value = AuraStemState()
    }
}
