package com.music.bitchord.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.PartyReaction
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.sin
import kotlin.random.Random

val POPULAR_PARTY_REACTIONS = listOf("🔥", "❤️", "💃", "🎧", "⚡", "🎉")

private data class ActiveParticle(
    val id: String,
    val emoji: String,
    val initialXRatio: Float,
    val driftFactor: Float,
)

/**
 * Fullscreen overlay that renders emojis floating upwards when listeners react in real time.
 */
@Composable
fun FloatingReactionOverlay(
    modifier: Modifier = Modifier,
) {
    val activeParticles = remember { mutableStateListOf<ActiveParticle>() }

    LaunchedEffect(Unit) {
        ListenTogether.reactions.collectLatest { reaction ->
            val particle = ActiveParticle(
                id = reaction.id.ifBlank { "${System.currentTimeMillis()}-${Random.nextInt()}" },
                emoji = reaction.emoji,
                initialXRatio = 0.25f + Random.nextFloat() * 0.55f,
                driftFactor = if (Random.nextBoolean()) 1f else -1f,
            )
            activeParticles.add(particle)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val heightPx = with(density) { maxHeight.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }

        activeParticles.forEach { particle ->
            SingleFloatingEmoji(
                particle = particle,
                totalHeightPx = heightPx,
                totalWidthPx = widthPx,
                onFinished = { activeParticles.remove(particle) },
            )
        }
    }
}

@Composable
private fun SingleFloatingEmoji(
    particle: ActiveParticle,
    totalHeightPx: Float,
    totalWidthPx: Float,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(particle.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing),
        )
        onFinished()
    }

    val t = progress.value
    // Float upwards from bottom 85% to top 15%
    val yPos = totalHeightPx * 0.85f - (t * totalHeightPx * 0.70f)
    // Sinusoidal drift
    val xOffset = sin(t * Math.PI.toFloat() * 3f) * 36f * particle.driftFactor
    val xPos = (totalWidthPx * particle.initialXRatio) + xOffset
    // Alpha fades out towards top
    val alpha = when {
        t < 0.15f -> t / 0.15f
        t > 0.75f -> (1f - t) / 0.25f
        else -> 1f
    }
    // Scale up from 0.8 to 1.3
    val scale = 0.8f + (t * 0.5f)

    Box(
        modifier = Modifier
            .offset { IntOffset(xPos.toInt(), yPos.toInt()) }
            .alpha(alpha.coerceIn(0f, 1f))
            .scale(scale),
    ) {
        Text(
            text = particle.emoji,
            fontSize = 32.sp,
        )
    }
}

/**
 * Compact horizontal reaction pill strip for quickly sending emojis.
 */
@Composable
fun PartyReactionQuickBar(
    onSendReaction: (String) -> Unit = { ListenTogether.sendReaction(it) },
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        tonalElevation = 4.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            POPULAR_PARTY_REACTIONS.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onSendReaction(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                }
            }
        }
    }
}
