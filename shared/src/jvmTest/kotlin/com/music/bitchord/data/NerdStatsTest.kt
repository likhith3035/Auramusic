package com.music.bitchord.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NerdStatsTest {

    @Test
    fun bitPerfectTelemetryFormatting() {
        val t1 = NerdStats.BitPerfectTelemetry(
            active = true,
            sampleRateHz = 192000,
            bitDepth = 24,
            deviceName = "FiiO KA13",
        )
        assertEquals("Bit-Perfect 192kHz · 24-bit", t1.displayLabel)

        val t2 = NerdStats.BitPerfectTelemetry(
            active = true,
            sampleRateHz = 96000,
            bitDepth = 0,
        )
        assertEquals("Bit-Perfect 96kHz", t2.displayLabel)

        val t3 = NerdStats.BitPerfectTelemetry(
            active = true,
            sampleRateHz = 0,
            bitDepth = 0,
        )
        assertEquals("Bit-Perfect", t3.displayLabel)
    }

    @Test
    fun bitPerfectStateFlowUpdates() {
        NerdStats.bitPerfect.value = null
        assertNull(NerdStats.bitPerfect.value)

        val telemetry = NerdStats.BitPerfectTelemetry(
            active = true,
            sampleRateHz = 44100,
            bitDepth = 16,
            deviceName = "USB Audio DAC",
        )
        NerdStats.bitPerfect.value = telemetry
        assertEquals(telemetry, NerdStats.bitPerfect.value)
        assertEquals("Bit-Perfect 44kHz · 16-bit", NerdStats.bitPerfect.value?.displayLabel)

        NerdStats.bitPerfect.value = null
        assertNull(NerdStats.bitPerfect.value)
    }
}

