package com.music.bitchord.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.weather.WeatherVisualType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Raindrop(
    val relX: Float,
    val relYOffset: Float,
    val speed: Float,
    val lengthPx: Float,
    val alpha: Float,
    val strokeWidth: Float,
)

private data class Star(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val phase: Float,
    val isDiamond: Boolean,
)

private data class Snowflake(
    val relX: Float,
    val relYOffset: Float,
    val speed: Float,
    val radius: Float,
    val swayFreq: Float,
    val swayAmp: Float,
    val phase: Float,
    val alpha: Float,
)

private data class CloudCluster(
    val relX: Float,
    val relY: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float,
)

/**
 * Living, hardware-accelerated ambient weather canvas.
 * Renders real-time atmospheric visual effects (rain streaks with ripple splashes,
 * glowing solar corona, crescent moon with twinkling starfield, drifting volumetric clouds,
 * thunderstorm sheet lightning, and swaying snow) matching real-world outdoor weather.
 * Zero unicode emojis, GPU-accelerated Compose Canvas rendering.
 */
@Composable
fun LivingWeatherCanvas(
    visualType: WeatherVisualType,
    modifier: Modifier = Modifier,
    blendColor: Color = Color(0xFF10121C),
    reduceAnimation: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")

    // Master continuous phase clock (0f to 1f over 2400ms)
    val animRainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain_phase",
    )

    // Slower continuous clock for celestial & cloud drift (0f to 1f over 12000ms)
    val animSlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slow_phase",
    )

    // Breathing pulse for solar corona & moon bloom (0f to 1f over 3600ms)
    val animPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_phase",
    )

    // Thunderstorm lightning cycle clock (0f to 1f over 8500ms)
    val animLightningPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lightning_phase",
    )

    val rainPhase = if (reduceAnimation) 0.5f else animRainPhase
    val slowPhase = if (reduceAnimation) 0.35f else animSlowPhase
    val pulsePhase = if (reduceAnimation) 0.5f else animPulsePhase
    val lightningPhase = if (reduceAnimation) 0f else animLightningPhase

    // Pre-calculated particle systems
    val raindrops = remember {
        val rng = Random(42)
        List(34) {
            Raindrop(
                relX = rng.nextFloat(),
                relYOffset = rng.nextFloat(),
                speed = 0.75f + rng.nextFloat() * 0.75f,
                lengthPx = 14f + rng.nextFloat() * 16f,
                alpha = 0.35f + rng.nextFloat() * 0.50f,
                strokeWidth = 1.2f + rng.nextFloat() * 1.2f,
            )
        }
    }

    val stars = remember {
        val rng = Random(101)
        List(28) { index ->
            Star(
                relX = 0.05f + rng.nextFloat() * 0.90f,
                relY = 0.08f + rng.nextFloat() * 0.84f,
                radius = 1.0f + rng.nextFloat() * 1.8f,
                phase = rng.nextFloat() * 2f * PI.toFloat(),
                isDiamond = index % 5 == 0,
            )
        }
    }

    val snowflakes = remember {
        val rng = Random(202)
        List(28) {
            Snowflake(
                relX = rng.nextFloat(),
                relYOffset = rng.nextFloat(),
                speed = 0.35f + rng.nextFloat() * 0.45f,
                radius = 1.5f + rng.nextFloat() * 2.5f,
                swayFreq = 1.5f + rng.nextFloat() * 2.5f,
                swayAmp = 6f + rng.nextFloat() * 8f,
                phase = rng.nextFloat() * 2f * PI.toFloat(),
                alpha = 0.40f + rng.nextFloat() * 0.55f,
            )
        }
    }

    val clouds = remember {
        listOf(
            CloudCluster(relX = 0.25f, relY = 0.38f, scale = 1.05f, speed = 0.32f, alpha = 0.20f),
            CloudCluster(relX = 0.60f, relY = 0.32f, scale = 0.95f, speed = 0.48f, alpha = 0.28f),
            CloudCluster(relX = 0.82f, relY = 0.48f, scale = 1.25f, speed = 0.36f, alpha = 0.26f),
            CloudCluster(relX = 0.45f, relY = 0.55f, scale = 0.85f, speed = 0.40f, alpha = 0.16f),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        when (visualType) {
            WeatherVisualType.RAIN -> {
                drawRainScene(raindrops, rainPhase, width, height)
            }
            WeatherVisualType.CLEAR_DAY -> {
                drawSunnyScene(pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLEAR_NIGHT -> {
                drawClearNightScene(stars, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLOUDY -> {
                drawCloudyScene(clouds, slowPhase, width, height)
            }
            WeatherVisualType.THUNDERSTORM -> {
                drawThunderstormScene(raindrops, rainPhase, lightningPhase, width, height)
            }
            WeatherVisualType.SNOW -> {
                drawSnowScene(snowflakes, rainPhase, width, height)
            }
        }

        // Text-protection scrim on the left: ensures greeting, username, mood chip and buttons remain crisp and readable
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    blendColor.copy(alpha = 0.92f),
                    blendColor.copy(alpha = 0.78f),
                    blendColor.copy(alpha = 0.35f),
                    Color.Transparent,
                ),
                startX = 0f,
                endX = (width * 0.65f).coerceAtLeast(180.dp.toPx()),
            ),
            size = Size(width, height),
        )
    }
}

private fun DrawScope.drawRainScene(
    raindrops: List<Raindrop>,
    phase: Float,
    width: Float,
    height: Float,
) {
    val focalX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    // Subtle cool overcast ambient glow in upper-right area
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.14f),
                Color(0xFF0284C7).copy(alpha = 0.05f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.30f),
            radius = (width * 0.55f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    // Draw falling slanted raindrops across the full width
    for (drop in raindrops) {
        val progress = (phase * drop.speed + drop.relYOffset) % 1f
        val startX = drop.relX * (width + 40.dp.toPx()) - progress * (width * 0.12f)
        val startY = progress * (height + drop.lengthPx * 2f) - drop.lengthPx

        val endX = startX - (drop.lengthPx * 0.32f)
        val endY = startY + drop.lengthPx

        // Only draw if on canvas
        if (startY < height && endY > 0f) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7DD3FC).copy(alpha = drop.alpha * 0.25f),
                        Color(0xFFE0F2FE).copy(alpha = drop.alpha),
                    ),
                    startY = startY,
                    endY = endY,
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drop.strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        // Splash ripple on ground when drop reaches bottom 12%
        if (progress > 0.88f) {
            val splashProgress = (progress - 0.88f) / 0.12f
            val rippleRadius = splashProgress * 9f
            val rippleAlpha = (1f - splashProgress) * 0.40f * drop.alpha

            drawOval(
                color = Color(0xFFBAE6FD).copy(alpha = rippleAlpha),
                topLeft = Offset(endX - rippleRadius, height - 4f - (rippleRadius * 0.25f)),
                size = Size(rippleRadius * 2f, rippleRadius * 0.55f),
                style = Stroke(width = 1f),
            )
        }
    }
}

private fun DrawScope.drawSunnyScene(
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    val sunCenterY = height * 0.44f
    val sunRadius = (height * 0.22f).coerceIn(24.dp.toPx(), 38.dp.toPx())

    val pulseScale = 0.94f + 0.12f * pulsePhase

    // Outer coronal bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB703).copy(alpha = 0.28f * pulseScale),
                Color(0xFFFB8500).copy(alpha = 0.12f * pulseScale),
                Color(0xFFFF5400).copy(alpha = 0.04f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 2.8f * pulseScale,
        ),
    )

    // Solar ray bursts
    val rayCount = 10
    val angleOffset = slowPhase * 360f
    for (i in 0 until rayCount) {
        val baseAngle = (i * (360f / rayCount) + angleOffset) * (PI / 180.0)
        val rayInner = sunRadius * 1.15f
        val rayOuter = sunRadius * (1.55f + 0.25f * sin((pulsePhase * 2f * PI + i).toFloat()))

        val startX = (sunCenterX + cos(baseAngle) * rayInner).toFloat()
        val startY = (sunCenterY + sin(baseAngle) * rayInner).toFloat()
        val endX = (sunCenterX + cos(baseAngle) * rayOuter).toFloat()
        val endY = (sunCenterY + sin(baseAngle) * rayOuter).toFloat()

        drawLine(
            color = Color(0xFFFFE082).copy(alpha = 0.32f + 0.15f * pulsePhase),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round,
        )
    }

    // Secondary warm halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE082).copy(alpha = 0.50f),
                Color(0xFFFFB703).copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 1.45f,
        ),
    )

    // Solid brilliant solar core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFDF5),
                Color(0xFFFFEE58),
                Color(0xFFFFA000),
            ),
            center = Offset(sunCenterX - sunRadius * 0.2f, sunCenterY - sunRadius * 0.2f),
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = Offset(sunCenterX, sunCenterY),
    )
}

private fun DrawScope.drawClearNightScene(
    stars: List<Star>,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val moonCenterX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    val moonCenterY = height * 0.44f
    val moonRadius = (height * 0.24f).coerceIn(25.dp.toPx(), 40.dp.toPx())

    // Ambient celestial night glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF6366F1).copy(alpha = 0.15f),
                Color(0xFF312E81).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = (width * 0.50f).coerceAtLeast(moonRadius * 2.5f),
        ),
    )

    // Twinkling stars across the sky
    for (star in stars) {
        val starX = star.relX * width
        val starY = star.relY * height
        val twinkle = 0.25f + 0.70f * (0.5f + 0.5f * sin((slowPhase * 4f * PI + star.phase).toFloat()))
        val starAlpha = twinkle.coerceIn(0f, 1f)

        if (star.isDiamond) {
            // Delicate 4-point cross star
            val armLength = star.radius * 2.8f
            drawLine(
                color = Color(0xFFE0E7FF).copy(alpha = starAlpha),
                start = Offset(starX - armLength, starY),
                end = Offset(starX + armLength, starY),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFE0E7FF).copy(alpha = starAlpha),
                start = Offset(starX, starY - armLength),
                end = Offset(starX, starY + armLength),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = star.radius * 0.9f,
                center = Offset(starX, starY),
            )
        } else {
            drawCircle(
                color = Color(0xFFEDE9FE).copy(alpha = starAlpha),
                radius = star.radius,
                center = Offset(starX, starY),
            )
        }
    }

    // Lunar corona halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFC7D2FE).copy(alpha = 0.22f + 0.08f * pulsePhase),
                Color(0xFF818CF8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 2.2f,
        ),
    )

    // Stylized Crescent Moon Path
    val crescentPath = Path().apply {
        val topTipX = moonCenterX - moonRadius * 0.10f
        val topTipY = moonCenterY - moonRadius
        val bottomTipX = moonCenterX + moonRadius * 0.15f
        val bottomTipY = moonCenterY + moonRadius * 0.90f

        moveTo(topTipX, topTipY)
        // Outer curved silhouette (sweeps outward to the left)
        cubicTo(
            moonCenterX - moonRadius * 1.35f, moonCenterY - moonRadius * 0.50f,
            moonCenterX - moonRadius * 1.35f, moonCenterY + moonRadius * 0.50f,
            bottomTipX, bottomTipY,
        )
        // Inner crescent cutout scoop (curves back up to top tip)
        cubicTo(
            moonCenterX - moonRadius * 0.35f, moonCenterY + moonRadius * 0.45f,
            moonCenterX - moonRadius * 0.35f, moonCenterY - moonRadius * 0.45f,
            topTipX, topTipY,
        )
        close()
    }

    drawPath(
        path = crescentPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF1F5F9),
                Color(0xFFCBD5E1),
            ),
            start = Offset(moonCenterX - moonRadius, moonCenterY - moonRadius),
            end = Offset(moonCenterX + moonRadius, moonCenterY + moonRadius),
        ),
    )

    // Subtle lunar crater shadows
    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.28f),
        radius = moonRadius * 0.14f,
        center = Offset(moonCenterX - moonRadius * 0.60f, moonCenterY - moonRadius * 0.15f),
    )
    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.24f),
        radius = moonRadius * 0.10f,
        center = Offset(moonCenterX - moonRadius * 0.45f, moonCenterY + moonRadius * 0.25f),
    )
}

private fun DrawScope.drawCloudyScene(
    clouds: List<CloudCluster>,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val focalX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    // Soft overcast daylight gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF94A3B8).copy(alpha = 0.18f),
                Color(0xFF475569).copy(alpha = 0.05f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.38f),
            radius = (width * 0.55f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    // Volumetric cloud puffs drifting slowly across the sky
    for (cloud in clouds) {
        val driftProgress = (slowPhase * cloud.speed) % 1f
        val driftOffset = (driftProgress - 0.5f) * (width * 0.35f)
        val centerX = (cloud.relX * width + driftOffset).coerceIn(0f, width * 1.1f)
        val centerY = cloud.relY * height
        val baseR = height * 0.18f * cloud.scale

        // Draw multiple overlapping cloud lobes
        val lobePositions = listOf(
            Offset(centerX - baseR * 0.75f, centerY + baseR * 0.15f) to (baseR * 0.70f),
            Offset(centerX - baseR * 0.25f, centerY - baseR * 0.35f) to (baseR * 0.95f),
            Offset(centerX + baseR * 0.40f, centerY - baseR * 0.15f) to (baseR * 0.80f),
            Offset(centerX + baseR * 0.85f, centerY + baseR * 0.20f) to (baseR * 0.60f),
        )

        for ((pos, r) in lobePositions) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0).copy(alpha = cloud.alpha),
                        Color(0xFF94A3B8).copy(alpha = cloud.alpha * 0.65f),
                        Color.Transparent,
                    ),
                    center = Offset(pos.x, pos.y - r * 0.2f),
                    radius = r * 1.3f,
                ),
                radius = r,
                center = pos,
            )
        }

        // Soft cloud base shelf
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF64748B).copy(alpha = cloud.alpha * 0.55f),
                    Color(0xFF475569).copy(alpha = cloud.alpha * 0.25f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(centerX - baseR * 1.1f, centerY + baseR * 0.05f),
            size = Size(baseR * 2.2f, baseR * 0.55f),
        )
    }
}

private fun DrawScope.drawThunderstormScene(
    raindrops: List<Raindrop>,
    rainPhase: Float,
    lightningPhase: Float,
    width: Float,
    height: Float,
) {
    // Lightning flash intensity calculation
    val flashIntensity = when {
        lightningPhase in 0.12f..0.15f -> {
            val p = (lightningPhase - 0.12f) / 0.03f
            sin(p * PI.toFloat()) * 0.85f
        }
        lightningPhase in 0.16f..0.21f -> {
            val p = (lightningPhase - 0.16f) / 0.05f
            sin(p * PI.toFloat()) * 1.0f
        }
        else -> 0f
    }

    val boltStartX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)

    // Ambient thunderstorm background
    val ambientColor = if (flashIntensity > 0.05f) {
        Color(0xFF818CF8).copy(alpha = 0.18f + 0.35f * flashIntensity)
    } else {
        Color(0xFF1E1B4B).copy(alpha = 0.25f)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                ambientColor,
                Color.Transparent,
            ),
            center = Offset(boltStartX, height * 0.35f),
            radius = (width * 0.60f).coerceAtLeast(150.dp.toPx()),
        ),
    )

    // Lightning bolt during peak flash
    if (flashIntensity > 0.35f) {
        val boltPath = Path().apply {
            val startX = boltStartX
            val startY = height * 0.08f
            moveTo(startX, startY)
            lineTo(startX - 10f, startY + height * 0.22f)
            lineTo(startX + 6f, startY + height * 0.28f)
            lineTo(startX - 14f, startY + height * 0.55f)
            lineTo(startX - 4f, startY + height * 0.60f)
            lineTo(startX - 18f, startY + height * 0.85f)
        }

        // Bolt outer glow
        drawPath(
            path = boltPath,
            color = Color(0xFFA5B4FC).copy(alpha = flashIntensity * 0.70f),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round),
        )

        // Bolt white core
        drawPath(
            path = boltPath,
            color = Color.White.copy(alpha = flashIntensity),
            style = Stroke(width = 1.8f, cap = StrokeCap.Round),
        )
    }

    // Falling rain streaks (faster and heavier) across full width
    for (drop in raindrops) {
        val progress = (rainPhase * drop.speed * 1.35f + drop.relYOffset) % 1f
        val startX = drop.relX * (width + 40.dp.toPx()) - progress * (width * 0.14f)
        val startY = progress * (height + drop.lengthPx * 2f) - drop.lengthPx

        val endX = startX - (drop.lengthPx * 0.38f)
        val endY = startY + drop.lengthPx * 1.15f

        if (startY < height && endY > 0f) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFC7D2FE).copy(alpha = drop.alpha * 0.30f),
                        Color(0xFFEEF2FF).copy(alpha = drop.alpha * 0.90f),
                    ),
                    startY = startY,
                    endY = endY,
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drop.strokeWidth * 1.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawSnowScene(
    snowflakes: List<Snowflake>,
    phase: Float,
    width: Float,
    height: Float,
) {
    val focalX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    // Ambient winter haze
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE0F2FE).copy(alpha = 0.12f),
                Color(0xFFBAE6FD).copy(alpha = 0.04f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.40f),
            radius = (width * 0.55f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    // Falling swaying snowflakes
    for (flake in snowflakes) {
        val progress = (phase * flake.speed + flake.relYOffset) % 1f
        val sway = sin(phase * 2f * PI.toFloat() * flake.swayFreq + flake.phase) * flake.swayAmp
        val x = (flake.relX * width + sway).coerceIn(0f, width)
        val y = progress * (height + flake.radius * 2f) - flake.radius

        if (y in 0f..height) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flake.alpha),
                        Color(0xFFE0F2FE).copy(alpha = flake.alpha * 0.60f),
                        Color.Transparent,
                    ),
                    radius = flake.radius * 1.3f,
                    center = Offset(x, y),
                ),
                radius = flake.radius,
                center = Offset(x, y),
            )
        }
    }
}

