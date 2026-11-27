package com.music.bitchord.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.weather.AuraWeatherRepository
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.player.PlayerSettings
import com.music.bitchord.ui.rememberIsForeground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.theme.NothingThemeTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Aesthetic typography personalities for the Home screen celestial canopy clock.
 * Features ultra-bold display kerning matching flagship mobile lockscreens,
 * minimalist luxury, Nothing OS digital monospace, and editorial serif.
 */
enum class AuraClockStyle(
    val label: String,
    val fontWeight: FontWeight,
    val letterSpacing: Float,
    val fontSize: Float,
    val useMonospace: Boolean = false,
    val useSerif: Boolean = false,
    val shadowAlpha: Float = 0.55f,
) {
    HERO_BOLD(
        label = "Aura Bold Display",
        fontWeight = FontWeight.W800,
        letterSpacing = -1.5f,
        fontSize = 48f,
        shadowAlpha = 0.55f,
    ),
    MINIMAL_SLEEK(
        label = "Modern Sleek",
        fontWeight = FontWeight.W400,
        letterSpacing = -0.5f,
        fontSize = 46f,
        shadowAlpha = 0.45f,
    ),
    TECH_MONO(
        label = "Cyber Digital",
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8f,
        fontSize = 42f,
        useMonospace = true,
        shadowAlpha = 0.50f,
    ),
    EDITORIAL_SERIF(
        label = "Editorial Serif",
        fontWeight = FontWeight.Bold,
        letterSpacing = -0.5f,
        fontSize = 46f,
        useSerif = true,
        shadowAlpha = 0.50f,
    ),
}

/**
 * Flagship capabilities highlighted in the Home screen feature discovery carousel.
 */
enum class AuraFeature(
    val title: String,
    val subtitle: String,
    val badge: String,
    val actionLabel: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
) {
    STUDIO_EQ(
        title = "10-Band Studio EQ",
        subtitle = "Parametric audio curve & bass boost",
        badge = "STUDIO",
        actionLabel = "Tune EQ",
        icon = Icons.Rounded.GraphicEq,
        gradientColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    ),
    LISTEN_TOGETHER(
        title = "Listen Together",
        subtitle = "Zero-drift party rooms with live soundbites",
        badge = "SOCIAL",
        actionLabel = "Party",
        icon = Icons.Rounded.Radio,
        gradientColors = listOf(Color(0xFFFF007F), Color(0xFF7928CA)),
    ),
    KARAOKE_LYRICS(
        title = "Live Synced Lyrics",
        subtitle = "Word-by-word karaoke & live translation",
        badge = "LIVE",
        actionLabel = "Sing",
        icon = Icons.Rounded.Mic,
        gradientColors = listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)),
    ),
    SLEEP_TIMER(
        title = "Smart Sleep Timer",
        subtitle = "Gentle fade-out while you drift to sleep",
        badge = "NIGHT",
        actionLabel = "Timer",
        icon = Icons.Rounded.Bedtime,
        gradientColors = listOf(Color(0xFF8338EC), Color(0xFF3A86FF)),
    ),
    REPLAY_2026(
        title = "2026 Replay Recap",
        subtitle = "Your top artists, tracks & hologram card",
        badge = "SPECIAL",
        actionLabel = "View",
        icon = Icons.Rounded.Star,
        gradientColors = listOf(Color(0xFFFFB703), Color(0xFFFB8500)),
    ),
    AURA_AI(
        title = "Aura AI Companion",
        subtitle = "Acoustic psychology, roasts & instant recommendations",
        badge = "GEN-AI",
        actionLabel = "Chat",
        icon = Icons.Rounded.AutoAwesome,
        gradientColors = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
    ),
}

/**
 * Time of day categorization with curated celestial styling, mood responses, and clean styling.
 */
enum class TimeOfDay(
    val greeting: String,
    val periodTag: String,
    val dotMatrixGreeting: String,
    val subtitles: List<String>,
    val moodPills: List<String>,
    val gradientColors: List<Color>,
) {
    MORNING(
        greeting = "Good morning",
        periodTag = "SUNRISE VIBES",
        dotMatrixGreeting = "G O O D   M O R N I N G",
        subtitles = listOf(
            "Start your morning with energizing acoustic & fresh beats",
            "Crisp morning melodies brewed to fuel your day",
            "Your morning coffee soundtrack is curated and ready",
        ),
        moodPills = listOf("Morning Energy", "Acoustic Brew", "Mindful Wakeup", "Fresh Beats"),
        gradientColors = listOf(
            Color(0xFF381512).copy(alpha = 0.55f),
            Color(0xFF1E1026).copy(alpha = 0.65f),
            Color(0xFF0F0B14).copy(alpha = 0.85f),
        ),
    ),
    AFTERNOON(
        greeting = "Good afternoon",
        periodTag = "MIDDAY ENERGY",
        dotMatrixGreeting = "G O O D   A F T E R N O O N",
        subtitles = listOf(
            "Power through your afternoon with focus beats & favorite grooves",
            "Midday energy boost - upbeat rhythms to keep momentum alive",
            "Stay locked in the zone with your daily focus soundtrack",
        ),
        moodPills = listOf("Midday Focus", "Upbeat Hits", "Deep Flow", "Lofi Chill"),
        gradientColors = listOf(
            Color(0xFF0C243B).copy(alpha = 0.55f),
            Color(0xFF0C1B2E).copy(alpha = 0.65f),
            Color(0xFF080D1A).copy(alpha = 0.85f),
        ),
    ),
    EVENING(
        greeting = "Good evening",
        periodTag = "GOLDEN HOUR",
        dotMatrixGreeting = "G O O D   E V E N I N G",
        subtitles = listOf(
            "Unwind and recharge with chill vibes & golden hour melodies",
            "Dusk is here - slow down and let the soul rhythms play",
            "Sunset melodies crafted for a relaxed, serene evening",
        ),
        moodPills = listOf("Golden Hour", "Sunset Melodies", "Chill R&B", "Smooth Soul"),
        gradientColors = listOf(
            Color(0xFF35122F).copy(alpha = 0.55f),
            Color(0xFF1E112E).copy(alpha = 0.65f),
            Color(0xFF0E0A1A).copy(alpha = 0.85f),
        ),
    ),
    NIGHT(
        greeting = "Good night",
        periodTag = "MIDNIGHT AURA",
        dotMatrixGreeting = "G O O D   N I G H T",
        subtitles = listOf(
            "Late night reflections & soft, immersive ambient soundscapes",
            "Dim the room lights and immerse in deep midnight audio",
            "Gentle midnight melodies designed for peaceful dreams",
        ),
        moodPills = listOf("Late Night Lo-Fi", "Deep Ambient", "Sleep Soundscapes", "Piano Dreams"),
        gradientColors = listOf(
            Color(0xFF131038).copy(alpha = 0.55f),
            Color(0xFF0C1026).copy(alpha = 0.65f),
            Color(0xFF060812).copy(alpha = 0.85f),
        ),
    );

    fun pickSubtitle(): String = subtitles.random()

    companion object {
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }

        fun current(): TimeOfDay = fromHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
}

/**
 * High-end dynamic greeting header rendered at the top of the AuraMusic Home feed.
 * Features an atmospheric celestial card, live digital clock pill, animated equalizer indicator,
 * dynamic contextual mood chips, and dual-mode theme support (Classic Aura vs Nothing OS).
 * Strictly zero unicode emojis - clean vector glyphs and typography only.
 */
@Composable
fun AuraGreetingHeader(
    accountName: String?,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    timeOfDay: TimeOfDay? = null,
    currentSongTitle: String? = null,
    isPlaying: Boolean = false,
    onResumeClick: (() -> Unit)? = null,
    onMoodClick: ((String) -> Unit)? = null,
) {
    val firstName = remember(accountName) {
        accountName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    val weather by AuraWeatherRepository.currentWeather.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val isForeground = rememberIsForeground()
    var showWeatherSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AuraWeatherRepository.refresh()
    }

    if (showWeatherSheet) {
        AuraWeatherLocationSheet(
            nothingOsMode = nothingOsMode,
            onDismiss = { showWeatherSheet = false },
        )
    }

    // Real-time ticking clock aligned to minute boundaries (pauses while backgrounded)
    var clockCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        while (true) {
            clockCalendar = Calendar.getInstance()
            AuraWeatherRepository.refreshCelestialState()
            val nowMs = System.currentTimeMillis()
            val delayMs = (60_000L - (nowMs % 60_000L)).coerceAtLeast(1000L)
            delay(delayMs)
        }
    }

    val (clockTime, clockPeriod) = remember(clockCalendar) {
        val hour12 = clockCalendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = String.format(Locale.getDefault(), "%02d", clockCalendar.get(Calendar.MINUTE))
        val period = if (clockCalendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        Pair("$hour12:$minute", period)
    }

    val clockDate = remember(clockCalendar) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(clockCalendar.time)
    }

    val currentHour = clockCalendar.get(Calendar.HOUR_OF_DAY)
    val effectiveTimeOfDay = remember(timeOfDay, currentHour) {
        timeOfDay ?: TimeOfDay.fromHour(currentHour)
    }

    val headlineGreeting = remember(effectiveTimeOfDay, firstName) {
        if (!firstName.isNullOrBlank()) {
            "${effectiveTimeOfDay.greeting}, $firstName"
        } else {
            effectiveTimeOfDay.greeting
        }
    }

    val ambientTimeSubtitle = remember(effectiveTimeOfDay) {
        effectiveTimeOfDay.pickSubtitle()
    }

    val weatherSummary = remember(weather) {
        buildString {
            append(weather.displayTemperature)
            append(" · ")
            append(weather.conditionLabel)
            if (!weather.city.isNullOrBlank()) {
                append(" · ")
                append(weather.city)
            }
        }
    }

    val effectiveSubtitle = when {
        !currentSongTitle.isNullOrBlank() && isPlaying -> "Playing - $currentSongTitle"
        !currentSongTitle.isNullOrBlank() && !isPlaying -> "Paused - $currentSongTitle"
        else -> ambientTimeSubtitle
    }

    val appHost = remember { runCatching { AppUi.host }.getOrNull() }
    val fallbackClockFlow = remember(appHost) {
        if (appHost == null) kotlinx.coroutines.flow.MutableStateFlow(0) else null
    }
    val persistedClockIndex by (appHost?.homeClockStyleIndex ?: fallbackClockFlow!!).collectAsStateWithLifecycle()
    var localClockStyleIndex by remember { mutableIntStateOf(0) }
    val activeClockIndex = if (appHost != null) persistedClockIndex else localClockStyleIndex

    val isDarkBackdrop = nothingOsMode || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val blendColor = if (nothingOsMode) NothingThemeTokens.Black else MaterialTheme.colorScheme.background
    val primaryInk = when {
        nothingOsMode -> NothingThemeTokens.White
        isDarkBackdrop -> Color.White
        else -> Color(0xFF0F172A)
    }
    val secondaryInk = when {
        nothingOsMode -> NothingThemeTokens.MutedText
        isDarkBackdrop -> Color.White.copy(alpha = 0.80f)
        else -> Color(0xFF334155)
    }
    val subtleInk = when {
        nothingOsMode -> NothingThemeTokens.MutedText
        isDarkBackdrop -> Color.White.copy(alpha = 0.72f)
        else -> Color(0xFF475569)
    }
    val topContentPad = (topInset + 4.dp).coerceAtLeast(12.dp)

    // Frameless, edge-feathered atmospheric canopy with living celestial canvas
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topInset + 204.dp),
    ) {
        LivingWeatherCanvas(
            visualType = weather.visualType,
            blendColor = blendColor,
            reduceAnimation = reduceAnimation,
            isDay = weather.isDay,
            lightMode = !isDarkBackdrop,
            topInset = topInset,
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle tap target on the right celestial sky area for opening weather/location controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = topContentPad, end = 12.dp, bottom = 18.dp)
                .width(120.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { showWeatherSheet = true },
        )

        val styles = AuraClockStyle.entries
        val activeClockStyle = styles[activeClockIndex.mod(styles.size)]
        val clockFontFamily = when {
            nothingOsMode || activeClockStyle.useMonospace -> FontFamily.Monospace
            activeClockStyle.useSerif -> FontFamily.Serif
            else -> MaterialTheme.typography.displayLarge.fontFamily ?: FontFamily.SansSerif
        }
        val bodyFontFamily = if (nothingOsMode) {
            FontFamily.Monospace
        } else {
            MaterialTheme.typography.bodyMedium.fontFamily ?: FontFamily.SansSerif
        }
        val textShadowColor = if (isDarkBackdrop) {
            Color.Black.copy(alpha = activeClockStyle.shadowAlpha)
        } else {
            Color.White.copy(alpha = 0.45f)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = topContentPad, bottom = 14.dp),
        ) {
            val celestialReserve = if (maxWidth < 360.dp) 64.dp else 92.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {
                // Top Section: Real-Time Digital Clock & Date with persisted tap-to-cycle font personality
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) {
                            val next = (activeClockIndex + 1).mod(styles.size)
                            if (appHost != null) {
                                appHost.setHomeClockStyleIndex(next)
                            } else {
                                localClockStyleIndex = next
                            }
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = clockTime,
                            style = TextStyle(
                                fontFamily = clockFontFamily,
                                fontWeight = activeClockStyle.fontWeight,
                                fontSize = activeClockStyle.fontSize.sp,
                                letterSpacing = activeClockStyle.letterSpacing.sp,
                                lineHeight = activeClockStyle.fontSize.sp,
                                shadow = Shadow(
                                    color = textShadowColor,
                                    offset = Offset(0f, 3f),
                                    blurRadius = 8f,
                                ),
                            ),
                            color = primaryInk,
                        )
                        if (clockPeriod.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = clockPeriod,
                                style = TextStyle(
                                    fontFamily = clockFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = 0.5.sp,
                                    shadow = Shadow(
                                        color = textShadowColor.copy(alpha = (textShadowColor.alpha * 0.85f).coerceIn(0f, 1f)),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 6f,
                                    ),
                                ),
                                color = primaryInk.copy(alpha = 0.90f),
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = clockDate,
                        style = TextStyle(
                            fontFamily = bodyFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            letterSpacing = 0.2.sp,
                            shadow = Shadow(
                                color = textShadowColor.copy(alpha = (textShadowColor.alpha * 0.70f).coerceIn(0f, 1f)),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f,
                            ),
                        ),
                        color = secondaryInk,
                    )
                }

                Spacer(Modifier.height(7.dp))

                // Always-accessible tappable weather pill (even while music is playing)
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = when {
                        nothingOsMode -> NothingThemeTokens.SurfaceElevated.copy(alpha = 0.75f)
                        isDarkBackdrop -> Color.White.copy(alpha = 0.11f)
                        else -> Color.White.copy(alpha = 0.58f)
                    },
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = when {
                            nothingOsMode -> NothingThemeTokens.Border
                            isDarkBackdrop -> Color.White.copy(alpha = 0.14f)
                            else -> Color(0xFF0F172A).copy(alpha = 0.10f)
                        },
                    ),
                    modifier = Modifier
                        .padding(end = celestialReserve)
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(role = Role.Button) { showWeatherSheet = true },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Weather & location",
                            tint = if (nothingOsMode) NothingThemeTokens.Red else secondaryInk,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = weatherSummary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = bodyFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                            ),
                            color = primaryInk.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Section: Dynamic Headline Greeting & Playback/Atmospheric Subtitle
                Column(modifier = Modifier.padding(end = celestialReserve)) {
                    Text(
                        text = headlineGreeting,
                        style = TextStyle(
                            fontFamily = bodyFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.4).sp,
                            shadow = Shadow(
                                color = textShadowColor.copy(alpha = (textShadowColor.alpha * 0.85f).coerceIn(0f, 1f)),
                                offset = Offset(0f, 2f),
                                blurRadius = 6f,
                            ),
                        ),
                        color = primaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .then(
                                if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                                    Modifier.clickable(role = Role.Button) { onResumeClick() }
                                } else {
                                    Modifier.clickable(role = Role.Button) { showWeatherSheet = true }
                                },
                            ),
                    ) {
                        if (onResumeClick != null && !currentSongTitle.isNullOrBlank()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Resume",
                                tint = if (nothingOsMode && isPlaying) NothingThemeTokens.Red else primaryInk,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = effectiveSubtitle,
                            style = TextStyle(
                                fontFamily = bodyFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                shadow = Shadow(
                                    color = textShadowColor.copy(alpha = (textShadowColor.alpha * 0.72f).coerceIn(0f, 1f)),
                                    offset = Offset(0f, 1.5f),
                                    blurRadius = 4f,
                                ),
                            ),
                            color = subtleInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isPlaying && !currentSongTitle.isNullOrBlank()) {
                            Spacer(Modifier.width(6.dp))
                            GreetingWaveformBars(
                                tint = if (nothingOsMode) NothingThemeTokens.Red else if (isDarkBackdrop) Color(0xFF00D2FF) else Color(0xFF0284C7),
                                animated = !reduceAnimation && isForeground,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingWaveformBars(
    tint: Color,
    animated: Boolean,
) {
    // Only instantiate the infinite transition while playing and motion is enabled;
    // draw directly inside Canvas so parent header layout never invalidates per frame.
    if (!animated) {
        Canvas(modifier = Modifier.size(width = 10.dp, height = 10.dp)) {
            val barW = 2.dp.toPx()
            val gap = 2.dp.toPx()
            val corner = CornerRadius(barW * 0.5f, barW * 0.5f)
            val heights = floatArrayOf(0.55f, 0.90f, 0.45f)
            for (i in 0..2) {
                val h = size.height * heights[i]
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(i * (barW + gap), size.height - h),
                    size = Size(barW, h),
                    cornerRadius = corner,
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "greeting_pulse")
    val bar1 = transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb1",
    )
    val bar2 = transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb2",
    )
    val bar3 = transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gb3",
    )
    Canvas(modifier = Modifier.size(width = 10.dp, height = 10.dp)) {
        val barW = 2.dp.toPx()
        val gap = 2.dp.toPx()
        val corner = CornerRadius(barW * 0.5f, barW * 0.5f)
        val h1 = size.height * bar1.value
        val h2 = size.height * bar2.value
        val h3 = size.height * bar3.value
        drawRoundRect(tint, Offset(0f, size.height - h1), Size(barW, h1), corner)
        drawRoundRect(tint, Offset(barW + gap, size.height - h2), Size(barW, h2), corner)
        drawRoundRect(tint, Offset((barW + gap) * 2f, size.height - h3), Size(barW, h3), corner)
    }
}

/**
 * Feature Highlights carousel showcasing AuraMusic's flagship features
 * with uniform compact card dimensions, space-saving layout, and vector iconography.
 */
@Composable
fun FeatureHighlightsStrip(
    onFeatureClick: (AuraFeature) -> Unit,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
    ) {
        items(
            items = AuraFeature.entries,
            key = { it.name },
            contentType = { "feature_card" },
        ) { feature ->
            val cardBg = when {
                nothingOsMode && isDarkTheme -> NothingThemeTokens.Surface
                nothingOsMode -> Color(0xFFF3F3F7)
                isDarkTheme -> Color(0xFF131622)
                else -> Color(0xFFF3F5FA)
            }
            val cardBorder = when {
                nothingOsMode && isDarkTheme -> NothingThemeTokens.Border
                nothingOsMode -> Color(0xFFD4D4DC)
                isDarkTheme -> Color.White.copy(alpha = 0.09f)
                else -> Color(0xFF0F172A).copy(alpha = 0.09f)
            }
            val titleColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
            val subtitleColor = when {
                nothingOsMode && isDarkTheme -> NothingThemeTokens.MutedText
                isDarkTheme -> Color.White.copy(alpha = 0.60f)
                else -> Color(0xFF475569)
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = cardBorder,
                ),
                modifier = Modifier
                    .width(162.dp)
                    .height(98.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onFeatureClick(feature) },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle ambient accent glow in top-right corner
                    if (!nothingOsMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(56.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            feature.gradientColors.first().copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Top Row: Compact Category Badge + Feature Icon (Vector)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (nothingOsMode) {
                                    if (isDarkTheme) NothingThemeTokens.SurfaceElevated else Color(0xFFE7E7EE)
                                } else feature.gradientColors.first().copy(alpha = if (isDarkTheme) 0.18f else 0.14f),
                                border = if (nothingOsMode) androidx.compose.foundation.BorderStroke(1.dp, cardBorder) else null,
                            ) {
                                Text(
                                    text = feature.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp,
                                        letterSpacing = 0.8.sp,
                                    ),
                                    color = if (nothingOsMode) NothingThemeTokens.Red else feature.gradientColors.first(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (nothingOsMode) {
                                            if (isDarkTheme) NothingThemeTokens.SurfaceElevated else Color(0xFFE7E7EE)
                                        } else feature.gradientColors.first().copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = if (nothingOsMode) titleColor else feature.gradientColors.first(),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }

                        // Bottom Section: Title, Subtitle, and subtle arrow
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = feature.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = feature.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                    ),
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )

                                Icon(
                                    imageVector = BitChordIcons.ChevronRight,
                                    contentDescription = null,
                                    tint = if (nothingOsMode) NothingThemeTokens.Red else feature.gradientColors.first().copy(alpha = 0.90f),
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
