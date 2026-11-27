package com.music.bitchord.data.telemetry

import com.music.bitchord.data.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class AnalyticsConfigSource {
    NONE,
    BUILD_CONFIG,
    RUNTIME_OVERRIDE,
}

data class AnalyticsConnectionStatus(
    val isConfigured: Boolean = false,
    val source: AnalyticsConfigSource = AnalyticsConfigSource.NONE,
    val host: String = AuraAnalytics.DEFAULT_POSTHOG_HOST,
    val maskedKey: String = "",
    val queuedEvents: Int = 0,
    val sentThisSession: Int = 0,
    val lastHttpCode: Int? = null,
    val lastError: String? = null,
    val lastSuccessAtMs: Long? = null,
)

/**
 * Privacy-strict, cross-platform (Android + Desktop) PostHog event ingestion client.
 *
 * Uses only anonymous randomized installation UUIDs (shared with [AuraTelemetryClient])
 * and never accepts song titles, artist names, search query text, emails, or locations.
 */
object AuraAnalytics {

    const val DEFAULT_POSTHOG_HOST = "https://us.i.posthog.com"
    private const val LIB_NAME = "auramusic-kmp"
    private const val QUEUE_FILE_NAME = "aura_analytics_queue.jsonl"
    private const val CONFIG_FILE_NAME = "aura_posthog_config.properties"
    private const val INSTALLED_MARKER_NAME = "aura_analytics_installed.txt"
    private const val MAX_QUEUE_SIZE = 200
    private const val BATCH_SIZE = 25
    private const val FLUSH_DEBOUNCE_MS = 4_000L

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileLock = Any()
    private val networkMutex = Mutex()
    private val launchTrackedThisProcess = AtomicBoolean(false)
    private val sentSessionCounter = AtomicInteger(0)

    private var storageDir: File? = null
    private var buildApiKey: String = ""
    private var buildHost: String = DEFAULT_POSTHOG_HOST
    private var runtimeApiKey: String = ""
    private var runtimeHost: String = ""
    private var appVersion: String = "unknown"
    private var platform: String = "Unknown"
    private var osVersion: String = System.getProperty("os.name") ?: "Unknown"
    private var installIdProvider: () -> String = { AuraTelemetryClient.getOrCreateInstallId() }

    private var scheduledFlushJob: Job? = null
    private var lastHttpCode: Int? = null
    private var lastError: String? = null
    private var lastSuccessAtMs: Long? = null

    private val _status = MutableStateFlow(AnalyticsConnectionStatus())
    val status: StateFlow<AnalyticsConnectionStatus> = _status.asStateFlow()

    private val httpClient by lazy {
        Http.client.newBuilder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun init(
        directory: File,
        buildPosthogApiKey: String,
        buildPosthogHost: String = DEFAULT_POSTHOG_HOST,
        version: String,
        platform: String,
        osVersion: String = System.getProperty("os.name") ?: "Unknown",
        distinctIdProvider: () -> String = { AuraTelemetryClient.getOrCreateInstallId() },
    ) {
        synchronized(fileLock) {
            directory.mkdirs()
            storageDir = directory
            buildApiKey = buildPosthogApiKey.trim()
            buildHost = normalizeHost(buildPosthogHost)
            appVersion = version.trim().ifBlank { "unknown" }
            this.platform = platform.trim().ifBlank { "Unknown" }
            this.osVersion = osVersion.trim().ifBlank { "Unknown" }
            installIdProvider = distinctIdProvider
            loadRuntimeConfigLocked()
        }
        publishStatus()
    }

    fun isConfigured(): Boolean = activeCredentials() != null

    fun normalizeHost(raw: String?): String {
        val trimmed = raw?.trim().orEmpty().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_POSTHOG_HOST
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun maskKey(key: String): String {
        val clean = key.trim()
        if (clean.isEmpty()) return ""
        return if (clean.length <= 10) {
            clean.take(4) + "..."
        } else {
            clean.take(8) + "..." + clean.takeLast(4)
        }
    }

    fun trackAppLaunch() {
        val creds = activeCredentials() ?: return
        val dir = storageDir
        if (dir != null) {
            val shouldRecordInstall = synchronized(fileLock) {
                val marker = File(dir, INSTALLED_MARKER_NAME)
                if (!marker.exists()) {
                    runCatching { marker.writeText(Instant.now().toString()) }
                    true
                } else {
                    false
                }
            }
            if (shouldRecordInstall) {
                enqueueEvent(
                    name = "app_installed",
                    properties = mapOf(
                        "install_version" to JsonPrimitive(appVersion),
                    ),
                    flushImmediately = true,
                )
            }
        }

        if (launchTrackedThisProcess.compareAndSet(false, true)) {
            val personProps = buildJsonObject {
                put("app_version", appVersion)
                put("platform", platform)
                put("os_version", osVersion)
            }
            val onceProps = buildJsonObject {
                put("first_version", appVersion)
                put("first_platform", platform)
            }
            enqueueEvent(
                name = "app_opened",
                properties = mapOf(
                    "\$set" to personProps,
                    "\$set_once" to onceProps,
                    "host" to JsonPrimitive(creds.second),
                ),
                flushImmediately = true,
            )
        }
    }

    fun trackPlayCounted() {
        enqueueEvent(name = "track_played")
    }

    fun trackDownloadCompleted(format: String? = null) {
        val cleanFormat = format?.trim()?.takeIf { it.isNotBlank() }?.take(16) ?: "audio"
        enqueueEvent(
            name = "track_downloaded",
            properties = mapOf("format" to JsonPrimitive(cleanFormat)),
        )
    }

    fun trackSearchSubmitted(source: String) {
        val cleanSource = source.trim().takeIf { it.isNotBlank() }?.take(24) ?: "unknown"
        enqueueEvent(
            name = "search_performed",
            properties = mapOf("source" to JsonPrimitive(cleanSource)),
        )
    }

    fun trackFeatureUsed(feature: String) {
        val cleanFeature = feature.trim().takeIf { it.isNotBlank() }?.take(32) ?: return
        enqueueEvent(
            name = "feature_used",
            properties = mapOf("feature" to JsonPrimitive(cleanFeature)),
        )
    }

    suspend fun flushNow(): Boolean = withContext(Dispatchers.IO) {
        flushInternal()
    }

    suspend fun saveAndVerifyConfig(
        candidateKey: String,
        candidateHost: String = DEFAULT_POSTHOG_HOST,
    ): Result<Int> = withContext(Dispatchers.IO) {
        val trimmedKey = candidateKey.trim()
        if (trimmedKey.length < 8 || trimmedKey.contains(' ') || trimmedKey.contains('\n')) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter a valid PostHog Project API key (starts with phc_)."),
            )
        }
        val cleanHost = normalizeHost(candidateHost)
        val verification = sendSingleCaptureTest(trimmedKey, cleanHost)
        verification.onSuccess { code ->
            synchronized(fileLock) {
                runtimeApiKey = trimmedKey
                runtimeHost = cleanHost
                saveRuntimeConfigLocked(trimmedKey, cleanHost)
                lastHttpCode = code
                lastError = null
                lastSuccessAtMs = System.currentTimeMillis()
            }
            publishStatus()
            flushInternal()
        }.onFailure { err ->
            synchronized(fileLock) {
                lastError = err.message ?: "Verification failed"
            }
            publishStatus()
        }
        verification
    }

    fun clearRuntimeConfig() {
        synchronized(fileLock) {
            runtimeApiKey = ""
            runtimeHost = ""
            storageDir?.let { File(it, CONFIG_FILE_NAME).delete() }
            lastError = null
            lastHttpCode = null
        }
        publishStatus()
    }

    internal fun resetForTesting() {
        scheduledFlushJob?.cancel()
        scheduledFlushJob = null
        launchTrackedThisProcess.set(false)
        sentSessionCounter.set(0)
        synchronized(fileLock) {
            storageDir = null
            buildApiKey = ""
            buildHost = DEFAULT_POSTHOG_HOST
            runtimeApiKey = ""
            runtimeHost = ""
            lastHttpCode = null
            lastError = null
            lastSuccessAtMs = null
        }
        _status.value = AnalyticsConnectionStatus()
    }

    private fun enqueueEvent(
        name: String,
        properties: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        flushImmediately: Boolean = false,
    ) {
        if (activeCredentials() == null) return
        val dir = storageDir ?: return
        val distinctId = runCatching { installIdProvider() }.getOrElse { "" }.trim()
        if (distinctId.isBlank()) return

        val propsObj = buildJsonObject {
            put("\$lib", LIB_NAME)
            put("app_version", appVersion)
            put("platform", platform)
            put("os_version", osVersion)
            for ((k, v) in properties) {
                if (k != "host") put(k, v)
            }
        }
        val eventObj = buildJsonObject {
            put("event", name)
            put("distinct_id", distinctId)
            put("timestamp", Instant.now().toString())
            put("properties", propsObj)
        }

        val queueSizeAfter: Int
        synchronized(fileLock) {
            val queueFile = File(dir, QUEUE_FILE_NAME)
            val lines = if (queueFile.exists()) {
                runCatching { queueFile.readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val updated = (lines + eventObj.toString()).takeLast(MAX_QUEUE_SIZE)
            runCatching {
                queueFile.writeText(updated.joinToString("\n", postfix = "\n"))
            }
            queueSizeAfter = updated.size
        }
        publishStatus()

        if (flushImmediately || queueSizeAfter >= 5) {
            scheduledFlushJob?.cancel()
            scope.launch { flushInternal() }
        } else if (scheduledFlushJob?.isActive != true) {
            scheduledFlushJob = scope.launch {
                delay(FLUSH_DEBOUNCE_MS)
                flushInternal()
            }
        }
    }

    private suspend fun flushInternal(): Boolean = networkMutex.withLock {
        val creds = activeCredentials() ?: return@withLock false
        val dir = storageDir ?: return@withLock false
        val queueFile = File(dir, QUEUE_FILE_NAME)

        val pendingLines = synchronized(fileLock) {
            if (!queueFile.exists()) return@synchronized emptyList()
            runCatching { queueFile.readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())
        }
        if (pendingLines.isEmpty()) return@withLock true

        var remaining = pendingLines
        while (remaining.isNotEmpty()) {
            val chunk = remaining.take(BATCH_SIZE)
            val events = chunk.mapNotNull { line ->
                runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
            }
            if (events.isEmpty()) {
                remaining = remaining.drop(chunk.size)
                replaceQueueFile(queueFile, remaining)
                continue
            }

            val payload = buildJsonObject {
                put("api_key", creds.first)
                put("historical_migration", false)
                put("batch", JsonArray(events))
            }

            val result = postJson("${creds.second}/batch/", payload)
            val code = result.first
            val err = result.second
            if (code in 200..299) {
                sentSessionCounter.addAndGet(events.size)
                remaining = remaining.drop(chunk.size)
                replaceQueueFile(queueFile, remaining)
                synchronized(fileLock) {
                    lastHttpCode = code
                    lastError = null
                    lastSuccessAtMs = System.currentTimeMillis()
                }
                publishStatus()
            } else if (code == 400 || code == 401 || code == 403) {
                // Drop invalid/unauthorized batches to avoid retrying bad payloads/keys forever,
                // while surfacing the real HTTP error in status.
                remaining = remaining.drop(chunk.size)
                replaceQueueFile(queueFile, remaining)
                synchronized(fileLock) {
                    lastHttpCode = code
                    lastError = err ?: "PostHog rejected batch (HTTP $code)"
                }
                publishStatus()
                return@withLock false
            } else {
                synchronized(fileLock) {
                    if (code != null) lastHttpCode = code
                    lastError = err ?: "Network error flushing PostHog batch"
                }
                publishStatus()
                return@withLock false
            }
        }
        true
    }

    private fun replaceQueueFile(queueFile: File, remaining: List<String>) {
        synchronized(fileLock) {
            runCatching {
                if (remaining.isEmpty()) {
                    queueFile.delete()
                } else {
                    queueFile.writeText(remaining.joinToString("\n", postfix = "\n"))
                }
            }
        }
    }

    private fun sendSingleCaptureTest(apiKey: String, host: String): Result<Int> {
        val distinctId = runCatching { installIdProvider() }.getOrElse { "admin-verifier" }
            .ifBlank { "admin-verifier" }
        val payload = buildJsonObject {
            put("api_key", apiKey)
            put("event", "founder_telemetry_verified")
            put("distinct_id", distinctId)
            put("timestamp", Instant.now().toString())
            put(
                "properties",
                buildJsonObject {
                    put("\$lib", LIB_NAME)
                    put("app_version", appVersion)
                    put("platform", platform)
                    put("os_version", osVersion)
                },
            )
        }
        val (code, error) = postJson("$host/capture/", payload)
        return if (code != null && code in 200..299) {
            Result.success(code)
        } else {
            Result.failure(IllegalStateException(error ?: "PostHog verification failed"))
        }
    }

    private fun postJson(url: String, payload: JsonObject): Pair<Int?, String?> {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/$appVersion ($platform)")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    res.code to null
                } else {
                    val bodySnippet = runCatching { res.body.string().take(160) }.getOrNull().orEmpty()
                    res.code to "HTTP ${res.code}" + (if (bodySnippet.isNotBlank()) ": $bodySnippet" else "")
                }
            }
        } catch (e: Exception) {
            null to (e.message ?: "Request failed")
        }
    }

    private fun activeCredentials(): Pair<String, String>? {
        synchronized(fileLock) {
            if (runtimeApiKey.isNotBlank()) {
                return runtimeApiKey to runtimeHost.ifBlank { DEFAULT_POSTHOG_HOST }
            }
            if (buildApiKey.isNotBlank()) {
                return buildApiKey to buildHost.ifBlank { DEFAULT_POSTHOG_HOST }
            }
            return null
        }
    }

    private fun loadRuntimeConfigLocked() {
        val dir = storageDir ?: return
        val file = File(dir, CONFIG_FILE_NAME)
        if (!file.exists()) {
            runtimeApiKey = ""
            runtimeHost = ""
            return
        }
        runCatching {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            runtimeApiKey = props.getProperty("apiKey").orEmpty().trim()
            val hostProp = props.getProperty("host").orEmpty().trim()
            runtimeHost = if (hostProp.isNotBlank()) normalizeHost(hostProp) else DEFAULT_POSTHOG_HOST
        }
    }

    private fun saveRuntimeConfigLocked(apiKey: String, host: String) {
        val dir = storageDir ?: return
        runCatching {
            val props = Properties()
            props.setProperty("apiKey", apiKey)
            props.setProperty("host", host)
            File(dir, CONFIG_FILE_NAME).outputStream().use { out ->
                props.store(out, "AuraMusic founder PostHog verification override")
            }
        }
    }

    private fun queuedCountLocked(): Int {
        val dir = storageDir ?: return 0
        val file = File(dir, QUEUE_FILE_NAME)
        if (!file.exists()) return 0
        return runCatching { file.readLines().count { it.isNotBlank() } }.getOrDefault(0)
    }

    private fun publishStatus() {
        _status.value = synchronized(fileLock) {
            val source = when {
                runtimeApiKey.isNotBlank() -> AnalyticsConfigSource.RUNTIME_OVERRIDE
                buildApiKey.isNotBlank() -> AnalyticsConfigSource.BUILD_CONFIG
                else -> AnalyticsConfigSource.NONE
            }
            val key = when (source) {
                AnalyticsConfigSource.RUNTIME_OVERRIDE -> runtimeApiKey
                AnalyticsConfigSource.BUILD_CONFIG -> buildApiKey
                AnalyticsConfigSource.NONE -> ""
            }
            val host = when (source) {
                AnalyticsConfigSource.RUNTIME_OVERRIDE -> runtimeHost.ifBlank { DEFAULT_POSTHOG_HOST }
                AnalyticsConfigSource.BUILD_CONFIG -> buildHost.ifBlank { DEFAULT_POSTHOG_HOST }
                AnalyticsConfigSource.NONE -> buildHost.ifBlank { DEFAULT_POSTHOG_HOST }
            }
            AnalyticsConnectionStatus(
                isConfigured = source != AnalyticsConfigSource.NONE,
                source = source,
                host = host,
                maskedKey = maskKey(key),
                queuedEvents = queuedCountLocked(),
                sentThisSession = sentSessionCounter.get(),
                lastHttpCode = lastHttpCode,
                lastError = lastError,
                lastSuccessAtMs = lastSuccessAtMs,
            )
        }
    }
}
