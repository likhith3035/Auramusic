package com.music.bitchord.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Time of day categorization with curated celestial styling, mood responses, and icons.
 */
enum class TimeOfDay(
    val greeting: String,
    val emoji: String,
    val periodTag: String,
    val dotMatrixGreeting: String,
    val subtitles: List<String>,
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
            "Late night reflections, soft lo-fi beats & ambient soundscapes",
            "Dim the lights and immerse in deep midnight frequencies",
            "Tranquil nighttime acoustics for restful dreaming",
        ),
        gradientColors = listOf(
            Color(0xFF131533).copy(alpha = 0.55f),
            Color(0xFF0E1024).copy(alpha = 0.65f),
            Color(0xFF070812).copy(alpha = 0.85f),
        ),
    );

    fun pickSubtitle(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return subtitles[dayOfYear % subtitles.size]
    }

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
 * and dual-mode theme support (Classic Aura glassmorphism vs Nothing OS AMOLED dot matrix).
 */
@Composable
fun AuraGreetingHeader(
    accountName: String?,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
    timeOfDay: TimeOfDay = remember { TimeOfDay.current() },
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
        // Nothing OS Mode: AMOLED Pitch Black + NDot Matrix + Red Glyph
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NothingThemeTokens.Surface,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
                .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(22.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                // Top Row: Pulsing LED glyph, Matrix Tag, and Live Clock Readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NothingThemeTokens.Red),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = timeOfDay.dotMatrixGreeting,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                            color = NothingThemeTokens.White,
                        )
                    }

                    // Nothing OS Audio Pulse Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(NothingThemeTokens.SurfaceElevated)
                            .border(1.dp, NothingThemeTokens.Border, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        listOf(bar1, bar2, bar3).forEach { barHeight ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(NothingThemeTokens.Red),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Main headline greeting
                val headlineText = if (!firstName.isNullOrBlank()) {
                    "${timeOfDay.greeting}, $firstName"
                } else {
                    "${timeOfDay.greeting}, listener"
                }

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = NothingThemeTokens.White,
                )

                Spacer(Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    color = NothingThemeTokens.MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        // Classic Aura: Luxurious Celestial Atmospheric Card with Ambient Radial Gradient
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF10121C),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f),
                        ),
                    ),
                    shape = RoundedCornerShape(26.dp),
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
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    // Top Row: Time Period Tag + Live Clock & Mini Equalizer
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = timeOfDay.emoji,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = timeOfDay.periodTag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }

                        // Right: Dancing Equalizer Bars
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(14.dp),
                            ) {
                                listOf(bar1, bar2, bar3).forEach { barHeight ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(Color(0xFF00D2FF)),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Dynamic Personalized Greeting
                    val greetingHeadline = if (!firstName.isNullOrBlank()) {
                        "${timeOfDay.greeting}, $firstName ${timeOfDay.emoji}"
                    } else {
                        "${timeOfDay.greeting}, listener ${timeOfDay.emoji}"
                    }

                    Text(
                        text = greetingHeadline,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp,
                        ),
                        color = Color.White,
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                        ),
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
