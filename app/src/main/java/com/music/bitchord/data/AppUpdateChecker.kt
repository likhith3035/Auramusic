package com.music.bitchord.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

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
        val expectedSha256: String? = null,
    )

    private val updateHttpClient: OkHttpClient by lazy {
        Http.client.newBuilder()
            .addNetworkInterceptor { chain ->
                val url = chain.request().url.toString()
                if (!isAllowedUpdateUrl(url)) {
                    throw SecurityException("Blocked redirect to untrusted update host")
                }
                chain.proceed(chain.request())
            }
            .build()
    }

    internal fun isAllowedUpdateUrl(rawUrl: String?): Boolean {
        val raw = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return false
        return host == "github.com" ||
            host.endsWith(".github.com") ||
            host == "githubusercontent.com" ||
            host.endsWith(".githubusercontent.com")
    }

    internal fun sanitizeVersionForFilename(rawVersion: String): String {
        val cleaned = rawVersion.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace("..", "_")
            .trim('.')
            .take(48)
        return cleaned.ifBlank { "latest" }
    }

    private fun parseSha256Digest(raw: String?): String? {
        val candidate = raw?.trim()?.removePrefix("sha256:")?.lowercase(Locale.ROOT) ?: return null
        return candidate.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
    }

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
        val body = updateHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        } ?: return@runCatching null

        val release = if (includeBeta) {
            val releases = json.parseToJsonElement(body) as? JsonArray ?: return@runCatching null
            releases.firstOrNull() as? JsonObject ?: return@runCatching null
        } else {
            json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
        }

        val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val releaseUrl = release["html_url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf(::isAllowedUpdateUrl) ?: return@runCatching null
        val (apkUrl, sha256) = apkAssetSelection(release)
        val notes = release["body"]?.jsonPrimitive?.contentOrNull
        val latest = tag.removePrefix("v")
        val isMandatory = notes?.contains("[MANDATORY]", ignoreCase = true) == true ||
            release["isMandatory"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true
        UpdateInfo(latest, releaseUrl, apkUrl, notes, isMandatory, sha256)
    }.getOrNull()

    private fun fetchFromFallbackJson(): UpdateInfo? = runCatching {
        val request = Request.Builder()
            .url(FALLBACK_VERSION_URL)
            .header("User-Agent", "AuraMusic")
            .build()
        val body = updateHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        } ?: return@runCatching null
        val root = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
        val version = root["version"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val url = (root["releaseUrl"]?.jsonPrimitive?.contentOrNull ?: "https://github.com/likhith3035/Auramusic/releases")
            .takeIf(::isAllowedUpdateUrl)
            ?: "https://github.com/likhith3035/Auramusic/releases"
        val apkUrl = root["apkUrl"]?.jsonPrimitive?.contentOrNull?.takeIf(::isAllowedUpdateUrl)
        val expectedSha256 = parseSha256Digest(root["sha256"]?.jsonPrimitive?.contentOrNull)
        val notes = root["notes"]?.jsonPrimitive?.contentOrNull
        val isMandatory = root["isMandatory"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true ||
            notes?.contains("[MANDATORY]", ignoreCase = true) == true
        val latest = version.removePrefix("v")
        UpdateInfo(latest, url, apkUrl, notes, isMandatory, expectedSha256)
    }.getOrNull()

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val update = fetchFromGitHubRelease() ?: fetchFromFallbackJson()
        if (update != null && isNewer(update.version, BuildConfig.VERSION_NAME)) {
            // Respect the user's "Skip this version" choice — unless the
            // release is marked as mandatory.
            val skipped = AppSettings.skippedUpdateVersion.value
            if (!update.isMandatory && skipped == update.version) {
                return@withContext null
            }
            _available.value = update
            return@withContext update
        }
        null
    }

    /**
     * Wipes any APK left over from a previous run, **unless** the cached file
     * matches the version of the currently-available update. This eliminates
     * the frustrating loop where users download the APK, fail to install
     * (e.g. the system dialog was dismissed), re-launch, and are forced to
     * download the whole thing again.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, CACHE_SUBDIR)
        val files = dir.listFiles() ?: return@withContext
        // If there's already an update available from a previous check, see if
        // the cached APK matches its version so we can skip re-downloading.
        val keepVersion = _available.value?.version?.let(::sanitizeVersionForFilename)
        for (file in files) {
            if (keepVersion != null && file.name.contains(keepVersion)) {
                // Cached APK belongs to the latest update — verify it's intact.
                if (file.length() > 0 && verifyApkSignatureMatchesInstalled(context, file)) {
                    _download.value = DownloadState.Ready(file)
                    continue
                }
            }
            file.delete()
        }
    }

    /**
     * Smart ABI-Aware APK selection:
     * 1. Inspects [android.os.Build.SUPPORTED_ABIS] (arm64-v8a, armeabi-v7a, x86_64, etc.).
     * 2. Finds the APK tailored for the primary device ABI (faster native code, 50% smaller download).
     * 3. Falls back to a universal APK if available.
     * 4. Falls back to any uploaded APK.
     */
    private fun apkAssetSelection(release: JsonObject): Pair<String?, String?> = runCatching {
        val assets = release["assets"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.filter { asset ->
                asset["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true &&
                    asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded" &&
                    isAllowedUpdateUrl(asset["browser_download_url"]?.jsonPrimitive?.contentOrNull)
            } ?: emptyList()

        if (assets.isEmpty()) return@runCatching (null to null)

        fun pick(asset: JsonObject?): Pair<String?, String?>? {
            if (asset == null) return null
            val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull
                ?.takeIf(::isAllowedUpdateUrl) ?: return null
            val digest = parseSha256Digest(asset["digest"]?.jsonPrimitive?.contentOrNull)
            return url to digest
        }

        val deviceAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }

        // 1. Device-specific architecture match (e.g. arm64-v8a, armeabi-v7a)
        for (abi in deviceAbis) {
            val matched = assets.firstOrNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
                name.contains(abi) || (abi == "arm64-v8a" && (name.contains("arm64") || name.contains("aarch64")))
            }
            pick(matched)?.let { return@runCatching it }
        }

        // 2. Universal build fallback
        val universal = assets.firstOrNull { asset ->
            val name = asset["name"]?.jsonPrimitive?.contentOrNull?.lowercase().orEmpty()
            name.contains("universal")
        }
        pick(universal)?.let { return@runCatching it }

        // 3. Fallback to first uploaded APK
        pick(assets.firstOrNull()) ?: (null to null)
    }.getOrElse { null to null }

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download] and system notifications.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        if (!isAllowedUpdateUrl(url)) {
            _download.value = DownloadState.Failed("Blocked untrusted update URL")
            return@withContext
        }
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f, 0L, 0L, 0L)
        AppUpdateNotificationHelper.showProgress(context, info.version, 0, 0L, 0L, 0L)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val safeVersion = sanitizeVersionForFilename(info.version)
            val target = File(dir, "bitchord-$safeVersion.apk")
            check(target.canonicalFile.parentFile == dir.canonicalFile) { "Invalid update path" }

            val request = Request.Builder().url(url).build()
            var total = 0L
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            updateHttpClient.newCall(request).execute().use { response ->
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
                            sha256Digest.update(buffer, 0, read)
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
                target.delete()
                error("Downloaded APK is incomplete: got ${target.length()} of $total bytes")
            }
            info.expectedSha256?.let { expected ->
                val actual = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    target.delete()
                    error("Downloaded APK failed SHA-256 verification")
                }
            }
            if (!verifyApkSignatureMatchesInstalled(context, target)) {
                target.delete()
                error("Downloaded APK signature does not match installed app")
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
     * Hands a downloaded APK to the system's [PackageInstaller] session API.
     *
     * Unlike the old `ACTION_VIEW` approach, the session API is guaranteed to
     * show the system install confirmation on every ROM (Xiaomi, Samsung,
     * Huawei, stock AOSP, etc.). The result arrives asynchronously in
     * [AppInstallReceiver].
     *
     * On Android 8+ the user must have enabled "Install Unknown Apps" for
     * this app; without it the session commit is rejected, so we pre-flight
     * that check and send them to the toggle first.
     */
    fun installApk(context: Context, file: File) {
        if (!verifyApkSignatureMatchesInstalled(context, file)) {
            _download.value = DownloadState.Failed("Downloaded APK signature mismatch — uninstall the old version first")
            file.delete()
            return
        }

        // Pre-flight: ensure "Install Unknown Apps" is allowed for this app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            _download.value = DownloadState.Failed("Please allow AuraMusic to install updates, then tap Install again")
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            ).apply {
                setSize(file.length())
                // On API 31+ mark this as an update from the same package so
                // the system can show a streamlined confirmation.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("aura_update", 0, file.length()).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                        session.fsync(output)
                    }
                }

                // Build a PendingIntent that delivers the result to our receiver.
                val callbackIntent = Intent(context, AppInstallReceiver::class.java).apply {
                    action = AppInstallReceiver.ACTION_INSTALL_STATUS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pendingIntent.intentSender)
            }
            Log.i("AppUpdateChecker", "PackageInstaller session $sessionId committed")
        }.onFailure { error ->
            Log.e("AppUpdateChecker", "PackageInstaller session failed", error)
            // Fallback to the classic ACTION_VIEW intent.
            installApkLegacy(context, file)
        }
    }

    /**
     * Legacy `ACTION_VIEW` fallback, used only when [PackageInstaller] fails
     * (e.g. on heavily customised ROMs that break the session API).
     */
    private fun installApkLegacy(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(installIntent)
    }

    /** Called from [AppInstallReceiver] when the install completes. */
    internal fun onInstallSuccess() {
        _download.value = DownloadState.Idle
    }

    /** Called from [AppInstallReceiver] when the install fails. */
    internal fun onInstallFailed(message: String) {
        _download.value = DownloadState.Failed(message)
    }

    @Suppress("DEPRECATION")
    private fun signingDigests(info: PackageInfo?): Set<String> {
        if (info == null) return emptySet()
        val sigs: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
        } else {
            info.signatures ?: emptyArray()
        }
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun verifyApkSignatureMatchesInstalled(context: Context, apkFile: File): Boolean = runCatching {
        if (!apkFile.isFile) return@runCatching false
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val installedInfo = pm.getPackageInfo(context.packageName, flags)
        val installedDigests = signingDigests(installedInfo)
        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return@runCatching false
        if (archiveInfo.packageName != context.packageName) return@runCatching false
        val archiveDigests = signingDigests(archiveInfo)
        installedDigests.isNotEmpty() && archiveDigests.isNotEmpty() &&
            installedDigests.any { it in archiveDigests }
    }.getOrDefault(false)

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
