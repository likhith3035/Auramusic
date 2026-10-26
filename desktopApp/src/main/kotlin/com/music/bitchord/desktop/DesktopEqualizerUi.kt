package com.music.bitchord.desktop

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.music.bitchord.playback.EqLayout
import com.music.bitchord.playback.EqualizerPreset
import com.music.bitchord.ui.icons.BitChordIcons
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The app's own equaliser, in the two shapes people have already been taught to read one in.
 *
 * **Dynamic** is a tone pad: one puck over a dot grid, tilting the spectrum left-to-right and
 * shaping the middle of it bottom-to-top, with Broad and Focused choosing how wide each move is.
 *
 * **Manual** is the seven-band graphic equaliser, with a preset list beside it.
 *
 * Both drive the same filters — [EqLayout], shared with the Android build — so the tab is a way of
 * describing a curve rather than a different piece of audio machinery.
 *
 * Laid out for a window rather than transcribed from the phone: the two tabs sit in a dialog wide
 * enough that the preset list stands beside the bands instead of behind a sheet, and every control
 * is driven by a pointer, so the haptic ticks Android leans on are carried by hover and the snap
 * instead.
 */
@Composable
internal fun DesktopEqualizerDialog(onDismiss: () -> Unit) {
    val enabled by DesktopEqualizerSettings.enabled.collectAsState()
    val mode by DesktopEqualizerSettings.mode.collectAsState()
    val balance by DesktopEqualizerSettings.balance.collectAsState()

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 560) {
        Row(
            Modifier.fillMaxWidth().padding(start = panelInset(22.dp), end = panelInset(18.dp), top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    DesktopStrings["equalizer", "Equalizer"],
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    DesktopStrings["equalizer_footer", "Shapes the sound inside AuraMusic, before it reaches the system."],
                    style = MaterialTheme.typography.bodySmall,
                    color = DesktopSecondary,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = DesktopEqualizerSettings::setEnabled,
                colors = desktopSwitchColors(),
            )
        }

        // Off, the apparatus below reads as unavailable rather than merely idle: dimmed, and it
        // stops answering the pointer, so nobody drags a band wondering why nothing changed.
        val alpha by animateFloatAsState(if (enabled) 1f else 0.38f, label = "equalizerEnabled")
        Column(
            Modifier
                .graphicsLayer { this.alpha = alpha }
                .then(if (enabled) Modifier else Modifier.pointerInput(Unit) {})
                .then(desktopPanelBody(cardMax = 520.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))
            ModeTabs(mode, DesktopEqualizerSettings::setMode)

            when (mode) {
                DesktopEqualizerMode.DYNAMIC -> DynamicTab()
                DesktopEqualizerMode.MANUAL -> ManualTab()
            }

            SectionLabel(DesktopStrings["equalizer_balance", "Balance"])
            BalanceControl(balance, DesktopEqualizerSettings::setBalance)
            Text(
                DesktopStrings["equalizer_balance_footer", "Shifts the sound towards one ear by quietening the other."],
                style = MaterialTheme.typography.bodySmall,
                color = DesktopSecondary,
                modifier = Modifier.padding(horizontal = panelInset(22.dp), vertical = 6.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = panelInset(20.dp), vertical = 14.dp)) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(DesktopStrings["done", "Done"], color = Color.White) }
        }
    }
}

@Composable
private fun ModeTabs(mode: DesktopEqualizerMode, onSelect: (DesktopEqualizerMode) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = panelInset(22.dp))
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DesktopEqualizerMode.entries.forEach { option ->
            val selected = option == mode
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.92f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (option) {
                        DesktopEqualizerMode.DYNAMIC -> DesktopStrings["equalizer_dynamic", "Dynamic"]
                        DesktopEqualizerMode.MANUAL -> DesktopStrings["equalizer_manual", "Manual"]
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.Black else Color.White,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        color = DesktopSecondary,
        modifier = Modifier.padding(start = panelInset(22.dp), top = 18.dp, bottom = 6.dp),
    )
}

// ---- Dynamic ---------------------------------------------------------------

@Composable
private fun DynamicTab() {
    val x by DesktopEqualizerSettings.toneX.collectAsState()
    val y by DesktopEqualizerSettings.toneY.collectAsState()
    val focused by DesktopEqualizerSettings.focused.collectAsState()

    SectionLabel(DesktopStrings["equalizer_presets", "Soundstage Presets"])
    DynamicSoundstagePresets(currentX = x, currentY = y, onSelect = DesktopEqualizerSettings::setTone)

    Spacer(Modifier.height(10.dp))
    DesktopDynamicFrequencyCurve(x = x, y = y, focused = focused)

    Spacer(Modifier.height(10.dp))
    SectionLabel(DesktopStrings["equalizer_tone", "Tone Matrix"])
    TonePad(x, y, DesktopEqualizerSettings::setTone)

    ToneReadoutBar(
        x = x,
        y = y,
        onReset = { DesktopEqualizerSettings.setTone(0, 0) },
    )

    Row(Modifier.padding(horizontal = panelInset(22.dp)), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidthChoice(DesktopStrings["equalizer_broad", "Broad"], !focused) {
            DesktopEqualizerSettings.setFocused(false)
        }
        WidthChoice(DesktopStrings["equalizer_focused", "Focused"], focused) {
            DesktopEqualizerSettings.setFocused(true)
        }
    }
    Text(
        if (focused) {
            DesktopStrings["equalizer_focused_footer", "Drag left for a warmer sound and right for a brighter one; down scoops the middle, up brings it forward. Focused works on narrower stretches, so it changes less around what it moves."]
        } else {
            DesktopStrings["equalizer_broad_footer", "Drag left for a warmer sound and right for a brighter one; down scoops the middle, up brings it forward. Broad moves wide stretches of the spectrum at once."]
        },
        style = MaterialTheme.typography.bodySmall,
        color = DesktopSecondary,
        modifier = Modifier.padding(horizontal = panelInset(22.dp), vertical = 8.dp),
    )
}

private data class DesktopTonePreset(
    val name: String,
    val icon: String,
    val x: Int,
    val y: Int,
)

private val DESKTOP_TONE_PRESETS = listOf(
    DesktopTonePreset("Warm Vinyl", "🎛️", -4, -2),
    DesktopTonePreset("Crystal Air", "✨", 4, 3),
    DesktopTonePreset("Vocal Punch", "🎙️", 0, 4),
    DesktopTonePreset("Club Bass", "🔊", -3, 2),
    DesktopTonePreset("Studio Flat", "⚖️", 0, 0),
)

@Composable
private fun DynamicSoundstagePresets(
    currentX: Int,
    currentY: Int,
    onSelect: (Int, Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = panelInset(22.dp)),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DESKTOP_TONE_PRESETS.forEach { preset ->
            val isActive = currentX == preset.x && currentY == preset.y
            val bgColor = if (isActive) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)
            val textColor = if (isActive) Color.White else DesktopSecondary

            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onSelect(preset.x, preset.y) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(preset.icon, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    preset.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Live 7-band acoustic spectrum curve calculated directly from (x, y, focused).
 */
@Composable
private fun DesktopDynamicFrequencyCurve(
    x: Int,
    y: Int,
    focused: Boolean,
) {
    val tiltMultiplier = if (focused) 0.8f else 1.2f
    val contourMultiplier = if (focused) 1.5f else 1.2f

    val tiltDb = x * EqLayout.TONE_DB_PER_STEP * tiltMultiplier
    val contourDb = y * EqLayout.TONE_DB_PER_STEP * contourMultiplier

    // 7 sample frequencies matching graphic bands: 60Hz, 150Hz, 400Hz, 1kHz, 2.5kHz, 6kHz, 14kHz
    val bandWeights = remember(tiltDb, contourDb) {
        listOf(
            -tiltDb * 1.0f + contourDb * 0.2f,
            -tiltDb * 0.7f + contourDb * 0.1f,
            -tiltDb * 0.3f - contourDb * 0.4f,
            0f + contourDb * 1.0f,
            tiltDb * 0.3f - contourDb * 0.4f,
            tiltDb * 0.7f + contourDb * 0.1f,
            tiltDb * 1.0f + contourDb * 0.3f,
        ).map { it.coerceIn(-12f, 12f) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = panelInset(22.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141418))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ACOUSTIC FREQUENCY RESPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesktopSecondary.copy(alpha = 0.8f),
                )
                Text(
                    if (x == 0 && y == 0) "0 dB FLAT" else "${if (x >= 0) "+$x" else "$x"} / ${if (y >= 0) "+$y" else "$y"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = DesktopAccent.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(6.dp))

            Canvas(Modifier.fillMaxWidth().height(68.dp)) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val maxDb = 12f

                // Draw horizontal reference lines: +6dB, 0dB, -6dB
                val zeroY = midY
                val plus6Y = midY - (6f / maxDb) * (h * 0.42f)
                val minus6Y = midY + (6f / maxDb) * (h * 0.42f)

                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, plus6Y),
                    end = Offset(w, plus6Y),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, minus6Y),
                    end = Offset(w, minus6Y),
                    strokeWidth = 1f,
                )

                // Compute points
                val points = bandWeights.mapIndexed { i, db ->
                    val px = (i.toFloat() / (bandWeights.size - 1)) * w
                    val py = midY - (db / maxDb) * (h * 0.42f)
                    Offset(px, py)
                }

                // Spline path
                val curvePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                // Fill under curve
                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(w, midY)
                    lineTo(0f, midY)
                    close()
                }

                val fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.22f),
                        Color(0xFF818CF8).copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = h,
                )
                drawPath(fillPath, fillBrush)

                // Stroke curve with glowing gradient
                val strokeBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFF59E0B),
                        Color(0xFF10B981),
                        Color(0xFF06B6D4),
                    )
                )
                drawPath(
                    curvePath,
                    brush = strokeBrush,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Nodes on curve
                points.forEach { pt ->
                    drawCircle(Color.White, radius = 2.2.dp.toPx(), center = pt)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Frequency labels
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("60Hz", "150Hz", "400Hz", "1kHz", "2.5kHz", "6kHz", "14kHz").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = DesktopSecondary.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneReadoutBar(
    x: Int,
    y: Int,
    onReset: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = panelInset(22.dp), vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val tiltDesc = when {
                x < 0 -> "Warmer"
                x > 0 -> "Brighter"
                else -> "Balanced"
            }
            val contourDesc = when {
                y < 0 -> "Scooped"
                y > 0 -> "Forward"
                else -> "Neutral"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DesktopStrings["equalizer_tilt", "Tilt"], style = MaterialTheme.typography.bodySmall, color = DesktopSecondary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${if (x > 0) "+$x" else "$x"} ($tiltDesc)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DesktopStrings["equalizer_contour", "Contour"], style = MaterialTheme.typography.bodySmall, color = DesktopSecondary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${if (y > 0) "+$y" else "$y"} ($contourDesc)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        if (x != 0 || y != 0) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onReset)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Refresh, "Center", tint = DesktopAccent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Center", style = MaterialTheme.typography.labelSmall, color = DesktopAccent)
            }
        }
    }
}

@Composable
private fun WidthChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else DesktopSecondary,
        )
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(BitChordIcons.Check, null, tint = DesktopAccent, modifier = Modifier.size(15.dp))
        }
    }
}

/**
 * The dot grid and its puck, enhanced with quadrant ambient glows, crosshairs, and glowing lens puck.
 */
@Composable
private fun TonePad(x: Int, y: Int, onChange: (Int, Int) -> Unit) {
    val steps = EqLayout.TONE_STEPS
    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)

    val animatedX by animateFloatAsState(
        x.toFloat(),
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadX",
    )
    val animatedY by animateFloatAsState(
        y.toFloat(),
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadY",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = panelInset(22.dp))
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101014))
    ) {
        // Corner direction badges
        Text(
            "🔥 WARM BASS",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF59E0B).copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )
        Text(
            "✨ BRIGHT AIR",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF06B6D4).copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        )
        Text(
            "🎧 WARM SCOOP",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8B5CF6).copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
        Text(
            "⚡ CRISP SCOOP",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEC4899).copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
        )

        Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(steps) {
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

            // Draw 4 quadrant soft glows
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(inset, inset),
                    radius = usableWidth * 0.45f
                ),
                radius = usableWidth * 0.45f,
                center = Offset(inset, inset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width - inset, inset),
                    radius = usableWidth * 0.45f
                ),
                radius = usableWidth * 0.45f,
                center = Offset(size.width - inset, inset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(inset, size.height - inset),
                    radius = usableWidth * 0.45f
                ),
                radius = usableWidth * 0.45f,
                center = Offset(inset, size.height - inset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEC4899).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width - inset, size.height - inset),
                    radius = usableWidth * 0.45f
                ),
                radius = usableWidth * 0.45f,
                center = Offset(size.width - inset, size.height - inset)
            )

            // Centre crosshairs
            val midX = inset + usableWidth * 0.5f
            val midY = inset + usableHeight * 0.5f
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(inset, midY),
                end = Offset(size.width - inset, midY),
                strokeWidth = 1f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(midX, inset),
                end = Offset(midX, size.height - inset),
                strokeWidth = 1f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = 18f,
                center = Offset(midX, midY),
                style = Stroke(width = 1f)
            )

            // Grid dots
            for (column in 0..columns) {
                for (row in 0..columns) {
                    val onAxis = column == steps || row == steps
                    drawCircle(
                        color = Color.White.copy(alpha = if (onAxis) 0.50f else 0.18f),
                        radius = (if (onAxis) DOT_RADIUS.toPx() * 1.2f else DOT_RADIUS.toPx()),
                        center = Offset(
                            inset + usableWidth * column / columns,
                            inset + usableHeight * row / columns,
                        ),
                    )
                }
            }

            val centre = Offset(
                inset + usableWidth * (animatedX + steps) / columns,
                inset + usableHeight * (steps - animatedY) / columns,
            )

            // Puck glow tint matching direction
            val puckGlowColor = when {
                animatedX < 0 && animatedY > 0 -> Color(0xFFF59E0B)
                animatedX >= 0 && animatedY > 0 -> Color(0xFF06B6D4)
                animatedX < 0 && animatedY <= 0 -> Color(0xFF8B5CF6)
                else -> Color(0xFFEC4899)
            }

            // Glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(puckGlowColor.copy(alpha = 0.40f), Color.Transparent),
                    center = centre,
                    radius = inset * 2.2f,
                ),
                radius = inset * 2.2f,
                center = centre,
            )

            // Outer specular ring
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = inset,
                center = centre,
                style = Stroke(width = 2.dp.toPx())
            )
            // Solid center core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, puckGlowColor),
                    center = centre,
                    radius = inset * 0.75f,
                ),
                radius = inset * 0.75f,
                center = centre,
            )
        }
    }
}

/** Which step the pointer is over, clamped to the grid. */
private fun stepAt(offset: Offset, size: Size, inset: Float, steps: Int): Pair<Int, Int> {
    val usableWidth = (size.width - inset * 2).coerceAtLeast(1f)
    val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
    val fractionX = ((offset.x - inset) / usableWidth).coerceIn(0f, 1f)
    val fractionY = ((offset.y - inset) / usableHeight).coerceIn(0f, 1f)
    val x = (fractionX * steps * 2).roundToInt() - steps
    // Screen coordinates grow downwards and the control does not: up is more.
    return x to (steps - (fractionY * steps * 2).roundToInt())
}

// ---- Manual ----------------------------------------------------------------

@Composable
private fun ManualTab() {
    val bands by DesktopEqualizerSettings.bands.collectAsState()
    val preset by DesktopEqualizerSettings.preset.collectAsState()

    SectionLabel(DesktopStrings["equalizer_bands", "Bands"])
    Row(Modifier.fillMaxWidth().padding(horizontal = panelInset(18.dp))) {
        BandSliders(bands, Modifier.weight(1f)) { DesktopEqualizerSettings.setBands(it) }
        Spacer(Modifier.width(14.dp))
        // Beside the bands rather than behind a sheet: a window has the width for the list, and
        // seeing the curve move as a preset is picked is the point of having them.
        PresetList(preset, Modifier.width(168.dp))
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = panelInset(22.dp), vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            DesktopStrings["equalizer_bands_footer", "Moving a band leaves the preset behind and the curve becomes your own."],
            style = MaterialTheme.typography.bodySmall,
            color = DesktopSecondary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { DesktopEqualizerSettings.setPreset(EqualizerPreset.FLAT) }) {
            Text(DesktopStrings["equalizer_reset", "Reset all bands"], color = DesktopAccent)
        }
    }
}

/**
 * Seven vertical faders, drawn.
 *
 * A Material slider rotated a quarter turn keeps its horizontal geometry and its horizontal thumb,
 * which is why every Compose equaliser built that way feels wrong. The fill runs from the centre
 * because the value is signed: −6 dB and +6 dB are equal and opposite, and a bar growing from the
 * floor would draw them as a short one and a tall one.
 */
@Composable
private fun BandSliders(bands: List<Float>, modifier: Modifier = Modifier, onChange: (List<Float>) -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        EqLayout.MANUAL_BANDS_HZ.forEachIndexed { index, hz ->
            val value = bands.getOrElse(index) { 0f }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatGain(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (abs(value) < 0.05f) DesktopSecondary else DesktopAccent,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                BandFader(value) { updated ->
                    onChange(bands.toMutableList().also { it[index] = updated })
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    formatFrequency(hz),
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BandFader(value: Float, onChange: (Float) -> Unit) {
    val range = EqLayout.MANUAL_RANGE_DB
    val latest by rememberUpdatedState(value)

    Canvas(
        Modifier
            .width(30.dp)
            .height(150.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var last = latest
                    fun report(offset: Offset) {
                        val inset = KNOB_RADIUS.toPx()
                        val usable = (size.height - inset * 2).coerceAtLeast(1f)
                        val fraction = ((offset.y - inset) / usable).coerceIn(0f, 1f)
                        val snapped = snapGain((1f - fraction * 2f) * range, range)
                        if (abs(snapped - last) > 0.001f) {
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

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(centreX - trackWidth / 2, inset),
            size = Size(trackWidth, usable),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        if (abs(value) > 0.05f) {
            drawRoundRect(
                color = DesktopAccent,
                topLeft = Offset(centreX - trackWidth / 2, minOf(centreY, knobY)),
                size = Size(trackWidth, abs(centreY - knobY)),
                cornerRadius = CornerRadius(trackWidth / 2),
            )
        }
        // The zero line, over the track so it survives a full-scale fill.
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(centreX - trackWidth, centreY),
            end = Offset(centreX + trackWidth, centreY),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = inset + 1.5f,
            center = Offset(centreX, knobY + 1.5f),
        )
        drawCircle(color = Color.White, radius = inset, center = Offset(centreX, knobY))
    }
}

@Composable
private fun PresetList(selected: EqualizerPreset, modifier: Modifier = Modifier) {
    Column(modifier.heightIn(max = 186.dp).verticalScroll(rememberScrollState())) {
        EqualizerPreset.entries.forEach { preset ->
            // Custom is a state the bands can be in, not a curve anyone can choose.
            if (preset == EqualizerPreset.CUSTOM && selected != EqualizerPreset.CUSTOM) return@forEach
            val chosen = preset == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (chosen) Color.White.copy(alpha = 0.09f) else Color.Transparent)
                    .clickable(enabled = preset != EqualizerPreset.CUSTOM) {
                        DesktopEqualizerSettings.setPreset(preset)
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    presetLabel(preset),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (chosen) {
                    Icon(BitChordIcons.Check, null, tint = DesktopAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ---- Balance ---------------------------------------------------------------

/**
 * L and R with the trim between them, and a detent at centre: dead centre is where this spends its
 * life, and it is the one position a drag cannot land on by aim alone.
 */
@Composable
private fun BalanceControl(balance: Float, onChange: (Float) -> Unit) {
    val latest by rememberUpdatedState(balance)
    Column(Modifier.padding(horizontal = panelInset(22.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(DesktopStrings["equalizer_balance_left", "L"], style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(
                String.format(Locale.ROOT, "%.2f", balance),
                style = MaterialTheme.typography.bodyMedium,
                color = DesktopSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(DesktopStrings["equalizer_balance_right", "R"], style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(KNOB_RADIUS * 2 + 8.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var last = latest
                        fun report(offset: Offset) {
                            val inset = KNOB_RADIUS.toPx()
                            val usable = (size.width - inset * 2).coerceAtLeast(1f)
                            val raw = ((offset.x - inset) / usable).coerceIn(0f, 1f) * 2f - 1f
                            val snapped = if (abs(raw) < BALANCE_DETENT) 0f else raw
                            if (abs(snapped - last) > 0.004f) {
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
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(inset, centreY - trackHeight / 2),
                size = Size(usable, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2),
            )
            if (abs(balance) > 0.004f) {
                drawRoundRect(
                    color = DesktopAccent,
                    topLeft = Offset(minOf(centreX, knobX), centreY - trackHeight / 2),
                    size = Size(abs(centreX - knobX), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2),
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = inset + 1.5f,
                center = Offset(knobX, centreY + 1.5f),
            )
            drawCircle(color = Color.White, radius = inset, center = Offset(knobX, centreY))
        }
    }
}

// ---- Formatting ------------------------------------------------------------

/**
 * Half-decibel steps, with a wider one at zero.
 *
 * Flat is the value a band gets dragged back to far more often than any other, so it is given a
 * detent to fall into rather than a coordinate to find.
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
        String.format(Locale.ROOT, "%.1f", value)
    }

/** "60", "2.5k", "14k" — the labels a hardware equaliser prints. */
private fun formatFrequency(hz: Float): String = when {
    hz < 1_000f -> hz.roundToInt().toString()
    hz % 1_000f == 0f -> "${(hz / 1_000f).roundToInt()}k"
    else -> String.format(Locale.ROOT, "%.1fk", hz / 1_000f)
}

private fun presetLabel(preset: EqualizerPreset): String = when (preset) {
    EqualizerPreset.FLAT -> DesktopStrings["eq_preset_flat", "Flat"]
    EqualizerPreset.ACOUSTIC -> DesktopStrings["eq_preset_acoustic", "Acoustic"]
    EqualizerPreset.BASS_BOOST -> DesktopStrings["eq_preset_bass_boost", "Bass boost"]
    EqualizerPreset.BASS_CUT -> DesktopStrings["eq_preset_bass_cut", "Bass cut"]
    EqualizerPreset.VOCAL -> DesktopStrings["eq_preset_vocal", "Vocal"]
    EqualizerPreset.TREBLE_BOOST -> DesktopStrings["eq_preset_treble_boost", "Treble boost"]
    EqualizerPreset.TREBLE_CUT -> DesktopStrings["eq_preset_treble_cut", "Treble cut"]
    EqualizerPreset.LOUDNESS -> DesktopStrings["eq_preset_loudness", "Loudness"]
    EqualizerPreset.SPOKEN_WORD -> DesktopStrings["eq_preset_spoken_word", "Spoken word"]
    EqualizerPreset.ELECTRONIC -> DesktopStrings["eq_preset_electronic", "Electronic"]
    EqualizerPreset.ROCK -> DesktopStrings["eq_preset_rock", "Rock"]
    EqualizerPreset.HIP_HOP -> DesktopStrings["eq_preset_hip_hop", "Hip-hop"]
    EqualizerPreset.JAZZ -> DesktopStrings["eq_preset_jazz", "Jazz"]
    EqualizerPreset.CLASSICAL -> DesktopStrings["eq_preset_classical", "Classical"]
    EqualizerPreset.SMALL_SPEAKERS -> DesktopStrings["eq_preset_small_speakers", "Small speakers"]
    EqualizerPreset.LATE_NIGHT -> DesktopStrings["eq_preset_late_night", "Late night"]
    EqualizerPreset.CUSTOM -> DesktopStrings["eq_preset_custom", "Custom"]
}

private val PUCK_RADIUS = 16.dp
private val DOT_RADIUS = 2.2.dp
private val TRACK_WIDTH = 5.dp
private val KNOB_RADIUS = 9.dp

/** How close to flat a band has to be dragged before it snaps there. */
private const val ZERO_DETENT_DB = 0.6f

/** Same, for the balance trim's own centre. */
private const val BALANCE_DETENT = 0.04f
