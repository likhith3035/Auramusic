package com.music.bitchord.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.aura_splash_screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

/**
 * Data representation of an atmospheric floating audio ember particle.
 */
private data class SplashParticle(
    val initialX: Float,
    val initialY: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val swayAmount: Float,
)

/**
 * High-end cinematic opening splash screen for AuraMusic.
 * Displays the official "AuraMusic BY LIKHITH" artwork on AMOLED pitch black
 * with multi-layered micro-animations:
 *
 * 1. Radiant multi-color ambient nebula that rotates and breathes smoothly.
 * 2. Drifting cyber-neon audio embers floating gently upwards.
 * 3. Holographic light sheen gliding diagonally across the artwork.
 * 4. Micro-breathing scale on the central soundwave ribbon.
 * 5. Smooth dissolve exit handoff to the preloaded app (instant tap to skip).
 */
@Composable
fun AuraSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overallAlpha = remember { Animatable(1f) }
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.88f) }
    val sheenProgress = remember { Animatable(-0.5f) }

    // Seeded floating ambient particles
    val particles = remember {
        val colors = listOf(
            Color(0xFF00D2FF), // Cyan
            Color(0xFF9D4EDD), // Electric Purple
            Color(0xFFFF007F), // Neon Magenta
            Color(0xFF00F5D4), // Mint Neon
        )
        val rng = Random(42)
        List(16) {
            SplashParticle(
                initialX = rng.nextFloat(),
                initialY = rng.nextFloat(),
                size = 1.5f + rng.nextFloat() * 2.5f,
                color = colors[it % colors.size],
                speed = 0.08f + rng.nextFloat() * 0.12f,
                swayAmount = 10f + rng.nextFloat() * 15f,
            )
        }
    }

    // Continuous infinite transitions for breathing aura
    val infiniteTransition = rememberInfiniteTransition(label = "aura_splash_infinite")

    // Slow ambient rotation of the chromatic nebula
    val nebulaRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nebula_rotation",
    )

    // Breathing aura bloom
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_glow",
    )
    val auraScalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_scale",
    )

    // Micro-breathing scale for the central soundwave graphic
    val imageBreathPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.022f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "image_pulse",
    )

    // Upward drift progression for floating embers
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_drift",
    )

    val smoothDecel = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    LaunchedEffect(Unit) {
        // Entrance: 0ms -> 650ms
        launch {
            entranceAlpha.animateTo(1f, animationSpec = tween(600, easing = smoothDecel))
        }
        launch {
            entranceScale.animateTo(1f, animationSpec = tween(700, easing = smoothDecel))
        }

        // Holographic sheen sweep starting at 700ms
        launch {
            delay(700)
            sheenProgress.animateTo(1.5f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
        }

        // Total 2550ms hold + 450ms exit dissolve = exactly 3000ms (3.0 seconds)
        delay(2550)

        overallAlpha.animateTo(0f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overallAlpha.value }
            .background(Color(0xFF000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onFinished()
            },
        contentAlignment = Alignment.Center,
    ) {
        // Layer 1: Ambient floating cyber-neon embers in the pitch black void
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width > 0 && height > 0) {
                particles.forEach { p ->
                    val progressY = (p.initialY - particleDrift * p.speed).let {
                        if (it < 0f) it + 1f else it
                    }
                    val xOffset = kotlin.math.sin(progressY * 6.28318f + p.initialX * 10f) * p.swayAmount
                    val x = (p.initialX * width + xOffset).coerceIn(0f, width)
                    val y = progressY * height
                    val particleAlpha = (0.35f + 0.45f * kotlin.math.sin(progressY * 3.14159f)) * entranceAlpha.value

                    drawCircle(
                        color = p.color.copy(alpha = particleAlpha.coerceIn(0f, 1f)),
                        radius = p.size.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }
        }

        // Layer 2: Rotating chromatic aura halo behind the soundwave ribbon
        Box(
            modifier = Modifier
                .size(340.dp)
                .graphicsLayer {
                    alpha = auraGlow * entranceAlpha.value * 0.85f
                    rotationZ = nebulaRotation
                    scaleX = entranceScale.value * auraScalePulse * 1.15f
                    scaleY = entranceScale.value * auraScalePulse * 1.15f
                }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00D2FF).copy(alpha = 0.35f),
                            Color(0xFF9D4EDD).copy(alpha = 0.30f),
                            Color(0xFFFF007F).copy(alpha = 0.25f),
                            Color(0xFF00F5D4).copy(alpha = 0.20f),
                            Color(0xFF00D2FF).copy(alpha = 0.35f),
                        ),
                    ),
                ),
        )

        // Layer 3: Concentric deep radial blur for intense center glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    alpha = auraGlow * entranceAlpha.value
                    scaleX = entranceScale.value * auraScalePulse
                    scaleY = entranceScale.value * auraScalePulse
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00D2FF).copy(alpha = 0.42f),
                            Color(0xFF9D4EDD).copy(alpha = 0.32f),
                            Color(0xFFFF007F).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Layer 4: Official AuraMusic splash artwork with micro-breathing scale
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(Res.drawable.aura_splash_screen),
                contentDescription = "AuraMusic by Likhith",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        scaleX = entranceScale.value * imageBreathPulse
                        scaleY = entranceScale.value * imageBreathPulse
                    },
            )

            // Holographic diagonal light sheen beam sweep across the logo
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (entranceAlpha.value * 0.32f).coerceIn(0f, 1f) },
            ) {
                val sheenX = size.width * sheenProgress.value
                val sheenWidth = size.width * 0.45f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00D2FF).copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.35f),
                            Color(0xFFFF007F).copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                        start = Offset(sheenX - sheenWidth, 0f),
                        end = Offset(sheenX + sheenWidth, size.height),
                    ),
                )
            }
        }
    }
}
