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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 1. Warm harmonic audio chime cue and tactile vibration on launch.
 * 2. Radiant multi-color ambient nebula that rotates and breathes smoothly.
 * 3. Drifting cyber-neon audio embers floating gently upwards.
 * 4. Holographic light sheen and electric laser sweep gliding across the artwork.
 * 5. Micro-breathing scale on the central soundwave ribbon.
 * 6. Snappy ~1.75s cinematic pacing with smooth fluid zoom-out dissolve exit (instant tap to skip).
 */
@Composable
fun AuraSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayIntroCue: (() -> Unit)? = null,
) {
    val overallAlpha = remember { Animatable(1f) }
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.90f) }
    val exitScale = remember { Animatable(1.0f) }
    val sheenProgress = remember { Animatable(-0.5f) }
    val laserArcProgress = remember { Animatable(0f) }

    // Seeded floating ambient particles
    val particles = remember {
        val colors = listOf(
            Color(0xFF00D2FF), // Cyan
            Color(0xFF9D4EDD), // Electric Purple
            Color(0xFFFF007F), // Neon Magenta
            Color(0xFF00F5D4), // Mint Neon
        )
        val rng = Random(42)
        List(18) {
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

    // Ambient rotation of the chromatic nebula
    val nebulaRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nebula_rotation",
    )

    // Breathing aura bloom
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_glow",
    )
    val auraScalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_scale",
    )

    // Micro-breathing scale for the central soundwave graphic
    val imageBreathPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "image_pulse",
    )

    // Upward drift progression for floating embers
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_drift",
    )

    val smoothDecel = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    LaunchedEffect(Unit) {
        // Trigger non-blocking sonic chime and tactile pulse
        onPlayIntroCue?.invoke()

        // Snappy, punchy entrance: 0ms -> 450ms
        launch {
            entranceAlpha.animateTo(1f, animationSpec = tween(450, easing = smoothDecel))
        }
        launch {
            entranceScale.animateTo(1f, animationSpec = tween(550, easing = smoothDecel))
        }

        // Electric laser arc & holographic sheen sweep across the logo
        launch {
            delay(250)
            sheenProgress.animateTo(1.6f, animationSpec = tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            laserArcProgress.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
        }

        // Fast & elegant hold (~1350ms) followed by fluid scale-dissolve exit (~360ms)
        delay(1350)

        launch {
            exitScale.animateTo(1.08f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        }
        overallAlpha.animateTo(0f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overallAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
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
                    alpha = auraGlow * entranceAlpha.value * 0.88f
                    rotationZ = nebulaRotation
                    scaleX = entranceScale.value * auraScalePulse * 1.15f
                    scaleY = entranceScale.value * auraScalePulse * 1.15f
                }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00D2FF).copy(alpha = 0.35f),
                            Color(0xFF9D4EDD).copy(alpha = 0.32f),
                            Color(0xFFFF007F).copy(alpha = 0.28f),
                            Color(0xFF00F5D4).copy(alpha = 0.22f),
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
                            Color(0xFF00D2FF).copy(alpha = 0.45f),
                            Color(0xFF9D4EDD).copy(alpha = 0.35f),
                            Color(0xFFFF007F).copy(alpha = 0.20f),
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
                    .graphicsLayer { alpha = (entranceAlpha.value * 0.38f).coerceIn(0f, 1f) },
            ) {
                val sheenX = size.width * sheenProgress.value
                val sheenWidth = size.width * 0.45f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00D2FF).copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.40f),
                            Color(0xFFFF007F).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        start = Offset(sheenX - sheenWidth, 0f),
                        end = Offset(sheenX + sheenWidth, size.height),
                    ),
                )

                // Electric laser scanline sweep during entrance
                if (laserArcProgress.value in 0.01f..0.99f) {
                    val laserY = size.height * (0.35f + laserArcProgress.value * 0.30f)
                    val laserAlpha = (1f - (laserArcProgress.value - 0.5f) * 2f).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00D2FF).copy(alpha = 0.4f * laserAlpha),
                                Color.White.copy(alpha = 0.85f * laserAlpha),
                                Color(0xFF00D2FF).copy(alpha = 0.4f * laserAlpha),
                                Color.Transparent,
                            ),
                        ),
                        topLeft = Offset(0f, laserY - 1.5f),
                        size = androidx.compose.ui.geometry.Size(size.width, 3f),
                    )
                }
            }
        }

        // Layer 5: Futuristic Minimalist Tagline at the bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .graphicsLayer {
                    alpha = entranceAlpha.value * 0.85f
                    translationY = (1f - entranceAlpha.value) * 20f
                },
        ) {
            Text(
                text = "A U R A M U S I C",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "LOSSLESS · CONNECTED · INTUITIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                ),
                color = Color(0xFF00D2FF).copy(alpha = 0.75f),
            )
        }
    }
}
