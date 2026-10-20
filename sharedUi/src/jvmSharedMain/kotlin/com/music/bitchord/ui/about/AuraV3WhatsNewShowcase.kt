package com.music.bitchord.ui.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Metadata for a v3.0 highlight card.
 */
private data class V3FeatureHighlight(
    val title: String,
    val subtitle: String,
    val tag: String,
    val icon: ImageVector,
    val accentColor: Color,
    val glowGradient: List<Color>,
)

private val V3_FEATURES = listOf(
    V3FeatureHighlight(
        title = "5 Liquid Glass App Icons",
        subtitle = "Choose from Azure, Obsidian, Crimson, Amethyst, and Prism glowing soundwaves on your home screen.",
        tag = "CUSTOM ICONS",
        icon = Icons.Rounded.AutoAwesome,
        accentColor = Color(0xFF67E8F9),
        glowGradient = listOf(Color(0xFF06B6D4).copy(alpha = 0.25f), Color(0xFF0891B2).copy(alpha = 0.05f)),
    ),
    V3FeatureHighlight(
        title = "Audio Output Route Pill",
        subtitle = "Apple Music-style interactive pill on the player showing connected headphones or speakers with 1-tap switching.",
        tag = "AUDIO ROUTING",
        icon = Icons.Rounded.Cloud,
        accentColor = Color(0xFFA855F7),
        glowGradient = listOf(Color(0xFF9333EA).copy(alpha = 0.25f), Color(0xFF6B21A8).copy(alpha = 0.05f)),
    ),
    V3FeatureHighlight(
        title = "Song Info & Track Specs",
        subtitle = "Deep-dive into bitrates, sample rates, duration, release year, songwriters, and Behind the Track synopsis.",
        tag = "TRACK SPECS",
        icon = Icons.Rounded.AutoAwesome,
        accentColor = Color(0xFF10B981),
        glowGradient = listOf(Color(0xFF059669).copy(alpha = 0.25f), Color(0xFF047857).copy(alpha = 0.05f)),
    ),
    V3FeatureHighlight(
        title = "Organized Downloads & Local Music",
        subtitle = "Browse your offline music library cleanly separated into Songs, Artists, and Albums tabs.",
        tag = "LOCAL MUSIC",
        icon = Icons.Rounded.ThumbUp,
        accentColor = Color(0xFFF59E0B),
        glowGradient = listOf(Color(0xFFD97706).copy(alpha = 0.25f), Color(0xFFB45309).copy(alpha = 0.05f)),
    ),
    V3FeatureHighlight(
        title = "Listen Together Cloud Sync",
        subtitle = "Ultra-low latency zero-drift party engine with live in-room messaging and soundbites.",
        tag = "PARTY ENGINE",
        icon = Icons.Rounded.Forum,
        accentColor = Color(0xFFEC4899),
        glowGradient = listOf(Color(0xFFDB2777).copy(alpha = 0.25f), Color(0xFFBE185D).copy(alpha = 0.05f)),
    ),
)

/**
 * Creative glassmorphic Showcase UI introducing AuraMusic 3.0.
 */
@Composable
fun AuraV3WhatsNewShowcase(
    onDismiss: () -> Unit,
    onExploreParty: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFA855F7).copy(alpha = pulseGlow),
                            Color(0xFF38BDF8).copy(alpha = 0.2f),
                            Color(0xFF1E1E2E).copy(alpha = 0.6f),
                        ),
                    ),
                ),
                shape = RoundedCornerShape(28.dp),
            ),
        color = Color(0xFF0D0D14),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Top Bar: Badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Shiny Version 3.0 Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF7C3AED).copy(alpha = 0.25f),
                                    Color(0xFF2563EB).copy(alpha = 0.25f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFFA78BFA), Color(0xFF60A5FA)),
                            ),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                        )
                        Text(
                            text = "AURAMUSIC 3.1 • MAJOR UPDATE",
                            color = Color(0xFFE0E7FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F2E)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Title
            Text(
                text = "Welcome to AuraMusic 3.1",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "5 Liquid Glass app icons, interactive audio routing pill, comprehensive track specifications, and organized offline music tabs.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            // 4 Bento Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                V3_FEATURES.forEach { item ->
                    BentoFeatureRow(item)
                }
            }

            Spacer(Modifier.height(22.dp))

            // Developer / Attribution Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Text(
                    text = "Crafted with passion by ",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                )
                Text(
                    text = "Kami Likhith",
                    color = Color(0xFFA78BFA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onExploreParty,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED),
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Explore Listen Together",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF272738)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCBD5E1),
                    ),
                ) {
                    Text(
                        text = "Got it, continue listening",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoFeatureRow(item: V3FeatureHighlight) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF14141F), Color(0xFF101018)),
                ),
            )
            .border(1.dp, Color(0xFF262638), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, item.accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(item.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = item.tag,
                            color = item.accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = item.subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

/**
 * Dialog wrapper for displaying the v3.0 showcase anywhere in the app.
 */
@Composable
fun AuraV3WhatsNewDialog(
    onDismissRequest: () -> Unit,
    onExploreParty: () -> Unit = onDismissRequest,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AuraV3WhatsNewShowcase(
                onDismiss = onDismissRequest,
                onExploreParty = onExploreParty,
            )
        }
    }
}
