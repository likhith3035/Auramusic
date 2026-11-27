package com.music.bitchord.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mathematical definition of a cubic Bézier curve segment for vector waveform rendering.
 */
private data class WaveBezierSegment(
    val cp1X: Float, val cp1Y: Float,
    val cp2X: Float, val cp2Y: Float,
    val endX: Float, val endY: Float,
)

/**
 * Definition of an individual harmonic waveform layer.
 */
private data class WaveDefinition(
    val startX: Float,
    val startY: Float,
    val segments: List<WaveBezierSegment>,
    val strokeWidth: Float,
    val alpha: Float,
)

/**
 * High-end cinematic opening splash screen for AuraMusic.
 * 
 * Displays the authentic brand lockup centered on AMOLED pitch black:
 * 1. Razor-sharp vector soundwaves with dynamic harmonic breathing.
 * 2. Brand name "AuraMusic" directly below the soundwaves (standard kerning, no wide spacing).
 * 3. Official "BY LIKHITH" attribution with precision hairline accents.
 * 4. Zero initial blank screen: visible immediately on frame 1 with a punchy spring entrance.
 * 5. Snappy ~1.2s duration with smooth exit dissolve (instant tap-to-skip).
 */
@Composable
fun AuraSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayIntroCue: (() -> Unit)? = null,
    reduceAnimation: Boolean = false,
    fastTrack: Boolean = false,
    playAudioCue: Boolean = true,
) {
    // Visible immediately on frame 1 — zero blank black screen
    val overallAlpha = remember { Animatable(1f) }
    val entranceScale = remember { Animatable(if (reduceAnimation || fastTrack) 1f else 0.92f) }
    val exitScale = remember { Animatable(1.0f) }

    // Subtle living micro-phase breathing so the waveform is always active
    val infiniteTransition = rememberInfiniteTransition(label = "aura_waveform_living")
    val breathingAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave_idle_breathing",
    )
    val idleBreathing = if (reduceAnimation || fastTrack) 0f else breathingAnim

    // Pre-allocated cubic Bézier curves corresponding to the authentic Aura brand geometry
    // Viewport coordinate space: 730 x 484, baseline Y = 242f
    val wave1 = remember {
        WaveDefinition(
            startX = 60f,
            startY = 242f,
            segments = listOf(
                WaveBezierSegment(145f, 230f, 205f, 75f, 275f, 75f),
                WaveBezierSegment(350f, 75f, 375f, 215f, 420f, 315f),
                WaveBezierSegment(455f, 395f, 495f, 410f, 535f, 410f),
                WaveBezierSegment(580f, 410f, 610f, 195f, 645f, 195f),
                WaveBezierSegment(665f, 195f, 678f, 225f, 690f, 242f),
            ),
            strokeWidth = 24f,
            alpha = 1.0f,
        )
    }

    val wave2 = remember {
        WaveDefinition(
            startX = 95f,
            startY = 242f,
            segments = listOf(
                WaveBezierSegment(160f, 225f, 210f, 120f, 275f, 120f),
                WaveBezierSegment(340f, 120f, 370f, 225f, 415f, 295f),
                WaveBezierSegment(445f, 345f, 485f, 360f, 525f, 360f),
                WaveBezierSegment(565f, 360f, 595f, 225f, 635f, 225f),
                WaveBezierSegment(650f, 225f, 660f, 235f, 670f, 242f),
            ),
            strokeWidth = 16f,
            alpha = 0.82f,
        )
    }

    val wave3 = remember {
        WaveDefinition(
            startX = 130f,
            startY = 242f,
            segments = listOf(
                WaveBezierSegment(175f, 220f, 220f, 160f, 275f, 160f),
                WaveBezierSegment(330f, 160f, 360f, 230f, 405f, 280f),
                WaveBezierSegment(435f, 315f, 475f, 325f, 515f, 325f),
                WaveBezierSegment(550f, 325f, 580f, 250f, 615f, 250f),
                WaveBezierSegment(625f, 250f, 635f, 242f, 645f, 242f),
            ),
            strokeWidth = 11f,
            alpha = 0.55f,
        )
    }

    // Pre-allocated gradient color palettes for zero-allocation rendering
    val wave1Colors = remember {
        listOf(
            Color(0xFF00E5FF), // Electric Cyan
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFF818CF8), // Indigo Lavender
            Color(0xFFC084FC), // Orchid Purple
        )
    }

    val wave2Colors = remember {
        listOf(
            Color(0xFF38BDF8),
            Color(0xFFA855F7),
        )
    }

    val wave3Colors = remember {
        listOf(
            Color(0xFF67E8F9),
            Color(0xFFE879F9),
        )
    }

    val ambientGlowColors = remember {
        listOf(
            Color(0x3500E5FF),
            Color(0x227928CA),
            Color.Transparent,
        )
    }

    val leftAccentGradient = remember {
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = 0.55f))
        )
    }

    val rightAccentGradient = remember {
        Brush.horizontalGradient(
            listOf(Color(0xFF00E5FF).copy(alpha = 0.55f), Color.Transparent)
        )
    }

    // Reusable, zero-allocation Path objects
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val splashBrushCache = remember {
        object {
            var width = -1f
            var height = -1f
            var ambientGlow: Brush? = null
            var wave3Brush: Brush? = null
            var wave2Brush: Brush? = null
            var wave1Brush: Brush? = null
            var stroke3: Stroke? = null
            var stroke2: Stroke? = null
            var stroke1: Stroke? = null
        }
    }

    val punchySpring = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    LaunchedEffect(fastTrack, reduceAnimation) {
        if (fastTrack) {
            overallAlpha.animateTo(0f, animationSpec = tween(140, easing = FastOutSlowInEasing))
            onFinished()
            return@LaunchedEffect
        }

        // 1. Play crystalline audio chime & tactile haptic tick when allowed
        if (playAudioCue) {
            onPlayIntroCue?.invoke()
        }

        if (reduceAnimation) {
            delay(280)
            overallAlpha.animateTo(0f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            onFinished()
            return@LaunchedEffect
        }

        // 2. Punchy spring entrance: 0ms -> 380ms
        launch {
            entranceScale.animateTo(1.0f, animationSpec = tween(380, easing = punchySpring))
        }

        // 3. Crisp cinematic hold showcasing the living brand lockup (~700ms)
        delay(880)

        // 4. Seamless velvet dissolve into main UI: 880ms -> 1180ms (300ms)
        launch {
            exitScale.animateTo(1.04f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        overallAlpha.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
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
        // Centered Cohesive Brand Lockup
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
            },
        ) {
            // 1. Vector Soundwave Living Waveform
            Canvas(
                modifier = Modifier.size(width = 280.dp, height = 160.dp),
            ) {
                val canvasW = size.width
                val canvasH = size.height
                if (canvasW <= 0f || canvasH <= 0f) return@Canvas

                val scale = minOf(canvasW / 730f, canvasH / 484f) * 0.96f
                val offsetX = (canvasW - 730f * scale) / 2f
                val offsetY = (canvasH - 484f * scale) / 2f
                val baseY = 242f

                // Living wave phase breathing
                val liveProgress = 1f + 0.025f * idleBreathing

                fun mapX(x: Float): Float = offsetX + x * scale
                fun mapY(y: Float): Float = offsetY + (baseY + (y - baseY) * liveProgress) * scale

                if (splashBrushCache.width != canvasW || splashBrushCache.height != canvasH) {
                    splashBrushCache.width = canvasW
                    splashBrushCache.height = canvasH
                    splashBrushCache.ambientGlow = Brush.radialGradient(
                        colors = ambientGlowColors,
                        center = Offset(canvasW / 2f, canvasH / 2f),
                        radius = canvasW * 0.48f,
                    )
                    splashBrushCache.wave3Brush = Brush.linearGradient(
                        colors = wave3Colors,
                        start = Offset(mapX(130f), offsetY + baseY * scale),
                        end = Offset(mapX(645f), offsetY + baseY * scale),
                    )
                    splashBrushCache.wave2Brush = Brush.linearGradient(
                        colors = wave2Colors,
                        start = Offset(mapX(95f), offsetY + baseY * scale),
                        end = Offset(mapX(670f), offsetY + baseY * scale),
                    )
                    splashBrushCache.wave1Brush = Brush.linearGradient(
                        colors = wave1Colors,
                        start = Offset(mapX(60f), offsetY + baseY * scale),
                        end = Offset(mapX(705f), offsetY + baseY * scale),
                    )
                    splashBrushCache.stroke3 = Stroke(
                        width = wave3.strokeWidth * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    splashBrushCache.stroke2 = Stroke(
                        width = wave2.strokeWidth * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    splashBrushCache.stroke1 = Stroke(
                        width = wave1.strokeWidth * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                }

                // Ambient soft luminous glow behind the waveform
                splashBrushCache.ambientGlow?.let { drawCircle(brush = it) }

                // Helper to populate reusable Path without object allocation
                fun populateWave(path: Path, wave: WaveDefinition) {
                    path.reset()
                    path.moveTo(mapX(wave.startX), mapY(wave.startY))
                    for (seg in wave.segments) {
                        path.cubicTo(
                            mapX(seg.cp1X), mapY(seg.cp1Y),
                            mapX(seg.cp2X), mapY(seg.cp2Y),
                            mapX(seg.endX), mapY(seg.endY),
                        )
                    }
                }

                // Wave 3 (Ambient Harmonic Layer)
                populateWave(path3, wave3)
                drawPath(
                    path = path3,
                    brush = splashBrushCache.wave3Brush!!,
                    alpha = wave3.alpha,
                    style = splashBrushCache.stroke3!!,
                )

                // Wave 2 (Secondary Resonance Layer)
                populateWave(path2, wave2)
                drawPath(
                    path = path2,
                    brush = splashBrushCache.wave2Brush!!,
                    alpha = wave2.alpha,
                    style = splashBrushCache.stroke2!!,
                )

                // Wave 1 (Primary Central Waveform)
                populateWave(path1, wave1)
                drawPath(
                    path = path1,
                    brush = splashBrushCache.wave1Brush!!,
                    alpha = wave1.alpha,
                    style = splashBrushCache.stroke1!!,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 2. AuraMusic Title (Normal kerning, bold, positioned directly below logo)
            Text(
                text = "AuraMusic",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            // 3. Official Attribution: — BY LIKHITH —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(1.dp)
                        .background(leftAccentGradient),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "BY LIKHITH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        fontSize = 11.sp,
                    ),
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(1.dp)
                        .background(rightAccentGradient),
                )
            }
        }
    }
}
