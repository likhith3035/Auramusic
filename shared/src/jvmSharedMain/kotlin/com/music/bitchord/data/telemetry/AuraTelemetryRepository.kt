package com.music.bitchord.data.telemetry

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
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

data class LiveNetworkPulse(
    val onlineNow: Int,
    val androidOnline: Int,
    val pcOnline: Int,
    val activeParties: Int,
    val partyMembers: Int,
    val totalPartiesCreated: Long,
    val totalChatsSent: Long,
    val latencyMs: Long,
)

enum class ReleaseAssetPlatform {
    ANDROID,
    DESKTOP,
    OTHER,
}

data class ReleaseAssetDownload(
    val name: String,
    val downloadCount: Long,
    val platform: ReleaseAssetPlatform,
)

data class ReleaseDownloadInfo(
    val tagName: String,
    val name: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val totalDownloads: Long,
    val androidDownloads: Long,
    val desktopDownloads: Long,
    val assets: List<ReleaseAssetDownload>,
)

data class GitHubDownloadsSummary(
    val totalDownloads: Long,
    val androidDownloads: Long,
    val desktopDownloads: Long,
    val otherDownloads: Long,
    val releaseCount: Int,
    val latestTag: String?,
    val latestReleaseDownloads: Long,
    val releases: List<ReleaseDownloadInfo>,
    val fetchedAtMs: Long,
)

data class TelemetryDashboardSnapshot(
    val livePulseResult: Result<LiveNetworkPulse>,
    val githubDownloadsResult: Result<GitHubDownloadsSummary>,
    val dailyHeartbeatResult: Result<TelemetryStats>,
    val analyticsStatus: AnalyticsConnectionStatus,
    val fetchedAtMs: Long,
)

object AuraTelemetryRepository {
    internal var statsBaseUrl = "https://auramusic-stats.onrender.com"
    internal var livePresenceUrl = "https://auramusic-kjov.onrender.com/stats?format=json"
    internal var githubReleasesUrl = "https://api.github.com/repos/likhith3035/Auramusic/releases?per_page=100"

    private const val GITHUB_CACHE_TTL_MS = 120_000L
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedGitHubDownloads: GitHubDownloadsSummary? = null

    /**
     * Non-blocking background pre-warm probe to wake up sleeping Render free-tier containers.
     */
    suspend fun preWarmServer(): Unit = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$statsBaseUrl/health")
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

    suspend fun fetchDashboardSnapshot(
        forceRefresh: Boolean = false,
    ): TelemetryDashboardSnapshot = coroutineScope {
        val liveDeferred = async(Dispatchers.IO) { fetchLiveNetworkPulse() }
        val downloadsDeferred = async(Dispatchers.IO) { fetchGitHubDownloads(forceRefresh = forceRefresh) }
        val heartbeatDeferred = async(Dispatchers.IO) { fetchStats() }

        TelemetryDashboardSnapshot(
            livePulseResult = liveDeferred.await(),
            githubDownloadsResult = downloadsDeferred.await(),
            dailyHeartbeatResult = heartbeatDeferred.await(),
            analyticsStatus = AuraAnalytics.status.value,
            fetchedAtMs = System.currentTimeMillis(),
        )
    }

    suspend fun fetchLiveNetworkPulse(): Result<LiveNetworkPulse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(livePresenceUrl)
                .header("User-Agent", "AuraMusic/Admin")
                .header("Accept", "application/json")
                .get()
                .build()

            val client = Http.client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val bodyStr = response.body.string()
                Result.success(parseLivePresenceJson(bodyStr, latency))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchGitHubDownloads(forceRefresh: Boolean = false): Result<GitHubDownloadsSummary> =
        withContext(Dispatchers.IO) {
            val cached = cachedGitHubDownloads
            val now = System.currentTimeMillis()
            if (!forceRefresh && cached != null && (now - cached.fetchedAtMs) < GITHUB_CACHE_TTL_MS) {
                return@withContext Result.success(cached)
            }

            try {
                val request = Request.Builder()
                    .url(githubReleasesUrl)
                    .header("User-Agent", "AuraMusic/Admin")
                    .header("Accept", "application/vnd.github+json")
                    .get()
                    .build()

                val client = Http.client.newBuilder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (cached != null) return@withContext Result.success(cached)
                        return@withContext Result.failure(Exception("GitHub API HTTP ${response.code}"))
                    }
                    val bodyStr = response.body.string()
                    val parsed = parseGitHubReleasesJson(bodyStr, now)
                    cachedGitHubDownloads = parsed
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                if (cached != null) return@withContext Result.success(cached)
                Result.failure(e)
            }
        }

    suspend fun fetchStats(): Result<TelemetryStats> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("$statsBaseUrl/api/stats")
                .header("User-Agent", "AuraMusic/Admin")
                .get()
                .build()

            val client = Http.client.newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val bodyStr = response.body.string()
                Result.success(parseTelemetryStatsJson(bodyStr, latency))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendManualHeartbeat(version: String, platform: String = "Android"): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val installId = AuraTelemetryClient.getOrCreateInstallId()
                val jsonPayload = buildJsonObject {
                    put("installId", installId)
                    put("version", version)
                    put("platform", platform)
                    put("osVersion", System.getProperty("os.name") ?: "Android")
                }.toString()

                val request = Request.Builder()
                    .url("$statsBaseUrl/api/heartbeat")
                    .header("User-Agent", "AuraMusic/$version ($platform)")
                    .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val client = Http.client.newBuilder()
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
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

    internal fun parseTelemetryStatsJson(bodyStr: String, latencyMs: Long): TelemetryStats {
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

        return TelemetryStats(
            activeToday = activeToday,
            allTimeDevices = allTimeDevices,
            versions = versionsMap,
            platforms = platformsMap,
            dailyHistory = historyList,
            latencyMs = latencyMs,
            isOnline = true,
        )
    }

    internal fun parseLivePresenceJson(bodyStr: String, latencyMs: Long): LiveNetworkPulse {
        val root = json.parseToJsonElement(bodyStr).jsonObject
        return LiveNetworkPulse(
            onlineNow = root["online"]?.jsonPrimitive?.intOrNull ?: 0,
            androidOnline = root["android"]?.jsonPrimitive?.intOrNull ?: 0,
            pcOnline = root["pc"]?.jsonPrimitive?.intOrNull ?: 0,
            activeParties = root["activeParties"]?.jsonPrimitive?.intOrNull ?: 0,
            partyMembers = root["partyMembers"]?.jsonPrimitive?.intOrNull ?: 0,
            totalPartiesCreated = root["totalParties"]?.jsonPrimitive?.longOrNull ?: 0L,
            totalChatsSent = root["totalChats"]?.jsonPrimitive?.longOrNull ?: 0L,
            latencyMs = latencyMs,
        )
    }

    internal fun parseGitHubReleasesJson(
        bodyStr: String,
        fetchedAtMs: Long = System.currentTimeMillis(),
    ): GitHubDownloadsSummary {
        val releasesArray = json.parseToJsonElement(bodyStr) as? JsonArray
            ?: throw IllegalStateException("Unexpected GitHub releases response")

        val releaseInfos = mutableListOf<ReleaseDownloadInfo>()
        var grandTotal = 0L
        var grandAndroid = 0L
        var grandDesktop = 0L
        var grandOther = 0L

        for (element in releasesArray) {
            val obj = element as? JsonObject ?: continue
            if (obj["draft"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true) continue

            val tagName = obj["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: tagName
            val publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val isPrerelease = obj["prerelease"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true

            val assets = obj["assets"]?.jsonArray.orEmpty().mapNotNull { assetElem ->
                val assetObj = assetElem as? JsonObject ?: return@mapNotNull null
                val assetName = assetObj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val downloads = (assetObj["download_count"]?.jsonPrimitive?.longOrNull ?: 0L).coerceAtLeast(0L)
                ReleaseAssetDownload(
                    name = assetName,
                    downloadCount = downloads,
                    platform = classifyAssetPlatform(assetName),
                )
            }

            val releaseTotal = assets.sumOf { it.downloadCount }
            val releaseAndroid = assets.filter { it.platform == ReleaseAssetPlatform.ANDROID }.sumOf { it.downloadCount }
            val releaseDesktop = assets.filter { it.platform == ReleaseAssetPlatform.DESKTOP }.sumOf { it.downloadCount }
            val releaseOther = assets.filter { it.platform == ReleaseAssetPlatform.OTHER }.sumOf { it.downloadCount }

            grandTotal += releaseTotal
            grandAndroid += releaseAndroid
            grandDesktop += releaseDesktop
            grandOther += releaseOther

            releaseInfos += ReleaseDownloadInfo(
                tagName = tagName,
                name = name,
                publishedAt = publishedAt,
                isPrerelease = isPrerelease,
                totalDownloads = releaseTotal,
                androidDownloads = releaseAndroid,
                desktopDownloads = releaseDesktop,
                assets = assets,
            )
        }

        val latest = releaseInfos.firstOrNull { !it.isPrerelease } ?: releaseInfos.firstOrNull()
        return GitHubDownloadsSummary(
            totalDownloads = grandTotal,
            androidDownloads = grandAndroid,
            desktopDownloads = grandDesktop,
            otherDownloads = grandOther,
            releaseCount = releaseInfos.size,
            latestTag = latest?.tagName,
            latestReleaseDownloads = latest?.totalDownloads ?: 0L,
            releases = releaseInfos,
            fetchedAtMs = fetchedAtMs,
        )
    }

    internal fun classifyAssetPlatform(assetName: String): ReleaseAssetPlatform {
        val lower = assetName.trim().lowercase()
        return when {
            lower.endsWith(".apk") || lower.endsWith(".aab") -> ReleaseAssetPlatform.ANDROID
            lower.endsWith(".exe") ||
                lower.endsWith(".msi") ||
                lower.endsWith(".appimage") ||
                lower.endsWith(".deb") ||
                lower.endsWith(".rpm") ||
                lower.endsWith(".dmg") ||
                lower.endsWith(".pkg") ||
                lower.endsWith(".tar.gz") ||
                lower.endsWith(".zip") && (
                    lower.contains("windows") ||
                        lower.contains("linux") ||
                        lower.contains("mac") ||
                        lower.contains("darwin")
                    ) -> ReleaseAssetPlatform.DESKTOP
            else -> ReleaseAssetPlatform.OTHER
        }
    }

    internal fun clearCacheForTesting() {
        cachedGitHubDownloads = null
    }
}

