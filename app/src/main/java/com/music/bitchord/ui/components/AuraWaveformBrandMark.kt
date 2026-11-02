package com.music.bitchord.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
private data class WaveSegment(
    val cp1X: Float,
    val cp1Y: Float,
    val cp2X: Float,
    val cp2Y: Float,
    val endX: Float,
    val endY: Float,
)

@Immutable
private data class WaveData(
    val startX: Float,
    val startY: Float,
    val segments: List<WaveSegment>,
    val strokeWidth: Float,
    val alpha: Float,
)

/**
 * Authentic AuraMusic vector waveform brand mark.
 * Renders the signature 3-layer cubic Bézier soundwave with living harmonic breathing
 * and subtle cyan-purple luminous aura.
 */
@Composable
fun AuraWaveformBrandMark(
    modifier: Modifier = Modifier,
    width: Dp = 80.dp,
    height: Dp = 50.dp,
    animated: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_brandmark_living")
    val idleBreathing by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "aura_brandmark_breathing",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val wave1 = remember {
        WaveData(
            startX = 60f,
            startY = 242f,
            segments = listOf(
                WaveSegment(145f, 230f, 205f, 75f, 275f, 75f),
                WaveSegment(350f, 75f, 375f, 215f, 420f, 315f),
                WaveSegment(455f, 395f, 495f, 410f, 535f, 410f),
                WaveSegment(580f, 410f, 610f, 195f, 645f, 195f),
                WaveSegment(665f, 195f, 678f, 225f, 690f, 242f),
            ),
            strokeWidth = 24f,
            alpha = 1.0f,
        )
    }

    val wave2 = remember {
        WaveData(
            startX = 95f,
            startY = 242f,
            segments = listOf(
                WaveSegment(160f, 225f, 210f, 120f, 275f, 120f),
                WaveSegment(340f, 120f, 370f, 225f, 415f, 295f),
                WaveSegment(445f, 345f, 485f, 360f, 525f, 360f),
                WaveSegment(565f, 360f, 595f, 225f, 635f, 225f),
                WaveSegment(650f, 225f, 660f, 235f, 670f, 242f),
            ),
            strokeWidth = 16f,
            alpha = 0.85f,
        )
    }

    val wave3 = remember {
        WaveData(
            startX = 130f,
            startY = 242f,
            segments = listOf(
                WaveSegment(175f, 220f, 220f, 160f, 275f, 160f),
                WaveSegment(330f, 160f, 360f, 230f, 405f, 280f),
                WaveSegment(435f, 315f, 475f, 325f, 515f, 325f),
                WaveSegment(550f, 325f, 580f, 250f, 615f, 250f),
                WaveSegment(625f, 250f, 635f, 242f, 645f, 242f),
            ),
            strokeWidth = 11f,
            alpha = 0.60f,
        )
    }

    val wave1Colors = remember {
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF38BDF8),
            Color(0xFF818CF8),
            Color(0xFFC084FC),
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
            Color(0x3800E5FF),
            Color(0x207928CA),
            Color.Transparent,
        )
    }

    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }

    Canvas(
        modifier = modifier.size(width = width, height = height),
    ) {
        val canvasW = size.width
        val canvasH = size.height
        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        val scale = minOf(canvasW / 730f, canvasH / 484f) * 0.98f
        val offsetX = (canvasW - 730f * scale) / 2f
        val offsetY = (canvasH - 484f * scale) / 2f
        val baseY = 242f
        val liveProgress = 1f + 0.035f * idleBreathing

        fun mapX(x: Float): Float = offsetX + x * scale
        fun mapY(y: Float): Float = offsetY + (baseY + (y - baseY) * liveProgress) * scale

        // Luminous radial glow behind mark
        drawCircle(
            brush = Brush.radialGradient(
                colors = ambientGlowColors,
                center = Offset(canvasW / 2f, canvasH / 2f),
                radius = canvasW * 0.52f,
            ),
        )

        fun populate(path: Path, wave: WaveData) {
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

        // Layer 3 (Outer ambient wave)
        populate(path3, wave3)
        drawPath(
            path = path3,
            brush = Brush.linearGradient(
                colors = wave3Colors,
                start = Offset(mapX(130f), offsetY + baseY * scale),
                end = Offset(mapX(645f), offsetY + baseY * scale),
            ),
            alpha = wave3.alpha,
            style = Stroke(
                width = wave3.strokeWidth * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Layer 2 (Middle resonance wave)
        populate(path2, wave2)
        drawPath(
            path = path2,
            brush = Brush.linearGradient(
                colors = wave2Colors,
                start = Offset(mapX(95f), offsetY + baseY * scale),
                end = Offset(mapX(670f), offsetY + baseY * scale),
            ),
            alpha = wave2.alpha,
            style = Stroke(
                width = wave2.strokeWidth * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Layer 1 (Core sharp wave)
        populate(path1, wave1)
        drawPath(
            path = path1,
            brush = Brush.linearGradient(
                colors = wave1Colors,
                start = Offset(mapX(60f), offsetY + baseY * scale),
                end = Offset(mapX(690f), offsetY + baseY * scale),
            ),
            alpha = wave1.alpha,
            style = Stroke(
                width = wave1.strokeWidth * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

