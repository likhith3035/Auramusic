package com.music.bitchord.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.theme.NothingThemeTokens
import java.util.Calendar

/**
 * Flagship capabilities highlighted in the Home screen feature discovery carousel.
 */
enum class AuraFeature(
    val title: String,
    val subtitle: String,
    val badge: String,
    val actionLabel: String,
    val icon: String,
    val gradientColors: List<Color>,
) {
    STUDIO_EQ(
        title = "10-Band Studio EQ",
        subtitle = "Parametric audio curve & bass boost",
        badge = "STUDIO",
        actionLabel = "Tune EQ",
        icon = "🎛️",
        gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    ),
    LISTEN_TOGETHER(
        title = "Listen Together",
        subtitle = "Zero-drift party rooms with live soundbites",
        badge = "SOCIAL",
        actionLabel = "Party",
        icon = "📻",
        gradientColors = listOf(Color(0xFFFF007F), Color(0xFF7928CA)),
    ),
    KARAOKE_LYRICS(
        title = "Live Synced Lyrics",
        subtitle = "Word-by-word karaoke & live translation",
        badge = "LIVE",
        actionLabel = "Sing",
        icon = "🎤",
        gradientColors = listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)),
    ),
    SLEEP_TIMER(
        title = "Smart Sleep Timer",
        subtitle = "Gentle fade-out while you drift to sleep",
        badge = "NIGHT",
        actionLabel = "Timer",
        icon = "🌙",
        gradientColors = listOf(Color(0xFF8338EC), Color(0xFF3A86FF)),
    ),
    REPLAY_2026(
        title = "2026 Replay Recap",
        subtitle = "Your top artists, tracks & hologram card",
        badge = "SPECIAL",
        actionLabel = "View",
        icon = "✨",
        gradientColors = listOf(Color(0xFFFFB703), Color(0xFFFB8500)),
    ),
    ONBOARDING_TOUR(
        title = "Welcome Tour",
        subtitle = "Revisit all features and setup tips",
        badge = "TOUR",
        actionLabel = "Explore",
        icon = "🚀",
        gradientColors = listOf(Color(0xFF06D6A0), Color(0xFF118AB2)),
    ),
}

/**
 * Time of day categorization with curated celestial styling, mood responses, and icons.
 */
enum class TimeOfDay(
    val greeting: String,
    val emoji: String,
    val periodTag: String,
    val dotMatrixGreeting: String,
    val subtitles: List<String>,
    val moodPills: List<String>,
    val gradientColors: List<Color>,
) {
    MORNING(
        greeting = "Good morning",
        emoji = "🌅",
        periodTag = "SUNRISE VIBES",
        dotMatrixGreeting = "G O O D   M O R N I N G",
        subtitles = listOf(
            "Start your morning with energizing acoustic & fresh beats",
            "Crisp morning melodies brewed to fuel your day",
            "Your morning coffee soundtrack is curated and ready",
        ),
        moodPills = listOf("⚡ Morning Energy", "☕ Acoustic Brew", "🧘 Mindful Wakeup", "✨ Fresh Beats"),
        gradientColors = listOf(
            Color(0xFF381512).copy(alpha = 0.55f),
            Color(0xFF1E1026).copy(alpha = 0.65f),
            Color(0xFF0F0B14).copy(alpha = 0.85f),
        ),
    ),
    AFTERNOON(
        greeting = "Good afternoon",
        emoji = "☀️",
        periodTag = "MIDDAY ENERGY",
        dotMatrixGreeting = "G O O D   A F T E R N O O N",
        subtitles = listOf(
            "Power through your afternoon with focus beats & favorite grooves",
            "Midday energy boost — upbeat rhythms to keep momentum alive",
            "Stay locked in the zone with your daily focus soundtrack",
        ),
        moodPills = listOf("⚡ Midday Focus", "🔥 Upbeat Hits", "🎧 Deep Flow", "☕ Lofi Chill"),
        gradientColors = listOf(
            Color(0xFF0C243B).copy(alpha = 0.55f),
            Color(0xFF0C1B2E).copy(alpha = 0.65f),
            Color(0xFF080D1A).copy(alpha = 0.85f),
        ),
    ),
    EVENING(
        greeting = "Good evening",
        emoji = "🌆",
        periodTag = "GOLDEN HOUR",
        dotMatrixGreeting = "G O O D   E V E N I N G",
        subtitles = listOf(
            "Unwind and recharge with chill vibes & golden hour melodies",
            "Dusk is here — slow down and let the soul rhythms play",
            "Sunset melodies crafted for a relaxed, serene evening",
        ),
        moodPills = listOf("🌆 Golden Hour", "✨ Sunset Melodies", "🍷 Chill R&B", "🎷 Smooth Soul"),
        gradientColors = listOf(
            Color(0xFF35122F).copy(alpha = 0.55f),
            Color(0xFF1E112E).copy(alpha = 0.65f),
            Color(0xFF0E0A1A).copy(alpha = 0.85f),
        ),
    ),
    NIGHT(
        greeting = "Good night",
        emoji = "🌙",
        periodTag = "MIDNIGHT AURA",
        dotMatrixGreeting = "G O O D   N I G H T",
        subtitles = listOf(
            "Late night reflections & soft, immersive ambient soundscapes",
            "Dim the room lights and immerse in deep midnight audio",
            "Gentle midnight melodies designed for peaceful dreams",
        ),
        moodPills = listOf("🌙 Late Night Lo-Fi", "🌌 Deep Ambient", "💤 Sleep Soundscapes", "🎹 Piano Dreams"),
        gradientColors = listOf(
            Color(0xFF131038).copy(alpha = 0.55f),
            Color(0xFF0C1026).copy(alpha = 0.65f),
            Color(0xFF060812).copy(alpha = 0.85f),
        ),
    );

    fun pickSubtitle(): String = subtitles.random()

    companion object {
        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..16 -> AFTERNOON
                in 17..21 -> EVENING
                else -> NIGHT
            }
        }
    }
}

/**
 * High-end dynamic greeting header rendered at the top of the AuraMusic Home feed.
 * Features an atmospheric celestial card, live digital clock pill, animated equalizer indicator,
 * dynamic contextual mood chips, and dual-mode theme support (Classic Aura vs Nothing OS).
 */
@Composable
fun AuraGreetingHeader(
    accountName: String?,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
    timeOfDay: TimeOfDay = remember { TimeOfDay.current() },
    currentSongTitle: String? = null,
    isPlaying: Boolean = false,
    onResumeClick: (() -> Unit)? = null,
    onMoodClick: ((String) -> Unit)? = null,
) {
    val firstName = remember(accountName) {
        accountName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    val subtitle = remember(timeOfDay) { timeOfDay.pickSubtitle() }

    // Animated soundwave equalizer bars in the greeting card
    val infiniteTransition = rememberInfiniteTransition(label = "greeting_pulse")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb3",
    )

    if (nothingOsMode) {
        // Nothing OS Mode: AMOLED Pitch Black + NDot Matrix + Red Glyph (Compact Edition)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NothingThemeTokens.Surface,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 3.dp)
                .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(16.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                // Top Row: Pulsing LED glyph, Matrix Tag, and Nothing OS Audio Pulse Indicator / 1-Tap Quick Resume
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingThemeTokens.Red),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = timeOfDay.dotMatrixGreeting,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                                fontSize = 10.sp,
                            ),
                            color = NothingThemeTokens.White,
                        )
                    }

                    // Nothing OS Audio Pulse Indicator / 1-Tap Quick Resume
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(
                                1.dp,
                                if (isPlaying) NothingThemeTokens.Red else NothingThemeTokens.Border,
                                RoundedCornerShape(percent = 50),
                            )
                            .then(
                                if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                                    Modifier.clickable { onResumeClick() }
                                } else Modifier
                            )
                            .padding(horizontal = 7.dp, vertical = 3.5.dp),
                    ) {
                        if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                            Text(
                                text = if (isPlaying) "⏸" else "▶",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (isPlaying) NothingThemeTokens.Red else NothingThemeTokens.White,
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (isPlaying) "PAUSE" else "RESUME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = if (isPlaying) NothingThemeTokens.Red else NothingThemeTokens.White,
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        listOf(bar1, bar2, bar3).forEach { barHeight ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height((if (isPlaying || currentSongTitle.isNullOrBlank()) barHeight * 0.7f else 4f).dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(NothingThemeTokens.Red),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Main headline greeting
                val headlineText = if (!firstName.isNullOrBlank()) {
                    "${timeOfDay.greeting}, $firstName"
                } else {
                    "${timeOfDay.greeting}, listener"
                }

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = NothingThemeTokens.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                // Single-line compact subtitle (shows current/paused song if available)
                val effectiveSubtitle = when {
                    !currentSongTitle.isNullOrBlank() && isPlaying -> "Playing • $currentSongTitle"
                    !currentSongTitle.isNullOrBlank() && !isPlaying -> "Paused • $currentSongTitle"
                    else -> subtitle
                }
                Text(
                    text = effectiveSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = NothingThemeTokens.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Contextual Mood Chips (tight, compact micro-pills)
                if (onMoodClick != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        timeOfDay.moodPills.forEach { mood ->
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = NothingThemeTokens.SurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NothingThemeTokens.Border),
                                modifier = Modifier.clickable { onMoodClick(mood) },
                            ) {
                                Text(
                                    text = mood,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                    ),
                                    color = NothingThemeTokens.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Classic Aura: Luxurious Celestial Atmospheric Card with Ambient Radial Gradient (Compact Edition)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF10121C),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 3.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    ),
                    shape = RoundedCornerShape(18.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = timeOfDay.gradientColors,
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    // Top Row: Time Period Tag + Mini Equalizer / 1-Tap Quick Resume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.White.copy(alpha = 0.12f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = timeOfDay.emoji,
                                    fontSize = 11.sp,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = timeOfDay.periodTag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontSize = 10.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }

                        // Right: Quick Resume Pill + Dancing Equalizer Bars
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isPlaying) Color(0xFF00D2FF).copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) Color(0xFF00D2FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier.then(
                                if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                                    Modifier.clickable { onResumeClick() }
                                } else Modifier
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                            ) {
                                if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                                    Text(
                                        text = if (isPlaying) "⏸" else "▶",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlaying) "Playing" else "Resume",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(11.dp),
                                ) {
                                    listOf(bar1, bar2, bar3).forEach { barHeight ->
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height((if (isPlaying || currentSongTitle.isNullOrBlank()) barHeight * 0.7f else 4f).dp)
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(Color(0xFF00D2FF)),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Dynamic Personalized Greeting
                    val greetingHeadline = if (!firstName.isNullOrBlank()) {
                        "${timeOfDay.greeting}, $firstName ${timeOfDay.emoji}"
                    } else {
                        "${timeOfDay.greeting}, listener ${timeOfDay.emoji}"
                    }

                    Text(
                        text = greetingHeadline,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(2.dp))

                    val effectiveClassicSubtitle = when {
                        !currentSongTitle.isNullOrBlank() && isPlaying -> "Playing • $currentSongTitle"
                        !currentSongTitle.isNullOrBlank() && !isPlaying -> "Paused • $currentSongTitle"
                        else -> subtitle
                    }
                    Text(
                        text = effectiveClassicSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        ),
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Contextual Mood Chips
                    if (onMoodClick != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            timeOfDay.moodPills.forEach { mood ->
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    modifier = Modifier.clickable { onMoodClick(mood) },
                                ) {
                                    Text(
                                        text = mood,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
 * Feature Highlights carousel showcasing AuraMusic's flagship features
 * with one-tap action triggers and high-end visual design.
 */
@Composable
fun FeatureHighlightsStrip(
    onFeatureClick: (AuraFeature) -> Unit,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
    ) {
        items(AuraFeature.values()) { feature ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (nothingOsMode) NothingThemeTokens.Surface else Color(0xFF141724),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.10f),
                ),
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onFeatureClick(feature) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else feature.gradientColors.first().copy(alpha = 0.20f),
                        ) {
                            Text(
                                text = feature.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                ),
                                color = if (nothingOsMode) NothingThemeTokens.Red else feature.gradientColors.first(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }

                        Text(feature.icon, fontSize = 20.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (nothingOsMode) NothingThemeTokens.Red else feature.gradientColors.first(),
                            modifier = Modifier.clickable { onFeatureClick(feature) },
                        ) {
                            Text(
                                text = feature.actionLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = if (nothingOsMode) NothingThemeTokens.White else Color.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
