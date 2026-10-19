package com.music.bitchord.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
 * Filter categories for engineering and feature milestones.
 */
enum class ChangelogCategory(val label: String, val emoji: String, val color: Color) {
    All("All Updates", "⚡", Color(0xFF818CF8)),
    Audio("Audio & Sleep", "🎵", Color(0xFF10B981)),
    Lyrics("Lyrics & Caching", "📝", Color(0xFF38BDF8)),
    QueueUI("Queue & Haptics", "✨", Color(0xFFA855F7)),
    CloudParty("Cloud Party", "☁️", Color(0xFF8B5CF6)),
    BuildEngineering("Build & CI Lab", "🛠️", Color(0xFFF59E0B)),
}

/**
 * Structured model representing an update or feature enhancement entry.
 * Includes precise timestamps, categories, plain-English user explanations,
 * and behind-the-scenes build challenges & solutions.
 */
data class ChangelogEntry(
    val id: String,
    val date: String,
    val time: String,
    val title: String,
    val tag: String,
    val tagColor: Color,
    val category: ChangelogCategory,
    val description: String,
    val highlights: List<String>,
    val buildChallenge: String? = null,
    val buildSolution: String? = null,
)

/**
 * Complete, chronological log of all enhancements, features, and engineering milestones in AuraMusic.
 */
val AURA_CHANGELOG_ENTRIES: List<ChangelogEntry> = listOf(
    ChangelogEntry(
        id = "smart-sleep-timer-fade-out",
        date = "October 19, 2026",
        time = "11:20 PM IST",
        title = "Smart Sleep Timer: Exponential Volume Fade-Out & End-of-Track Preset",
        tag = "Audio Engine",
        tagColor = Color(0xFF10B981),
        category = ChangelogCategory.Audio,
        description = "Music no longer cuts off abruptly when you drift off to sleep. Over the final 30 seconds of your countdown, the volume gently fades down like a natural whisper using a smooth mathematical cosine curve. Plus, a new 1-tap 'End of Current Track' mode stops playback right when your current song finishes.",
        highlights = listOf(
            "1-Tap 'End of Current Track' hero action card with live active badge right at the top of the timer sheet.",
            "Gentle 30-second volume fade-out using cosine ease-out formula: (1.0 - cos(progress * π)) / 2.0.",
            "Dedicated toggle switch in settings allowing you to enable or disable fade-out at any time.",
            "Failsafe auto-reset: Restores baseline 100% volume immediately on timer cancel, skip, or completion.",
        ),
        buildChallenge = "Abrupt music stops during sleep wake listeners up. In addition, when fading volume down in code, cancelling the timer or restarting playback could leave player volume stuck at 0%, making the app seem broken or muted.",
        buildSolution = "Engineered a reactive StateFlow volume multiplier tied to ExoPlayer on Android and desktop audio sinks. When the timer expires, is cancelled, or pauses, the multiplier automatically and safely resets to 1.0f baseline so future playback is always full volume.",
    ),
    ChangelogEntry(
        id = "offline-lyrics-caching-engine",
        date = "October 19, 2026",
        time = "11:05 PM IST",
        title = "Instant Offline Lyrics Engine: 0ms Cache & GZIP Compression",
        tag = "Lyrics & Storage",
        tagColor = Color(0xFF38BDF8),
        category = ChangelogCategory.Lyrics,
        description = "Synchronized karaoke lyrics now load in 0ms and work completely offline without an active internet connection! The first time a track plays, its timed lyrics, syllables, and background vocals are saved both in high-speed RAM and in a space-saving compressed format on disk.",
        highlights = listOf(
            "2-Tier Caching: Checks high-speed memory first (0ms), compressed disk cache second (~1ms).",
            "Lightweight GZIP JSON disk serialization saving over 85% storage space per cached song.",
            "Preserves word-by-word synced karaoke timestamps and background vocal alignments.",
            "Seamless offline mode: repeat tracks now display live scrolling lyrics on airplanes and subways.",
        ),
        buildChallenge = "External lyric APIs (LrcLib, Genius, Musixmatch) have network latency, rate limits, and fail completely in airplane or offline mode. Moreover, storing uncompressed lyric JSON files consumed unnecessary local flash storage on mobile devices.",
        buildSolution = "Created a dedicated CachedLyricsData schema with Kotlinx Serialization and GZIP streaming. We write to disk on background dispatchers, eliminating UI frame drops and cutting repeat lyric load times from 850ms down to 0ms.",
    ),
    ChangelogEntry(
        id = "queue-drag-reordering-haptics",
        date = "October 19, 2026",
        time = "10:45 PM IST",
        title = "Queue Drag Reordering & Tactile Haptic Feedback",
        tag = "UI / UX",
        tagColor = Color(0xFFA855F7),
        category = ChangelogCategory.QueueUI,
        description = "Reorganizing your music queue is now fluid and tactile. We expanded the touch target for the drag handle, added physical vibration pulses when lifting songs, and rendered high-contrast elevated styling so dragging tracks feels intuitive.",
        highlights = listOf(
            "Expanded drag handle hit target from a narrow 20dp icon to a comfortable 36dp container.",
            "Crisp Haptic.Select physical vibration confirmation the exact millisecond a track is picked up.",
            "Elevated visual card styling with rounded corners and translucent white glow when lifted.",
            "Butter-smooth inline queue animation on both Android touch screens and desktop mouse drag.",
        ),
        buildChallenge = "The previous drag handle had a narrow 20dp hit area that often caused users to accidentally tap and jump to a song instead of dragging it, leading to frustrating queue interruptions on mobile touchscreens.",
        buildSolution = "Encapsulated the drag handle inside an enlarged 36dp circular target box with isolated pointer drag detection, paired with a distinct Haptic.Select pulse and a lifted translucent card border.",
    ),
    ChangelogEntry(
        id = "multiplatform-cicd-gradle-harmony",
        date = "October 19, 2026",
        time = "10:15 PM IST",
        title = "Multiplatform CI/CD Pipeline & Gradle JDK Harmony",
        tag = "Build & CI",
        tagColor = Color(0xFFF59E0B),
        category = ChangelogCategory.BuildEngineering,
        description = "Strengthened the automated build, test, and release system across Windows, Linux, and Android. Resolved build-time toolchain mismatches and streamlined release APK generation.",
        highlights = listOf(
            "Automated GitHub Actions CI validating tests, lint, and packaging release APKs on every commit.",
            "Pinned JDK build environment to Android Studio JBR, eliminating Java version conflicts.",
            "Disambiguated Gradle build flavors (compileDevDebugKotlin vs compileProdDebugKotlin).",
            "Verified clean compilation across :shared, :sharedUi, :desktopApp, and :app.",
        ),
        buildChallenge = "Running Gradle on Windows defaulted to host Java 25, which Android Gradle Plugin rejected. In addition, ambiguous build flavor tasks (compileDebugKotlin vs compileDevDebugKotlin) caused CI verification failures.",
        buildSolution = "Pinned Gradle execution to Android Studio's bundled JetBrains Runtime (JBR) and targeted specific variant tasks (compileDevDebugKotlin), passing all unit tests and automated GitHub Actions release builds.",
    ),
    ChangelogEntry(
        id = "auramusic-v3-cloud-party",
        date = "October 19, 2026",
        time = "6:30 PM IST",
        title = "AuraMusic 3.0: Cloud Party & Live Sync",
        tag = "Major Release 3.0",
        tagColor = Color(0xFF8B5CF6),
        category = ChangelogCategory.CloudParty,
        description = "Major generational upgrade introducing a dedicated Render cloud backend, real-time party chat with zero lag, floating emoji reactions, live soundbites, collaborative song suggestions, and official auramusic:// deep linking.",
        highlights = listOf(
            "Dedicated Cloud Sync: High-speed low-latency WebSocket party server hosted on Render.",
            "In-Party Live Chat: Instant messaging with optimistic rendering and preserved history.",
            "Floating Reactions & Soundbites: Airhorn, cheer, and flying emoji bursts during playback.",
            "Collaborative Democracy: Listeners can suggest songs and vote to skip tracks together.",
            "AuraMusic Deep Links: Direct auramusic://party/<code> invite URLs with automatic app launch.",
            "Smooth Bottom Sheets: Reworked party navigation preventing modal freezes or stuck states.",
        ),
        buildChallenge = "WebSocket connections would intermittently disconnect during network handover (Wi-Fi to 4G), and modal bottom sheets caused state freeze when closed abruptly.",
        buildSolution = "Engineered an automatic exponential reconnect loop on the client with heartbeat ping-pongs, plus decoupled state handling in Compose BottomSheetScaffold preventing any frozen UI.",
    ),
    ChangelogEntry(
        id = "nothing-os-ui-mode",
        date = "October 6, 2026",
        time = "11:45 PM IST",
        title = "Nothing OS UI & Glyph Aesthetic Mode",
        tag = "Design System",
        tagColor = Color(0xFFD71921),
        category = ChangelogCategory.QueueUI,
        description = "Authentic Nothing OS experience featuring true pitch-black AMOLED black, iconic Nothing Red accents, hardware dot-matrix timers, and animated glyph equalizer.",
        highlights = listOf(
            "True pitch-black AMOLED (#000000) zero-power theme saving 100% OLED battery.",
            "Hardware-inspired Nothing Dot-Matrix digital clock rendering for track position and remaining time.",
            "Nothing Red (#D71921) circular glyph play/pause control and active pill state indicators.",
            "Animated vertical LED matrix soundwave equalizer reacting smoothly to playback state.",
            "Ultra-clean typography: keeps song titles and lyrics in crisp modern sans-serif for perfect readability.",
            "Toggleable anytime with 1 tap in Settings -> Appearance.",
        ),
        buildChallenge = "Rendering dynamic dot-matrix fonts and animating LED soundwaves every frame risked GPU battery drain.",
        buildSolution = "Optimized shader draw loops using Canvas path caching and true pitch-black (#000000) surfaces that power down OLED pixels completely.",
    ),
    ChangelogEntry(
        id = "settings-shortcut-and-dev-hub",
        date = "October 6, 2026",
        time = "8:25 PM IST",
        title = "Direct Settings Top-Bar Icon & Developer Hub",
        tag = "UI / UX",
        tagColor = Color(0xFF60A5FA),
        category = ChangelogCategory.QueueUI,
        description = "Instant 1-tap Settings access beside your profile icon on both mobile and desktop, plus the Lead Developer Hub honoring kami likhith with an interactive updates timeline.",
        highlights = listOf(
            "Dedicated Settings gear icon placed beside the profile avatar in the top bar on Android and Desktop.",
            "Eliminated navigation confusion when opening settings while signed in with multiple accounts.",
            "Lead Developer attribution card showcasing kami likhith with portfolio and GitHub shortcuts.",
            "Interactive 'What's New & Enhancements' changelog tracking recent updates with precise timestamps.",
        ),
        buildChallenge = "Multi-account profile dropdowns caused the settings option to be buried several levels deep in the navigation hierarchy.",
        buildSolution = "Integrated a top-level dedicated gear button directly in the main app bar and established the Lead Developer attribution hub.",
    ),
    ChangelogEntry(
        id = "perf-and-zero-crash",
        date = "October 6, 2026",
        time = "7:55 PM IST",
        title = "Performance Boost & Zero-Crash Architecture",
        tag = "Performance",
        tagColor = Color(0xFF34D399),
        category = ChangelogCategory.BuildEngineering,
        description = "Eliminated nested scroll collisions, stopped duplicate 544px artwork quantization lag, and integrated thread-safe memory caching with network timeouts.",
        highlights = listOf(
            "Fixed app crash on 'Song details & credits' by resolving nested vertical scroll conflict in bottom sheets.",
            "Eliminated UI lag by caching artwork palette computations instead of re-quantizing 544px bitmaps.",
            "Added ConcurrentHashMap in-memory caching for instant song detail loading on subsequent views.",
            "Enforced 3-second safe network timeouts preventing app hangs on sluggish network connections.",
        ),
        buildChallenge = "Opening song credits crashed with nested scrolling exceptions, while 544px album art palette generation caused UI stutter.",
        buildSolution = "Decoupled nested sheet scroll handlers and added bitmap quantizer memory caching, achieving 60fps scrolling and zero crash reports.",
    ),
    ChangelogEntry(
        id = "song-details-credits",
        date = "October 6, 2026",
        time = "7:10 PM IST",
        title = "Song Details & Credits Studio",
        tag = "Feature",
        tagColor = Color(0xFFA78BFA),
        category = ChangelogCategory.Audio,
        description = "Complete track metadata studio unveiling release dates, lossless audio quality indicators, and interactive contributor artist chips.",
        highlights = listOf(
            "Deep track insights: exact release date, release year, album art preview, and behind-the-song synopsis.",
            "Interactive contributing artist chips allowing direct profile exploration for featured vocalists and producers.",
            "Lossless audio badges displaying Hi-Res, FLAC, and Opus stream quality indicators.",
            "Dedicated 'Song details & credits' action item integrated into the action menu.",
        ),
        buildChallenge = "Music streaming APIs often deliver fragmented metadata without complete producer, release year, or lossless stream details.",
        buildSolution = "Aggregated metadata from YouTube Music, MusicBrainz, and local audio tag engines to construct a unified credit card.",
    ),
    ChangelogEntry(
        id = "auramusic-rebrand-splash",
        date = "October 6, 2026",
        time = "2:15 PM IST",
        title = "AuraMusic by likhith Rebranding & Signature Splash",
        tag = "Visuals",
        tagColor = Color(0xFFF472B6),
        category = ChangelogCategory.QueueUI,
        description = "Rebranded the entire ecosystem to AuraMusic by likhith with a signature soundwave bloom launch animation and unified multiplatform builds.",
        highlights = listOf(
            "Obsidian soundwave bloom opening splash with luminous aura glow and butter-smooth launch handoff.",
            "Zero startup freeze and tap-to-skip gesture for instant music enjoyment.",
            "Universal Android APK compilation pipeline and portable Windows desktop packaging.",
            "Consistent branding across Android, Windows, and shared UI layers.",
        ),
        buildChallenge = "Cold launch splash screens felt sluggish on older devices with long startup delays.",
        buildSolution = "Created an obsidian soundwave bloom vector animation with lazy engine warmup and instant tap-to-skip support.",
    ),
    ChangelogEntry(
        id = "release-radar-enhancements",
        date = "October 5, 2026",
        time = "6:30 PM IST",
        title = "New Release Radar & Audio Enhancements",
        tag = "Feature",
        tagColor = Color(0xFFFBBF24),
        category = ChangelogCategory.Audio,
        description = "Background artist tracking notifications, Spotify Canvas video backdrops, synced lyrics offset sliders, and Listen Together sync.",
        highlights = listOf(
            "Background sync notification worker alerting when followed artists drop new songs.",
            "Full-screen Spotify Canvas looping video background support for immersive playback.",
            "Synced lyrics millisecond precision offset adjustment slider for perfect lyrical timing.",
            "Listen Together real-time synchronized group playback backend.",
        ),
        buildChallenge = "Background artist polling drained battery when executed continuously in Android background services.",
        buildSolution = "Migrated background updates to Android WorkManager with system charging and unmetered Wi-Fi constraints.",
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
                        text = "Engineering Log (${AURA_CHANGELOG_ENTRIES.size})",
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
 * Detailed timeline component displaying all updates, features, and engineering milestones.
 * Includes interactive category pill filters, live keyword search, and collapsible
 * Behind-the-Build (Challenge & Solution) technical cards.
 */
@Composable
fun AuraChangelogTimeline(
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var selectedCategory by remember { mutableStateOf(ChangelogCategory.All) }
    var searchQuery by remember { mutableStateOf("") }
    var showV3Showcase by remember { mutableStateOf(false) }

    val filteredEntries = remember(selectedCategory, searchQuery) {
        AURA_CHANGELOG_ENTRIES.filter { entry ->
            val matchesCategory = selectedCategory == ChangelogCategory.All || entry.category == selectedCategory
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                entry.title.lowercase().contains(q) ||
                    entry.description.lowercase().contains(q) ||
                    entry.tag.lowercase().contains(q) ||
                    entry.date.lowercase().contains(q) ||
                    (entry.buildChallenge?.lowercase()?.contains(q) == true) ||
                    (entry.buildSolution?.lowercase()?.contains(q) == true) ||
                    entry.highlights.any { it.lowercase().contains(q) }
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F14))
            .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 24.dp),
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
                        text = "Engineering & Innovation Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 19.sp,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "AuraMusic by kami likhith · ${AURA_CHANGELOG_ENTRIES.size} milestones recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
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

        Spacer(Modifier.height(14.dp))

        // Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF171722),
            border = BorderStroke(1.dp, Color(0xFF282838)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search features, fixes, or build challenges...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF818CF8),
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Category Filter Pills (Horizontally Scrollable)
        val categoryScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChangelogCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val count = remember(category) {
                    if (category == ChangelogCategory.All) {
                        AURA_CHANGELOG_ENTRIES.size
                    } else {
                        AURA_CHANGELOG_ENTRIES.count { it.category == category }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) category.color.copy(alpha = 0.22f) else Color(0xFF161622),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) category.color else Color(0xFF282838),
                    ),
                    modifier = Modifier.clickable { selectedCategory = category },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(text = category.emoji, fontSize = 12.sp)
                        Text(
                            text = category.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) category.color else Color(0xFF27273A))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // V3.0 Highlights Spotlight Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED).copy(alpha = 0.22f), Color(0xFF2563EB).copy(alpha = 0.16f))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFFA78BFA).copy(alpha = 0.5f), Color(0xFF60A5FA).copy(alpha = 0.35f))),
                    RoundedCornerShape(14.dp)
                )
                .clickable { showV3Showcase = true }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF7C3AED).copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFC4B5FD),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AuraMusic 3.0 Interactive Showcase",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Tap to view full feature spotlight & soundbite cards",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                    )
                }
                Text(
                    text = "Explore →",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA78BFA),
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF232330), thickness = 1.dp)
        Spacer(Modifier.height(14.dp))

        if (showV3Showcase) {
            AuraV3WhatsNewDialog(
                onDismissRequest = { showV3Showcase = false },
            )
        }

        // Empty state when search or filter returns zero results
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No milestones matched '$searchQuery'",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try switching categories or clearing search",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            // Scrollable Timeline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                filteredEntries.forEachIndexed { index, entry ->
                    ChangelogTimelineItem(
                        entry = entry,
                        isLast = index == filteredEntries.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangelogTimelineItem(
    entry: ChangelogEntry,
    isLast: Boolean,
) {
    var expandedBuildDetails by remember { mutableStateOf(false) }

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
                .padding(bottom = if (isLast) 12.dp else 24.dp),
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
                        .background(entry.tagColor.copy(alpha = 0.16f))
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

            // Behind the Build: Challenge & Solution Collapsible Section
            if (entry.buildChallenge != null || entry.buildSolution != null) {
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (expandedBuildDetails) Color(0xFF181826) else Color(0xFF14141E),
                    border = BorderStroke(
                        1.dp,
                        if (expandedBuildDetails) Color(0xFF4338CA).copy(alpha = 0.5f) else Color(0xFF252538),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expandedBuildDetails = !expandedBuildDetails }
                        .animateContentSize(animationSpec = tween(250)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Behind the Build: Challenge & Solution",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (expandedBuildDetails) Color(0xFFFBBF24) else Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (expandedBuildDetails) "Hide ▲" else "Read fix ▼",
                                color = Color(0xFF818CF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (expandedBuildDetails) {
                            Spacer(Modifier.height(10.dp))

                            if (entry.buildChallenge != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.16f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "⚠️", fontSize = 11.sp)
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "What was happening / Challenge faced:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFFFCA5A5),
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = entry.buildChallenge,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFECACA),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                    )
                                }
                            }

                            if (entry.buildSolution != null) {
                                Spacer(Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF064E3B).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "✅", fontSize = 11.sp)
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "How we conquered it in code:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF6EE7B7),
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = entry.buildSolution,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                    )
                                }
                            }
                        }
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
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F0F14),
            border = BorderStroke(1.dp, Color(0xFF272738)),
            tonalElevation = 6.dp,
        ) {
            AuraChangelogTimeline(onClose = onDismissRequest)
        }
    }
}
