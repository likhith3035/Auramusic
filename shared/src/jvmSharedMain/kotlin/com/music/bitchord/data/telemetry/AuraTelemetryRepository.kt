package com.music.bitchord.data.telemetry

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DailyHistoryItem(
    val date: String,
    val count: Int,
)

data class TelemetryStats(
    val activeToday: Int,
    val allTimeDevices: Int,
    val versions: Map<String, Int>,
    val platforms: Map<String, Int>,
    val dailyHistory: List<DailyHistoryItem>,
    val latencyMs: Long,
    val isOnline: Boolean,
)

object AuraTelemetryRepository {
    private const val BASE_URL = "https://auramusic-stats.onrender.com"
    private const val STATS_URL = "$BASE_URL/api/stats"
    private const val HEARTBEAT_URL = "$BASE_URL/api/heartbeat"
    private const val HEALTH_URL = "$BASE_URL/health"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Non-blocking background pre-warm probe to wake up sleeping Render free-tier containers.
     */
    suspend fun preWarmServer(): Unit = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(HEALTH_URL)
                .header("User-Agent", "AuraMusic/PreWarm")
                .get()
                .build()

            val client = Http.client.newBuilder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().close()
        } catch (_: Exception) {
            // Silently ignore pre-warm failures
        }
    }

    suspend fun fetchStats(): Result<TelemetryStats> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(STATS_URL)
                .header("User-Agent", "AuraMusic/Admin")
                .get()
                .build()

            val client = Http.client.newBuilder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(50, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val bodyStr = response.body.string()
                val root = json.parseToJsonElement(bodyStr).jsonObject

                val activeToday = root["activeToday"]?.jsonPrimitive?.intOrNull ?: 0
                val allTimeDevices = root["allTimeDevices"]?.jsonPrimitive?.intOrNull ?: 0

                val todayBreakdown = root["todayBreakdown"]?.jsonObject
                val versionsMap = mutableMapOf<String, Int>()
                todayBreakdown?.get("versions")?.jsonObject?.forEach { (k, v) ->
                    v.jsonPrimitive.intOrNull?.let { versionsMap[k] = it }
                }

                val platformsMap = mutableMapOf<String, Int>()
                todayBreakdown?.get("platforms")?.jsonObject?.forEach { (k, v) ->
                    v.jsonPrimitive.intOrNull?.let { platformsMap[k] = it }
                }

                val historyList = mutableListOf<DailyHistoryItem>()
                root["dailyHistory"]?.jsonArray?.forEach { elem ->
                    val obj = elem.jsonObject
                    val date = obj["date"]?.jsonPrimitive?.content ?: ""
                    val count = obj["count"]?.jsonPrimitive?.intOrNull ?: 0
                    if (date.isNotEmpty()) {
                        historyList.add(DailyHistoryItem(date = date, count = count))
                    }
                }

                Result.success(
                    TelemetryStats(
                        activeToday = activeToday,
                        allTimeDevices = allTimeDevices,
                        versions = versionsMap,
                        platforms = platformsMap,
                        dailyHistory = historyList,
                        latencyMs = latency,
                        isOnline = true,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendManualHeartbeat(version: String, platform: String = "Android"): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val installId = AuraTelemetryClient.getOrCreateInstallId()
            val jsonPayload = """
                {
                    "installId": "$installId",
                    "version": "$version",
                    "platform": "$platform",
                    "osVersion": "${System.getProperty("os.name") ?: "Android"}"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(HEARTBEAT_URL)
                .header("User-Agent", "AuraMusic/$version ($platform)")
                .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val client = Http.client.newBuilder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(50, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val bodyStr = response.body.string()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val active = root["activeToday"]?.jsonPrimitive?.intOrNull ?: 1
                Result.success(active)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

