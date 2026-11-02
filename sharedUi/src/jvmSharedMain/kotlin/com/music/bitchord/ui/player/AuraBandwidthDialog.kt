package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.network.NetworkQuality
import com.music.bitchord.data.network.NetworkTier


/**
 * Pre-Flight Bandwidth Guard Dialog:
 * Proactively advises users before switching into Video mode when
 * network throughput is slow or mobile data is metered.
 * Zero Unicode emojis - clean vector glyphs and typography.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AuraBandwidthDialog(
    quality: NetworkQuality,
    nothingOsMode: Boolean,
    onConfirmQuality: (Int?) -> Unit,
    onStayOnAudio: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12141F),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        ) {
            // Category Badge & Signal Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (quality.tier == NetworkTier.SLOW) Color(0xFFFF5252) else Color(0xFFFFB74D),
                        ),
                )
                Text(
                    text = if (quality.isMetered) "METERED CELLULAR DATA" else "SLOW NETWORK ADVISORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 10.sp,
                    ),
                    color = Color(0xFFFFB74D),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = if (quality.isMetered) "Conserve Mobile Data?" else "Stream Data-Saver Video?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = Color.White,
            )

            Spacer(Modifier.height(6.dp))

            // Body
            val mbps = (quality.bandwidthKbps / 1000f).coerceAtLeast(0.5f)
            val description = when {
                quality.isMetered && quality.tier == NetworkTier.SLOW ->
                    "Your cellular connection is metered and running slow (approx %.1f Mbps). Streaming 1080p video may stutter and quickly drain mobile data.".format(mbps)
                quality.isMetered ->
                    "You are currently on mobile data. Music videos stream up to 25 MB per track, whereas Studio Audio consumes under 2 MB.".format(mbps)
                else ->
                    "Your network throughput is currently limited (approx %.1f Mbps). 360p Data Saver ensures smooth playback without video pauses.".format(mbps)
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                color = Color.White.copy(alpha = 0.72f),
            )

            Spacer(Modifier.height(20.dp))

            // Option 1: Stream in 360p Data Saver (Recommended)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.16f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onConfirmQuality(360)
                        onDismiss()
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Color(0xFF6366F1).copy(alpha = 0.22f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stream in 360p Data Saver (Recommended)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Smooth playback, 85% less data, zero audio stutter",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Option 2: Keep Studio Audio Uninterrupted
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.10f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStayOnAudio()
                        onDismiss()
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Color(0xFF8B5CF6).copy(alpha = 0.22f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFC4B5FD),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep Studio Audio with Artwork",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Lossless acoustic clarity, minimal data, zero buffering",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Option 3: Force Full HD
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.08f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onConfirmQuality(null) // Auto/Full HD
                        onDismiss()
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HighQuality,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Force Full HD Anyway",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

