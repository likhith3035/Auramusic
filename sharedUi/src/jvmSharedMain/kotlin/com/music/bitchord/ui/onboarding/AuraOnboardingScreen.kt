package com.music.bitchord.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.ui.AppUi
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
 */
@Composable
fun AuraOnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val nothingOsMode by AppUi.host.nothingOsMode.collectAsStateWithLifecycle()

    val currentStep = ONBOARDING_STEPS[currentStepIndex]
    val isLastStep = currentStepIndex == ONBOARDING_STEPS.lastIndex

    // Subtle dancing equalizer animation for slide 1
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_eq")
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 44f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    val eq4 by infiniteTransition.animateFloat(
        initialValue = 52f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4",
    )
    val eq5 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 42f,
        animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq5",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (nothingOsMode) NothingThemeTokens.Black else Color(0xFF090A10)),
    ) {
        // Subtle ambient radial glow behind active step
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            currentStep.accentColors.first().copy(alpha = if (nothingOsMode) 0.12f else 0.28f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top App Bar: Brand mark + Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AURAMUSIC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                    )
                }

                TextButton(
                    onClick = onFinished,
                ) {
                    Text(
                        text = if (nothingOsMode) "SKIP" else "Skip",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.65f),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Center Content: Interactive Visualizer + Carousel Slide Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                        }
                    },
                    label = "onboarding_step_content",
                ) { stepIdx ->
                    val step = ONBOARDING_STEPS[stepIdx]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Visual Art Container per step
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    if (nothingOsMode) NothingThemeTokens.Surface
                                    else Color(0xFF141724).copy(alpha = 0.85f),
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = if (nothingOsMode) {
                                            listOf(NothingThemeTokens.Border, NothingThemeTokens.Border)
                                        } else {
                                            listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))
                                        },
                                    ),
                                    shape = RoundedCornerShape(32.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (stepIdx) {
                                0 -> {
                                    // Animated Equalizer Visualizer
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.height(70.dp),
                                    ) {
                                        listOf(eq1, eq2, eq3, eq4, eq5).forEachIndexed { i, barHeight ->
                                            val barBrush = if (nothingOsMode) {
                                                Brush.verticalGradient(listOf(NothingThemeTokens.Red, NothingThemeTokens.Red))
                                            } else {
                                                Brush.verticalGradient(listOf(step.accentColors[0], step.accentColors[1]))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(8.dp)
                                                    .height(barHeight.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(barBrush),
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    // Party Sync Soundbite Emojis
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text("📻", fontSize = 38.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("🔥", "🎺", "🚀", "🎉").forEach { emoji ->
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(30.dp),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(emoji, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Karaoke Lyrics Microphone & Wave
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("🎤", fontSize = 38.sp)
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = if (nothingOsMode) NothingThemeTokens.Red.copy(alpha = 0.2f) else Color(0xFF00F5D4).copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                text = "♪ Sing along in sync",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                ),
                                                color = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00F5D4),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                                3 -> {
                                    // Theme Toggle Preview
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(if (nothingOsMode) "⬛" else "🔮", fontSize = 36.sp)
                                        Text(
                                            text = if (nothingOsMode) "NOTHING OS" else "CLASSIC AURA",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                            ),
                                            color = if (nothingOsMode) NothingThemeTokens.White else Color(0xFF00D2FF),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

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
                                    letterSpacing = 1.4.sp,
                                ),
                                color = if (nothingOsMode) NothingThemeTokens.Red else step.accentColors.first(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Headline Title
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(10.dp))

                        // Descriptive Subtitle
                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                        // Interactive Theme Picker on Step 3
                        if (stepIdx == 3) {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // Option A: Classic Aura
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (!nothingOsMode) Color(0xFF1E1033) else NothingThemeTokens.Surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (!nothingOsMode) 2.dp else 1.dp,
                                        color = if (!nothingOsMode) Color(0xFF9D4EDD) else NothingThemeTokens.Border,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { AppUi.host.setNothingOsMode(false) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("🔮", fontSize = 20.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Classic Aura",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Vibrant & Glass",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White.copy(alpha = 0.65f),
                                        )
                                    }
                                }

                                // Option B: Nothing OS Mode
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (nothingOsMode) 2.dp else 1.dp,
                                        color = if (nothingOsMode) NothingThemeTokens.Red else NothingThemeTokens.Border,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { AppUi.host.setNothingOsMode(true) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("🔴", fontSize = 20.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Nothing OS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = NothingThemeTokens.White,
                                        )
                                        Text(
                                            text = "AMOLED Matrix",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                            ),
                                            color = NothingThemeTokens.MutedText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Bottom Navigation: Indicator Dots + Next/Finish Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ONBOARDING_STEPS.indices.forEach { index ->
                        val isSelected = index == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 24.dp else 6.dp)
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
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = if (isLastStep) {
                            if (nothingOsMode) "START LISTENING →" else "Get Started ✨"
                        } else {
                            if (nothingOsMode) "NEXT →" else "Next →"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (nothingOsMode) 1.sp else 0.sp,
                        ),
                    )
                }
            }
        }
    }
}
