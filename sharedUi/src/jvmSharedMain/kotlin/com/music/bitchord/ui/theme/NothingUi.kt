package com.music.bitchord.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Nothing OS Design System tokens for AuraMusic.
 * High-contrast AMOLED pitch black, stark whites, slate borders, and Nothing Red accents.
 */
object NothingThemeTokens {
    val Red = Color(0xFFD71921)
    val RedGlow = Color(0xFFFF2E3D)
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0C0C0E)
    val SurfaceElevated = Color(0xFF141417)
    val Border = Color(0xFF26262B)
    val BorderSubtle = Color(0xFF1E1E22)
    val White = Color(0xFFFFFFFF)
    val MutedText = Color(0xFF909096)
    val DimDot = Color(0xFF222226)
}

/**
 * Squircle card styled with Nothing OS signature subtle border and pitch-black surface.
 */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    backgroundColor: Color = NothingThemeTokens.Surface,
    borderColor: Color = NothingThemeTokens.Border,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        content = content,
    )
}

/**
 * Capsule pill button with an optional active red LED indicator dot.
 */
@Composable
fun NothingPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val borderColor = if (isActive) NothingThemeTokens.Red else NothingThemeTokens.Border
    val textColor = if (isActive) NothingThemeTokens.White else NothingThemeTokens.MutedText

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isActive) NothingThemeTokens.SurfaceElevated else NothingThemeTokens.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingThemeTokens.Red),
            )
            Spacer(Modifier.width(6.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * 3x5 Dot Matrix character definitions for numbers 0-9, colon, and minus sign.
 * 1 means active illuminated dot, 0 means unlit ghost dot.
 */
private val DOT_MATRIX_CHARS: Map<Char, List<IntArray>> = mapOf(
    '0' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '1' to listOf(
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 1),
    ),
    '2' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
    ),
    '3' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '4' to listOf(
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 0, 1),
    ),
    '5' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '6' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '7' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
    ),
    '8' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '9' to listOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    ':' to listOf(
        intArrayOf(0),
        intArrayOf(1),
        intArrayOf(0),
        intArrayOf(1),
        intArrayOf(0),
    ),
    '-' to listOf(
        intArrayOf(0, 0, 0),
        intArrayOf(0, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0),
        intArrayOf(0, 0, 0),
    ),
)

/**
 * Authentic Nothing OS hardware Dot-Matrix digital clock & timestamp renderer.
 * Draws every active dot brightly, and unlit dots with a faint ghostly outline.
 */
@Composable
fun NothingGlyphTimer(
    timeText: String,
    modifier: Modifier = Modifier,
    dotColor: Color = NothingThemeTokens.White,
    ghostColor: Color = Color.White.copy(alpha = 0.09f),
    dotRadiusDp: Dp = 1.3.dp,
    spacingDp: Dp = 1.0.dp,
    charSpacingDp: Dp = 2.4.dp,
) {
    Canvas(modifier = modifier) {
        val dotRadius = dotRadiusDp.toPx()
        val spacing = spacingDp.toPx()
        val charSpacing = charSpacingDp.toPx()
        val cellStep = dotRadius * 2 + spacing

        var cursorX = 0f

        timeText.forEach { ch ->
            val matrix = DOT_MATRIX_CHARS[ch]
            if (matrix != null) {
                val cols = matrix.firstOrNull()?.size ?: 0
                matrix.forEachIndexed { rowIndex, row ->
                    val y = rowIndex * cellStep + dotRadius
                    row.forEachIndexed { colIndex, cell ->
                        val x = cursorX + colIndex * cellStep + dotRadius
                        val isLit = cell == 1
                        drawCircle(
                            color = if (isLit) dotColor else ghostColor,
                            radius = dotRadius,
                            center = Offset(x, y),
                        )
                    }
                }
                cursorX += cols * cellStep + charSpacing
            } else {
                // Unknown character, skip width
                cursorX += cellStep * 2 + charSpacing
            }
        }
    }
}

/**
 * Animated vertical LED matrix soundwave equalizer reacting to audio playback.
 * Four columns of stacked LED dots that bounce smoothly when music is playing.
 */
@Composable
fun NothingGlyphEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    dotColor: Color = NothingThemeTokens.White,
    peakColor: Color = NothingThemeTokens.Red,
    ghostColor: Color = Color.White.copy(alpha = 0.12f),
    barCount: Int = 4,
    dotsPerBar: Int = 5,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nothingEqTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nothingEqPhase",
    )

    Canvas(modifier = modifier) {
        val dotRadius = 1.8.dp.toPx()
        val spacing = 1.5.dp.toPx()
        val barSpacing = 4.5.dp.toPx()
        val cellY = dotRadius * 2 + spacing
        val totalBarWidth = dotRadius * 2

        val totalWidth = barCount * totalBarWidth + (barCount - 1) * barSpacing
        val startX = (size.width - totalWidth) / 2f

        for (b in 0 until barCount) {
            val barX = startX + b * (totalBarWidth + barSpacing) + dotRadius

            // Compute height/active dot count based on phase or idle
            val activeDots = if (isPlaying) {
                val wave = (sin(phase + b * 1.5f) + 1f) / 2f // 0..1
                val minActive = 1
                (minActive + (wave * (dotsPerBar - minActive)).toInt()).coerceIn(1, dotsPerBar)
            } else {
                1
            }

            for (d in 0 until dotsPerBar) {
                // d = 0 is bottom, d = dotsPerBar - 1 is top
                val y = size.height - (d * cellY + dotRadius)
                val isLit = d < activeDots
                val isPeak = isLit && d == activeDots - 1 && d >= 3

                val color = when {
                    isPeak -> peakColor
                    isLit -> dotColor
                    else -> ghostColor
                }

                drawCircle(
                    color = color,
                    radius = dotRadius,
                    center = Offset(barX, y),
                )
            }
        }
    }
}
