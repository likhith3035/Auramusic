package com.music.bitchord.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Immutable
private data class OnboardingMeshPalette(
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val orb4: Color,
)

private val PALETTES = listOf(
    // 0: Velvet Sunset (Red / Coral / Violet / Deep Midnight)
    OnboardingMeshPalette(
        orb1 = Color(0xFFFF2A55),
        orb2 = Color(0xFFFF6F3C),
        orb3 = Color(0xFF8B1FA9),
        orb4 = Color(0xFF24063A),
    ),
    // 1: Cosmic Aura (Cyan / Sky / Orchid / Dark Indigo)
    OnboardingMeshPalette(
        orb1 = Color(0xFF00E5FF),
        orb2 = Color(0xFF38BDF8),
        orb3 = Color(0xFF9333EA),
        orb4 = Color(0xFF0F172A),
    ),
    // 2: Cyber Magenta (Raspberry / Amber / Rose / Midnight Violet)
    OnboardingMeshPalette(
        orb1 = Color(0xFFFF007F),
        orb2 = Color(0xFFFF9A3C),
        orb3 = Color(0xFF7928CA),
        orb4 = Color(0xFF16032B),
    ),
    // 3: Astral Aurora (Turquoise / Electric Violet / Warm Rose / Deep Plum)
    OnboardingMeshPalette(
        orb1 = Color(0xFF00F5D4),
        orb2 = Color(0xFF8B5CF6),
        orb3 = Color(0xFFF43F5E),
        orb4 = Color(0xFF1E1035),
    ),
)

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
    val r = lerp(c1.red, c2.red, t)
    val g = lerp(c1.green, c2.green, t)
    val b = lerp(c1.blue, c2.blue, t)
    val a = lerp(c1.alpha, c2.alpha, t)
    return Color(r, g, b, a)
}

/**
 * Continuous smoother-step for fluid C1-continuous interpolations.
 */
private fun smoothStep(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

/**
 * Butter-smooth living ambient mesh background.
 * Uses continuous trigonometric harmonic orbital drift and continuous chromatic phase cycling
 * to eliminate all discrete timer ticks, jumps, and micro-stutters.
 */
@Composable
fun AuraLivingMeshBackground(
    modifier: Modifier = Modifier,
    blurRadius: Float = 72f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_onboarding_living_mesh")

    // Continuous orbital phase: 20s cycle
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit_phase",
    )

    // Continuous chromatic cycle across the 4 palettes: 24s full loop
    val colorCyclePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "color_cycle_phase",
    )

    val currentPaletteIndex = colorCyclePhase.toInt() % PALETTES.size
    val nextPaletteIndex = (currentPaletteIndex + 1) % PALETTES.size
    val rawFraction = colorCyclePhase - colorCyclePhase.toInt()
    val blendFraction = smoothStep(rawFraction)

    val pA = PALETTES[currentPaletteIndex]
    val pB = PALETTES[nextPaletteIndex]

    val activeColor1 = remember(currentPaletteIndex, nextPaletteIndex, blendFraction) {
        lerpColor(pA.orb1, pB.orb1, blendFraction)
    }
    val activeColor2 = remember(currentPaletteIndex, nextPaletteIndex, blendFraction) {
        lerpColor(pA.orb2, pB.orb2, blendFraction)
    }
    val activeColor3 = remember(currentPaletteIndex, nextPaletteIndex, blendFraction) {
        lerpColor(pA.orb3, pB.orb3, blendFraction)
    }
    val activeColor4 = remember(currentPaletteIndex, nextPaletteIndex, blendFraction) {
        lerpColor(pA.orb4, pB.orb4, blendFraction)
    }

    val baseBackgroundColor = Color(0xFF0A0414)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.25f
                }
                .blur(blurRadius.dp),
        ) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val t = orbitPhase

            // Orbital node 1: Top-Right (warm primary aura)
            val c1X = (0.82f + 0.12f * cos(t * 1.0f)) * width
            val c1Y = (0.18f + 0.11f * sin(t * 0.9f)) * height
            val r1 = size.maxDimension * 0.65f

            // Orbital node 2: Top-Left (accent glow)
            val c2X = (0.18f + 0.13f * cos(t * -0.8f + 1.2f)) * width
            val c2Y = (0.28f + 0.10f * sin(t * 1.1f + 0.8f)) * height
            val r2 = size.maxDimension * 0.58f

            // Orbital node 3: Center-Right (mid-range radiance)
            val c3X = (0.75f + 0.14f * cos(t * 0.7f + 2.5f)) * width
            val c3Y = (0.68f + 0.12f * sin(t * -1.0f + 1.8f)) * height
            val r3 = size.maxDimension * 0.62f

            // Orbital node 4: Bottom-Left (deep foundation)
            val c4X = (0.22f + 0.11f * cos(t * -1.2f + 3.1f)) * width
            val c4Y = (0.82f + 0.09f * sin(t * 0.85f + 2.1f)) * height
            val r4 = size.maxDimension * 0.70f

            fun drawOrb(center: Offset, color: Color, radius: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to color.copy(alpha = 0.82f),
                        0.35f to color.copy(alpha = 0.55f),
                        0.70f to color.copy(alpha = 0.22f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    center = center,
                    radius = radius,
                )
            }

            drawOrb(Offset(c4X, c4Y), activeColor4, r4)
            drawOrb(Offset(c3X, c3Y), activeColor3, r3)
            drawOrb(Offset(c2X, c2Y), activeColor2, r2)
            drawOrb(Offset(c1X, c1Y), activeColor1, r1)

            // Contrast enhancement scrim for razor-sharp typography readability
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.08f),
                    0.45f to Color.Black.copy(alpha = 0.18f),
                    1.0f to Color.Black.copy(alpha = 0.38f),
                ),
            )
        }
    }
}

