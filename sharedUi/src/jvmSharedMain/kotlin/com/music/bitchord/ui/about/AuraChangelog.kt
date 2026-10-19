package com.music.bitchord.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Structured model representing an update or feature enhancement entry.
 */
data class ChangelogEntry(
    val id: String,
    val date: String,
    val time: String,
    val title: String,
    val tag: String,
    val tagColor: Color,
    val description: String,
    val highlights: List<String>,
)

/**
 * Chronological log of recent enhancements and features in AuraMusic from October 5 to October 6, 2026.
 */
val AURA_CHANGELOG_ENTRIES: List<ChangelogEntry> = listOf(
    ChangelogEntry(
        id = "auramusic-v3-cloud-party",
        date = "October 19, 2026",
        time = "6:30 PM IST",
        title = "AuraMusic 3.0: Cloud Party & Live Sync",
        tag = "Major Release 3.0",
        tagColor = Color(0xFF8B5CF6),
        description = "Major generational upgrade introducing a dedicated Render cloud backend, real-time party chat with zero lag, floating emoji reactions, live soundbites, collaborative song suggestions, and official auramusic:// deep linking.",
        highlights = listOf(
            "Dedicated Cloud Sync: High-speed low-latency WebSocket party server hosted on Render.",
            "In-Party Live Chat: Instant messaging with optimistic rendering and preserved history.",
            "Floating Reactions & Soundbites: Airhorn, cheer, and flying emoji bursts during playback.",
            "Collaborative Democracy: Listeners can suggest songs and vote to skip tracks together.",
            "AuraMusic Deep Links: Direct auramusic://party/<code> invite URLs with automatic app launch.",
            "Smooth Bottom Sheets: Reworked party navigation preventing modal freezes or stuck states.",
        ),
    ),
    ChangelogEntry(
        id = "nothing-os-ui-mode",
        date = "October 6, 2026",
        time = "11:45 PM IST",
        title = "Nothing OS UI & Glyph Aesthetic Mode",
        tag = "Design System",
        tagColor = Color(0xFFD71921),
        description = "Authentic Nothing OS experience featuring true pitch-black AMOLED black, iconic Nothing Red accents, hardware dot-matrix timers, and animated glyph equalizer.",
        highlights = listOf(
            "True pitch-black AMOLED (#000000) zero-power theme saving 100% OLED battery.",
            "Hardware-inspired Nothing Dot-Matrix digital clock rendering for track position and remaining time.",
            "Nothing Red (#D71921) circular glyph play/pause control and active pill state indicators.",
            "Animated vertical LED matrix soundwave equalizer reacting smoothly to playback state.",
            "Ultra-clean typography: keeps song titles and lyrics in crisp modern sans-serif for perfect readability.",
            "Toggleable anytime with 1 tap in Settings -> Appearance.",
        ),
    ),
    ChangelogEntry(
        id = "settings-shortcut-and-dev-hub",
        date = "October 6, 2026",
        time = "8:25 PM IST",
        title = "Direct Settings Top-Bar Icon & Developer Hub",
        tag = "UI / UX",
        tagColor = Color(0xFF60A5FA),
        description = "Instant 1-tap Settings access beside your profile icon on both mobile and desktop, plus the Lead Developer Hub honoring kami likhith with an interactive updates timeline.",
        highlights = listOf(
            "Dedicated Settings gear icon placed beside the profile avatar in the top bar on Android and Desktop.",
            "Eliminated navigation confusion when opening settings while signed in with multiple accounts.",
            "Lead Developer attribution card showcasing kami likhith with portfolio and GitHub shortcuts.",
            "Interactive 'What's New & Enhancements' changelog tracking recent updates with precise timestamps.",
        ),
    ),
    ChangelogEntry(
        id = "perf-and-zero-crash",
        date = "October 6, 2026",
        time = "7:55 PM IST",
        title = "Performance Boost & Zero-Crash Architecture",
        tag = "Performance",
        tagColor = Color(0xFF34D399),
        description = "Eliminated nested scroll collisions, stopped duplicate 544px artwork quantization lag, and integrated thread-safe memory caching with network timeouts.",
        highlights = listOf(
            "Fixed app crash on 'Song details & credits' by resolving nested vertical scroll conflict in bottom sheets.",
            "Eliminated UI lag by caching artwork palette computations instead of re-quantizing 544px bitmaps.",
            "Added ConcurrentHashMap in-memory caching for instant song detail loading on subsequent views.",
            "Enforced 3-second safe network timeouts preventing app hangs on sluggish network connections.",
        ),
    ),
    ChangelogEntry(
        id = "song-details-credits",
        date = "October 6, 2026",
        time = "7:10 PM IST",
        title = "Song Details & Credits Studio",
        tag = "Feature",
        tagColor = Color(0xFFA78BFA),
        description = "Complete track metadata studio unveiling release dates, lossless audio quality indicators, and interactive contributor artist chips.",
        highlights = listOf(
            "Deep track insights: exact release date, release year, album art preview, and behind-the-song synopsis.",
            "Interactive contributing artist chips allowing direct profile exploration for featured vocalists and producers.",
            "Lossless audio badges displaying Hi-Res, FLAC, and Opus stream quality indicators.",
            "Dedicated 'Song details & credits' action item integrated into the action menu.",
        ),
    ),
    ChangelogEntry(
        id = "auramusic-rebrand-splash",
        date = "October 6, 2026",
        time = "2:15 PM IST",
        title = "AuraMusic by likhith Rebranding & Signature Splash",
        tag = "Visuals",
        tagColor = Color(0xFFF472B6),
        description = "Rebranded the entire ecosystem to AuraMusic by likhith with a signature soundwave bloom launch animation and unified multiplatform builds.",
        highlights = listOf(
            "Obsidian soundwave bloom opening splash with luminous aura glow and butter-smooth launch handoff.",
            "Zero startup freeze and tap-to-skip gesture for instant music enjoyment.",
            "Universal Android APK compilation pipeline and portable Windows desktop packaging.",
            "Consistent branding across Android, Windows, and shared UI layers.",
        ),
    ),
    ChangelogEntry(
        id = "release-radar-enhancements",
        date = "October 5, 2026",
        time = "6:30 PM IST",
        title = "New Release Radar & Audio Enhancements",
        tag = "Feature",
        tagColor = Color(0xFFFBBF24),
        description = "Background artist tracking notifications, Spotify Canvas video backdrops, synced lyrics offset sliders, and Listen Together sync.",
        highlights = listOf(
            "Background sync notification worker alerting when followed artists drop new songs.",
            "Full-screen Spotify Canvas looping video background support for immersive playback.",
            "Synced lyrics millisecond precision offset adjustment slider for perfect lyrical timing.",
            "Listen Together real-time synchronized group playback backend.",
        ),
    ),
)

/**
 * Premium Developer Profile Card honoring kami likhith.
 * Includes direct links to Developer portfolio, GitHub, and an interactive "What's New" changelog entry.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperProfileCard(
    developerName: String = "kami likhith",
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF13131A),
        border = BorderStroke(1.dp, Color(0xFF272738)),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
        ) {
            // Header: Avatar, Name, and Role
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Gradient Avatar with Initials
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "KL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = developerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Lead Dev",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Lead Developer & Maintainer · AuraMusic",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bio / Description
            Text(
                text = "Building high-performance, beautiful multiplatform audio experiences with real-time sync, zero lag, and modern aesthetics.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD1D5DB),
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // What's New Trigger
                Button(
                    onClick = onOpenChangelog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "What's New (${AURA_CHANGELOG_ENTRIES.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }

                // Portfolio Link
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://devlikhith.vercel.app/") },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF374151)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5E7EB)),
                    modifier = Modifier.height(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9CA3AF),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Portfolio", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF6B7280),
                    )
                }

                // GitHub Link
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://github.com/likhith3035") },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF374151)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5E7EB)),
                    modifier = Modifier.height(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF9CA3AF),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "GitHub", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF6B7280),
                    )
                }
            }
        }
    }
}

/**
 * Detailed timeline component displaying all updates and enhancements from yesterday and today.
 */
@Composable
fun AuraChangelogTimeline(
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F14))
            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        // Sheet / Dialog Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "What's New & Enhancements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "AuraMusic by kami likhith · Recent Updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                )
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9CA3AF),
                    )
                }
            }
        }

        var showV3Showcase by remember { mutableStateOf(false) }

        Spacer(Modifier.height(14.dp))

        // V3.0 Highlights Spotlight Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED).copy(alpha = 0.25f), Color(0xFF2563EB).copy(alpha = 0.18f))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFFA78BFA).copy(alpha = 0.6f), Color(0xFF60A5FA).copy(alpha = 0.4f))),
                    RoundedCornerShape(16.dp)
                )
                .clickable { showV3Showcase = true }
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFC4B5FD),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AuraMusic 3.0 Showcase",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "Tap to view interactive feature spotlight & cards",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = "View",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA78BFA),
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF232330), thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        if (showV3Showcase) {
            AuraV3WhatsNewDialog(
                onDismissRequest = { showV3Showcase = false },
            )
        }

        // Scrollable Timeline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            AURA_CHANGELOG_ENTRIES.forEachIndexed { index, entry ->
                ChangelogTimelineItem(
                    entry = entry,
                    isLast = index == AURA_CHANGELOG_ENTRIES.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun ChangelogTimelineItem(
    entry: ChangelogEntry,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Left Column: Dot and Vertical Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(entry.tagColor)
                    .border(2.dp, Color(0xFF0F0F14), CircleShape),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF272738)),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Right Column: Content Card
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 8.dp else 22.dp),
        ) {
            // Timestamp & Category Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(entry.tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = entry.tag,
                        color = entry.tagColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF6B7280),
                    )
                    Text(
                        text = "${entry.date} · ${entry.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
            )

            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD1D5DB),
                lineHeight = 18.sp,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(8.dp))

            // Bullet Highlights
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161622))
                    .padding(10.dp),
            ) {
                entry.highlights.forEach { highlight ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "•",
                            color = entry.tagColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            text = highlight,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal dialog for Desktop or Android overlay to inspect the changelog timeline.
 */
@Composable
fun AuraChangelogDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F0F14),
            border = BorderStroke(1.dp, Color(0xFF272738)),
            tonalElevation = 6.dp,
        ) {
            AuraChangelogTimeline(onClose = onDismissRequest)
        }
    }
}
