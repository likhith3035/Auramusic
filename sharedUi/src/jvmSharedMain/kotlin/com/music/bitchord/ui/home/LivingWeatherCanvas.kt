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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.weather.WeatherVisualType
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.aura_cloud_cumulus
import com.music.bitchord.sharedui.resources.aura_cloud_storm
import com.music.bitchord.sharedui.resources.aura_cloud_wispy
import com.music.bitchord.sharedui.resources.aura_moon_photorealistic
import com.music.bitchord.sharedui.resources.aura_sun_day_photorealistic
import com.music.bitchord.sharedui.resources.aura_sun_sunset_photorealistic
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class RainDepthTier {
    FAR,
    MID,
    NEAR,
}

private data class Raindrop(
    val relX: Float,
    val relYOffset: Float,
    val speed: Float,
    val lengthPx: Float,
    val alpha: Float,
    val strokeWidth: Float,
    val tier: RainDepthTier,
    val splashPhaseBias: Float,
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

private enum class CloudTextureKind {
    CUMULUS,
    STORM,
    WISPY,
}

private data class CloudSpritePlacement(
    val kind: CloudTextureKind,
    val relX: Float,
    val relY: Float,
    val widthRel: Float,
    val speed: Float,
    val alpha: Float,
)

private data class CloudSpritePack(
    val cumulus: Painter,
    val storm: Painter,
    val wispy: Painter,
)

/**
 * Living, hardware-accelerated ambient weather canvas.
 * Combines photorealistic astronomical & meteorological sprites (NASA Moon/Sun discs,
 * volumetric cumulus/storm/wispy clouds) with multi-depth particle shaders (3-tier parallax
 * rain with splash droplets, swaying snow, twinkling stars, and storm sheet lightning).
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

    val animRainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain_phase",
    )

    val animSlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slow_phase",
    )

    val animPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_phase",
    )

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

    // Pre-allocated 3-tier parallax rainfall particles (far mist, mid crisp streaks, near soft motion drops)
    val raindrops = remember {
        val rng = Random(42)
        val farDrops = List(42) {
            Raindrop(
                relX = rng.nextFloat(),
                relYOffset = rng.nextFloat(),
                speed = 0.72f + rng.nextFloat() * 0.35f,
                lengthPx = 10f + rng.nextFloat() * 9f,
                alpha = 0.14f + rng.nextFloat() * 0.16f,
                strokeWidth = 0.75f + rng.nextFloat() * 0.35f,
                tier = RainDepthTier.FAR,
                splashPhaseBias = rng.nextFloat(),
            )
        }
        val midDrops = List(34) {
            Raindrop(
                relX = rng.nextFloat(),
                relYOffset = rng.nextFloat(),
                speed = 0.95f + rng.nextFloat() * 0.55f,
                lengthPx = 18f + rng.nextFloat() * 16f,
                alpha = 0.38f + rng.nextFloat() * 0.38f,
                strokeWidth = 1.15f + rng.nextFloat() * 0.65f,
                tier = RainDepthTier.MID,
                splashPhaseBias = rng.nextFloat(),
            )
        }
        val nearDrops = List(10) {
            Raindrop(
                relX = rng.nextFloat(),
                relYOffset = rng.nextFloat(),
                speed = 1.35f + rng.nextFloat() * 0.45f,
                lengthPx = 30f + rng.nextFloat() * 20f,
                alpha = 0.16f + rng.nextFloat() * 0.14f,
                strokeWidth = 2.1f + rng.nextFloat() * 0.9f,
                tier = RainDepthTier.NEAR,
                splashPhaseBias = rng.nextFloat(),
            )
        }
        farDrops + midDrops + nearDrops
    }

    val stars = remember {
        val rng = Random(101)
        List(32) { index ->
            Star(
                relX = 0.04f + rng.nextFloat() * 0.92f,
                relY = 0.06f + rng.nextFloat() * 0.80f,
                radius = 0.9f + rng.nextFloat() * 1.7f,
                phase = rng.nextFloat() * 2f * PI.toFloat(),
                isDiamond = index % 5 == 0,
            )
        }
    }

    val snowflakes = remember {
        val rng = Random(202)
        List(30) {
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

    // Photorealistic astronomical and meteorological textures
    val moonPainter = painterResource(Res.drawable.aura_moon_photorealistic)
    val sunDayPainter = painterResource(Res.drawable.aura_sun_day_photorealistic)
    val sunSunsetPainter = painterResource(Res.drawable.aura_sun_sunset_photorealistic)
    val cloudSprites = CloudSpritePack(
        cumulus = painterResource(Res.drawable.aura_cloud_cumulus),
        storm = painterResource(Res.drawable.aura_cloud_storm),
        wispy = painterResource(Res.drawable.aura_cloud_wispy),
    )

    // Balanced cloud formations positioned primarily in right & mid sky to frame celestial bodies cleanly
    val dayClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.42f, relY = 0.24f, widthRel = 0.52f, speed = 0.16f, alpha = 0.48f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, relX = 0.88f, relY = 0.54f, widthRel = 0.44f, speed = 0.24f, alpha = 0.78f),
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.62f, relY = 0.66f, widthRel = 0.48f, speed = 0.20f, alpha = 0.42f),
        )
    }

    val nightClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.44f, relY = 0.28f, widthRel = 0.54f, speed = 0.14f, alpha = 0.34f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, relX = 0.86f, relY = 0.64f, widthRel = 0.46f, speed = 0.22f, alpha = 0.36f),
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.66f, relY = 0.72f, widthRel = 0.50f, speed = 0.18f, alpha = 0.28f),
        )
    }

    val overcastClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.28f, relY = 0.22f, widthRel = 0.62f, speed = 0.18f, alpha = 0.55f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, relX = 0.62f, relY = 0.38f, widthRel = 0.56f, speed = 0.28f, alpha = 0.88f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, relX = 0.90f, relY = 0.52f, widthRel = 0.52f, speed = 0.22f, alpha = 0.82f),
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.46f, relY = 0.64f, widthRel = 0.60f, speed = 0.24f, alpha = 0.45f),
        )
    }

    val stormClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, relX = 0.22f, relY = 0.16f, widthRel = 0.65f, speed = 0.20f, alpha = 0.28f),
            CloudSpritePlacement(CloudTextureKind.STORM, relX = 0.58f, relY = 0.26f, widthRel = 0.62f, speed = 0.30f, alpha = 0.92f),
            CloudSpritePlacement(CloudTextureKind.STORM, relX = 0.88f, relY = 0.34f, widthRel = 0.58f, speed = 0.26f, alpha = 0.94f),
            CloudSpritePlacement(CloudTextureKind.STORM, relX = 0.34f, relY = 0.38f, widthRel = 0.50f, speed = 0.22f, alpha = 0.78f),
        )
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
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
                drawRainScene(raindrops, stormClouds, cloudSprites, rainPhase, slowPhase, width, height)
            }
            WeatherVisualType.CLEAR_DAY -> {
                drawSunnyScene(sunDayPainter, dayClouds, cloudSprites, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLEAR_NIGHT -> {
                drawClearNightScene(stars, nightClouds, cloudSprites, pulsePhase, slowPhase, width, height, moonPainter)
            }
            WeatherVisualType.SUNRISE -> {
                drawSunriseScene(stars, sunSunsetPainter, dayClouds, cloudSprites, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.SUNSET -> {
                drawSunsetScene(sunSunsetPainter, dayClouds, cloudSprites, pulsePhase, slowPhase, width, height)
            }
            WeatherVisualType.CLOUDY -> {
                drawCloudyScene(overcastClouds, cloudSprites, slowPhase, width, height)
            }
            WeatherVisualType.THUNDERSTORM -> {
                drawThunderstormScene(raindrops, stormClouds, cloudSprites, rainPhase, slowPhase, lightningPhase, width, height)
            }
            WeatherVisualType.SNOW -> {
                drawSnowScene(snowflakes, overcastClouds, cloudSprites, rainPhase, slowPhase, width, height)
            }
        }

        // Left vignette scrim preserves atmospheric sky while keeping clock and greeting crisp
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    blendColor.copy(alpha = 0.52f),
                    blendColor.copy(alpha = 0.32f),
                    blendColor.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                startX = 0f,
                endX = (width * 0.68f).coerceAtLeast(180.dp.toPx()),
            ),
            size = Size(width, height),
        )

        // Seamless bottom fade melts into the Home screen surface with zero hard border
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    blendColor.copy(alpha = 0.42f),
                    blendColor.copy(alpha = 0.88f),
                    blendColor,
                ),
                startY = height * 0.52f,
                endY = height,
            ),
            size = Size(width, height),
        )
    }
}

private fun DrawScope.drawParallaxClouds(
    placements: List<CloudSpritePlacement>,
    sprites: CloudSpritePack,
    slowPhase: Float,
    width: Float,
    height: Float,
    tintColor: Color,
    alphaMultiplier: Float = 1.0f,
) {
    for (p in placements) {
        val effectiveAlpha = (p.alpha * alphaMultiplier).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0.01f) continue

        val painter = when (p.kind) {
            CloudTextureKind.CUMULUS -> sprites.cumulus
            CloudTextureKind.STORM -> sprites.storm
            CloudTextureKind.WISPY -> sprites.wispy
        }
        val intrinsic = painter.intrinsicSize
        val aspect = if (intrinsic.width > 0f && intrinsic.height > 0f) {
            intrinsic.height / intrinsic.width
        } else {
            0.44f
        }

        val drawW = (width * p.widthRel).coerceAtLeast(140.dp.toPx())
        val drawH = drawW * aspect
        val driftSpan = width * 0.22f
        val driftOffset = sin(slowPhase * 2f * PI.toFloat() + p.relX * 5f) * driftSpan * p.speed

        val left = (p.relX * width) - (drawW * 0.5f) + driftOffset
        val top = (p.relY * height) - (drawH * 0.5f)

        val filter = if (tintColor == Color.White) {
            null
        } else {
            ColorFilter.tint(tintColor, BlendMode.Modulate)
        }

        with(painter) {
            translate(left = left, top = top) {
                draw(
                    size = Size(drawW, drawH),
                    alpha = effectiveAlpha,
                    colorFilter = filter,
                )
            }
        }
    }
}

private fun DrawScope.drawCelestialSun(
    sunPainter: Painter,
    sunCenterX: Float,
    sunCenterY: Float,
    sunRadius: Float,
    pulseScale: Float,
    coronaInnerColor: Color,
    coronaMidColor: Color,
    coronaOuterColor: Color,
    flareTintColor: Color,
    limbOverlayColor: Color,
) {
    // 1. Wide multi-stop atmospheric corona bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coronaInnerColor.copy(alpha = 0.58f * pulseScale),
                coronaMidColor.copy(alpha = 0.34f * pulseScale),
                coronaOuterColor.copy(alpha = 0.14f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = sunRadius * 2.45f * pulseScale,
        ),
    )

    // 2. Horizontal cinematic optical lens flare streak
    val flareHalfSpan = sunRadius * 3.6f
    val flareThickness = sunRadius * 0.22f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f * pulseScale),
                flareTintColor.copy(alpha = 0.28f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = flareHalfSpan,
        ),
        topLeft = Offset(sunCenterX - flareHalfSpan, sunCenterY - flareThickness * 0.5f),
        size = Size(flareHalfSpan * 2f, flareThickness),
    )

    // 3. Photorealistic NASA SDO Solar Disc Sprite (textured photosphere + corona prominences)
    val spriteSize = sunRadius * 2.26f
    with(sunPainter) {
        translate(left = sunCenterX - spriteSize * 0.5f, top = sunCenterY - spriteSize * 0.5f) {
            draw(
                size = Size(spriteSize, spriteSize),
                alpha = 1.0f,
            )
        }
    }

    // 4. Soft incandescent core bloom uniting textured photosphere with atmospheric sky glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                limbOverlayColor.copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX - sunRadius * 0.12f, sunCenterY - sunRadius * 0.12f),
            radius = sunRadius * 1.08f,
        ),
        radius = sunRadius * 1.08f,
        center = Offset(sunCenterX, sunCenterY),
    )
}

private fun DrawScope.drawMultiDepthRain(
    raindrops: List<Raindrop>,
    rainPhase: Float,
    width: Float,
    height: Float,
    streakSpeedMultiplier: Float,
    windTiltFactor: Float,
    streakCoolColor: Color,
    streakBrightColor: Color,
    rippleAlphaScale: Float = 1.0f,
) {
    // Low-lying humid rain mist near the bottom edge
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                streakCoolColor.copy(alpha = 0.08f),
                streakCoolColor.copy(alpha = 0.15f),
            ),
            startY = height * 0.55f,
            endY = height,
        ),
        size = Size(width, height),
    )

    for (drop in raindrops) {
        val tierAlphaMul = when (drop.tier) {
            RainDepthTier.FAR -> 0.85f
            RainDepthTier.MID -> 1.0f
            RainDepthTier.NEAR -> 0.90f
        }
        val progress = (rainPhase * drop.speed * streakSpeedMultiplier + drop.relYOffset) % 1f
        val startX = drop.relX * (width + 48.dp.toPx()) - progress * (width * windTiltFactor)
        val startY = progress * (height + drop.lengthPx * 2f) - drop.lengthPx
        val endX = startX - (drop.lengthPx * (0.28f + windTiltFactor * 0.6f))
        val endY = startY + drop.lengthPx

        if (startY < height && endY > 0f) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        streakCoolColor.copy(alpha = drop.alpha * 0.45f * tierAlphaMul),
                        streakBrightColor.copy(alpha = drop.alpha * tierAlphaMul),
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

        // Expanding dual ripples + fine upward splash droplets for mid-ground rain hitting bottom boundary
        if (drop.tier == RainDepthTier.MID && progress > 0.84f) {
            val splashProgress = ((progress - 0.84f) / 0.16f).coerceIn(0f, 1f)
            val rippleRadius = (4f + splashProgress * 12f)
            val rippleAlpha = (1f - splashProgress) * 0.45f * drop.alpha * rippleAlphaScale
            val groundY = height - 8f - (drop.splashPhaseBias * 14f)

            if (rippleAlpha > 0.02f) {
                drawOval(
                    color = streakBrightColor.copy(alpha = rippleAlpha),
                    topLeft = Offset(endX - rippleRadius, groundY - rippleRadius * 0.22f),
                    size = Size(rippleRadius * 2f, rippleRadius * 0.44f),
                    style = Stroke(width = 1.0f),
                )

                // Two bouncing splash micro-droplets
                val arcRise = sin(splashProgress * PI.toFloat()) * (6f + drop.splashPhaseBias * 6f)
                val spreadX = splashProgress * (5f + drop.splashPhaseBias * 5f)
                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha * 1.15f),
                    radius = 1.2f,
                    center = Offset(endX - spreadX, groundY - arcRise),
                )
                drawCircle(
                    color = streakBrightColor.copy(alpha = rippleAlpha * 1.10f),
                    radius = 1.0f,
                    center = Offset(endX + spreadX * 0.85f, groundY - arcRise * 0.8f),
                )
            }
        }
    }
}

private fun DrawScope.drawRainScene(
    raindrops: List<Raindrop>,
    stormClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    rainPhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    // Moody cool blue-slate overcast sky atmosphere
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0E1A2B).copy(alpha = 0.92f),
                Color(0xFF16263B).copy(alpha = 0.84f),
                Color(0xFF1C3047).copy(alpha = 0.65f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    val focalX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.72f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.16f),
                Color(0xFF0284C7).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.28f),
            radius = (width * 0.58f).coerceAtLeast(140.dp.toPx()),
        ),
    )

    // Photorealistic storm cloud layers drifting across the top canopy
    drawParallaxClouds(
        placements = stormClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFD5E2F2),
        alphaMultiplier = 0.90f,
    )

    drawMultiDepthRain(
        raindrops = raindrops,
        rainPhase = rainPhase,
        width = width,
        height = height,
        streakSpeedMultiplier = 1.05f,
        windTiltFactor = 0.12f,
        streakCoolColor = Color(0xFF7DD3FC),
        streakBrightColor = Color(0xFFE0F2FE),
    )
}

private fun DrawScope.drawSunnyScene(
    sunDayPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.44f
    val sunRadius = (height * 0.30f).coerceIn(54.dp.toPx(), 64.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Crisp azure daytime sky atmosphere fading into the canopy
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0C2D59).copy(alpha = 0.90f),
                Color(0xFF15457A).copy(alpha = 0.80f),
                Color(0xFF1B5791).copy(alpha = 0.55f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    // 2. Warm solar Rayleigh scattering radiating around sun position
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB703).copy(alpha = 0.28f * pulseScale),
                Color(0xFF38BDF8).copy(alpha = 0.14f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = (width * 0.65f).coerceAtLeast(sunRadius * 4.5f),
        ),
    )

    // 3. Secondary optical lens ghost reflection
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.16f * pulseScale),
                Color(0xFFFDE047).copy(alpha = 0.08f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(sunCenterX - sunRadius * 1.65f, sunCenterY + sunRadius * 0.72f),
            radius = sunRadius * 0.42f,
        ),
    )

    // 4. Photorealistic white-gold solar orb + corona
    drawCelestialSun(
        sunPainter = sunDayPainter,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        coronaInnerColor = Color(0xFFFFF9C4),
        coronaMidColor = Color(0xFFFDE047),
        coronaOuterColor = Color(0xFFF59E0B),
        flareTintColor = Color(0xFFFEF08A),
        limbOverlayColor = Color(0xFFFFF3B0),
    )

    // 5. Realistic sunlit cumulus & wispy clouds drifting softly around lower solar edge
    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFFFFBF2),
        alphaMultiplier = 0.92f,
    )
}

private fun DrawScope.drawSunriseScene(
    stars: List<Star>,
    sunSunsetPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.54f
    val sunRadius = (height * 0.29f).coerceIn(52.dp.toPx(), 62.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Dawn sky atmosphere: twilight indigo -> rose -> amber horizon
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF18112C).copy(alpha = 0.85f),
                Color(0xFF3E183E).copy(alpha = 0.78f),
                Color(0xFF832346).copy(alpha = 0.70f),
                Color(0xFFD9531E).copy(alpha = 0.55f),
                Color(0xFFFBBF24).copy(alpha = 0.42f),
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    // 2. Faint fading morning stars
    for (star in stars) {
        if (star.relX < 0.65f && star.relY < 0.45f) {
            val twinkle = 0.20f + 0.50f * (0.5f + 0.5f * sin((slowPhase * 3f * PI + star.phase).toFloat()))
            drawCircle(
                color = Color(0xFFFFEDD5).copy(alpha = (twinkle * 0.45f).coerceIn(0f, 1f)),
                radius = star.radius * 0.8f,
                center = Offset(star.relX * width, star.relY * height),
            )
        }
    }

    // 3. Crepuscular dawn god-rays radiating upward from rising sun
    val rayAngles = listOf(-75f, -52f, -30f, -8f, 14f, 36f)
    for (angleDeg in rayAngles) {
        val rad = angleDeg * (PI.toFloat() / 180f)
        val rayLen = width * 0.72f
        val rayEnd = Offset(sunCenterX + sin(rad) * rayLen, sunCenterY - cos(rad) * rayLen)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.16f * pulseScale),
                    Color(0xFFFDBA74).copy(alpha = 0.06f * pulseScale),
                    Color.Transparent,
                ),
                start = Offset(sunCenterX, sunCenterY),
                end = rayEnd,
            ),
            start = Offset(sunCenterX, sunCenterY),
            end = rayEnd,
            strokeWidth = 20.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // 4. Photorealistic rising sun orb
    drawCelestialSun(
        sunPainter = sunSunsetPainter,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        coronaInnerColor = Color(0xFFFEF08A),
        coronaMidColor = Color(0xFFFB923C),
        coronaOuterColor = Color(0xFFF43F5E),
        flareTintColor = Color(0xFFFDE68A),
        limbOverlayColor = Color(0xFFFED7AA),
    )

    // 5. Dawn cumulus & wispy clouds glowing warm peach/gold
    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFFFD6A5),
        alphaMultiplier = 0.90f,
    )
}

private fun DrawScope.drawSunsetScene(
    sunSunsetPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = height * 0.50f
    val sunRadius = (height * 0.30f).coerceIn(54.dp.toPx(), 64.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Deep twilight dusk sky atmosphere: violet -> crimson -> amber
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF100B20).copy(alpha = 0.88f),
                Color(0xFF2B0E32).copy(alpha = 0.82f),
                Color(0xFF6B1438).copy(alpha = 0.74f),
                Color(0xFFA81D38).copy(alpha = 0.62f),
                Color(0xFFC2410C).copy(alpha = 0.50f),
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    // 2. Wide warm sunset horizon glow
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF97316).copy(alpha = 0.38f * pulseScale),
                Color(0xFFBE123C).copy(alpha = 0.22f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY + sunRadius * 0.18f),
            radius = width * 0.65f,
        ),
        topLeft = Offset(sunCenterX - width * 0.65f, sunCenterY - sunRadius * 1.4f),
        size = Size(width * 1.3f, sunRadius * 3.2f),
    )

    // 3. Photorealistic molten sunset solar orb
    drawCelestialSun(
        sunPainter = sunSunsetPainter,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        coronaInnerColor = Color(0xFFFED7AA),
        coronaMidColor = Color(0xFFF97316),
        coronaOuterColor = Color(0xFFE11D48),
        flareTintColor = Color(0xFFFB923C),
        limbOverlayColor = Color(0xFFFCA5A5),
    )

    // 4. Backlit dusk clouds tinted fiery coral & rose
    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFFFA585),
        alphaMultiplier = 0.88f,
    )
}

private fun DrawScope.drawClearNightScene(
    stars: List<Star>,
    nightClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
    moonPainter: Painter,
) {
    val moonCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val moonCenterY = height * 0.46f
    // Pin lunar radius around 58–64dp so trimming the lower mood-chip area leaves the Moon full-sized
    val moonRadius = (height * 0.31f).coerceIn(54.dp.toPx(), 64.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    // 1. Deep nocturnal Rayleigh starlight glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4338CA).copy(alpha = 0.24f * pulseScale),
                Color(0xFF1E1B4B).copy(alpha = 0.12f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = (width * 0.65f).coerceAtLeast(moonRadius * 3.6f),
        ),
    )

    // 2. Twinkling stars with diffraction spikes
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

    // 3. Soft lunar corona & atmospheric bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f * pulseScale),
                Color(0xFFE0E7FF).copy(alpha = 0.22f * pulseScale),
                Color(0xFF818CF8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 2.20f * pulseScale,
        ),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.62f * pulseScale),
                Color(0xFFEDE9FE).copy(alpha = 0.30f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 1.35f,
        ),
    )

    // 4. Photorealistic NASA Full Moon disc
    with(moonPainter) {
        translate(moonCenterX - moonRadius, moonCenterY - moonRadius) {
            draw(
                size = Size(moonRadius * 2f, moonRadius * 2f),
                alpha = 1.0f,
            )
        }
    }

    // 5. Spherical limb darkening
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0xFF0F172A).copy(alpha = 0.10f),
                Color(0xFF020617).copy(alpha = 0.36f),
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius,
        ),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY),
    )

    // 6. Photorealistic moonlit wisps & cumulus clouds drifting gracefully across the lower moon
    drawParallaxClouds(
        placements = nightClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFB8C4E6),
        alphaMultiplier = 1.0f,
    )
}

private fun DrawScope.drawCloudyScene(
    overcastClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    // Rich overcast slate-blue sky atmosphere
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A283D).copy(alpha = 0.92f),
                Color(0xFF283A52).copy(alpha = 0.85f),
                Color(0xFF334763).copy(alpha = 0.62f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    val focalX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.72f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2E8F0).copy(alpha = 0.24f),
                Color(0xFF94A3B8).copy(alpha = 0.10f),
                Color.Transparent,
            ),
            center = Offset(focalX, height * 0.36f),
            radius = (width * 0.62f).coerceAtLeast(150.dp.toPx()),
        ),
    )

    drawParallaxClouds(
        placements = overcastClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFE6EDF7),
        alphaMultiplier = 1.0f,
    )
}

private fun DrawScope.drawThunderstormScene(
    raindrops: List<Raindrop>,
    stormClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    rainPhase: Float,
    slowPhase: Float,
    lightningPhase: Float,
    width: Float,
    height: Float,
) {
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

    val boltStartX = (width - 72.dp.toPx()).coerceAtLeast(width * 0.74f)

    // Deep stormy indigo sky brightening internally during lightning sheet discharge
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090E1A).copy(alpha = 0.95f),
                Color(0xFF111B2E).copy(alpha = 0.88f),
                Color(0xFF162138).copy(alpha = 0.70f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    val ambientColor = if (flashIntensity > 0.05f) {
        Color(0xFF93C5FD).copy(alpha = 0.22f + 0.45f * flashIntensity)
    } else {
        Color(0xFF1E1B4B).copy(alpha = 0.25f)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ambientColor, Color.Transparent),
            center = Offset(boltStartX, height * 0.34f),
            radius = (width * 0.65f).coerceAtLeast(150.dp.toPx()),
        ),
    )

    // Photorealistic heavy dark storm clouds illuminated subtly when sheet lightning fires
    val cloudTint = if (flashIntensity > 0.08f) {
        Color(0xFFEEF2FF)
    } else {
        Color(0xFFBCCAE0)
    }
    drawParallaxClouds(
        placements = stormClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = cloudTint,
        alphaMultiplier = 0.96f,
    )

    // Branched lightning bolt during peak discharge
    if (flashIntensity > 0.30f) {
        val startY = height * 0.10f
        val primaryBolt = Path().apply {
            moveTo(boltStartX, startY)
            lineTo(boltStartX - 12f, startY + height * 0.20f)
            lineTo(boltStartX + 8f, startY + height * 0.26f)
            lineTo(boltStartX - 16f, startY + height * 0.50f)
            lineTo(boltStartX - 6f, startY + height * 0.56f)
            lineTo(boltStartX - 22f, startY + height * 0.82f)
        }
        val forkBranch1 = Path().apply {
            moveTo(boltStartX + 8f, startY + height * 0.26f)
            lineTo(boltStartX + 26f, startY + height * 0.42f)
            lineTo(boltStartX + 20f, startY + height * 0.48f)
            lineTo(boltStartX + 34f, startY + height * 0.64f)
        }
        val forkBranch2 = Path().apply {
            moveTo(boltStartX - 16f, startY + height * 0.50f)
            lineTo(boltStartX - 32f, startY + height * 0.66f)
        }

        for (path in listOf(primaryBolt, forkBranch1, forkBranch2)) {
            val isFork = path != primaryBolt
            drawPath(
                path = path,
                color = Color(0xFFA5B4FC).copy(alpha = flashIntensity * (if (isFork) 0.50f else 0.75f)),
                style = Stroke(width = if (isFork) 3.0f else 5.5f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = flashIntensity * (if (isFork) 0.75f else 1.0f)),
                style = Stroke(width = if (isFork) 1.2f else 2.0f, cap = StrokeCap.Round),
            )
        }
    }

    drawMultiDepthRain(
        raindrops = raindrops,
        rainPhase = rainPhase,
        width = width,
        height = height,
        streakSpeedMultiplier = 1.32f,
        windTiltFactor = 0.16f,
        streakCoolColor = Color(0xFFA5B4FC),
        streakBrightColor = Color(0xFFEEF2FF),
        rippleAlphaScale = 1.15f,
    )
}

private fun DrawScope.drawSnowScene(
    snowflakes: List<Snowflake>,
    overcastClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    rainPhase: Float,
    slowPhase: Float,
    width: Float,
    height: Float,
) {
    // Crisp winter sky atmosphere
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF142236).copy(alpha = 0.88f),
                Color(0xFF1E324D).copy(alpha = 0.72f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    drawParallaxClouds(
        placements = overcastClouds,
        sprites = cloudSprites,
        slowPhase = slowPhase,
        width = width,
        height = height,
        tintColor = Color(0xFFE8F1FA),
        alphaMultiplier = 0.65f,
    )

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
