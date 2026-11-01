package com.music.bitchord.ui.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom luxury animated Aura AI emblem.
 * Adapts dynamically to BitChord's MaterialTheme (Dark, Light, and Nothing OS modes).
 */
@Composable
fun AuraOrbIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    isThinking: Boolean = false,
    isPlaying: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
) {
    val transition = rememberInfiniteTransition(label = "AuraOrbAnim")

    val pulseScale by transition.animateFloat(
        initialValue = if (isThinking) 0.85f else 0.95f,
        targetValue = if (isThinking) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 650 else 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val rotationDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2800 else if (isPlaying) 5500 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotationDegrees",
    )

    val discColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val innerHoleColor = MaterialTheme.colorScheme.surface
    val coreRestingColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f) * pulseScale

            // 1. Solid matte disc base with boundary
            drawCircle(
                color = discColor,
                radius = radius,
                center = center,
            )
            drawCircle(
                color = borderColor,
                radius = radius,
                center = center,
                style = Stroke(width = 0.75.dp.toPx()),
            )

            // 2. Precision Outer Caliper Track
            val stroke1 = 1.2.dp.toPx()
            drawCircle(
                color = trackColor,
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = stroke1),
            )

            // 3. Rotating Precision Aperture Arc
            val sweepAngle = if (isThinking) 160f else 90f
            drawArc(
                color = activeColor,
                startAngle = rotationDegrees,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
                size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                style = Stroke(width = stroke1, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            // Counter-rotating auxiliary indicator arc
            drawArc(
                color = secondaryColor,
                startAngle = -rotationDegrees * 1.5f + 180f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.52f, center.y - radius * 0.52f),
                size = androidx.compose.ui.geometry.Size(radius * 1.04f, radius * 1.04f),
                style = Stroke(width = stroke1 * 0.9f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            // 4. Center Sonic Precision Core
            val coreRadius = radius * (if (isThinking) 0.26f else 0.22f)
            drawCircle(
                color = if (isThinking) activeColor else coreRestingColor,
                radius = coreRadius,
                center = center,
            )
            drawCircle(
                color = innerHoleColor,
                radius = coreRadius * 0.55f,
                center = center,
            )
        }
    }
}
