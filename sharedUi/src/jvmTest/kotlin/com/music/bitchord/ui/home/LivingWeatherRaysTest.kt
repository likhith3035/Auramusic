package com.music.bitchord.ui.home

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LivingWeatherRaysTest {

    @Test
    fun volumetricRayStopsAreStrictlyOrderedAndFeatheredToZero() {
        val shafts = listOf(
            VolumetricRayShaft(0.58f, 0.036f, Color(0xFFFFF7C2), 0.22f),
            VolumetricRayShaft(0.49f, 0.048f, Color(0xFFFDE68A), 0.18f),
            VolumetricRayShaft(0.74f, 0.042f, Color(0xFFFDBA74), 0.14f),
        )
        val stops = buildVolumetricRayStops(shafts)
        assertEquals(0f, stops.first().first)
        assertEquals(0f, stops.first().second.alpha)
        assertEquals(1f, stops.last().first)
        assertEquals(0f, stops.last().second.alpha)

        for (i in 1 until stops.size) {
            assertTrue(
                stops[i].first >= stops[i - 1].first,
                "Sweep gradient stop at index $i (${stops[i].first}) must not precede ${stops[i - 1].first}",
            )
        }
        assertTrue(stops.any { it.second.alpha > 0.15f })
    }

    @Test
    fun overlappingShaftsAreClampedMonotonicallyWithoutFailure() {
        val overlapping = listOf(
            VolumetricRayShaft(0.50f, 0.08f, Color(0xFFFDE047), 0.20f),
            VolumetricRayShaft(0.52f, 0.08f, Color(0xFFFB923C), 0.18f),
        )
        val stops = buildVolumetricRayStops(overlapping)
        assertTrue(stops.size >= 2)
        for (i in 1 until stops.size) {
            assertTrue(stops[i].first >= stops[i - 1].first)
        }
    }
}
