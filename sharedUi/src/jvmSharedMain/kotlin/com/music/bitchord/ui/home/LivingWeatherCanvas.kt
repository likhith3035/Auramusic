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
    isDay: Boolean = true,
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

        val effectiveVisualType = when {
            visualType == WeatherVisualType.CLEAR_DAY && !isDay -> WeatherVisualType.CLEAR_NIGHT
            visualType == WeatherVisualType.CLEAR_NIGHT && isDay -> WeatherVisualType.CLEAR_DAY
            else -> visualType
        }

        when (effectiveVisualType) {
            WeatherVisualType.RAIN -> {
                drawRainScene(raindrops, clouds, rainPhase, slowPhase, width, height)
            }
            WeatherVisualType.CLEAR_DAY -> {
                drawSunnyScene(clouds, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLEAR_NIGHT -> {
                drawClearNightScene(stars, clouds, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.SUNRISE -> {
                drawSunriseScene(stars, clouds, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.SUNSET -> {
                drawSunsetScene(clouds, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLOUDY -> {
                drawCloudyScene(clouds, slowPhase, width, height)
            }
            WeatherVisualType.THUNDERSTORM -> {
                drawThunderstormScene(raindrops, clouds, rainPhase, slowPhase, lightningPhase, width, height)
            }
            WeatherVisualType.SNOW -> {
                drawSnowScene(snowflakes, clouds, rainPhase, slowPhase, width, height)
            }
        }

        // Text-protection scrim on the left: ensures digital clock, date, greeting, and subtitle remain 100% crisp and readable
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    blendColor.copy(alpha = 0.85f),
                    blendColor.copy(alpha = 0.65f),
                    blendColor.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
                startX = 0f,
                endX = (width * 0.65f).coerceAtLeast(180.dp.toPx()),
            ),
            size = Size(width, height),
        )

        // Seamless bottom fade scrim: completely melts into the Home screen background with zero bounding box or border
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    blendColor.copy(alpha = 0.35f),
                    blendColor.copy(alpha = 0.85f),
                    blendColor,
                ),
                startY = height * 0.48f,
                endY = height,
            ),
            size = Size(width, height),
        )
    }
}

private fun DrawScope.drawAtmosphericClouds(
    clouds: List<CloudCluster>,
    slowPhase: Float,
    width: Float,
    height: Float,
    cloudTint: Color,
    baseAlphaMultiplier: Float = 1.0f,
) {
    for (cloud in clouds) {
        val effectiveAlpha = cloud.alpha * baseAlphaMultiplier
        if (effectiveAlpha <= 0.01f) continue

        val driftProgress = (slowPhase * cloud.speed) % 1f
        val driftOffset = (driftProgress - 0.5f) * (width * 0.35f)
        val centerX = (cloud.relX * width + driftOffset).coerceIn(0f, width * 1.15f)
        val centerY = cloud.relY * height
        val baseR = height * 0.18f * cloud.scale

        // Volumetric overlapping soft Gaussian cloud lobes
        val lobes = listOf(
            Triple(Offset(centerX - baseR * 0.75f, centerY + baseR * 0.15f), baseR * 0.72f, baseR * 0.42f),
            Triple(Offset(centerX - baseR * 0.20f, centerY - baseR * 0.25f), baseR * 0.98f, baseR * 0.55f),
            Triple(Offset(centerX + baseR * 0.42f, centerY - baseR * 0.10f), baseR * 0.82f, baseR * 0.48f),
            Triple(Offset(centerX + baseR * 0.85f, centerY + baseR * 0.20f), baseR * 0.62f, baseR * 0.38f),
        )

        for ((pos, rX, rY) in lobes) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cloudTint.copy(alpha = effectiveAlpha),
                        cloudTint.copy(alpha = effectiveAlpha * 0.55f),
                        Color.Transparent,
                    ),
                    center = Offset(pos.x, pos.y - rY * 0.25f),
                    radius = rX * 1.25f,
                ),
                topLeft = Offset(pos.x - rX, pos.y - rY),
                size = Size(rX * 2f, rY * 2f),
            )
        }

        // Base atmospheric mist shelf
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(
                    cloudTint.copy(alpha = effectiveAlpha * 0.45f),
                    cloudTint.copy(alpha = effectiveAlpha * 0.15f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(centerX - baseR * 1.1f, centerY + baseR * 0.05f),
            size = Size(baseR * 2.2f, baseR * 0.50f),
        )
    }
}

private fun DrawScope.drawRainScene(
    raindrops: List<Raindrop>,
    clouds: List<CloudCluster>,
    rainPhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val focalX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    // Cool overcast ambient glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.16f),
                Color(0xFF0284C7).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.30f),
            radius = (width * 0.55f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    // Soft overcast rain clouds
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFF64748B),
        baseAlphaMultiplier = 0.75f,
    )

    // Draw falling slanted raindrops across full width
    for (drop in raindrops) {
        val progress = (rainPhase * drop.speed + drop.relYOffset) % 1f
        val startX = drop.relX * (width + 40.dp.toPx()) - progress * (width * 0.12f)
        val startY = progress * (height + drop.lengthPx * 2f) - drop.lengthPx

        val endX = startX - (drop.lengthPx * 0.32f)
        val endY = startY + drop.lengthPx

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
    clouds: List<CloudCluster>,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 76.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.44f
    val sunRadius = (height * 0.26f).coerceIn(46.dp.toPx(), 60.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Broad daylight atmospheric scattering filling sky with true Rayleigh warmth
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFA500).copy(alpha = 0.24f * pulseScale),
                Color(0xFFFFB703).copy(alpha = 0.14f * pulseScale),
                Color(0xFF38BDF8).copy(alpha = 0.05f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = (width * 0.60f).coerceAtLeast(sunRadius * 4.4f),
        ),
    )

    // 2. Multi-layered dynamic solar corona with gentle pulsation
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = 0.50f * pulseScale),
                Color(0xFFFFE082).copy(alpha = 0.30f * pulseScale),
                Color(0xFFFFB703).copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 2.35f * pulseScale,
        ),
    )

    // 3. Optical 8-point aperture starburst rays radiating from the blazing core
    val starburstAngles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
    for (angleDeg in starburstAngles) {
        val rad = (angleDeg + pulsePhase * 4f) * (PI.toFloat() / 180f)
        val isCardinal = (angleDeg % 90f == 0f)
        val rayLength = sunRadius * (if (isCardinal) 2.2f else 1.6f) * pulseScale
        val rayEnd = Offset(
            sunCenterX + cos(rad) * rayLength,
            sunCenterY + sin(rad) * rayLength,
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f * pulseScale),
                    Color(0xFFFEF08A).copy(alpha = 0.15f * pulseScale),
                    Color.Transparent,
                ),
                start = Offset(sunCenterX, sunCenterY),
                end = rayEnd,
            ),
            start = Offset(sunCenterX, sunCenterY),
            end = rayEnd,
            strokeWidth = if (isCardinal) 1.5f else 1.0f,
            cap = StrokeCap.Round,
        )
    }

    // 4. Cinematic horizontal anamorphic lens flare with subtle cyan-amber fringe
    val flareHalfSpan = sunRadius * 3.8f
    val flareThickness = sunRadius * 0.22f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.62f * pulseScale),
                Color(0xFFFEF08A).copy(alpha = 0.28f * pulseScale),
                Color(0xFF38BDF8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = flareHalfSpan,
        ),
        topLeft = Offset(sunCenterX - flareHalfSpan, sunCenterY - flareThickness * 0.5f),
        size = Size(flareHalfSpan * 2f, flareThickness),
    )

    // Secondary circular lens flare ghost reflection along optical axis
    val ghostCenterX = sunCenterX - sunRadius * 1.5f
    val ghostCenterY = sunCenterY + sunRadius * 0.85f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.15f * pulseScale),
                Color(0xFF818CF8).copy(alpha = 0.08f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(ghostCenterX, ghostCenterY),
            radius = sunRadius * 0.40f,
        ),
    )

    // 5. Photorealistic incandescent Sun body with limb darkening & white-hot core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),       // Specular white-hot core
                Color(0xFFFFFDE7),       // White-yellow inner photosphere
                Color(0xFFFEF08A),       // Photosphere midtones
                Color(0xFFFBBF24),       // Golden chromosphere
                Color(0xFFF59E0B),       // Solar amber
                Color(0xFFD97706),       // Limb darkening edge
            ),
            center = Offset(sunCenterX - sunRadius * 0.16f, sunCenterY - sunRadius * 0.16f),
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = Offset(sunCenterX, sunCenterY),
    )

    // 6. Photospheric specular crescent rim highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFFFF9C4).copy(alpha = 0.40f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX - sunRadius * 0.20f, sunCenterY - sunRadius * 0.20f),
            radius = sunRadius * 0.65f,
        ),
        radius = sunRadius * 0.65f,
        center = Offset(sunCenterX - sunRadius * 0.20f, sunCenterY - sunRadius * 0.20f),
    )

    // 7. Drift soft sunlit clouds across lower perimeter
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFFEF3C7),
        baseAlphaMultiplier = 0.55f,
    )
}

private fun DrawScope.drawSunriseScene(
    stars: List<Star>,
    clouds: List<CloudCluster>,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 76.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.52f
    val sunRadius = (height * 0.25f).coerceIn(44.dp.toPx(), 58.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Dawn sky atmosphere: deep twilight purple at top fading into soft coral, rose, and radiant morning gold
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF18112C).copy(alpha = 0.75f),
                Color(0xFF38153A).copy(alpha = 0.70f),
                Color(0xFF701F48).copy(alpha = 0.65f),
                Color(0xFFC2410C).copy(alpha = 0.55f),
                Color(0xFFFBBF24).copy(alpha = 0.50f),
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    // 2. Faint morning stars fading in the western upper sky
    for (star in stars) {
        if (star.relX < 0.65f && star.relY < 0.45f) {
            val starX = star.relX * width
            val starY = star.relY * height
            val twinkle = 0.20f + 0.50f * (0.5f + 0.5f * sin((slowPhase * 3f * PI + star.phase).toFloat()))
            drawCircle(
                color = Color(0xFFFFEDD5).copy(alpha = (twinkle * 0.45f).coerceIn(0f, 1f)),
                radius = star.radius * 0.8f,
                center = Offset(starX, starY),
            )
        }
    }

    // 3. Volumetric morning god-rays (crepuscular rays radiating upwards from the rising sun)
    val rayAngles = listOf(-80f, -60f, -40f, -20f, 0f, 20f, 40f)
    for (angleDeg in rayAngles) {
        val rad = angleDeg * (PI.toFloat() / 180f)
        val rayLen = width * 0.75f
        val rayEnd = Offset(
            sunCenterX + sin(rad) * rayLen,
            sunCenterY - cos(rad) * rayLen,
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.18f * pulseScale),
                    Color(0xFFFDBA74).copy(alpha = 0.08f * pulseScale),
                    Color.Transparent,
                ),
                start = Offset(sunCenterX, sunCenterY),
                end = rayEnd,
            ),
            start = Offset(sunCenterX, sunCenterY),
            end = rayEnd,
            strokeWidth = (22.dp.toPx() * (1f + 0.15f * sin(slowPhase * 2f * PI.toFloat()))),
            cap = StrokeCap.Round,
        )
    }

    // 4. Broad atmospheric morning dawn glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF59E0B).copy(alpha = 0.35f * pulseScale),
                Color(0xFFFB7185).copy(alpha = 0.20f * pulseScale),
                Color(0xFFC026D3).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = (width * 0.60f).coerceAtLeast(sunRadius * 4.5f),
        ),
    )

    // 5. Dawn solar corona
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A).copy(alpha = 0.55f * pulseScale),
                Color(0xFFFB923C).copy(alpha = 0.30f * pulseScale),
                Color(0xFFF43F5E).copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 2.2f * pulseScale,
        ),
    )

    // 6. Dawn anamorphic horizontal flare streak
    val flareHalfSpan = sunRadius * 3.8f
    val flareThickness = sunRadius * 0.22f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB).copy(alpha = 0.60f * pulseScale),
                Color(0xFFFDE68A).copy(alpha = 0.28f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = flareHalfSpan,
        ),
        topLeft = Offset(sunCenterX - flareHalfSpan, sunCenterY - flareThickness * 0.5f),
        size = Size(flareHalfSpan * 2f, flareThickness),
    )

    // 7. Photorealistic rising Sun body (molten peach-white core with dawn chromosphere)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),       // Blinding white core
                Color(0xFFFFFBEB),       // Incandescent mantle
                Color(0xFFFDE047),       // Morning gold
                Color(0xFFFB923C),       // Peach chromosphere
                Color(0xFFEA580C),       // Dawn limb
            ),
            center = Offset(sunCenterX - sunRadius * 0.15f, sunCenterY - sunRadius * 0.15f),
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = Offset(sunCenterX, sunCenterY),
    )

    // 8. Golden morning horizon mist layers
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFDE047).copy(alpha = 0.35f),
                Color(0xFFFB923C).copy(alpha = 0.18f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY + sunRadius * 0.6f),
            radius = sunRadius * 2.5f,
        ),
        topLeft = Offset(sunCenterX - sunRadius * 2.2f, sunCenterY + sunRadius * 0.2f),
        size = Size(sunRadius * 4.4f, sunRadius * 0.9f),
    )

    // 9. Soft morning clouds catching peach and gold dawn light
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFFED7AA),
        baseAlphaMultiplier = 0.60f,
    )
}

private fun DrawScope.drawSunsetScene(
    clouds: List<CloudCluster>,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 76.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.48f
    val sunRadius = (height * 0.26f).coerceIn(46.dp.toPx(), 60.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Rich twilight/dusk sky atmosphere: deep indigo-violet at top to crimson, burning orange, and sunset gold
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F0B1E).copy(alpha = 0.80f),
                Color(0xFF260D2E).copy(alpha = 0.75f),
                Color(0xFF581234).copy(alpha = 0.70f),
                Color(0xFF9F1239).copy(alpha = 0.65f),
                Color(0xFFC2410C).copy(alpha = 0.60f),
                Color(0xFFF59E0B).copy(alpha = 0.50f),
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    // 2. Broad blazing horizontal sunset horizon glow
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF97316).copy(alpha = 0.38f * pulseScale),
                Color(0xFFBE123C).copy(alpha = 0.22f * pulseScale),
                Color(0xFF4C0519).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY + sunRadius * 0.2f),
            radius = width * 0.65f,
        ),
        topLeft = Offset(sunCenterX - width * 0.65f, sunCenterY - sunRadius * 1.5f),
        size = Size(width * 1.3f, sunRadius * 3.5f),
    )

    // 3. Volumetric burning sunset corona
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFED7AA).copy(alpha = 0.55f * pulseScale),
                Color(0xFFF97316).copy(alpha = 0.35f * pulseScale),
                Color(0xFFE11D48).copy(alpha = 0.15f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 2.4f * pulseScale,
        ),
    )

    // 4. Blazing ruby-amber horizontal anamorphic lens flare
    val flareHalfSpan = sunRadius * 4.2f
    val flareThickness = sunRadius * 0.26f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB).copy(alpha = 0.65f * pulseScale),
                Color(0xFFFB923C).copy(alpha = 0.32f * pulseScale),
                Color(0xFFE11D48).copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = flareHalfSpan,
        ),
        topLeft = Offset(sunCenterX - flareHalfSpan, sunCenterY - flareThickness * 0.5f),
        size = Size(flareHalfSpan * 2f, flareThickness),
    )

    // 5. Photorealistic molten sunset Sun with rich limb darkening
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),       // Intense white-amber core
                Color(0xFFFEF3C7),       // White-gold inner mantle
                Color(0xFFFBBF24),       // Sunset gold
                Color(0xFFF97316),       // Burning orange
                Color(0xFFDC2626),       // Cadmium red mantle
                Color(0xFF991B1B),       // Crimson sunset limb
            ),
            center = Offset(sunCenterX - sunRadius * 0.12f, sunCenterY - sunRadius * 0.12f),
            radius = sunRadius,
        ),
        radius = sunRadius,
        center = Offset(sunCenterX, sunCenterY),
    )

    // 6. Sunset rim specular crescent highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color(0xFFFEF08A).copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX - sunRadius * 0.18f, sunCenterY - sunRadius * 0.18f),
            radius = sunRadius * 0.62f,
        ),
        radius = sunRadius * 0.62f,
        center = Offset(sunCenterX - sunRadius * 0.18f, sunCenterY - sunRadius * 0.18f),
    )

    // 7. Dramatic backlit sunset clouds with glowing fiery orange-red bottoms and deep purple tops
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFF87171),
        baseAlphaMultiplier = 0.65f,
    )
}

private fun DrawScope.drawClearNightScene(
    stars: List<Star>,
    clouds: List<CloudCluster>,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val moonCenterX = (width - 76.dp.toPx()).coerceAtLeast(width * 0.74f)
    val moonCenterY = height * 0.44f
    val moonRadius = (height * 0.28f).coerceIn(48.dp.toPx(), 64.dp.toPx())

    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Ambient celestial deep night glow with subtle Rayleigh starlight dispersion
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4338CA).copy(alpha = 0.22f * pulseScale),
                Color(0xFF1E1B4B).copy(alpha = 0.12f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = (width * 0.65f).coerceAtLeast(moonRadius * 3.6f),
        ),
    )

    // 2. Twinkling stars across the sky with cross diffraction spikes
    for (star in stars) {
        val starX = star.relX * width
        val starY = star.relY * height
        val twinkle = 0.25f + 0.70f * (0.5f + 0.5f * sin((slowPhase * 4f * PI + star.phase).toFloat()))
        val starAlpha = twinkle.coerceIn(0f, 1f)

        if (star.isDiamond) {
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

    // 3. Volumetric ethereal lunar corona halo (soft white inner glow with violet-blue Rayleigh outer fringe)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f * pulseScale),
                Color(0xFFE0E7FF).copy(alpha = 0.20f * pulseScale),
                Color(0xFF818CF8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 2.15f * pulseScale,
        ),
    )

    // Close atmospheric lunar bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.58f * pulseScale),
                Color(0xFFEDE9FE).copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 1.34f,
        ),
    )

    // 4. Photorealistic 3D Spherical Moon Body with multi-stop regolith crust
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),       // Specular highland
                Color(0xFFF8FAFC),       // Bright anorthosite crust
                Color(0xFFE2E8F0),       // Anorthosite regolith
                Color(0xFFCBD5E1),       // Regolith midtones
                Color(0xFF94A3B8),       // Shaded regolith
                Color(0xFF64748B),       // Terminator edge
                Color(0xFF334155),       // Shadow rim
            ),
            center = Offset(moonCenterX - moonRadius * 0.28f, moonCenterY - moonRadius * 0.28f),
            radius = moonRadius * 1.25f,
        ),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY),
    )

    // 5. Authentic Lunar Maria (basaltic plains matching actual lunar geography)
    // Oceanus Procellarum & Mare Imbrium (northwest)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF334155).copy(alpha = 0.54f),
                Color(0xFF475569).copy(alpha = 0.26f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX - moonRadius * 0.32f, moonCenterY - moonRadius * 0.26f),
            radius = moonRadius * 0.40f,
        ),
        topLeft = Offset(moonCenterX - moonRadius * 0.65f, moonCenterY - moonRadius * 0.55f),
        size = Size(moonRadius * 0.66f, moonRadius * 0.52f),
    )

    // Sinus Iridum (Bay of Rainbows - curved highland promontory)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.45f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX - moonRadius * 0.42f, moonCenterY - moonRadius * 0.45f),
            radius = moonRadius * 0.16f,
        ),
        topLeft = Offset(moonCenterX - moonRadius * 0.50f, moonCenterY - moonRadius * 0.52f),
        size = Size(moonRadius * 0.22f, moonRadius * 0.18f),
    )

    // Mare Serenitatis & Mare Tranquillitatis (northeast/east)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.50f),
                Color(0xFF334155).copy(alpha = 0.24f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX + moonRadius * 0.20f, moonCenterY - moonRadius * 0.16f),
            radius = moonRadius * 0.32f,
        ),
        topLeft = Offset(moonCenterX + moonRadius * 0.02f, moonCenterY - moonRadius * 0.38f),
        size = Size(moonRadius * 0.50f, moonRadius * 0.40f),
    )

    // Mare Crisium (far eastern circular sea)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.52f),
                Color(0xFF334155).copy(alpha = 0.22f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX + moonRadius * 0.54f, moonCenterY - moonRadius * 0.08f),
            radius = moonRadius * 0.18f,
        ),
        topLeft = Offset(moonCenterX + moonRadius * 0.40f, moonCenterY - moonRadius * 0.22f),
        size = Size(moonRadius * 0.28f, moonRadius * 0.26f),
    )

    // Mare Fecunditatis & Mare Nectaris (southeast)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF334155).copy(alpha = 0.45f),
                Color(0xFF475569).copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX + moonRadius * 0.30f, moonCenterY + moonRadius * 0.24f),
            radius = moonRadius * 0.26f,
        ),
        topLeft = Offset(moonCenterX + moonRadius * 0.12f, moonCenterY + moonRadius * 0.08f),
        size = Size(moonRadius * 0.42f, moonRadius * 0.36f),
    )

    // Mare Nubium & Mare Humorum (southwest)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.48f),
                Color(0xFF334155).copy(alpha = 0.22f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX - moonRadius * 0.18f, moonCenterY + moonRadius * 0.26f),
            radius = moonRadius * 0.28f,
        ),
        topLeft = Offset(moonCenterX - moonRadius * 0.38f, moonCenterY + moonRadius * 0.10f),
        size = Size(moonRadius * 0.46f, moonRadius * 0.38f),
    )

    // 6. Tycho Crater & Radiating Ejecta Rays (south highlands)
    val tychoX = moonCenterX - moonRadius * 0.06f
    val tychoY = moonCenterY + moonRadius * 0.58f

    // Bright crater impact core
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = moonRadius * 0.06f,
        center = Offset(tychoX, tychoY),
    )
    drawCircle(
        color = Color(0xFF1E293B).copy(alpha = 0.40f),
        radius = moonRadius * 0.09f,
        center = Offset(tychoX, tychoY),
        style = Stroke(width = 1.0f),
    )

    // 12 Radiating high-albedo ejecta rays reaching across the lunar disk
    val rayAngles = listOf(195f, 210f, 225f, 245f, 270f, 290f, 310f, 330f, 345f, 5f, 20f, 35f)
    for (angleDeg in rayAngles) {
        val rad = angleDeg * (PI.toFloat() / 180f)
        val rayLen = moonRadius * 0.70f
        val endX = tychoX + cos(rad) * rayLen
        val endY = tychoY + sin(rad) * rayLen
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                start = Offset(tychoX, tychoY),
                end = Offset(endX, endY),
            ),
            start = Offset(tychoX, tychoY),
            end = Offset(endX, endY),
            strokeWidth = 1.0f,
        )
    }

    // Copernicus crater (equatorial northwest) with dual terraced rings
    val copernicusX = moonCenterX - moonRadius * 0.26f
    val copernicusY = moonCenterY + moonRadius * 0.04f
    drawCircle(
        color = Color.White.copy(alpha = 0.90f),
        radius = moonRadius * 0.05f,
        center = Offset(copernicusX, copernicusY),
    )
    drawCircle(
        color = Color(0xFF334155).copy(alpha = 0.35f),
        radius = moonRadius * 0.08f,
        center = Offset(copernicusX, copernicusY),
        style = Stroke(width = 0.8f),
    )

    // Aristarchus Crater (brightest anomaly in the northwest)
    val aristarchusX = moonCenterX - moonRadius * 0.44f
    val aristarchusY = moonCenterY - moonRadius * 0.10f
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = moonRadius * 0.035f,
        center = Offset(aristarchusX, aristarchusY),
    )

    // 7. Limb Darkening & Spherical Rim Depth
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0xFF0F172A).copy(alpha = 0.18f),
                Color(0xFF020617).copy(alpha = 0.52f),
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius,
        ),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY),
    )

    // 8. Soft nocturnal moonlit clouds passing peacefully
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFC7D2FE),
        baseAlphaMultiplier = 0.38f,
    )
}

private fun DrawScope.drawCloudyScene(
    clouds: List<CloudCluster>,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val focalX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.76f)
    // Soft overcast ambient illumination
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFCBD5E1).copy(alpha = 0.22f),
                Color(0xFF64748B).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.38f),
            radius = (width * 0.60f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFE2E8F0),
        baseAlphaMultiplier = 1.0f,
    )
}

private fun DrawScope.drawThunderstormScene(
    raindrops: List<Raindrop>,
    clouds: List<CloudCluster>,
    rainPhase: Float,
    slowPhase: Float,
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

    // Atmospheric ionization sheet flash
    val ambientColor = if (flashIntensity > 0.05f) {
        Color(0xFF93C5FD).copy(alpha = 0.18f + 0.40f * flashIntensity)
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
            radius = (width * 0.65f).coerceAtLeast(150.dp.toPx()),
        ),
    )

    // Storm clouds
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFF334155),
        baseAlphaMultiplier = 0.85f,
    )

    // Lightning bolt with branched forks during peak flash
    if (flashIntensity > 0.30f) {
        val startY = height * 0.06f
        val primaryBolt = Path().apply {
            moveTo(boltStartX, startY)
            lineTo(boltStartX - 12f, startY + height * 0.20f)
            lineTo(boltStartX + 8f, startY + height * 0.26f)
            lineTo(boltStartX - 16f, startY + height * 0.52f)
            lineTo(boltStartX - 6f, startY + height * 0.58f)
            lineTo(boltStartX - 22f, startY + height * 0.86f)
        }

        val forkBranch1 = Path().apply {
            moveTo(boltStartX + 8f, startY + height * 0.26f)
            lineTo(boltStartX + 26f, startY + height * 0.42f)
            lineTo(boltStartX + 20f, startY + height * 0.48f)
            lineTo(boltStartX + 34f, startY + height * 0.64f)
        }

        val forkBranch2 = Path().apply {
            moveTo(boltStartX - 16f, startY + height * 0.52f)
            lineTo(boltStartX - 32f, startY + height * 0.68f)
        }

        for (path in listOf(primaryBolt, forkBranch1, forkBranch2)) {
            val isFork = path != primaryBolt
            val glowWidth = if (isFork) 3.0f else 5.5f
            drawPath(
                path = path,
                color = Color(0xFFA5B4FC).copy(alpha = flashIntensity * (if (isFork) 0.50f else 0.75f)),
                style = Stroke(width = glowWidth, cap = StrokeCap.Round),
            )
            val coreWidth = if (isFork) 1.2f else 2.0f
            drawPath(
                path = path,
                color = Color.White.copy(alpha = flashIntensity * (if (isFork) 0.75f else 1.0f)),
                style = Stroke(width = coreWidth, cap = StrokeCap.Round),
            )
        }
    }

    // Heavy storm rain streaks across full width
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
    clouds: List<CloudCluster>,
    rainPhase: Float,
    slowPhase: Float,
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

    // Soft winter clouds
    drawAtmosphericClouds(
        clouds = clouds,
        slowPhase = slowPhase,
        width = width,
        height = height,
        cloudTint = Color(0xFFE2E8F0),
        baseAlphaMultiplier = 0.50f,
    )

    // Falling swaying snowflakes
    for (flake in snowflakes) {
        val progress = (rainPhase * flake.speed + flake.relYOffset) % 1f
        val sway = sin(rainPhase * 2f * PI.toFloat() * flake.swayFreq + flake.phase) * flake.swayAmp
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

