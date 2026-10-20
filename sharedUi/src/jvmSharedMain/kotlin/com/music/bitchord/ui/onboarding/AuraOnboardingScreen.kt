package com.music.bitchord.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.player.PlayerBackHandler
import com.music.bitchord.ui.theme.NothingThemeTokens

/**
 * Data representation of a step in the AuraMusic welcome onboarding walkthrough.
 */
private data class OnboardingStep(
    val badge: String,
    val title: String,
    val subtitle: String,
    val accentColors: List<Color>,
)

private val ONBOARDING_STEPS = listOf(
    OnboardingStep(
        badge = "HI-RES & DSD AUDIO",
        title = "Studio-Grade Lossless",
        subtitle = "Experience pure bit-perfect playback, 32-bit float internal audio pipeline, and a 10-band parametric studio equalizer with bass boost.",
        accentColors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    ),
    OnboardingStep(
        badge = "ZERO-DRIFT SYNC",
        title = "Listen Together Live",
        subtitle = "Host synchronized music sessions with friends worldwide in real time. Share party queues and broadcast live soundbite reactions with zero lag.",
        accentColors = listOf(Color(0xFFFF007F), Color(0xFF7928CA)),
    ),
    OnboardingStep(
        badge = "WORD-BY-WORD KARAOKE",
        title = "Live Synced Lyrics",
        subtitle = "Sing along with real-time timed lyrics, vocal syllable highlights, romanization for foreign languages, and one-tap live translations.",
        accentColors = listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)),
    ),
    OnboardingStep(
        badge = "CUSTOMIZE YOUR AURA",
        title = "Choose Your Aesthetic",
        subtitle = "Tailor your interface. Select between rich glassmorphic vibrance or minimalist AMOLED pitch-black with Nothing OS dot-matrix styling.",
        accentColors = listOf(Color(0xFFD71921), Color(0xFF9D4EDD)),
    ),
)

/**
 * High-end interactive onboarding tour for first-time launches and feature discovery.
 * Features state-of-the-art glassmorphic hero cards, interactive visualizers, and tactile navigation.
 */
@Composable
fun AuraOnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val nothingOsMode by AppUi.host.nothingOsMode.collectAsStateWithLifecycle()

    PlayerBackHandler(enabled = true) {
        if (currentStepIndex > 0) {
            currentStepIndex--
        } else {
            onFinished()
        }
    }

    val currentStep = ONBOARDING_STEPS[currentStepIndex]
    val isLastStep = currentStepIndex == ONBOARDING_STEPS.lastIndex

    // Dancing equalizer animation for Studio Lossless visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_eq")
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 46f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 40f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = 54f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    val eq4 by infiniteTransition.animateFloat(
        initialValue = 50f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4",
    )
    val eq5 by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 42f,
        animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq5",
    )
    val eq6 by infiniteTransition.animateFloat(
        initialValue = 36f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq6",
    )
    val eq7 by infiniteTransition.animateFloat(
        initialValue = 24f, targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq7",
    )
    val eq8 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq8",
    )

    // Breathing pulse for party sync and lyric highlight animations
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (nothingOsMode) NothingThemeTokens.Black else Color(0xFF0A0C14)),
    ) {
        // Ambient radial glow behind active step
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .size(340.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            currentStep.accentColors.first().copy(alpha = if (nothingOsMode) 0.12f else 0.26f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top App Bar: Brand mark + Tactile Glassmorphic Skip Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF)),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "AURAMUSIC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.4.sp,
                            fontSize = 11.sp,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                    )
                }

                // Sleek Tactile Skip Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (nothingOsMode) NothingThemeTokens.Surface else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.14f),
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { onFinished() },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (nothingOsMode) "SKIP" else "Skip",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = if (nothingOsMode) 1.sp else 0.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "→",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nothingOsMode) NothingThemeTokens.Red else Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Center Content: Interactive Visualizer + Carousel Slide Content
            var totalDragX by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(currentStepIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                if (totalDragX < -50f && currentStepIndex < ONBOARDING_STEPS.lastIndex) {
                                    currentStepIndex++
                                } else if (totalDragX > 50f && currentStepIndex > 0) {
                                    currentStepIndex--
                                }
                                totalDragX = 0f
                            },
                            onDragCancel = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(280)))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(280)))
                        }
                    },
                    label = "onboarding_step_content",
                ) { stepIdx ->
                    val step = ONBOARDING_STEPS[stepIdx]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // High-End Hero Showcase Card Container
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = if (nothingOsMode) NothingThemeTokens.Surface else Color(0xFF131625).copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier
                                .width(300.dp)
                                .height(172.dp)
                                .clip(RoundedCornerShape(26.dp)),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Subtle ambient highlight in corner
                                if (!nothingOsMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(90.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        step.accentColors.first().copy(alpha = 0.25f),
                                                        Color.Transparent,
                                                    ),
                                                ),
                                            ),
                                    )
                                }

                                when (stepIdx) {
                                    0 -> {
                                        // Slide 0: Studio-Grade Lossless (Parametric EQ & Hi-Res DAC Rack)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            // Top Status Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF261F0A),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        Color(0xFFFFD166).copy(alpha = 0.45f),
                                                    ),
                                                ) {
                                                    Text(
                                                        text = "HI-RES • 24-BIT / 192kHz",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 8.sp,
                                                            letterSpacing = 0.6.sp,
                                                        ),
                                                        color = Color(0xFFFFD166),
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF00F5D4)),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "BIT-PERFECT",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 8.5.sp,
                                                        ),
                                                        color = Color(0xFF00F5D4),
                                                    )
                                                }
                                            }

                                            // Center: 8-Band Equalizer Frequency Bars
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom,
                                            ) {
                                                val bars = listOf(eq1, eq2, eq3, eq4, eq5, eq6, eq7, eq8)
                                                val freqs = listOf("32", "64", "125", "250", "500", "1k", "4k", "16k")

                                                bars.forEachIndexed { i, barH ->
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Bottom,
                                                    ) {
                                                        // Floating peak dot
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (nothingOsMode) NothingThemeTokens.Red
                                                                    else Color.White.copy(alpha = 0.85f),
                                                                ),
                                                        )
                                                        Spacer(Modifier.height(3.dp))
                                                        // Spectrum Bar
                                                        Box(
                                                            modifier = Modifier
                                                                .width(7.dp)
                                                                .height(barH.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(
                                                                    if (nothingOsMode) {
                                                                        Brush.verticalGradient(
                                                                            listOf(NothingThemeTokens.Red, NothingThemeTokens.Red.copy(alpha = 0.4f)),
                                                                        )
                                                                    } else {
                                                                        Brush.verticalGradient(
                                                                            listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
                                                                        )
                                                                    },
                                                                ),
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                            text = freqs[i],
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 7.5.sp,
                                                            ),
                                                            color = Color.White.copy(alpha = 0.45f),
                                                        )
                                                    }
                                                }
                                            }

                                            // Bottom Specs Pill
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "🎛️ 10-Band Studio EQ",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.70f),
                                                )
                                                Text(
                                                    text = "+4.5 dB Bass Boost",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00C6FF),
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        // Slide 1: Listen Together Live (Zero-Drift Party Sync Room)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            // Top Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFFF007F)),
                                                    )
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = "LIVE PARTY ROOM",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 0.8.sp,
                                                        ),
                                                        color = Color(0xFFFF007F),
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(percent = 50),
                                                    color = Color(0xFF06D6A0).copy(alpha = 0.15f),
                                                ) {
                                                    Text(
                                                        text = "0ms DRIFT",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 8.5.sp,
                                                        ),
                                                        color = Color(0xFF06D6A0),
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }

                                            // Center: Party Avatars with audio pulse wave
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                // Overlapping Party Avatars
                                                val avatars = listOf("🎧", "✨", "🎸", "🔥")
                                                val avatarGradients = listOf(
                                                    listOf(Color(0xFFFF007F), Color(0xFF7928CA)),
                                                    listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)),
                                                    listOf(Color(0xFFFFB703), Color(0xFFFB8500)),
                                                    listOf(Color(0xFF8338EC), Color(0xFF3A86FF)),
                                                )

                                                avatars.forEachIndexed { i, icon ->
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(start = if (i > 0) (-10).dp else 0.dp)
                                                            .size(42.dp)
                                                            .clip(CircleShape)
                                                            .background(Brush.linearGradient(avatarGradients[i]))
                                                            .border(
                                                                width = 2.dp,
                                                                color = Color(0xFF131625),
                                                                shape = CircleShape,
                                                            ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(icon, fontSize = 16.sp)
                                                    }
                                                }
                                            }

                                            // Floating Soundbite Reactions
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                listOf("🔥 Fire", "🎺 Horn", "🚀 Vibe", "🎉 Party").forEach { reaction ->
                                                    Surface(
                                                        shape = RoundedCornerShape(percent = 50),
                                                        color = Color.White.copy(alpha = 0.08f),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            Color.White.copy(alpha = 0.12f),
                                                        ),
                                                    ) {
                                                        Text(
                                                            text = reaction,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Medium,
                                                            ),
                                                            color = Color.White.copy(alpha = 0.85f),
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Slide 2: Live Synced Lyrics (Word-by-Word Karaoke)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            // Top Header
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🎤", fontSize = 13.sp)
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = "KARAOKE SYNC",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 0.8.sp,
                                                        ),
                                                        color = Color(0xFF00F5D4),
                                                    )
                                                }

                                                Text(
                                                    text = "TRANSLATE & ROMANIZE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.sp,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.50f),
                                                )
                                            }

                                            // Center Lyrics Lines
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                // Previous Line
                                                Text(
                                                    text = "The city lights reflect into your eyes...",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 10.5.sp,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.32f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )

                                                // Active Glowing Line
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF00F5D4).copy(alpha = 0.12f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        Color(0xFF00F5D4).copy(alpha = 0.35f),
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            text = "Take me back to where we started ♪",
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                            ),
                                                            color = Color.White,
                                                            maxLines = 1,
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF00F5D4).copy(alpha = pulseAlpha)),
                                                        )
                                                    }
                                                }

                                                // Next Line
                                                Text(
                                                    text = "Before the morning breaks the silence...",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 10.5.sp,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.32f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            // Karaoke Progress Bar
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text("02:14", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.White.copy(alpha = 0.5f))
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(Color.White.copy(alpha = 0.15f)),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.65f)
                                                            .height(3.dp)
                                                            .background(Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)))),
                                                    )
                                                }
                                                Text("03:42", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.White.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                    3 -> {
                                        // Slide 3: Interactive Theme Showcase (Classic Aura vs Nothing OS)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            // Theme A: Classic Aura
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (!nothingOsMode) Color(0xFF22113A) else Color(0xFF141724),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (!nothingOsMode) 2.dp else 1.dp,
                                                    color = if (!nothingOsMode) Color(0xFF9D4EDD) else Color.White.copy(alpha = 0.10f),
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { AppUi.host.setNothingOsMode(false) },
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("🔮", fontSize = 16.sp)
                                                        if (!nothingOsMode) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 7.5.sp,
                                                                ),
                                                                color = Color(0xFF9D4EDD),
                                                            )
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "Classic Aura",
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.5.sp,
                                                            ),
                                                            color = Color.White,
                                                        )
                                                        Text(
                                                            text = "Glass & Vibrant Glow",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                            color = Color.White.copy(alpha = 0.60f),
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(percent = 50))
                                                            .background(
                                                                Brush.horizontalGradient(
                                                                    listOf(Color(0xFF00C6FF), Color(0xFF9D4EDD)),
                                                                ),
                                                            ),
                                                    )
                                                }
                                            }

                                            // Theme B: Nothing OS Mode
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (nothingOsMode) 2.dp else 1.dp,
                                                    color = if (nothingOsMode) NothingThemeTokens.Red else NothingThemeTokens.Border,
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { AppUi.host.setNothingOsMode(true) },
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text("🔴", fontSize = 16.sp)
                                                        if (nothingOsMode) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 7.5.sp,
                                                                ),
                                                                color = NothingThemeTokens.Red,
                                                            )
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "Nothing OS",
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.5.sp,
                                                            ),
                                                            color = NothingThemeTokens.White,
                                                        )
                                                        Text(
                                                            text = "AMOLED Dot Matrix",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 8.5.sp,
                                                            ),
                                                            color = NothingThemeTokens.MutedText,
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(percent = 50))
                                                            .background(NothingThemeTokens.Red),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Category Badge Pill
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.10f),
                            border = if (nothingOsMode) androidx.compose.foundation.BorderStroke(1.dp, NothingThemeTokens.Border) else null,
                        ) {
                            Text(
                                text = step.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.3.sp,
                                    fontSize = 9.sp,
                                ),
                                color = if (nothingOsMode) NothingThemeTokens.Red else step.accentColors.first(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Headline Title
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                fontSize = 22.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(8.dp))

                        // Descriptive Subtitle
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 19.sp,
                                fontSize = 12.5.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.68f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bottom Navigation: Expanding Pill Indicators + Action CTA Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Expanding Page Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ONBOARDING_STEPS.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 6.dp,
                            label = "indicator_width_$index",
                        )
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(indicatorWidth)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (isSelected) {
                                        if (nothingOsMode) NothingThemeTokens.Red else Color.White
                                    } else {
                                        if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.20f)
                                    },
                                )
                                .clickable { currentStepIndex = index },
                        )
                    }
                }

                // Action Buttons: Back + Next / Finish
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStepIndex > 0) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (nothingOsMode) NothingThemeTokens.Surface else Color.White.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.15f),
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { currentStepIndex-- },
                        ) {
                            Text(
                                text = if (nothingOsMode) "← BACK" else "← Back",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    letterSpacing = if (nothingOsMode) 1.sp else 0.sp,
                                ),
                                color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onFinished()
                            } else {
                                currentStepIndex++
                            }
                        },
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF),
                            contentColor = if (nothingOsMode) NothingThemeTokens.White else Color.Black,
                        ),
                        modifier = Modifier.height(46.dp),
                    ) {
                        Text(
                            text = if (isLastStep) {
                                if (nothingOsMode) "START LISTENING →" else "Get Started ✨"
                            } else {
                                if (nothingOsMode) "NEXT →" else "Next →"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = if (nothingOsMode) 1.sp else 0.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
