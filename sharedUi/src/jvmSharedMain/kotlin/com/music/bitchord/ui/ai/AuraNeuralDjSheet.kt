package com.music.bitchord.ui.ai

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DiscFull
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.ai.AuraNeuralDjEngine
import com.music.bitchord.data.model.Song

/**
 * Aura Neural DJ modal displaying harmonic Camelot Wheel keys, BPM detection,
 * and seamless transitions from queue candidates.
 */
@Composable
fun AuraNeuralDjSheet(
    currentSong: Song?,
    queueCandidates: List<Song>,
    onClose: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentSong == null) return

    val currentProfile = remember(currentSong.videoId) {
        AuraNeuralDjEngine.analyzeSong(currentSong)
    }

    val harmonicMatches = remember(currentSong.videoId, queueCandidates) {
        AuraNeuralDjEngine.findHarmonicTransitions(currentSong, queueCandidates)
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF0E111F),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F5D4)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DiscFull,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aura Neural DJ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Harmonic Camelot Wheel & BPM Matching",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                            ),
                            color = Color(0xFF00F5D4),
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Currently Playing DJ Deck Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161A2D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222638)),
                    ) {
                        if (!currentSong.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentSong.thumbnailUrl,
                                contentDescription = currentSong.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentSong.artist ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DjBadge(label = "${currentProfile.bpm} BPM", color = Color(0xFFFFA07A))
                            DjBadge(label = "Key ${currentProfile.key}", color = Color(0xFF00F5D4))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Harmonically Compatible Next Tracks",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
                color = Color.White.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(8.dp))

            if (harmonicMatches.isEmpty()) {
                Text(
                    text = "Add more songs to queue to see harmonic transition recommendations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(harmonicMatches.take(6)) { match ->
                        val isHarmonic = match.key.isCompatibleWith(currentProfile.key)
                        val crossfadeSec = AuraNeuralDjEngine.calculateOptimalCrossfade(currentProfile, match)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF131627),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isHarmonic) Color(0xFF00F5D4).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPlaySong(match.song) },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = match.song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = match.song.artist ?: "Unknown",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color.White.copy(alpha = 0.6f),
                                        maxLines = 1,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        DjBadge(label = "${match.bpm} BPM", color = Color(0xFFFFA07A))
                                        DjBadge(label = "Key ${match.key}", color = if (isHarmonic) Color(0xFF00F5D4) else Color.White.copy(alpha = 0.5f))
                                        DjBadge(label = "${crossfadeSec}s Mix", color = Color(0xFFE94057))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onQueueSong(match.song) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlaylistAdd,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPlaySong(match.song) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF00F5D4),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DjBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
