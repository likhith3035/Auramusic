package com.music.bitchord.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.EqualizerMode
import com.music.bitchord.playback.AudioOutputStatus
import com.music.bitchord.playback.EqLayout
import com.music.bitchord.playback.EqualizerPreset
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The app's own equaliser, in the two shapes people have already been taught to
 * read one in.
 *
 * **Dynamic** is a tone pad: one puck over a dot grid, tilting the spectrum
 * left-to-right and shaping the middle of it bottom-to-top, with Broad and
 * Focused choosing how wide each move is. Two numbers and a puck, for the
 * majority of people who want it warmer or brighter and have no interest in
 * which decibel went where.
 *
 * **Manual** is the seven-band graphic equaliser, with a preset list over it.
 *
 * Both drive the same filters — see [EqLayout] — so the tab is a way of
 * describing a curve rather than a different piece of audio machinery, and
 * switching between them glides rather than clicks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessionId by AppSettings.audioSessionId.collectAsStateWithLifecycle()
    val enabled by AppSettings.equalizerEnabled.collectAsStateWithLifecycle()
    val mode by AppSettings.equalizerMode.collectAsStateWithLifecycle()
    val toneX by AppSettings.equalizerToneX.collectAsStateWithLifecycle()
    val toneY by AppSettings.equalizerToneY.collectAsStateWithLifecycle()
    val focused by AppSettings.equalizerFocused.collectAsStateWithLifecycle()
    val balance by AppSettings.equalizerBalance.collectAsStateWithLifecycle()
    val bands by AppSettings.equalizerBands.collectAsStateWithLifecycle()
    val preset by AppSettings.equalizerPreset.collectAsStateWithLifecycle()
    val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()

    val haptics = rememberHaptics()
    var pickingPreset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsGroup(
            footer = stringResource(R.string.equalizer_footer),
            topSpacing = 8.dp,
        ) {
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.equalizer),
                trailing = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppSettings.setEqualizerEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = {
                    haptics.play(if (!enabled) Haptic.ToggleOn else Haptic.ToggleOff)
                    AppSettings.setEqualizerEnabled(!enabled)
                },
            )
        }

        // Off, the whole apparatus below reads as unavailable rather than
        // merely idle. Each control is told so itself rather than having its
        // touches swallowed by a layer above: the pad and the faders all claim
        // the gesture with `awaitFirstDown(requireUnconsumed = false)` — they
        // have to, or the scrolling page would steal every vertical drag — so
        // an ancestor's consumption never reaches them. Consuming on the
        // Initial pass to force it took the page's own scrolling down too.
        val settingsAlpha by animateFloatAsState(
            targetValue = if (enabled) 1f else 0.38f,
            animationSpec = tween(220),
            label = "equalizerEnabled",
        )
        Column(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            Spacer(Modifier.height(18.dp))
            SegmentedControl(
                options = EqualizerMode.entries.map { it.localizedLabel() },
                selectedIndex = EqualizerMode.entries.indexOf(mode),
                onSelect = { AppSettings.setEqualizerMode(EqualizerMode.entries[it]) },
                enabled = enabled,
                modifier = Modifier.padding(horizontal = GROUP_INSET),
            )

            when (mode) {
                EqualizerMode.DYNAMIC -> {
                    DynamicFrequencyCurve(
                        x = toneX,
                        y = toneY,
                        focused = focused,
                        enabled = enabled,
                    )

                    DynamicSoundstagePresets(
                        currentX = toneX,
                        currentY = toneY,
                        enabled = enabled,
                        onSelect = { x, y ->
                            AppSettings.setEqualizerTone(x, y)
                        },
                    )

                    SettingsGroup(
                        header = stringResource(R.string.equalizer_tone),
                        footer = stringResource(
                            if (focused) R.string.equalizer_focused_footer else R.string.equalizer_broad_footer,
                        ),
                    ) {
                        TonePad(
                            x = toneX,
                            y = toneY,
                            onChange = { newX, newY -> AppSettings.setEqualizerTone(newX, newY) },
                            enabled = enabled,
                        )
                        ToneReadout(
                            x = toneX,
                            y = toneY,
                            onReset = { AppSettings.setEqualizerTone(0, 0) },
                        )
                        RowDivider()
                        ChoiceRow(
                            label = stringResource(R.string.equalizer_broad),
                            selected = !focused,
                            onClick = { AppSettings.setEqualizerFocused(false) },
                            enabled = enabled,
                        )
                        RowDivider()
                        ChoiceRow(
                            label = stringResource(R.string.equalizer_focused),
                            selected = focused,
                            onClick = { AppSettings.setEqualizerFocused(true) },
                            enabled = enabled,
                        )
                    }
                }

                EqualizerMode.MANUAL -> {
                    EqualizerFrequencyCurve(
                        bands = bands,
                        enabled = enabled,
                    )

                    QuickPresetPills(
                        selectedPreset = preset,
                        enabled = enabled,
                        onSelectPreset = { AppSettings.setEqualizerPreset(it) },
                        onOpenMore = { pickingPreset = true },
                    )

                    StudioEnhancerRow(
                        bands = bands,
                        enabled = enabled,
                        onChange = { AppSettings.setEqualizerBands(it) },
                    )

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = stringResource(R.string.equalizer_preset),
                            value = preset.localizedLabel(),
                            onClick = { pickingPreset = true },
                            enabled = enabled,
                        )
                    }
                    SettingsGroup(
                        header = stringResource(R.string.equalizer_bands),
                        footer = stringResource(R.string.equalizer_bands_footer),
                    ) {
                        BandSliders(
                            bands = bands,
                            onChange = { AppSettings.setEqualizerBands(it) },
                            enabled = enabled,
                        )
                        RowDivider()
                        DestructiveRow(
                            label = stringResource(R.string.equalizer_reset),
                            enabled = enabled,
                        ) {
                            haptics.play(Haptic.Select)
                            AppSettings.setEqualizerPreset(EqualizerPreset.FLAT)
                        }
                    }
                }
            }

            SettingsGroup(
                header = stringResource(R.string.equalizer_balance),
                footer = stringResource(R.string.equalizer_balance_footer),
            ) {
                BalanceControl(
                    balance = balance,
                    onChange = { AppSettings.setEqualizerBalance(it) },
                    enabled = enabled,
                )
            }

            // A stream that never gets decoded to linear PCM — a passthrough or
            // offload bitstream — reaches the output as the bytes it arrived
            // as, and there is no sample for a filter to touch. Said plainly
            // here rather than left as a dead control, because the alternative
            // is a screen that responds to every touch and changes no sound.
            //
            // This tested `actualEncoding == ENCODING_PCM_FLOAT` for a long
            // while, which was true of the old pipeline and has been wrong
            // since the DSP chain moved upstream into [PrecisionAudioSink].
            // Float output does not affect the equaliser at all; it only drops
            // Media3's *own* processors, which is a claim about silence
            // skipping and belongs on that setting, not this screen.
            if (!outputStatus.dspAvailable) {
                SettingsGroup(footer = stringResource(R.string.equalizer_bitstream_footer)) {
                    SettingsRow(
                        icon = Icons.Rounded.Warning,
                        title = stringResource(R.string.equalizer_bitstream_title),
                        subtitle = stringResource(R.string.equalizer_bitstream_subtitle),
                    )
                }
            }

        }

        // Outside the dimmed block on purpose: the device's own panel works
        // whether or not ours is switched on, and greying out a route to
        // somebody else's equaliser would be claiming ground we do not hold.
        SettingsGroup(footer = stringResource(R.string.system_equalizer_footer)) {
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = stringResource(R.string.system_equalizer),
                subtitle = stringResource(R.string.system_equalizer_subtitle),
                onClick = { openEqualizer(context, sessionId) },
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (pickingPreset) {
        ModalBottomSheet(
            onDismissRequest = { pickingPreset = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PresetSheet(
                selected = preset,
                onSelect = {
                    AppSettings.setEqualizerPreset(it)
                    pickingPreset = false
                },
            )
        }
    }
}

// ---- Tone pad --------------------------------------------------------------

// ---- Dynamic Tone Pad & Spectrum ------------------------------------------

@Composable
private fun DynamicFrequencyCurve(
    x: Int,
    y: Int,
    focused: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val hzLabels = listOf("60", "150", "400", "1k", "2.5k", "6k", "14k")
    val tiltDb = x * EqLayout.TONE_DB_PER_STEP
    val contourDb = y * EqLayout.TONE_DB_PER_STEP
    val range = EqLayout.MANUAL_RANGE_DB

    val b0Target = (-tiltDb * 1.0f).coerceIn(-range, range)
    val b1Target = (-tiltDb * 0.85f).coerceIn(-range, range)
    val b2Target = (-tiltDb * 0.40f + contourDb * (if (focused) 0.15f else 0.40f)).coerceIn(-range, range)
    val b3Target = (contourDb * (if (focused) 1.0f else 0.88f)).coerceIn(-range, range)
    val b4Target = (tiltDb * 0.40f + contourDb * (if (focused) 0.15f else 0.40f)).coerceIn(-range, range)
    val b5Target = (tiltDb * 0.85f).coerceIn(-range, range)
    val b6Target = (tiltDb * 1.0f).coerceIn(-range, range)

    val b0 by animateFloatAsState(b0Target, spring(stiffness = Spring.StiffnessLow), label = "dynB0")
    val b1 by animateFloatAsState(b1Target, spring(stiffness = Spring.StiffnessLow), label = "dynB1")
    val b2 by animateFloatAsState(b2Target, spring(stiffness = Spring.StiffnessLow), label = "dynB2")
    val b3 by animateFloatAsState(b3Target, spring(stiffness = Spring.StiffnessLow), label = "dynB3")
    val b4 by animateFloatAsState(b4Target, spring(stiffness = Spring.StiffnessLow), label = "dynB4")
    val b5 by animateFloatAsState(b5Target, spring(stiffness = Spring.StiffnessLow), label = "dynB5")
    val b6 by animateFloatAsState(b6Target, spring(stiffness = Spring.StiffnessLow), label = "dynB6")
    val animatedBands = listOf(b0, b1, b2, b3, b4, b5, b6)

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        color = onSurfaceVariant.copy(alpha = 0.7f),
    )

    val dynamicTag = when {
        x == 0 && y == 0 -> "BALANCED FLAT • 0 dB"
        x < 0 && y > 0 -> "WARM & FORWARD • ${signed(x)} / ${signed(y)}"
        x > 0 && y > 0 -> "BRIGHT & VOCAL • ${signed(x)} / ${signed(y)}"
        x < 0 && y < 0 -> "DEEP BASS SCOOP • ${signed(x)} / ${signed(y)}"
        x > 0 && y < 0 -> "CRISP AIR SCOOP • ${signed(x)} / ${signed(y)}"
        x < 0 -> "WARM TILT • ${signed(x)}"
        x > 0 -> "BRIGHT TILT • ${signed(x)}"
        y > 0 -> "VOCAL PUNCH • ${signed(y)}"
        else -> "SCOOPED MIDS • ${signed(y)}"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF13131A),
                        Color(0xFF0C0C10),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.28f),
                        Color(0xFF22222E),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(top = 12.dp, bottom = 8.dp, start = 10.dp, end = 10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (enabled) Color(0xFF00F5D4) else Color.Gray),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "DYNAMIC ACOUSTIC RESPONSE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (enabled) Color(0xFF00F5D4) else onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = dynamicTag,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    fontSize = 10.sp,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp),
            ) {
                val w = size.width
                val h = size.height
                val padY = 12.dp.toPx()
                val usableH = h - padY * 2
                val midY = padY + usableH / 2f

                val stepDb = listOf(12f, 6f, 0f, -6f, -12f)
                stepDb.forEach { db ->
                    val yPos = midY - (db / range) * (usableH / 2f)
                    val isZero = abs(db) < 0.1f
                    drawLine(
                        color = if (isZero) primaryColor.copy(alpha = 0.35f) else gridColor,
                        start = Offset(0f, yPos),
                        end = Offset(w, yPos),
                        strokeWidth = if (isZero) 1.5.dp.toPx() else 1.dp.toPx(),
                    )
                }

                val count = animatedBands.size
                val stepX = w / (count - 1)
                val points = animatedBands.mapIndexed { i, db ->
                    val clamped = db.coerceIn(-range, range)
                    Offset(
                        x = i * stepX,
                        y = midY - (clamped / range) * (usableH / 2f),
                    )
                }

                val path = Path()
                val fillPath = Path()
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, midY)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, midY)
                    fillPath.close()

                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (enabled) 0.22f else 0.05f),
                            Color.Transparent,
                        ),
                        startY = padY,
                        endY = h,
                    )
                    drawPath(fillPath, brush = fillBrush)

                    val curveBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF59E0B),
                            Color(0xFF10B981),
                            Color(0xFF06B6D4),
                        ),
                    )
                    drawPath(
                        path = path,
                        brush = if (enabled) curveBrush else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )

                    points.forEach { pt ->
                        drawCircle(
                            color = Color(0xFF0C0C10),
                            radius = 4.dp.toPx(),
                            center = pt,
                        )
                        drawCircle(
                            color = if (enabled) primaryColor else Color.Gray,
                            radius = 2.5.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                hzLabels.forEach { label ->
                    Text(
                        text = label,
                        style = labelStyle,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class SoundstageMacro(
    val title: String,
    val emoji: String,
    val x: Int,
    val y: Int,
)

private val SOUNDSTAGE_MACROS = listOf(
    SoundstageMacro("Warm Vinyl", "🎛️", -3, 1),
    SoundstageMacro("Crystal Air", "✨", 3, -2),
    SoundstageMacro("Vocal Punch", "🎙️", 0, 3),
    SoundstageMacro("Club Bass", "🔊", -4, -2),
    SoundstageMacro("Studio Flat", "⚖️", 0, 0),
)

@Composable
private fun DynamicSoundstagePresets(
    currentX: Int,
    currentY: Int,
    enabled: Boolean,
    onSelect: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GROUP_INSET, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SOUNDSTAGE_MACROS.forEach { macro ->
            val isSelected = currentX == macro.x && currentY == macro.y
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else Color(0xFF14141E)
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else Color(0xFF262638)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) {
                        haptics.play(Haptic.Select)
                        onSelect(macro.x, macro.y)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = macro.emoji, fontSize = 13.sp)
                    Text(
                        text = macro.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                    )
                }
            }
        }
    }
}

/**
 * The dot grid and its puck with 4-quadrant dynamic lighting and guidance badges.
 */
@Composable
private fun TonePad(
    x: Int,
    y: Int,
    enabled: Boolean,
    onChange: (Int, Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val primaryColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    val steps = EqLayout.TONE_STEPS

    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)

    val animatedX by animateFloatAsState(
        targetValue = x.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadX",
    )
    val animatedY by animateFloatAsState(
        targetValue = y.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadY",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_INSET, vertical = 8.dp),
    ) {
        // Quadrant Guidance Tags Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "🔥 WARM BASS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "✨ BRIGHT AIR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color(0xFF06B6D4).copy(alpha = 0.8f),
                letterSpacing = 0.5.sp,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PAD_HEIGHT)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F0F17))
                .border(1.dp, Color(0xFF222232), RoundedCornerShape(16.dp))
                .pointerInput(steps, enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var lastX = latestX
                        var lastY = latestY
                        fun report(offset: Offset) {
                            val (newX, newY) = stepAt(offset, size.toSize(), PUCK_RADIUS.toPx(), steps)
                            if (newX != lastX || newY != lastY) {
                                lastX = newX
                                lastY = newY
                                haptics.play(Haptic.Tick)
                                onChange(newX, newY)
                            }
                        }
                        report(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            if (change.positionChanged()) {
                                change.consume()
                                report(change.position)
                            }
                        }
                    }
                },
        ) {
            val inset = PUCK_RADIUS.toPx()
            val usableWidth = size.width - inset * 2
            val usableHeight = size.height - inset * 2
            val columns = steps * 2
            val dotRadius = DOT_RADIUS.toPx()

            // 4-Quadrant Ambient Lighting
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.6f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width, 0f),
                    radius = size.width * 0.6f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(0f, size.height),
                    radius = size.width * 0.6f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEC4899).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width, size.height),
                    radius = size.width * 0.6f,
                ),
            )

            // Crosshair central axis lines
            val centerX = inset + usableWidth * 0.5f
            val centerY = inset + usableHeight * 0.5f
            drawLine(
                color = primaryColor.copy(alpha = 0.16f),
                start = Offset(centerX, inset),
                end = Offset(centerX, size.height - inset),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.16f),
                start = Offset(inset, centerY),
                end = Offset(size.width - inset, centerY),
                strokeWidth = 1.dp.toPx(),
            )

            // Grid dots
            for (column in 0..columns) {
                for (row in 0..columns) {
                    val onAxis = column == steps || row == steps
                    drawCircle(
                        color = if (onAxis) primaryColor.copy(alpha = 0.50f) else dotColor.copy(alpha = 0.25f),
                        radius = if (onAxis) dotRadius * 1.2f else dotRadius,
                        center = Offset(
                            inset + usableWidth * column / columns,
                            inset + usableHeight * row / columns,
                        ),
                    )
                }
            }

            // Center zero detent glow ring when puck is at center
            if (x == 0 && y == 0) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.20f),
                    radius = inset * 1.4f,
                    center = Offset(centerX, centerY),
                )
            }

            // Animated Puck Center
            val puckCentre = Offset(
                inset + usableWidth * (animatedX + steps) / columns,
                inset + usableHeight * (steps - animatedY) / columns,
            )

            // Dynamic puck color based on quadrant
            val puckAccent = when {
                animatedX < -0.4f -> Color(0xFFF59E0B)
                animatedX > 0.4f -> Color(0xFF06B6D4)
                animatedY > 0.5f -> Color(0xFF10B981)
                animatedY < -0.5f -> Color(0xFFEC4899)
                else -> primaryColor
            }

            // Glowing halo rings
            for (ring in 3 downTo 1) {
                drawCircle(
                    color = puckAccent.copy(alpha = 0.14f / ring),
                    radius = inset + ring * 5f,
                    center = puckCentre,
                )
            }

            // Lens Puck Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF28293E), Color(0xFF12121E)),
                    center = puckCentre,
                    radius = inset,
                ),
                radius = inset,
                center = puckCentre,
            )

            // Hairline Specular Highlight Border
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), puckAccent.copy(alpha = 0.40f)),
                    startY = puckCentre.y - inset,
                    endY = puckCentre.y + inset,
                ),
                radius = inset,
                center = puckCentre,
                style = Stroke(width = 1.8.dp.toPx()),
            )

            // Glowing center core dot
            drawCircle(
                color = puckAccent,
                radius = 3.5.dp.toPx(),
                center = puckCentre,
            )
        }

        // Quadrant Guidance Tags Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "🎧 WARM SCOOP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.8f),
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "⚡ CRISP SCOOP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color(0xFFEC4899).copy(alpha = 0.8f),
                letterSpacing = 0.5.sp,
            )
        }
    }
}

/** Which step the finger is over, clamped to the grid. */
private fun stepAt(offset: Offset, size: Size, inset: Float, steps: Int): Pair<Int, Int> {
    val usableWidth = (size.width - inset * 2).coerceAtLeast(1f)
    val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
    val fractionX = ((offset.x - inset) / usableWidth).coerceIn(0f, 1f)
    val fractionY = ((offset.y - inset) / usableHeight).coerceIn(0f, 1f)
    val x = (fractionX * steps * 2).roundToInt() - steps
    val y = steps - (fractionY * steps * 2).roundToInt()
    return x to y
}

/** Interactive readout chips showing tilt and contour values with 1-tap center reset. */
@Composable
private fun ToneReadout(
    x: Int,
    y: Int,
    onReset: () -> Unit,
) {
    val haptics = rememberHaptics()
    val isCentered = x == 0 && y == 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tilt Chip
        Box(
            modifier = Modifier
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141420))
                .border(1.dp, Color(0xFF28283C), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Waves,
                    contentDescription = stringResource(R.string.equalizer_tilt),
                    tint = if (x < 0) Color(0xFFF59E0B) else if (x > 0) Color(0xFF06B6D4) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        x < 0 -> "Warm (${signed(x)})"
                        x > 0 -> "Bright (${signed(x)})"
                        else -> "Tilt: 0"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        // Contour Chip
        Box(
            modifier = Modifier
                .padding(end = if (!isCentered) 6.dp else 0.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141420))
                .border(1.dp, Color(0xFF28283C), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                    contentDescription = stringResource(R.string.equalizer_contour),
                    tint = if (y > 0) Color(0xFF10B981) else if (y < 0) Color(0xFFEC4899) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        y > 0 -> "Vocal (${signed(y)})"
                        y < 0 -> "Scoop (${signed(y)})"
                        else -> "Contour: 0"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        // Quick Zero Detent Button
        if (!isCentered) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .clickable {
                        haptics.play(Haptic.Select)
                        onReset()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Center",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "Center",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Zero has no sign; anything else wears one, so the direction reads at a glance. */
private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

/** A row that is one of a set, ticked when it is the one in force. */
@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (!selected) {
                    haptics.play(Haptic.Select)
                    onClick()
                }
            }
            .padding(horizontal = ROW_INSET, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ---- Studio Visualizer & Quick Presets ------------------------------------

@Composable
private fun EqualizerFrequencyCurve(
    bands: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val range = EqLayout.MANUAL_RANGE_DB
    val hzLabels = listOf("60", "150", "400", "1k", "2.5k", "6k", "14k")

    val b0 by animateFloatAsState(bands.getOrElse(0) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b0")
    val b1 by animateFloatAsState(bands.getOrElse(1) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b1")
    val b2 by animateFloatAsState(bands.getOrElse(2) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b2")
    val b3 by animateFloatAsState(bands.getOrElse(3) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b3")
    val b4 by animateFloatAsState(bands.getOrElse(4) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b4")
    val b5 by animateFloatAsState(bands.getOrElse(5) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b5")
    val b6 by animateFloatAsState(bands.getOrElse(6) { 0f }, spring(stiffness = Spring.StiffnessLow), label = "b6")
    val animatedBands = listOf(b0, b1, b2, b3, b4, b5, b6)

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        color = onSurfaceVariant.copy(alpha = 0.7f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF13131A),
                        Color(0xFF0C0C10),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.28f),
                        Color(0xFF22222E),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(top = 12.dp, bottom = 8.dp, start = 10.dp, end = 10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (enabled) Color(0xFF00F5D4) else Color.Gray),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "STUDIO SPECTRUM RESPONSE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (enabled) Color(0xFF00F5D4) else onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = "±12 dB • 7 BANDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp),
            ) {
                val w = size.width
                val h = size.height
                val padY = 12.dp.toPx()
                val usableH = h - padY * 2
                val midY = padY + usableH / 2f

                // Horizontal grid lines
                val stepDb = listOf(12f, 6f, 0f, -6f, -12f)
                stepDb.forEach { db ->
                    val yPos = midY - (db / range) * (usableH / 2f)
                    val isZero = abs(db) < 0.1f
                    drawLine(
                        color = if (isZero) primaryColor.copy(alpha = 0.35f) else gridColor,
                        start = Offset(0f, yPos),
                        end = Offset(w, yPos),
                        strokeWidth = if (isZero) 1.2.dp.toPx() else 0.8.dp.toPx(),
                    )
                }

                val count = animatedBands.size
                val points = ArrayList<Offset>(count)
                val padX = 14.dp.toPx()
                val usableW = w - padX * 2
                for (i in 0 until count) {
                    val px = padX + (i.toFloat() / (count - 1)) * usableW
                    val py = midY - (animatedBands[i] / range) * (usableH / 2f)
                    points.add(Offset(px, py))
                }

                // Smooth cubic curve path
                val path = Path()
                path.moveTo(0f, points[0].y)
                path.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[maxOf(0, i - 1)]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = points[minOf(points.size - 1, i + 2)]

                    val cp1x = p1.x + (p2.x - p0.x) / 6f
                    val cp1y = p1.y + (p2.y - p0.y) / 6f
                    val cp2x = p2.x - (p3.x - p1.x) / 6f
                    val cp2y = p2.y - (p3.y - p1.y) / 6f

                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                }
                path.lineTo(w, points.last().y)

                // Fill under curve
                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(w, h)
                fillPath.lineTo(0f, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (enabled) 0.32f else 0.08f),
                            Color(0xFF00F5D4).copy(alpha = if (enabled) 0.12f else 0.03f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = h,
                    ),
                )

                // Stroke line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(
                                Color(0xFF00F5D4),
                                Color(0xFF00BBF9),
                                Color(0xFF8338EC),
                                Color(0xFFFF007F),
                            )
                        } else {
                            listOf(Color.Gray, Color.DarkGray)
                        },
                    ),
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Anchor points
                points.forEachIndexed { idx, pt ->
                    val bandVal = animatedBands.getOrElse(idx) { 0f }
                    val isBoosted = bandVal > 0.5f
                    val isCut = bandVal < -0.5f
                    val nodeColor = when {
                        !enabled -> Color.Gray
                        isBoosted -> Color(0xFF00F5D4)
                        isCut -> Color(0xFFFF9E00)
                        else -> primaryColor
                    }

                    drawCircle(
                        color = nodeColor.copy(alpha = 0.25f),
                        radius = 6.5.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = 3.5.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.2.dp.toPx(),
                        center = pt,
                    )
                }
            }

            // Frequency Labels along bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                hzLabels.forEach { label ->
                    Text(
                        text = label,
                        style = labelStyle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPresetPills(
    selectedPreset: EqualizerPreset,
    enabled: Boolean,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scrollState = rememberScrollState()

    val quickList = listOf(
        Pair("Flat", EqualizerPreset.FLAT),
        Pair("Bass Boost", EqualizerPreset.BASS_BOOST),
        Pair("Rock", EqualizerPreset.ROCK),
        Pair("Electronic", EqualizerPreset.ELECTRONIC),
        Pair("Vocal", EqualizerPreset.VOCAL),
        Pair("Acoustic", EqualizerPreset.ACOUSTIC),
        Pair("Loudness", EqualizerPreset.LOUDNESS),
        Pair("Late Night", EqualizerPreset.LATE_NIGHT),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = GROUP_INSET, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        quickList.forEach { (title, preset) ->
            val isSelected = selectedPreset == preset
            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            primaryColor.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable(enabled = enabled) {
                        haptics.play(Haptic.Select)
                        onSelectPreset(preset)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // More Presets Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(enabled = enabled) {
                    haptics.play(Haptic.Select)
                    onOpenMore()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "All Presets...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StudioEnhancerRow(
    bands: List<Float>,
    enabled: Boolean,
    onChange: (List<Float>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val range = EqLayout.MANUAL_RANGE_DB

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EnhancerPill(
            label = "Deep Bass",
            icon = "🔊",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Select)
                val updated = bands.toMutableList()
                val currentLow = (updated.getOrElse(0) { 0f } + updated.getOrElse(1) { 0f }) / 2f
                val delta = if (currentLow > 2f) -3f else 4f
                if (updated.size > 0) updated[0] = (updated[0] + delta).coerceIn(-range, range)
                if (updated.size > 1) updated[1] = (updated[1] + delta).coerceIn(-range, range)
                onChange(updated)
            },
        )

        EnhancerPill(
            label = "Vocal Clarity",
            icon = "🎤",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Select)
                val updated = bands.toMutableList()
                val currentMid = (updated.getOrElse(3) { 0f } + updated.getOrElse(4) { 0f }) / 2f
                val delta = if (currentMid > 1.5f) -2.5f else 3.5f
                if (updated.size > 3) updated[3] = (updated[3] + delta).coerceIn(-range, range)
                if (updated.size > 4) updated[4] = (updated[4] + delta).coerceIn(-range, range)
                onChange(updated)
            },
        )

        EnhancerPill(
            label = "Air & Treble",
            icon = "✨",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Select)
                val updated = bands.toMutableList()
                val currentHigh = (updated.getOrElse(5) { 0f } + updated.getOrElse(6) { 0f }) / 2f
                val delta = if (currentHigh > 1.5f) -2.5f else 3.5f
                if (updated.size > 5) updated[5] = (updated[5] + delta).coerceIn(-range, range)
                if (updated.size > 6) updated[6] = (updated[6] + delta).coerceIn(-range, range)
                onChange(updated)
            },
        )
    }
}

@Composable
private fun EnhancerPill(
    label: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = icon, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

// ---- Manual bands ----------------------------------------------------------

/**
 * Seven vertical faders, drawn.
 *
 * Material's `Slider` rotated a quarter turn keeps its horizontal touch
 * geometry and its horizontal thumb, which is why every Compose equaliser built
 * that way feels wrong under the finger. These are a track, a fill and a knob
 * on a canvas, taking the gesture on first touch the way [TonePad] does.
 *
 * The fill runs from the centre rather than the bottom because the value it is
 * showing is signed: a band at −6 dB and a band at +6 dB are equal and opposite,
 * and a bar growing from the floor would draw them as a short one and a tall
 * one.
 */
@Composable
private fun BandSliders(
    bands: List<Float>,
    enabled: Boolean,
    onChange: (List<Float>) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_INSET - 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        EqLayout.MANUAL_BANDS_HZ.forEachIndexed { index, hz ->
            val value = bands.getOrElse(index) { 0f }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatGain(value),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = when {
                        value > 0.05f -> Color(0xFF00F5D4)
                        value < -0.05f -> Color(0xFFFF9E00)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                BandFader(
                    enabled = enabled,
                    value = value,
                    onChange = { updated ->
                        onChange(bands.toMutableList().also { it[index] = updated })
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatFrequency(hz),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BandFader(
    value: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val haptics = rememberHaptics()
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val knobColor = MaterialTheme.colorScheme.surface
    val range = EqLayout.MANUAL_RANGE_DB
    // See [TonePad]: the gesture block outlives the composition that made it.
    val latest by rememberUpdatedState(value)

    Canvas(
        modifier = Modifier
            .width(FADER_WIDTH)
            .height(FADER_HEIGHT)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var last = latest
                    fun report(offset: Offset) {
                        val inset = KNOB_RADIUS.toPx()
                        val usable = (size.height - inset * 2).coerceAtLeast(1f)
                        val fraction = ((offset.y - inset) / usable).coerceIn(0f, 1f)
                        val raw = (1f - fraction * 2f) * range
                        val snapped = snapGain(raw, range)
                        if (abs(snapped - last) > 0.001f) {
                            // A tick per step, and the step at zero is the one
                            // people are aiming for when they undo a band.
                            haptics.play(Haptic.Tick)
                            last = snapped
                            onChange(snapped)
                        }
                    }
                    report(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if (change.positionChanged()) {
                            change.consume()
                            report(change.position)
                        }
                    }
                }
            },
    ) {
        val inset = KNOB_RADIUS.toPx()
        val usable = size.height - inset * 2
        val centreY = inset + usable / 2
        val trackWidth = TRACK_WIDTH.toPx()
        val centreX = size.width / 2
        val knobY = inset + usable * (1f - (value / range + 1f) / 2f)
        val isBoost = value > 0.05f
        val isCut = value < -0.05f

        // Draw track groove background
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(centreX - trackWidth / 2, inset),
            size = Size(trackWidth, usable),
            cornerRadius = CornerRadius(trackWidth / 2),
        )

        // Draw active level gradient fill from centre 0dB to current knob position
        if (abs(value) > 0.05f) {
            val fillBrush = if (isBoost) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)),
                    startY = knobY,
                    endY = centreY,
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF9E00), Color(0xFFFF007F)),
                    startY = centreY,
                    endY = knobY,
                )
            }
            drawRoundRect(
                brush = fillBrush,
                topLeft = Offset(centreX - trackWidth / 2, minOf(centreY, knobY)),
                size = Size(trackWidth, abs(centreY - knobY)),
                cornerRadius = CornerRadius(trackWidth / 2),
            )
        }

        // The zero reference line
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(centreX - trackWidth * 1.5f, centreY),
            end = Offset(centreX + trackWidth * 1.5f, centreY),
            strokeWidth = 1.dp.toPx(),
        )

        // Knob outer glow if boosted or cut
        if (abs(value) > 0.05f) {
            val glowColor = if (isBoost) Color(0xFF00F5D4) else Color(0xFFFF9E00)
            drawCircle(
                color = glowColor.copy(alpha = 0.28f),
                radius = inset + 3.dp.toPx(),
                center = Offset(centreX, knobY),
            )
        }

        // Knob shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = inset + 1.5f,
            center = Offset(centreX, knobY + 1.5f),
        )

        // Knob outer metallic rim
        drawCircle(
            color = if (abs(value) > 0.05f) {
                if (isBoost) Color(0xFF00F5D4) else Color(0xFFFF9E00)
            } else {
                Color(0xFF383842)
            },
            radius = inset,
            center = Offset(centreX, knobY),
        )

        // Knob inner body
        drawCircle(
            color = knobColor,
            radius = inset - 1.5.dp.toPx(),
            center = Offset(centreX, knobY),
        )

        // Center tactile pip
        drawCircle(
            color = if (abs(value) > 0.05f) {
                if (isBoost) Color(0xFF00F5D4) else Color(0xFFFF9E00)
            } else {
                Color.White.copy(alpha = 0.6f)
            },
            radius = 2.5.dp.toPx(),
            center = Offset(centreX, knobY),
        )
    }
}

/**
 * Half-decibel steps, with a wider one at zero.
 *
 * Flat is the value a band gets dragged back to far more often than any other,
 * and it is the one value a finger cannot hit reliably on a 150 dp travel — so
 * it is given a detent to fall into rather than a coordinate to find.
 */
private fun snapGain(raw: Float, range: Float): Float {
    if (abs(raw) < ZERO_DETENT_DB) return 0f
    return ((raw * 2f).roundToInt() / 2f).coerceIn(-range, range)
}

private fun formatGain(value: Float): String = when {
    abs(value) < 0.05f -> "0"
    value > 0 -> "+" + trimGain(value)
    else -> trimGain(value)
}

/** "+6" rather than "+6.0", but "+6.5" when the half matters. */
private fun trimGain(value: Float): String =
    if (abs(value - value.roundToInt()) < 0.05f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

/** "60", "2.5k", "14k" — the labels a hardware equaliser prints. */
private fun formatFrequency(hz: Float): String = when {
    hz < 1_000f -> hz.roundToInt().toString()
    hz % 1_000f == 0f -> "${(hz / 1_000f).roundToInt()}k"
    else -> String.format(Locale.getDefault(), "%.1fk", hz / 1_000f)
}

// ---- Balance ---------------------------------------------------------------

/**
 * L and R with the trim between them, and a detent at centre for the same
 * reason [snapGain] has one: dead centre is where this spends its life, and it
 * is the one position a drag cannot land on by aim alone.
 */
@Composable
private fun BalanceControl(
    balance: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val haptics = rememberHaptics()
    val trackColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.primary
    val knobColor = MaterialTheme.colorScheme.background
    // See [TonePad]: the gesture block outlives the composition that made it.
    val latest by rememberUpdatedState(balance)

    Column(Modifier.padding(horizontal = ROW_INSET, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.equalizer_balance_left),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = String.format(Locale.getDefault(), "%.2f", balance),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.equalizer_balance_right),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(KNOB_RADIUS * 2 + 8.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var last = latest
                        fun report(offset: Offset) {
                            val inset = KNOB_RADIUS.toPx()
                            val usable = (size.width - inset * 2).coerceAtLeast(1f)
                            val fraction = ((offset.x - inset) / usable).coerceIn(0f, 1f)
                            val raw = fraction * 2f - 1f
                            val snapped = if (abs(raw) < BALANCE_DETENT) 0f else raw
                            if (abs(snapped - last) > 0.004f) {
                                if (snapped == 0f && last != 0f) haptics.play(Haptic.Tick)
                                last = snapped
                                onChange(snapped)
                            }
                        }
                        report(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            if (change.positionChanged()) {
                                change.consume()
                                report(change.position)
                            }
                        }
                    }
                },
        ) {
            val inset = KNOB_RADIUS.toPx()
            val usable = size.width - inset * 2
            val centreY = size.height / 2
            val centreX = inset + usable / 2
            val trackHeight = TRACK_WIDTH.toPx()
            val knobX = inset + usable * (balance + 1f) / 2f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(inset, centreY - trackHeight / 2),
                size = Size(usable, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2),
            )
            if (abs(balance) > 0.004f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(minOf(centreX, knobX), centreY - trackHeight / 2),
                    size = Size(abs(centreX - knobX), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2),
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = inset + 1.5f,
                center = Offset(knobX, centreY + 1.5f),
            )
            drawCircle(color = knobColor, radius = inset, center = Offset(knobX, centreY))
        }
    }
}

// ---- Presets ---------------------------------------------------------------

@Composable
private fun PresetSheet(selected: EqualizerPreset, onSelect: (EqualizerPreset) -> Unit) {
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.equalizer_preset),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        // Sixteen curves is taller than the sheet on most phones, and laid
        // out straight the ones past the fold were simply unreachable. The
        // title and its rule stay put while the list underneath scrolls;
        // `fill = false` keeps a short list from stretching the sheet to the
        // full height it is allowed.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            EqualizerPreset.entries.forEach { preset ->
                // Custom is a state the bands can be in, not a curve anyone can
                // choose, so it is only ever shown when it is the one in force.
                if (preset == EqualizerPreset.CUSTOM && selected != EqualizerPreset.CUSTOM) {
                    return@forEach
                }
                val chosen = preset == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = preset != EqualizerPreset.CUSTOM) {
                            haptics.play(Haptic.Select)
                            onSelect(preset)
                        }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = preset.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (chosen) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.selected),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
        }
        }
    }
}

@Composable
private fun EqualizerMode.localizedLabel(): String = stringResource(
    when (this) {
        EqualizerMode.DYNAMIC -> R.string.equalizer_dynamic
        EqualizerMode.MANUAL -> R.string.equalizer_manual
    },
)

@Composable
private fun EqualizerPreset.localizedLabel(): String = stringResource(
    when (this) {
        EqualizerPreset.FLAT -> R.string.eq_preset_flat
        EqualizerPreset.ACOUSTIC -> R.string.eq_preset_acoustic
        EqualizerPreset.BASS_BOOST -> R.string.eq_preset_bass_boost
        EqualizerPreset.BASS_CUT -> R.string.eq_preset_bass_cut
        EqualizerPreset.VOCAL -> R.string.eq_preset_vocal
        EqualizerPreset.TREBLE_BOOST -> R.string.eq_preset_treble_boost
        EqualizerPreset.TREBLE_CUT -> R.string.eq_preset_treble_cut
        EqualizerPreset.LOUDNESS -> R.string.eq_preset_loudness
        EqualizerPreset.SPOKEN_WORD -> R.string.eq_preset_spoken_word
        EqualizerPreset.ELECTRONIC -> R.string.eq_preset_electronic
        EqualizerPreset.ROCK -> R.string.eq_preset_rock
        EqualizerPreset.HIP_HOP -> R.string.eq_preset_hip_hop
        EqualizerPreset.JAZZ -> R.string.eq_preset_jazz
        EqualizerPreset.CLASSICAL -> R.string.eq_preset_classical
        EqualizerPreset.SMALL_SPEAKERS -> R.string.eq_preset_small_speakers
        EqualizerPreset.LATE_NIGHT -> R.string.eq_preset_late_night
        EqualizerPreset.CUSTOM -> R.string.eq_preset_custom
    },
)

private val PAD_HEIGHT = 230.dp
private val PUCK_RADIUS = 19.dp
private val DOT_RADIUS = 2.5.dp
private val FADER_HEIGHT = 150.dp
private val FADER_WIDTH = 34.dp
private val TRACK_WIDTH = 5.dp
private val KNOB_RADIUS = 10.dp

/** How close to flat a band has to be dragged before it snaps there. */
private const val ZERO_DETENT_DB = 0.6f

/** Same, for the balance trim's own centre. */
private const val BALANCE_DETENT = 0.04f
