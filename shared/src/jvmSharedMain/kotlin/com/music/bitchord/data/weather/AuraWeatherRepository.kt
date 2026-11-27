package com.music.bitchord.data.weather

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

enum class WeatherVisualType {
    CLEAR_DAY,
    CLEAR_NIGHT,
    SUNRISE,
    SUNSET,
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

    internal data class DailySolarWindow(
        val sunriseEpochMs: Long,
        val sunsetEpochMs: Long,
    )

    private const val CACHE_DURATION_MS = 45 * 60 * 1000L // 45 minutes
    private const val CACHE_FILE_NAME = "aura_weather_cache.json"
    private const val SUNRISE_PRE_WINDOW_MS = 30L * 60_000L
    private const val SUNRISE_POST_WINDOW_MS = 50L * 60_000L
    private const val SUNSET_PRE_WINDOW_MS = 45L * 60_000L
    private const val SUNSET_POST_WINDOW_MS = 40L * 60_000L

    private val cacheLock = Any()

    @Volatile
    private var storageDir: File? = null
    @Volatile
    private var lastFetchTimeMs = 0L

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var isFahrenheitPreference = false
    @Volatile
    private var customLocation: AuraLocationResult? = null

    private val _currentWeather = MutableStateFlow(defaultFallbackWeather())
    val currentWeather: StateFlow<AuraWeatherInfo> = _currentWeather.asStateFlow()

    @Volatile
    private var cachedLat: Double? = null
    @Volatile
    private var cachedLon: Double? = null
    @Volatile
    private var cachedCity: String? = null
    @Volatile
    private var cachedCountry: String? = null
    @Volatile
    private var cachedWeatherCode: Int? = null
    @Volatile
    private var cachedTemperatureC: Int? = null
    @Volatile
    private var cachedHumidity: Int? = null
    @Volatile
    private var cachedWindSpeedKmH: Double? = null
    @Volatile
    private var cachedApiIsDay: Boolean? = null
    @Volatile
    private var cachedUtcOffsetSeconds: Int? = null
    @Volatile
    private var cachedSolarWindows: List<DailySolarWindow> = emptyList()
    @Volatile
    private var manualVisualOverride: WeatherVisualType? = null

    /**
     * Binds a writable directory for persisting the last fetched local weather,
     * custom location, solar windows, and unit preference across cold restarts.
     */
    fun init(directory: File) {
        synchronized(cacheLock) {
            storageDir = directory
            loadCacheLocked()
        }
    }

    internal fun resetForTests() {
        synchronized(cacheLock) {
            storageDir = null
            lastFetchTimeMs = 0L
            isFahrenheitPreference = false
            customLocation = null
            cachedLat = null
            cachedLon = null
            cachedCity = null
            cachedCountry = null
            cachedWeatherCode = null
            cachedTemperatureC = null
            cachedHumidity = null
            cachedWindSpeedKmH = null
            cachedApiIsDay = null
            cachedUtcOffsetSeconds = null
            cachedSolarWindows = emptyList()
            manualVisualOverride = null
            _currentWeather.value = defaultFallbackWeather()
        }
    }

    fun isCustomLocation(): Boolean = customLocation != null

    fun currentCustomLocation(): AuraLocationResult? = customLocation

    fun currentManualVisualOverride(): WeatherVisualType? = manualVisualOverride

    fun setManualVisualOverride(override: WeatherVisualType?) {
        manualVisualOverride = override
        if (override == null) {
            refreshCelestialState()
        } else {
            val curr = _currentWeather.value
            val isDay = override != WeatherVisualType.CLEAR_NIGHT
            val label = conditionLabelForOverride(override)
            _currentWeather.value = curr.copy(
                visualType = override,
                conditionLabel = label,
                isDay = isDay,
                moodPills = moodPillsFor(override, isDay),
            )
        }
    }

    /**
     * Re-evaluates astronomical Sunrise / Sunset / Daylight / Night transitions against cached
     * city solar timestamps as local time advances, without making extra network requests.
     */
    fun refreshCelestialState(nowEpochMs: Long = System.currentTimeMillis()) {
        if (manualVisualOverride != null) return
        val curr = _currentWeather.value
        val fallbackIsDay = cachedApiIsDay ?: isDayTime(nowEpochMs, cachedUtcOffsetSeconds)
        val (autoLabel, autoVisual, effectiveIsDay) = mapWmoCode(
            code = cachedWeatherCode ?: 0,
            fallbackIsDay = fallbackIsDay,
            nowEpochMs = nowEpochMs,
            solarWindows = cachedSolarWindows,
            utcOffsetSeconds = cachedUtcOffsetSeconds,
        )
        if (curr.visualType != autoVisual || curr.conditionLabel != autoLabel || curr.isDay != effectiveIsDay) {
            _currentWeather.value = curr.copy(
                visualType = autoVisual,
                conditionLabel = autoLabel,
                isDay = effectiveIsDay,
                moodPills = moodPillsFor(autoVisual, effectiveIsDay),
            )
        }
    }

    private fun conditionLabelForOverride(override: WeatherVisualType): String = when (override) {
        WeatherVisualType.SUNRISE -> "Sunrise"
        WeatherVisualType.SUNSET -> "Sunset"
        WeatherVisualType.CLEAR_DAY -> "Clear Sky"
        WeatherVisualType.CLEAR_NIGHT -> "Clear Night"
        WeatherVisualType.RAIN -> "Rainy"
        WeatherVisualType.THUNDERSTORM -> "Thunderstorm"
        WeatherVisualType.CLOUDY -> "Cloudy"
        WeatherVisualType.SNOW -> "Snowing"
    }

    fun setTemperatureUnit(useFahrenheit: Boolean) {
        isFahrenheitPreference = useFahrenheit
        val curr = _currentWeather.value
        _currentWeather.value = curr.copy(isFahrenheit = useFahrenheit)
        persistState()
    }

    fun toggleTemperatureUnit() {
        setTemperatureUnit(!isFahrenheitPreference)
    }

    suspend fun setCustomLocation(location: AuraLocationResult?) {
        customLocation = location
        lastFetchTimeMs = 0L // invalidate cache
        persistState()
        refresh(force = true)
    }

    suspend fun clearCustomLocation() {
        customLocation = null
        lastFetchTimeMs = 0L // invalidate cache
        persistState()
        refresh(force = true)
    }

    private fun persistState() {
        synchronized(cacheLock) {
            saveCacheLocked()
        }
    }

    private fun saveCacheLocked() {
        val dir = storageDir ?: return
        runCatching {
            if (!dir.exists()) dir.mkdirs()
            val payload = buildJsonObject {
                put("isFahrenheit", isFahrenheitPreference)
                customLocation?.let { loc ->
                    put(
                        "customLocation",
                        buildJsonObject {
                            put("name", loc.name)
                            loc.country?.let { put("country", it) }
                            loc.admin1?.let { put("admin1", it) }
                            put("latitude", loc.latitude)
                            put("longitude", loc.longitude)
                        },
                    )
                }
                cachedLat?.let { put("cachedLat", it) }
                cachedLon?.let { put("cachedLon", it) }
                cachedCity?.let { put("cachedCity", it) }
                cachedCountry?.let { put("cachedCountry", it) }
                cachedWeatherCode?.let { put("cachedWeatherCode", it) }
                cachedTemperatureC?.let { put("cachedTemperatureC", it) }
                cachedHumidity?.let { put("cachedHumidity", it) }
                cachedWindSpeedKmH?.let { put("cachedWindSpeedKmH", it) }
                cachedApiIsDay?.let { put("cachedApiIsDay", it) }
                cachedUtcOffsetSeconds?.let { put("cachedUtcOffsetSeconds", it) }
                put("lastFetchTimeMs", lastFetchTimeMs)
                if (cachedSolarWindows.isNotEmpty()) {
                    put(
                        "solarWindows",
                        buildJsonArray {
                            cachedSolarWindows.forEach { w ->
                                add(
                                    buildJsonObject {
                                        put("sunriseEpochMs", w.sunriseEpochMs)
                                        put("sunsetEpochMs", w.sunsetEpochMs)
                                    },
                                )
                            }
                        },
                    )
                }
            }
            File(dir, CACHE_FILE_NAME).writeText(payload.toString())
        }
    }

    private fun loadCacheLocked() {
        val file = storageDir?.let { File(it, CACHE_FILE_NAME) } ?: return
        if (!file.isFile) return
        val root = runCatching { json.parseToJsonElement(file.readText()).jsonObject }.getOrNull() ?: return

        isFahrenheitPreference = root.bool("isFahrenheit") ?: false
        customLocation = (root["customLocation"] as? JsonObject)?.let { loc ->
            val name = loc.str("name") ?: return@let null
            val lat = loc.dbl("latitude") ?: return@let null
            val lon = loc.dbl("longitude") ?: return@let null
            AuraLocationResult(
                name = name,
                country = loc.str("country"),
                admin1 = loc.str("admin1"),
                latitude = lat,
                longitude = lon,
            )
        }
        cachedLat = root.dbl("cachedLat")
        cachedLon = root.dbl("cachedLon")
        cachedCity = root.str("cachedCity")
        cachedCountry = root.str("cachedCountry")
        cachedWeatherCode = root.int("cachedWeatherCode")
        cachedTemperatureC = root.int("cachedTemperatureC")
        cachedHumidity = root.int("cachedHumidity")
        cachedWindSpeedKmH = root.dbl("cachedWindSpeedKmH")
        cachedApiIsDay = root.bool("cachedApiIsDay")
        cachedUtcOffsetSeconds = root.int("cachedUtcOffsetSeconds")
        lastFetchTimeMs = root.long("lastFetchTimeMs") ?: 0L
        cachedSolarWindows = (root["solarWindows"] as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val rise = obj.long("sunriseEpochMs") ?: return@mapNotNull null
                val set = obj.long("sunsetEpochMs") ?: return@mapNotNull null
                if (set > rise) DailySolarWindow(rise, set) else null
            }
            .orEmpty()

        val temp = cachedTemperatureC
        val code = cachedWeatherCode
        if (temp != null && code != null) {
            val now = System.currentTimeMillis()
            val fallbackIsDay = cachedApiIsDay ?: isDayTime(now, cachedUtcOffsetSeconds)
            val mapped = mapWmoCode(
                code = code,
                fallbackIsDay = fallbackIsDay,
                nowEpochMs = now,
                solarWindows = cachedSolarWindows,
                utcOffsetSeconds = cachedUtcOffsetSeconds,
            )
            val activeOverride = manualVisualOverride
            val visualType = activeOverride ?: mapped.visualType
            val label = if (activeOverride != null) conditionLabelForOverride(activeOverride) else mapped.label
            val finalIsDay = if (activeOverride != null) activeOverride != WeatherVisualType.CLEAR_NIGHT else mapped.isDay
            _currentWeather.value = AuraWeatherInfo(
                temperatureC = temp,
                conditionLabel = label,
                visualType = visualType,
                city = customLocation?.name ?: cachedCity,
                country = customLocation?.country ?: cachedCountry,
                isDay = finalIsDay,
                moodPills = moodPillsFor(visualType, finalIsDay),
                humidity = cachedHumidity,
                windSpeedKmH = cachedWindSpeedKmH,
                isCustomLocation = customLocation != null,
                isFahrenheit = isFahrenheitPreference,
            )
        } else {
            _currentWeather.value = _currentWeather.value.copy(
                isFahrenheit = isFahrenheitPreference,
                isCustomLocation = customLocation != null,
                city = customLocation?.name ?: cachedCity,
                country = customLocation?.country ?: cachedCountry,
            )
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonObject.dbl(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull

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
                .header("User-Agent", "AuraMusic/3.3.0")
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
            refreshCelestialState(now)
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

                // Fetch real-time weather + daily astronomical sunrise/sunset in the target city's timezone
                val url =
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m" +
                        "&daily=sunrise,sunset&timezone=auto&past_days=1&forecast_days=2"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AuraMusic/3.3.0")
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use
                    val bodyStr = body.string()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val current = root["current"]?.jsonObject ?: return@use

                    val (parsedUtcOffset, parsedSolarWindows) = parseDailySolarWindows(root)
                    cachedUtcOffsetSeconds = parsedUtcOffset
                    cachedSolarWindows = parsedSolarWindows

                    val tempRaw = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: 22.0
                    val weatherCode = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
                    cachedWeatherCode = weatherCode
                    val apiIsDayInt = current["is_day"]?.jsonPrimitive?.intOrNull
                    val fallbackIsDay = if (apiIsDayInt != null) {
                        apiIsDayInt == 1
                    } else {
                        isDayTime(now, parsedUtcOffset)
                    }
                    cachedApiIsDay = fallbackIsDay

                    val humidity = current["relative_humidity_2m"]?.jsonPrimitive?.intOrNull
                    val windSpeed = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull

                    val (autoLabel, autoVisual, effectiveIsDay) = mapWmoCode(
                        code = weatherCode,
                        fallbackIsDay = fallbackIsDay,
                        nowEpochMs = now,
                        solarWindows = parsedSolarWindows,
                        utcOffsetSeconds = parsedUtcOffset,
                    )
                    val activeOverride = manualVisualOverride
                    val visualType = activeOverride ?: autoVisual
                    val conditionLabel = if (activeOverride != null) {
                        conditionLabelForOverride(activeOverride)
                    } else {
                        autoLabel
                    }
                    val finalIsDay = if (activeOverride != null) {
                        activeOverride != WeatherVisualType.CLEAR_NIGHT
                    } else {
                        effectiveIsDay
                    }
                    val pills = moodPillsFor(visualType, finalIsDay)

                    val tempC = tempRaw.roundToInt()
                    cachedTemperatureC = tempC
                    cachedHumidity = humidity
                    cachedWindSpeedKmH = windSpeed
                    _currentWeather.value = AuraWeatherInfo(
                        temperatureC = tempC,
                        conditionLabel = conditionLabel,
                        visualType = visualType,
                        city = city,
                        country = country,
                        isDay = finalIsDay,
                        moodPills = pills,
                        humidity = humidity,
                        windSpeedKmH = windSpeed,
                        isCustomLocation = isCustom,
                        isFahrenheit = isFahrenheitPreference,
                    )
                    lastFetchTimeMs = now
                    persistState()
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
                .header("User-Agent", "AuraMusic/3.3.0")
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
                .header("User-Agent", "AuraMusic/3.3.0")
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

    internal fun parseDailySolarWindows(root: JsonObject): Pair<Int?, List<DailySolarWindow>> {
        val utcOffsetSeconds = root["utc_offset_seconds"]?.jsonPrimitive?.intOrNull
        val daily = root["daily"]?.jsonObject ?: return utcOffsetSeconds to emptyList()
        val sunriseArr = daily["sunrise"]?.jsonArray ?: return utcOffsetSeconds to emptyList()
        val sunsetArr = daily["sunset"]?.jsonArray ?: return utcOffsetSeconds to emptyList()
        val offset = ZoneOffset.ofTotalSeconds(utcOffsetSeconds ?: 0)

        val count = minOf(sunriseArr.size, sunsetArr.size)
        val windows = buildList {
            for (i in 0 until count) {
                val riseStr = sunriseArr[i].jsonPrimitive.content
                val setStr = sunsetArr[i].jsonPrimitive.content
                val riseMs = parseIsoLocalEpochMs(riseStr, offset)
                val setMs = parseIsoLocalEpochMs(setStr, offset)
                if (riseMs != null && setMs != null && setMs > riseMs) {
                    add(DailySolarWindow(sunriseEpochMs = riseMs, sunsetEpochMs = setMs))
                }
            }
        }
        return utcOffsetSeconds to windows
    }

    private fun parseIsoLocalEpochMs(isoLocal: String, offset: ZoneOffset): Long? {
        return try {
            LocalDateTime.parse(isoLocal).toInstant(offset).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Determines clear-sky celestial phase (Sunrise, Daylight, Sunset, Clear Night) from exact
     * astronomical sunrise/sunset timestamps, falling back to location-offset clock hours if unavailable.
     */
    internal fun resolveAstronomicalCelestialState(
        nowEpochMs: Long,
        solarWindows: List<DailySolarWindow>,
        utcOffsetSeconds: Int?,
        fallbackIsDay: Boolean,
    ): Pair<WeatherVisualType, Boolean> {
        if (solarWindows.isNotEmpty()) {
            val isSunrise = solarWindows.any {
                nowEpochMs in (it.sunriseEpochMs - SUNRISE_PRE_WINDOW_MS)..(it.sunriseEpochMs + SUNRISE_POST_WINDOW_MS)
            }
            if (isSunrise) {
                return WeatherVisualType.SUNRISE to true
            }

            val inSunset = solarWindows.firstOrNull {
                nowEpochMs in (it.sunsetEpochMs - SUNSET_PRE_WINDOW_MS)..(it.sunsetEpochMs + SUNSET_POST_WINDOW_MS)
            }
            if (inSunset != null) {
                return WeatherVisualType.SUNSET to (nowEpochMs < inSunset.sunsetEpochMs)
            }

            val isDaylight = solarWindows.any {
                nowEpochMs >= (it.sunriseEpochMs + SUNRISE_POST_WINDOW_MS) &&
                    nowEpochMs < (it.sunsetEpochMs - SUNSET_PRE_WINDOW_MS)
            }
            return if (isDaylight) {
                WeatherVisualType.CLEAR_DAY to true
            } else {
                WeatherVisualType.CLEAR_NIGHT to false
            }
        }

        val hour = localHourFor(nowEpochMs, utcOffsetSeconds)
        return when {
            !fallbackIsDay || hour < 5 || hour >= 20 -> WeatherVisualType.CLEAR_NIGHT to false
            hour in 5..7 -> WeatherVisualType.SUNRISE to true
            hour in 17..19 -> WeatherVisualType.SUNSET to true
            else -> WeatherVisualType.CLEAR_DAY to true
        }
    }

    internal data class MappedWeather(
        val label: String,
        val visualType: WeatherVisualType,
        val isDay: Boolean,
    )

    internal fun mapWmoCode(
        code: Int,
        fallbackIsDay: Boolean,
        nowEpochMs: Long = System.currentTimeMillis(),
        solarWindows: List<DailySolarWindow> = cachedSolarWindows,
        utcOffsetSeconds: Int? = cachedUtcOffsetSeconds,
    ): MappedWeather {
        val (celestialType, effectiveIsDay) = resolveAstronomicalCelestialState(
            nowEpochMs = nowEpochMs,
            solarWindows = solarWindows,
            utcOffsetSeconds = utcOffsetSeconds,
            fallbackIsDay = fallbackIsDay,
        )
        val celestialLabel = when (celestialType) {
            WeatherVisualType.SUNRISE -> "Sunrise"
            WeatherVisualType.SUNSET -> "Sunset"
            WeatherVisualType.CLEAR_DAY -> "Clear Sky"
            else -> "Clear Night"
        }

        val (label, visual) = when (code) {
            0 -> celestialLabel to celestialType
            1 -> (
                when (celestialType) {
                    WeatherVisualType.SUNRISE -> "Dawn"
                    WeatherVisualType.SUNSET -> "Golden Hour"
                    WeatherVisualType.CLEAR_DAY -> "Mainly Clear"
                    else -> "Clear Night"
                }
            ) to celestialType
            2 -> (
                when (celestialType) {
                    WeatherVisualType.SUNRISE -> "Sunrise Clouds"
                    WeatherVisualType.SUNSET -> "Sunset Clouds"
                    WeatherVisualType.CLEAR_DAY -> "Partly Cloudy"
                    else -> "Partly Cloudy"
                }
            ) to celestialType
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
            else -> celestialLabel to celestialType
        }
        return MappedWeather(label = label, visualType = visual, isDay = effectiveIsDay)
    }

    fun moodPillsFor(visualType: WeatherVisualType, isDay: Boolean): List<String> = when (visualType) {
        WeatherVisualType.SUNRISE -> listOf("Sunrise Acoustic", "Morning Dawn", "Fresh Awakening", "Golden Hour")
        WeatherVisualType.SUNSET -> listOf("Sunset Chill", "Golden Hour Grooves", "Dusk Lo-Fi", "Evening Drive")
        WeatherVisualType.RAIN -> listOf("Rainy Day Lo-Fi", "Acoustic Rain", "Cozy Jazz", "Chill Hop")
        WeatherVisualType.THUNDERSTORM -> listOf("Storm Ambient", "Dark Academia", "Thunder Rhythms", "Deep Chill")
        WeatherVisualType.SNOW -> listOf("Winter Acoustic", "Warm Coffee", "Soft Piano", "Chill Snow")
        WeatherVisualType.CLOUDY -> listOf("Cloudy Afternoon", "Melancholy Indie", "Warm Acoustic", "Mellow Beats")
        WeatherVisualType.CLEAR_DAY -> listOf("Sunny Energy", "Upbeat Grooves", "Morning Coffee", "Indie Vibes")
        WeatherVisualType.CLEAR_NIGHT -> listOf("Midnight Ambient", "Night Drive", "Deep Focus", "Piano Dreams")
    }

    private fun localHourFor(nowEpochMs: Long, utcOffsetSeconds: Int?): Int {
        val cal = if (utcOffsetSeconds != null) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = nowEpochMs + utcOffsetSeconds * 1000L
            }
        } else {
            Calendar.getInstance().apply {
                timeInMillis = nowEpochMs
            }
        }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun isDayTime(nowEpochMs: Long = System.currentTimeMillis(), utcOffsetSeconds: Int? = null): Boolean {
        val hour = localHourFor(nowEpochMs, utcOffsetSeconds)
        return hour in 6..18
    }

    private fun defaultFallbackWeather(): AuraWeatherInfo {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = isDayTime()
        val visualType = when {
            hour in 5..7 -> WeatherVisualType.SUNRISE
            hour in 17..19 -> WeatherVisualType.SUNSET
            isDay -> WeatherVisualType.CLEAR_DAY
            else -> WeatherVisualType.CLEAR_NIGHT
        }
        val label = when (visualType) {
            WeatherVisualType.SUNRISE -> "Sunrise"
            WeatherVisualType.SUNSET -> "Sunset"
            WeatherVisualType.CLEAR_DAY -> "Clear Sky"
            else -> "Clear Night"
        }
        val temp = when (visualType) {
            WeatherVisualType.SUNRISE -> 20
            WeatherVisualType.SUNSET -> 22
            WeatherVisualType.CLEAR_DAY -> 25
            else -> 19
        }
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
