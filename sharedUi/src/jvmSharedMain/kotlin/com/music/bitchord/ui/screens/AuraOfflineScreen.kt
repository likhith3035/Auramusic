package com.music.bitchord.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.aura_offline_hero
import com.music.bitchord.sharedui.resources.aura_offline_waves
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import org.jetbrains.compose.resources.painterResource

/**
 * AuraMusic Offline Experience.
 *
 * Appears when the user has no internet connection, transforming a network drop
 * into an engaging, branded cyberpunk experience with instant offline playback actions:
 * 1. "Listen to Local Music": Immediately plays and shuffles stored device audio.
 * 2. "Browse Device Music": Opens the full local device browser (Songs, Artists, Albums).
 * 3. "Try Again": Re-tests network connectivity and reloads active feeds.
 */
@Composable
fun AuraOfflineScreen(
    onListenLocalMusic: () -> Unit,
    onBrowseDeviceMusic: () -> Unit,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isRetrying: Boolean = false,
) {
    val haptics = rememberHaptics()

    // Smooth continuous spin when actively retrying network
    val infiniteTransition = rememberInfiniteTransition(label = "offlineRetrySpin")
    val retryRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "retryRotation",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06050A)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 40.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero Illustration: Glowing AuraWave logo, "AuraMusic — BY LIKHITH —", & Headphones
            Image(
                painter = painterResource(Res.drawable.aura_offline_hero),
                contentDescription = "AuraMusic Offline",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 290.dp),
            )

            Spacer(Modifier.height(14.dp))

            // Headline: "You're Offline" with neon cyan & magenta highlights
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "You're ",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Color.White,
                )
                Text(
                    text = "Off",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Color(0xFF00E5FF),
                )
                Text(
                    text = "line",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = Color(0xFFFF2A85),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Subtitle Description
            Text(
                text = "No internet connection right now.\nYou can still enjoy music stored on your phone.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(26.dp))

            // Button 1: "Listen to Local Music" (Vibrant Cyan-to-Magenta Gradient Pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00C6FF),
                                Color(0xFF0072FF),
                                Color(0xFFD500F9),
                                Color(0xFFFF2A85),
                            )
                        )
                    )
                    .clickable {
                        haptics.play(Haptic.ToggleOn)
                        onListenLocalMusic()
                    }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Listen to Local Music",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Button 2: "Browse Device Music" (Frosted Glass Dark Pill with Neon Border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF13111E).copy(alpha = 0.85f))
                    .border(
                        width = 1.4.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00D2FF).copy(alpha = 0.7f),
                                Color(0xFF9D00FF).copy(alpha = 0.8f),
                                Color(0xFFFF007A).copy(alpha = 0.7f),
                            )
                        ),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .clickable {
                        haptics.play(Haptic.Select)
                        onBrowseDeviceMusic()
                    }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFCE93D8),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Browse Device Music",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Button 3: "Try Again" with Animated Refresh Circle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        haptics.play(Haptic.Tap)
                        onTryAgain()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Try Again",
                        tint = Color.White,
                        modifier = Modifier
                            .size(19.dp)
                            .rotate(if (isRetrying) retryRotation else 0f),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isRetrying) "Checking connection..." else "Try Again",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Bottom Wave Decoration Asset
            Image(
                painter = painterResource(Res.drawable.aura_offline_waves),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 60.dp),
            )

            // Tagline
            Text(
                text = "Your music. Your vibe. Anywhere.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.3.sp,
                ),
                color = Color(0xFF7E7D9C),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            // Home Indicator Bar
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE040FB).copy(alpha = 0.6f)),
            )
        }
    }
}
