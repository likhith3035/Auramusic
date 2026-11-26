package com.music.bitchord.data.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class AuraWeatherSolarTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun epochMs(isoLocal: String, offsetSeconds: Int): Long {
        return LocalDateTime.parse(isoLocal).toInstant(ZoneOffset.ofTotalSeconds(offsetSeconds)).toEpochMilli()
    }

    @Test
    fun parsesDailySunriseAndSunsetWithCityOffset() {
        val sampleJson = """
            {
              "utc_offset_seconds": 19800,
              "daily": {
                "time": ["2026-11-26", "2026-11-27"],
                "sunrise": ["2026-11-26T06:27", "2026-11-27T06:27"],
                "sunset": ["2026-11-26T17:39", "2026-11-27T17:39"]
              }
            }
        """.trimIndent()

        val root = json.parseToJsonElement(sampleJson).jsonObject
        val (offset, windows) = AuraWeatherRepository.parseDailySolarWindows(root)

        assertEquals(19800, offset)
        assertEquals(2, windows.size)
        assertEquals(epochMs("2026-11-26T06:27", 19800), windows[0].sunriseEpochMs)
        assertEquals(epochMs("2026-11-26T17:39", 19800), windows[0].sunsetEpochMs)
    }

    @Test
    fun winterSunriseAndSunsetPreserveTwilightEvenAfterApiIsDayFlips() {
        val ist = 19800
        val windows = listOf(
            AuraWeatherRepository.DailySolarWindow(
                sunriseEpochMs = epochMs("2026-11-26T06:27", ist),
                sunsetEpochMs = epochMs("2026-11-26T17:39", ist),
            ),
        )

        // Pre-sunrise dawn (06:00 while is_day is still 0) -> SUNRISE
        val (preDawnType, _) = AuraWeatherRepository.resolveAstronomicalCelestialState(
            nowEpochMs = epochMs("2026-11-26T06:00", ist),
            solarWindows = windows,
            utcOffsetSeconds = ist,
            fallbackIsDay = false,
        )
        assertEquals(WeatherVisualType.SUNRISE, preDawnType)

        // Midday clear sky (11:30) -> CLEAR_DAY
        val (dayType, dayFlag) = AuraWeatherRepository.resolveAstronomicalCelestialState(
            nowEpochMs = epochMs("2026-11-26T11:30", ist),
            solarWindows = windows,
            utcOffsetSeconds = ist,
            fallbackIsDay = true,
        )
        assertEquals(WeatherVisualType.CLEAR_DAY, dayType)
        assertEquals(true, dayFlag)

        // Post-sunset twilight (17:48, 9m after 17:39 sunset even with fallbackIsDay = false) -> SUNSET
        val (duskType, duskIsDay) = AuraWeatherRepository.resolveAstronomicalCelestialState(
            nowEpochMs = epochMs("2026-11-26T17:48", ist),
            solarWindows = windows,
            utcOffsetSeconds = ist,
            fallbackIsDay = false,
        )
        assertEquals(WeatherVisualType.SUNSET, duskType)
        assertEquals(false, duskIsDay)

        // Night well after sunset (18:30) -> CLEAR_NIGHT
        val (nightType, nightIsDay) = AuraWeatherRepository.resolveAstronomicalCelestialState(
            nowEpochMs = epochMs("2026-11-26T18:30", ist),
            solarWindows = windows,
            utcOffsetSeconds = ist,
            fallbackIsDay = false,
        )
        assertEquals(WeatherVisualType.CLEAR_NIGHT, nightType)
        assertEquals(false, nightIsDay)
    }

    @Test
    fun earlyWinterCitySunsetMatchesCityTimezone() {
        val tokyoOffset = 32400 // UTC+9
        val windows = listOf(
            AuraWeatherRepository.DailySolarWindow(
                sunriseEpochMs = epochMs("2026-11-26T06:27", tokyoOffset),
                sunsetEpochMs = epochMs("2026-11-26T16:29", tokyoOffset),
            ),
            AuraWeatherRepository.DailySolarWindow(
                sunriseEpochMs = epochMs("2026-11-27T06:28", tokyoOffset),
                sunsetEpochMs = epochMs("2026-11-27T16:29", tokyoOffset),
            ),
        )

        // 16:00 JST in Tokyo precedes 16:29 sunset -> SUNSET
        val mapped = AuraWeatherRepository.mapWmoCode(
            code = 1,
            fallbackIsDay = true,
            nowEpochMs = epochMs("2026-11-26T16:00", tokyoOffset),
            solarWindows = windows,
            utcOffsetSeconds = tokyoOffset,
        )
        assertEquals(WeatherVisualType.SUNSET, mapped.visualType)
        assertEquals("Golden Hour", mapped.label)

        // Rainy weather (WMO 61) remains RAIN even during sunset window
        val rainyDuringSunset = AuraWeatherRepository.mapWmoCode(
            code = 61,
            fallbackIsDay = true,
            nowEpochMs = epochMs("2026-11-26T16:00", tokyoOffset),
            solarWindows = windows,
            utcOffsetSeconds = tokyoOffset,
        )
        assertEquals(WeatherVisualType.RAIN, rainyDuringSunset.visualType)
        assertEquals("Rainy", rainyDuringSunset.label)
    }
}
