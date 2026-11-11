package com.music.bitchord.data.weather

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder
import java.util.Calendar
import kotlin.math.roundToInt

enum class WeatherVisualType {
    CLEAR_DAY,
    CLEAR_NIGHT,
    RAIN,
    THUNDERSTORM,
    CLOUDY,
    SNOW,
}

data class AuraLocationResult(
    val name: String,
    val country: String?,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double,
) {
    val displaySubtitle: String
        get() = listOfNotNull(admin1, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class AuraWeatherInfo(
    val temperatureC: Int,
    val conditionLabel: String,
    val visualType: WeatherVisualType,
    val city: String?,
    val country: String? = null,
    val isDay: Boolean,
    val moodPills: List<String>,
    val humidity: Int? = null,
    val windSpeedKmH: Double? = null,
    val isCustomLocation: Boolean = false,
    val isFahrenheit: Boolean = false,
) {
    val temperatureF: Int
        get() = ((temperatureC * 9.0 / 5.0) + 32.0).roundToInt()

    val displayTemperature: String
        get() = if (isFahrenheit) "$temperatureF°F" else "$temperatureC°C"
}

/**
 * Lightweight, zero-permission, real-time contextual weather and geocoding repository.
 * Powered by Open-Meteo (100% free, zero API keys required).
 * Supports automatic coarse IP location, secondary failover IP resolution,
 * instant worldwide city search, and temperature unit switching (°C / °F).
 */
object AuraWeatherRepository {

    private const val CACHE_DURATION_MS = 45 * 60 * 1000L // 45 minutes
    private var lastFetchTimeMs = 0L

    private val json = Json { ignoreUnknownKeys = true }

    private var isFahrenheitPreference = false
    private var customLocation: AuraLocationResult? = null

    private val _currentWeather = MutableStateFlow(defaultFallbackWeather())
    val currentWeather: StateFlow<AuraWeatherInfo> = _currentWeather.asStateFlow()

    private var cachedLat: Double? = null
    private var cachedLon: Double? = null
    private var cachedCity: String? = null
    private var cachedCountry: String? = null

    fun isCustomLocation(): Boolean = customLocation != null

    fun currentCustomLocation(): AuraLocationResult? = customLocation

    fun setTemperatureUnit(useFahrenheit: Boolean) {
        isFahrenheitPreference = useFahrenheit
        val curr = _currentWeather.value
        _currentWeather.value = curr.copy(isFahrenheit = useFahrenheit)
    }

    fun toggleTemperatureUnit() {
        setTemperatureUnit(!isFahrenheitPreference)
    }

    suspend fun setCustomLocation(location: AuraLocationResult?) {
        customLocation = location
        lastFetchTimeMs = 0L // invalidate cache
        refresh(force = true)
    }

    suspend fun clearCustomLocation() {
        customLocation = null
        lastFetchTimeMs = 0L // invalidate cache
        refresh(force = true)
    }

    /**
     * Search cities and towns worldwide using Open-Meteo's keyless Geocoding API.
     */
    suspend fun searchCities(query: String): List<AuraLocationResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/3.2.1")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body ?: return@use emptyList()
                val bodyStr = body.string()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val results = root["results"]?.jsonArray ?: return@use emptyList()

                results.mapNotNull { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val lat = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val lon = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val country = obj["country"]?.jsonPrimitive?.content
                    val admin1 = obj["admin1"]?.jsonPrimitive?.content

                    AuraLocationResult(
                        name = name,
                        country = country,
                        admin1 = admin1,
                        latitude = lat,
                        longitude = lon,
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && lastFetchTimeMs > 0 && (now - lastFetchTimeMs) < CACHE_DURATION_MS) {
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val isCustom = customLocation != null
                val lat: Double
                val lon: Double
                val city: String?
                val country: String?

                if (isCustom) {
                    val loc = customLocation!!
                    lat = loc.latitude
                    lon = loc.longitude
                    city = loc.name
                    country = loc.country
                } else {
                    // Resolve coordinates if not already cached
                    if (cachedLat == null || cachedLon == null) {
                        resolveCoordinates()
                    }
                    lat = cachedLat ?: 17.3850
                    lon = cachedLon ?: 78.4867
                    city = cachedCity
                    country = cachedCountry
                }

                // Fetch real-time weather from Open-Meteo
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AuraMusic/3.2.1")
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use
                    val bodyStr = body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val current = root["current"]?.jsonObject ?: return@use

                    val tempRaw = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: 22.0
                    val weatherCode = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
                    val isDayInt = current["is_day"]?.jsonPrimitive?.intOrNull ?: if (isDayTime()) 1 else 0
                    val isDay = isDayInt == 1
                    val humidity = current["relative_humidity_2m"]?.jsonPrimitive?.intOrNull
                    val windSpeed = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull

                    val (conditionLabel, visualType) = mapWmoCode(weatherCode, isDay)
                    val pills = moodPillsFor(visualType, isDay)

                    _currentWeather.value = AuraWeatherInfo(
                        temperatureC = tempRaw.roundToInt(),
                        conditionLabel = conditionLabel,
                        visualType = visualType,
                        city = city,
                        country = country,
                        isDay = isDay,
                        moodPills = pills,
                        humidity = humidity,
                        windSpeedKmH = windSpeed,
                        isCustomLocation = isCustom,
                        isFahrenheit = isFahrenheitPreference,
                    )
                    lastFetchTimeMs = now
                }
            } catch (ignored: Exception) {
                // Keep existing weather or fallback gracefully without crashing
                if (_currentWeather.value.temperatureC == 0 && _currentWeather.value.city == null) {
                    _currentWeather.value = defaultFallbackWeather()
                }
            }
        }
    }

    private fun resolveCoordinates() {
        // Primary coarse resolver: GeoJS
        if (resolveViaGeoJs()) return

        // Secondary failover resolver: FreeIPApi
        resolveViaFreeIpApi()
    }

    private fun resolveViaGeoJs(): Boolean {
        return try {
            val geoUrl = "https://get.geojs.io/v1/ip/geo.json"
            val request = Request.Builder()
                .url(geoUrl)
                .header("User-Agent", "AuraMusic/3.2.1")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                val root = json.parseToJsonElement(body.string()).jsonObject

                val latStr = root["latitude"]?.jsonPrimitive?.content
                val lonStr = root["longitude"]?.jsonPrimitive?.content
                cachedCity = root["city"]?.jsonPrimitive?.content
                cachedCountry = root["country"]?.jsonPrimitive?.content

                if (latStr != null && lonStr != null) {
                    cachedLat = latStr.toDoubleOrNull()
                    cachedLon = lonStr.toDoubleOrNull()
                    cachedLat != null && cachedLon != null
                } else false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveViaFreeIpApi(): Boolean {
        return try {
            val url = "https://freeipapi.com/api/json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/3.2.1")
                .build()

            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                val root = json.parseToJsonElement(body.string()).jsonObject

                val lat = root["latitude"]?.jsonPrimitive?.doubleOrNull
                val lon = root["longitude"]?.jsonPrimitive?.doubleOrNull
                cachedCity = root["cityName"]?.jsonPrimitive?.content
                cachedCountry = root["countryName"]?.jsonPrimitive?.content

                if (lat != null && lon != null) {
                    cachedLat = lat
                    cachedLon = lon
                    true
                } else false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun mapWmoCode(code: Int, isDay: Boolean): Pair<String, WeatherVisualType> {
        return when (code) {
            0 -> if (isDay) "Clear Sky" to WeatherVisualType.CLEAR_DAY else "Clear Night" to WeatherVisualType.CLEAR_NIGHT
            1 -> if (isDay) "Mainly Clear" to WeatherVisualType.CLEAR_DAY else "Clear Night" to WeatherVisualType.CLEAR_NIGHT
            2 -> if (isDay) "Partly Cloudy" to WeatherVisualType.CLEAR_DAY else "Partly Cloudy" to WeatherVisualType.CLEAR_NIGHT
            3 -> "Overcast" to WeatherVisualType.CLOUDY
            45, 48 -> "Foggy" to WeatherVisualType.CLOUDY
            51, 53, 55 -> "Light Drizzle" to WeatherVisualType.RAIN
            56, 57 -> "Freezing Drizzle" to WeatherVisualType.RAIN
            61, 63 -> "Rainy" to WeatherVisualType.RAIN
            65 -> "Heavy Rain" to WeatherVisualType.RAIN
            66, 67 -> "Freezing Rain" to WeatherVisualType.RAIN
            71, 73, 75, 77 -> "Snowing" to WeatherVisualType.SNOW
            80, 81, 82 -> "Rain Showers" to WeatherVisualType.RAIN
            85, 86 -> "Snow Showers" to WeatherVisualType.SNOW
            95, 96, 99 -> "Thunderstorm" to WeatherVisualType.THUNDERSTORM
            else -> if (isDay) "Clear Sky" to WeatherVisualType.CLEAR_DAY else "Clear Night" to WeatherVisualType.CLEAR_NIGHT
        }
    }

    fun moodPillsFor(visualType: WeatherVisualType, isDay: Boolean): List<String> = when (visualType) {
        WeatherVisualType.RAIN -> listOf("Rainy Day Lo-Fi", "Acoustic Rain", "Cozy Jazz", "Chill Hop")
        WeatherVisualType.THUNDERSTORM -> listOf("Storm Ambient", "Dark Academia", "Thunder Rhythms", "Deep Chill")
        WeatherVisualType.SNOW -> listOf("Winter Acoustic", "Warm Coffee", "Soft Piano", "Chill Snow")
        WeatherVisualType.CLOUDY -> listOf("Cloudy Afternoon", "Melancholy Indie", "Warm Acoustic", "Mellow Beats")
        WeatherVisualType.CLEAR_DAY -> listOf("Sunny Energy", "Upbeat Grooves", "Morning Coffee", "Indie Vibes")
        WeatherVisualType.CLEAR_NIGHT -> listOf("Midnight Ambient", "Night Drive", "Deep Focus", "Piano Dreams")
    }

    private fun isDayTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }

    private fun defaultFallbackWeather(): AuraWeatherInfo {
        val isDay = isDayTime()
        val visualType = if (isDay) WeatherVisualType.CLEAR_DAY else WeatherVisualType.CLEAR_NIGHT
        val label = if (isDay) "Clear Sky" else "Clear Night"
        val temp = if (isDay) 23 else 19
        return AuraWeatherInfo(
            temperatureC = temp,
            conditionLabel = label,
            visualType = visualType,
            city = null,
            country = null,
            isDay = isDay,
            moodPills = moodPillsFor(visualType, isDay),
            isFahrenheit = isFahrenheitPreference,
        )
    }
}
