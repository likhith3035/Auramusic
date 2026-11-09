package com.music.bitchord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.network.AuraDataUsageTracker
import com.music.bitchord.data.network.AuraNetworkMonitor
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.NothingThemeTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data Usage & Network Economy Card for Settings.
 * Matches Aura and Nothing OS visual aesthetic.
 * Zero Unicode emojis - strict vector and typography design.
 */
@Composable
fun AuraDataUsageCard(
    modifier: Modifier = Modifier,
    nothingOsMode: Boolean = false,
) {
    val cellularBytes by AppSettings.cellularDataBytes.collectAsStateWithLifecycle()
    val wifiBytes by AppSettings.wifiDataBytes.collectAsStateWithLifecycle()
    val videoBytes by AppSettings.videoDataBytes.collectAsStateWithLifecycle()
    val audioBytes by AppSettings.audioDataBytes.collectAsStateWithLifecycle()
    val otherBytes by AppSettings.otherDataBytes.collectAsStateWithLifecycle()
    val savedBytes by AppSettings.savedDataBytes.collectAsStateWithLifecycle()
    val resetTimeMs by AppSettings.dataUsageResetTimeMs.collectAsStateWithLifecycle()
    val quality by AuraNetworkMonitor.quality.collectAsStateWithLifecycle()

    val totalBytes = cellularBytes + wifiBytes
    val safeTotal = if (totalBytes > 0) totalBytes.toFloat() else 1f
    val cellRatio = (cellularBytes.toFloat() / safeTotal).coerceIn(0f, 1f)
    val wifiRatio = (wifiBytes.toFloat() / safeTotal).coerceIn(0f, 1f)

    val cardBg = if (nothingOsMode) {
        NothingThemeTokens.Surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val cardBorder = if (nothingOsMode) {
        NothingThemeTokens.Border
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Header Row: Metric Tag + Current Network Connection Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)),
                    )
                    Text(
                        text = "NETWORK TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp,
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White.copy(alpha = 0.85f),
                        ),
                    )
                }

                // Active connection badge
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.15f)),
                ) {
                    Text(
                        text = if (quality.isWifi) "WI-FI ACTIVE" else "CELLULAR ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (quality.isWifi) Color(0xFF00E676) else Color(0xFFFFB74D),
                        ),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Stat: Total Data Consumed
            Text(
                text = AuraDataUsageTracker.formatBytes(totalBytes),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                ),
            )
            Text(
                text = "Total transferred across streaming, video, and cache",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.55f),
                ),
            )

            Spacer(Modifier.height(18.dp))

            // Two-Tone Segmented Progress Bar (Cellular vs Wi-Fi)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                if (totalBytes > 0) {
                    Row(modifier = Modifier.matchParentSize()) {
                        if (cellRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(cellRatio.coerceAtLeast(0.001f))
                                    .height(8.dp)
                                    .background(Color(0xFFFF5252)),
                            )
                        }
                        if (wifiRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(wifiRatio.coerceAtLeast(0.001f))
                                    .height(8.dp)
                                    .background(Color(0xFF00E676)),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bar Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252)),
                    )
                    Text(
                        text = "Cellular: ${AuraDataUsageTracker.formatBytes(cellularBytes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White.copy(alpha = 0.75f),
                        ),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)),
                    )
                    Text(
                        text = "Wi-Fi: ${AuraDataUsageTracker.formatBytes(wifiBytes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White.copy(alpha = 0.75f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Hairline separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.08f)),
            )

            Spacer(Modifier.height(14.dp))

            // Breakdown List
            UsageBreakdownRow(
                icon = Icons.Rounded.PlayArrow,
                title = "Video Streaming",
                value = AuraDataUsageTracker.formatBytes(videoBytes),
                nothingOsMode = nothingOsMode,
            )
            Spacer(Modifier.height(10.dp))
            UsageBreakdownRow(
                icon = Icons.Rounded.MusicNote,
                title = "Studio Audio",
                value = AuraDataUsageTracker.formatBytes(audioBytes),
                nothingOsMode = nothingOsMode,
            )
            Spacer(Modifier.height(10.dp))
            UsageBreakdownRow(
                icon = Icons.Rounded.Image,
                title = "Artwork & Canvases",
                value = AuraDataUsageTracker.formatBytes(otherBytes),
                nothingOsMode = nothingOsMode,
            )
            Spacer(Modifier.height(10.dp))
            UsageBreakdownRow(
                icon = Icons.Rounded.CheckCircle,
                title = "Bandwidth Saved",
                value = AuraDataUsageTracker.formatBytes(savedBytes),
                badge = "SAVED BY AURA",
                accentColor = Color(0xFF00E676),
                nothingOsMode = nothingOsMode,
            )

            Spacer(Modifier.height(18.dp))

            // Footer: Reset Timestamp & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val dateStr = remember(resetTimeMs) {
                    if (resetTimeMs > 0) {
                        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(resetTimeMs))
                    } else "Current Cycle"
                }

                Text(
                    text = "Cycle started: $dateStr",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.45f),
                    ),
                )

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.clickable {
                        AppSettings.resetDataUsageStats()
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Reset Counter",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageBreakdownRow(
    icon: ImageVector,
    title: String,
    value: String,
    badge: String? = null,
    accentColor: Color? = null,
    nothingOsMode: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor ?: (if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.6f)),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (nothingOsMode) NothingThemeTokens.White else Color.White.copy(alpha = 0.85f),
                ),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, Color(0xFF00E676).copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = value,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = accentColor ?: (if (nothingOsMode) NothingThemeTokens.White else Color.White),
            ),
        )
    }
}

