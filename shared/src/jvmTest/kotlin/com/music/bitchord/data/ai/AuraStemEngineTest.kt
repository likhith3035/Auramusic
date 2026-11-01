package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuraStemEngineTest {

    @Test
    fun testKaraokeToggle() {
        AuraStemEngine.resetStems()
        assertFalse(AuraStemEngine.stemState.value.karaokeModeEnabled)

        AuraStemEngine.toggleKaraokeMode()
        assertTrue(AuraStemEngine.stemState.value.karaokeModeEnabled)
        assertTrue(AuraStemEngine.stemState.value.vocalVolume < 0.2f)

        AuraStemEngine.toggleKaraokeMode()
        assertFalse(AuraStemEngine.stemState.value.karaokeModeEnabled)
        assertEquals(1.0f, AuraStemEngine.stemState.value.vocalVolume)
    }

    @Test
    fun testPitchShift() {
        AuraStemEngine.resetStems()
        AuraStemEngine.setPitchShift(3)
        assertEquals(3, AuraStemEngine.stemState.value.pitchShiftSemitones)

        // Clamping to -6..6
        AuraStemEngine.setPitchShift(10)
        assertEquals(6, AuraStemEngine.stemState.value.pitchShiftSemitones)

        AuraStemEngine.setPitchShift(-12)
        assertEquals(-6, AuraStemEngine.stemState.value.pitchShiftSemitones)
    }
}
