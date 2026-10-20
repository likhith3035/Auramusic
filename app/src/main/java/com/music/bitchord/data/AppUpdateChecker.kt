package com.music.bitchord.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.FileProvider
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File

/**
 * BitChord ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * polls the repo's releases once per launch and compares its tag
 * against the running build.
 *
 * The update itself is also handled here: the release's `.apk` asset is
 * downloaded into the app's cache and handed to the system package installer,
 * so the whole round trip stays inside the app instead of bouncing out to a
 * browser.
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        /** The release's own Markdown body, shown as this update's "what's new". */
        val notes: String?,
        val isMandatory: Boolean = false,
    )

    private const val CACHE_SUBDIR = "updates"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/likhith3035/Auramusic/releases/latest"

    private const val ALL_RELEASES_URL =
        "https://api.github.com/repos/likhith3035/Auramusic/releases?per_page=5"

    private const val FALLBACK_VERSION_URL =
        "https://raw.githubusercontent.com/likhith3035/Auramusic/main/version.json"

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(
            val fraction: Float,
            val bytesRead: Long = 0L,
            val totalBytes: Long = 0L,
            val bytesPerSecond: Long = 0L,
        ) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    private fun fetchFromGitHubRelease(): UpdateInfo? = runCatching {
        val includeBeta = AppSettings.includeBetaUpdates.value
        val url = if (includeBeta) ALL_RELEASES_URL else LATEST_RELEASE_URL
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AuraMusic")
            .build()
        val body = Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        } ?: return@runCatching null

        val release = if (includeBeta) {
            val releases = json.parseToJsonElement(body) as? JsonArray ?: return@runCatching null
            releases.firstOrNull() as? JsonObject ?: return@runCatching null
        } else {
            json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
        }

        val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val releaseUrl = release["html_url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val apkUrl = apkAssetUrl(release)
        val notes = release["body"]?.jsonPrimitive?.contentOrNull
        val latest = tag.removePrefix("v")
        val isMandatory = notes?.contains("[MANDATORY]", ignoreCase = true) == true ||
            release["isMandatory"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true
        UpdateInfo(latest, releaseUrl, apkUrl, notes, isMandatory)
    }.getOrNull()

    private fun fetchFromFallbackJson(): UpdateInfo? = runCatching {
        val request = Request.Builder()
            .url(FALLBACK_VERSION_URL)
            .header("User-Agent", "AuraMusic")
            .build()
        val body = Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        } ?: return@runCatching null
        val root = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
        val version = root["version"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val url = root["releaseUrl"]?.jsonPrimitive?.contentOrNull
            ?: "https://github.com/likhith3035/Auramusic/releases"
        val apkUrl = root["apkUrl"]?.jsonPrimitive?.contentOrNull
        val notes = root["notes"]?.jsonPrimitive?.contentOrNull
        val isMandatory = root["isMandatory"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true ||
            notes?.contains("[MANDATORY]", ignoreCase = true) == true
        val latest = version.removePrefix("v")
        UpdateInfo(latest, url, apkUrl, notes, isMandatory)
    }.getOrNull()

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val update = fetchFromGitHubRelease() ?: fetchFromFallbackJson()
        if (update != null && isNewer(update.version, BuildConfig.VERSION_NAME)) {
            _available.value = update
            return@withContext update
        }
        null
    }

    /**
     * Wipes any APK left over from a previous run. Called once at cold start
     * so a downloaded update is only ever "Install Now" for the session that
     * downloaded it — the next launch starts clean rather than trying to work
     * out whether a leftover file is still good.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * Smart ABI-Aware APK selection:
     * 1. Inspects [android.os.Build.SUPPORTED_ABIS] (arm64-v8a, armeabi-v7a, x86_64, etc.).
     * 2. Finds the APK tailored for the primary device ABI (faster native code, 50% smaller download).
     * 3. Falls back to a universal APK if available.
     * 4. Falls back to any uploaded APK.
     */
    private fun apkAssetUrl(release: JsonObject): String? = runCatching {
        val assets = release["assets"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.filter { asset ->
                asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true &&
                    asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
            } ?: emptyList()

        if (assets.isEmpty()) return@runCatching null

        val deviceAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }

        // 1. Device-specific architecture match (e.g. arm64-v8a, armeabi-v7a)
        for (abi in deviceAbis) {
            val matched = assets.firstOrNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
                name.contains(abi) || (abi == "arm64-v8a" && (name.contains("arm64") || name.contains("aarch64")))
            }
            if (matched != null) {
                return@runCatching matched["browser_download_url"]?.jsonPrimitive?.contentOrNull
            }
        }

        // 2. Universal build fallback
        val universal = assets.firstOrNull { asset ->
            val name = asset["name"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
            name.contains("universal")
        }
        if (universal != null) {
            return@runCatching universal["browser_download_url"]?.jsonPrimitive?.contentOrNull
        }

        // 3. Fallback to first uploaded APK
        assets.firstOrNull()?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download] and system notifications.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f, 0L, 0L, 0L)
        AppUpdateNotificationHelper.showProgress(context, info.version, 0, 0L, 0L, 0L)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "bitchord-${info.version}.apk")

            val request = Request.Builder().url(url).build()
            var total = 0L
            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                total = body.contentLength().takeIf { it > 0 } ?: 0L

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        var lastNotifyTime = 0L
                        var lastSpeedTime = SystemClock.elapsedRealtime()
                        var bytesSinceSpeedSample = 0L
                        var currentSpeed = 0L

                        while (true) {
                            if (downloadCancelled) {
                                _download.value = DownloadState.Idle
                                AppUpdateNotificationHelper.dismiss(context)
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readTotal += read

                            val now = SystemClock.elapsedRealtime()
                            bytesSinceSpeedSample += read
                            val speedElapsed = now - lastSpeedTime
                            if (speedElapsed >= 400) {
                                currentSpeed = (bytesSinceSpeedSample * 1000L) / speedElapsed
                                lastSpeedTime = now
                                bytesSinceSpeedSample = 0L
                            }

                            val frac = if (total > 0L) {
                                (readTotal.toFloat() / total).coerceIn(0f, 1f)
                            } else 0f

                            _download.value = DownloadState.Downloading(
                                fraction = frac,
                                bytesRead = readTotal,
                                totalBytes = total,
                                bytesPerSecond = currentSpeed,
                            )

                            if (now - lastNotifyTime >= 500) {
                                lastNotifyTime = now
                                val percent = (frac * 100).toInt()
                                AppUpdateNotificationHelper.showProgress(
                                    context = context,
                                    version = info.version,
                                    percent = percent,
                                    bytesRead = readTotal,
                                    totalBytes = total,
                                    bytesPerSec = currentSpeed,
                                )
                            }
                        }
                    }
                }
            }

            // Verify integrity
            if (total > 0L && target.length() < total) {
                error("Downloaded APK is incomplete: got ${target.length()} of $total bytes")
            }

            _download.value = DownloadState.Ready(target)
            AppUpdateNotificationHelper.showReady(context, info.version, target)
        }.onFailure { error ->
            AppUpdateNotificationHelper.dismiss(context)
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload(context: Context? = null) {
        downloadCancelled = true
        _download.value = DownloadState.Idle
        context?.let { AppUpdateNotificationHelper.dismiss(it) }
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload(context: Context? = null) {
        _download.value = DownloadState.Idle
        context?.let { AppUpdateNotificationHelper.dismiss(it) }
    }

    /**
     * Hands a downloaded APK to the system installer.
     *
     * Sideloaded apps need the user's blessing per app ("install unknown apps");
     * without it the installer intent silently does nothing on most ROMs, so
     * the user is sent to that one switch first and taps Install again after.
     */
    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
        )
    }

    /** A version split into its numeric dotted parts and whether it carries a "-suffix" (e.g. "-beta2"). */
    private data class ParsedVersion(val parts: List<Int>, val isPreRelease: Boolean)

    private fun parseVersion(raw: String): ParsedVersion {
        val dash = raw.indexOf('-')
        val base = if (dash >= 0) raw.substring(0, dash) else raw
        return ParsedVersion(base.split(".").map { it.toIntOrNull() ?: 0 }, dash >= 0)
    }

    /**
     * Numeric, dot-separated comparison — "1.10" outranks "1.9" — with one
     * extra rule: a "-betaN" build (see the debug build type's
     * `versionNameSuffix` in app/build.gradle.kts) is treated as older than a
     * plain release of the same numbers, since the beta by definition predates
     * the tag it was testing toward. Without this, a beta and the release it
     * matches compare equal and testers never get nudged onto the real build.
     */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.parts.size, c.parts.size)) {
            val a = l.parts.getOrElse(i) { 0 }
            val b = c.parts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        // Identical numbers: a plain tag outranks its own beta tag.
        return !l.isPreRelease && c.isPreRelease
    }
}
