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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.weather.WeatherVisualType
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.aura_cloud_cumulus
import com.music.bitchord.sharedui.resources.aura_cloud_storm
import com.music.bitchord.sharedui.resources.aura_cloud_wispy
import com.music.bitchord.sharedui.resources.aura_moon_photorealistic
import com.music.bitchord.sharedui.resources.aura_sun_day_photorealistic
import com.music.bitchord.sharedui.resources.aura_sun_sunset_photorealistic
import com.music.bitchord.ui.graphics.FADE_CURVE_STOPS
import com.music.bitchord.ui.graphics.smootherStep
import com.music.bitchord.ui.rememberIsForeground
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private enum class RainDepthTier {
    FAR,
    MID,
    NEAR,
}

private enum class CloudSpeedTier {
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
    val startFraction: Float,
    val relY: Float,
    val widthRel: Float,
    val tier: CloudSpeedTier,
    val alpha: Float,
)

private data class CloudSpritePack(
    val cumulus: Painter? = null,
    val storm: Painter? = null,
    val wispy: Painter? = null,
)

private class ScenePhaseStates(
    val rain: State<Float>,
    val slow: State<Float>,
    val cloudFar: State<Float>,
    val cloudMid: State<Float>,
    val cloudNear: State<Float>,
    val pulse: State<Float>,
    val lightning: State<Float>,
)

internal data class VolumetricRayShaft(
    val centerFraction: Float,
    val halfWidthFraction: Float,
    val color: Color,
    val peakAlpha: Float,
)

/**
 * Builds strictly non-decreasing sweep-gradient color stops in [0f..1f] where each crepuscular
 * ray shaft follows a soft bell curve so diverging beams dissolve smoothly without sharp edges.
 */
internal fun buildVolumetricRayStops(
    shafts: List<VolumetricRayShaft>,
): Array<Pair<Float, Color>> {
    val sorted = shafts.sortedBy { it.centerFraction }
    val stops = ArrayList<Pair<Float, Color>>(sorted.size * 7 + 2)
    stops.add(0f to Color.Transparent)
    var cursor = 0.002f
    for (shaft in sorted) {
        val rawStart = (shaft.centerFraction - shaft.halfWidthFraction).coerceIn(cursor, 0.990f)
        val rawEnd = (shaft.centerFraction + shaft.halfWidthFraction).coerceIn(rawStart + 0.006f, 0.998f)
        if (rawEnd <= rawStart + 0.006f) continue
        val center = ((rawStart + rawEnd) * 0.5f).coerceIn(rawStart + 0.002f, rawEnd - 0.002f)
        val halfSpan = (rawEnd - rawStart) * 0.5f
        val c = shaft.color
        val peak = shaft.peakAlpha.coerceIn(0f, 1f)
        stops.add(rawStart to Color.Transparent)
        stops.add((center - halfSpan * 0.68f).coerceAtLeast(rawStart + 0.0005f) to c.copy(alpha = peak * 0.14f))
        stops.add((center - halfSpan * 0.34f).coerceAtLeast(rawStart + 0.0010f) to c.copy(alpha = peak * 0.52f))
        stops.add(center to c.copy(alpha = peak))
        stops.add((center + halfSpan * 0.34f).coerceAtMost(rawEnd - 0.0010f) to c.copy(alpha = peak * 0.52f))
        stops.add((center + halfSpan * 0.68f).coerceAtMost(rawEnd - 0.0005f) to c.copy(alpha = peak * 0.14f))
        stops.add(rawEnd to Color.Transparent)
        cursor = (rawEnd + 0.001f).coerceAtMost(0.992f)
    }
    stops.add(1f to Color.Transparent)
    return stops.toTypedArray()
}

private class WeatherDrawCache {
    var width: Float = -1f
    var height: Float = -1f
    var topInsetPx: Float = -1f
    var lightMode: Boolean = false
    var blendColor: Color = Color.Unspecified
    var vignetteBrush: Brush? = null
    var topFeatherBrush: Brush? = null
    var bottomFeatherBrush: Brush? = null
    var topFadeEnd: Float = 0f
    var bottomFadeStart: Float = 0f
    val primaryBolt = Path()
    val forkBranch1 = Path()
    val forkBranch2 = Path()

    val rayLayerPaint = Paint()
    var rayLayerBounds: Rect = Rect.Zero
    private var rayCacheW: Float = -1f
    private var rayCacheH: Float = -1f
    private var rayCacheSunX: Float = -1f
    private var rayCacheSunY: Float = -1f
    private var rayCacheSunRadius: Float = -1f
    private var rayCacheRadius: Float = -1f
    private var rayCacheHorizonY: Float = -1f
    private var rayCacheIsSunset: Boolean = false
    private var rayCacheLightMode: Boolean = false
    var rayPrimarySweepBrush: Brush? = null
    var raySecondarySweepBrush: Brush? = null
    var rayRadialMaskBrush: Brush? = null
    var rayHorizonMaskBrush: Brush? = null

    fun ensureSunRays(
        w: Float,
        h: Float,
        sunX: Float,
        sunY: Float,
        sunRadius: Float,
        rayRadius: Float,
        horizonBottomY: Float,
        isSunset: Boolean,
        light: Boolean,
    ) {
        if (
            rayCacheW == w &&
            rayCacheH == h &&
            rayCacheSunX == sunX &&
            rayCacheSunY == sunY &&
            rayCacheSunRadius == sunRadius &&
            rayCacheRadius == rayRadius &&
            rayCacheHorizonY == horizonBottomY &&
            rayCacheIsSunset == isSunset &&
            rayCacheLightMode == light &&
            rayPrimarySweepBrush != null
        ) {
            return
        }
        rayCacheW = w
        rayCacheH = h
        rayCacheSunX = sunX
        rayCacheSunY = sunY
        rayCacheSunRadius = sunRadius
        rayCacheRadius = rayRadius
        rayCacheHorizonY = horizonBottomY
        rayCacheIsSunset = isSunset
        rayCacheLightMode = light
        rayLayerBounds = Rect(0f, 0f, w, h)

        val sunCenter = Offset(sunX, sunY)
        val primaryShafts = if (isSunset) {
            listOf(
                VolumetricRayShaft(0.418f, 0.032f, Color(0xFFFDBA74), if (light) 0.14f else 0.16f),
                VolumetricRayShaft(0.506f, 0.056f, Color(0xFFFDE047), if (light) 0.17f else 0.21f),
                VolumetricRayShaft(0.594f, 0.038f, Color(0xFFFB923C), if (light) 0.19f else 0.24f),
                VolumetricRayShaft(0.662f, 0.021f, Color(0xFFFEF08A), if (light) 0.15f else 0.18f),
                VolumetricRayShaft(0.738f, 0.046f, Color(0xFFFB7185), if (light) 0.12f else 0.15f),
                VolumetricRayShaft(0.836f, 0.036f, Color(0xFFE879F9), if (light) 0.07f else 0.09f),
            )
        } else {
            listOf(
                VolumetricRayShaft(0.382f, 0.024f, Color(0xFFFED7AA), if (light) 0.12f else 0.14f),
                VolumetricRayShaft(0.488f, 0.052f, Color(0xFFFDE68A), if (light) 0.17f else 0.20f),
                VolumetricRayShaft(0.586f, 0.036f, Color(0xFFFFF7C2), if (light) 0.19f else 0.24f),
                VolumetricRayShaft(0.654f, 0.020f, Color(0xFFFEF08A), if (light) 0.15f else 0.18f),
                VolumetricRayShaft(0.736f, 0.044f, Color(0xFFFDBA74), if (light) 0.13f else 0.16f),
                VolumetricRayShaft(0.838f, 0.034f, Color(0xFFFDA4AF), if (light) 0.09f else 0.11f),
            )
        }
        val secondaryShafts = if (isSunset) {
            listOf(
                VolumetricRayShaft(0.464f, 0.028f, Color(0xFFF97316), if (light) 0.10f else 0.11f),
                VolumetricRayShaft(0.554f, 0.030f, Color(0xFFFDE68A), if (light) 0.13f else 0.15f),
                VolumetricRayShaft(0.628f, 0.022f, Color(0xFFFED7AA), if (light) 0.15f else 0.17f),
                VolumetricRayShaft(0.700f, 0.032f, Color(0xFFFB7185), if (light) 0.10f else 0.12f),
                VolumetricRayShaft(0.796f, 0.040f, Color(0xFFF43F5E), if (light) 0.08f else 0.09f),
            )
        } else {
            listOf(
                VolumetricRayShaft(0.436f, 0.028f, Color(0xFFFDA4AF), if (light) 0.08f else 0.09f),
                VolumetricRayShaft(0.542f, 0.032f, Color(0xFFFDBA74), if (light) 0.12f else 0.14f),
                VolumetricRayShaft(0.622f, 0.022f, Color(0xFFFFFBEB), if (light) 0.15f else 0.17f),
                VolumetricRayShaft(0.694f, 0.030f, Color(0xFFFDE047), if (light) 0.11f else 0.12f),
                VolumetricRayShaft(0.790f, 0.038f, Color(0xFFFB923C), if (light) 0.08f else 0.10f),
            )
        }

        rayPrimarySweepBrush = Brush.sweepGradient(
            *buildVolumetricRayStops(primaryShafts),
            center = sunCenter,
        )
        raySecondarySweepBrush = Brush.sweepGradient(
            *buildVolumetricRayStops(secondaryShafts),
            center = sunCenter,
        )

        val coronaStart = (sunRadius * 0.55f / rayRadius).coerceIn(0.03f, 0.14f)
        val coronaPeak = (sunRadius * 1.45f / rayRadius).coerceIn(coronaStart + 0.05f, 0.28f)
        rayRadialMaskBrush = Brush.radialGradient(
            0.00f to Color.Black.copy(alpha = 0.08f),
            coronaStart to Color.Black.copy(alpha = 0.48f),
            coronaPeak to Color.Black.copy(alpha = 0.96f),
            0.48f to Color.Black.copy(alpha = 0.62f),
            0.76f to Color.Black.copy(alpha = 0.20f),
            1.00f to Color.Transparent,
            center = sunCenter,
            radius = rayRadius,
        )

        val fadeStartFrac = ((sunY + sunRadius * 0.25f) / h).coerceIn(0.05f, 0.85f)
        val fadeEndFrac = (horizonBottomY / h).coerceIn(fadeStartFrac + 0.08f, 0.99f)
        val fadeMidFrac = ((fadeStartFrac + fadeEndFrac) * 0.5f).coerceIn(fadeStartFrac + 0.02f, fadeEndFrac - 0.02f)
        rayHorizonMaskBrush = Brush.verticalGradient(
            0.00f to Color.Black,
            fadeStartFrac to Color.Black,
            fadeMidFrac to Color.Black.copy(alpha = 0.34f),
            fadeEndFrac to Color.Transparent,
            1.00f to Color.Transparent,
            startY = 0f,
            endY = h,
        )
    }

    fun vignette(w: Float, h: Float, topInset: Float, light: Boolean, blend: Color, endX: Float): Brush {
        ensureGeometry(w, h, topInset, light, blend, endX)
        return vignetteBrush!!
    }

    fun topFeather(w: Float, h: Float, topInset: Float, light: Boolean, blend: Color, endX: Float): Pair<Brush, Float> {
        ensureGeometry(w, h, topInset, light, blend, endX)
        return topFeatherBrush!! to topFadeEnd
    }

    fun bottomFeather(w: Float, h: Float, topInset: Float, light: Boolean, blend: Color, endX: Float): Pair<Brush, Float> {
        ensureGeometry(w, h, topInset, light, blend, endX)
        return bottomFeatherBrush!! to bottomFadeStart
    }

    private fun ensureGeometry(
        w: Float,
        h: Float,
        topInset: Float,
        light: Boolean,
        blend: Color,
        endX: Float,
    ) {
        if (
            width == w &&
            height == h &&
            topInsetPx == topInset &&
            lightMode == light &&
            blendColor == blend &&
            vignetteBrush != null &&
            topFeatherBrush != null &&
            bottomFeatherBrush != null
        ) {
            return
        }
        width = w
        height = h
        topInsetPx = topInset
        lightMode = light
        blendColor = blend

        val contentTop = topInset.coerceIn(0f, h * 0.50f)
        val contentH = (h - contentTop).coerceAtLeast(1f)
        topFadeEnd = (contentTop + contentH * 0.24f).coerceIn(h * 0.22f, h * 0.58f)
        bottomFadeStart = (contentTop + contentH * 0.56f).coerceIn(topFadeEnd + 4f, (h - 2f).coerceAtLeast(topFadeEnd + 4f))

        vignetteBrush = Brush.horizontalGradient(
            colors = if (light) {
                listOf(
                    blend.copy(alpha = 0.22f),
                    blend.copy(alpha = 0.10f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    blend.copy(alpha = 0.34f),
                    blend.copy(alpha = 0.18f),
                    blend.copy(alpha = 0.05f),
                    Color.Transparent,
                )
            },
            startX = 0f,
            endX = endX,
        )

        // DstOut mask: src alpha 1 erases completely, src alpha 0 preserves 100%.
        // Smootherstep gives zero slope/curvature at both ends so zero rectangular seam appears.
        val topStops = Array(FADE_CURVE_STOPS + 1) { i ->
            val t = i / FADE_CURVE_STOPS.toFloat()
            t to Color.Black.copy(alpha = 1f - smootherStep(t))
        }
        topFeatherBrush = Brush.verticalGradient(
            colorStops = topStops,
            startY = 0f,
            endY = topFadeEnd.coerceAtLeast(1f),
        )

        val bottomStops = Array(FADE_CURVE_STOPS + 1) { i ->
            val t = i / FADE_CURVE_STOPS.toFloat()
            t to Color.Black.copy(alpha = smootherStep(t))
        }
        bottomFeatherBrush = Brush.verticalGradient(
            colorStops = bottomStops,
            startY = bottomFadeStart,
            endY = h.coerceAtLeast(bottomFadeStart + 1f),
        )
    }

    val lightningPrimaryOuterStroke = Stroke(width = 5.5f, cap = StrokeCap.Round)
    val lightningPrimaryInnerStroke = Stroke(width = 2.0f, cap = StrokeCap.Round)
    val lightningForkOuterStroke = Stroke(width = 3.0f, cap = StrokeCap.Round)
    val lightningForkInnerStroke = Stroke(width = 1.2f, cap = StrokeCap.Round)

    private var rainSkyH = -1f
    private var rainSkyIsDay = true
    private var rainSkyLight = false
    private var rainSkyBrushCache: Brush? = null

    private var rainAmbX = -1f
    private var rainAmbY = -1f
    private var rainAmbR = -1f
    private var rainAmbLight = false
    private var rainAmbBrushCache: Brush? = null

    private var sunriseSkyH = -1f
    private var sunriseSkyLight = false
    private var sunriseSkyBrushCache: Brush? = null

    private var sunsetSkyH = -1f
    private var sunsetSkyLight = false
    private var sunsetSkyBrushCache: Brush? = null

    fun rainSkyBrush(height: Float, isDay: Boolean, lightMode: Boolean): Brush {
        val cached = rainSkyBrushCache
        if (cached != null && rainSkyH == height && rainSkyIsDay == isDay && rainSkyLight == lightMode) {
            return cached
        }
        rainSkyH = height
        rainSkyIsDay = isDay
        rainSkyLight = lightMode
        val skyColors = when {
            lightMode -> listOf(
                Color(0xFF60A5FA).copy(alpha = 0.26f),
                Color(0xFF93C5FD).copy(alpha = 0.22f),
                Color(0xFFBFDBFE).copy(alpha = 0.14f),
                Color.Transparent,
            )
            !isDay -> listOf(
                Color(0xFF07101E).copy(alpha = 0.78f),
                Color(0xFF0E1B2F).copy(alpha = 0.70f),
                Color(0xFF14243D).copy(alpha = 0.44f),
                Color.Transparent,
            )
            else -> listOf(
                Color(0xFF0E1A2B).copy(alpha = 0.78f),
                Color(0xFF16263B).copy(alpha = 0.70f),
                Color(0xFF1C3047).copy(alpha = 0.46f),
                Color.Transparent,
            )
        }
        return Brush.verticalGradient(colors = skyColors, startY = 0f, endY = height).also {
            rainSkyBrushCache = it
        }
    }

    fun rainAmbientBrush(focalX: Float, focalY: Float, radius: Float, lightMode: Boolean): Brush {
        val cached = rainAmbBrushCache
        if (cached != null && rainAmbX == focalX && rainAmbY == focalY && rainAmbR == radius && rainAmbLight == lightMode) {
            return cached
        }
        rainAmbX = focalX
        rainAmbY = focalY
        rainAmbR = radius
        rainAmbLight = lightMode
        return Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = if (lightMode) 0.18f else 0.14f),
                Color(0xFF0284C7).copy(alpha = 0.05f),
                Color.Transparent,
            ),
            center = Offset(focalX, focalY),
            radius = radius,
        ).also { rainAmbBrushCache = it }
    }

    fun sunriseSkyBrush(height: Float, lightMode: Boolean): Brush {
        val cached = sunriseSkyBrushCache
        if (cached != null && sunriseSkyH == height && sunriseSkyLight == lightMode) return cached
        sunriseSkyH = height
        sunriseSkyLight = lightMode
        return Brush.verticalGradient(
            colors = if (lightMode) {
                listOf(
                    Color(0xFFF472B6).copy(alpha = 0.16f),
                    Color(0xFFFB923C).copy(alpha = 0.20f),
                    Color(0xFFFDE68A).copy(alpha = 0.14f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color(0xFF18112C).copy(alpha = 0.74f),
                    Color(0xFF3E183E).copy(alpha = 0.66f),
                    Color(0xFF832346).copy(alpha = 0.56f),
                    Color(0xFFD9531E).copy(alpha = 0.34f),
                    Color.Transparent,
                )
            },
            startY = 0f,
            endY = height,
        ).also { sunriseSkyBrushCache = it }
    }

    fun sunsetSkyBrush(height: Float, lightMode: Boolean): Brush {
        val cached = sunsetSkyBrushCache
        if (cached != null && sunsetSkyH == height && sunsetSkyLight == lightMode) return cached
        sunsetSkyH = height
        sunsetSkyLight = lightMode
        return Brush.verticalGradient(
            colors = if (lightMode) {
                listOf(
                    Color(0xFFA855F7).copy(alpha = 0.14f),
                    Color(0xFFFB7185).copy(alpha = 0.20f),
                    Color(0xFFFDBA74).copy(alpha = 0.16f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color(0xFF100B20).copy(alpha = 0.76f),
                    Color(0xFF2B0E32).copy(alpha = 0.70f),
                    Color(0xFF6B1438).copy(alpha = 0.58f),
                    Color(0xFFA81D38).copy(alpha = 0.36f),
                    Color.Transparent,
                )
            },
            startY = 0f,
            endY = height,
        ).also { sunsetSkyBrushCache = it }
    }
}

@Composable
private fun rememberScenePhases(
    visualType: WeatherVisualType,
    shouldAnimate: Boolean,
): ScenePhaseStates {
    val staticRain = remember { mutableFloatStateOf(0.5f) }
    val staticSlow = remember { mutableFloatStateOf(0.35f) }
    val staticCloudFar = remember { mutableFloatStateOf(0.15f) }
    val staticCloudMid = remember { mutableFloatStateOf(0.28f) }
    val staticCloudNear = remember { mutableFloatStateOf(0.42f) }
    val staticPulse = remember { mutableFloatStateOf(0.5f) }
    val staticLightning = remember { mutableFloatStateOf(0f) }

    if (!shouldAnimate) {
        return remember(
            staticRain,
            staticSlow,
            staticCloudFar,
            staticCloudMid,
            staticCloudNear,
            staticPulse,
            staticLightning,
        ) {
            ScenePhaseStates(
                rain = staticRain,
                slow = staticSlow,
                cloudFar = staticCloudFar,
                cloudMid = staticCloudMid,
                cloudNear = staticCloudNear,
                pulse = staticPulse,
                lightning = staticLightning,
            )
        }
    }

    val needsRain = visualType == WeatherVisualType.RAIN ||
        visualType == WeatherVisualType.THUNDERSTORM ||
        visualType == WeatherVisualType.SNOW
    val needsPulse = visualType == WeatherVisualType.CLEAR_DAY ||
        visualType == WeatherVisualType.CLEAR_NIGHT ||
        visualType == WeatherVisualType.SUNRISE ||
        visualType == WeatherVisualType.SUNSET
    val needsLightning = visualType == WeatherVisualType.THUNDERSTORM

    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")

    val rainState = if (needsRain) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1250, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rain_phase",
        )
    } else {
        staticRain
    }

    val slowState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slow_phase",
    )

    val cloudFarState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(76000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloud_far_phase",
    )

    val cloudMidState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(52000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloud_mid_phase",
    )

    val cloudNearState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloud_near_phase",
    )

    val pulseState = if (needsPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_phase",
        )
    } else {
        staticPulse
    }

    val lightningState = if (needsLightning) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "lightning_phase",
        )
    } else {
        staticLightning
    }

    return remember(
        rainState,
        slowState,
        cloudFarState,
        cloudMidState,
        cloudNearState,
        pulseState,
        lightningState,
    ) {
        ScenePhaseStates(
            rain = rainState,
            slow = slowState,
            cloudFar = cloudFarState,
            cloudMid = cloudMidState,
            cloudNear = cloudNearState,
            pulse = pulseState,
            lightning = lightningState,
        )
    }
}

/**
 * Living, hardware-accelerated ambient weather canvas.
 * Combines photorealistic astronomical & meteorological sprites with continuous
 * one-way cloud parallax, multi-depth precipitation, and smooth top/bottom
 * edge feathering that melts seamlessly into any theme surface.
 */
@Composable
fun LivingWeatherCanvas(
    visualType: WeatherVisualType,
    modifier: Modifier = Modifier,
    blendColor: Color = Color(0xFF000000),
    reduceAnimation: Boolean = false,
    isDay: Boolean = true,
    lightMode: Boolean = false,
    topInset: Dp = 0.dp,
    active: Boolean = true,
) {
    val effectiveVisualType = when {
        visualType == WeatherVisualType.CLEAR_DAY && !isDay -> WeatherVisualType.CLEAR_NIGHT
        visualType == WeatherVisualType.CLEAR_NIGHT && isDay -> WeatherVisualType.CLEAR_DAY
        else -> visualType
    }

    val isForeground = rememberIsForeground()
    val shouldAnimate = !reduceAnimation && active && isForeground

    // Animation States are read strictly inside Canvas DrawScope so 0 recompositions happen per frame.
    val phases = rememberScenePhases(effectiveVisualType, shouldAnimate)
    val drawCache = remember { WeatherDrawCache() }

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
                relY = 0.08f + rng.nextFloat() * 0.72f,
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

    // Load only the astronomical & cloud textures required for the active weather scene.
    val moonPainter = if (effectiveVisualType == WeatherVisualType.CLEAR_NIGHT) {
        painterResource(Res.drawable.aura_moon_photorealistic)
    } else {
        null
    }
    val sunDayPainter = if (effectiveVisualType == WeatherVisualType.CLEAR_DAY) {
        painterResource(Res.drawable.aura_sun_day_photorealistic)
    } else {
        null
    }
    val sunSunsetPainter = if (
        effectiveVisualType == WeatherVisualType.SUNRISE ||
        effectiveVisualType == WeatherVisualType.SUNSET
    ) {
        painterResource(Res.drawable.aura_sun_sunset_photorealistic)
    } else {
        null
    }
    val needsCumulus = effectiveVisualType != WeatherVisualType.RAIN &&
        effectiveVisualType != WeatherVisualType.THUNDERSTORM
    val needsStorm = effectiveVisualType == WeatherVisualType.RAIN ||
        effectiveVisualType == WeatherVisualType.THUNDERSTORM
    val cumulusPainter = if (needsCumulus) painterResource(Res.drawable.aura_cloud_cumulus) else null
    val stormPainter = if (needsStorm) painterResource(Res.drawable.aura_cloud_storm) else null
    val wispyPainter = painterResource(Res.drawable.aura_cloud_wispy)
    val cloudSprites = remember(cumulusPainter, stormPainter, wispyPainter) {
        CloudSpritePack(
            cumulus = cumulusPainter,
            storm = stormPainter,
            wispy = wispyPainter,
        )
    }

    // Staggered formations across [0..1) travel span so sky always has depth as clouds wrap smoothly.
    val dayClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.16f, relY = 0.24f, widthRel = 0.52f, tier = CloudSpeedTier.FAR, alpha = 0.46f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, startFraction = 0.62f, relY = 0.52f, widthRel = 0.44f, tier = CloudSpeedTier.NEAR, alpha = 0.76f),
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.84f, relY = 0.64f, widthRel = 0.48f, tier = CloudSpeedTier.MID, alpha = 0.40f),
        )
    }

    val nightClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.20f, relY = 0.26f, widthRel = 0.54f, tier = CloudSpeedTier.FAR, alpha = 0.34f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, startFraction = 0.64f, relY = 0.60f, widthRel = 0.46f, tier = CloudSpeedTier.NEAR, alpha = 0.36f),
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.88f, relY = 0.70f, widthRel = 0.50f, tier = CloudSpeedTier.MID, alpha = 0.28f),
        )
    }

    val overcastClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.10f, relY = 0.22f, widthRel = 0.62f, tier = CloudSpeedTier.FAR, alpha = 0.52f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, startFraction = 0.44f, relY = 0.36f, widthRel = 0.56f, tier = CloudSpeedTier.NEAR, alpha = 0.84f),
            CloudSpritePlacement(CloudTextureKind.CUMULUS, startFraction = 0.74f, relY = 0.50f, widthRel = 0.52f, tier = CloudSpeedTier.MID, alpha = 0.78f),
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.92f, relY = 0.60f, widthRel = 0.60f, tier = CloudSpeedTier.FAR, alpha = 0.42f),
        )
    }

    val stormClouds = remember {
        listOf(
            CloudSpritePlacement(CloudTextureKind.WISPY, startFraction = 0.12f, relY = 0.18f, widthRel = 0.65f, tier = CloudSpeedTier.FAR, alpha = 0.30f),
            CloudSpritePlacement(CloudTextureKind.STORM, startFraction = 0.42f, relY = 0.28f, widthRel = 0.62f, tier = CloudSpeedTier.NEAR, alpha = 0.90f),
            CloudSpritePlacement(CloudTextureKind.STORM, startFraction = 0.76f, relY = 0.34f, widthRel = 0.58f, tier = CloudSpeedTier.MID, alpha = 0.92f),
            CloudSpritePlacement(CloudTextureKind.STORM, startFraction = 0.28f, relY = 0.42f, widthRel = 0.50f, tier = CloudSpeedTier.MID, alpha = 0.76f),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clipToBounds(),
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val topInsetPx = topInset.toPx().coerceAtLeast(0f)
        val contentTop = topInsetPx.coerceAtMost(height * 0.50f)
        val contentHeight = (height - contentTop).coerceAtLeast(1f)

        val rainPhase = phases.rain.value
        val slowPhase = phases.slow.value
        val cloudFarPhase = phases.cloudFar.value
        val cloudMidPhase = phases.cloudMid.value
        val cloudNearPhase = phases.cloudNear.value
        val pulsePhase = phases.pulse.value
        val lightningPhase = phases.lightning.value

        when (effectiveVisualType) {
            WeatherVisualType.RAIN -> {
                drawRainScene(
                    raindrops = raindrops,
                    stormClouds = stormClouds,
                    cloudSprites = cloudSprites,
                    rainPhase = rainPhase,
                    cloudFarPhase = cloudFarPhase,
                    cloudMidPhase = cloudMidPhase,
                    cloudNearPhase = cloudNearPhase,
                    width = width,
                    height = height,
                    contentTop = contentTop,
                    contentHeight = contentHeight,
                    drawCache = drawCache,
                    isDay = isDay,
                    lightMode = lightMode,
                )
            }
            WeatherVisualType.CLEAR_DAY -> {
                sunDayPainter?.let {
                    drawSunnyScene(
                        sunDayPainter = it,
                        dayClouds = dayClouds,
                        cloudSprites = cloudSprites,
                        pulsePhase = pulsePhase,
                        cloudFarPhase = cloudFarPhase,
                        cloudMidPhase = cloudMidPhase,
                        cloudNearPhase = cloudNearPhase,
                        width = width,
                        height = height,
                        contentTop = contentTop,
                        contentHeight = contentHeight,
                        lightMode = lightMode,
                    )
                }
            }
            WeatherVisualType.CLEAR_NIGHT -> {
                moonPainter?.let {
                    drawClearNightScene(
                        stars = stars,
                        nightClouds = nightClouds,
                        cloudSprites = cloudSprites,
                        pulsePhase = pulsePhase,
                        slowPhase = slowPhase,
                        cloudFarPhase = cloudFarPhase,
                        cloudMidPhase = cloudMidPhase,
                        cloudNearPhase = cloudNearPhase,
                        width = width,
                        height = height,
                        contentTop = contentTop,
                        contentHeight = contentHeight,
                        moonPainter = it,
                        lightMode = lightMode,
                    )
                }
            }
            WeatherVisualType.SUNRISE -> {
                sunSunsetPainter?.let {
                    drawSunriseScene(
                        stars = stars,
                        sunSunsetPainter = it,
                        dayClouds = dayClouds,
                        cloudSprites = cloudSprites,
                        pulsePhase = pulsePhase,
                        slowPhase = slowPhase,
                        cloudFarPhase = cloudFarPhase,
                        cloudMidPhase = cloudMidPhase,
                        cloudNearPhase = cloudNearPhase,
                        width = width,
                        height = height,
                        contentTop = contentTop,
                        contentHeight = contentHeight,
                        drawCache = drawCache,
                        lightMode = lightMode,
                    )
                }
            }
            WeatherVisualType.SUNSET -> {
                sunSunsetPainter?.let {
                    drawSunsetScene(
                        sunSunsetPainter = it,
                        dayClouds = dayClouds,
                        cloudSprites = cloudSprites,
                        pulsePhase = pulsePhase,
                        slowPhase = slowPhase,
                        cloudFarPhase = cloudFarPhase,
                        cloudMidPhase = cloudMidPhase,
                        cloudNearPhase = cloudNearPhase,
                        width = width,
                        height = height,
                        contentTop = contentTop,
                        contentHeight = contentHeight,
                        drawCache = drawCache,
                        lightMode = lightMode,
                    )
                }
            }
            WeatherVisualType.CLOUDY -> {
                drawCloudyScene(
                    overcastClouds = overcastClouds,
                    cloudSprites = cloudSprites,
                    cloudFarPhase = cloudFarPhase,
                    cloudMidPhase = cloudMidPhase,
                    cloudNearPhase = cloudNearPhase,
                    width = width,
                    height = height,
                    contentTop = contentTop,
                    contentHeight = contentHeight,
                    isDay = isDay,
                    lightMode = lightMode,
                )
            }
            WeatherVisualType.THUNDERSTORM -> {
                drawThunderstormScene(
                    raindrops = raindrops,
                    stormClouds = stormClouds,
                    cloudSprites = cloudSprites,
                    rainPhase = rainPhase,
                    cloudFarPhase = cloudFarPhase,
                    cloudMidPhase = cloudMidPhase,
                    cloudNearPhase = cloudNearPhase,
                    lightningPhase = lightningPhase,
                    width = width,
                    height = height,
                    contentTop = contentTop,
                    contentHeight = contentHeight,
                    drawCache = drawCache,
                    lightMode = lightMode,
                )
            }
            WeatherVisualType.SNOW -> {
                drawSnowScene(
                    snowflakes = snowflakes,
                    overcastClouds = overcastClouds,
                    cloudSprites = cloudSprites,
                    rainPhase = rainPhase,
                    cloudFarPhase = cloudFarPhase,
                    cloudMidPhase = cloudMidPhase,
                    cloudNearPhase = cloudNearPhase,
                    width = width,
                    height = height,
                    contentTop = contentTop,
                    contentHeight = contentHeight,
                    isDay = isDay,
                    lightMode = lightMode,
                )
            }
        }

        val endX = (width * 0.64f).coerceAtLeast(180.dp.toPx())
        // Subtle left legibility scrim keeps clock & greeting crisp without flattening the sky
        drawRect(
            brush = drawCache.vignette(width, height, topInsetPx, lightMode, blendColor, endX),
            size = Size(width, height),
        )

        // True alpha feathering at both top (small opacity near top bar) and bottom (seamless fade-out)
        val (topBrush, topEnd) = drawCache.topFeather(width, height, topInsetPx, lightMode, blendColor, endX)
        drawRect(
            brush = topBrush,
            topLeft = Offset.Zero,
            size = Size(width, topEnd),
            blendMode = BlendMode.DstOut,
        )

        val (bottomBrush, bottomStart) = drawCache.bottomFeather(width, height, topInsetPx, lightMode, blendColor, endX)
        if (bottomStart < height) {
            drawRect(
                brush = bottomBrush,
                topLeft = Offset(0f, bottomStart),
                size = Size(width, height - bottomStart),
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

private fun DrawScope.drawParallaxClouds(
    placements: List<CloudSpritePlacement>,
    sprites: CloudSpritePack,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    contentTop: Float,
    contentHeight: Float,
    tintColor: Color,
    alphaMultiplier: Float = 1.0f,
    includeFarAndMid: Boolean = true,
    includeNear: Boolean = true,
) {
    val filter = if (tintColor == Color.White) {
        null
    } else {
        ColorFilter.tint(tintColor, BlendMode.Modulate)
    }
    for (p in placements) {
        if (p.tier == CloudSpeedTier.NEAR && !includeNear) continue
        if (p.tier != CloudSpeedTier.NEAR && !includeFarAndMid) continue
        val effectiveAlpha = (p.alpha * alphaMultiplier).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0.01f) continue

        val painter = when (p.kind) {
            CloudTextureKind.CUMULUS -> sprites.cumulus
            CloudTextureKind.STORM -> sprites.storm
            CloudTextureKind.WISPY -> sprites.wispy
        } ?: continue
        val intrinsic = painter.intrinsicSize
        val aspect = if (intrinsic.width > 0f && intrinsic.height > 0f) {
            intrinsic.height / intrinsic.width
        } else {
            0.44f
        }

        val phase = when (p.tier) {
            CloudSpeedTier.FAR -> cloudFarPhase
            CloudSpeedTier.MID -> cloudMidPhase
            CloudSpeedTier.NEAR -> cloudNearPhase
        }

        val drawW = (width * p.widthRel).coerceAtLeast(140.dp.toPx())
        val drawH = drawW * aspect
        // Continuous one-way drift left -> right with offscreen wrap (no pendulum back-and-forth)
        val travelSpan = width + drawW * 1.24f
        val progress = (p.startFraction + phase) % 1f
        val left = -drawW * 1.12f + progress * travelSpan
        val top = contentTop + (p.relY * contentHeight) - (drawH * 0.5f)
        val edgeEnvelope = (minOf(progress, 1f - progress) / 0.12f).coerceIn(0f, 1f)
        val drawAlpha = (effectiveAlpha * smootherStep(edgeEnvelope)).coerceIn(0f, 1f)
        if (drawAlpha <= 0.01f) continue

        with(painter) {
            translate(left = left, top = top) {
                draw(
                    size = Size(drawW, drawH),
                    alpha = drawAlpha,
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
    isHorizonSun: Boolean = false,
    horizonMistColor: Color = Color.Transparent,
) {
    val sunCenter = Offset(sunCenterX, sunCenterY)

    // 1. Wide multi-stop atmospheric Mie corona bloom
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to coronaInnerColor.copy(alpha = 0.50f * pulseScale),
            0.42f to coronaMidColor.copy(alpha = 0.28f * pulseScale),
            0.74f to coronaOuterColor.copy(alpha = 0.11f),
            1.00f to Color.Transparent,
            center = sunCenter,
            radius = sunRadius * 2.65f * pulseScale,
        ),
        radius = sunRadius * 2.65f * pulseScale,
        center = sunCenter,
    )

    // 2. Soft diffused optical atmospheric scatter (proportionally scaled circle avoids sharp oval cutoffs)
    val flareBaseRadius = sunRadius * 1.45f
    withTransform({
        scale(scaleX = 2.15f, scaleY = 0.58f, pivot = sunCenter)
    }) {
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to Color.White.copy(alpha = 0.22f * pulseScale),
                0.38f to flareTintColor.copy(alpha = 0.11f * pulseScale),
                0.76f to flareTintColor.copy(alpha = 0.03f),
                1.00f to Color.Transparent,
                center = sunCenter,
                radius = flareBaseRadius,
            ),
            radius = flareBaseRadius,
            center = sunCenter,
        )
    }

    // 3. Photorealistic NASA SDO Solar Disc Sprite
    val spriteSize = sunRadius * 2.20f
    with(sunPainter) {
        translate(left = sunCenterX - spriteSize * 0.5f, top = sunCenterY - spriteSize * 0.5f) {
            draw(
                size = Size(spriteSize, spriteSize),
                alpha = if (isHorizonSun) 0.94f else 1.0f,
            )
        }
    }

    // 4. Soft incandescent core & atmospheric horizon veil so the sun sits inside Earth's airglow
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to Color.White.copy(alpha = 0.28f),
            0.62f to limbOverlayColor.copy(alpha = 0.12f),
            1.00f to Color.Transparent,
            center = Offset(sunCenterX - sunRadius * 0.12f, sunCenterY - sunRadius * 0.12f),
            radius = sunRadius * 1.12f,
        ),
        radius = sunRadius * 1.12f,
        center = sunCenter,
    )

    if (isHorizonSun && horizonMistColor != Color.Transparent) {
        drawCircle(
            brush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.56f to Color.Transparent,
                0.84f to horizonMistColor.copy(alpha = 0.22f),
                1.00f to horizonMistColor.copy(alpha = 0.34f),
                startY = sunCenterY - sunRadius * 1.10f,
                endY = sunCenterY + sunRadius * 1.15f,
            ),
            radius = sunRadius * 1.14f,
            center = sunCenter,
        )
    }
}

private fun DrawScope.drawMultiDepthRain(
    raindrops: List<Raindrop>,
    rainPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    streakSpeedMultiplier: Float,
    windTiltFactor: Float,
    streakCoolColor: Color,
    streakBrightColor: Color,
    rippleAlphaScale: Float = 1.0f,
) {
    // Soft humid rain mist in lower sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                streakCoolColor.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            startY = contentTop + contentHeight * 0.42f,
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

        // Expanding ripples placed slightly above the bottom feather so droplets dissolve gently
        if (drop.tier == RainDepthTier.MID && progress in 0.72f..0.88f) {
            val splashProgress = ((progress - 0.72f) / 0.16f).coerceIn(0f, 1f)
            val rippleRadius = (4f + splashProgress * 11f)
            val rippleAlpha = (1f - splashProgress) * 0.42f * drop.alpha * rippleAlphaScale
            val groundY = contentTop + contentHeight * (0.76f + drop.splashPhaseBias * 0.08f)

            if (rippleAlpha > 0.02f) {
                drawOval(
                    color = streakBrightColor.copy(alpha = rippleAlpha),
                    topLeft = Offset(endX - rippleRadius, groundY - rippleRadius * 0.22f),
                    size = Size(rippleRadius * 2f, rippleRadius * 0.44f),
                    style = Stroke(width = 1.0f),
                )

                val arcRise = sin(splashProgress * PI.toFloat()) * (5f + drop.splashPhaseBias * 5f)
                val spreadX = splashProgress * (4.5f + drop.splashPhaseBias * 4.5f)
                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha * 1.10f),
                    radius = 1.15f,
                    center = Offset(endX - spreadX, groundY - arcRise),
                )
                drawCircle(
                    color = streakBrightColor.copy(alpha = rippleAlpha),
                    radius = 0.95f,
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
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    drawCache: WeatherDrawCache,
    isDay: Boolean,
    lightMode: Boolean,
) {
    drawRect(
        brush = drawCache.rainSkyBrush(height, isDay, lightMode),
        size = Size(width, height),
    )

    val focalX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.72f)
    drawCircle(
        brush = drawCache.rainAmbientBrush(
            focalX = focalX,
            focalY = contentTop + contentHeight * 0.28f,
            radius = (width * 0.56f).coerceAtLeast(140.dp.toPx()),
            lightMode = lightMode,
        ),
    )

    val cloudTint = when {
        lightMode -> Color(0xFFCBD5E1)
        !isDay -> Color(0xFF8FA4C4)
        else -> Color(0xFFD5E2F2)
    }
    drawParallaxClouds(
        placements = stormClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = cloudTint,
        alphaMultiplier = if (lightMode) 0.78f else if (!isDay) 0.76f else 0.88f,
    )

    drawMultiDepthRain(
        raindrops = raindrops,
        rainPhase = rainPhase,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        streakSpeedMultiplier = 1.05f,
        windTiltFactor = 0.12f,
        streakCoolColor = if (lightMode) Color(0xFF0284C7) else Color(0xFF7DD3FC),
        streakBrightColor = if (lightMode) Color(0xFF0369A1) else Color(0xFFE0F2FE),
    )
}

private fun DrawScope.drawSunnyScene(
    sunDayPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    lightMode: Boolean,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = contentTop + contentHeight * 0.44f
    val sunRadius = (contentHeight * 0.28f).coerceIn(48.dp.toPx(), 60.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    drawRect(
        brush = Brush.verticalGradient(
            colors = if (lightMode) {
                listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.24f),
                    Color(0xFF7DD3FC).copy(alpha = 0.18f),
                    Color(0xFFBAE6FD).copy(alpha = 0.10f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color(0xFF0C2D59).copy(alpha = 0.76f),
                    Color(0xFF15457A).copy(alpha = 0.66f),
                    Color(0xFF1B5791).copy(alpha = 0.40f),
                    Color.Transparent,
                )
            },
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB703).copy(alpha = (if (lightMode) 0.22f else 0.26f) * pulseScale),
                Color(0xFF38BDF8).copy(alpha = 0.12f),
                Color.Transparent,
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = (width * 0.62f).coerceAtLeast(sunRadius * 4.2f),
        ),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.15f * pulseScale),
                Color(0xFFFDE047).copy(alpha = 0.08f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(sunCenterX - sunRadius * 1.65f, sunCenterY + sunRadius * 0.72f),
            radius = sunRadius * 0.42f,
        ),
    )

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

    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = if (lightMode) Color(0xFFFFFFFF) else Color(0xFFFFFBF2),
        alphaMultiplier = if (lightMode) 0.85f else 0.90f,
    )
}

private fun DrawScope.drawHorizonScatterGlow(
    center: Offset,
    radius: Float,
    coreColor: Color,
    midColor: Color,
    outerColor: Color,
    alphaScale: Float,
) {
    withTransform({
        scale(scaleX = 1.52f, scaleY = 0.58f, pivot = center)
    }) {
        drawCircle(
            brush = Brush.radialGradient(
                0.00f to coreColor.copy(alpha = (0.34f * alphaScale).coerceIn(0f, 1f)),
                0.36f to midColor.copy(alpha = (0.20f * alphaScale).coerceIn(0f, 1f)),
                0.70f to outerColor.copy(alpha = (0.08f * alphaScale).coerceIn(0f, 1f)),
                1.00f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private fun DrawScope.drawVolumetricSunRays(
    drawCache: WeatherDrawCache,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    sunCenterX: Float,
    sunCenterY: Float,
    sunRadius: Float,
    pulseScale: Float,
    slowPhase: Float,
    isSunset: Boolean,
    lightMode: Boolean,
    layerAlpha: Float,
    primaryPass: Boolean,
) {
    val rayRadius = max(width * 0.90f, contentHeight * 1.48f).coerceAtLeast(sunRadius * 4.6f)
    val horizonBottomY = (contentTop + contentHeight * 0.96f).coerceAtLeast(sunCenterY + sunRadius * 1.15f)
    drawCache.ensureSunRays(
        w = width,
        h = height,
        sunX = sunCenterX,
        sunY = sunCenterY,
        sunRadius = sunRadius,
        rayRadius = rayRadius,
        horizonBottomY = horizonBottomY,
        isSunset = isSunset,
        light = lightMode,
    )
    val primaryBrush = drawCache.rayPrimarySweepBrush ?: return
    val secondaryBrush = drawCache.raySecondarySweepBrush ?: return
    val radialMaskBrush = drawCache.rayRadialMaskBrush ?: return
    val horizonMaskBrush = drawCache.rayHorizonMaskBrush ?: return

    drawCache.rayLayerPaint.alpha = layerAlpha.coerceIn(0f, 1f)
    drawCache.rayLayerPaint.blendMode = if (lightMode) BlendMode.SrcOver else BlendMode.Screen

    val sunCenter = Offset(sunCenterX, sunCenterY)
    val angleRad = (slowPhase * 2f * PI).toFloat()
    val primaryDriftDeg = sin(angleRad) * 2.2f
    val secondaryDriftDeg = -sin(angleRad + 1.05f) * 3.1f
    val secondaryAlpha = ((0.72f + 0.14f * (pulseScale - 0.94f) / 0.12f)).coerceIn(0.45f, 0.95f)

    drawIntoCanvas { canvas ->
        canvas.saveLayer(drawCache.rayLayerBounds, drawCache.rayLayerPaint)
        try {
            if (primaryPass) {
                withTransform({
                    rotate(degrees = primaryDriftDeg, pivot = sunCenter)
                }) {
                    drawCircle(
                        brush = primaryBrush,
                        radius = rayRadius,
                        center = sunCenter,
                    )
                }
                withTransform({
                    rotate(degrees = secondaryDriftDeg, pivot = sunCenter)
                }) {
                    drawCircle(
                        brush = secondaryBrush,
                        radius = rayRadius,
                        center = sunCenter,
                        alpha = secondaryAlpha,
                        blendMode = BlendMode.Plus,
                    )
                }
            } else {
                withTransform({
                    rotate(degrees = secondaryDriftDeg, pivot = sunCenter)
                }) {
                    drawCircle(
                        brush = secondaryBrush,
                        radius = rayRadius,
                        center = sunCenter,
                        alpha = secondaryAlpha,
                    )
                }
            }

            // Soften beam roots past the solar corona and fade radially before the viewport edges
            drawCircle(
                brush = radialMaskBrush,
                radius = rayRadius,
                center = sunCenter,
                blendMode = BlendMode.DstIn,
            )

            // Ground beams along the horizon so light fans upward across clouds instead of pointing downward
            drawRect(
                brush = horizonMaskBrush,
                topLeft = Offset.Zero,
                size = Size(width, height),
                blendMode = BlendMode.DstIn,
            )
        } finally {
            canvas.restore()
        }
    }
}

private fun DrawScope.drawSunriseScene(
    stars: List<Star>,
    sunSunsetPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    drawCache: WeatherDrawCache,
    lightMode: Boolean,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = contentTop + contentHeight * 0.50f
    val sunRadius = (contentHeight * 0.27f).coerceIn(48.dp.toPx(), 58.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    drawRect(
        brush = drawCache.sunriseSkyBrush(height, lightMode),
        size = Size(width, height),
    )

    for (star in stars) {
        if (star.relX < 0.65f && star.relY < 0.45f) {
            // 4f * PI completes two full cycles cleanly per 0..1 loop so stars never jump
            val twinkle = 0.20f + 0.50f * (0.5f + 0.5f * sin((slowPhase * 4f * PI + star.phase).toFloat()))
            val starColor = if (lightMode) Color(0xFFB45309) else Color(0xFFFFEDD5)
            drawCircle(
                color = starColor.copy(alpha = (twinkle * 0.40f).coerceIn(0f, 1f)),
                radius = star.radius * 0.8f,
                center = Offset(star.relX * width, contentTop + star.relY * contentHeight),
            )
        }
    }

    drawHorizonScatterGlow(
        center = Offset(sunCenterX, sunCenterY + sunRadius * 0.22f),
        radius = (width * 0.50f).coerceAtLeast(sunRadius * 3.5f),
        coreColor = Color(0xFFFFF7C2),
        midColor = Color(0xFFFB923C),
        outerColor = Color(0xFFF43F5E),
        alphaScale = (if (lightMode) 0.65f else 0.92f) * pulseScale,
    )

    drawVolumetricSunRays(
        drawCache = drawCache,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        slowPhase = slowPhase,
        isSunset = false,
        lightMode = lightMode,
        layerAlpha = (if (lightMode) 0.72f else 0.92f) * (0.90f + 0.10f * pulsePhase),
        primaryPass = true,
    )

    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = Color(0xFFFFD6A5),
        alphaMultiplier = if (lightMode) 0.76f else 0.86f,
        includeFarAndMid = true,
        includeNear = false,
    )

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
        isHorizonSun = true,
        horizonMistColor = if (lightMode) Color(0xFFFDE68A) else Color(0xFFD9531E),
    )

    drawVolumetricSunRays(
        drawCache = drawCache,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        slowPhase = slowPhase,
        isSunset = false,
        lightMode = lightMode,
        layerAlpha = if (lightMode) 0.24f else 0.34f,
        primaryPass = false,
    )

    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = Color(0xFFFFD6A5),
        alphaMultiplier = if (lightMode) 0.78f else 0.88f,
        includeFarAndMid = false,
        includeNear = true,
    )
}

private fun DrawScope.drawSunsetScene(
    sunSunsetPainter: Painter,
    dayClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    drawCache: WeatherDrawCache,
    lightMode: Boolean,
) {
    val sunCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val sunCenterY = contentTop + contentHeight * 0.48f
    val sunRadius = (contentHeight * 0.28f).coerceIn(48.dp.toPx(), 60.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    drawRect(
        brush = drawCache.sunsetSkyBrush(height, lightMode),
        size = Size(width, height),
    )

    drawHorizonScatterGlow(
        center = Offset(sunCenterX, sunCenterY + sunRadius * 0.20f),
        radius = (width * 0.54f).coerceAtLeast(sunRadius * 3.8f),
        coreColor = Color(0xFFFFB703),
        midColor = Color(0xFFF97316),
        outerColor = Color(0xFFBE123C),
        alphaScale = (if (lightMode) 0.70f else 1.0f) * pulseScale,
    )

    drawVolumetricSunRays(
        drawCache = drawCache,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        slowPhase = slowPhase,
        isSunset = true,
        lightMode = lightMode,
        layerAlpha = (if (lightMode) 0.70f else 0.94f) * (0.90f + 0.10f * pulsePhase),
        primaryPass = true,
    )

    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = Color(0xFFFFA585),
        alphaMultiplier = if (lightMode) 0.74f else 0.84f,
        includeFarAndMid = true,
        includeNear = false,
    )

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
        isHorizonSun = true,
        horizonMistColor = if (lightMode) Color(0xFFFDBA74) else Color(0xFFA81D38),
    )

    drawVolumetricSunRays(
        drawCache = drawCache,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        sunCenterX = sunCenterX,
        sunCenterY = sunCenterY,
        sunRadius = sunRadius,
        pulseScale = pulseScale,
        slowPhase = slowPhase,
        isSunset = true,
        lightMode = lightMode,
        layerAlpha = if (lightMode) 0.24f else 0.36f,
        primaryPass = false,
    )

    drawParallaxClouds(
        placements = dayClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = Color(0xFFFFA585),
        alphaMultiplier = if (lightMode) 0.76f else 0.86f,
        includeFarAndMid = false,
        includeNear = true,
    )
}

private fun DrawScope.drawClearNightScene(
    stars: List<Star>,
    nightClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    pulsePhase: Float,
    slowPhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    moonPainter: Painter,
    lightMode: Boolean,
) {
    val moonCenterX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.74f)
    val moonCenterY = contentTop + contentHeight * 0.45f
    val moonRadius = (contentHeight * 0.29f).coerceIn(48.dp.toPx(), 60.dp.toPx())
    val pulseScale = 0.94f + 0.12f * pulsePhase

    if (lightMode) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = 0.18f),
                    Color(0xFF818CF8).copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = height,
            ),
            size = Size(width, height),
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4338CA).copy(alpha = (if (lightMode) 0.18f else 0.24f) * pulseScale),
                Color(0xFF1E1B4B).copy(alpha = 0.10f * pulseScale),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = (width * 0.62f).coerceAtLeast(moonRadius * 3.4f),
        ),
    )

    for (star in stars) {
        val starX = star.relX * width
        val starY = contentTop + star.relY * contentHeight
        val twinkle = 0.25f + 0.70f * (0.5f + 0.5f * sin((slowPhase * 4f * PI + star.phase).toFloat()))
        val starAlpha = twinkle.coerceIn(0f, 1f)
        val spikeTint = if (lightMode) Color(0xFF4F46E5) else Color(0xFFE0E7FF)
        val dotTint = if (lightMode) Color(0xFF6366F1) else Color(0xFFEDE9FE)

        if (star.isDiamond) {
            val armLength = star.radius * 2.8f
            drawLine(
                color = spikeTint.copy(alpha = starAlpha),
                start = Offset(starX - armLength, starY),
                end = Offset(starX + armLength, starY),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = spikeTint.copy(alpha = starAlpha),
                start = Offset(starX, starY - armLength),
                end = Offset(starX, starY + armLength),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = (if (lightMode) Color(0xFF4338CA) else Color.White).copy(alpha = starAlpha),
                radius = star.radius * 0.9f,
                center = Offset(starX, starY),
            )
        } else {
            drawCircle(
                color = dotTint.copy(alpha = starAlpha),
                radius = star.radius,
                center = Offset(starX, starY),
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.36f * pulseScale),
                Color(0xFFE0E7FF).copy(alpha = 0.20f * pulseScale),
                Color(0xFF818CF8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 2.10f * pulseScale,
        ),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.58f * pulseScale),
                Color(0xFFEDE9FE).copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius * 1.32f,
        ),
    )

    with(moonPainter) {
        translate(moonCenterX - moonRadius, moonCenterY - moonRadius) {
            draw(
                size = Size(moonRadius * 2f, moonRadius * 2f),
                alpha = 1.0f,
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0xFF0F172A).copy(alpha = 0.10f),
                Color(0xFF020617).copy(alpha = 0.34f),
            ),
            center = Offset(moonCenterX, moonCenterY),
            radius = moonRadius,
        ),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY),
    )

    drawParallaxClouds(
        placements = nightClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = if (lightMode) Color(0xFF94A3B8) else Color(0xFFB8C4E6),
        alphaMultiplier = if (lightMode) 0.72f else 0.96f,
    )
}

private fun DrawScope.drawCloudyScene(
    overcastClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    isDay: Boolean,
    lightMode: Boolean,
) {
    val skyColors = when {
        lightMode -> listOf(
            Color(0xFF94A3B8).copy(alpha = 0.24f),
            Color(0xFFCBD5E1).copy(alpha = 0.18f),
            Color.Transparent,
        )
        !isDay -> listOf(
            Color(0xFF0B1322).copy(alpha = 0.78f),
            Color(0xFF132036).copy(alpha = 0.68f),
            Color(0xFF1B2B44).copy(alpha = 0.42f),
            Color.Transparent,
        )
        else -> listOf(
            Color(0xFF1A283D).copy(alpha = 0.76f),
            Color(0xFF283A52).copy(alpha = 0.66f),
            Color(0xFF334763).copy(alpha = 0.42f),
            Color.Transparent,
        )
    }
    drawRect(
        brush = Brush.verticalGradient(colors = skyColors, startY = 0f, endY = height),
        size = Size(width, height),
    )

    val focalX = (width - 74.dp.toPx()).coerceAtLeast(width * 0.72f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2E8F0).copy(alpha = if (!isDay) 0.14f else 0.22f),
                Color(0xFF94A3B8).copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(focalX, contentTop + contentHeight * 0.36f),
            radius = (width * 0.60f).coerceAtLeast(145.dp.toPx()),
        ),
    )

    val cloudTint = when {
        lightMode -> Color(0xFFE2E8F0)
        !isDay -> Color(0xFF94A6C9)
        else -> Color(0xFFE6EDF7)
    }
    drawParallaxClouds(
        placements = overcastClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = cloudTint,
        alphaMultiplier = if (!isDay && !lightMode) 0.72f else 0.94f,
    )
}

private fun DrawScope.drawThunderstormScene(
    raindrops: List<Raindrop>,
    stormClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    rainPhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    lightningPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    drawCache: WeatherDrawCache,
    lightMode: Boolean,
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

    drawRect(
        brush = Brush.verticalGradient(
            colors = if (lightMode) {
                listOf(
                    Color(0xFF6366F1).copy(alpha = 0.22f),
                    Color(0xFF818CF8).copy(alpha = 0.16f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color(0xFF090E1A).copy(alpha = 0.80f),
                    Color(0xFF111B2E).copy(alpha = 0.72f),
                    Color(0xFF162138).copy(alpha = 0.48f),
                    Color.Transparent,
                )
            },
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    val ambientColor = if (flashIntensity > 0.05f) {
        Color(0xFF93C5FD).copy(alpha = 0.20f + 0.42f * flashIntensity)
    } else {
        Color(0xFF1E1B4B).copy(alpha = if (lightMode) 0.14f else 0.22f)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ambientColor, Color.Transparent),
            center = Offset(boltStartX, contentTop + contentHeight * 0.34f),
            radius = (width * 0.62f).coerceAtLeast(145.dp.toPx()),
        ),
    )

    val cloudTint = when {
        flashIntensity > 0.08f -> Color(0xFFEEF2FF)
        lightMode -> Color(0xFF94A3B8)
        else -> Color(0xFFBCCAE0)
    }
    drawParallaxClouds(
        placements = stormClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = cloudTint,
        alphaMultiplier = if (lightMode) 0.82f else 0.94f,
    )

    if (flashIntensity > 0.30f) {
        val startY = contentTop + contentHeight * 0.10f
        val primaryBolt = drawCache.primaryBolt.apply {
            reset()
            moveTo(boltStartX, startY)
            lineTo(boltStartX - 12f, startY + contentHeight * 0.20f)
            lineTo(boltStartX + 8f, startY + contentHeight * 0.26f)
            lineTo(boltStartX - 16f, startY + contentHeight * 0.50f)
            lineTo(boltStartX - 6f, startY + contentHeight * 0.56f)
            lineTo(boltStartX - 22f, startY + contentHeight * 0.78f)
        }
        val forkBranch1 = drawCache.forkBranch1.apply {
            reset()
            moveTo(boltStartX + 8f, startY + contentHeight * 0.26f)
            lineTo(boltStartX + 26f, startY + contentHeight * 0.42f)
            lineTo(boltStartX + 20f, startY + contentHeight * 0.48f)
            lineTo(boltStartX + 34f, startY + contentHeight * 0.62f)
        }
        val forkBranch2 = drawCache.forkBranch2.apply {
            reset()
            moveTo(boltStartX - 16f, startY + contentHeight * 0.50f)
            lineTo(boltStartX - 32f, startY + contentHeight * 0.64f)
        }

        val outerColor = if (lightMode) Color(0xFF4F46E5) else Color(0xFFA5B4FC)

        drawPath(primaryBolt, color = outerColor.copy(alpha = flashIntensity * 0.75f), style = drawCache.lightningPrimaryOuterStroke)
        drawPath(primaryBolt, color = Color.White.copy(alpha = flashIntensity), style = drawCache.lightningPrimaryInnerStroke)
        drawPath(forkBranch1, color = outerColor.copy(alpha = flashIntensity * 0.50f), style = drawCache.lightningForkOuterStroke)
        drawPath(forkBranch1, color = Color.White.copy(alpha = flashIntensity * 0.75f), style = drawCache.lightningForkInnerStroke)
        drawPath(forkBranch2, color = outerColor.copy(alpha = flashIntensity * 0.50f), style = drawCache.lightningForkOuterStroke)
        drawPath(forkBranch2, color = Color.White.copy(alpha = flashIntensity * 0.75f), style = drawCache.lightningForkInnerStroke)
    }

    drawMultiDepthRain(
        raindrops = raindrops,
        rainPhase = rainPhase,
        width = width,
        height = height,
        contentTop = contentTop,
        contentHeight = contentHeight,
        streakSpeedMultiplier = 1.32f,
        windTiltFactor = 0.16f,
        streakCoolColor = if (lightMode) Color(0xFF4338CA) else Color(0xFFA5B4FC),
        streakBrightColor = if (lightMode) Color(0xFF3730A3) else Color(0xFFEEF2FF),
        rippleAlphaScale = 1.15f,
    )
}

private fun DrawScope.drawSnowScene(
    snowflakes: List<Snowflake>,
    overcastClouds: List<CloudSpritePlacement>,
    cloudSprites: CloudSpritePack,
    rainPhase: Float,
    cloudFarPhase: Float,
    cloudMidPhase: Float,
    cloudNearPhase: Float,
    width: Float,
    height: Float,
    contentTop: Float,
    contentHeight: Float,
    isDay: Boolean,
    lightMode: Boolean,
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = when {
                lightMode -> listOf(
                    Color(0xFFBFDBFE).copy(alpha = 0.24f),
                    Color(0xFFE2E8F0).copy(alpha = 0.16f),
                    Color.Transparent,
                )
                !isDay -> listOf(
                    Color(0xFF0A1424).copy(alpha = 0.76f),
                    Color(0xFF14243A).copy(alpha = 0.56f),
                    Color.Transparent,
                )
                else -> listOf(
                    Color(0xFF142236).copy(alpha = 0.74f),
                    Color(0xFF1E324D).copy(alpha = 0.54f),
                    Color.Transparent,
                )
            },
            startY = 0f,
            endY = height,
        ),
        size = Size(width, height),
    )

    drawParallaxClouds(
        placements = overcastClouds,
        sprites = cloudSprites,
        cloudFarPhase = cloudFarPhase,
        cloudMidPhase = cloudMidPhase,
        cloudNearPhase = cloudNearPhase,
        width = width,
        contentTop = contentTop,
        contentHeight = contentHeight,
        tintColor = if (lightMode) Color(0xFFF1F5F9) else if (!isDay) Color(0xFFAEBED8) else Color(0xFFE8F1FA),
        alphaMultiplier = if (lightMode) 0.75f else 0.65f,
    )

    for (flake in snowflakes) {
        val progress = (rainPhase * flake.speed + flake.relYOffset) % 1f
        val sway = sin(rainPhase * 2f * PI.toFloat() * flake.swayFreq + flake.phase) * flake.swayAmp
        val x = (flake.relX * width + sway).coerceIn(0f, width)
        val y = progress * (height + flake.radius * 2f) - flake.radius

        if (y in 0f..height) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (lightMode) {
                        listOf(
                            Color(0xFF93C5FD).copy(alpha = flake.alpha),
                            Color(0xFFBFDBFE).copy(alpha = flake.alpha * 0.55f),
                            Color.Transparent,
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = flake.alpha),
                            Color(0xFFE0F2FE).copy(alpha = flake.alpha * 0.60f),
                            Color.Transparent,
                        )
                    },
                    radius = flake.radius * 1.3f,
                    center = Offset(x, y),
                ),
                radius = flake.radius,
                center = Offset(x, y),
            )
        }
    }
}
