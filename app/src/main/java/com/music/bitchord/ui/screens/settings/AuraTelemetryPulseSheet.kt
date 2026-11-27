package com.music.bitchord.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.telemetry.AnalyticsConfigSource
import com.music.bitchord.data.telemetry.AnalyticsConnectionStatus
import com.music.bitchord.data.telemetry.AuraAnalytics
import com.music.bitchord.data.telemetry.AuraTelemetryClient
import com.music.bitchord.data.telemetry.AuraTelemetryRepository
import com.music.bitchord.data.telemetry.DailyHistoryItem
import com.music.bitchord.data.telemetry.GitHubDownloadsSummary
import com.music.bitchord.data.telemetry.LiveNetworkPulse
import com.music.bitchord.data.telemetry.TelemetryStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraTelemetryPulseSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val analyticsStatus by AuraAnalytics.status.collectAsStateWithLifecycle()

    var stats by remember { mutableStateOf<TelemetryStats?>(null) }
    var livePulse by remember { mutableStateOf<LiveNetworkPulse?>(null) }
    var livePulseError by remember { mutableStateOf<String?>(null) }
    var githubDownloads by remember { mutableStateOf<GitHubDownloadsSummary?>(null) }
    var githubDownloadsError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingSeconds by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var pingNotice by remember { mutableStateOf<String?>(null) }
    var isSharingCard by remember { mutableStateOf(false) }
    var reportCopiedNotice by remember { mutableStateOf<String?>(null) }

    val onlineCountAnim = remember { Animatable(0f) }
    val downloadCountAnim = remember { Animatable(0f) }
    val activeCountAnim = remember { Animatable(0f) }
    val allTimeCountAnim = remember { Animatable(0f) }

    // Track elapsed time during loading to show smart progressive wake-up feedback
    LaunchedEffect(isLoading) {
        if (isLoading) {
            loadingSeconds = 0
            while (isActive && isLoading) {
                delay(1000)
                loadingSeconds++
            }
        }
    }

    fun refreshStats(forceGitHubRefresh: Boolean = false) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            livePulseError = null
            githubDownloadsError = null

            val snapshot = AuraTelemetryRepository.fetchDashboardSnapshot(forceRefresh = forceGitHubRefresh)
            isLoading = false

            snapshot.livePulseResult.fold(
                onSuccess = { live ->
                    livePulse = live
                    launch {
                        onlineCountAnim.snapTo(0f)
                        onlineCountAnim.animateTo(live.onlineNow.toFloat(), tween(700))
                    }
                },
                onFailure = { err ->
                    livePulseError = err.message ?: "Presence unreachable"
                },
            )

            snapshot.githubDownloadsResult.fold(
                onSuccess = { dl ->
                    githubDownloads = dl
                    launch {
                        downloadCountAnim.snapTo(0f)
                        downloadCountAnim.animateTo(dl.totalDownloads.toFloat(), tween(800))
                    }
                },
                onFailure = { err ->
                    githubDownloadsError = err.message ?: "GitHub API unavailable"
                },
            )

            snapshot.dailyHeartbeatResult.fold(
                onSuccess = { res ->
                    stats = res
                    launch {
                        activeCountAnim.snapTo(0f)
                        activeCountAnim.animateTo(
                            targetValue = res.activeToday.toFloat(),
                            animationSpec = tween(800),
                        )
                    }
                    launch {
                        allTimeCountAnim.snapTo(0f)
                        allTimeCountAnim.animateTo(
                            targetValue = res.allTimeDevices.toFloat(),
                            animationSpec = tween(900),
                        )
                    }
                },
                onFailure = { err ->
                    errorMessage = err.message ?: "Could not connect to Render daily stats"
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshStats(forceGitHubRefresh = false)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Header
            TelemetryHeader(
                isLoading = isLoading,
                onRefresh = { refreshStats(forceGitHubRefresh = true) },
                onClose = onDismiss,
            )

            Spacer(Modifier.height(14.dp))

            // Multi-source real connection status ribbon
            ServerStatusRibbon(
                stats = stats,
                livePulse = livePulse,
                githubDownloads = githubDownloads,
                isLoading = isLoading,
                loadingSeconds = loadingSeconds,
                errorMessage = errorMessage,
            )

            Spacer(Modifier.height(16.dp))

            // Primary Real-Time Hero Grid (Live Online Now + GitHub Release Downloads)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val live = livePulse
                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    title = "ONLINE NOW",
                    value = if (live != null) onlineCountAnim.value.roundToInt().toString() else "--",
                    subtitle = when {
                        live != null -> "${live.androidOnline} Android · ${live.pcOnline} PC"
                        livePulseError != null -> "Presence offline"
                        else -> "Checking presence..."
                    },
                    tag = if (live != null) "LIVE" else "...",
                    tagColor = if (live != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                    accentColor = Color(0xFF10B981),
                )

                val dl = githubDownloads
                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    title = "GITHUB DOWNLOADS",
                    value = if (dl != null) downloadCountAnim.value.roundToInt().toString() else "--",
                    subtitle = when {
                        dl != null -> "${dl.androidDownloads} APK · ${dl.desktopDownloads} PC"
                        githubDownloadsError != null -> "GitHub API unreachable"
                        else -> "Loading releases..."
                    },
                    tag = "RELEASES",
                    tagColor = Color(0xFF8B5CF6),
                    accentColor = Color(0xFF8B5CF6),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Secondary Heartbeat Grid (Render Daily Check-ins)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    title = "ACTIVE TODAY",
                    value = if (stats != null) activeCountAnim.value.roundToInt().toString() else "--",
                    subtitle = "Render daily heartbeats",
                    tag = "DAU",
                    tagColor = Color(0xFF10B981),
                    accentColor = MaterialTheme.colorScheme.primary,
                )

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    title = "RENDER DEVICES",
                    value = if (stats != null) allTimeCountAnim.value.roundToInt().toString() else "--",
                    subtitle = "Since last server restart",
                    tag = "EPHEMERAL",
                    tagColor = Color(0xFF06B6D4),
                    accentColor = Color(0xFF06B6D4),
                )
            }

            if (stats != null && stats?.allTimeDevices == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Render Free resets its local stats.json file whenever the container sleeps. Use PostHog below for permanent historical DAU/WAU/MAU and usage counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Live Release Downloads & Listen Together Breakdown
            if (githubDownloads != null || livePulse != null) {
                ReleasesAndJamCard(
                    downloads = githubDownloads,
                    downloadsError = githubDownloadsError,
                    livePulse = livePulse,
                )
                Spacer(Modifier.height(16.dp))
            }

            // PostHog $0 Cloud Analytics Setup, Status & Live Verifier
            PostHogAnalyticsCard(
                status = analyticsStatus,
                onOpenDashboard = {
                    val dashboardUrl = analyticsStatus.host
                        .replace("us.i.posthog.com", "us.posthog.com")
                        .replace("eu.i.posthog.com", "eu.posthog.com")
                    uriHandler.openUri(dashboardUrl)
                },
            )

            Spacer(Modifier.height(16.dp))

            // 14-Day Activity Spline Chart
            val history = stats?.dailyHistory.orEmpty()
            ActivityHistoryCard(history = history)

            Spacer(Modifier.height(16.dp))

            // Version & Platform Matrix
            VersionPlatformCard(
                versions = stats?.versions.orEmpty(),
                platforms = stats?.platforms.orEmpty(),
                totalActive = stats?.activeToday ?: 1,
            )

            Spacer(Modifier.height(16.dp))

            // Live Test Heartbeat Action Card
            HeartbeatTestCard(
                isPinging = isPinging,
                pingNotice = pingNotice,
                onPingNow = {
                    coroutineScope.launch {
                        isPinging = true
                        pingNotice = null
                        AuraTelemetryRepository.sendManualHeartbeat(BuildConfig.VERSION_NAME).fold(
                            onSuccess = { total ->
                                isPinging = false
                                pingNotice = "Heartbeat recorded! Today's active count: $total"
                                refreshStats(forceGitHubRefresh = false)
                            },
                            onFailure = { err ->
                                isPinging = false
                                pingNotice = "Ping failed: ${err.message}"
                            },
                        )
                    }
                },
            )

            Spacer(Modifier.height(16.dp))

            // Export & Share Telemetry Milestone Card
            val shareStats = stats ?: TelemetryStats(
                activeToday = livePulse?.onlineNow ?: 0,
                allTimeDevices = 0,
                versions = emptyMap(),
                platforms = buildMap {
                    livePulse?.let {
                        put("Android", it.androidOnline)
                        put("Desktop", it.pcOnline)
                    }
                },
                dailyHistory = emptyList(),
                latencyMs = livePulse?.latencyMs ?: 0L,
                isOnline = livePulse != null || githubDownloads != null,
            )
            ShareActionsCard(
                stats = shareStats,
                isSharing = isSharingCard,
                copiedNotice = reportCopiedNotice,
                onShareCard = {
                    coroutineScope.launch {
                        isSharingCard = true
                        TelemetryCardRenderer.shareMilestoneCard(
                            context = context,
                            stats = shareStats,
                            livePulse = livePulse,
                            githubDownloads = githubDownloads,
                        )
                        isSharingCard = false
                    }
                },
                onCopyReport = {
                    val report = buildString {
                        appendLine("# AuraMusic Telemetry Report")
                        appendLine("- Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
                        livePulse?.let {
                            appendLine("- Online Now: ${it.onlineNow} (${it.androidOnline} Android, ${it.pcOnline} PC)")
                            appendLine("- Active Jam Parties: ${it.activeParties} (${it.partyMembers} members)")
                        }
                        githubDownloads?.let {
                            appendLine("- GitHub Release Downloads: ${it.totalDownloads} (${it.androidDownloads} APK, ${it.desktopDownloads} PC)")
                            it.latestTag?.let { tag ->
                                appendLine("- Latest Release ($tag): ${it.latestReleaseDownloads} downloads")
                            }
                        }
                        stats?.let {
                            appendLine("- Render Active Today: ${it.activeToday}")
                            appendLine("- Render Recorded Devices: ${it.allTimeDevices}")
                            if (it.versions.isNotEmpty()) {
                                appendLine("- Versions: " + it.versions.entries.joinToString { v -> "v${v.key} (${v.value})" })
                            }
                        }
                        appendLine("- PostHog Cloud Analytics: ${if (analyticsStatus.isConfigured) "Connected (${analyticsStatus.host})" else "Not Configured"}")
                    }
                    clipboardManager.setText(AnnotatedString(report))
                    reportCopiedNotice = "Telemetry report copied to clipboard!"
                },
            )

            Spacer(Modifier.height(16.dp))

            // Privacy Guarantee
            PrivacyGuaranteeCard()

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TelemetryHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Live Telemetry & Active Users",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Real-time anonymous telemetry · Render Cloud",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isLoading,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (isLoading) Modifier.rotate(rotation) else Modifier,
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ServerStatusRibbon(
    stats: TelemetryStats?,
    livePulse: LiveNetworkPulse?,
    githubDownloads: GitHubDownloadsSummary?,
    isLoading: Boolean,
    loadingSeconds: Int,
    errorMessage: String?,
) {
    val hasAnyOnline = livePulse != null || githubDownloads != null || stats != null
    val isWaking = isLoading && loadingSeconds >= 3 && !hasAnyOnline

    val ribbonBg = when {
        isWaking -> Color(0xFFF59E0B).copy(alpha = 0.14f)
        isLoading && !hasAnyOnline -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        hasAnyOnline -> Color(0xFF10B981).copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }

    val borderColor = when {
        isWaking -> Color(0xFFF59E0B).copy(alpha = 0.40f)
        isLoading && !hasAnyOnline -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        hasAnyOnline -> Color(0xFF10B981).copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ribbonBg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isWaking -> Color(0xFFF59E0B)
                                isLoading && !hasAnyOnline -> Color(0xFFFBBF24)
                                hasAnyOnline -> Color(0xFF10B981)
                                else -> MaterialTheme.colorScheme.error
                            },
                        ),
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val connectedSources = buildList {
                        if (livePulse != null) add("Presence")
                        if (githubDownloads != null) add("GitHub")
                        if (stats != null) add("Render")
                    }
                    Text(
                        text = when {
                            isWaking -> "Waking Render Free Tier (~25s)..."
                            isLoading && !hasAnyOnline -> "Fetching live telemetry sources..."
                            connectedSources.isNotEmpty() -> "LIVE · ${connectedSources.joinToString(" + ")}"
                            else -> "All telemetry sources offline"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isWaking -> Color(0xFFF59E0B)
                            isLoading && !hasAnyOnline -> Color(0xFFFBBF24)
                            connectedSources.isNotEmpty() -> Color(0xFF10B981)
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    if (isWaking) {
                        Text(
                            text = "Render containers sleep after 15m idle ($loadingSeconds s elapsed)...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    } else if (errorMessage != null) {
                        Text(
                            text = "Render stats note: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }

                val latencyMs = livePulse?.latencyMs ?: stats?.latencyMs
                if (latencyMs != null && latencyMs > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = if (latencyMs < 500) Color(0xFF10B981) else Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "$latencyMs ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            if (isWaking) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color(0xFFF59E0B),
                    trackColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ReleasesAndJamCard(
    downloads: GitHubDownloadsSummary?,
    downloadsError: String?,
    livePulse: LiveNetworkPulse?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "GitHub Release Downloads",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (livePulse != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = "${livePulse.activeParties} Jam Rooms · ${livePulse.partyMembers} Users",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (downloads == null) {
                Text(
                    text = downloadsError ?: "Could not load GitHub Release asset counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (downloads.releases.isEmpty()) {
                Text(
                    text = "No published GitHub releases found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val total = downloads.totalDownloads.coerceAtLeast(1L)
                for (rel in downloads.releases.take(5)) {
                    val pct = (rel.totalDownloads.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = rel.tagName.ifBlank { rel.name },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${rel.totalDownloads} (${rel.androidDownloads} APK · ${rel.desktopDownloads} PC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF8B5CF6),
                            trackColor = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostHogAnalyticsCard(
    status: AnalyticsConnectionStatus,
    onOpenDashboard: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var hostInput by remember(status.host) { mutableStateOf(status.host) }
    var isVerifying by remember { mutableStateOf(false) }
    var verifyMessage by remember { mutableStateOf<String?>(null) }
    var verifyError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(
            1.dp,
            if (status.isConfigured) {
                Color(0xFF10B981).copy(alpha = 0.35f)
            } else {
                Color(0xFFF59E0B).copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = if (status.isConfigured) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "PostHog Cloud Analytics ($0)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (status.isConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)).copy(alpha = 0.15f),
                ) {
                    Text(
                        text = if (status.isConfigured) "ACTIVE" else "SETUP NEEDED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (status.isConfigured) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (status.isConfigured) {
                val sourceLabel = when (status.source) {
                    AnalyticsConfigSource.BUILD_CONFIG -> "Bundled in release build"
                    AnalyticsConfigSource.RUNTIME_OVERRIDE -> "Device key override"
                    AnalyticsConfigSource.NONE -> "Not configured"
                }
                Text(
                    text = "$sourceLabel · Key ${status.maskedKey} · Host ${status.host}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "This device: ${status.sentThisSession} sent this session · ${status.queuedEvents} queued on disk" +
                        (status.lastHttpCode?.let { " · Last HTTP $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                status.lastError?.let { err ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Last delivery error: $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text(
                    text = "Not tracking events yet (no dummy data). Create a free project on posthog.com (1M events/mo, no card), copy your phc_... Project API key into local.properties / GitHub Secrets for all users, or test it on this device below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showEditor = !showEditor },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (showEditor) "Hide Key Setup" else if (status.isConfigured) "Test / Edit Key" else "Paste & Verify Key",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onOpenDashboard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Open PostHog",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            AnimatedVisibility(visible = showEditor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("PostHog Project API Key (phc_...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("PostHog Host (us.i.posthog.com or eu.i.posthog.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isVerifying = true
                                    verifyMessage = null
                                    AuraAnalytics.saveAndVerifyConfig(keyInput, hostInput).fold(
                                        onSuccess = { code ->
                                            isVerifying = false
                                            verifyError = false
                                            verifyMessage = "Verified live with PostHog (HTTP $code)! Saved on this device."
                                            keyInput = ""
                                        },
                                        onFailure = { err ->
                                            isVerifying = false
                                            verifyError = true
                                            verifyMessage = err.message ?: "Verification failed"
                                        },
                                    )
                                }
                            },
                            enabled = !isVerifying && keyInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Text("Verify & Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (status.source == AnalyticsConfigSource.RUNTIME_OVERRIDE) {
                            TextButton(
                                onClick = {
                                    AuraAnalytics.clearRuntimeConfig()
                                    verifyMessage = "Cleared device override."
                                    verifyError = false
                                },
                            ) {
                                Text("Reset", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    verifyMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (verifyError) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoStatCard(
    title: String,
    value: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ActivityHistoryCard(
    history: List<DailyHistoryItem>,
) {
    var selectedItem by remember { mutableStateOf<DailyHistoryItem?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "14-Day Activity & Growth",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (selectedItem != null) {
                            "${selectedItem?.date}: ${selectedItem?.count} active listeners"
                        } else {
                            "Tap points on the curve to inspect daily counts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedItem != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedItem != null) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No history recorded yet today. Ping server to start logging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(history) {
                            detectTapGestures { offset ->
                                val spacing = size.width / (history.size - 1).coerceAtLeast(1)
                                val index = (offset.x / spacing).roundToInt().coerceIn(0, history.lastIndex)
                                selectedItem = history[index]
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val graphHeight = h - bottomPadding
                    val maxVal = history.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

                    // Subtle background horizontal grid lines
                    for (step in 1..3) {
                        val y = graphHeight * (step / 4f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                        )
                    }

                    // Build spline path
                    val points = history.mapIndexed { idx, item ->
                        val x = if (history.size > 1) idx * (w / (history.size - 1)) else w / 2f
                        val yRatio = (item.count.toFloat() / maxVal).coerceIn(0.05f, 1f)
                        val y = graphHeight - (yRatio * (graphHeight * 0.82f))
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                                val controlY1 = p0.y
                                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                                val controlY2 = p1.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }
                        }
                    }

                    // Fill under curve
                    val filledPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, graphHeight)
                        lineTo(points.first().x, graphHeight)
                        close()
                    }

                    drawPath(
                        path = filledPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE11D48).copy(alpha = 0.32f),
                                Color(0xFFE11D48).copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = graphHeight,
                        ),
                    )

                    // Draw glowing curve stroke
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE11D48),
                                Color(0xFFF43F5E),
                                Color(0xFFFB7185),
                            ),
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Draw data nodes
                    for ((idx, p) in points.withIndex()) {
                        val isSelected = selectedItem == history[idx]
                        val r = if (isSelected) 6.dp.toPx() else 4.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = Color(0xFFE11D48),
                            radius = r * 1.5f,
                            center = p,
                        )
                        // Core dot
                        drawCircle(
                            color = if (isSelected) Color.White else Color(0xFFFFF1F2),
                            radius = r,
                            center = p,
                        )
                    }
                }

                // Date axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val firstDate = history.firstOrNull()?.date?.takeLast(5) ?: ""
                    val midDate = history.getOrNull(history.size / 2)?.date?.takeLast(5) ?: ""
                    val lastDate = history.lastOrNull()?.date?.takeLast(5) ?: ""

                    Text(text = firstDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = midDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = lastDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun VersionPlatformCard(
    versions: Map<String, Int>,
    platforms: Map<String, Int>,
    totalActive: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Version & Client Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(14.dp))

            if (versions.isEmpty()) {
                Text(
                    text = "No version metrics logged today yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for ((ver, count) in versions) {
                    val pct = if (totalActive > 0) (count.toFloat() / totalActive) else 0f
                    val pctInt = (pct * 100).roundToInt()

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "v$ver",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "$count users ($pctInt%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { pct.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Platform tag chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                for ((platform, count) in platforms) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = "$platform: $count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartbeatTestCard(
    isPinging: Boolean,
    pingNotice: String?,
    onPingNow: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Live Device Telemetry Test",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(6.dp))

            val installIdShort = AuraTelemetryClient.getOrCreateInstallId().take(8)
            val lastPing = AuraTelemetryClient.getLastPingDate() ?: "Not yet pinged today"

            Text(
                text = "Install Token: $installIdShort...  |  Last Check-in: $lastPing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onPingNow,
                enabled = !isPinging,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (isPinging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Transmitting Heartbeat...")
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ping Render Server Now",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            AnimatedVisibility(visible = pingNotice != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = pingNotice.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyGuaranteeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Privacy-First Architecture Guarantee",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "AuraMusic transmits zero personal identity, zero song titles/artists, zero search query text, and zero location information. Telemetry sends only anonymous random installation IDs and feature counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun ShareActionsCard(
    stats: TelemetryStats,
    isSharing: Boolean,
    copiedNotice: String?,
    onShareCard: () -> Unit,
    onCopyReport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Export & Share Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "FOUNDER TOOL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Generate a branded milestone poster or copy a clean Markdown report to share with your community or on GitHub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Share Milestone Poster Button
                Button(
                    onClick = onShareCard,
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Rendering...",
                            fontSize = 12.sp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Share Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Copy Markdown Report Button
                OutlinedButton(
                    onClick = onCopyReport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Copy Report",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }

            AnimatedVisibility(visible = copiedNotice != null) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = copiedNotice.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                    )
                }
            }
        }
    }
}

